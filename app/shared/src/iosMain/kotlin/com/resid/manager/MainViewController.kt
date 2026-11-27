package com.resid.manager

import androidx.compose.ui.window.ComposeUIViewController
import com.resid.manager.di.initKoinHelper

fun MainViewController(environment: String = "PROD") = ComposeUIViewController(
    configure = {
        currentIOSEnvironment = if (environment.equals("TEST", ignoreCase = true)) {
            AppEnvironment.TEST
        } else {
            AppEnvironment.PROD
        }
        initKoinHelper()
    }
) {
    App()
}