package com.resid.manager

import web.navigator.navigator
import kotlinx.browser.window

class JsPlatform: Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf("Chrome", "Firefox", "Safari", "Edge")

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
            ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
            ?: "Unknown"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun getBaseUrl(): String {
    val hostname = window.location.hostname
    return if (hostname == "localhost" || hostname == "127.0.0.1" || hostname == "0.0.0.0") {
        "http://localhost:8081"
    } else {
        "https://residmanager-api-1043005566320.europe-west1.run.app"
    }
}
