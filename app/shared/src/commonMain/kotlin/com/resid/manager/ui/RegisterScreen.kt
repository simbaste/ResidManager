package com.resid.manager.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resid.manager.ui.components.imageDropTarget
import com.resid.manager.ui.components.rememberImagePickerLauncher
import com.resid.manager.ui.theme.ResidTheme
import com.resid.manager.ui.theme.residColors
import com.resid.manager.viewmodel.LoginUiState
import org.jetbrains.compose.resources.decodeToImageBitmap

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun RegisterScreen(
    uiState: LoginUiState,
    onFirstNameChanged: (String) -> Unit = { },
    onLastNameChanged: (String) -> Unit = { },
    onBirthDateChanged: (String) -> Unit = { },
    onPhoneChanged: (String) -> Unit = { },
    onEmailChanged: (String) -> Unit = { },
    onPasswordChanged: (String) -> Unit = { },
    togglePasswordVisibility: () -> Unit = { },
    toggleTheme: () -> Unit = { },
    onRegister: () -> Unit = { },
    onNavigateToLogin: () -> Unit = { },
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
        val isLarge = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

        // Atmospheric Ambient Background
        LoginAtmosphericBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = if (isCompact) 16.dp else if (isLarge) 48.dp else 32.dp,
                    vertical = if (isCompact) 16.dp else 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TopBar Header
            RegisterTopBar(
                isCompact = isCompact,
                isDarkMode = uiState.darkMode,
                onToggleTheme = toggleTheme
            )

            Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 36.dp))

            // 2. Main Register Card Container
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 500.dp, max = 860.dp)
                        .fillMaxWidth(if (isCompact) 1f else 0.85f)
                        .shadow(
                            elevation = 28.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color.Black.copy(alpha = MaterialTheme.residColors.cardShadowElevation),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                        .background(
                            color = MaterialTheme.residColors.cardBackground,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.residColors.cardBorder,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Decorative Top Brand Accent Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isCompact) 20.dp else 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RegisterCardHeader(isCompact = isCompact)

                            Spacer(modifier = Modifier.height(24.dp))

                            RegisterForm(
                                firstName = uiState.firstName,
                                lastName = uiState.lastName,
                                birthDate = uiState.birthDate,
                                phone = uiState.phone,
                                email = uiState.email,
                                password = uiState.passwordPlain,
                                passwordVisible = uiState.passwordVisible,
                                isLoading = uiState.isLoading,
                                errorMessage = uiState.errorMessage,
                                isCompact = isCompact,
                                onFirstNameChanged = onFirstNameChanged,
                                onLastNameChanged = onLastNameChanged,
                                onBirthDateChanged = onBirthDateChanged,
                                onPhoneChanged = onPhoneChanged,
                                onEmailChanged = onEmailChanged,
                                onPasswordChanged = onPasswordChanged,
                                onTogglePasswordVisibility = togglePasswordVisibility,
                                onSubmit = onRegister
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            HorizontalDivider(
                                color = MaterialTheme.residColors.dividerColor,
                                thickness = 1.dp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            RegisterLoginOption(onNavigateToLogin = onNavigateToLogin)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Badge
            LoginSecurityBadge()

            Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 36.dp))

            // 3. Footer
            LoginFooter(isCompact = isCompact)
        }
    }
}

/**
 * TopBar: Brand Identity, Theme Switcher and Support Link.
 */
@Composable
fun RegisterTopBar(
    isCompact: Boolean,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 1200.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(
                        12.dp,
                        RoundedCornerShape(14.dp),
                        ambientColor = MaterialTheme.colorScheme.primary,
                        spotColor = MaterialTheme.colorScheme.primary
                    )
                    .background(
                        brush = Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(1.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.residColors.cardBackground,
                            RoundedCornerShape(12.5.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apartment,
                        contentDescription = "Resid Manager Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Resid Manager",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompact) 18.sp else 20.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
                Text(
                    text = "GESTION IMMOBILIÈRE",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.residColors.inputBackground, CircleShape)
                    .border(1.dp, MaterialTheme.residColors.inputBorder, CircleShape)
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkMode) "Mode clair" else "Mode sombre",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (!isCompact) {
                Text(
                    text = "Centre d'assistance",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Card Header: Logo Badge and Titles.
 */
@Composable
fun RegisterCardHeader(
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Apartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "RESID MANAGER",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Créer un compte",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = if (isCompact) 22.sp else 28.sp,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Rejoignez la plateforme moderne de gestion immobilière",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

/**
 * Profile Photo Upload Section with Click-to-Pick and Drag-and-Drop (Images only).
 */
@Composable
fun RegisterProfilePhotoSection(
    selectedImage: ImageBitmap?,
    onImageSelected: (ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDraggingOver by remember { mutableStateOf(false) }
    val imagePicker = rememberImagePickerLauncher { bytes ->
        onImageSelected(bytes)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isDraggingOver) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.residColors.inputBackground,
                RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isDraggingOver) 2.dp else 1.dp,
                color = if (isDraggingOver) MaterialTheme.colorScheme.primary else MaterialTheme.residColors.inputBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .imageDropTarget(
                onImageDropped = { bytes ->
                    onImageSelected(bytes)
                },
                onDragStateChanged = { dragging ->
                    isDraggingOver = dragging
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(
                    width = 2.dp,
                    color = if (selectedImage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .clickable { imagePicker.launch() },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImage != null) {
                Image(
                    bitmap = selectedImage,
                    contentDescription = "Photo de profil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Portrait",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Hover / Action Overlay with Camera Icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (selectedImage != null) 0.25f else 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Changer la photo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { imagePicker.launch() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Photo de profil",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (selectedImage != null) "(Photo ajoutée)" else "(Optionnel)",
                    color = if (selectedImage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = if (selectedImage != null) FontWeight.Medium else FontWeight.Normal
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isDraggingOver) "Déposez votre image ici..." else "Glissez-déposez ou cliquez pour importer votre portrait.",
                color = if (isDraggingOver) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Text(
                text = "Format JPG, PNG, WebP • Images uniquement",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

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

    // Compute Password Strength
    val passwordStrength = remember(password) {
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        score
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Optional Profile Photo Section with click & drop
        RegisterProfilePhotoSection(
            selectedImage = avatarBitmap,
            onImageSelected = { bytes ->
                try {
                    avatarBitmap = bytes.decodeToImageBitmap()
                } catch (_: Exception) {
                    // Ignore non-supported/corrupt image bytes
                }
            }
        )

        // Name Fields (Side by side on medium/large, stacked on compact)
        if (isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                RegisterTextField(
                    label = "PRÉNOM *",
                    value = firstName,
                    placeholder = "Jean",
                    onValueChange = onFirstNameChanged,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    imeAction = ImeAction.Next
                )
                RegisterTextField(
                    label = "NOM DE FAMILLE *",
                    value = lastName,
                    placeholder = "Dupont",
                    onValueChange = onLastNameChanged,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
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
                        imeAction = ImeAction.Next
                    )
                }
            }
        }

        // Contact Fields (BirthDate & Phone with Country Code Picker)
        if (isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                com.resid.manager.ui.components.AppDatePickerField(
                    date = birthDate,
                    onDateSelected = onBirthDateChanged,
                    label = "DATE DE NAISSANCE",
                    placeholder = "AAAA-MM-JJ",
                    yearRange = 1920..2026
                )
                com.resid.manager.ui.components.PhoneTextField(
                    value = phone,
                    onPhoneChanged = onPhoneChanged,
                    label = "TÉLÉPHONE",
                    imeAction = ImeAction.Next
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    com.resid.manager.ui.components.AppDatePickerField(
                        date = birthDate,
                        onDateSelected = onBirthDateChanged,
                        label = "DATE DE NAISSANCE",
                        placeholder = "AAAA-MM-JJ",
                        yearRange = 1920..2026
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    com.resid.manager.ui.components.PhoneTextField(
                        value = phone,
                        onPhoneChanged = onPhoneChanged,
                        label = "TÉLÉPHONE",
                        imeAction = ImeAction.Next
                    )
                }
            }
        }

        // Email Field
        RegisterTextField(
            label = "ADRESSE EMAIL *",
            value = email,
            placeholder = "nom@exemple.com",
            onValueChange = onEmailChanged,
            keyboardType = KeyboardType.Email,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
            imeAction = ImeAction.Next
        )

        // Password & Confirm Password
        if (isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                RegisterPasswordField(
                    label = "MOT DE PASSE *",
                    value = password,
                    placeholder = "••••••••",
                    passwordVisible = passwordVisible,
                    onValueChange = onPasswordChanged,
                    onTogglePasswordVisibility = onTogglePasswordVisibility,
                    imeAction = ImeAction.Next
                )
                RegisterPasswordField(
                    label = "CONFIRMER LE MOT DE PASSE *",
                    value = confirmPassword,
                    placeholder = "••••••••",
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
                        placeholder = "••••••••",
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
                        placeholder = "••••••••",
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

        // Password Security Visual Indicator
        PasswordStrengthIndicator(strength = passwordStrength, passwordLength = password.length)

        // Terms and Conditions Acceptance
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
                text = "J'accepte les Conditions Générales d'Utilisation et la Politique de confidentialité.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        // Error message display
        if (!errorMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.errorContainer.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .border(1.dp, colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
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

        // Submit Button
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
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            enabled = !isLoading && termsAccepted
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
                        modifier = Modifier.size(24.dp)
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
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable stylized text field for registration.
 */
@Composable
private fun RegisterTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    leadingIcon: @Composable (() -> Unit)? = null,
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

/**
 * Material 3 Date Picker field for Date of Birth.
 * Clickable input opening a multiplatform DatePickerDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDateOfBirthPickerField(
    value: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    // Parse existing YYYY-MM-DD to UTC epoch millis
    val initialSelectedMillis = remember(value) {
        parseDateToUtcMillis(value)
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedMillis,
        yearRange = 1920..2026
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "DATE DE NAISSANCE",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        "AAAA-MM-JJ",
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
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Sélectionner la date de naissance",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendrier",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            // Transparent overlay to make the whole field trigger the date picker dialog
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDatePicker = true }
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(formatUtcMillisToIsoDate(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = "Confirmer",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(
                        text = "Annuler",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.residColors.cardBackground
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                    todayContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

/**
 * Formats epoch UTC milliseconds into ISO date format "YYYY-MM-DD" across all Kotlin Multiplatform targets.
 */
private fun formatUtcMillisToIsoDate(utcMillis: Long): String {
    // 86,400,000 ms per day
    var days = (utcMillis / 86400000L).toInt()

    // Algorithm based on Gregorian calendar epoch computation
    var year = 1970
    while (true) {
        val daysInYear = if (isLeapYear(year)) 366 else 365
        if (days >= daysInYear) {
            days -= daysInYear
            year++
        } else if (days < 0) {
            year--
            days += if (isLeapYear(year)) 366 else 365
        } else {
            break
        }
    }

    val daysInMonths = intArrayOf(
        31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    )

    var month = 1
    for (dim in daysInMonths) {
        if (days >= dim) {
            days -= dim
            month++
        } else {
            break
        }
    }
    val day = days + 1

    val mStr = if (month < 10) "0$month" else "$month"
    val dStr = if (day < 10) "0$day" else "$day"
    return "$year-$mStr-$dStr"
}

/**
 * Parses "YYYY-MM-DD" into UTC epoch milliseconds.
 */
private fun parseDateToUtcMillis(isoDate: String): Long? {
    val parts = isoDate.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null

    var days = 0L
    if (year >= 1970) {
        for (y in 1970 until year) {
            days += if (isLeapYear(y)) 366 else 365
        }
    } else {
        for (y in year until 1970) {
            days -= if (isLeapYear(y)) 366 else 365
        }
    }

    val daysInMonths = intArrayOf(
        31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    )

    for (m in 1 until month) {
        days += daysInMonths[m - 1]
    }
    days += (day - 1)

    return days * 86400000L
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

/**
 * Reusable stylized password field with toggle button.
 */
@Composable
private fun RegisterPasswordField(
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

// ==========================================
// PREVIEWS
// ==========================================

@Preview
@Composable
fun RegisterScreenPreview() {
    ResidTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            RegisterScreen(
                uiState = LoginUiState()
            )
        }
    }
}

@Preview
@Composable
fun RegisterCardHeaderPreview() {
    ResidTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            RegisterCardHeader(isCompact = false)
        }
    }
}

@Preview
@Composable
fun RegisterFormPreview() {
    ResidTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            RegisterForm(
                firstName = "Jean",
                lastName = "Dupont",
                birthDate = "1990-05-15",
                phone = "+22501020304",
                email = "jean.dupont@residence.fr",
                password = "Password123!",
                passwordVisible = false,
                isLoading = false,
                errorMessage = null,
                isCompact = false,
                onFirstNameChanged = {},
                onLastNameChanged = {},
                onBirthDateChanged = {},
                onPhoneChanged = {},
                onEmailChanged = {},
                onPasswordChanged = {},
                onTogglePasswordVisibility = {},
                onSubmit = {}
            )
        }
    }
}