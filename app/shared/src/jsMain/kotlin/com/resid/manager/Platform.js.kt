package com.resid.manager

import kotlinx.browser.window
import web.navigator.navigator

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

class WebSessionStorage : SessionStorage {
    private val tokenKey = "jwt_token"
    private val userFnameKey = "user_fname"
    private val userLNameKey = "user_lname"

    override fun saveSession(token: String, fName: String, lName: String) {
        kotlinx.browser.localStorage.setItem(tokenKey, token)
        kotlinx.browser.localStorage.setItem(userFnameKey, fName)
        kotlinx.browser.localStorage.setItem(userLNameKey, lName)
    }

    override fun loadSession(): Triple<String, String, String>? {
        val token = kotlinx.browser.localStorage.getItem(tokenKey)
        val fName = kotlinx.browser.localStorage.getItem(userFnameKey)
        val lName = kotlinx.browser.localStorage.getItem(userLNameKey) ?: ""
        if (token != null && fName != null) {
            return Triple(token, fName, lName)
        }
        return null
    }

    override fun clearSession() {
        kotlinx.browser.localStorage.removeItem(tokenKey)
        kotlinx.browser.localStorage.removeItem(userFnameKey)
        kotlinx.browser.localStorage.removeItem(userLNameKey)
    }
}

actual fun createPlatformSessionStorage(): SessionStorage? = WebSessionStorage()

