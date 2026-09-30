package com.resid.manager.features.tickets.di

import com.resid.manager.features.tickets.TicketsViewModel
import com.resid.manager.features.tickets.data.TicketRepository
import com.resid.manager.features.tickets.data.TicketRepositoryImpl
import com.resid.manager.features.tickets.usecase.CreateTicketUseCase
import com.resid.manager.features.tickets.usecase.FetchTicketCategoriesUseCase
import com.resid.manager.features.tickets.usecase.FetchTicketsUseCase
import com.resid.manager.features.tickets.usecase.UpdateTicketStatusUseCase
import org.koin.dsl.module

val ticketsFeatureModule = module {
    single<TicketRepository> { TicketRepositoryImpl() }
    factory { FetchTicketsUseCase(get()) }
    factory { FetchTicketCategoriesUseCase(get()) }
    factory { CreateTicketUseCase(get()) }
    factory { UpdateTicketStatusUseCase(get()) }

    single {
        TicketsViewModel(
            fetchTicketsUseCase = get(),
            fetchTicketCategoriesUseCase = get(),
            createTicketUseCase = get(),
            updateTicketStatusUseCase = get()
        )
    }
}
