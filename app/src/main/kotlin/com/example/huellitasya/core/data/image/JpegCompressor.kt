package com.example.huellitasya.core.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Prepara una foto elegida en el selector para subirla a Storage (specs 004 y 005): la reduce a
 * [MAX_SIDE] px por lado y la comprime en JPEG, respetando la orientación EXIF.
 */
internal class JpegCompressor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** @param localUri `content://` que devuelve el selector de fotos. */
    suspend fun compress(localUri: String): ByteArray = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(localUri.toUri())
        ByteArrayOutputStream().use { output ->
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

    companion object {
        const val JPEG_CONTENT_TYPE = "image/jpeg"

        /** Mismo límite que `firebase/storage.rules`. */
        const val MAX_BYTES = 5 * 1024 * 1024

        private const val MAX_SIDE = 1024
        private const val JPEG_QUALITY = 85
    }
}
