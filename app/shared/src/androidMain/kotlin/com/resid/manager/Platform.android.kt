package com.resid.manager

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getBaseUrl(): String = "https://residmanager-api-1043005566320.europe-west1.run.app"
