package com.resid.manager

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getBaseUrl(): String = "https://residmanager-api-1043005566320.europe-west1.run.app"

object AndroidAppContextHolder {
    var appContext: Context? = null
}

class AndroidSessionStorage(private val context: Context) : SessionStorage {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("resid_manager_prefs", Context.MODE_PRIVATE)
    }

    private val tokenKey = "jwt_token"
    private val refreshTokenKey = "jwt_refresh_token"
    private val userFnameKey = "user_fname"
    private val userLNameKey = "user_lname"
    private val lastSelectedResidenceKey = "last_selected_residence_id"

    override fun saveSession(token: String, refreshToken: String?, fName: String, lName: String) {
        prefs.edit {
            putString(tokenKey, token)
            if (refreshToken != null) {
                putString(refreshTokenKey, refreshToken)
            } else {
                remove(refreshTokenKey)
            }
            putString(userFnameKey, fName)
            putString(userLNameKey, lName)
        }
    }

    override fun loadSession(): SessionData? {
        val token = prefs.getString(tokenKey, null)
        val refreshToken = prefs.getString(refreshTokenKey, null)
        val fName = prefs.getString(userFnameKey, null)
        val lName = prefs.getString(userLNameKey, "") ?: ""
        if (token != null && fName != null) {
            return SessionData(token, refreshToken, fName, lName)
        }
        return null
    }

    override fun updateTokens(token: String, refreshToken: String?) {
        prefs.edit {
            putString(tokenKey, token)
            if (refreshToken != null) {
                putString(refreshTokenKey, refreshToken)
            }
        }
    }

    override fun clearSession() {
        prefs.edit {
            remove(tokenKey)
                .remove(refreshTokenKey)
                .remove(userFnameKey)
                .remove(userLNameKey)
                .remove(lastSelectedResidenceKey)
        }
    }

    override fun saveLastSelectedResidenceId(residenceId: String) {
        prefs.edit {
            putString(lastSelectedResidenceKey, residenceId)
        }
    }

    override fun loadLastSelectedResidenceId(): String? {
        return prefs.getString(lastSelectedResidenceKey, null)
    }
}

actual fun createPlatformSessionStorage(): SessionStorage? {
    return AndroidAppContextHolder.appContext?.let { AndroidSessionStorage(it) }
}

actual fun getDefaultCountryCode(): String = java.util.Locale.getDefault().country.ifBlank { "FR" }

actual fun getCurrentEpochMillis(): Long = System.currentTimeMillis()



