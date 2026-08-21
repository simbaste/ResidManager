package com.resid.manager.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.resid.manager.data.*
import com.resid.manager.dto.UserDto
import com.resid.manager.dto.UserSearchDto
import com.resid.manager.dto.UserUpdateRequest
import com.resid.manager.validation.AuthValidator
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object UserService {

    fun getUserProfile(userId: UUID): UserDto = transaction {
        val dbUser = User.findById(userId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Utilisateur introuvable.")

        UserDto(
            id = dbUser.id.value.toString(),
            email = dbUser.email,
            name = "${dbUser.firstName} ${dbUser.lastName}",
            phone = dbUser.phone,
            birthDate = dbUser.birthDate?.toString(),
            createdAt = dbUser.createdAt.toString(),
            updatedAt = dbUser.updatedAt.toString()
        )
    }

    fun updateUserProfile(userId: UUID, request: UserUpdateRequest): UserDto = transaction {
        val dbUser = User.findById(userId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Utilisateur introuvable.")

        // Validations
        request.email?.let { email ->
            if (!AuthValidator.isValidEmail(email)) {
                throw HttpError(HttpStatusCode.BadRequest, "Format d'email invalide.")
            }
            if (email != dbUser.email) {
                val exists = User.find { Users.email eq email }.count() > 0
                if (exists) {
                    throw HttpError(HttpStatusCode.Conflict, "Cette adresse email est déjà utilisée.")
                }
                dbUser.email = email
            }
        }

        request.passwordPlain?.let { password ->
            if (!AuthValidator.isValidPassword(password)) {
                throw HttpError(
                    HttpStatusCode.BadRequest,
                    "Le nouveau mot de passe doit faire au moins 8 caractères et contenir un chiffre et un caractère spécial."
                )
            }
            dbUser.passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        }

        request.firstName?.let { dbUser.firstName = it }
        request.lastName?.let { dbUser.lastName = it }
        request.phone?.let { dbUser.phone = it }
        request.birthDate?.let { dateStr ->
            dbUser.birthDate = try {
                LocalDate.parse(dateStr)
            } catch (_: Exception) {
                null
            }
        }

        dbUser.updatedAt = LocalDateTime.now(Clock.systemUTC())
        dbUser.flush()

        UserDto(
            id = dbUser.id.value.toString(),
            email = dbUser.email,
            name = "${dbUser.firstName} ${dbUser.lastName}",
            phone = dbUser.phone,
            birthDate = dbUser.birthDate?.toString(),
            createdAt = dbUser.createdAt.toString(),
            updatedAt = dbUser.updatedAt.toString()
        )
    }

    fun deleteUser(userId: UUID) = transaction {
        val dbUser = User.findById(userId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Utilisateur introuvable.")

        Users.deleteWhere { id eq userId }
    }

    fun searchUsers(
        query: String?,
        residenceId: UUID?,
        role: Role?
    ): List<UserSearchDto> = transaction {
        val searchQuery = query?.trim() ?: ""

        if (residenceId != null) {
            var conditions: Op<Boolean> = (ResidenceMembers.residenceId eq residenceId) and
                    (ResidenceMembers.status eq InvitationStatus.ACCEPTED)

            if (role != null) {
                conditions = conditions and (ResidenceMembers.role eq role)
            }

            if (searchQuery.isNotBlank()) {
                val searchPattern = "%${searchQuery.lowercase()}%"
                conditions = conditions and (
                    (Users.email.lowerCase() like searchPattern) or
                    (Users.firstName.lowerCase() like searchPattern) or
                    (Users.lastName.lowerCase() like searchPattern)
                )
            }

            (Users innerJoin ResidenceMembers)
                .select(Users.id, Users.firstName, Users.lastName, Users.email, ResidenceMembers.role)
                .where { conditions }
                .withDistinct()
                .map { row ->
                    UserSearchDto(
                        id = row[Users.id].value.toString(),
                        email = row[Users.email],
                        name = "${row[Users.firstName]} ${row[Users.lastName]}",
                        roleDto = row[ResidenceMembers.role].convert()
                    )
                }
        } else {
            var conditions: Op<Boolean> = Op.TRUE

            if (searchQuery.isNotBlank()) {
                val searchPattern = "%${searchQuery.lowercase()}%"
                conditions = (Users.email.lowerCase() like searchPattern) or
                        (Users.firstName.lowerCase() like searchPattern) or
                        (Users.lastName.lowerCase() like searchPattern)
            }

            Users
                .select(Users.id, Users.firstName, Users.lastName, Users.email)
                .where { conditions }
                .map { row ->
                    UserSearchDto(
                        id = row[Users.id].value.toString(),
                        email = row[Users.email],
                        name = "${row[Users.firstName]} ${row[Users.lastName]}",
                        roleDto = null
                    )
                }
        }
    }
}
