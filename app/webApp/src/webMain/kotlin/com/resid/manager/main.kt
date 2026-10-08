package com.resid.manager

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    com.resid.manager.di.initKoinHelper()
    ComposeViewport {
        App()
    }
}
