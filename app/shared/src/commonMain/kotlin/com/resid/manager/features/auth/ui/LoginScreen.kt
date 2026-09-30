package com.resid.manager.features.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.resid.manager.features.auth.AuthViewModel
import com.resid.manager.features.auth.mvi.AuthEffect
import com.resid.manager.features.auth.mvi.AuthIntent
import com.resid.manager.features.auth.mvi.AuthUiState
import com.resid.manager.features.auth.ui.components.login.LoginAtmosphericBackground
import com.resid.manager.features.auth.ui.components.login.LoginCardHeader
import com.resid.manager.features.auth.ui.components.login.LoginFooter
import com.resid.manager.features.auth.ui.components.login.LoginForm
import com.resid.manager.features.auth.ui.components.login.LoginHeader
import com.resid.manager.features.auth.ui.components.login.LoginRegisterOption
import com.resid.manager.features.auth.ui.components.login.LoginSecurityBadge
import com.resid.manager.ui.theme.residColors
import org.koin.compose.koinInject

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = koinInject(),
    onNavigateToRegister: () -> Unit = {},
    onNavigateToMain: (String) -> Unit = {},
    onToggleTheme: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToMain -> onNavigateToMain(effect.token)
                is AuthEffect.NavigateToRegister -> onNavigateToRegister()
                else -> {}
            }
        }
    }

    LoginContent(
        uiState = uiState,
        onIntent = { intent ->
            if (intent is AuthIntent.ToggleTheme) {
                onToggleTheme()
            }
            viewModel.onIntent(intent)
        },
        onNavigateToRegister = onNavigateToRegister
    )
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LoginContent(
    uiState: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
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
                onToggleTheme = { onIntent(AuthIntent.ToggleTheme) }
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
                            onEmailChanged = { onIntent(AuthIntent.EmailChanged(it)) },
                            onPasswordChanged = { onIntent(AuthIntent.PasswordChanged(it)) },
                            onTogglePasswordVisibility = { onIntent(AuthIntent.TogglePasswordVisibility) },
                            onSubmit = { onIntent(AuthIntent.SubmitLogin) },
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
