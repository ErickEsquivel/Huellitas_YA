# 07 — Flujo: subir un registro médico

**Qué pasa:** en una categoría de registros (por ejemplo, Carné de vacunas) el usuario toca "+" y elige un PDF o una imagen. Después completa la fecha, el tipo y el veterinario, y toca "Subir Archivo". La app revisa el archivo y los datos, lo sube a Storage mostrando una **barra de avance real**, pide su enlace de descarga y crea el registro en Firestore. Al terminar vuelve a la lista, donde ya aparece.

> En los bloques de código, los comentarios que empiezan con `// ←` son explicaciones de esta guía. El resto es el código tal cual está en el proyecto.

## La cadena de llamadas

```text
Lista ("+") ─► navController.navigate(UploadRecordDestination(petId, category))
   │
UploadRecordRoute ─ selector del sistema ─► onAction(FilePicked(uri))
   │   └─ UploadRecordViewModel.pickFile()
   │         ├─ InspectRecordFileUseCase ─► RecordFileRepository.inspect (nombre, tipo, tamaño)
   │         └─ ValidateRecordFileUseCase (¿PDF o imagen? ¿menos de 10 MB?)
   │
UploadRecordScreen ─onAction(Upload)─► UploadRecordViewModel.upload()
   │   ├─ ValidateRecordFormUseCase   (fecha, veterinario)
   │   ├─ NetworkMonitor
   │   └─ UploadRecordUseCase         (@ApplicationScope)
   │         ├─ RecordFileRepository.upload ─► Storage putFile (Flow con el avance) ─► barra 0–100 %
   │         ├─ RecordFileRepository.downloadUrl
   │         └─ RecordRepository.createRecord ─► Firestore: users/{uid}/pets/{petId}/records/{id}.set(...)
   │
   ◄─ AppResult.Success ─ UploadRecordEvent.Uploaded ─► vuelve a la lista
```

## Paso 1. Elegir el archivo

[`UploadRecordScreen.kt` (UploadRecordRoute)](../app/src/main/kotlin/com/example/huellitasya/feature/records/upload/UploadRecordScreen.kt#L100)

```kotlin
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> // ← el selector de archivos del sistema
        // `null` = el usuario cerró el selector sin elegir: no pasa nada.
        uri?.let { viewModel.onAction(UploadRecordAction.FilePicked(it.toString())) } // ← la dirección del archivo (content://…) va al ViewModel
    }
    ...
        onPickFile = { filePicker.launch(PickableTypes) },          // ← abre el selector
```

```kotlin
private val PickableTypes = arrayOf(PDF_CONTENT_TYPE, "image/*")    // ← el selector solo muestra PDF e imágenes
```

## Paso 2. Revisar el archivo elegido

[`UploadRecordViewModel.kt` (`pickFile`)](../app/src/main/kotlin/com/example/huellitasya/feature/records/upload/UploadRecordViewModel.kt#L109)

```kotlin
    private fun pickFile(uri: String) {
        viewModelScope.launch {
            when (val result = inspectFile(uri)) {                   // ← pregunta al sistema nombre, tipo y tamaño
                is AppResult.Success -> {
                    val error = validateFile(result.data)            // ← ¿PDF o imagen? ¿menos de 10 MB?
                    if (error != null) {
                        showMessage(error.toUiText())                // ← "El archivo es demasiado grande (máx. 10 MB)"
                    } else {
                        _uiState.update { it.copy(localUri = uri, file = result.data) } // ← lo muestra en la tarjeta y aparece el formulario
                    }
                }
                is AppResult.Failure -> showMessage(result.error.toUiText())
            }
        }
    }
```

El repositorio consulta al sistema los datos del archivo, sin abrirlo entero ([`FirebaseStorageRecordFileRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirebaseStorageRecordFileRepository.kt#L34)):

```kotlin
    override suspend fun inspect(localUri: String): AppResult<LocalFileInfo, DataError> = withContext(Dispatchers.IO) { // ← trabajo de disco: fuera del hilo de la pantalla
        try {
            val uri = localUri.toUri()
            val resolver = context.contentResolver                   // ← el "gestor de archivos" de Android
            var name: String? = null
            var size = -1L
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor -> // ← pide solo el nombre y el tamaño
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0)
                    if (!cursor.isNull(1)) size = cursor.getLong(1)
                }
            }
            val contentType = resolver.getType(uri)                  // ← "application/pdf", "image/jpeg"…
```

La regla del archivo está en el dominio ([`Validators.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/validation/Validators.kt#L128)):

```kotlin
class ValidateRecordFileUseCase @Inject constructor() {
    operator fun invoke(file: LocalFileInfo): ValidationError? = when {
        file.contentType != PDF_CONTENT_TYPE && !file.contentType.startsWith("image/") -> ValidationError.UnsupportedFileType // ← ni PDF ni imagen
        file.sizeBytes <= 0 || file.sizeBytes >= MAX_RECORD_BYTES -> ValidationError.FileTooBig // ← 10 MB o más (o vacío)
        else -> null
    }
}
```

## Paso 3. El estado: el progreso también es estado

[`UploadRecordViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/records/upload/UploadRecordViewModel.kt#L36)

```kotlin
data class UploadRecordUiState(
    val pet: Pet? = null,                                            // ← para la cabecera con la foto
    /** Archivo elegido (`content://`) y lo que se sabe de él; `null` hasta elegir uno. */
    val localUri: String? = null,
    val file: LocalFileInfo? = null,
    val date: LocalDate = LocalDate.now(),                           // ← hoy por defecto
    val type: RecordType = RecordType.OTHER,
    val vet: String = "",
    val dateFeedback: FieldFeedback? = null,
    val vetFeedback: FieldFeedback? = null,
    /** Avance de la subida (0–1); `null` cuando no se está subiendo. */
    val progress: Float? = null,                                     // ← la barra lee este número
) {
    val isUploading: Boolean get() = progress != null                // ← derivado: hay barra = se está subiendo
}
```

Los argumentos llegan por la navegación, y el tipo por defecto depende de la categoría:

```kotlin
    private val petId: String = checkNotNull(savedStateHandle[PET_ID_ARG]) { "Falta la mascota" }
    val category: RecordCategory = RecordCategory.valueOf(checkNotNull(savedStateHandle[CATEGORY_ARG]) { "Falta la categoría" })

    private val _uiState = MutableStateFlow(UploadRecordUiState(type = defaultType(category))) // ← en el Carné viene "Vacunación"
```

## Paso 4. Subir: validar, comprobar la red y llamar al caso de uso

[`UploadRecordViewModel.kt` (`upload`)](../app/src/main/kotlin/com/example/huellitasya/feature/records/upload/UploadRecordViewModel.kt#L125)

```kotlin
    private fun upload() {
        val state = _uiState.value
        val localUri = state.localUri ?: return                      // ← sin archivo no hay nada que subir
        val file = state.file ?: return

        val dateError = validateForm.date(state.date)                // ← no puede ser futura
        val vetError = validateForm.vet(state.vet)                   // ← hasta 50 caracteres
        if (dateError != null || vetError != null) {
            _uiState.update { it.copy(dateFeedback = dateError?.toFieldFeedback(), vetFeedback = vetError?.toFieldFeedback()) }
            return
        }

        _uiState.update { it.copy(progress = 0f) }                   // ← aparece "Subiendo archivo…" con la barra en 0
        viewModelScope.launch {
            // Sin red, Storage reintenta sin fin: mejor avisar y no intentarlo.
            if (!networkMonitor.isOnline.first()) {
                _uiState.update { it.copy(progress = null) }         // ← quita la barra
                showMessage(DataError.Network.toUiText())
                return@launch
            }
            val draft = RecordDraft(category, state.date, state.type, state.vet.trim().ifEmpty { null }) // ← veterinario vacío = null
            val result = uploadRecord(petId, draft, localUri, file) { fraction -> // ← la lambda recibe el avance…
                _uiState.update { it.copy(progress = fraction) }     // ← …y lo guarda en el estado: la barra se mueve
            }
            when (result) {
                is AppResult.Success -> _events.send(UploadRecordEvent.Uploaded) // ← listo: vuelve a la lista
                is AppResult.Failure -> {
                    _uiState.update { it.copy(progress = null) }
                    showMessage(result.error.toUiText())
                }
            }
        }
    }
```

## Paso 5. El caso de uso: archivo, enlace y documento, en orden

[`RecordUseCases.kt` (`UploadRecordUseCase`)](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/RecordUseCases.kt#L70)

```kotlin
    ): AppResult<Unit, DataError> = appScope.async {                 // ← si el usuario sale de la pantalla, la subida termina igual
        val uid = authRepository.currentUser.first()?.uid
            ?: return@async AppResult.Failure(DataError.PermissionDenied)
        val recordId = recordRepository.newRecordId(uid, petId)      // ← un id que usan a la vez el archivo y el documento

        var uploadError: DataError? = null
        fileRepository.upload(uid, petId, recordId, localUri, file.contentType).collect { event -> // ← escucha el Flow de la subida hasta que termina
            when (event) {
                is AppResult.Success -> (event.data as? UploadProgress.InProgress)?.let { onProgress(it.fraction) } // ← avance: se lo pasa al ViewModel
                is AppResult.Failure -> uploadError = event.error    // ← la subida falló
            }
        }
        uploadError?.let { return@async AppResult.Failure(it) }      // ← si falló, no se crea nada
        onProgress(1f)                                               // ← barra al 100 %

        val fileUrl = when (val url = fileRepository.downloadUrl(uid, petId, recordId)) { // ← pide el enlace de descarga
            is AppResult.Success -> url.data
            is AppResult.Failure -> {
                fileRepository.delete(uid, petId, recordId)          // ← no se pudo: borra el archivo para no dejarlo suelto
                return@async url
            }
        }
        recordRepository.createRecord(uid, petId, recordId, draft, file, fileUrl).also { result -> // ← crea el documento con el enlace
            if (result is AppResult.Failure) fileRepository.delete(uid, petId, recordId) // ← si el documento falla, también borra el archivo
        }
    }.await()
```

**Por qué en este orden:** el documento guarda el enlace del archivo, así que el archivo tiene que existir antes. Si algo falla después de subirlo, se borra, para que no queden archivos sin registro que ocupen espacio.

## Paso 6. Subir a Storage informando el avance

[`FirebaseStorageRecordFileRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirebaseStorageRecordFileRepository.kt#L59)

```kotlin
    override fun upload(
        uid: String,
        petId: String,
        recordId: String,
        localUri: String,
        contentType: String,
    ): Flow<AppResult<UploadProgress, DataError>> = callbackFlow {   // ← un Flow que emite el avance mientras sube
        val metadata = StorageMetadata.Builder().setContentType(contentType).build() // ← el tipo (las reglas lo revisan)
        // `putFile` lee el `content://` y conoce el tamaño total, así el avance es real.
        val task = fileReference(uid, petId, recordId).putFile(localUri.toUri(), metadata) // ← empieza la subida a users/{uid}/pets/{petId}/records/{recordId}
        task.addOnProgressListener { snapshot ->                     // ← Storage avisa cada vez que sube un trozo
            if (snapshot.totalByteCount > 0) {
                trySend(AppResult.Success(UploadProgress.InProgress(snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount))) // ← bytes subidos / total = 0.0 a 1.0
            }
        }.addOnSuccessListener {
            trySend(AppResult.Success(UploadProgress.Done))          // ← terminó
            close()                                                  // ← cierra el Flow: el collect del caso de uso termina
        }.addOnFailureListener { error ->
            trySend(AppResult.Failure(error.toDataError()))          // ← falló
            close()
        }
        awaitClose { if (task.isInProgress) task.cancel() }          // ← si nadie escucha, cancela la subida
    }
```

```kotlin
    override suspend fun downloadUrl(uid: String, petId: String, recordId: String): AppResult<String, DataError> =
        try {
            AppResult.Success(fileReference(uid, petId, recordId).downloadUrl.await().toString()) // ← el enlace para ver o descargar el archivo
```

La regla de Storage ([`storage.rules`](../firebase/storage.rules#L29)) solo acepta archivos del dueño, PDF o `image/*`, de menos de 10 MB. La revisión es doble: en la app (paso 2) y en el servidor.

La barra se mueve con una animación suave ([`UploadRecordScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/records/upload/UploadRecordScreen.kt#L331)):

```kotlin
private fun UploadingOverlay(progress: Float) {
    val animated by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "uploadProgress") // ← pasa de un valor al siguiente sin saltos
```

## Paso 7. El documento en Firestore

[`FirestoreRecordRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirestoreRecordRepository.kt#L51)

```kotlin
    ): AppResult<Unit, DataError> = runData {
        val data = mapOf(
            CATEGORY to draft.category.name.lowercase(Locale.ROOT),  // ← "vaccines", "history" o "files"
            FILE_NAME to file.name,
            CONTENT_TYPE to file.contentType,
            SIZE_BYTES to file.sizeBytes,
            DATE to draft.date.toString(),                           // ← "2026-09-10"
            TYPE to draft.type.name.lowercase(Locale.ROOT),          // ← "vaccination", "consultation"…
            VET to draft.vet,                                        // ← puede ser null
            FILE_URL to fileUrl,                                     // ← el enlace del paso 6
            CREATED_AT to FieldValue.serverTimestamp(),
        )
        recordsCollection(uid, petId).document(recordId).set(data).await() // ← escribe users/{uid}/pets/{petId}/records/{recordId}
    }
```

La regla de `records` ([`firestore.rules`](../firebase/firestore.rules#L82)) valida cada campo. Además, los registros **no se pueden editar**: solo crear y borrar.

## Paso 8. Volver a la lista

La Route recibe `UploadRecordEvent.Uploaded` y llama a `onUploaded()`, que hace `navController.popBackStack()` ([`HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt)). La lista ya escuchaba `observeRecords` en vivo, así que el registro nuevo aparece solo, igual que en los otros flujos.

## Qué pasa si falla

| Situación | Dónde se detecta | Qué ve el usuario |
|---|---|---|
| Archivo de otro tipo | Selector (solo PDF e imágenes) y `ValidateRecordFileUseCase` | No se puede elegir, o "Elige un PDF o una imagen" |
| 10 MB o más | `ValidateRecordFileUseCase` | "El archivo es demasiado grande (máx. 10 MB)" |
| Fecha futura o veterinario muy largo | `ValidateRecordFormUseCase` | Aviso bajo el campo |
| Sin internet | `NetworkMonitor` | Snackbar "Sin conexión. Revisa tu internet" |
| Falla la subida, el enlace o el documento | `UploadRecordUseCase` | Snackbar con el error; no queda nada a medias |

## Archivos involucrados

- [`feature/records/upload/UploadRecordScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/records/upload/UploadRecordScreen.kt)
- [`feature/records/upload/UploadRecordViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/records/upload/UploadRecordViewModel.kt)
- [`feature/records/navigation/RecordsNavigation.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/records/navigation/RecordsNavigation.kt)
- [`core/domain/validation/Validators.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/validation/Validators.kt)
- [`core/domain/usecase/RecordUseCases.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/RecordUseCases.kt)
- [`core/domain/repository/RecordRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/repository/RecordRepository.kt)
- [`core/data/repository/FirebaseStorageRecordFileRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirebaseStorageRecordFileRepository.kt)
- [`core/data/repository/FirestoreRecordRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirestoreRecordRepository.kt)
- [`core/model/MedicalRecord.kt`](../app/src/main/kotlin/com/example/huellitasya/core/model/MedicalRecord.kt)
- [`navigation/HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt)
- [`firebase/firestore.rules`](../firebase/firestore.rules), [`firebase/storage.rules`](../firebase/storage.rules)
