package com.resid.manager.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.Role
import com.resid.manager.data.User
import com.resid.manager.data.Users
import com.resid.manager.data.convert
import com.resid.manager.dto.UserDto
import com.resid.manager.dto.UserPasswordUpdateRequest
import com.resid.manager.dto.UserSearchDto
import com.resid.manager.dto.UserUpdateRequest
import com.resid.manager.validation.AuthValidator
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
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
            firstName = dbUser.firstName,
            lastName = dbUser.lastName,
            phone = dbUser.phone,
            birthDate = dbUser.birthDate?.toString(),
            createdAt = dbUser.createdAt.toString(),
            updatedAt = dbUser.updatedAt.toString()
        )
    }

    fun updateUserPassword(
        userId: UUID,
        request: UserPasswordUpdateRequest,
    ) = transaction {
        val dbUser = User.findById(userId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Utilisateur introuvable.")

        // Verify the old password with BCrypt
        val verification = BCrypt.verifyer().verify(request.oldPassword.toCharArray(), dbUser.passwordHash)
        if (!verification.verified) {
            throw HttpError(HttpStatusCode.Unauthorized, "L'ancien mot de passe est incorrect.")
        }

        // Validate the new password complexity
        if (!AuthValidator.isValidPassword(request.newPassword)) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Le nouveau mot de passe doit faire au moins 8 caractères et contenir un chiffre et un caractère spécial."
            )
        }

        // Hash and save new password
        dbUser.passwordHash = BCrypt.withDefaults().hashToString(12, request.newPassword.toCharArray())
        dbUser.updatedAt = LocalDateTime.now(Clock.systemUTC())
        dbUser.flush()
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

        request.firstName?.takeIf { it.isNotBlank() }?.let { dbUser.firstName = it }
        request.lastName?.takeIf { it.isNotBlank() }?.let { dbUser.lastName = it }
        request.phone?.takeIf { it.isNotBlank() }?.let { dbUser.phone = it }
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
            firstName = dbUser.firstName,
            lastName = dbUser.lastName,
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
