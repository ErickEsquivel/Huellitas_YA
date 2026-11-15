# 008 — Modo sin facturación

- **Estado:** Approved
  - Implementado: el interruptor y el modo gratuito de la cuenta (recuperar contraseña, cambio de correo, Seguridad y eliminar cuenta).
  - Pendiente: el modo gratuito de los archivos y el script de migración.
- **Paquetes:** `core.domain`, `core.data`, `feature.auth`, `feature.profile`, `feature.settings`, `feature.pets`, `feature.records`; `firebase/firestore.rules`
- **Figma:** sin frames nuevos. Las pantallas nuevas reutilizan los componentes de la spec 003.
- **Relacionadas:** 001 (Firebase setup), 002 (auth), 004 (perfil), 005 (mascotas), 006 (registros médicos)

## Objetivo

Que la app siga funcionando completa con el proyecto `huellitasya-3fd78` en el plan gratuito **Spark**, sin una tarjeta vinculada, y que vuelva al backend actual (Cloud Functions + Storage) con solo cambiar un interruptor cuando alguien reactive el plan **Blaze**.

Hoy el plan Blaze lo paga un integrante como favor, y no hay quien lo reemplace. Las Cloud Functions no se borran: quedan en `functions/` para mostrarlas o volver a usarlas.

## Qué deja de funcionar sin Blaze

| Pieza | Requiere Blaze | Qué pasa en Spark |
|---|---|---|
| Authentication (correo y Google) | No | Sigue igual. |
| Firestore | No | Sigue igual, con la cuota gratuita diaria (≈ 50 000 lecturas, 20 000 escrituras, 1 GiB guardado). |
| Remote Config | No | Disponible. Se usa para el interruptor. |
| Cloud Functions (7 callables) | **Sí** | Dejan de ejecutarse y no se pueden desplegar. |
| Secret Manager (`SMTP_USER`, `SMTP_PASSWORD`, `RESET_CODE_PEPPER`) | **Sí** | Inaccesible. |
| Cloud Storage | **Sí** | La API responde 402/403 y la consola no deja ver el bucket ([FAQ de Firebase](https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024)). Los archivos que ya existen **no se pueden leer** mientras el proyecto siga en Spark. |

## Historias de usuario

- Como usuario, quiero recuperar mi contraseña, cambiar mi correo, borrar mi cuenta y subir fotos y archivos aunque el proyecto no tenga facturación.
- Como integrante del equipo, quiero activar otra vez las Cloud Functions y Storage sin publicar una versión nueva de la app, para mostrar el backend cuando haya facturación.

## El interruptor

- **Parámetro de Remote Config:** `blaze_activo` (booleano).
  - `true`: **modo Blaze**, que es el comportamiento actual de las specs 002, 004, 005 y 006.
  - `false`: **modo gratuito**, descrito en esta spec.
- **Valor por defecto en la app:** `false`. Si Remote Config no responde (sin red, primera apertura, error), la app usa el modo gratuito, que funciona en los dos planes.
- **Lectura:**
  - `fetchAndActivate()` al iniciar la app, con `minimumFetchInterval` de 1 h en release y 0 en debug.
  - El valor activo se lee al empezar cada operación. Un cambio en la consola se aplica, como tarde, en la siguiente apertura de la app después de una hora.
- **Capas:**
  - El dominio expone `BackendModeProvider { fun current(): BackendMode }`, con `BackendMode.Blaze` o `BackendMode.Free`, y su fake en `core/testing`.
  - Solo `core.data` conoce Remote Config (`RemoteConfigBackendModeProvider`). Firebase sigue usándose solo en `core.data` (spec 000).
- **Quién lo cambia:** un integrante con acceso al proyecto, en la consola de Firebase → Remote Config → `blaze_activo` → Publicar.

## Cómo se elige la implementación

Cada repositorio afectado conserva su implementación actual y suma una gratuita. Una tercera implementación, que es la que se registra en Hilt, delega en una u otra según `BackendModeProvider`.

| Interfaz (dominio) | Modo Blaze (sin cambios) | Modo gratuito (nueva) |
|---|---|---|
| `PasswordResetRepository` | `FunctionsPasswordResetRepository` | `AuthLinkPasswordResetRepository` |
| `EmailChangeRepository` | `FunctionsEmailChangeRepository` | `AuthLinkEmailChangeRepository` |
| `AccountRepository` (eliminar, aviso de contraseña) | `FunctionsAccountRepository` | `ClientAccountRepository` |
| `ProfilePhotoRepository` | `FirebaseStorageProfilePhotoRepository` | `FirestoreProfilePhotoRepository` |
| `PetPhotoRepository` | `FirebaseStoragePetPhotoRepository` | `FirestorePetPhotoRepository` |
| `RecordFileRepository` | `FirebaseStorageRecordFileRepository` | `FirestoreRecordFileRepository` |

Las pantallas que cambian según el modo (recuperar contraseña y cambio de correo) consultan `BackendModeProvider` a través de un use case. Ninguna feature importa `core.data`.

## Modo gratuito: cuenta

### Recuperar contraseña con enlace

```
Inicio de sesión ─ "¿Se te olvidó la contraseña?" ─► ¿Olvidaste tu contraseña? ─ "Enviar enlace" ─►
Revisa tu correo ─ "Volver a iniciar sesión" ─► Inicio de sesión
```

- "Revisa tu correo" no es un destino nuevo: es la misma pantalla de "¿Olvidaste tu contraseña?" con otro título y otro texto, sin el campo de correo. Atrás vuelve a Inicio de sesión, igual que "Volver a iniciar sesión".

- "Enviar enlace" llama a `sendPasswordResetEmail(email)`, con `setLanguageCode("es")` como ya hace la app.
- **Revisa tu correo:** "Si hay una cuenta con <correo>, te enviamos un enlace para crear una contraseña nueva. Revisa también la carpeta de spam." Tiene un botón "Reenviar enlace", que se habilita a los 60 s con cuenta regresiva, igual que en la spec 002.
- **Privacidad:** igual que hoy, la app avanza aunque el correo no tenga cuenta. Para eso la **protección contra enumeración de correos** de Authentication debe estar activa, como lo está por defecto en los proyectos nuevos.
- El enlace abre la página de Firebase para crear la contraseña. **Esa página no aplica la checklist de la app** (8 caracteres, mayúscula, número o símbolo). Para aplicarla, hay que configurar la política de contraseñas en Authentication → Configuración, si el plan Spark lo permite. Se comprueba al implementar.
- No se usan `verifyPasswordResetCode` ni `resetPasswordWithToken`, ni la colección `passwordResetCodes`.

### Cambiar el correo con enlace

- Al guardar un correo distinto en Datos Personales se llama a `verifyBeforeUpdateEmail(nuevo)`.
  - Si Firebase responde `requires-recent-login`, se reautentica con el diálogo de la spec 004 y se reintenta.
  - Si el correo ya es de otra cuenta, se marca en rojo bajo el campo, igual que hoy.
- En lugar de "Verifica tu correo" (código) se muestra un Snackbar: "Te enviamos un enlace a <correo>. El cambio se aplica cuando lo abras." Datos Personales vuelve a modo lectura con el correo anterior.
- Al abrir el enlace, Firebase cambia el correo en Auth y envía su propio aviso al correo anterior, con un enlace para revertir. Ese aviso reemplaza al de `confirmEmailChange`.
- **Sincronización con Firestore:** cambiar el correo cierra las sesiones del usuario. En el siguiente inicio de sesión, el use case de inicio (`createIfMissing`) copia `auth.email` a `users/{uid}.email` si son distintos.

### Cambiar la contraseña

- Seguridad funciona igual: `reauthenticate` + `updatePassword` ya corren en el cliente.
- No se llama a `notifyPasswordChanged`. El Snackbar dice "Contraseña actualizada", sin "Te enviamos un aviso a tu correo".

### Eliminar la cuenta desde el cliente

Mismo diálogo y misma reautenticación de la spec 004. Después, `ClientAccountRepository`, en `@ApplicationScope`:

1. borra lo que el usuario tenga en Firestore: cada recordatorio, cada mascota con sus registros y los archivos de esos registros (ver [Archivos en Firestore](#archivos-en-firestore)), la foto de perfil y `users/{uid}`;
2. llama a `currentUser.delete()`;
3. cierra la sesión local.

- Es idempotente, igual que `deleteAccount`: al reintentar se salta lo que ya no existe.
- Si el paso 2 falla, los datos ya se borraron pero la cuenta de Auth sigue existiendo. Se muestra el error, y al volver a entrar la cuenta empieza vacía, como una cuenta nueva.
- Requiere cambiar las reglas: hoy `users/{uid}` tiene `allow delete: if false`. Pasa a `allow delete: if isOwner()`.
- Los archivos de Storage del usuario no se pueden borrar desde Spark. Quedan huérfanos hasta que se reactive Blaze (ver [Volver a Blaze](#volver-a-blaze)).

## Modo gratuito: archivos

### Archivos en Firestore

Los archivos van como bytes (tipo `Blob` de Firestore) en documentos de hasta **900 KB**. Así quedan bajo el límite de 1 MiB por documento, con margen para los demás campos.

```
users/{uid}/media/{mediaId}                  ← metadatos
users/{uid}/media/{mediaId}/chunks/{n}       ← bytes, n = 0, 1, 2…
```

| Campo (`media/{mediaId}`) | Tipo | Notas |
|---|---|---|
| `contentType` | string | `image/jpeg`, `application/pdf` o `image/*` |
| `sizeBytes` | number | > 0 y < 10 MB, como en la spec 006 |
| `chunkCount` | number | 1 a 12 |
| `createdAt` | timestamp | `serverTimestamp()` |

| Campo (`chunks/{n}`) | Tipo | Notas |
|---|---|---|
| `data` | bytes | ≤ 900 KB |

- **Escritura:** primero los chunks y al final el documento de metadatos. Un archivo sin metadatos está incompleto: no se muestra y se borra en el siguiente intento.
- **Avance de subida:** se calcula por chunks escritos (`UploadProgress.InProgress`).
- **URL:** en lugar de la URL de descarga de Storage, `photoUrl` y `fileUrl` guardan `huellitas-media://users/{uid}/media/{mediaId}`. Los modelos y las pantallas no cambian.
- **IDs:**
  - foto de perfil: `mediaId = "profile"`;
  - foto de mascota: `pet-{petId}`;
  - archivo de registro: `record-{petId}-{recordId}`.
- **Costo:** un PDF de 10 MB ocupa 12 escrituras y 12 lecturas cada vez que se abre. La caché de Coil evita volver a leerlo. Con 1 GiB gratis, alcanzan unas cien fotos y algunas decenas de PDF por cuenta de prueba.

### Fotos

- Perfil y mascotas se comprimen igual que hoy: JPEG de máximo 1024 px por lado. Casi siempre caben en un solo chunk.

### Registros médicos

- Mismos tipos y tamaño que la spec 006 (PDF o imagen, < 10 MB), sin comprimir.
- **Ver y descargar:**
  - Coil carga las URLs `huellitas-media://` con un `Fetcher` de `core.data` que arma el archivo con los chunks. La miniatura del PDF sigue usando `PdfFirstPageDecoder`.
  - El `DownloadManager` del sistema no sabe leer esa URL. Para esos archivos, `RecordDownloader` escribe los bytes en Descargas con `MediaStore` y muestra la misma notificación de descarga completa.

### Archivos que ya están en Storage

- En modo gratuito, las URLs de Storage que ya existen no cargan, porque Storage responde 402/403. Las fotos muestran su placeholder. El detalle de un registro muestra "Este archivo no está disponible por ahora", en lugar de la vista previa y el botón de descargar.
- **Recomendado, antes de quitar la facturación:** correr una sola vez el script `functions/scripts/migrar-storage-a-firestore.ts`. Corre en el computador de quien tenga acceso al proyecto, con credenciales de administrador, y no se despliega. El script:
  - copia cada archivo de Storage a `media`;
  - reemplaza su `photoUrl` o `fileUrl`;
  - es idempotente y no borra nada de Storage.

## Reglas de Firestore

- `users/{uid}`: `allow delete: if isOwner()`.
- `users/{uid}/media/{mediaId}`:
  - leer, crear y borrar: solo el dueño;
  - sin `update`;
  - se validan `contentType` y `sizeBytes` como en `isValidRecord()`, y `chunkCount` de 1 a 12.
- `users/{uid}/media/{mediaId}/chunks/{n}`:
  - leer, crear y borrar: solo el dueño;
  - `request.resource.data.data is bytes` y `request.resource.data.data.size() <= 900 * 1024`.
- Publicar reglas es gratis en Spark: `firebase deploy --only firestore:rules`.

## Volver a Blaze

Pasos, en orden, para quien reactive la facturación:

1. Consola de Google Cloud → Facturación → vincular `huellitasya-3fd78` a una cuenta de facturación.
2. Volver a guardar los secretos, por si se perdieron:

   ```bash
   firebase functions:secrets:set SMTP_USER
   firebase functions:secrets:set SMTP_PASSWORD
   firebase functions:secrets:set RESET_CODE_PEPPER
   ```

3. Desplegar:

   ```bash
   firebase deploy --only functions,firestore:rules,storage
   ```

4. Remote Config → `blaze_activo = true` → Publicar.
5. Comprobar en un teléfono, después de reabrir la app pasada una hora (o con una build debug): recuperar la contraseña con código y subir una foto.

- **Datos de los dos modos:** lo que se guardó en `media` durante el modo gratuito se sigue mostrando, porque la URL `huellitas-media://` indica de dónde leer. Lo nuevo vuelve a ir a Storage.
- **Eliminar la cuenta en modo Blaze:** `deleteAccount` borra también `users/{uid}/media` (por `recursiveDelete`) y los archivos huérfanos de Storage de cuentas borradas en modo gratuito no se tocan. Limpiarlos queda fuera de alcance.

## Criterios de aceptación

**Interruptor**
- [ ] Con `blaze_activo = false`, o sin poder leer Remote Config, la app usa el modo gratuito.
- [ ] Con `blaze_activo = true` y Blaze activo, la app se comporta como hoy (specs 002, 004, 005 y 006).
- [ ] Cambiar el parámetro en la consola cambia el modo sin publicar una versión nueva.

**Modo gratuito, cuenta**
- [ ] "¿Olvidaste tu contraseña?" envía un enlace, muestra "Revisa tu correo" y avanza igual aunque el correo no tenga cuenta.
- [ ] El enlace permite crear una contraseña nueva e iniciar sesión con ella.
- [ ] Cambiar el correo envía un enlace al correo nuevo. Al abrirlo cambia el correo de Auth, y en el siguiente inicio de sesión también `users/{uid}.email`.
- [ ] Cambiar la contraseña en Seguridad funciona y no promete un aviso por correo.
- [ ] Eliminar la cuenta borra mascotas, registros, recordatorios, archivos y el usuario de Auth, y vuelve a Bienvenido.

**Modo gratuito, archivos**
- [ ] La foto de perfil y la de una mascota se suben, se ven y se reemplazan.
- [ ] Un PDF de más de 1 MB y una imagen se suben con avance, se ven en el detalle y se descargan a Descargas.
- [ ] Eliminar un registro o una mascota borra sus documentos de `media` y sus chunks.
- [ ] Un archivo que sigue en Storage muestra el placeholder o "no disponible por ahora", sin cerrar la app.

**General**
- [ ] Ninguna feature importa `core.data`. Remote Config solo aparece en `core.data`.
- [ ] Los tests de ViewModel y use cases cubren los dos modos con `FakeBackendModeProvider`.
- [ ] `functions/` no cambia y `npm --prefix functions test` sigue pasando.
- [ ] El README y la spec 001 explican el interruptor y la sección [Volver a Blaze](#volver-a-blaze).

## Fuera de alcance

- Un backend o servidor propio. Esta spec lo deja más fácil, porque cada pieza ya está detrás de una interfaz.
- Proveedores de almacenamiento externos (Supabase, Cloudinary): se descartan para no sumar otra cuenta y otras credenciales.
- Detectar Blaze automáticamente. El modo lo decide solo el interruptor.
- Limpiar los archivos huérfanos de Storage.
- App Check (sigue pendiente desde la spec 002).

## Preguntas abiertas

1. ¿Spark permite configurar la política de contraseñas de Authentication? Si no, la página del enlace acepta contraseñas más débiles que la checklist de la app.
2. ¿Alguien con acceso al proyecto puede correr el script de migración antes de quitar la facturación? Si no, las fotos y archivos subidos hasta ahora quedan sin mostrarse mientras el proyecto siga en Spark.
