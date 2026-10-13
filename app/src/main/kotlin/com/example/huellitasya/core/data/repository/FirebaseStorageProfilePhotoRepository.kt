package com.example.huellitasya.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.mapper.toDataError
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.ProfilePhotoRepository
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Sube la foto de perfil a `users/{uid}/profile.jpg` (spec 004, "Foto de perfil").
 * Antes la reduce a [MAX_SIDE] px por lado y la comprime en JPEG, respetando la orientación EXIF.
 */
internal class FirebaseStorageProfilePhotoRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
) : ProfilePhotoRepository {

    override suspend fun upload(uid: String, localUri: String): AppResult<String, DataError> {
        return try {
            val bytes = withContext(Dispatchers.IO) { compress(Uri.parse(localUri)) }
            if (bytes.size > MAX_BYTES) return AppResult.Failure(DataError.FileTooLarge)

            val reference = storage.reference.child("users/$uid/profile.jpg")
            val metadata = StorageMetadata.Builder().setContentType(JPEG_CONTENT_TYPE).build()
            reference.putBytes(bytes, metadata).await()
            val downloadUrl = reference.downloadUrl.await().toString()
            // La ruta es siempre la misma: el parámetro `v` evita que Coil muestre la foto anterior en caché.
            AppResult.Success("$downloadUrl&v=${System.currentTimeMillis()}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Failure(e.toDataError())
        }
    }

    private fun compress(uri: Uri): ByteArray {
        val bitmap = decodeScaled(uri)
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun decodeScaled(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder ya aplica la orientación EXIF.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val scale = MAX_SIDE.toFloat() / max(info.size.width, info.size.height)
                if (scale < 1f) {
                    decoder.setTargetSize(
                        (info.size.width * scale).roundToInt(),
                        (info.size.height * scale).roundToInt(),
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            decodeScaledLegacy(uri)
        }

    /** API < 28: `BitmapFactory` no rota según EXIF, así que se aplica a mano. */
    private fun decodeScaledLegacy(uri: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_SIDE) sampleSize *= 2
        val sampled = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: error("No se pudo leer la imagen")

        val rotation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).rotationDegrees
        } ?: 0
        val scale = minOf(1f, MAX_SIDE.toFloat() / max(sampled.width, sampled.height))
        if (rotation == 0 && scale == 1f) return sampled

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true)
            .also { if (it != sampled) sampled.recycle() }
    }

    private companion object {
        const val MAX_SIDE = 1024
        const val JPEG_QUALITY = 85
        const val JPEG_CONTENT_TYPE = "image/jpeg"

        /** Mismo límite que `firebase/storage.rules`. */
        const val MAX_BYTES = 5 * 1024 * 1024
    }
}
