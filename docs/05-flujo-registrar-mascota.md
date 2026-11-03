# 05 — Flujo: registrar una mascota

**Qué pasa:** el usuario toca "+" en Home, llena el formulario (nombre, especie, raza, fecha de nacimiento, sexo y peso), elige una foto si quiere y toca "Guardar Mascota". La app valida los datos, comprime la foto y la sube a Storage, crea la mascota en Firestore y vuelve a Home, donde aparece la tarjeta nueva.

> En los bloques de código, los comentarios que empiezan con `// ←` son explicaciones de esta guía. El resto es el código tal cual está en el proyecto.

## La cadena de llamadas

```text
Home ("+") ─► navController.navigate(RegisterPetDestination())
   │
RegisterPetScreen ─onAction(Save)─► RegisterPetViewModel.save()
   │   ├─ PetFormValidator            (valida los 6 campos)
   │   ├─ NetworkMonitor              (¿hay internet?)
   │   └─ RegisterPetUseCase          (@ApplicationScope)
   │         ├─ PetRepository.newPetId
   │         ├─ PetPhotoRepository.upload ─► JpegCompressor ─► Storage: users/{uid}/pets/{petId}.jpg
   │         └─ PetRepository.createPet   ─► Firestore: users/{uid}/pets/{petId}.set(...)
   │
   ◄─ AppResult.Success ─ RegisterPetEvent.PetSaved ─► vuelve a Home

Vuelta en vivo:
Firestore ─addSnapshotListener─► observePets (Flow) ─► HomeViewModel ─► HomeScreen (tarjeta nueva)
```

## Paso 1. Abrir la pantalla

[`HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt#L119)

```kotlin
                onAddPetClick = { navController.navigate(RegisterPetDestination()) }, // ← el "+" de Home abre Registrar mascota
```

## Paso 2. Elegir la foto

[`RegisterPetScreen.kt` (RegisterPetRoute)](../app/src/main/kotlin/com/example/huellitasya/feature/pets/register/RegisterPetScreen.kt#L39)

```kotlin
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> // ← el selector de fotos del sistema
        // `null` = el usuario cerró el selector sin elegir: no pasa nada.
        uri?.let { viewModel.onAction(RegisterPetAction.Form(PetFormAction.PhotoPicked(it.toString()))) } // ← guarda la dirección de la foto (content://…); todavía no se sube
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RegisterPetEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            RegisterPetEvent.PetSaved -> onPetSaved()                // ← al terminar de guardar, vuelve a Home
        }
    }

    RegisterPetScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        onPhotoClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) // ← abre el selector, solo imágenes
        },
    )
```

## Paso 3. El estado del formulario

El formulario se comparte entre Registrar y Editar, así que su estado vive aparte ([`PetForm.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/pets/form/PetForm.kt#L21)):

```kotlin
data class PetFormState(
    val name: String = "",
    val species: PetSpecies? = null,                                 // ← null = todavía no se eligió
    val breed: String = "",
    val birthDate: LocalDate? = null,
    val sex: PetSex? = null,
    val weight: String = "",                                         // ← texto, tal como lo escribe el usuario; se convierte al validar
    /** Foto recién elegida en el selector (`content://`), aún sin subir. */
    val newPhotoUri: String? = null,
```

Cada cambio de campo es una acción que produce un formulario nuevo:

```kotlin
fun PetFormState.reduce(action: PetFormAction): PetFormState = when (action) {
    is PetFormAction.NameChanged -> copy(name = action.value, nameFeedback = null, formFeedback = null) // ← nuevo valor y se borra el aviso anterior
    ...
    is PetFormAction.PhotoPicked -> copy(newPhotoUri = action.uri)
}
```

[`RegisterPetViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/pets/register/RegisterPetViewModel.kt#L27) envuelve ese formulario:

```kotlin
data class RegisterPetUiState(
    val form: PetFormState = PetFormState(),                         // ← los campos
    /** Overlay "Guardando..." (Figma 3234:1022). */
    val isSaving: Boolean = false,
)
...
    fun onAction(action: RegisterPetAction) {
        when (action) {
            is RegisterPetAction.Form -> _uiState.update {
                if (it.isSaving) it else it.copy(form = it.form.reduce(action.action)) // ← mientras guarda, ignora cambios
            }
            RegisterPetAction.Save -> save()                         // ← "Guardar Mascota"
        }
    }
```

## Paso 4. Guardar: validar, comprobar la red y llamar al caso de uso

[`RegisterPetViewModel.kt` (`save`)](../app/src/main/kotlin/com/example/huellitasya/feature/pets/register/RegisterPetViewModel.kt#L68)

```kotlin
    private fun save() {
        val state = _uiState.value
        if (state.isSaving) return                                   // ← evita guardar dos veces

        val draft = when (val result = validator.validate(state.form)) { // ← valida los 6 campos
            is PetFormValidator.Result.Invalid -> {
                _uiState.update { it.copy(form = result.form) }      // ← formulario con el aviso de cada campo mal
                return                                               // ← y no guarda
            }
            is PetFormValidator.Result.Valid -> result.draft         // ← todo bien: un PetDraft listo para guardar
        }

        _uiState.update { it.copy(isSaving = true) }                 // ← muestra "Guardando…"
        viewModelScope.launch {
            // Sin red, Firestore encola la escritura y no termina: mejor avisar y no intentarlo.
            if (!networkMonitor.isOnline.first()) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(RegisterPetEvent.ShowMessage(DataError.Network.toUiText()))
                return@launch
            }
            when (val result = registerPet(draft, state.form.newPhotoUri)) { // ← caso de uso, con la foto si se eligió
                is AppResult.Success -> _events.send(RegisterPetEvent.PetSaved)
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(RegisterPetEvent.ShowMessage(result.error.toUiText()))
                }
            }
        }
    }
```

El validador usa las reglas del dominio y arma el borrador ([`PetForm.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/pets/form/PetForm.kt#L85)):

```kotlin
    fun validate(form: PetFormState, today: LocalDate = LocalDate.now()): Result {
        val nameError = validateName(form.name)                      // ← de 2 a 50 caracteres
        val speciesError = if (form.species == null) ValidationError.EmptySpecies else null
        val breedError = validateBreed(form.breed)
        val birthDateError = validateBirthDate(form.birthDate, today) // ← no puede ser futura
        val sexError = if (form.sex == null) ValidationError.EmptySex else null
        val weightError = validateWeight(form.weight)                // ← número mayor que 0 y hasta 150 kg
        val errors = listOf(nameError, speciesError, breedError, birthDateError, sexError, weightError)

        if (errors.all { it == null }) {                             // ← ningún error
            return Result.Valid(
                PetDraft(
                    name = form.name.trim(),                         // ← sin espacios sobrantes
                    species = checkNotNull(form.species),
                    breed = form.breed.trim(),
                    birthDate = checkNotNull(form.birthDate),
                    sex = checkNotNull(form.sex),
                    weightKg = checkNotNull(validateWeight.parse(form.weight)), // ← "5,5" → 5.5
                ),
            )
        }
```

## Paso 5. El caso de uso: primero la foto, después el documento

[`PetUseCases.kt` (`RegisterPetUseCase`)](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/PetUseCases.kt#L72)

```kotlin
class RegisterPetUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
    private val photoRepository: PetPhotoRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,    // ← no se corta si la pantalla se cierra
) {
    suspend operator fun invoke(draft: PetDraft, photoUri: String?): AppResult<Unit, DataError> =
        appScope.async {
            val uid = authRepository.currentUser.first()?.uid        // ← el usuario con sesión
                ?: return@async AppResult.Failure(DataError.PermissionDenied)
            val petId = petRepository.newPetId(uid)                  // ← se genera el id antes, para nombrar la foto igual
            val photoUrl = if (photoUri == null) {
                null                                                 // ← sin foto, se guarda sin foto
            } else {
                when (val upload = photoRepository.upload(uid, petId, photoUri)) { // ← sube la foto a Storage
                    is AppResult.Success -> upload.data              // ← la URL para descargarla
                    is AppResult.Failure -> return@async upload      // ← si falla la foto, no se crea la mascota
                }
            }
            petRepository.createPet(uid, petId, draft, photoUrl)     // ← crea el documento en Firestore
        }.await()
}
```

## Paso 6. La foto: comprimir y subir a Storage

[`FirebaseStoragePetPhotoRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirebaseStoragePetPhotoRepository.kt)

```kotlin
    override suspend fun upload(uid: String, petId: String, localUri: String): AppResult<String, DataError> =
        uploadJpeg(photoReference(uid, petId), localUri, compressor) // ← función compartida con la foto de perfil
    ...
    private fun photoReference(uid: String, petId: String) = storage.reference.child("users/$uid/pets/$petId.jpg") // ← dónde queda en Storage
```

[`StorageUploads.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/StorageUploads.kt#L16)

```kotlin
internal suspend fun uploadJpeg(
    reference: StorageReference,
    localUri: String,
    compressor: JpegCompressor,
): AppResult<String, DataError> =
    try {
        val bytes = compressor.compress(localUri)                    // ← reduce a 1024 px y comprime en JPEG
        if (bytes.size > JpegCompressor.MAX_BYTES) {
            AppResult.Failure(DataError.FileTooLarge)                // ← más de 5 MB aun comprimida: no se sube
        } else {
            val metadata = StorageMetadata.Builder().setContentType(JpegCompressor.JPEG_CONTENT_TYPE).build() // ← tipo "image/jpeg" (las reglas lo exigen)
            reference.putBytes(bytes, metadata).await()              // ← sube los bytes y espera
            val downloadUrl = reference.downloadUrl.await().toString() // ← pide la URL pública de descarga
            // La ruta es siempre la misma: el parámetro `v` evita que Coil muestre la foto anterior en caché.
            AppResult.Success("$downloadUrl&v=${System.currentTimeMillis()}")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toDataError())                           // ← cualquier error de Firebase → DataError
    }
```

[`JpegCompressor.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/image/JpegCompressor.kt):

```kotlin
    suspend fun compress(localUri: String): ByteArray = withContext(Dispatchers.IO) { // ← trabajo pesado: se hace fuera del hilo de la pantalla
        val bitmap = decodeScaled(localUri.toUri())                  // ← lee la foto ya reducida y bien girada (EXIF)
        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output) // ← la comprime en JPEG
            bitmap.recycle()                                         // ← libera la memoria de la imagen
            output.toByteArray()
        }
    }
```

La regla de Storage ([`storage.rules`](../firebase/storage.rules#L18)) solo acepta archivos del dueño, de tipo `image/*` y de menos de 5 MB.

## Paso 7. El documento en Firestore

[`FirestorePetRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirestorePetRepository.kt#L63)

```kotlin
    override suspend fun createPet(
        uid: String,
        petId: String,
        draft: PetDraft,
        photoUrl: String?,
    ): AppResult<Unit, DataError> = runData {                        // ← atrapa errores y devuelve AppResult
        val data = draft.toFirestoreMap() + mapOf(                   // ← los campos de la mascota +
            PHOTO_URL to photoUrl,                                   // ← la URL de la foto (o null)
            CREATED_AT to FieldValue.serverTimestamp(),              // ← la hora del servidor
            UPDATED_AT to FieldValue.serverTimestamp(),
        )
        petsCollection(uid).document(petId).set(data).await()       // ← escribe users/{uid}/pets/{petId}
    }
```

```kotlin
    private fun PetDraft.toFirestoreMap(): Map<String, Any?> = mapOf(
        NAME to name,
        SPECIES to species.name.lowercase(Locale.ROOT),              // ← DOG → "dog"
        BREED to breed,
        BIRTH_DATE to birthDate.toString(),                          // ← LocalDate → "2026-06-06"
        SEX to sex.name.lowercase(Locale.ROOT),
        WEIGHT_KG to weightKg,
    )
```

Firestore revisa la regla de `pets` ([`firestore.rules`](../firebase/firestore.rules#L48)): que sea el dueño, nombre y raza de 2 a 50 caracteres, especie y sexo válidos, peso entre 0 y 150, sin campos extra, y la hora del servidor.

## Paso 8. Volver a Home, que ya muestra la mascota

[`HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt#L150)

```kotlin
            registerPetScreen(
                onBack = { navController.popBackStack() },
                onPetSaved = { fromIntro ->
                    if (fromIntro) {                                 // ← si venía de la Presentación (primera mascota)…
                        // Home reemplaza a Presentación y Registrar: "atrás" desde Home sale de la app.
                        navController.navigate(HomeDestination) {
                            popUpTo<PetsIntroDestination> { inclusive = true } // ← …quita Presentación del historial
                        }
                    } else {
                        navController.popBackStack()                 // ← si venía de Home, solo vuelve
                    }
                },
            )
```

Home escucha las mascotas en vivo con `observePets` ([`FirestorePetRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirestorePetRepository.kt#L30)). En cuanto `set` guarda el documento llega la lista nueva, y la tarjeta aparece sin recargar.

## Qué pasa si falla

| Situación | Dónde se detecta | Qué ve el usuario |
|---|---|---|
| Campos vacíos o inválidos | `PetFormValidator` | Campos en naranja y "Rellene todas las casillas indicadas" |
| Sin internet | `NetworkMonitor` | Snackbar "Sin conexión. Revisa tu internet" |
| Foto de más de 5 MB aun comprimida | `uploadJpeg` | "La imagen es demasiado grande"; la mascota no se crea |
| Firebase rechaza o falla | `runData` / `toDataError` | Snackbar con el error |

## Archivos involucrados

- [`feature/pets/register/RegisterPetScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/pets/register/RegisterPetScreen.kt)
- [`feature/pets/register/RegisterPetViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/pets/register/RegisterPetViewModel.kt)
- [`feature/pets/form/PetForm.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/pets/form/PetForm.kt)
- [`feature/pets/components/PetFormLayout.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/pets/components/PetFormLayout.kt)
- [`core/domain/validation/Validators.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/validation/Validators.kt)
- [`core/domain/usecase/PetUseCases.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/PetUseCases.kt)
- [`core/data/repository/FirebaseStoragePetPhotoRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirebaseStoragePetPhotoRepository.kt)
- [`core/data/repository/StorageUploads.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/StorageUploads.kt)
- [`core/data/image/JpegCompressor.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/image/JpegCompressor.kt)
- [`core/data/repository/FirestorePetRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirestorePetRepository.kt)
- [`navigation/HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt)
- [`firebase/firestore.rules`](../firebase/firestore.rules), [`firebase/storage.rules`](../firebase/storage.rules)
