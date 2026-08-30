package com.resid.manager.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.Role
import com.resid.manager.data.User
import com.resid.manager.data.Users
import com.resid.manager.data.convert
import com.resid.manager.dto.RegisterRequest
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
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object UserService {

    suspend fun createUser(request: RegisterRequest): UserDto = dbQuery {
        val alreadyExists = User.find { Users.email eq request.email }.count() > 0

        if (alreadyExists) throw HttpError(HttpStatusCode.Conflict, "Un utilisateur existe déjà avec cet email")

        // Validate the password complexity
        if (!AuthValidator.isValidPassword(request.passwordPlain)) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Le mot de passe doit faire au moins 8 caractères et contenir un chiffre et un caractère spécial."
            )
        }
        // Validate the email
        if (!AuthValidator.isValidEmail(request.email)) {
            throw HttpError(HttpStatusCode.BadRequest, "Format d'email invalide.")
        }
        val hashedPassword = BCrypt.withDefaults().hashToString(12, request.passwordPlain.toCharArray())
        // Parse birthDate if provided
        val parsedBirthDate = request.birthDate?.let {
            try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                null
            }
        }
        val now = LocalDateTime.now(Clock.systemUTC())
        val newUser = User.new {
            email = request.email
            passwordHash = hashedPassword
            firstName = request.firstName
            lastName = request.lastName
            birthDate = parsedBirthDate
            phone = request.phone
            createdAt = now
            updatedAt = now
        }
        newUser.flush()

        UserDto(
            id = newUser.id.value.toString(),
            email = newUser.email,
            firstName = newUser.firstName,
            lastName = newUser.lastName,
            phone = newUser.phone,
            birthDate = newUser.birthDate?.toString(),
            createdAt = newUser.createdAt.toString(),
            updatedAt = newUser.updatedAt.toString()
        )
    }

    // To be updated so only super admin can use this in future
    suspend fun getAllUsers(): List<UserDto> = dbQuery {
        User.all().map { user ->
            UserDto(
                id = user.id.value.toString(),
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                phone = user.phone,
                birthDate = user.birthDate?.toString(),
                createdAt = user.createdAt.toString(),
                updatedAt = user.updatedAt.toString()
            )
        }
    }

    suspend fun getUserProfile(userId: UUID): UserDto = dbQuery {
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

    suspend fun updateUserPassword(
        userId: UUID,
        request: UserPasswordUpdateRequest,
    ) = dbQuery {
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

    suspend fun updateUserProfile(userId: UUID, request: UserUpdateRequest): UserDto = dbQuery {
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

    suspend fun deleteUser(userId: UUID) = dbQuery {
        User.findById(userId)?.let {
            Users.deleteWhere { id eq userId }
        } ?: throw HttpError(HttpStatusCode.NotFound, "Utilisateur introuvable.")
    }

    suspend fun searchUsers(
        query: String?,
        residenceId: UUID?,
        role: Role?
    ): List<UserSearchDto> = dbQuery {
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
