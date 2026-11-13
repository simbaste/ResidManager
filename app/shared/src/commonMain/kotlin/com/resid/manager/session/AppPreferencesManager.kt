package com.resid.manager.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferencesManager {
    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _language = MutableStateFlow("fr")
    val language: StateFlow<String> = _language.asStateFlow()

    fun toggleDarkMode() {
        _darkMode.value = !_darkMode.value
    }

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
    }

    fun setLanguage(lang: String) {
        _language.value = lang
    }
}
