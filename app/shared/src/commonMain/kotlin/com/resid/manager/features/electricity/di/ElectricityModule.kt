package com.resid.manager.features.electricity.di

import com.resid.manager.features.electricity.ElectricityViewModel
import com.resid.manager.features.electricity.data.ElectricityRepository
import com.resid.manager.features.electricity.data.ElectricityRepositoryImpl
import com.resid.manager.features.electricity.usecase.CreateElectricityStatementUseCase
import com.resid.manager.features.electricity.usecase.FetchElectricityStatementsUseCase
import com.resid.manager.features.electricity.usecase.FetchPreviousIndexUseCase
import com.resid.manager.features.electricity.usecase.MarkStatementPaidUseCase
import org.koin.dsl.module

val electricityFeatureModule = module {
    single<ElectricityRepository> { ElectricityRepositoryImpl() }
    factory { FetchElectricityStatementsUseCase(get()) }
    factory { FetchPreviousIndexUseCase(get()) }
    factory { CreateElectricityStatementUseCase(get()) }
    factory { MarkStatementPaidUseCase(get()) }

    single {
        ElectricityViewModel(
            fetchElectricityStatementsUseCase = get(),
            fetchPreviousIndexUseCase = get(),
            createElectricityStatementUseCase = get(),
            markStatementPaidUseCase = get()
        )
    }
}
