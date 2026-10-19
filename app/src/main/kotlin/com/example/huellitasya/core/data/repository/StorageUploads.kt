package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.image.JpegCompressor
import com.example.huellitasya.core.data.mapper.toDataError
import com.example.huellitasya.core.domain.error.DataError
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Comprime la foto en [localUri], la sube a [reference] y devuelve su URL de descarga
 * (fotos de perfil y de mascotas, specs 004 y 005).
 */
internal suspend fun uploadJpeg(
    reference: StorageReference,
    localUri: String,
    compressor: JpegCompressor,
): AppResult<String, DataError> =
    try {
        val bytes = compressor.compress(localUri)
        if (bytes.size > JpegCompressor.MAX_BYTES) {
            AppResult.Failure(DataError.FileTooLarge)
        } else {
            val metadata = StorageMetadata.Builder().setContentType(JpegCompressor.JPEG_CONTENT_TYPE).build()
            reference.putBytes(bytes, metadata).await()
            val downloadUrl = reference.downloadUrl.await().toString()
            // La ruta es siempre la misma: el parámetro `v` evita que Coil muestre la foto anterior en caché.
            AppResult.Success("$downloadUrl&v=${System.currentTimeMillis()}")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toDataError())
    }
