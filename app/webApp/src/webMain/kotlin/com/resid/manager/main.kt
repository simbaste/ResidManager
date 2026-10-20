package com.resid.manager

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    setupGlobalErrorHandling()
    com.resid.manager.di.initKoinHelper()
    ComposeViewport {
        App()
    }
}

/**
 * Configure un gestionnaire d'erreurs global pour capturer les exceptions
 * non interceptées (Kotlin/JS & Kotlin/Wasm), éviter les écrans blancs silencieux
 * et fournir une trace exploitable dans la console et les DevTools.
 */
private fun setupGlobalErrorHandling() {
    window.addEventListener("error") { event ->
        println("[ResidManager Web Error] $event")
        val container = document.body
        if (container != null && document.getElementById("compose-unhandled-error-banner") == null) {
            val banner = document.createElement("div")
            banner.id = "compose-unhandled-error-banner"
            banner.setAttribute(
                "style",
                "position:fixed;bottom:12px;left:12px;right:12px;z-index:999999;" +
                        "background:#fee2e2;color:#991b1b;border:1px solid #ef4444;border-radius:8px;" +
                        "padding:12px 16px;font-family:monospace;font-size:12px;box-shadow:0 4px 6px -1px rgba(0,0,0,0.1);" +
                        "max-height:120px;overflow:auto;white-space:pre-wrap;"
            )
            banner.textContent =
                "Une erreur inattendue est survenue. Ouvrez la console DevTools (F12) pour examiner la pile d'appels."
            container.appendChild(banner)
        }
    }

    window.addEventListener("unhandledrejection") { event ->
        println("[ResidManager Unhandled Promise Rejection] $event")
    }
}


