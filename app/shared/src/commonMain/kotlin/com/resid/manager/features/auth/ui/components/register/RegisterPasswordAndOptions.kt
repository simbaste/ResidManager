package com.resid.manager.features.auth.ui.components.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resid.manager.ui.theme.residColors

/**
 * Reusable stylized password field with toggle button.
 */
@Composable
fun RegisterPasswordField(
    label: String,
    value: String,
    placeholder: String,
    passwordVisible: Boolean,
    onValueChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.residColors.inputBackground,
                unfocusedContainerColor = MaterialTheme.residColors.inputBackground,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.residColors.inputBorder,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                TextButton(
                    onClick = onTogglePasswordVisibility,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = if (passwordVisible) "Masquer" else "Afficher",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone?.invoke() }
            )
        )
    }
}

/**
 * 4-bar password security visual indicator matching the design.
 */
@Composable
fun PasswordStrengthIndicator(
    strength: Int,
    passwordLength: Int,
    modifier: Modifier = Modifier
) {
    val (label, color) = when {
        passwordLength == 0 -> "8 caractères minimum" to MaterialTheme.colorScheme.onSurfaceVariant
        strength <= 1 -> "Faible" to Color(0xFFEF4444)
        strength == 2 -> "Moyen" to Color(0xFFF59E0B)
        strength == 3 -> "Sécurisé" to Color(0xFF10B981)
        else -> "Très sécurisé" to Color(0xFF059669)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Force du mot de passe",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val emptyBarColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            for (i in 1..4) {
                val barColor = when {
                    i > strength || passwordLength == 0 -> emptyBarColor
                    strength <= 1 -> Color(0xFFEF4444)
                    strength == 2 -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(barColor, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

/**
 * Redirection to Login page.
 */
@Composable
fun RegisterLoginOption(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Déjà un compte ?",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Se connecter",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onNavigateToLogin() }
        )
    }
}
