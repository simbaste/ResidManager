package com.resid.manager.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageFormatDetectorTest {

    @Test
    fun detect_jpeg() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 0, 0, 0, 0, 0, 0, 0)
        val format = ImageFormatDetector.detect(bytes)
        assertEquals("image/jpeg", format.mimeType)
        assertEquals("jpg", format.extension)
        assertFalse(format.isHeic)
    }

    @Test
    fun detect_png() {
        val bytes = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
            0, 0, 0, 0
        )
        val format = ImageFormatDetector.detect(bytes)
        assertEquals("image/png", format.mimeType)
        assertEquals("png", format.extension)
        assertFalse(format.isHeic)
    }

    @Test
    fun detect_webp() {
        val bytes = ByteArray(12)
        bytes[0] = 0x52.toByte() // R
        bytes[1] = 0x49.toByte() // I
        bytes[8] = 0x57.toByte() // W
        bytes[9] = 0x45.toByte() // E
        val format = ImageFormatDetector.detect(bytes)
        assertEquals("image/webp", format.mimeType)
        assertEquals("webp", format.extension)
        assertFalse(format.isHeic)
    }

    @Test
    fun detect_heic() {
        // ftypheic
        val bytes = ByteArray(12)
        bytes[4] = 0x66.toByte() // 'f'
        bytes[5] = 0x74.toByte() // 't'
        bytes[6] = 0x79.toByte() // 'y'
        bytes[7] = 0x70.toByte() // 'p'
        bytes[8] = 'h'.code.toByte()
        bytes[9] = 'e'.code.toByte()
        bytes[10] = 'i'.code.toByte()
        bytes[11] = 'c'.code.toByte()

        val format = ImageFormatDetector.detect(bytes)
        assertEquals("image/heic", format.mimeType)
        assertEquals("heic", format.extension)
        assertTrue(format.isHeic)
    }
}
