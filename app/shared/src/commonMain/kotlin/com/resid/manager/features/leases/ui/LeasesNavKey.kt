package com.resid.manager.features.leases.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface LeasesNavKey : NavKey {
    @Serializable
    data object List : LeasesNavKey

    @Serializable
    data class Detail(val leaseId: String) : LeasesNavKey
}
