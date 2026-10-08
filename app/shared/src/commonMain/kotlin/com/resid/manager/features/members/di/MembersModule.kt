package com.resid.manager.features.members.di

import com.resid.manager.features.members.MembersViewModel
import com.resid.manager.features.members.usecase.FetchMembersUseCase
import com.resid.manager.features.members.usecase.InviteMemberUseCase
import org.koin.dsl.module

val membersFeatureModule = module {
    factory { FetchMembersUseCase(get()) }
    factory { InviteMemberUseCase(get()) }

    single {
        MembersViewModel(
            fetchMembersUseCase = get(),
            inviteMemberUseCase = get()
        )
    }
}
