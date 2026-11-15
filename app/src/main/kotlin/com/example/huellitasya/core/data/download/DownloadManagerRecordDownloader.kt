package com.example.huellitasya.core.data.download

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.mapper.toDataError
import com.example.huellitasya.core.data.media.FirestoreMediaStore
import com.example.huellitasya.core.data.media.MediaUrls
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.BackendMode
import com.example.huellitasya.core.domain.repository.BackendModeProvider
import com.example.huellitasya.core.domain.repository.RecordDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Descarga con el `DownloadManager` del sistema, que muestra el avance y, al terminar, una
 * notificación que abre el archivo (spec 006).
 *
 * Desde Android 10 va a la carpeta pública Descargas sin permisos. Antes haría falta
 * `WRITE_EXTERNAL_STORAGE`, así que en esas versiones queda en la carpeta Download de la app.
 *
 * Los archivos guardados en Firestore (`huellitas-media://…`, spec 008) no tienen una URL que el
 * `DownloadManager` sepa leer: la app escribe los bytes directamente en esas mismas carpetas.
 */
internal class DownloadManagerRecordDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: FirestoreMediaStore,
    private val mode: BackendModeProvider,
) : RecordDownloader {

    override suspend fun download(url: String, fileName: String, contentType: String): AppResult<Unit, DataError> {
        val safeName = fileName.replace(UnsafeChars, "_")
        return when {
            MediaUrls.isMedia(url) -> saveFromFirestore(url, safeName, contentType)
            // En el plan Spark, Storage no responde: la descarga fallaría después, ya en la notificación.
            mode.current() == BackendMode.Free -> AppResult.Failure(DataError.FileUnavailable)
            else -> enqueue(url, safeName, contentType)
        }
    }

    private fun enqueue(url: String, safeName: String, contentType: String): AppResult<Unit, DataError> = try {
        val request = DownloadManager.Request(url.toUri())
            .setTitle(safeName)
            .setMimeType(contentType)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName)
        } else {
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, safeName)
        }
        context.getSystemService(DownloadManager::class.java).enqueue(request)
        AppResult.Success(Unit)
    } catch (e: IllegalArgumentException) {
        AppResult.Failure(DataError.Unknown)
    } catch (e: IllegalStateException) {
        AppResult.Failure(DataError.Unknown)
    } catch (e: SecurityException) {
        AppResult.Failure(DataError.Unknown)
    }

    private suspend fun saveFromFirestore(url: String, safeName: String, contentType: String): AppResult<Unit, DataError> =
        try {
            val ref = MediaUrls.parse(url) ?: return AppResult.Failure(DataError.FileUnavailable)
            val media = store.read(ref) ?: return AppResult.Failure(DataError.FileUnavailable)
            withContext(Dispatchers.IO) { writeToDownloads(safeName, contentType, media.bytes) }
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Failure(e.toDataError())
        }

    private fun writeToDownloads(safeName: String, contentType: String, bytes: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                put(MediaStore.Downloads.MIME_TYPE, contentType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) { "MediaStore rechazó $safeName" }
            checkNotNull(resolver.openOutputStream(uri)) { "No se pudo abrir $uri" }.use { it.write(bytes) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } else {
            val folder = checkNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)) { "Sin almacenamiento externo" }
            File(folder, safeName).writeBytes(bytes)
        }
    }

    private companion object {
        /** Caracteres que no valen en un nombre de archivo. */
        val UnsafeChars = Regex("""[\\/:*?"<>|]""")
    }
}
