package com.resid.manager.features.auth.di

import com.resid.manager.SessionStorage
import com.resid.manager.features.auth.AuthViewModel
import com.resid.manager.features.auth.usecase.LoginUseCase
import com.resid.manager.features.auth.usecase.LogoutUseCase
import com.resid.manager.features.auth.usecase.RegisterUseCase
import org.koin.dsl.module

val authFeatureModule = module {
    factory { LoginUseCase(get(), getOrNull<SessionStorage>()) }
    factory { RegisterUseCase(get(), getOrNull<SessionStorage>()) }
    factory { LogoutUseCase(getOrNull<SessionStorage>()) }

    single {
        AuthViewModel(
            loginUseCase = get(),
            registerUseCase = get(),
            logoutUseCase = get(),
            sessionStorage = getOrNull<SessionStorage>()
        )
    }
}
