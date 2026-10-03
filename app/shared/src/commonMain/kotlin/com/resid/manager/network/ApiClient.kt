package com.resid.manager.network

import com.resid.manager.getBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiClient {
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        HttpResponseValidator {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    val path = response.call.request.url.encodedPath
                    // Ne pas déclencher la déconnexion automatique lors d'une tentative de login
                    if (!path.endsWith("/api/auth/login") && !path.endsWith("/api/auth/register")) {
                        AuthEvents.emitUnauthorized()
                    }
                }
            }
        }
    }

    val BASE_URL = getBaseUrl()
}

