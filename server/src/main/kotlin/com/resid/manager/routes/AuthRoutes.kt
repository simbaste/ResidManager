package com.resid.manager.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.resid.manager.auth.JwtConfig
import com.resid.manager.data.User
import com.resid.manager.data.Users
import com.resid.manager.dto.AuthRequest
import com.resid.manager.dto.AuthResponse
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.RegisterRequest
import com.resid.manager.dto.UserDto
import com.resid.manager.validation.AuthValidator
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

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
                    firstName = dbUser.firstName,
                    lastName = dbUser.lastName,
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


        // -----------------------------------------------------------------
        // SECTION 1: PUBLIC ROUTES
        // -----------------------------------------------------------------

        // POST /api/auth/register : Generic account creation
        post("/api/auth/register") {
            try {
                val request = call.receive<RegisterRequest>()

                // 1. Validate inputs via Shared Logic
                val validation = AuthValidator.validateRegister(
                    firstName = request.firstName,
                    lastName = request.lastName,
                    email = request.email,
                    passwordPlain = request.passwordPlain
                )

                if (validation.isFailure) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(validation.exceptionOrNull()?.message ?: "Données d'inscription invalides.")
                    )
                    return@post
                }

                // 2. Check if user already exists
                val alreadyExists = transaction {
                    User.find { Users.email eq request.email }.count() > 0
                }

                if (alreadyExists) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ErrorResponse("This email address is already registered. Try logging in instead.")
                    )
                    return@post
                }

                // 3. Hash password using BCrypt
                val hashedPassword = BCrypt.withDefaults().hashToString(12, request.passwordPlain.toCharArray())

                // Parse birthDate if provided
                val parsedBirthDate = request.birthDate?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        null
                    }
                }

                // 4. Save the new user record into DB
                val newUser = transaction {
                    User.new {
                        email = request.email
                        passwordHash = hashedPassword
                        firstName = request.firstName
                        lastName = request.lastName
                        birthDate = parsedBirthDate
                        phone = request.phone
                        createdAt = LocalDateTime.now(Clock.systemUTC())
                        updatedAt = LocalDateTime.now(Clock.systemUTC())
                    }
                }

                // 5. Auto-authenticate by generating and returning a JWT token
                val token = JwtConfig.generateToken(
                    userId = newUser.id.value.toString(),
                    email = newUser.email,
                )

                val userDto = UserDto(
                    id = newUser.id.value.toString(),
                    email = newUser.email,
                    firstName = newUser.firstName,
                    lastName = newUser.lastName,
                    phone = newUser.phone,
                    birthDate = newUser.birthDate?.toString(),
                    createdAt = newUser.createdAt.toString(),
                    updatedAt = newUser.createdAt.toString()
                )

                call.respond(HttpStatusCode.Created, AuthResponse(token, userDto))

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Une erreur est survenue lors de l'inscription: ${e.message}")
                )
            }
        }
    }
}
