package com.resid.manager.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.resid.manager.features.leases.ui.LeasesNavKey
import com.resid.manager.features.residences.ui.ResidencesNavKey
import com.resid.manager.features.units.ui.UnitsNavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * SerializersModule registering every polymorphic subclass of NavKey.
 * This guarantees type-safe state saving and restoration on all KMP platforms without reflection.
 */
val AppSerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(AppNavKey.Login::class, AppNavKey.Login.serializer())
        subclass(AppNavKey.Register::class, AppNavKey.Register.serializer())
        subclass(AppNavKey.Main::class, AppNavKey.Main.serializer())
        subclass(AppNavKey.Dashboard::class, AppNavKey.Dashboard.serializer())
        subclass(AppNavKey.Residences::class, AppNavKey.Residences.serializer())
        subclass(AppNavKey.Units::class, AppNavKey.Units.serializer())
        subclass(AppNavKey.Leases::class, AppNavKey.Leases.serializer())
        subclass(AppNavKey.Members::class, AppNavKey.Members.serializer())
        subclass(AppNavKey.Electricity::class, AppNavKey.Electricity.serializer())
        subclass(AppNavKey.Tickets::class, AppNavKey.Tickets.serializer())
        subclass(AppNavKey.Finances::class, AppNavKey.Finances.serializer())
        subclass(AppNavKey.Profile::class, AppNavKey.Profile.serializer())
        subclass(AppNavKey.CreateUnit::class, AppNavKey.CreateUnit.serializer())
        subclass(AppNavKey.CreateResidence::class, AppNavKey.CreateResidence.serializer())

        // Feature sub-routes: Residences
        subclass(ResidencesNavKey.Directory::class, ResidencesNavKey.Directory.serializer())
        subclass(ResidencesNavKey.Detail::class, ResidencesNavKey.Detail.serializer())
        subclass(ResidencesNavKey.Create::class, ResidencesNavKey.Create.serializer())
        subclass(ResidencesNavKey.Gallery::class, ResidencesNavKey.Gallery.serializer())

        // Feature sub-routes: Units
        subclass(UnitsNavKey.List::class, UnitsNavKey.List.serializer())
        subclass(UnitsNavKey.Detail::class, UnitsNavKey.Detail.serializer())
        subclass(UnitsNavKey.Create::class, UnitsNavKey.Create.serializer())

        // Feature sub-routes: Leases
        subclass(LeasesNavKey.List::class, LeasesNavKey.List.serializer())
        subclass(LeasesNavKey.Detail::class, LeasesNavKey.Detail.serializer())
    }
}

/**
 * Default [SavedStateConfiguration] configured with [AppSerializersModule].
 */
val DefaultSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = AppSerializersModule
}
