package com.resid.manager

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.localStorage

class WebSessionStorage : SessionStorage {
    private val tokenKey = "jwt_token"
    private val userKey = "user_name"

    override fun saveSession(token: String, userName: String) {
        localStorage.setItem(tokenKey, token)
        localStorage.setItem(userKey, userName)
    }

    override fun loadSession(): Pair<String, String>? {
        val token = localStorage.getItem(tokenKey)
        val userName = localStorage.getItem(userKey)
        if (token != null && userName != null) {
            return Pair(token, userName)
        }
        return null
    }

    override fun clearSession() {
        localStorage.removeItem(tokenKey)
        localStorage.removeItem(userKey)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    com.resid.manager.di.initKoinHelper()
    ComposeViewport {
        App(sessionStorage = WebSessionStorage())
    }
}
