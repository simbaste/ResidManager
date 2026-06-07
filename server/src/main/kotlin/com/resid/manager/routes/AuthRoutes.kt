package com.resid.manager.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.resid.manager.auth.JwtConfig
import com.resid.manager.data.User
import com.resid.manager.data.Users
import com.resid.manager.dto.AuthRequest
import com.resid.manager.dto.AuthResponse
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.UserDto
import com.resid.manager.validation.AuthValidator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.authRoutes() {
    route("/api/auth") {
        post("/login") {
            try {
                val request = call.receive<AuthRequest>()

                // 1. Validate inputs
                val validation = AuthValidator.validateLogin(request.email, request.passwordPlain)
                if (validation.isFailure) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(validation.exceptionOrNull()?.message ?: "Données de connexion invalides.")
                    )
                    return@post
                }

                // 2. Fetch user from Database inside an Exposed transaction
                val dbUser = transaction {
                    User.find { Users.email eq request.email }.firstOrNull()
                }

                if (dbUser == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse("Identifiants incorrects (utilisateur introuvable).")
                    )
                    return@post
                }

                // 3. Verify password using BCrypt
                val passwordVerification = BCrypt.verifyer().verify(
                    request.passwordPlain.toCharArray(),
                    dbUser.passwordHash
                )

                if (!passwordVerification.verified) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse("Identifiants incorrects (mot de passe invalide).")
                    )
                    return@post
                }

                // 4. Generate JWT token
                val token = JwtConfig.generateToken(
                    userId = dbUser.id.value.toString(),
                    email = dbUser.email,
                )

                val userDto = UserDto(
                    id = dbUser.id.value.toString(),
                    email = dbUser.email,
                    name = "${dbUser.firstName} ${dbUser.lastName}",
                    phone = dbUser.phone,
                    birthDate = dbUser.birthDate?.toString(),
                    createdAt = dbUser.createdAt.toString(),
                    updatedAt = dbUser.createdAt.toString()
                )

                call.respond(HttpStatusCode.OK, AuthResponse(token, userDto))

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Une erreur interne est survenue: ${e.localizedMessage}")
                )
            }
        }
    }
}
