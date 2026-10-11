package com.resid.manager

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
    private val lastSelectedResidenceKey = "last_selected_residence_id"

    override fun saveSession(token: String, refreshToken: String?, fName: String, lName: String) {
        kotlinx.browser.localStorage.setItem(tokenKey, token)
        if (refreshToken != null) {
            kotlinx.browser.localStorage.setItem(refreshTokenKey, refreshToken)
        } else {
            kotlinx.browser.localStorage.removeItem(refreshTokenKey)
        }
        kotlinx.browser.localStorage.setItem(userFnameKey, fName)
        kotlinx.browser.localStorage.setItem(userLNameKey, lName)
    }

    override fun loadSession(): SessionData? {
        val token = kotlinx.browser.localStorage.getItem(tokenKey)
        val refreshToken = kotlinx.browser.localStorage.getItem(refreshTokenKey)
        val fName = kotlinx.browser.localStorage.getItem(userFnameKey)
        val lName = kotlinx.browser.localStorage.getItem(userLNameKey) ?: ""
        if (token != null && fName != null) {
            return SessionData(token, refreshToken, fName, lName)
        }
        return null
    }

    override fun updateTokens(token: String, refreshToken: String?) {
        kotlinx.browser.localStorage.setItem(tokenKey, token)
        if (refreshToken != null) {
            kotlinx.browser.localStorage.setItem(refreshTokenKey, refreshToken)
        }
    }

    override fun clearSession() {
        kotlinx.browser.localStorage.removeItem(tokenKey)
        kotlinx.browser.localStorage.removeItem(refreshTokenKey)
        kotlinx.browser.localStorage.removeItem(userFnameKey)
        kotlinx.browser.localStorage.removeItem(userLNameKey)
        kotlinx.browser.localStorage.removeItem(lastSelectedResidenceKey)
    }

    override fun saveLastSelectedResidenceId(residenceId: String) {
        kotlinx.browser.localStorage.setItem(lastSelectedResidenceKey, residenceId)
    }

    override fun loadLastSelectedResidenceId(): String? {
        return kotlinx.browser.localStorage.getItem(lastSelectedResidenceKey)
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

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun jsDateNow(): Double = js("Date.now()")

actual fun getCurrentEpochMillis(): Long = jsDateNow().toLong()






