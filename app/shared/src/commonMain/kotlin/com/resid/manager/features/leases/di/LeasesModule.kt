package com.resid.manager.features.leases.di

import com.resid.manager.SessionStorage
import com.resid.manager.features.leases.LeasesViewModel
import com.resid.manager.features.leases.usecase.CreateLeaseUseCase
import com.resid.manager.features.leases.usecase.DraftLeaseUseCases
import com.resid.manager.features.leases.usecase.FetchLeasesUseCase
import com.resid.manager.features.leases.usecase.RecordLeasePaymentUseCase
import com.resid.manager.features.leases.usecase.UpdateLeaseStatusUseCase
import org.koin.dsl.module

val leasesFeatureModule = module {
    factory { FetchLeasesUseCase(get()) }
    factory { CreateLeaseUseCase(get()) }
    factory { RecordLeasePaymentUseCase(get()) }
    factory { UpdateLeaseStatusUseCase(get()) }
    factory { DraftLeaseUseCases() }

    single {
        LeasesViewModel(
            fetchLeasesUseCase = get(),
            createLeaseUseCase = get(),
            recordLeasePaymentUseCase = get(),
            updateLeaseStatusUseCase = get(),
            fetchResidencesUseCase = get(),
            fetchUnitsUseCase = get(),
            fetchMembersUseCase = get(),
            draftLeaseUseCases = get(),
            sessionStorage = getOrNull<SessionStorage>()
        )
    }
}
