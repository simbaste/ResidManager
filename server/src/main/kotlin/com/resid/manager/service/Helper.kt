package com.resid.manager.service

import com.resid.manager.data.HttpError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.UUID

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
