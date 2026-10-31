package com.resid.manager

import kotlinx.browser.localStorage

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun getBaseUrl(): String {
    val hostname = kotlinx.browser.window.location.hostname
    return if (hostname == "localhost" || hostname == "127.0.0.1" || hostname == "0.0.0.0") {
        "http://localhost:8081"
    } else {
        "https://residmanager-api-1043005566320.europe-west1.run.app"
    }
}

class WasmSessionStorage : SessionStorage {
    private val tokenKey = "jwt_token"
    private val refreshTokenKey = "jwt_refresh_token"
    private val userFnameKey = "user_fname"
    private val userLNameKey = "user_lname"
    private val userAvatarUrlKey = "user_avatar_url"
    private val lastSelectedResidenceKey = "last_selected_residence_id"

    override fun saveSession(token: String, refreshToken: String?, fName: String, lName: String, avatarUrl: String?) {
        localStorage.setItem(tokenKey, token)
        if (refreshToken != null) {
            localStorage.setItem(refreshTokenKey, refreshToken)
        } else {
            localStorage.removeItem(refreshTokenKey)
        }
        localStorage.setItem(userFnameKey, fName)
        localStorage.setItem(userLNameKey, lName)
        if (avatarUrl != null) {
            kotlinx.browser.localStorage.setItem(userAvatarUrlKey, avatarUrl)
        } else {
            kotlinx.browser.localStorage.removeItem(userAvatarUrlKey)
        }
    }

    override fun loadSession(): SessionData? {
        val token = localStorage.getItem(tokenKey)
        val refreshToken = localStorage.getItem(refreshTokenKey)
        val fName = localStorage.getItem(userFnameKey)
        val lName = localStorage.getItem(userLNameKey) ?: ""
        val avatarUrl = localStorage.getItem(userAvatarUrlKey)
        if (token != null && fName != null) {
            return SessionData(token, refreshToken, fName, lName, avatarUrl)
        }
        return null
    }

    override fun updateTokens(token: String, refreshToken: String?) {
        localStorage.setItem(tokenKey, token)
        if (refreshToken != null) {
            localStorage.setItem(refreshTokenKey, refreshToken)
        }
    }

    override fun updateAvatarUrl(avatarUrl: String?) {
        if (avatarUrl != null) {
            localStorage.setItem(userAvatarUrlKey, avatarUrl)
        } else {
            localStorage.removeItem(userAvatarUrlKey)
        }
    }

    override fun clearSession() {
        localStorage.removeItem(tokenKey)
        localStorage.removeItem(refreshTokenKey)
        localStorage.removeItem(userFnameKey)
        localStorage.removeItem(userLNameKey)
        localStorage.removeItem(userAvatarUrlKey)
        localStorage.removeItem(lastSelectedResidenceKey)
    }

    override fun saveLastSelectedResidenceId(residenceId: String) {
        localStorage.setItem(lastSelectedResidenceKey, residenceId)
    }

    override fun loadLastSelectedResidenceId(): String? {
        return localStorage.getItem(lastSelectedResidenceKey)
    }

    override fun saveResidenceConsulted(residenceId: String, timestamp: Long) {
        localStorage.setItem("residence_consulted_$residenceId", timestamp.toString())
    }

    override fun loadResidenceConsultedTimestamps(): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i) ?: continue
            if (key.startsWith("residence_consulted_")) {
                val resId = key.removePrefix("residence_consulted_")
                val ts = localStorage.getItem(key)?.toLongOrNull()
                if (ts != null) {
                    result[resId] = ts
                }
            }
        }
        return result
    }
}

actual fun createPlatformSessionStorage(): SessionStorage? = WasmSessionStorage()

actual fun getDefaultCountryCode(): String {
    val lang = kotlinx.browser.window.navigator.language // e.g. "fr-FR", "fr-CI", "en-US"
    val parts = lang.split("-", "_")
    return if (parts.size > 1 && parts[1].length == 2) {
        parts[1].uppercase()
    } else {
        "FR"
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsDateNow(): Double = js("Date.now()")

actual fun getCurrentEpochMillis(): Long = jsDateNow().toLong()






