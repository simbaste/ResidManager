package com.resid.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.resid.manager.features.auth.ui.LoginScreen
import com.resid.manager.features.auth.ui.RegisterScreen
import com.resid.manager.navigation.AppNavKey
import com.resid.manager.navigation.PlatformBackHandler
import com.resid.manager.navigation.rememberNavigator
import com.resid.manager.network.AuthEvents
import com.resid.manager.ui.AppShell
import com.resid.manager.ui.i18n.EnStrings
import com.resid.manager.ui.i18n.FrStrings
import com.resid.manager.ui.i18n.LocalStrings
import com.resid.manager.ui.theme.ResidTheme
import com.resid.manager.viewmodel.AuthScreen
import com.resid.manager.viewmodel.LoginViewModel
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val viewModel: LoginViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()

    val activeStrings = when (uiState.language) {
        "en" -> EnStrings
        else -> FrStrings
    }

    val initialKey: AppNavKey = when (uiState.currentScreen) {
        AuthScreen.LOGIN -> AppNavKey.Login
        AuthScreen.REGISTER -> AppNavKey.Register
        AuthScreen.MAIN -> AppNavKey.Main
    }
    val navigator = rememberNavigator(startEntry = initialKey)

    // Keep Navigator backstack synchronized when ViewModel changes currentScreen
    LaunchedEffect(uiState.currentScreen) {
        val targetKey: AppNavKey = when (uiState.currentScreen) {
            AuthScreen.LOGIN -> AppNavKey.Login
            AuthScreen.REGISTER -> AppNavKey.Register
            AuthScreen.MAIN -> AppNavKey.Main
        }
        if (navigator.current() != targetKey) {
            if (targetKey == AppNavKey.Main) {
                navigator.push(AppNavKey.Main, unique = true)
            } else {
                navigator.push(targetKey)
            }
        }
    }

    // Auto-logout when backend returns 401 Unauthorized (e.g. expired JWT token)
    LaunchedEffect(Unit) {
        AuthEvents.onUnauthorized.collect {
            viewModel.logout()
            navigator.push(AppNavKey.Login, unique = true)
        }
    }

    // Platform-native back action (handles browser back button on Web / system back on Android)
    PlatformBackHandler(enabled = navigator.backStack().size > 1) {
        if (navigator.popBack()) {
            when (navigator.current()) {
                AppNavKey.Login -> viewModel.navigateToLogin()
                AppNavKey.Register -> viewModel.navigateToRegister()
                else -> { /* in main flow */ }
            }
        }
    }

    CompositionLocalProvider(LocalStrings provides activeStrings) {
        ResidTheme(darkTheme = uiState.darkMode) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .safeContentPadding()
                    .fillMaxSize()
            ) {
                NavDisplay(
                    backStack = navigator.backStack(),
                    onBack = {
                        navigator.popBack()
                    },
                    entryProvider = { key: NavKey ->
                        when (key) {
                            AppNavKey.Login -> NavEntry(key) {
                                LoginScreen(
                                    onNavigateToRegister = {
                                        navigator.push(AppNavKey.Register)
                                        viewModel.navigateToRegister()
                                    },
                                    onNavigateToMain = { token, user ->
                                        navigator.push(AppNavKey.Main, unique = true)
                                        viewModel.navigateToMain(token = token, user = user)
                                    },
                                    onToggleTheme = viewModel::toggleTheme
                                )
                            }
                            AppNavKey.Register -> NavEntry(key) {
                                RegisterScreen(
                                    onNavigateToLogin = {
                                        navigator.popBack()
                                        viewModel.navigateToLogin()
                                    },
                                    onNavigateToMain = { token, user ->
                                        navigator.push(AppNavKey.Main, unique = true)
                                        viewModel.navigateToMain(token = token, user = user)
                                    },
                                    onToggleTheme = viewModel::toggleTheme
                                )
                            }
                            else -> NavEntry(key) {
                                AppShell(viewModel = viewModel)
                            }
                        }
                    }
                )
            }
        }
    }
}
