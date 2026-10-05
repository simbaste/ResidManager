package com.resid.manager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.window
import org.w3c.dom.events.Event

private object BrowserHistoryManager {
    private val activeHandlers = mutableListOf<() -> Unit>()
    private var isListenerAttached = false
    private var isPoppingInternal = false

    private val popStateListener: (Event) -> Unit = {
        if (!isPoppingInternal) {
            // Déclenché par le bouton "Précédent" du navigateur :
            // on exécute uniquement le handler le plus récent (le plus imbriqué)
            val handler = activeHandlers.removeLastOrNull()
            handler?.invoke()
        }
    }

    fun register(handler: () -> Unit) {
        activeHandlers.add(handler)
        window.history.pushState(null, "", window.location.href)

        if (!isListenerAttached) {
            window.addEventListener("popstate", popStateListener)
            isListenerAttached = true
        }
    }

    fun unregister(handler: () -> Unit) {
        val index = activeHandlers.lastIndexOf(handler)
        if (index != -1) {
            activeHandlers.removeAt(index)
            // Si le composant a été quitté programmatiquement (ex: clic sur la flèche retour dans l'UI),
            // on dépile l'entrée d'historique web poussée pour rester synchronisé avec le navigateur.
            isPoppingInternal = true
            window.history.back()
            window.setTimeout(
                {
                    isPoppingInternal = false
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
        BrowserHistoryManager.register(handler)

        onDispose {
            BrowserHistoryManager.unregister(handler)
        }
    }
}
