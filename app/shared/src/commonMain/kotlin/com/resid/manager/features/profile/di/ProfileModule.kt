package com.resid.manager.features.profile.di

import com.resid.manager.features.profile.ProfileViewModel
import com.resid.manager.features.profile.data.ProfileRepository
import com.resid.manager.features.profile.data.ProfileRepositoryImpl
import com.resid.manager.features.profile.usecase.AddTicketCategoryUseCase
import com.resid.manager.features.profile.usecase.FetchResidenceCategoriesUseCase
import com.resid.manager.features.profile.usecase.UpdateResidenceCurrencyUseCase
import com.resid.manager.features.profile.usecase.UpdateTicketCategoryUseCase
import com.resid.manager.features.profile.usecase.UpdateUserProfileUseCase
import org.koin.dsl.module

val profileFeatureModule = module {
    single<ProfileRepository> { ProfileRepositoryImpl() }
    factory { UpdateUserProfileUseCase(get()) }
    factory { FetchResidenceCategoriesUseCase(get()) }
    factory { AddTicketCategoryUseCase(get()) }
    factory { UpdateTicketCategoryUseCase(get()) }
    factory { UpdateResidenceCurrencyUseCase(get()) }

    single {
        ProfileViewModel(
            updateUserProfileUseCase = get(),
            fetchResidenceCategoriesUseCase = get(),
            addTicketCategoryUseCase = get(),
            updateTicketCategoryUseCase = get(),
            updateResidenceCurrencyUseCase = get()
        )
    }
}
