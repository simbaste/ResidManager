package com.resid.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.resid.manager.ui.AppShell
import com.resid.manager.ui.LoginScreen
import com.resid.manager.ui.RegisterScreen
import com.resid.manager.viewmodel.AuthScreen
import com.resid.manager.viewmodel.LoginViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
@Preview
fun App(sessionStorage: SessionStorage? = null) {
    MaterialTheme {
        // Resolve ViewModel via Koin DI with sessionStorage parameter
        val viewModel: LoginViewModel = koinInject { parametersOf(sessionStorage) }
        val uiState by viewModel.uiState.collectAsState()

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
        ) {
            when (uiState.currentScreen) {
                AuthScreen.LOGIN -> {
                    LoginScreen(viewModel = viewModel)
                }
                AuthScreen.REGISTER -> {
                    RegisterScreen(viewModel = viewModel)
                }
                AuthScreen.MAIN -> {
                    AppShell(viewModel = viewModel)
                }
            }
        }
    }
}
