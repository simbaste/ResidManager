package com.resid.manager.ui.components

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.launch

/**
 * Standard text field wrapper for consistent styling, error handling and auto-scroll behavior.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    imeAction: androidx.compose.ui.text.input.ImeAction? = null,
    onImeAction: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    bringIntoViewRequester: BringIntoViewRequester? = null
) {
    val coroutineScope = rememberCoroutineScope()

    val effectiveKeyboardOptions = if (imeAction != null) {
        keyboardOptions.copy(imeAction = imeAction)
    } else {
        keyboardOptions
    }

    val effectiveKeyboardActions = if (onImeAction != null) {
        KeyboardActions(
            onDone = { onImeAction() },
            onNext = { onImeAction() },
            onSearch = { onImeAction() },
            onSend = { onImeAction() },
            onGo = { onImeAction() }
        )
    } else {
        keyboardActions
    }

    val combinedModifier = if (bringIntoViewRequester != null) {
        modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent {
                if (it.isFocused) {
                    coroutineScope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            }
    } else {
        modifier
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
        } else null,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = effectiveKeyboardOptions,
        keyboardActions = effectiveKeyboardActions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = combinedModifier
    )
}
