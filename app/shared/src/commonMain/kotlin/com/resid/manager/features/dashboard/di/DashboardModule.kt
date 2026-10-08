package com.resid.manager.features.dashboard.di

import com.resid.manager.features.dashboard.DashboardViewModel
import com.resid.manager.features.dashboard.data.DashboardRepository
import com.resid.manager.features.dashboard.data.DashboardRepositoryImpl
import com.resid.manager.features.dashboard.usecase.GetDashboardDataUseCase
import org.koin.dsl.module

val dashboardFeatureModule = module {
    single<DashboardRepository> { DashboardRepositoryImpl() }
    factory { GetDashboardDataUseCase(get()) }
    single { DashboardViewModel(get()) }
}
