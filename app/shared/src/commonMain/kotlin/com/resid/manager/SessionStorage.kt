package com.resid.manager

interface SessionStorage {
    fun saveSession(token: String, userName: String)
    fun loadSession(): Pair<String, String>?
    fun clearSession()
}
