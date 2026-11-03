# 03 — Firebase y datos

Este documento explica dónde se guardan los datos, cómo se leen y se escriben, y quién puede acceder a ellos. Todo el código que habla con Firebase está en [`core/data/`](../app/src/main/kotlin/com/example/huellitasya/core/data).

## Los servicios de Firebase

| Servicio | Para qué se usa | Dónde está el código |
|---|---|---|
| **Authentication** | Cuentas con correo y contraseña o con Google. | [`FirebaseAuthRepository`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirebaseAuthRepository.kt) |
| **Firestore** | Base de datos en la nube. Guarda perfiles, mascotas, registros y recordatorios como documentos. | `Firestore*Repository` en [`core/data/repository/`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository) |
| **Storage** | Archivos: fotos de perfil y de mascotas, y los PDF e imágenes de los registros médicos. | `FirebaseStorage*Repository` |
| **Cloud Functions** | Backend propio en TypeScript, para lo que el teléfono no debe hacer solo. | [`functions/src/index.ts`](../functions/src/index.ts) y los `Functions*Repository` |

## Cómo están organizados los datos

Firestore guarda **documentos** dentro de **colecciones**. Todo cuelga del usuario: `users/{uid}`, donde `{uid}` es el identificador de la cuenta.

```text
users/{uid}                          perfil: nombre, correo, foto, teléfono, dirección…
├── pets/{petId}                     una mascota
│   └── records/{recordId}           un registro médico de esa mascota
└── reminders/{reminderId}           un recordatorio (de cualquier mascota)
```

| Colección | Campos | Modelo en la app |
|---|---|---|
| `users/{uid}` | `uid`, `name`, `email`, `photoUrl`, `provider`, `phone`, `address`, `birthDate`, `createdAt`, `updatedAt` | [`UserProfile`](../app/src/main/kotlin/com/example/huellitasya/core/model/UserProfile.kt) |
| `users/{uid}/pets/{petId}` | `name`, `species` (`dog`/`cat`/`other`), `breed`, `birthDate` (`yyyy-MM-dd`), `sex`, `weightKg`, `photoUrl`, `createdAt`, `updatedAt` | [`Pet`](../app/src/main/kotlin/com/example/huellitasya/core/model/Pet.kt) |
| `…/pets/{petId}/records/{recordId}` | `category`, `fileName`, `contentType`, `sizeBytes`, `date`, `type`, `vet`, `fileUrl`, `createdAt` | [`MedicalRecord`](../app/src/main/kotlin/com/example/huellitasya/core/model/MedicalRecord.kt) |
| `users/{uid}/reminders/{reminderId}` | `petId`, `kind` (`vaccine`/`vet`/`other`), `subtype`, `title`, `dateTime`, `createdAt`, `updatedAt` | [`Reminder`](../app/src/main/kotlin/com/example/huellitasya/core/model/Reminder.kt) |

Los **archivos** de Storage siguen la misma idea:

```text
users/{uid}/profile.jpg                              foto de perfil
users/{uid}/pets/{petId}.jpg                         foto de la mascota
users/{uid}/pets/{petId}/records/{recordId}          archivo del registro (PDF o imagen)
```

## Leer en vivo: `callbackFlow` + `addSnapshotListener`

Las pantallas no piden los datos una vez: los **escuchan**. Si cambian (aquí o en otro teléfono), la pantalla se actualiza sola. Así lee los recordatorios [`FirestoreReminderRepository`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirestoreReminderRepository.kt):

> Los comentarios que empiezan con `// ←` son explicaciones de esta guía.

```kotlin
    private fun remindersCollection(uid: String) =
        firestore.collection(USERS).document(uid).collection(REMINDERS)          // ← la ruta users/{uid}/reminders

    override fun observeReminders(uid: String): Flow<AppResult<List<Reminder>, DataError>> = callbackFlow { // ← convierte un listener de Firebase en un Flow de Kotlin
        val registration = remindersCollection(uid).addSnapshotListener { snapshot, error -> // ← Firestore llama aquí ahora y con cada cambio
            val result = when {
                error != null -> AppResult.Failure(error.toDataError())          // ← falló (sin permiso, por ejemplo): error con nombre
                snapshot == null -> AppResult.Success(emptyList())
                else -> AppResult.Success(snapshot.documents.mapNotNull { it.toReminder() }.sortedBy { it.dateTime }) // ← cada documento → Reminder (los incompletos se descartan), ordenados por fecha
            }
            trySend(result)                                                      // ← entrega el resultado a quien escucha el Flow
        }
        awaitClose { registration.remove() }                                     // ← cuando nadie escucha (la pantalla se cerró), deja de escuchar a Firestore
    }
```

Los mismos pasos usan `observePets`, `observeRecords` y `observeProfile`.

## Convertir documentos en modelos: los *mappers*

[`FirebaseMappers.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/mapper/FirebaseMappers.kt) convierte en las dos direcciones:
- **Documento → modelo:** `toReminder()`, `toPet()`, `toMedicalRecord()`.
- **Modelo → valores para Firestore:** `wireValue()` (`ReminderKind.VACCINE` → `"vaccine"`) y `toTimestamp()` (`LocalDateTime` → `Timestamp`).
- **Excepción de Firebase → error de la app:** `toDataError()` y `toAuthError()`.

```kotlin
internal fun Throwable.toAuthError(): AuthError = when (this) {                   // ← cada excepción de Firebase se vuelve un error con nombre
    is FirebaseAuthRecentLoginRequiredException -> AuthError.RequiresRecentLogin
    is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
    is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException -> AuthError.InvalidCredentials
    is FirebaseNetworkException -> AuthError.Network
    is FirebaseTooManyRequestsException -> AuthError.TooManyRequests
    else -> AuthError.Unknown
}
```

Así, el resto de la app nunca ve una excepción de Firebase: solo errores con nombre, como `AuthError.Network`.

## Escribir

| Operación | Método de Firestore | Ejemplo en el proyecto |
|---|---|---|
| Crear con un id ya generado | `document(id).set(data).await()` | `createReminder`, `createPet` |
| Cambiar algunos campos | `document(id).update(mapOf(...)).await()` | `updateDateTime`, `updateProfile` |
| Borrar | `document(id).delete().await()` | `deleteReminder`, `deletePet` |
| Generar un id nuevo | `collection.document().id` | `newReminderId`, `newPetId` |

- `await()` espera a que Firebase termine sin bloquear la pantalla: la función es `suspend`.
- `FieldValue.serverTimestamp()` guarda **la hora del servidor**, no la del teléfono, que puede estar mal o haber sido cambiada.
- Cada escritura va dentro de `runData { … }`, que atrapa la excepción y devuelve `AppResult.Failure(e.toDataError())` en vez de cerrar la app.

## Sin conexión

- **Leer:** Firestore guarda una **caché** en el teléfono. Sin red, las pantallas muestran lo último que se descargó.
- **Escribir:** sin red, Firestore **encola** la escritura y el `await()` no termina hasta que vuelve la conexión. Por eso los ViewModels preguntan antes a [`NetworkMonitor`](../app/src/main/kotlin/com/example/huellitasya/core/domain/repository/NetworkMonitor.kt) y, si no hay internet, muestran "Sin conexión. Revisa tu internet" en lugar de quedarse esperando.
- [`ConnectivityNetworkMonitor`](../app/src/main/kotlin/com/example/huellitasya/core/data/network/ConnectivityNetworkMonitor.kt) solo cuenta como conectada una red con internet **validado**: un wifi sin salida a internet cuenta como desconectado.

## Reglas de seguridad

El teléfono puede ser manipulado, así que la seguridad real está en el servidor: las **reglas** de [`firebase/firestore.rules`](../firebase/firestore.rules) y [`firebase/storage.rules`](../firebase/storage.rules). Se revisan en cada lectura y escritura. Ejemplo de los recordatorios:

```javascript
        allow read: if isOwner();                                             // ← leer: solo el dueño (request.auth.uid == uid)
        allow create: if isOwner()
                      && isValidReminder()                                    // ← campos y valores permitidos, sin campos extra
                      && exists(/databases/$(database)/documents/users/$(uid)/pets/$(request.resource.data.petId)) // ← la mascota existe y es suya
                      && request.resource.data.createdAt == request.time      // ← la hora la pone el servidor
                      && request.resource.data.updatedAt == request.time;
        allow update: if isOwner()
                      && isValidReminder()
                      && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['dateTime', 'updatedAt']) // ← al editar, solo cambia la fecha
                      && request.resource.data.updatedAt == request.time;
        allow delete: if isOwner();                                           // ← borrar: solo el dueño
```

En Storage, las reglas limitan el tipo y el tamaño: fotos de hasta 5 MB, y PDF o imágenes de hasta 10 MB en los registros.

Las reglas se publican con la Firebase CLI: `firebase deploy --only firestore:rules`.

## Cloud Functions

Son funciones que corren en los servidores de Google ([`functions/src/index.ts`](../functions/src/index.ts), región `southamerica-east1`). La app las llama desde `core/data`.

| Función | Para qué |
|---|---|
| `requestPasswordResetCode` | Genera un código de 4 dígitos y lo envía por correo para recuperar la contraseña. |
| `verifyPasswordResetCode` | Comprueba el código y devuelve un permiso de un solo uso. |
| `resetPasswordWithToken` | Cambia la contraseña con ese permiso. |
| `requestEmailChangeCode` | Envía un código al correo nuevo cuando se cambia el correo. |
| `confirmEmailChange` | Comprueba el código, cambia el correo y avisa al anterior. |
| `notifyPasswordChanged` | Avisa por correo que la contraseña cambió. |
| `deleteAccount` | Borra la cuenta completa: archivos, documentos y usuario. |

Se usan porque Firebase Auth no envía códigos de 4 dígitos, y porque borrar una cuenta entera solo es seguro con permisos de administrador.

## Avisos de recordatorios: WorkManager

Los avisos del teléfono no dependen de Firebase: los programa la propia app con **WorkManager**, que sobrevive al cierre de la app y al reinicio del teléfono.
- [`SyncReminderNotificationsUseCase`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/ReminderUseCases.kt) escucha los recordatorios y las mascotas mientras la app está abierta.
- Con cada cambio, [`WorkManagerReminderNotifier`](../app/src/main/kotlin/com/example/huellitasya/core/data/notifications/WorkManagerReminderNotifier.kt) programa un trabajo por cada recordatorio futuro y cancela los demás.
- A la hora indicada, [`ReminderWorker`](../app/src/main/kotlin/com/example/huellitasya/core/data/notifications/ReminderWorker.kt) muestra el aviso.

El recorrido completo está en [04 — Flujo: crear un recordatorio](04-flujo-crear-recordatorio.md).
