package com.resid.manager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.window
import org.w3c.dom.events.Event

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    if (!enabled) return

    DisposableEffect(Unit) {
        // Push a state into browser history so that the 'Back' button triggers popstate
        window.history.pushState(null, "", window.location.href)

        val listener: (Event) -> Unit = {
            onBack()
        }

        window.addEventListener("popstate", listener)

        onDispose {
            window.removeEventListener("popstate", listener)
        }
    }
}
