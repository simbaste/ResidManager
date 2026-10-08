package com.resid.manager

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getBaseUrl(): String = "https://residmanager-api-1043005566320.europe-west1.run.app"

actual fun createPlatformSessionStorage(): SessionStorage? = null

actual fun getDefaultCountryCode(): String = java.util.Locale.getDefault().country.ifBlank { "FR" }

actual fun getCurrentEpochMillis(): Long = System.currentTimeMillis()



