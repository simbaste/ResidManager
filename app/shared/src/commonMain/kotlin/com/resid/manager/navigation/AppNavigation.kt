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

    @Serializable
    data object Dashboard : AppNavKey

    @Serializable
    data object Residences : AppNavKey

    @Serializable
    data object Units : AppNavKey

    @Serializable
    data object Leases : AppNavKey

    @Serializable
    data object Members : AppNavKey

    @Serializable
    data object Electricity : AppNavKey

    @Serializable
    data object Tickets : AppNavKey

    @Serializable
    data object Finances : AppNavKey

    @Serializable
    data object Profile : AppNavKey
}

/**
 * Platform-agnostic back handler (interacts with browser history popstate on web/js and Android system back).
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
