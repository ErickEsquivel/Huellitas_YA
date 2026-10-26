package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.LocalFileInfo
import com.example.huellitasya.core.model.MedicalRecord
import com.example.huellitasya.core.model.RecordDraft
import com.example.huellitasya.core.model.UploadProgress
import kotlinx.coroutines.flow.Flow

/** Registros médicos de una mascota en `users/{uid}/pets/{petId}/records` (spec 006). */
interface RecordRepository {
    /** Todos los registros de la mascota, del más nuevo al más antiguo; sin red, desde la caché. */
    fun observeRecords(uid: String, petId: String): Flow<AppResult<List<MedicalRecord>, DataError>>

    /** Id para un registro nuevo. Se genera antes de subir el archivo, que usa la misma ruta. */
    fun newRecordId(uid: String, petId: String): String

    suspend fun createRecord(
        uid: String,
        petId: String,
        recordId: String,
        draft: RecordDraft,
        file: LocalFileInfo,
    ): AppResult<Unit, DataError>

    suspend fun deleteRecord(uid: String, petId: String, recordId: String): AppResult<Unit, DataError>
}

/** Archivos de los registros médicos en Storage: `users/{uid}/pets/{petId}/records/{recordId}`. */
interface RecordFileRepository {
    /** Nombre, tipo y tamaño del archivo elegido (`content://`); la capa de datos lo resuelve. */
    suspend fun inspect(localUri: String): AppResult<LocalFileInfo, DataError>

    /** Sube el archivo tal cual (sin comprimir, para que el documento siga legible) y emite su avance. */
    fun upload(uid: String, petId: String, recordId: String, localUri: String, contentType: String): Flow<AppResult<UploadProgress, DataError>>

    /** URL de descarga, para mostrar la vista previa y descargar el archivo. */
    suspend fun downloadUrl(uid: String, petId: String, recordId: String): AppResult<String, DataError>

    /** Borra el archivo. Si ya no existía, también es un éxito. */
    suspend fun delete(uid: String, petId: String, recordId: String): AppResult<Unit, DataError>
}
