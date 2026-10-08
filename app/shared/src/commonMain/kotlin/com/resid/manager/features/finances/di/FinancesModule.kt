package com.resid.manager.features.finances.di

import com.resid.manager.features.finances.FinancesViewModel
import com.resid.manager.features.finances.data.FinanceRepository
import com.resid.manager.features.finances.data.FinanceRepositoryImpl
import com.resid.manager.features.finances.usecase.FetchTransactionsUseCase
import com.resid.manager.features.finances.usecase.RecordExpenseUseCase
import org.koin.dsl.module

val financesFeatureModule = module {
    single<FinanceRepository> { FinanceRepositoryImpl() }
    factory { FetchTransactionsUseCase(get()) }
    factory { RecordExpenseUseCase(get()) }

    single {
        FinancesViewModel(
            fetchTransactionsUseCase = get(),
            recordExpenseUseCase = get()
        )
    }
}
