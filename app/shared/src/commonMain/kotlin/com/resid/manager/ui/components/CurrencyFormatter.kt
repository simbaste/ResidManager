package com.resid.manager.ui.components

import kotlin.math.roundToLong

/**
 * Formats a monetary amount into a clean, human-readable format with thousands grouping (e.g. 60 000, 1 250 500).
 *
 * @param amount The numerical amount to format.
 * @param currencySymbol Optional currency symbol or code (e.g. "XOF", "€", "$").
 * @param maxDecimals Maximum decimal places to show (default: 2; trailing zeroes are trimmed if integer).
 * @return Human-readable formatted string (e.g., "60 000 XOF", "125.50 €").
 */
fun formatAmount(
    amount: Double,
    currencySymbol: String? = null,
    maxDecimals: Int = 2
): String {
    val isNegative = amount < 0
    val absAmount = kotlin.math.abs(amount)

    // Separate integer and decimal parts
    val integerPart = absAmount.toLong()
    val rawDecimal = absAmount - integerPart

    val integerDigits = integerPart.toString()
    val formattedInteger = buildString {
        if (isNegative) append('-')
        val length = integerDigits.length
        for (i in 0 until length) {
            append(integerDigits[i])
            val remaining = length - 1 - i
            if (remaining > 0 && remaining % 3 == 0) {
                append(' ')
            }
        }
    }

    val formattedNumber = if (maxDecimals > 0 && rawDecimal > 0.000001) {
        var multiplier = 1.0
        repeat(maxDecimals) { multiplier *= 10 }
        val roundedDec = ((rawDecimal * multiplier).roundToLong()).toString().padStart(maxDecimals, '0')
        val trimmedDec = roundedDec.trimEnd('0')
        if (trimmedDec.isNotEmpty()) {
            "$formattedInteger.$trimmedDec"
        } else {
            formattedInteger
        }
    } else {
        formattedInteger
    }

    return if (!currencySymbol.isNullOrBlank()) {
        "$formattedNumber $currencySymbol"
    } else {
        formattedNumber
    }
}

/**
 * Extension helper on [Double] for formatting currency amounts cleanly.
 */
fun Double.toFormattedAmount(currencySymbol: String? = null, maxDecimals: Int = 2): String =
    formatAmount(this, currencySymbol, maxDecimals)

/**
 * Extension helper on [Long] for formatting currency amounts cleanly.
 */
fun Long.toFormattedAmount(currencySymbol: String? = null): String =
    formatAmount(this.toDouble(), currencySymbol, maxDecimals = 0)
