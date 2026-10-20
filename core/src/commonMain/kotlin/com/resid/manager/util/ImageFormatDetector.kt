package com.resid.manager.util

object ImageFormatDetector {

    data class ImageFormat(
        val mimeType: String,
        val extension: String,
        val isHeic: Boolean = false
    )

    fun detect(bytes: ByteArray): ImageFormat {
        if (bytes.size < 12) {
            return ImageFormat(mimeType = "image/jpeg", extension = "jpg")
        }

        // Détection HEIC / HEIF (signature 'ftyp' suivie d'un sous-type compatible)
        val isFtyp = bytes[4] == 0x66.toByte() && // 'f'
                bytes[5] == 0x74.toByte() && // 't'
                bytes[6] == 0x79.toByte() && // 'y'
                bytes[7] == 0x70.toByte()    // 'p'

        if (isFtyp) {
            val majorBrand = bytes.copyOfRange(8, 12).decodeToString()
            if (majorBrand in listOf("heic", "heix", "hevc", "hevx", "mif1", "msf1")) {
                return ImageFormat(mimeType = "image/heic", extension = "heic", isHeic = true)
            }
        }

        // PNG (89 50 4E 47 0D 0A 1A 0A)
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte()) {
            return ImageFormat(mimeType = "image/png", extension = "png")
        }

        // WebP (RIFF .... WEBP)
        if (bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte()
        ) {
            return ImageFormat(mimeType = "image/webp", extension = "webp")
        }

        // Défaut JPEG (FF D8 FF)
        return ImageFormat(mimeType = "image/jpeg", extension = "jpg")
    }
}
