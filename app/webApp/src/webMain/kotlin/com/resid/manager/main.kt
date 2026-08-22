package com.resid.manager

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.localStorage

class WebSessionStorage : SessionStorage {
    private val tokenKey = "jwt_token"
    private val userFnameKey = "user_fname"
    private val userLNameKey = "user_lname"

    override fun saveSession(token: String, fName: String, lName: String) {
        localStorage.setItem(tokenKey, token)
        localStorage.setItem(userFnameKey, fName)
        localStorage.setItem(userLNameKey, lName)
    }

    override fun loadSession(): Triple<String, String, String>? {
        val token = localStorage.getItem(tokenKey)
        val fName = localStorage.getItem(userFnameKey)
        val lName = localStorage.getItem(userLNameKey) ?: ""
        if (token != null && fName != null) {
            return Triple(token, fName, lName)
        }
        return null
    }

    override fun clearSession() {
        localStorage.removeItem(tokenKey)
        localStorage.removeItem(userFnameKey)
        localStorage.removeItem(userLNameKey)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    com.resid.manager.di.initKoinHelper()
    ComposeViewport {
        App(sessionStorage = WebSessionStorage())
    }
}
