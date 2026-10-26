package com.example.huellitasya.core.ui.pdf

import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Decoder de Coil que dibuja la primera página de un PDF (vista previa del detalle, spec 006).
 * Coil descarga el archivo y lo guarda en su caché de disco, y `PdfRenderer` lo abre desde ahí.
 */
class PdfFirstPageDecoder(private val result: SourceFetchResult) : Decoder {

    override suspend fun decode(): DecodeResult {
        val file = result.source.file().toFile()
        val bitmap = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                renderer.openPage(0).use { page ->
                    // Las medidas de la página están en puntos (1/72"): se escala hasta MAX_WIDTH_PX.
                    val scale = min(MAX_WIDTH_PX.toFloat() / page.width, MAX_SCALE)
                    createBitmap((page.width * scale).roundToInt(), (page.height * scale).roundToInt()).apply {
                        // Las páginas sin fondo serían transparentes.
                        eraseColor(Color.WHITE)
                        page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
        return DecodeResult(image = bitmap.asImage(), isSampled = true)
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder =
            PdfFirstPageDecoder(result)
    }

    private companion object {
        const val MAX_WIDTH_PX = 1080
        const val MAX_SCALE = 3f
    }
}
