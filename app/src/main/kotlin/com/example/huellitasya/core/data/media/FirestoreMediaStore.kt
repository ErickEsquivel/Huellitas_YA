package com.example.huellitasya.core.data.media

import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Referencia a un archivo guardado en Firestore: `users/{uid}/media/{mediaId}` (spec 008). */
internal data class MediaRef(val uid: String, val mediaId: String)

/** Un archivo leído de Firestore. */
internal class MediaFile(val bytes: ByteArray, val contentType: String)

/**
 * URLs `huellitas-media://users/{uid}/media/{mediaId}` que van en `photoUrl` y `fileUrl` en lugar
 * de la URL de Storage (spec 008). El parámetro `v` cambia cuando se reemplaza una foto, para que
 * Coil no muestre la anterior desde su caché.
 */
internal object MediaUrls {
    const val SCHEME = "huellitas-media"

    private val Pattern = Regex("""^$SCHEME://users/([^/?]+)/media/([^/?]+)(\?.*)?$""")

    fun of(ref: MediaRef, version: Long? = null): String =
        "$SCHEME://users/${ref.uid}/media/${ref.mediaId}" + (version?.let { "?v=$it" } ?: "")

    fun parse(url: String): MediaRef? = Pattern.matchEntire(url)?.let { MediaRef(it.groupValues[1], it.groupValues[2]) }

    fun isMedia(url: String): Boolean = url.startsWith("$SCHEME://")

    /** IDs fijos por archivo: así subir otra foto reemplaza la anterior. */
    fun profilePhoto(uid: String) = MediaRef(uid, "profile")
    fun petPhoto(uid: String, petId: String) = MediaRef(uid, "pet-$petId")
    fun recordFile(uid: String, petId: String, recordId: String) = MediaRef(uid, "record-$petId-$recordId")
}

/** Parte [bytes] en pedazos de como máximo [size] bytes (al menos uno, aunque esté vacío). */
internal fun chunk(bytes: ByteArray, size: Int): List<ByteArray> =
    if (bytes.isEmpty()) listOf(bytes) else (bytes.indices step size).map { bytes.copyOfRange(it, minOf(it + size, bytes.size)) }

/**
 * Archivos guardados como bytes en Firestore, para el plan Spark, donde Storage no responde
 * (spec 008, "Archivos en Firestore"):
 *
 * ```
 * users/{uid}/media/{mediaId}             contentType, sizeBytes, chunkCount, createdAt
 * users/{uid}/media/{mediaId}/chunks/{n}  data (bytes, ≤ 900 KB)
 * ```
 *
 * Primero se escriben los chunks y al final los metadatos: un archivo sin metadatos está
 * incompleto y no se lee. Las reglas no permiten editar, así que reemplazar es borrar y crear.
 */
@Singleton
internal class FirestoreMediaStore @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private fun mediaDocument(ref: MediaRef) =
        firestore.collection(USERS).document(ref.uid).collection(MEDIA).document(ref.mediaId)

    suspend fun write(ref: MediaRef, bytes: ByteArray, contentType: String, onProgress: (Float) -> Unit = {}) {
        delete(ref)
        val document = mediaDocument(ref)
        val parts = chunk(bytes, CHUNK_BYTES)
        parts.forEachIndexed { index, part ->
            document.collection(CHUNKS).document(index.toString()).set(mapOf(DATA to Blob.fromBytes(part))).await()
            onProgress((index + 1f) / parts.size)
        }
        document.set(
            mapOf(
                CONTENT_TYPE to contentType,
                SIZE_BYTES to bytes.size.toLong(),
                CHUNK_COUNT to parts.size.toLong(),
                CREATED_AT to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** El archivo completo, o `null` si no existe o quedó incompleto. */
    suspend fun read(ref: MediaRef): MediaFile? {
        val document = mediaDocument(ref)
        val metadata = document.get().await()
        if (!metadata.exists()) return null
        val count = metadata.getLong(CHUNK_COUNT)?.toInt() ?: return null
        val contentType = metadata.getString(CONTENT_TYPE) ?: return null
        val parts = (0 until count).map { index ->
            document.collection(CHUNKS).document(index.toString()).get().await().getBlob(DATA)?.toBytes() ?: return null
        }
        val bytes = ByteArray(parts.sumOf { it.size })
        var offset = 0
        parts.forEach { part ->
            part.copyInto(bytes, offset)
            offset += part.size
        }
        return MediaFile(bytes, contentType)
    }

    /** Borra los metadatos y los chunks. Si no existía, no hace nada. */
    suspend fun delete(ref: MediaRef) {
        val document = mediaDocument(ref)
        // Primero los metadatos: si falla a la mitad, el archivo queda como incompleto, no como dañado.
        document.delete().await()
        val chunks = document.collection(CHUNKS).get().await().documents
        if (chunks.isNotEmpty()) {
            firestore.batch().apply { chunks.forEach { delete(it.reference) } }.commit().await()
        }
    }

    /** Todos los archivos del usuario, para eliminar la cuenta desde la app (spec 008). */
    suspend fun deleteAll(uid: String) {
        val media = firestore.collection(USERS).document(uid).collection(MEDIA).get().await().documents
        media.forEach { delete(MediaRef(uid, it.id)) }
    }

    companion object {
        /** Margen bajo el límite de 1 MiB por documento de Firestore. */
        const val CHUNK_BYTES = 900 * 1024

        private const val USERS = "users"
        private const val MEDIA = "media"
        private const val CHUNKS = "chunks"
        private const val DATA = "data"
        private const val CONTENT_TYPE = "contentType"
        private const val SIZE_BYTES = "sizeBytes"
        private const val CHUNK_COUNT = "chunkCount"
        private const val CREATED_AT = "createdAt"
    }
}
