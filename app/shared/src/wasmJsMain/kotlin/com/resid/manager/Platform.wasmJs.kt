package com.resid.manager

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun getBaseUrl(): String = "https://residmanager-api-1043005566320.europe-west1.run.app"

actual fun createPlatformSessionStorage(): SessionStorage? = null

actual fun getDefaultCountryCode(): String = "FR"

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
actual fun getCurrentEpochMillis(): Long = js("Date.now()")





