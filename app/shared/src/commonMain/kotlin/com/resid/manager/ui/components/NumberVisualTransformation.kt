package com.resid.manager.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * VisualTransformation that formats numbers by adding space grouping for thousands (e.g., "60000" -> "60 000").
 * Supports optional decimal separator ('.' or ',').
 */
class ThousandsSeparatorVisualTransformation(
    private val separator: Char = ' '
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Split integer and decimal parts if any
        val decimalSeparatorIndex = originalText.indexOfAny(charArrayOf('.', ','))
        val integerPart = if (decimalSeparatorIndex >= 0) originalText.substring(0, decimalSeparatorIndex) else originalText
        val decimalPart = if (decimalSeparatorIndex >= 0) originalText.substring(decimalSeparatorIndex) else ""

        // Format integer part with thousands separator from right to left
        val isNegative = integerPart.startsWith('-')
        val digits = if (isNegative) integerPart.substring(1) else integerPart

        val formattedInteger = buildString {
            if (isNegative) append('-')
            val length = digits.length
            for (i in 0 until length) {
                append(digits[i])
                val remaining = length - 1 - i
                if (remaining > 0 && remaining % 3 == 0) {
                    append(separator)
                }
            }
        }

        val formattedText = formattedInteger + decimalPart

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clampedOffset = offset.coerceIn(0, originalText.length)

                // If cursor is after decimal point
                if (decimalSeparatorIndex >= 0 && clampedOffset > decimalSeparatorIndex) {
                    val integerTransformedLength = formattedInteger.length
                    val decimalOffset = clampedOffset - decimalSeparatorIndex
                    return integerTransformedLength + decimalOffset
                }

                // Cursor is within integer part
                var transformedOffset = 0
                var originalCount = 0

                if (isNegative && clampedOffset > 0) {
                    transformedOffset = 1
                    originalCount = 1
                }

                for (i in 0 until digits.length) {
                    if (originalCount >= clampedOffset) break
                    transformedOffset++
                    originalCount++
                    val remaining = digits.length - 1 - i
                    if (remaining > 0 && remaining % 3 == 0 && originalCount < clampedOffset) {
                        transformedOffset++
                    }
                }

                return transformedOffset.coerceIn(0, formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val clampedOffset = offset.coerceIn(0, formattedText.length)

                val integerTransformedLength = formattedInteger.length
                if (clampedOffset >= integerTransformedLength) {
                    val excess = clampedOffset - integerTransformedLength
                    val originalDecimal = if (decimalSeparatorIndex >= 0) decimalSeparatorIndex else originalText.length
                    return (originalDecimal + excess).coerceIn(0, originalText.length)
                }

                var originalOffset = 0
                for (i in 0 until clampedOffset) {
                    if (formattedText[i] != separator) {
                        originalOffset++
                    }
                }
                return originalOffset.coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
