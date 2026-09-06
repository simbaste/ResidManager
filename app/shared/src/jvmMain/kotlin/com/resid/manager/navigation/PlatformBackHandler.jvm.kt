package com.resid.manager.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop / JVM doesn't have an operating system back gesture by default
}
