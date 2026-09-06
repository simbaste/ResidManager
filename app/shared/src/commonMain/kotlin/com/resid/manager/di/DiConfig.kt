package com.resid.manager.di

import com.resid.manager.SessionStorage
import com.resid.manager.createPlatformSessionStorage
import com.resid.manager.repository.AuthRepository
import com.resid.manager.repository.AuthRepositoryImpl
import com.resid.manager.repository.LeaseRepository
import com.resid.manager.repository.LeaseRepositoryImpl
import com.resid.manager.repository.MemberRepository
import com.resid.manager.repository.MemberRepositoryImpl
import com.resid.manager.repository.ResidenceRepository
import com.resid.manager.repository.ResidenceRepositoryImpl
import com.resid.manager.repository.ResidenceUnitRepository
import com.resid.manager.repository.ResidenceUnitRepositoryImpl
import com.resid.manager.usecase.SearchResidencesUseCase
import com.resid.manager.viewmodel.LoginViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
        }
    }
}

val repositoryModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<ResidenceRepository> { ResidenceRepositoryImpl(get()) }
    single<ResidenceUnitRepository> { ResidenceUnitRepositoryImpl(get()) }
    single<LeaseRepository> { LeaseRepositoryImpl(get()) }
    single<MemberRepository> { MemberRepositoryImpl(get()) }
}

val useCaseModule = module {
    single { SearchResidencesUseCase(get()) }
}

val sessionStorageModule = module {
    createPlatformSessionStorage()?.let { storage ->
        single<SessionStorage> { storage }
    }
}

val viewModelModule = module {
    single { 
        LoginViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            getOrNull<SessionStorage>()
        )
    }
}

val sharedAppModule = module {
    includes(networkModule, repositoryModule, useCaseModule, sessionStorageModule, viewModelModule)
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(sharedAppModule)
}

fun initKoinHelper() = initKoin {}
