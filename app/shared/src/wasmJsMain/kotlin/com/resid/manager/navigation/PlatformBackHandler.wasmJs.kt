package com.resid.manager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.window
import org.w3c.dom.events.Event

private object WasmBrowserHistoryManager {
    private val activeHandlers = mutableListOf<() -> Unit>()
    private var isListenerAttached = false
    private var isPoppingInternal = false

    private val popStateListener: (Event) -> Unit = {
        if (!isPoppingInternal) {
            val handler = activeHandlers.removeLastOrNull()
            handler?.invoke()
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    fun register(handler: () -> Unit) {
        activeHandlers.add(handler)
        window.history.pushState(null, "", window.location.href)

        if (!isListenerAttached) {
            window.addEventListener("popstate", popStateListener)
            isListenerAttached = true
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    fun unregister(handler: () -> Unit) {
        val index = activeHandlers.lastIndexOf(handler)
        if (index != -1) {
            activeHandlers.removeAt(index)
            isPoppingInternal = true
            window.history.back()
            window.setTimeout(
                {
                    isPoppingInternal = false
                    null
                },
                50,
            )
        }
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        val handler = { currentOnBack() }
        WasmBrowserHistoryManager.register(handler)

        onDispose {
            WasmBrowserHistoryManager.unregister(handler)
        }
    }
}
