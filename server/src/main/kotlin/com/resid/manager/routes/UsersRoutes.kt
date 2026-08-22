package com.resid.manager.routes

import com.resid.manager.data.HttpError
import com.resid.manager.data.Role
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.UserPasswordUpdateRequest
import com.resid.manager.dto.UserUpdateRequest
import com.resid.manager.helpers.tryOptional
import com.resid.manager.service.UserService
import com.resid.manager.service.getQueryUuid
import com.resid.manager.service.getRequesterUuid
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.userRoutes() {
    route("/api/users") {
        authenticate("auth-jwt") {
            // GET /api/user : Fetch logged-in user profile details
            get("/me") {
                try {
                    val userId = call.getRequesterUuid()
                    val userDto = UserService.getUserProfile(userId)
                    call.respond(HttpStatusCode.OK, userDto)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("Utilisateur introuvable : ${e.message}")
                    )
                }
            }

            // PUT /api/users : Update logged-in user profile
            put {
                try {
                    val userId = call.getRequesterUuid()
                    val request = call.receive<UserUpdateRequest>()
                    val updatedUserDto = UserService.updateUserProfile(userId, request)
                    call.respond(HttpStatusCode.OK, updatedUserDto)
                } catch (e: ContentTransformationException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Impossible de convertir le body de la requête"))
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la mise à jour du profil : ${e.message}")
                    )
                }
            }

            // PUT /api/users/password : Update logged-in user password
            put("/password") {
                try {
                    val userId = call.getRequesterUuid()
                    val request = call.receive<UserPasswordUpdateRequest>()
                    UserService.updateUserPassword(userId, request)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Mot de passe mis à jour avec succès."))
                } catch (e: ContentTransformationException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Impossible de convertir le body de la requête"))
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la mise à jour du mot de passe : ${e.message}")
                    )
                }
            }

            // DELETE /api/user : Delete logged-in user profile
            delete {
                try {
                    val userId = call.getRequesterUuid()
                    UserService.deleteUser(userId)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Compte utilisateur supprimé avec succès."))
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la suppression du profil : ${e.message}")
                    )
                }
            }

            // GET /api/user/search : Search users (by name/email, with optional residenceId and role filter)
            get("/search") {
                try {
                    val query = call.request.queryParameters["q"]
                    val residenceId = call.getQueryUuid("residenceId")
                    val role = call.request.queryParameters["role"]?.let {
                        tryOptional { Role.valueOf(it) }
                    }

                    val users = UserService.searchUsers(query, residenceId, role)
                    call.respond(HttpStatusCode.OK, users)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la recherche des utilisateurs : ${e.message}")
                    )
                }
            }
        }
    }

    // Backward-compatibility route aliases for existing frontend calls
    authenticate("auth-jwt") {
        get("/api/profile") {
            try {
                val userId = call.getRequesterUuid()
                val userDto = UserService.getUserProfile(userId)
                call.respond(HttpStatusCode.OK, userDto)
            } catch (e: HttpError) {
                call.respond(e.code, ErrorResponse(e.message))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("Utilisateur introuvable : ${e.message}")
                )
            }
        }

        put("/api/profile") {
            try {
                val userId = call.getRequesterUuid()
                val request = call.receive<UserUpdateRequest>()
                val updatedUserDto = UserService.updateUserProfile(userId, request)
                call.respond(HttpStatusCode.OK, updatedUserDto)
            } catch (e: ContentTransformationException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Impossible de convertir le body de la requête"))
            } catch (e: HttpError) {
                call.respond(e.code, ErrorResponse(e.message))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Erreur lors de la mise à jour du profil : ${e.message}")
                )
            }
        }

        get("/api/users/search") {
            try {
                val query = call.request.queryParameters["q"]
                val residenceId = call.getQueryUuid("residenceId")
                val role = call.request.queryParameters["role"]?.let {
                    tryOptional { Role.valueOf(it) }
                }

                val users = UserService.searchUsers(query, residenceId, role)
                call.respond(HttpStatusCode.OK, users)
            } catch (e: HttpError) {
                call.respond(e.code, ErrorResponse(e.message))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Erreur lors de la recherche des utilisateurs : ${e.message}")
                )
            }
        }
    }
}
