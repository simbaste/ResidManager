package com.resid.manager.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * VisualTransformation that formats phone digits into groups separated by spaces.
 * Example: "0102030405" -> "01 02 03 04 05"
 * Grouping is every 2 digits by default (very common in France, Ivory Coast, Senegal, etc.).
 */
class PhoneVisualTransformation(
    private val groupSize: Int = 2,
    private val separator: Char = ' '
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formattedText = buildString {
            for (i in originalText.indices) {
                append(originalText[i])
                if ((i + 1) % groupSize == 0 && (i + 1) < originalText.length) {
                    append(separator)
                }
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceIn(0, originalText.length)
                val spaces = if (clamped == 0) 0 else (clamped - 1) / groupSize
                return (clamped + spaces).coerceIn(0, formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceIn(0, formattedText.length)
                var count = 0
                for (i in 0 until clamped) {
                    if (formattedText[i] != separator) {
                        count++
                    }
                }
                return count.coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
