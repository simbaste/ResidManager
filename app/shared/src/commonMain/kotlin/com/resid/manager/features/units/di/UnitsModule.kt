package com.resid.manager.features.units.di

import com.resid.manager.features.units.UnitsViewModel
import com.resid.manager.features.units.usecase.CreateUnitUseCase
import com.resid.manager.features.units.usecase.DeleteUnitUseCase
import com.resid.manager.features.units.usecase.FetchUnitPhotosUseCase
import com.resid.manager.features.units.usecase.FetchUnitsUseCase
import com.resid.manager.features.units.usecase.UpdateUnitPhotosUseCase
import com.resid.manager.features.units.usecase.UpdateUnitUseCase
import org.koin.dsl.module

val unitsFeatureModule = module {
    factory { FetchUnitsUseCase(get()) }
    factory { FetchUnitPhotosUseCase(get()) }
    factory { CreateUnitUseCase(get()) }
    factory { UpdateUnitUseCase(get()) }
    factory { UpdateUnitPhotosUseCase(get(), get()) }
    factory { DeleteUnitUseCase(get()) }

    single {
        UnitsViewModel(
            fetchUnitsUseCase = get(),
            fetchUnitPhotosUseCase = get(),
            createUnitUseCase = get(),
            updateUnitUseCase = get(),
            updateUnitPhotosUseCase = get(),
            deleteUnitUseCase = get()
        )
    }
}
