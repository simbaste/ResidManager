package com.resid.manager.features.residences.di

import com.resid.manager.features.residences.ResidencesViewModel
import com.resid.manager.features.residences.usecase.CreateResidenceUseCase
import com.resid.manager.features.residences.usecase.DeleteResidenceUseCase
import com.resid.manager.features.residences.usecase.FetchResidencesUseCase
import com.resid.manager.features.residences.usecase.JoinResidenceUseCase
import com.resid.manager.features.residences.usecase.UpdateResidenceUseCase
import org.koin.dsl.module

val residencesFeatureModule = module {
    factory { FetchResidencesUseCase(get()) }
    factory { CreateResidenceUseCase(get()) }
    factory { UpdateResidenceUseCase(get()) }
    factory { DeleteResidenceUseCase(get()) }
    factory { JoinResidenceUseCase() }

    single {
        ResidencesViewModel(
            fetchResidencesUseCase = get(),
            createResidenceUseCase = get(),
            updateResidenceUseCase = get(),
            deleteResidenceUseCase = get(),
            searchResidencesUseCase = get(),
            joinResidenceUseCase = get()
        )
    }
}
