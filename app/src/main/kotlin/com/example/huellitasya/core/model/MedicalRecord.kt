package com.example.huellitasya.core.model

import java.time.LocalDate

/** Categoría de un registro médico: cada una es una tarjeta del perfil de la mascota (spec 006). */
enum class RecordCategory { VACCINES, HISTORY, FILES }

enum class RecordType { VACCINATION, CONSULTATION, EXAM, OTHER }

/** Documento de una mascota en `users/{uid}/pets/{petId}/records/{recordId}` (spec 006). */
data class MedicalRecord(
    val id: String,
    val petId: String,
    val category: RecordCategory,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val date: LocalDate,
    val type: RecordType,
    val vet: String?,
    /** URL de descarga del archivo: miniatura, vista previa y descarga. */
    val fileUrl: String,
) {
    val isPdf: Boolean get() = contentType == PDF_CONTENT_TYPE
}

/** Datos del formulario "Subir Archivo", ya validados. */
data class RecordDraft(
    val category: RecordCategory,
    val date: LocalDate,
    val type: RecordType,
    val vet: String?,
)

/** Lo que se sabe del archivo elegido en el selector antes de subirlo. */
data class LocalFileInfo(
    val name: String,
    val contentType: String,
    val sizeBytes: Long,
)

/** Avance de una subida: [InProgress] con la fracción subida (0–1) y, al final, [Done]. */
sealed interface UploadProgress {
    data class InProgress(val fraction: Float) : UploadProgress
    data object Done : UploadProgress
}

const val PDF_CONTENT_TYPE = "application/pdf"

/** Mismo límite que `firebase/storage.rules` y `firestore.rules`. */
const val MAX_RECORD_BYTES = 10L * 1024 * 1024
