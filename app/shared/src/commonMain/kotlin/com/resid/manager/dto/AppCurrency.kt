package com.resid.manager.dto

import kotlinx.serialization.Serializable

/**
 * Centralized currency definition for the entire application.
 * Makes it effortless to add or update currencies across mobile, desktop, and web clients.
 */
@Serializable
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val shortLabel: String
) {
    XOF(
        code = "XOF",
        symbol = "FCFA",
        displayName = "Franc CFA (XOF - FCFA)",
        shortLabel = "Franc CFA (XOF)"
    ),
    EUR(
        code = "EUR",
        symbol = "€",
        displayName = "Euro (EUR - €)",
        shortLabel = "Euro (EUR)"
    ),
    USD(
        code = "USD",
        symbol = "$",
        displayName = "US Dollar (USD - $)",
        shortLabel = "US Dollar (USD)"
    );

    companion object {
        val DEFAULT = XOF

        fun fromCode(code: String?): AppCurrency {
            if (code.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: DEFAULT
        }

        fun fromSymbol(symbol: String?): AppCurrency {
            if (symbol.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { 
                it.symbol.equals(symbol.trim(), ignoreCase = true) || 
                it.name.equals(symbol.trim(), ignoreCase = true)
            } ?: DEFAULT
        }
    }
}
