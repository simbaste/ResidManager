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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.collectAsState
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
import com.resid.manager.viewmodel.LoginViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinInject(),
    onNavigateToRegister: () -> Unit = { viewModel.navigateToRegister() }
) {
    val uiState by viewModel.uiState.collectAsState()
    var rememberMe by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val colorScheme = MaterialTheme.colorScheme
    val residColors = MaterialTheme.residColors

    // All tokens are now provided directly by the Theme without any manual `if` branching!
    val bgColor = colorScheme.background
    val cardBg = residColors.cardBackground
    val cardBorder = residColors.cardBorder
    val brandPrimary = colorScheme.primary
    val brandPrimaryContainer = colorScheme.primaryContainer
    val onPrimaryColor = colorScheme.onPrimary
    val textColorPrimary = colorScheme.onSurface
    val textColorSecondary = colorScheme.onSurfaceVariant
    val inputContainerBg = residColors.inputBackground
    val inputBorderColor = residColors.inputBorder
    val dividerColor = residColors.dividerColor

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
        val isLarge = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

        // Atmospheric Background: Luxury Ambient Glows + Subtle Radial Grid Pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
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
            // ==========================================
            // 1. TOP BAR: Brand Identity, Theme Switcher & Quick Help
            // ==========================================
            Row(
                modifier = Modifier
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
                            .shadow(12.dp, RoundedCornerShape(14.dp), ambientColor = brandPrimary, spotColor = brandPrimary)
                            .background(
                                brush = Brush.linearGradient(listOf(brandPrimary, brandPrimaryContainer)),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(1.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(cardBg, RoundedCornerShape(12.5.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🏢",
                                fontSize = 20.sp
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
                                color = textColorPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isCompact) 18.sp else 20.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(brandPrimary, CircleShape)
                            )
                        }
                        Text(
                            text = "GESTION IMMOBILIÈRE",
                            color = brandPrimary,
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
                    // Dark / Light Theme Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(inputContainerBg, CircleShape)
                            .border(1.dp, inputBorderColor, CircleShape)
                    ) {
                        Text(
                            text = if (uiState.darkMode) "☀️" else "🌙",
                            fontSize = 16.sp
                        )
                    }

                    if (!isCompact) {
                        Text(
                            text = "Centre d'assistance",
                            color = textColorSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 40.dp))

            // ==========================================
            // 2. MAIN LOGIN CARD (Centered & Responsive)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = residColors.cardShadowElevation),
                        spotColor = brandPrimary.copy(alpha = 0.2f)
                    )
                    .background(
                        color = cardBg,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = cardBorder,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(if (isCompact) 20.dp else 36.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Lock / Icon Badge
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(brandPrimary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .border(1.dp, brandPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔒", fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Resid Manager - Connexion",
                        color = textColorPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompact) 22.sp else 26.sp,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Accédez à votre espace de gestion résidentielle et patrimoine",
                        color = textColorSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Social SSO Button: Continuer avec Google
                    OutlinedButton(
                        onClick = { /* Future SSO integration */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = inputContainerBg,
                            contentColor = textColorPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, inputBorderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🌐", fontSize = 16.sp)
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
                            color = dividerColor
                        )
                        Text(
                            text = "OU AVEC EMAIL",
                            color = textColorSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = dividerColor
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Field 1: Email
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ADRESSE EMAIL",
                            color = textColorPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = { viewModel.onEmailChanged(it) },
                            placeholder = { Text("nom@residence.fr", color = textColorSecondary.copy(alpha = 0.6f), fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColorPrimary,
                                unfocusedTextColor = textColorPrimary,
                                focusedContainerColor = inputContainerBg,
                                unfocusedContainerColor = inputContainerBg,
                                focusedBorderColor = brandPrimary,
                                unfocusedBorderColor = inputBorderColor,
                                cursorColor = brandPrimary
                            ),
                            leadingIcon = {
                                Text(text = "✉️", fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
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
                            color = textColorPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        OutlinedTextField(
                            value = uiState.passwordPlain,
                            onValueChange = { viewModel.onPasswordChanged(it) },
                            placeholder = { Text("••••••••••••", color = textColorSecondary.copy(alpha = 0.6f), fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColorPrimary,
                                unfocusedTextColor = textColorPrimary,
                                focusedContainerColor = inputContainerBg,
                                unfocusedContainerColor = inputContainerBg,
                                focusedBorderColor = brandPrimary,
                                unfocusedBorderColor = inputBorderColor,
                                cursorColor = brandPrimary
                            ),
                            visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Text(text = "🔑", fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                            },
                            trailingIcon = {
                                TextButton(
                                    onClick = { viewModel.togglePasswordVisibility() },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = if (uiState.passwordVisible) "Masquer" else "Afficher",
                                        color = brandPrimary,
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
                                    if (!uiState.isLoading) viewModel.login()
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
                                    checkedColor = brandPrimary,
                                    uncheckedColor = inputBorderColor,
                                    checkmarkColor = onPrimaryColor
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Se souvenir de moi",
                                color = textColorPrimary,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "Mot de passe oublié ?",
                            color = brandPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { /* Forgot password action */ }
                        )
                    }

                    // Error Message display
                    val error = uiState.errorMessage
                    if (!error.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colorScheme.errorContainer.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .border(1.dp, colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = error,
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
                            viewModel.login()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = brandPrimary,
                                spotColor = brandPrimaryContainer
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(listOf(brandPrimary, brandPrimaryContainer)),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color = onPrimaryColor,
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
                                        color = onPrimaryColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "➜", color = onPrimaryColor, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Registration Switch Link
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pas encore de compte ?",
                            color = textColorSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "S'inscrire",
                            color = brandPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToRegister() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Text(text = "🛡️", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Connexion sécurisée SSL 256-bit • Données hébergées en France",
                    color = textColorSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 36.dp))

            // ==========================================
            // 3. FOOTER: Copyright & Legal
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1200.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "© 2026 Resid Manager. Système de gestion de patrimoine immobilier.",
                        color = textColorSecondary,
                        fontSize = 11.sp,
                        textAlign = if (isCompact) TextAlign.Center else TextAlign.Start
                    )

                    if (!isCompact) {
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text(text = "Confidentialité", color = textColorSecondary, fontSize = 11.sp)
                            Text(text = "Mentions légales", color = textColorSecondary, fontSize = 11.sp)
                            Text(text = "Contact", color = textColorSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    ResidTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Render basic preview shell
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Resid Manager - Connexion (Preview)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

