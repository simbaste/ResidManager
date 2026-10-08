package com.resid.manager.routes

import com.resid.manager.auth.JwtConfig
import com.resid.manager.data.Role
import com.resid.manager.dto.AuthResponse
import com.resid.manager.dto.RegisterRequest
import com.resid.manager.dto.UserPasswordUpdateRequest
import com.resid.manager.dto.UserUpdateRequest
import com.resid.manager.helpers.tryOptional
import com.resid.manager.service.UserService
import com.resid.manager.service.executeRequest
import com.resid.manager.service.getQueryUuid
import com.resid.manager.service.getRequesterUuid
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.userRoutes() {
    route("/api/users") {
        // POST /api/users : Create a new user
        post {
            call.executeRequest(HttpStatusCode.Created) {
                val request = call.receive<RegisterRequest>()
                val createdUser = UserService.createUser(request)
                val token = JwtConfig.generateToken(
                    userId = createdUser.id,
                    email = createdUser.email,
                )
                val refreshToken = JwtConfig.generateRefreshToken(
                    userId = createdUser.id,
                    email = createdUser.email,
                )
                AuthResponse(token = token, refreshToken = refreshToken, user = createdUser)
            }
        }

        authenticate("auth-jwt") {
            // GET /api/users: Fetch all users in BD
            get {
                call.executeRequest(HttpStatusCode.OK) {
                    UserService.getAllUsers()
                }
            }

            // GET /api/users/me : Fetch logged-in user profile details
            get("/me") {
                call.executeRequest(HttpStatusCode.OK) {
                    val userId = call.getRequesterUuid()
                    UserService.getUserProfile(userId)
                }
            }

            // PUT /api/users : Update logged-in user profile
            put {
                call.executeRequest(HttpStatusCode.OK) {
                    val userId = call.getRequesterUuid()
                    val request = call.receive<UserUpdateRequest>()
                    UserService.updateUserProfile(userId, request)
                }
            }

            // PUT /api/users/password : Update logged-in user password
            put("/password") {
                call.executeRequest(HttpStatusCode.OK) {
                    val userId = call.getRequesterUuid()
                    val request = call.receive<UserPasswordUpdateRequest>()
                    UserService.updateUserPassword(userId, request)
                    mapOf("message" to "Mot de passe mis à jour avec succès.")
                }
            }

            // DELETE /api/users : Delete logged-in user profile
            delete {
                call.executeRequest(HttpStatusCode.OK) {
                    val userId = call.getRequesterUuid()
                    UserService.deleteUser(userId)
                    mapOf("message" to "Compte utilisateur supprimé avec succès.")
                }
            }

            // GET /api/users/search : Search users
            get("/search") {
                call.executeRequest(HttpStatusCode.OK) {
                    val query = call.request.queryParameters["q"]
                    val residenceId = call.getQueryUuid("residenceId")
                    val role = call.request.queryParameters["role"]?.let {
                        tryOptional { Role.valueOf(it) }
                    }
                    UserService.searchUsers(query, residenceId, role)
                }
            }
        }
    }

    // Backward-compatibility route aliases for existing frontend calls
    authenticate("auth-jwt") {
        get("/api/profile") {
            call.executeRequest(HttpStatusCode.OK) {
                val userId = call.getRequesterUuid()
                UserService.getUserProfile(userId)
            }
        }

        put("/api/profile") {
            call.executeRequest(HttpStatusCode.OK) {
                val userId = call.getRequesterUuid()
                val request = call.receive<UserUpdateRequest>()
                UserService.updateUserProfile(userId, request)
            }
        }

        get("/api/users/search") {
            call.executeRequest(HttpStatusCode.OK) {
                val query = call.request.queryParameters["q"]
                val residenceId = call.getQueryUuid("residenceId")
                val role = call.request.queryParameters["role"]?.let {
                    tryOptional { Role.valueOf(it) }
                }
                UserService.searchUsers(query, residenceId, role)
            }
        }
    }
}
