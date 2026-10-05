package com.resid.manager.features.auth.ui.components.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.resid.manager.features.auth.ui.components.AuthTopBar

/**
 * Header section: Logo, Brand titles, Theme switcher, and Support Link.
 * Delegates to the unified [AuthTopBar] component.
 */
@Composable
fun LoginHeader(
    isCompact: Boolean,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthTopBar(
        isCompact = isCompact,
        isDarkMode = isDarkMode,
        onToggleTheme = onToggleTheme,
        modifier = modifier
    )
}

