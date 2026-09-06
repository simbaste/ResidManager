package com.resid.manager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppNavKey : NavKey {
    @Serializable
    data object Login : AppNavKey

    @Serializable
    data object Register : AppNavKey

    @Serializable
    data object Main : AppNavKey
}

/**
 * Platform-agnostic back handler (interacts with browser history popstate on web/js and Android system back).
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
