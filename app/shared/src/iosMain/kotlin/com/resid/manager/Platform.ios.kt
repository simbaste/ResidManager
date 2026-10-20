package com.resid.manager

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getBaseUrl(): String = "https://residmanager-api-1043005566320.europe-west1.run.app"

class IOSSessionStorage : SessionStorage {
    private val tokenKey = "jwt_token"
    private val refreshTokenKey = "jwt_refresh_token"
    private val userFnameKey = "user_fname"
    private val userLNameKey = "user_lname"
    private val userAvatarUrlKey = "user_avatar_url"
    private val lastSelectedResidenceKey = "last_selected_residence_id"

    private val userDefaults = platform.Foundation.NSUserDefaults.standardUserDefaults

    override fun saveSession(token: String, refreshToken: String?, fName: String, lName: String, avatarUrl: String?) {
        userDefaults.setObject(token, forKey = tokenKey)
        if (refreshToken != null) {
            userDefaults.setObject(refreshToken, forKey = refreshTokenKey)
        } else {
            userDefaults.removeObjectForKey(refreshTokenKey)
        }
        userDefaults.setObject(fName, forKey = userFnameKey)
        userDefaults.setObject(lName, forKey = userLNameKey)
        if (avatarUrl != null) {
            userDefaults.setObject(avatarUrl, forKey = userAvatarUrlKey)
        } else {
            userDefaults.removeObjectForKey(userAvatarUrlKey)
        }
    }

    override fun loadSession(): SessionData? {
        val token = userDefaults.stringForKey(tokenKey)
        val refreshToken = userDefaults.stringForKey(refreshTokenKey)
        val fName = userDefaults.stringForKey(userFnameKey)
        val lName = userDefaults.stringForKey(userLNameKey) ?: ""
        val avatarUrl = userDefaults.stringForKey(userAvatarUrlKey)
        if (token != null && fName != null) {
            return SessionData(token, refreshToken, fName, lName, avatarUrl)
        }
        return null
    }

    override fun updateTokens(token: String, refreshToken: String?) {
        userDefaults.setObject(token, forKey = tokenKey)
        if (refreshToken != null) {
            userDefaults.setObject(refreshToken, forKey = refreshTokenKey)
        }
    }

    override fun updateAvatarUrl(avatarUrl: String?) {
        if (avatarUrl != null) {
            userDefaults.setObject(avatarUrl, forKey = userAvatarUrlKey)
        } else {
            userDefaults.removeObjectForKey(userAvatarUrlKey)
        }
    }

    override fun clearSession() {
        userDefaults.removeObjectForKey(tokenKey)
        userDefaults.removeObjectForKey(refreshTokenKey)
        userDefaults.removeObjectForKey(userFnameKey)
        userDefaults.removeObjectForKey(userLNameKey)
        userDefaults.removeObjectForKey(userAvatarUrlKey)
        userDefaults.removeObjectForKey(lastSelectedResidenceKey)
    }

    override fun saveLastSelectedResidenceId(residenceId: String) {
        userDefaults.setObject(residenceId, forKey = lastSelectedResidenceKey)
    }

    override fun loadLastSelectedResidenceId(): String? {
        return userDefaults.stringForKey(lastSelectedResidenceKey)
    }
}

actual fun createPlatformSessionStorage(): SessionStorage? = IOSSessionStorage()

actual fun getDefaultCountryCode(): String = NSLocale.currentLocale.countryCode ?: "FR"

// Reference date for Cocoa is Jan 1, 2001 (978307200 seconds after Jan 1, 1970)
actual fun getCurrentEpochMillis(): Long {
    val secondsSinceReferenceDate = platform.Foundation.NSDate().timeIntervalSinceReferenceDate()
    return ((secondsSinceReferenceDate + 978307200.0) * 1000.0).toLong()
}






