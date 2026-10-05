package com.resid.manager

interface SessionStorage {
    fun saveSession(token: String, fName: String, lName: String)
    fun loadSession(): Triple<String, String, String>?
    fun clearSession()
    fun saveLastSelectedResidenceId(residenceId: String)
    fun loadLastSelectedResidenceId(): String?
}

/**
 * Returns the platform-specific implementation of SessionStorage.
 */
expect fun createPlatformSessionStorage(): SessionStorage?

