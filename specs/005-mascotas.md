# 005 — Mascotas

- **Estado:** Approved
- **Paquetes:** `feature.pets`, `feature.home`, `navigation`, `core.model`, `core.domain`, `core.data`, `core.designsystem`, `core.ui`
- **Figma:** [Huellitas YA!](https://www.figma.com/design/02Bq5fbLLxTbaAD5hZ1sxS/Huellitas-YA-) ("Momento 2"). Capturas de referencia en [`figma/005-mascotas/`](figma/005-mascotas/).
  - Presentación: `PRESENTACION` (19:80)
  - Registrar mascota: `REGISTRO MASCOTAS` (158:1359); campos vacíos `REGISTRO MASCOTAS2` (158:1230); lleno `REGISTRO MASCOTAS 3` (19:63); guardando `REGISTRO MASCOTAS 7` (3234:1022)
  - Home: sin mascotas `HOME 4` (3050:2045); una `HOME 1` (145:1126); varias `HOME 2` (19:100)
  - Perfil de la mascota: `PERRO 1` (54:30)
  - Editar mascota: `EDIT PERRO 1` (56:192); actualizando `EDIT PERRO 8` (3242:2283)
  - Eliminar mascota: diálogo `EDIT PERRO 2` (3050:1515); eliminando `EDIT PERRO 6` (3242:2009)

## Objetivo

Que el dueño registre a sus mascotas, las vea en Home, consulte el perfil de cada una y pueda editarlas o eliminarlas. Es la base de los registros médicos (vacunas, historial, archivos), que llegan en la siguiente spec.

## Historias de usuario

- Como usuario nuevo, después de iniciar sesión quiero ver la presentación y registrar a mi primera mascota.
- Como usuario, quiero ver todas mis mascotas en Home y agregar más con "+".
- Como usuario, quiero abrir el perfil de una mascota para ver sus datos.
- Como usuario, quiero corregir los datos o la foto de una mascota.
- Como usuario, quiero eliminar una mascota que ya no cuido.

## Flujo de navegación

```
[Inicia sesión / abre la app con sesión] ─► ¿tiene mascotas?
    ├─ no ─► Presentación ─"Comenzar"─► Registrar mascota ─"Guardar Mascota"─► Home
    └─ sí ─► Home
Home ─tarjeta─► Perfil de la mascota ─"Editar información"─► Editar mascota
                                                              ├─ "Actualizar información" ─► Perfil de la mascota
                                                              └─ "Eliminar mascota" ─► diálogo ─► Home
Home ─"+"─► Registrar mascota ─► Home
Configuración ─"Cerrar sesión"─► Bienvenido
```

- **Presentación sale del grafo de auth** (spec 002): sin sesión, la app abre en Bienvenido.
- El grafo principal elige su destino inicial una sola vez, al crearse: Presentación si la primera lectura de `users/{uid}/pets` está vacía, Home si no. Si la lectura falla, va a Home. Mientras no hay respuesta se sigue viendo el splash del sistema.
- Al guardar desde Presentación, Registrar reemplaza a Presentación por Home (`popUpTo` inclusivo): "atrás" desde Home sale de la app.
- Home pasa a ser la pestaña inicial y se habilita en la barra inferior. Calendario sigue inhabilitado.
- La barra inferior se oculta en Presentación, Registrar, Perfil de la mascota y Editar. Sin ella, el `Scaffold` solo reserva la barra de estado: cada pantalla aplica `navigationBarsPadding` y su fondo llega hasta abajo.

## Operaciones (CRUD)

| Operación | Pantalla | Qué hace |
|-----------|----------|----------|
| Crear | Registrar mascota | Valida, sube la foto (opcional) y crea `users/{uid}/pets/{petId}` |
| Leer | Home, Perfil de la mascota | Escuchan la colección y el documento en vivo; sin red, desde la caché |
| Actualizar | Editar mascota | Guarda solo los campos que cambiaron; si cambia la foto, la reemplaza |
| Eliminar | Editar mascota → diálogo | Borra la foto de Storage y el documento |

### Registrar mascota

- Foto circular arriba con la cámara (PNG de Figma) abajo a la derecha. Sin foto, se ve el placeholder desenfocado con el ícono de subir imagen. Tocarla abre el Photo Picker; la foto elegida se ve al instante (aún sin subir).
- Campos (tarjetas blancas de 360 dp con ícono a la izquierda y la etiqueta encima del valor): Nombre, Especie, Raza, Fecha de nacimiento, y en una fila Sexo y Peso.
  - Nombre y Raza: texto.
  - Especie: menú con "Canina", "Felina" y "Otra".
  - Fecha de nacimiento: `DatePickerDialog`, se muestra `dd / MM /yyyy` como en Figma.
  - Sexo: menú con "Masculino" y "Femenino".
  - Peso: número decimal en kg; se muestra "5 KG".
- "Guardar Mascota" se ve gris (`ButtonIncomplete`) hasta que todos los campos tienen valor; entonces se ve verde (`GreenPrimary`).
- Con campos vacíos, se resaltan y aparece "Rellene todas las casillas indicadas" sobre el botón.
- Al guardar se muestra el overlay "Guardando…" (spinner verde sobre la pantalla desenfocada) hasta que termina. Corre en `@ApplicationScope`: si el usuario sale, el guardado termina igual.
- Antes de escribir se consulta `NetworkMonitor` (como en la spec 004): sin red, Snackbar "Sin conexión. Revisa tu internet".

### Home

- "Hola, cómo estás?!" / "Veamos como están tus mascotas" y la campana (`ScreenHeader`).
- Una tarjeta verde por mascota, ordenadas por `createdAt`: foto circular, nombre, "raza · edad" y ">". Tocarla abre su perfil.
- Debajo, la tarjeta "+" (`AddPlaceholderCard`) abre Registrar mascota.
- Edad: "N días" si tiene menos de un mes, "N meses" si tiene menos de un año, y "N años" (o "1 año") desde ahí.

### Perfil de la mascota

- "<", "Perfil de <nombre>", foto, "raza · edad".
- Filas: Fecha de nacimiento ("06 de junio de 2026"), Peso ("5 kg") y Sexo.
- "Registros Medicos": tarjetas "Carné de vacunas", "Historial clínico" y "Archivos adicionales" con "0 Archivos". En esta spec no tienen acción.
- "Editar información" abre Editar mascota.
- Si la mascota se elimina (desde otro dispositivo, por ejemplo), la pantalla vuelve a Home.

### Editar mascota

- "Actualiza la información de tu mascota": mismo formulario que Registrar, con los datos guardados.
- "Actualizar información" se ve verde solo si hay cambios. Guarda con el overlay "Actualizando..." y vuelve al perfil.
- "Eliminar mascota" (rojo) abre el diálogo del cachorro (el mismo de "Eliminar cuenta", spec 004): "¿Seguro deseas eliminarlo?", "No podrás recuperar los datos luego de confirmar la eliminación." y "Eliminar Información". Al confirmar, overlay "Eliminando..." y vuelve a Home.

## Desviaciones de Figma

- **Campos vacíos en naranja** (`FieldStatus.Warning`), no en rojo al 50 %: es el mismo aviso de "campos vacíos" de auth y perfil (spec 002).
- **Especie y Sexo son menús**, no texto libre: así el dato se puede filtrar y traducir. Figma solo muestra el valor elegido.
- **La foto es opcional**: el error de campos vacíos de Figma no resalta la foto.
- **Ícono de Nombre**: persona dentro de un círculo, del estilo de los demás íconos de línea (Figma usa un avatar genérico de Material).
- Los textos de Figma "Sex"/"Pes" del frame 19:63 son un recorte del diseño: se usan "Sexo" y "Peso".

## Reglas de validación (en cliente, `core.domain`)

| Campo | Regla | Mensaje |
|-------|-------|---------|
| Todos menos la foto | Vacío | Campo resaltado + "Rellene todas las casillas indicadas" (un solo aviso) |
| Nombre | 2 a 50 caracteres (`ValidateNameUseCase`) | "El nombre debe tener al menos 2 caracteres" / "Máximo 50 caracteres" |
| Raza | 2 a 50 caracteres | "La raza debe tener al menos 2 caracteres" / "Máximo 50 caracteres" |
| Fecha de nacimiento | No futura ni anterior a 1990 | "Fecha no válida" |
| Peso | Número mayor que 0 y hasta 150 kg, con punto o coma decimal | "Ingresa un peso válido" |

## Errores

| Origen | Error | Dónde | Mensaje |
|--------|-------|-------|---------|
| Sin red (`NetworkMonitor` o `Network`) | `DataError.Network` | Snackbar | "Sin conexión. Revisa tu internet" |
| Imagen > 5 MB tras comprimir | `DataError.FileTooLarge` | Snackbar | "La imagen es demasiado grande" |
| La mascota no existe | `DataError.NotFound` | — | (vuelve a Home) |
| Otros | `DataError.Unknown` / `PermissionDenied` | Snackbar | "Ocurrió un error inesperado" |

## Contratos

### Modelos (`core.model`)

```kotlin
data class Pet(
    val id: String,
    val name: String,
    val species: PetSpecies,
    val breed: String,
    val birthDate: LocalDate,
    val sex: PetSex,
    val weightKg: Double,
    val photoUrl: String?,
)

/** Datos del formulario ya validados (crear y editar). */
data class PetDraft(name, species, breed, birthDate, sex, weightKg)

enum class PetSpecies { DOG, CAT, OTHER }
enum class PetSex { MALE, FEMALE }
```

### Firestore: `users/{uid}/pets/{petId}`

| Campo | Tipo | Notas |
|-------|------|-------|
| `name` | string | 2 a 50 caracteres |
| `species` | string | `dog`, `cat` u `other` |
| `breed` | string | 2 a 50 caracteres |
| `birthDate` | string | `yyyy-MM-dd` |
| `sex` | string | `male` o `female` |
| `weightKg` | number | > 0 y ≤ 150 |
| `photoUrl` | string? | URL de descarga de Storage |
| `createdAt` | timestamp | `serverTimestamp()`, no cambia |
| `updatedAt` | timestamp | `serverTimestamp()` |

Como cuelga de `users/{uid}`, `deleteAccount` (spec 004) ya borra las mascotas (`recursiveDelete`) y sus fotos (prefijo `users/{uid}/`).

### Reglas (`firebase/firestore.rules`)

- Solo el dueño lee y escribe sus mascotas.
- `create` y `update`: solo los campos del esquema, con los tipos y límites de la tabla; `createdAt` no cambia en `update`.
- `delete`: el dueño.

### Storage: `users/{uid}/pets/{petId}.jpg`

Mismas reglas que la foto de perfil (solo el dueño; `image/*`; < 5 MB), y además el dueño puede borrarla. La foto se comprime igual que en la spec 004 (1024 px, JPEG 85).

### Domain (`core.domain`)

- `PetRepository` (implementación `FirestorePetRepository`):
  - `observePets(uid): Flow<AppResult<List<Pet>, DataError>>`
  - `observePet(uid, petId): Flow<AppResult<Pet, DataError>>`
  - `newPetId(uid): String`
  - `createPet(uid, petId, draft, photoUrl): AppResult<Unit, DataError>`
  - `updatePet(uid, petId, draft, photoUrl): AppResult<Unit, DataError>`
  - `deletePet(uid, petId): AppResult<Unit, DataError>`
- `PetPhotoRepository` (implementación `FirebaseStoragePetPhotoRepository`): `upload(uid, petId, localUri)` y `delete(uid, petId)`.
- Use cases:
  - `ObservePetsUseCase`, `ObservePetUseCase`
  - `RegisterPetUseCase` (`@ApplicationScope`): genera el id, sube la foto si hay y crea el documento.
  - `UpdatePetUseCase` (`@ApplicationScope`): si cambió la foto la sube, y guarda.
  - `DeletePetUseCase` (`@ApplicationScope`): borra la foto (si no existe, sigue) y el documento.
  - `ValidatePetBreedUseCase`, `ValidatePetWeightUseCase`, `ValidatePetBirthDateUseCase`
- `PetAge` (lógica pura): calcula días, meses o años para "raza · edad".

### UI

- `feature.pets`: `intro/` (Presentación), `register/`, `detail/`, `edit/` y `components/PetFormField`, `PetPhoto`.
- `feature.home`: `HomeViewModel` + `HomeScreen` con la lista.
- `core.designsystem` (lo usan ≥ 2 pantallas, spec 003):
  - `AppTitle` (antes en `feature.auth.components`): lo usan Bienvenido y Presentación.
  - `ProgressOverlay(text)`: spinner de Figma + texto ("Conectando...", "Guardando...", "Actualizando...", "Eliminando...").
  - `HuellitasDatePickerDialog` (antes privado en Datos Personales).
  - `DeleteConfirmDialog` (antes `DeleteAccountDialog`): el diálogo del cachorro con el texto del botón como parámetro.
- Los strings van en `strings_pets.xml` y los recursos llevan el prefijo `img_pets_*` / `ic_pets_*`.

### Rutas

| Destino | Argumentos |
|---------|-----------|
| `PetsIntroDestination` | — |
| `RegisterPetDestination` | `fromIntro: Boolean` (al guardar, reemplaza a Presentación) |
| `PetDetailDestination` | `petId` |
| `EditPetDestination` | `petId` |

## Reparto (equipo)

Cada parte va en su rama y su PR, en orden, con un commit por tarea.

| Persona | Rama | Alcance |
|---------|------|---------|
| Erick | `feature/mascotas-registro` | Esta spec, modelo y repositorio (crear, observar), reglas, Presentación tras el login y Registrar mascota |
| Mateo | `feature/mascotas-home-perfil` | Home con la lista y Perfil de la mascota |
| Julian | `feature/mascotas-editar` | Editar y eliminar mascota, y la verificación en dispositivo |

## Cambios a otras specs

- **002 — Autenticación:** Presentación sale del grafo de auth; sin sesión la app abre en Bienvenido y "Cerrar sesión" vuelve a Bienvenido.
- **003 — Design system:** `AppTitle`, `ProgressOverlay`, `HuellitasDatePickerDialog` y `DeleteConfirmDialog` en el catálogo.
- **004 — Perfil de usuario:** Datos Personales y Configuración usan los componentes que suben al design system.

## Criterios de aceptación

> Marcar cada casilla al verificarla en dispositivo.

- [x] Sin sesión, la app abre en Bienvenido. ("Cerrar sesión" usa el mismo cambio de grafo, verificado en la spec 004.)
- [x] Una cuenta sin mascotas ve Presentación al iniciar sesión (y al reabrir la app); "Comenzar" abre Registrar mascota.
- [x] Una cuenta con mascotas entra directo a Home.
- [x] En Registrar, el botón se ve gris hasta llenar todos los campos; con campos vacíos se resaltan y aparece "Rellene todas las casillas indicadas".
- [ ] Datos inválidos (raza de 1 letra, peso 0, fecha futura) muestran su aviso y no se escribe nada.
- [x] Guardar muestra "Guardando…" y lleva a Home con la mascota nueva; "atrás" desde Home sale de la app.
- [x] La foto elegida se ve antes de guardar y queda en Storage en `users/{uid}/pets/{petId}.jpg`; sin foto, se guarda igual.
- [ ] Home muestra una tarjeta por mascota con "raza · edad", y "+" abre Registrar.
- [ ] El perfil de la mascota muestra sus datos y las tres tarjetas de registros con "0 Archivos".
- [ ] Editar guarda solo los cambios con "Actualizando..." y el perfil se actualiza en vivo.
- [ ] Eliminar pide confirmación, muestra "Eliminando..." y la mascota y su foto desaparecen.
- [ ] Sin conexión, Home muestra la caché, y guardar, editar o eliminar muestran "Sin conexión. Revisa tu internet" sin quedarse cargando.
- [ ] Las reglas impiden leer o escribir las mascotas de otro usuario y guardar campos fuera del esquema.
- [ ] Eliminar la cuenta borra también sus mascotas y sus fotos.

> Verificados en un Vivo V2436 (Erick): Bienvenido sin sesión, Presentación sin mascotas, Home directo con mascotas, el formulario (avisos, menús, fecha, peso, foto) y el guardado con y sin foto. Reglas de creación y lectura probadas por REST (8 de 8): otra cuenta no lee ni crea, y se rechazan campos extra, especie inválida, peso 0 y faltantes. Quedan para Mateo y Julian Home, perfil, editar y eliminar.

## Plan de tests

Unit tests en la JVM con los fakes de `core/testing`:

- Validadores: raza, peso, fecha de la mascota.
- `PetAge`: días, meses, años y los límites (29 días, 11 meses, 1 año).
- Use cases: registrar (con y sin foto, foto fallida), actualizar, eliminar.
- ViewModels: `RegisterPetViewModel`, `HomeViewModel`, `PetDetailViewModel`, `EditPetViewModel`. El destino inicial (Presentación o Home) en `MainNavViewModel`.

## Fuera de alcance

- Contenido de los registros médicos (vacunas, historial clínico, archivos): spec siguiente.
- Calendario y recordatorios.
- Compartir una mascota entre varias cuentas.
