package com.resid.manager.helpers

fun <T> tryOptional(block: () -> T): T? {
    return try {
        block()
    } catch (_: Exception) {
        null
    }
}

suspend fun <T> tryOptionalSuspend(block: suspend  () -> T): T? {
    return try {
        block()
    } catch (_: Exception) {
        null
    }
}
