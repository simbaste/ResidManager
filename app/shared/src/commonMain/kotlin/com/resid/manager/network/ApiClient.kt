package com.resid.manager.network

import com.resid.manager.createPlatformSessionStorage
import com.resid.manager.dto.RefreshTokenRequest
import com.resid.manager.dto.TokenRefreshResponse
import com.resid.manager.getBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

object ApiClient {
    private val refreshMutex = Mutex()
    private val sessionStorage by lazy { createPlatformSessionStorage() }

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }.apply {
        // Intercept all HTTP calls to handle 401 Unauthorized with token refresh & retry
        plugin(HttpSend).intercept { request ->
            val originalCall = execute(request)
            val path = request.url.buildString()

            // If 401 received and this is not an auth endpoint, attempt refresh
            if (originalCall.response.status == HttpStatusCode.Unauthorized &&
                !path.contains("/api/auth/login") &&
                !path.contains("/api/auth/register") &&
                !path.contains("/api/auth/refresh")
            ) {
                val refreshedSuccessfully = refreshMutex.withLock {
                    val session = sessionStorage?.loadSession()
                    val refreshToken = session?.refreshToken

                    !refreshToken.isNullOrBlank() && try {
                        // Call refresh endpoint with a separate raw request to avoid circular interception
                        val refreshResponse = HttpClient {
                            install(ContentNegotiation) {
                                json(Json {
                                    ignoreUnknownKeys = true
                                    isLenient = true
                                })
                            }
                        }.post("${getBaseUrl()}/api/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody(RefreshTokenRequest(refreshToken))
                        }

                        if (refreshResponse.status == HttpStatusCode.OK) {
                            val refreshBody = refreshResponse.body<TokenRefreshResponse>()
                            sessionStorage?.updateTokens(
                                token = refreshBody.token,
                                refreshToken = refreshBody.refreshToken ?: refreshToken
                            )
                            true
                        } else {
                            false
                        }
                    } catch (_: Exception) {
                        false
                    }
                    // Call refresh endpoint with a separate raw request to avoid circular interception
                }

                if (refreshedSuccessfully) {
                    // Update Authorization header on original request with new token and retry
                    val latestSession = sessionStorage?.loadSession()
                    if (latestSession != null) {
                        request.headers.remove(HttpHeaders.Authorization)
                        request.header(HttpHeaders.Authorization, "Bearer ${latestSession.token}")
                    }
                    execute(request)
                } else {
                    // Refresh failed or no refresh token -> notify app to redirect to Login
                    sessionStorage?.clearSession()
                    AuthEvents.emitUnauthorized()
                    originalCall
                }
            } else {
                originalCall
            }
        }
    }

    val BASE_URL = getBaseUrl()

    /**
     * Parse résilient du corps d'erreur d'une HttpResponse :
     * Tente de désérialiser en [ErrorResponse] si le contenu est JSON,
     * ou lit le texte brut sinon, évitant ainsi le crash NoTransformationFoundException.
     */
    suspend fun parseErrorMessage(response: HttpResponse, fallbackMessage: String): String {
        return try {
            val contentType = response.headers[HttpHeaders.ContentType] ?: ""
            if (contentType.contains("application/json", ignoreCase = true)) {
                val errorBody = response.body<com.resid.manager.dto.ErrorResponse>()
                errorBody.message.ifBlank { fallbackMessage }
            } else {
                val rawText = response.body<String>()
                if (rawText.isNotBlank()) {
                    rawText.take(300)
                } else {
                    fallbackMessage
                }
            }
        } catch (_: Exception) {
            fallbackMessage
        }
    }
}





