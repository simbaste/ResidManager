package com.resid.manager

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getBaseUrl(): String

expect fun getDefaultCountryCode(): String

expect fun getCurrentEpochMillis(): Long



