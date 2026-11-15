package com.example.huellitasya.core.data.media

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUrlsTest {

    @Test
    fun `la URL de un archivo se puede leer de vuelta`() {
        val ref = MediaUrls.recordFile("uid-1", "pet-9", "rec-3")

        val url = MediaUrls.of(ref)

        assertEquals("huellitas-media://users/uid-1/media/record-pet-9-rec-3", url)
        assertEquals(ref, MediaUrls.parse(url))
    }

    @Test
    fun `la version de una foto no cambia a que archivo apunta`() {
        val ref = MediaUrls.profilePhoto("uid-1")

        val url = MediaUrls.of(ref, version = 1_700_000_000_000)

        assertEquals("huellitas-media://users/uid-1/media/profile?v=1700000000000", url)
        assertEquals(ref, MediaUrls.parse(url))
    }

    @Test
    fun `una URL de Storage no es de Firestore`() {
        val storageUrl = "https://firebasestorage.googleapis.com/v0/b/x/o/users%2Fuid%2Fprofile.jpg?alt=media"

        assertFalse(MediaUrls.isMedia(storageUrl))
        assertNull(MediaUrls.parse(storageUrl))
        assertTrue(MediaUrls.isMedia(MediaUrls.of(MediaUrls.petPhoto("u", "p"))))
    }

    @Test
    fun `los pedazos respetan el tamano y al unirlos dan el archivo`() {
        val bytes = ByteArray(2_500) { it.toByte() }

        val parts = chunk(bytes, 1_000)

        assertEquals(listOf(1_000, 1_000, 500), parts.map { it.size })
        assertArrayEquals(bytes, parts.reduce { acc, part -> acc + part })
    }

    @Test
    fun `un PDF de casi 10 MB cabe en el maximo de 12 pedazos`() {
        val parts = chunk(ByteArray(10 * 1024 * 1024 - 1), FirestoreMediaStore.CHUNK_BYTES)

        assertTrue(parts.size <= 12)
    }
}
