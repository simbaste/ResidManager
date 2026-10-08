package com.resid.manager.features.units.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface UnitsNavKey : NavKey {
    @Serializable
    data object List : UnitsNavKey

    @Serializable
    data class Detail(val unitId: String) : UnitsNavKey
}
