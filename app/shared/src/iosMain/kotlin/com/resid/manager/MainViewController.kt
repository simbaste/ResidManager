package com.resid.manager

import androidx.compose.ui.window.ComposeUIViewController
import com.resid.manager.di.initKoinHelper

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoinHelper()
    }
) {
    App()
}