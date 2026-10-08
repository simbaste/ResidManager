package com.resid.manager.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resid.manager.ui.theme.ResidTheme
import com.resid.manager.ui.theme.residColors
import com.resid.manager.viewmodel.LoginUiState

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChanged: (String) -> Unit = { },
    onPasswordChanged: (String) -> Unit = { },
    togglePasswordVisibility: () -> Unit = { },
    toggleTheme: () -> Unit = { },
    onLogin: () -> Unit = { },
    onNavigateToRegister: () -> Unit = { },
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
            // 1. Header / TopBar
            LoginHeader(
                isCompact = isCompact,
                isDarkMode = uiState.darkMode,
                onToggleTheme = toggleTheme
            )

            Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 40.dp))

            // 2. Main Login Card Container
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 420.dp, max = 560.dp)
                        .fillMaxWidth(if (isCompact) 1f else if (isLarge) 0.35f else 0.55f)
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
                        .padding(if (isCompact) 20.dp else 36.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoginCardHeader(isCompact = isCompact)

                        Spacer(modifier = Modifier.height(24.dp))

                        LoginForm(
                            email = uiState.email,
                            password = uiState.passwordPlain,
                            passwordVisible = uiState.passwordVisible,
                            isLoading = uiState.isLoading,
                            errorMessage = uiState.errorMessage,
                            onEmailChanged = onEmailChanged,
                            onPasswordChanged = onPasswordChanged,
                            onTogglePasswordVisibility = togglePasswordVisibility,
                            onSubmit = onLogin,
                            onForgotPasswordClick = { /* Forgot password action */ }
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        HorizontalDivider(color = MaterialTheme.residColors.dividerColor, thickness = 1.dp)

                        Spacer(modifier = Modifier.height(16.dp))

                        LoginRegisterOption(onNavigateToRegister = onNavigateToRegister)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Trust Badge
            LoginSecurityBadge()

            Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 36.dp))

            // 3. Footer
            LoginFooter(isCompact = isCompact)
        }
    }
}

/**
 * Atmospheric background canvas drawing luxury glow orbs and subtle grid pattern.
 */
@Composable
fun LoginAtmosphericBackground(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val residColors = MaterialTheme.residColors
    val brandPrimary = colorScheme.primary
    val brandPrimaryContainer = colorScheme.primaryContainer
    val textColorPrimary = colorScheme.onSurface

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Top-left primary glow orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(brandPrimary.copy(alpha = residColors.ambientGlowAlpha), Color.Transparent),
                center = Offset(w * 0.05f, h * 0.05f),
                radius = (w * 0.55f).coerceAtLeast(350f)
            ),
            radius = (w * 0.55f).coerceAtLeast(350f),
            center = Offset(w * 0.05f, h * 0.05f)
        )

        // Bottom-right primary glow orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(brandPrimaryContainer.copy(alpha = residColors.secondaryGlowAlpha), Color.Transparent),
                center = Offset(w * 0.95f, h * 0.95f),
                radius = (w * 0.6f).coerceAtLeast(400f)
            ),
            radius = (w * 0.6f).coerceAtLeast(400f),
            center = Offset(w * 0.95f, h * 0.95f)
        )

        // Center-right subtle ambient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorScheme.secondary.copy(alpha = residColors.secondaryGlowAlpha * 0.7f), Color.Transparent),
                center = Offset(w * 0.8f, h * 0.45f),
                radius = (w * 0.4f).coerceAtLeast(250f)
            ),
            radius = (w * 0.4f).coerceAtLeast(250f),
            center = Offset(w * 0.8f, h * 0.45f)
        )

        // Subtle Architectural Grid Pattern
        val spacing = 36.dp.toPx()
        var x = 0f
        while (x < w) {
            var y = 0f
            while (y < h) {
                drawCircle(
                    color = textColorPrimary.copy(alpha = residColors.gridAlpha),
                    radius = 1.2f,
                    center = Offset(x, y)
                )
                y += spacing
            }
            x += spacing
        }
    }
}

/**
 * Header section: Logo, Brand titles, Theme switcher, and Support Link.
 */
@Composable
fun LoginHeader(
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
        // Brand Logo + Titles
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

        // Right Actions: Theme Toggle Button + Support Center Link
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
 * Top icon badge and titles of the login card.
 */
@Composable
fun LoginCardHeader(
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Resid Manager - Connexion",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = if (isCompact) 22.sp else 26.sp,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Accédez à votre espace de gestion résidentielle et patrimoine",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

/**
 * State-hoisted login input form with social SSO, email, password, remember-me, and submit action.
 */
@Composable
fun LoginForm(
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rememberMe by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        // Social SSO Button: Continuer avec Google
        OutlinedButton(
            onClick = { /* Future SSO integration */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.residColors.inputBackground,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.residColors.inputBorder)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continuer avec Google",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Divider with label
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.residColors.dividerColor
            )
            Text(
                text = "OU AVEC EMAIL",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.residColors.dividerColor
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Field 1: Email
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ADRESSE EMAIL",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChanged,
                placeholder = {
                    Text(
                        "nom@residence.fr",
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
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Field 2: Password
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "MOT DE PASSE",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChanged,
                placeholder = {
                    Text(
                        "••••••••••••",
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
                        contentDescription = "Mot de passe",
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
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (!isLoading) onSubmit()
                    }
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Options: Remember Me & Forgot Password
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { rememberMe = !rememberMe }
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.residColors.inputBorder,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Se souvenir de moi",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }

            Text(
                text = "Mot de passe oublié ?",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onForgotPasswordClick() }
            )
        }

        // Error Message display
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
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

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Submit Button: Brand Gradient + Glow
        Button(
            onClick = {
                focusManager.clearFocus()
                onSubmit()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = MaterialTheme.colorScheme.primary,
                    spotColor = MaterialTheme.colorScheme.primaryContainer
                ),
            shape = RoundedCornerShape(12.dp),
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
                            text = "Se connecter",
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

/**
 * Switch link to navigate to registration.
 */
@Composable
fun LoginRegisterOption(
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Pas encore de compte ?",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "S'inscrire",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onNavigateToRegister() }
        )
    }
}

/**
 * Security SSL badge display.
 */
@Composable
fun LoginSecurityBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Connexion sécurisée SSL 256-bit • Données hébergées en France",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Footer: Copyright and legal mentions.
 */
@Composable
fun LoginFooter(
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 1200.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = MaterialTheme.residColors.dividerColor, thickness = 1.dp)
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "© 2026 Resid Manager. Système de gestion de patrimoine immobilier.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = if (isCompact) TextAlign.Center else TextAlign.Start
            )

            if (!isCompact) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(text = "Confidentialité", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(text = "Mentions légales", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(text = "Contact", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview
@Composable
fun LoginScreenPreview() {
    ResidTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LoginScreen(
                uiState = LoginUiState()
            )
        }
    }
}

@Preview
@Composable
fun LoginHeaderPreview() {
    ResidTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            LoginHeader(isCompact = false, isDarkMode = true, onToggleTheme = {})
        }
    }
}

@Preview
@Composable
fun LoginFormPreview() {
    ResidTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            LoginForm(
                email = "demo@residmanager.fr",
                password = "Password123!",
                passwordVisible = false,
                isLoading = false,
                errorMessage = null,
                onEmailChanged = {},
                onPasswordChanged = {},
                onTogglePasswordVisibility = {},
                onSubmit = {},
                onForgotPasswordClick = {}
            )
        }
    }
}

@Preview
@Composable
fun LoginFooterPreview() {
    ResidTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            LoginFooter(isCompact = false)
        }
    }
}
