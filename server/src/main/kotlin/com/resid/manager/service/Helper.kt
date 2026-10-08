package com.resid.manager.service

import com.resid.manager.data.HttpError
import com.resid.manager.dto.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.response.respond
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

const val DEFAULT_DB_TIMEOUT_MS = 10_000L // 10 seconds default timeout

/**
 * Executes the database transaction asynchronously on Dispatchers.IO with a timeout,
 * ensuring Netty worker threads are never blocked.
 */
suspend fun <T> dbQuery(
    timeoutMs: Long = DEFAULT_DB_TIMEOUT_MS,
    block: suspend Transaction.() -> T
): T = try {
    withTimeout(timeoutMs.milliseconds) {
        newSuspendedTransaction(Dispatchers.IO) {
            block()
        }
    }
} catch (e: TimeoutCancellationException) {
    e.printStackTrace()
    throw HttpError(
        HttpStatusCode.GatewayTimeout,
        "Le délai d'attente de la base de données a été dépassé (timeout)."
    )
}

/**
 * Unified route execution wrapper that catches exceptions and maps to proper HTTP responses.
 */
suspend inline fun <T> ApplicationCall.executeRequest(
    statusOnSuccess: HttpStatusCode = HttpStatusCode.OK,
    crossinline block: suspend () -> T
) {
    try {
        val result = block()
        if (result is Unit) {
            respond(statusOnSuccess)
        } else {
            respond(statusOnSuccess, result as Any)
        }
    } catch (e: ContentTransformationException) {
        respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Corps de requête invalide."))
    } catch (e: HttpError) {
        respond(e.code, ErrorResponse(e.message))
    } catch (e: Exception) {
        respond(
            HttpStatusCode.InternalServerError,
            ErrorResponse(e.message ?: "Une erreur interne est survenue.")
        )
    }
}

fun ApplicationCall.getRequesterUuid(): UUID {
    val principal = principal<JWTPrincipal>()
    val requesterUserId = principal?.payload?.getClaim("userId")?.asString()
        ?: throw HttpError(HttpStatusCode.Unauthorized, "Non authentifié.")
    return try {
        UUID.fromString(requesterUserId)
    } catch (e: Exception) {
        throw HttpError(HttpStatusCode.Unauthorized, "Impossible d'authentifier l'auteur de la requête.")
    }
}

fun ApplicationCall.getQueryUuid(paramName: String, required: Boolean = false): UUID? {
    val value = request.queryParameters[paramName]
    if (value == null) {
        if (required) throw HttpError(HttpStatusCode.BadRequest, "ID manquant : $paramName.")
        return null
    }
    return try {
        UUID.fromString(value)
    } catch (e: Exception) {
        throw HttpError(HttpStatusCode.BadRequest, "ID invalide pour le paramètre '$paramName': ${e.message}")
    }
}

fun ApplicationCall.getPathUuid(paramName: String = "id"): UUID {
    val value = parameters[paramName]
        ?: throw HttpError(HttpStatusCode.BadRequest, "Paramètre de chemin manquant : '$paramName'.")
    return try {
        UUID.fromString(value)
    } catch (e: Exception) {
        throw HttpError(HttpStatusCode.BadRequest, "ID invalide pour le paramètre '$paramName': ${e.message}")
    }
}
