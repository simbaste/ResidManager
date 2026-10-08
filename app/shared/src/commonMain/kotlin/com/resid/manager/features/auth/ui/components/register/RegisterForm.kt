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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resid.manager.ui.components.AppDatePickerField
import com.resid.manager.ui.components.PhoneTextField
import com.resid.manager.ui.theme.residColors
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * State-hoisted Registration Form with 2-column responsive layout, password strength indicator, terms acceptance.
 */
@Composable
fun RegisterForm(
    firstName: String,
    lastName: String,
    birthDate: String,
    phone: String,
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    isCompact: Boolean,
    onFirstNameChanged: (String) -> Unit,
    onLastNameChanged: (String) -> Unit,
    onBirthDateChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmPassword by remember { mutableStateOf("") }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(true) }
    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme

    val passwordStrength = remember(password) {
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        score
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RegisterProfilePhotoSection(
            selectedImage = avatarBitmap,
            onImageSelected = { bytes ->
                try {
                    avatarBitmap = bytes.decodeToImageBitmap()
                } catch (_: Exception) {
                }
            }
        )

        if (isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                RegisterTextField(
                    label = "PRÉNOM *",
                    value = firstName,
                    placeholder = "Jean",
                    onValueChange = onFirstNameChanged,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    contentType = ContentType.PersonFirstName,
                    imeAction = ImeAction.Next
                )
                RegisterTextField(
                    label = "NOM DE FAMILLE *",
                    value = lastName,
                    placeholder = "Dupont",
                    onValueChange = onLastNameChanged,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    contentType = ContentType.PersonLastName,
                    imeAction = ImeAction.Next
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    RegisterTextField(
                        label = "PRÉNOM *",
                        value = firstName,
                        placeholder = "Jean",
                        onValueChange = onFirstNameChanged,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        contentType = ContentType.PersonFirstName,
                        imeAction = ImeAction.Next
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    RegisterTextField(
                        label = "NOM DE FAMILLE *",
                        value = lastName,
                        placeholder = "Dupont",
                        onValueChange = onLastNameChanged,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        contentType = ContentType.PersonLastName,
                        imeAction = ImeAction.Next
                    )
                }
            }
        }

        if (isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AppDatePickerField(
                    date = birthDate,
                    onDateSelected = onBirthDateChanged,
                    label = "DATE DE NAISSANCE",
                    placeholder = "AAAA-MM-JJ",
                    yearRange = 1920..2026
                )
                PhoneTextField(
                    value = phone,
                    onPhoneChanged = onPhoneChanged,
                    label = "Téléphone",
                    imeAction = ImeAction.Next
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    AppDatePickerField(
                        date = birthDate,
                        onDateSelected = onBirthDateChanged,
                        label = "DATE DE NAISSANCE",
                        placeholder = "AAAA-MM-JJ",
                        yearRange = 1920..2026
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    PhoneTextField(
                        value = phone,
                        onPhoneChanged = onPhoneChanged,
                        label = "Téléphone",
                        imeAction = ImeAction.Next
                    )
                }
            }
        }

        RegisterTextField(
            label = "ADRESSE EMAIL PROFESSIONNELLE OU PERSONNELLE *",
            value = email,
            placeholder = "jean.dupont@exemple.fr",
            onValueChange = onEmailChanged,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            contentType = ContentType.EmailAddress,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )

        if (isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                RegisterPasswordField(
                    label = "MOT DE PASSE *",
                    value = password,
                    placeholder = "8+ car., chiffre, symbole",
                    passwordVisible = passwordVisible,
                    onValueChange = onPasswordChanged,
                    onTogglePasswordVisibility = onTogglePasswordVisibility,
                    imeAction = ImeAction.Next
                )
                RegisterPasswordField(
                    label = "CONFIRMER LE MOT DE PASSE *",
                    value = confirmPassword,
                    placeholder = "Répéter le mot de passe",
                    passwordVisible = confirmPasswordVisible,
                    onValueChange = { confirmPassword = it },
                    onTogglePasswordVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                    imeAction = ImeAction.Done,
                    onDone = {
                        focusManager.clearFocus()
                        if (!isLoading && termsAccepted) onSubmit()
                    }
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    RegisterPasswordField(
                        label = "MOT DE PASSE *",
                        value = password,
                        placeholder = "8+ car., chiffre, symbole",
                        passwordVisible = passwordVisible,
                        onValueChange = onPasswordChanged,
                        onTogglePasswordVisibility = onTogglePasswordVisibility,
                        imeAction = ImeAction.Next
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    RegisterPasswordField(
                        label = "CONFIRMER LE MOT DE PASSE *",
                        value = confirmPassword,
                        placeholder = "Répéter le mot de passe",
                        passwordVisible = confirmPasswordVisible,
                        onValueChange = { confirmPassword = it },
                        onTogglePasswordVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                        imeAction = ImeAction.Done,
                        onDone = {
                            focusManager.clearFocus()
                            if (!isLoading && termsAccepted) onSubmit()
                        }
                    )
                }
            }
        }

        PasswordStrengthIndicator(
            strength = passwordStrength,
            passwordLength = password.length
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { termsAccepted = !termsAccepted }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.residColors.inputBorder,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "J'accepte les Conditions Générales d'Utilisation et la Politique de Confidentialité de Resid Manager.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Start
                )
            }
        }

        Button(
            onClick = {
                focusManager.clearFocus()
                onSubmit()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = MaterialTheme.colorScheme.primary,
                    spotColor = MaterialTheme.colorScheme.primaryContainer
                ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading && termsAccepted,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Créer mon compte",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    leadingIcon: @Composable (() -> Unit)? = null,
    contentType: ContentType? = null,
    modifier: Modifier = Modifier
) {
    val fieldModifier = if (contentType != null) {
        modifier.fillMaxWidth().semantics { this.contentType = contentType }
    } else {
        modifier.fillMaxWidth()
    }

    Column(modifier = fieldModifier) {
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
            leadingIcon = leadingIcon?.let {
                {
                    Box(modifier = Modifier.padding(start = 4.dp)) {
                        it()
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            )
        )
    }
}
