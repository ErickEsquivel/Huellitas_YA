package com.example.huellitasya.core.data.media

import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Lee de Firestore los archivos con URL `huellitas-media://…` (spec 008) y los deja en la caché de
 * la app, así Coil no vuelve a leer los chunks cada vez. Las fotos de Storage siguen pasando por
 * el cargador de red de Coil.
 *
 * El resultado es un archivo en disco, que es lo que necesita `PdfFirstPageDecoder`.
 */
internal class MediaFetcher(
    private val url: String,
    private val ref: MediaRef,
    private val store: FirestoreMediaStore,
    private val cacheDir: File,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val file = File(cacheDir, sha256(url))
        if (!file.exists()) {
            val media = store.read(ref) ?: throw FileNotFoundException(url)
            withContext(Dispatchers.IO) {
                cacheDir.mkdirs()
                val partial = File(cacheDir, "${file.name}.tmp")
                partial.writeBytes(media.bytes)
                partial.renameTo(file)
            }
        }
        return SourceFetchResult(
            source = ImageSource(file = file.toOkioPath(), fileSystem = FileSystem.SYSTEM),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val store: FirestoreMediaStore, private val cacheDir: File) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != MediaUrls.SCHEME) return null
            val url = data.toString()
            val ref = MediaUrls.parse(url) ?: return null
            return MediaFetcher(url, ref, store, cacheDir)
        }
    }

    private companion object {
        fun sha256(text: String): String =
            MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

/** El `ImageLoader` de toda la app: el de siempre, más [MediaFetcher]. */
internal class HuellitasImageLoaderFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: FirestoreMediaStore,
) : SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(MediaFetcher.Factory(store, File(this@HuellitasImageLoaderFactory.context.cacheDir, CACHE_DIR))) }
            .build()

    private companion object {
        const val CACHE_DIR = "huellitas-media"
    }
}
