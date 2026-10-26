package com.example.huellitasya.core.data.download

import android.app.DownloadManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.net.toUri
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.RecordDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Descarga con el `DownloadManager` del sistema, que muestra el avance y, al terminar, una
 * notificación que abre el archivo (spec 006).
 *
 * Desde Android 10 va a la carpeta pública Descargas sin permisos. Antes haría falta
 * `WRITE_EXTERNAL_STORAGE`, así que en esas versiones queda en la carpeta Download de la app.
 */
internal class DownloadManagerRecordDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : RecordDownloader {

    override fun download(url: String, fileName: String, contentType: String): AppResult<Unit, DataError> = try {
        val safeName = fileName.replace(UnsafeChars, "_")
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

    private companion object {
        /** Caracteres que no valen en un nombre de archivo. */
        val UnsafeChars = Regex("""[\\/:*?"<>|]""")
    }
}
