package com.resid.manager.di

import com.resid.manager.SessionStorage
import com.resid.manager.repository.*
import com.resid.manager.usecase.SearchResidencesUseCase
import com.resid.manager.viewmodel.LoginViewModel
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
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
    single<LogementRepository> { LogementRepositoryImpl(get()) }
}

val useCaseModule = module {
    single { SearchResidencesUseCase(get()) }
}

val viewModelModule = module {
    factory { (sessionStorage: SessionStorage?) -> 
        LoginViewModel(get(), get(), get(), get(), sessionStorage)
    }
}

val sharedAppModule = module {
    includes(networkModule, repositoryModule, useCaseModule, viewModelModule)
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(sharedAppModule)
}

fun initKoinHelper() = initKoin {}
