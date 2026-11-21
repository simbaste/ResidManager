package com.resid.manager.features.leases.di

import com.resid.manager.SessionStorage
import com.resid.manager.features.leases.LeasesViewModel
import com.resid.manager.features.leases.usecase.CreateLeaseUseCase
import com.resid.manager.features.leases.usecase.DraftLeaseUseCases
import com.resid.manager.features.leases.usecase.FetchLeasesUseCase
import com.resid.manager.features.leases.usecase.FetchTenantGuarantorsUseCase
import com.resid.manager.features.leases.usecase.FinalizeLeaseUseCase
import com.resid.manager.features.leases.usecase.GetAvailableUnitsUseCase
import com.resid.manager.features.leases.usecase.InitLeaseDraftUseCase
import com.resid.manager.features.leases.usecase.LoadLeaseDraftUseCase
import com.resid.manager.features.leases.usecase.RecordLeasePaymentUseCase
import com.resid.manager.features.leases.usecase.SaveLeaseStepUseCase
import com.resid.manager.features.leases.usecase.SearchUsersUseCase
import com.resid.manager.features.leases.usecase.UpdateLeaseStatusUseCase
import com.resid.manager.features.leases.wizard.LeaseWizardScreenViewModel
import org.koin.dsl.module

val leasesFeatureModule = module {
    factory { FetchLeasesUseCase(get()) }
    factory { CreateLeaseUseCase(get()) }
    factory { RecordLeasePaymentUseCase(get()) }
    factory { UpdateLeaseStatusUseCase(get()) }
    factory { DraftLeaseUseCases() }

    // Wizard UseCases
    factory { SearchUsersUseCase(get()) }
    factory { FetchTenantGuarantorsUseCase(get()) }
    factory { GetAvailableUnitsUseCase(get()) }
    factory { InitLeaseDraftUseCase(get()) }
    factory { SaveLeaseStepUseCase(get()) }
    factory { FinalizeLeaseUseCase(get()) }
    factory { LoadLeaseDraftUseCase(get()) }

    factory {
        LeaseWizardScreenViewModel(
            searchUsersUseCase = get(),
            fetchTenantGuarantorsUseCase = get(),
            getAvailableUnitsUseCase = get(),
            initLeaseDraftUseCase = get(),
            saveLeaseStepUseCase = get(),
            finalizeLeaseUseCase = get(),
            loadLeaseDraftUseCase = get(),
            sessionStorage = getOrNull<SessionStorage>()
        )
    }

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
