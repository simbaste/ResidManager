package com.resid.manager.ui.components

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Specialized text field for numeric monetary and charge amounts.
 * - Formats automatically with thousands grouping (e.g. 60 000) using [ThousandsSeparatorVisualTransformation].
 * - Filters raw input so only digits and a single decimal point/comma are preserved.
 * - Optional currency symbol displayed as suffix.
 */
@Composable
fun AmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
    currencySymbol: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    allowDecimals: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    bringIntoViewRequester: BringIntoViewRequester? = null
) {
    val visualTransformation = remember { ThousandsSeparatorVisualTransformation() }

    AppTextField(
        value = value,
        onValueChange = { input ->
            // Clean up: remove spaces (in case user pasted formatted text) and replace comma by dot
            val cleaned = input.replace(" ", "").replace("\u00A0", "")

            val isValid = if (allowDecimals) {
                // Digits only or with at most one dot/comma
                cleaned.isEmpty() || cleaned.matches(Regex("""^\d*([.,]\d*)?$"""))
            } else {
                cleaned.isEmpty() || cleaned.matches(Regex("""^\d*$"""))
            }

            if (isValid) {
                onValueChange(cleaned)
            }
        },
        label = label,
        placeholder = placeholder,
        isError = isError,
        errorMessage = errorMessage,
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (allowDecimals) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = imeAction
        ),
        imeAction = imeAction,
        onImeAction = onImeAction,
        trailingIcon = if (!currencySymbol.isNullOrBlank()) {
            {
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        bringIntoViewRequester = bringIntoViewRequester,
        modifier = modifier
    )
}
