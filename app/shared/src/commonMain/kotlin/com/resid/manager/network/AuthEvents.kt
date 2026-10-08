package com.resid.manager.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AuthEvents {
    private val _onUnauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val onUnauthorized = _onUnauthorized.asSharedFlow()

    fun emitUnauthorized() {
        _onUnauthorized.tryEmit(Unit)
    }
}
