package com.resid.manager.features.residences.di

import com.resid.manager.SessionStorage
import com.resid.manager.features.residences.ResidencesViewModel
import com.resid.manager.features.residences.usecase.CreateResidenceUseCase
import com.resid.manager.features.residences.usecase.DeleteResidenceUseCase
import com.resid.manager.features.residences.usecase.FetchResidencePhotosUseCase
import com.resid.manager.features.residences.usecase.FetchResidencesUseCase
import com.resid.manager.features.residences.usecase.JoinResidenceUseCase
import com.resid.manager.features.residences.usecase.UpdateResidencePhotosUseCase
import com.resid.manager.features.residences.usecase.UpdateResidenceUseCase
import org.koin.dsl.module

val residencesFeatureModule = module {
    factory { FetchResidencesUseCase(get()) }
    factory { FetchResidencePhotosUseCase(get()) }
    factory { CreateResidenceUseCase(get()) }
    factory { UpdateResidenceUseCase(get()) }
    factory { UpdateResidencePhotosUseCase(get(), get()) }
    factory { DeleteResidenceUseCase(get()) }
    factory { JoinResidenceUseCase() }

    single {
        ResidencesViewModel(
            fetchResidencesUseCase = get(),
            fetchResidencePhotosUseCase = get(),
            createResidenceUseCase = get(),
            updateResidenceUseCase = get(),
            updateResidencePhotosUseCase = get(),
            deleteResidenceUseCase = get(),
            searchResidencesUseCase = get(),
            joinResidenceUseCase = get(),
            sessionStorage = getOrNull<SessionStorage>()
        )
    }
}
