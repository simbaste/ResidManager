package com.resid.manager

data class SessionData(
    val token: String,
    val refreshToken: String?,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String? = null
)

interface SessionStorage {
    fun saveSession(token: String, refreshToken: String?, fName: String, lName: String, avatarUrl: String? = null)
    fun loadSession(): SessionData?
    fun updateTokens(token: String, refreshToken: String?)
    fun updateAvatarUrl(avatarUrl: String?)
    fun clearSession()
    fun saveLastSelectedResidenceId(residenceId: String)
    fun loadLastSelectedResidenceId(): String?
}

/**
 * Returns the platform-specific implementation of SessionStorage.
 */
expect fun createPlatformSessionStorage(): SessionStorage?

