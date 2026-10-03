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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.resid.manager.features.auth.ui.LoginScreen
import com.resid.manager.features.auth.ui.RegisterScreen
import com.resid.manager.navigation.AppNavKey
import com.resid.manager.navigation.PlatformBackHandler
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

    // Navigation 3 BackStack
    val backStack = remember {
        val initialKey: AppNavKey = when (uiState.currentScreen) {
            AuthScreen.LOGIN -> AppNavKey.Login
            AuthScreen.REGISTER -> AppNavKey.Register
            AuthScreen.MAIN -> AppNavKey.Main
        }
        NavBackStack<AppNavKey>(initialKey)
    }

    // Keep Nav3 backstack synchronized when ViewModel changes currentScreen
    LaunchedEffect(uiState.currentScreen) {
        val targetKey: AppNavKey = when (uiState.currentScreen) {
            AuthScreen.LOGIN -> AppNavKey.Login
            AuthScreen.REGISTER -> AppNavKey.Register
            AuthScreen.MAIN -> AppNavKey.Main
        }
        if (backStack.lastOrNull() != targetKey) {
            if (targetKey == AppNavKey.Main) {
                backStack.clear()
                backStack.add(AppNavKey.Main)
            } else {
                backStack.add(targetKey)
            }
        }
    }

    // Auto-logout when backend returns 401 Unauthorized (e.g. expired JWT token)
    LaunchedEffect(Unit) {
        AuthEvents.onUnauthorized.collect {
            viewModel.logout()
            backStack.clear()
            backStack.add(AppNavKey.Login)
        }
    }

    // Platform-native back action (handles browser back button on Web / system back on Android)
    PlatformBackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()?.let {
            when (backStack.lastOrNull()) {
                AppNavKey.Login -> viewModel.navigateToLogin()
                AppNavKey.Register -> viewModel.navigateToRegister()
                AppNavKey.Main -> { /* in main dashboard */ }
                null -> {}
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
                    backStack = backStack,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    },
                    entryProvider = { key: AppNavKey ->
                        when (key) {
                            AppNavKey.Login -> NavEntry(key) {
                                LoginScreen(
                                    onNavigateToRegister = {
                                        backStack.add(AppNavKey.Register)
                                        viewModel.navigateToRegister()
                                    },
                                    onNavigateToMain = {
                                        backStack.clear()
                                        backStack.add(AppNavKey.Main)
                                        viewModel.navigateToMain()
                                    },
                                    onToggleTheme = viewModel::toggleTheme
                                )
                            }
                            AppNavKey.Register -> NavEntry(key) {
                                RegisterScreen(
                                    onNavigateToLogin = {
                                        backStack.removeLastOrNull()
                                        viewModel.navigateToLogin()
                                    },
                                    onNavigateToMain = {
                                        backStack.clear()
                                        backStack.add(AppNavKey.Main)
                                        viewModel.navigateToMain()
                                    },
                                    onToggleTheme = viewModel::toggleTheme
                                )
                            }
                            AppNavKey.Main -> NavEntry(key) {
                                AppShell(viewModel = viewModel)
                            }
                        }
                    }
                )
            }
        }
    }
}
