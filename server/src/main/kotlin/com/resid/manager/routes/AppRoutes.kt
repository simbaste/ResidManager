package com.resid.manager.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.resid.manager.auth.JwtConfig
import com.resid.manager.data.*
import com.resid.manager.dto.*
import com.resid.manager.validation.AuthValidator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun Application.configureAppRoutes() {
    routing {
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
                        createdAt = LocalDateTime.now()
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
                    name = "${newUser.firstName} ${newUser.lastName}",
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

        // -----------------------------------------------------------------
        // SECTION 2: SECURED ROUTES (JWT AUTHENTICATED)
        // -----------------------------------------------------------------
        authenticate("auth-jwt") {
            
            // GET /api/profile : Fetch logged-in user profile details
            get("/api/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""

                try {
                    val userDto = transaction {
                        val dbUser = User.findById(UUID.fromString(userId)) ?: throw Exception("Utilisateur introuvable.")
                        
                        
                        UserDto(
                            id = dbUser.id.value.toString(),
                            email = dbUser.email,
                            name = "${dbUser.firstName} ${dbUser.lastName}",
                            phone = dbUser.phone,
                            birthDate = dbUser.birthDate?.toString(),
                            createdAt = dbUser.createdAt.toString(),
                            updatedAt = dbUser.createdAt.toString()
                        )
                    }

                    call.respond(HttpStatusCode.OK, userDto)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("Utilisateur introuvable : ${e.message}")
                    )
                }
            }

            // PUT /api/profile : Update profile
            put("/api/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""

                try {
                    val request = call.receive<UserUpdateRequest>()

                    // Optional validations
                    request.email?.let {
                        if (!AuthValidator.isValidEmail(it)) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Format d'email invalide."))
                            return@put
                        }
                    }
                    request.passwordPlain?.let {
                        if (!AuthValidator.isValidPassword(it)) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Le nouveau mot de passe doit faire au moins 8 caractères et contenir un chiffre et un caractère spécial."))
                            return@put
                        }
                    }

                    val updatedUserDto = transaction {
                        val dbUser = User.findById(UUID.fromString(userId)) ?: throw Exception("Utilisateur introuvable.")

                        request.email?.let { dbUser.email = it }
                        request.passwordPlain?.let {
                            dbUser.passwordHash = BCrypt.withDefaults().hashToString(12, it.toCharArray())
                        }
                        request.firstName?.let { dbUser.firstName = it }
                        request.lastName?.let { dbUser.lastName = it }
                        request.birthDate?.let { dateStr ->
                            dbUser.birthDate = try {
                                LocalDate.parse(dateStr)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        request.phone?.let { dbUser.phone = it }

                        dbUser.flush()

                        UserDto(
                            id = dbUser.id.value.toString(),
                            email = dbUser.email,
                            name = "${dbUser.firstName} ${dbUser.lastName}",
                            phone = dbUser.phone,
                            birthDate = dbUser.birthDate?.toString(),
                            createdAt = dbUser.createdAt.toString(),
                            updatedAt = dbUser.createdAt.toString()
                        )
                    }

                    call.respond(HttpStatusCode.OK, updatedUserDto)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la mise à jour du profil : ${e.message}")
                    )
                }
            }

            // GET /api/residences : List residences for the directory (Owned and Associated)
            get("/api/residences") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""

                try {
                    val directoryDto = transaction {
                        // 1. Query owned residences (where role is m.role = 'OWNER')
                        val ownedList = (Residences innerJoin ResidenceMembers)
                            .select(Residences.id, Residences.name, Residences.address, Residences.photoUrl)
                            .where { 
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and 
                                (ResidenceMembers.role eq "OWNER") and 
                                (ResidenceMembers.status eq "ACCEPTED") 
                            }
                            .map { row ->
                                val resId = row[Residences.id].value
                                val totalUnits = Logement.find { Logements.residenceId eq resId }.count()
                                ResidenceSummaryItem(
                                    id = resId.toString(),
                                    name = row[Residences.name],
                                    address = row[Residences.address],
                                    photoUrl = row[Residences.photoUrl],
                                    totalUnits = totalUnits.toInt()
                                )
                            }

                        // 2. Query associated residences (where role is not m.role = 'OWNER')
                        val associatedList = (Residences innerJoin ResidenceMembers)
                            .select(Residences.id, Residences.name, Residences.address, Residences.photoUrl, ResidenceMembers.role)
                            .where { 
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and 
                                (ResidenceMembers.role neq "OWNER") and 
                                (ResidenceMembers.status eq "ACCEPTED") 
                            }
                            .map { row ->
                                AssociatedResidenceItem(
                                    id = row[Residences.id].value.toString(),
                                    name = row[Residences.name],
                                    address = row[Residences.address],
                                    photoUrl = row[Residences.photoUrl],
                                    role = row[ResidenceMembers.role]
                                )
                            }

                        ResidenceDirectoryDTO(ownedResidences = ownedList, associatedResidences = associatedList)
                    }

                    call.respond(HttpStatusCode.OK, directoryDto)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération de l'annuaire : ${e.message}")
                    )
                }
            }

            // POST /api/residences : Create a residence
            post("/api/residences") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""

                try {
                    val request = call.receive<ResidenceCreateRequest>()
                    
                    val newResidence = transaction {
                        // 1. Insert residence
                        val r = Residence.new {
                            name = request.name
                            address = request.address
                            photoUrl = null
                            currencyPivot = request.defaultCurrency.ifBlank { "XOF" }
                            kWhPrice = request.kWhPrice
                            createdAt = LocalDateTime.now()
                            updatedAt = LocalDateTime.now()
                        }
                        
                        // Force flush the residence insert to database to satisfy the foreign key constraint
                        r.flush()
                        
                        // 2. Insert member record linking user to this residence as OWNER with ACCEPTED status
                        ResidenceMembers.insert {
                            it[ResidenceMembers.userId] = UUID.fromString(userId)
                            it[ResidenceMembers.residenceId] = r.id.value
                            it[ResidenceMembers.role] = "OWNER"
                            it[ResidenceMembers.status] = "ACCEPTED"
                            it[ResidenceMembers.createdAt] = LocalDateTime.now()
                        }
                        r
                    }

                    val summary = ResidenceSummaryItem(
                        id = newResidence.id.value.toString(),
                        name = newResidence.name,
                        address = newResidence.address,
                        photoUrl = newResidence.photoUrl,
                        totalUnits = 0
                    )

                    call.respond(HttpStatusCode.Created, summary)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la création de la résidence : ${e.message}")
                    )
                }
            }
            
            // DELETE /api/residences/{id} delete a residence
            delete("/api/residences/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""

                try {
                    // 1. Verify that the caller is the OWNER of this residence
                    val userRole = transaction {
                        ResidenceMembers
                            .select(ResidenceMembers.role)
                            .where { 
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and 
                                (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and 
                                (ResidenceMembers.status eq "ACCEPTED") 
                            }
                            .singleOrNull()?.get(ResidenceMembers.role)
                    }

                    if (userRole == null || userRole != "OWNER") {
                        call.respond(
                            HttpStatusCode.Forbidden, 
                            ErrorResponse("Accès interdit: Seuls les propriétaires de cette résidence (OWNER) peuvent la supprimer.")
                        )
                        return@delete
                    }

                    // 2. Perform the deletion inside a transaction using Exposed DSL
                    transaction {
                        Residences.deleteWhere { Residences.id eq UUID.fromString(residenceId) }
                    }

                    call.respond(HttpStatusCode.OK, mapOf("message" to "La résidence a été supprimée avec succès !"))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la suppression de la résidence : ${e.message}")
                    )
                }
            }

            // GET /api/residences/search?name=... : Search for a residence by name
            get("/api/residences/search") {
                val searchName = call.request.queryParameters["name"] ?: ""

                try {
                    val results = transaction {
                        Residences
                            .select(Residences.id, Residences.name, Residences.address, Residences.photoUrl)
                            .where { Residences.name.lowerCase() like "%${searchName.lowercase()}%" }
                            .map { row ->
                                val resId = row[Residences.id].value
                                val totalUnits = Logement.find { Logements.residenceId eq resId }.count()
                                ResidenceSummaryItem(
                                    id = resId.toString(),
                                    name = row[Residences.name],
                                    address = row[Residences.address],
                                    photoUrl = row[Residences.photoUrl],
                                    totalUnits = totalUnits.toInt()
                                )
                            }
                    }

                    call.respond(HttpStatusCode.OK, results)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la recherche des résidences : ${e.message}")
                    )
                }
            }

            // POST /api/residences/{id}/join : Request to join a residence
            post("/api/residences/{id}/join") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""

                try {
                    transaction {
                        ResidenceMembers.insert {
                            it[ResidenceMembers.userId] = UUID.fromString(userId)
                            it[ResidenceMembers.residenceId] = UUID.fromString(residenceId)
                            it[ResidenceMembers.role] = "TENANT"
                            it[ResidenceMembers.status] = "PENDING_APPROVAL"
                            it[ResidenceMembers.createdAt] = LocalDateTime.now()
                        }
                    }
                    call.respond(HttpStatusCode.OK, mapOf("residenceId" to residenceId, "status" to "Demande envoyée"))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la demande d'adhésion : ${e.message}")
                    )
                }
            }

            // POST /api/residences/{id}/members/invite : Invite a user via email
            post("/api/residences/{id}/members/invite") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""

                try {
                    // Check if sender is OWNER or ADMIN
                    val userRole = transaction {
                        ResidenceMembers
                            .select(ResidenceMembers.role)
                            .where { 
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and 
                                (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and 
                                (ResidenceMembers.status eq "ACCEPTED") 
                            }
                            .singleOrNull()?.get(ResidenceMembers.role)
                    }

                    if (userRole == null || (userRole != "OWNER" && userRole != "ADMIN")) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit: Seuls les propriétaires et administrateurs peuvent inviter des membres."))
                        return@post
                    }

                    val request = call.receive<InviteMemberRequest>()

                    // Find invited user by email
                    val invitedUser = transaction {
                        User.find { Users.email eq request.email }.firstOrNull()
                    }

                    if (invitedUser == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Utilisateur introuvable avec l'adresse email : ${request.email}"))
                        return@post
                    }

                    // Insert into residence_members with status 'INVITED' and role
                    transaction {
                        ResidenceMembers.insert {
                            it[ResidenceMembers.userId] = invitedUser.id.value
                            it[ResidenceMembers.residenceId] = UUID.fromString(residenceId)
                            it[ResidenceMembers.role] = request.role
                            it[ResidenceMembers.status] = "INVITED"
                            it[ResidenceMembers.createdAt] = LocalDateTime.now()
                        }
                    }

                    call.respond(HttpStatusCode.OK, mapOf("residenceId" to residenceId, "status" to "Invitation envoyée"))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de l'invitation : ${e.message}")
                    )
                }
            }

            // POST /api/residences/{id}/members/{user_id}/status : Accept/Refuse a member or request
            post("/api/residences/{id}/members/{user_id}/status") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""
                val targetUserId = call.parameters["user_id"] ?: ""

                try {
                    // Check if sender is OWNER or ADMIN
                    val userRole = transaction {
                        ResidenceMembers
                            .select(ResidenceMembers.role)
                            .where { 
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and 
                                (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and 
                                (ResidenceMembers.status eq "ACCEPTED") 
                            }
                            .singleOrNull()?.get(ResidenceMembers.role)
                    }

                    if (userRole == null || (userRole != "OWNER" && userRole != "ADMIN")) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit: Seuls les propriétaires et administrateurs peuvent accepter/modifier les membres."))
                        return@post
                    }

                    val request = call.receive<MemberStatusUpdateRequest>()

                    transaction {
                        ResidenceMembers.update({ 
                            (ResidenceMembers.userId eq UUID.fromString(targetUserId)) and 
                            (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) 
                        }) {
                            it[status] = request.status
                            val newRole = request.role
                            if (newRole != null) {
                                it[role] = newRole
                            }
                        }
                    }

                    call.respond(HttpStatusCode.OK, mapOf("residenceId" to residenceId, "targetUserId" to targetUserId, "status" to "Statut mis à jour"))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la mise à jour du membre : ${e.message}")
                    )
                }
            }
            
            // PUT /api/residences/{id}/members/{user_id}/status : Accept/Decline invitation or update status
            put("/api/residences/{id}/members/{user_id}/status") {
                val principal = call.principal<JWTPrincipal>()
                val callerId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""
                val targetUserId = call.parameters["user_id"] ?: ""

                try {
                    val request = call.receive<MemberStatusUpdateRequest>()

                    // 1. Fetch the target member's current status and role
                    val existingMember = transaction {
                        ResidenceMembers
                            .select(ResidenceMembers.status, ResidenceMembers.role)
                            .where { 
                                (ResidenceMembers.userId eq UUID.fromString(targetUserId)) and 
                                (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) 
                            }
                            .singleOrNull()
                    }

                    if (existingMember == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Aucune invitation ou inscription trouvée pour cet utilisateur."))
                        return@put
                    }

                    // 2. Validate permissions: Only OWNER, ADMIN, or the INVITED user themselves can modify it
                    val isSelf = callerId == targetUserId

                    if (!isSelf) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : Seul l'utilisateur invité lui-même peut modifier le statut de cette invitation."))
                        return@put
                    }

                    // 3. Perform update
                    transaction {
                        ResidenceMembers.update({ 
                            (ResidenceMembers.userId eq UUID.fromString(targetUserId)) and 
                            (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) 
                        }) {
                            it[status] = request.status
                        }
                    }

                    call.respond(HttpStatusCode.OK, mapOf(
                        "residenceId" to residenceId,
                        "userId" to targetUserId,
                        "status" to request.status,
                        "message" to "Le statut de l'invitation a été mis à jour avec succès !"
                    ))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la mise à jour de l'invitation : ${e.message}")
                    )
                }
            }

            // GET /api/user/search?q=name Search a user by his name
            get("/api/users/search") {
                val query = call.request.queryParameters["q"] ?: ""
                try {
                    val users = transaction {
                        Users.select(Users.id, Users.firstName, Users.lastName, Users.email)
                            .where {
                                (Users.email like "%$query%") or
                                (Users.firstName like "%$query%") or
                                (Users.lastName like "%$query%")
                            }
                            .map { row ->
                                UserSearchDto(
                                    id = row[Users.id].value.toString(),
                                    email = row[Users.email],
                                    name = "${row[Users.firstName]} ${row[Users.lastName]}",
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, users)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur lors de la recherche d'utilisateurs: ${e.message}"))
                }
            }

            // GET /api/residences/{residence_id}/members Get All the residence members
            get("/api/residences/{id}/members") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""

                try {
                    // Check if member exists in residence_members with ACCEPTED status
                    val isMember = transaction {
                        !ResidenceMembers
                            .select(ResidenceMembers.userId)
                            .where { 
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and 
                                (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and 
                                (ResidenceMembers.status eq "ACCEPTED") 
                            }
                            .empty()
                    }

                    if (!isMember) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : vous ne faites pas partie de cette résidence."))
                        return@get
                    }

                    val membersList = transaction {
                        // Join Users with ResidenceMembers
                        (Users innerJoin ResidenceMembers)
                            .select(
                                Users.id,
                                Users.firstName,
                                Users.lastName,
                                Users.email,
                                Users.phone,
                                ResidenceMembers.role,
                                ResidenceMembers.status
                            )
                            .where { ResidenceMembers.residenceId eq UUID.fromString(residenceId) }
                            .map { row ->
                                ResidenceMemberSummary(
                                    userId = row[Users.id].value.toString(),
                                    firstName = row[Users.firstName],
                                    lastName = row[Users.lastName],
                                    email = row[Users.email],
                                    phone = row[Users.phone],
                                    role = row[ResidenceMembers.role],
                                    status = row[ResidenceMembers.status]
                                )
                            }
                    }

                    call.respond(HttpStatusCode.OK, membersList)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération de la liste des membres : ${e.message}")
                    )
                }
            }

            // GET /api/residences/{id}/logements : List units
            get("/api/residences/{id}/logements") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""

                try {
                    // Check if member exists in residence_members
                    val isMember = transaction {
                        exec("SELECT 1 FROM residence_members WHERE user_id = '$userId'::uuid AND residence_id = '$residenceId'::uuid AND status = 'ACCEPTED'") { rs ->
                            rs.next()
                        } ?: false
                    }

                    if (!isMember) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : vous ne faites pas partie de cette résidence."))
                        return@get
                    }

                    val list = transaction {
                        Logement.find { Logements.residenceId eq UUID.fromString(residenceId) }.map {
                            LogementDto(
                                id = it.id.value.toString(),
                                residenceId = it.residence.id.value.toString(),
                                name = it.name,
                                floor = it.floor,
                                type = it.type,
                                nominalRent = it.nominalRent,
                                serviceCharges = it.serviceCharges,
                                initialElectricityIndex = it.initialElectricityIndex,
                                status = it.status
                            )
                        }
                    }

                    call.respond(HttpStatusCode.OK, list)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération des logements : ${e.message}")
                    )
                }
            }

            // POST /api/residences/{id}/logements : Create units
            post("/api/residences/{id}/logements") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""

                try {
                    // Verify if calling user has ADMIN or RESIDENCE_MANAGER role inside residence_members
                    val userRole = transaction {
                        exec("SELECT role FROM residence_members WHERE user_id = '$userId'::uuid AND residence_id = '$residenceId'::uuid AND status = 'ACCEPTED'") { rs ->
                            if (rs.next()) rs.getString(1) else null
                        }
                    }

                    if (userRole == null || (userRole != "OWNER" && userRole != "ADMIN" && userRole != "RESIDENCE_MANAGER")) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : Seuls les administrateurs et gestionnaires de cette résidence peuvent ajouter des logements."))
                        return@post
                    }

                    val request = call.receive<LogementCreateRequest>()

                    // Validation
                    if (request.name.isBlank() || request.floor.isBlank() || request.type.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Veuillez remplir tous les champs obligatoires."))
                        return@post
                    }
                    if (request.nominalRent < 0.0 || request.serviceCharges < 0.0 || request.initialElectricityIndex < 0.0) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Les montants financiers et d'index d'électricité doivent être supérieurs ou égaux à 0."))
                        return@post
                    }

                    val dto = transaction {
                        val activeRes = Residence.findById(UUID.fromString(residenceId)) ?: throw Exception("Résidence introuvable.")
                        
                        val newLogement = Logement.new {
                            residence = activeRes
                            name = request.name
                            floor = request.floor
                            type = request.type
                            nominalRent = request.nominalRent
                            serviceCharges = request.serviceCharges
                            initialElectricityIndex = request.initialElectricityIndex
                            createdAt = LocalDateTime.now()
                            updatedAt = LocalDateTime.now()
                            status = "AVAILABLE" // Forced initial business state
                        }

                        newLogement.flush()

                        LogementDto(
                            id = newLogement.id.value.toString(),
                            residenceId = newLogement.residence.id.value.toString(),
                            name = newLogement.name,
                            floor = newLogement.floor,
                            type = newLogement.type,
                            nominalRent = newLogement.nominalRent,
                            serviceCharges = newLogement.serviceCharges,
                            initialElectricityIndex = newLogement.initialElectricityIndex,
                            status = newLogement.status
                        )
                    }

                    call.respond(HttpStatusCode.Created, dto)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la création du logement : ${e.message}")
                    )
                }
            }

            // POST /api/logements/{id}/baux : Create a lease agreement
            post("/api/logements/{id}/baux") {
                val logementId = call.parameters["id"] ?: ""
                // Placeholder: receive LeaseCreateRequest, insert in Baux table, calculate V_initial, set Logement status to OCCUPIED
                call.respond(HttpStatusCode.Created, mapOf("logementId" to logementId, "message" to "Contrat de bail créé"))
            }

            // PUT /api/baux/{id}/payment : Log a deposit, down payment, or lease balance payment
            put("/api/baux/{id}/payment") {
                val leaseId = call.parameters["id"] ?: ""
                // Placeholder: receive payment body, update lease status, generate a financial_transaction entry
                call.respond(HttpStatusCode.OK, mapOf("leaseId" to leaseId, "status" to "Paiement enregistré"))
            }

            // POST /api/logements/{id}/electricity : Enter new meter index and generate statement
            post("/api/logements/{id}/electricity") {
                val logementId = call.parameters["id"] ?: ""
                // Placeholder: receive ElectricityStatementCreateRequest, query oldIndex, calculate amountDue = (newIndex - oldIndex) * kWhPrice, insert statement in DB
                call.respond(HttpStatusCode.Created, mapOf("logementId" to logementId, "message" to "Relevé d'électricité enregistré"))
            }

            // GET /api/residences/{id}/electricity/statements : List statements with filters
            get("/api/residences/{id}/electricity/statements") {
                val residenceId = call.parameters["id"] ?: ""
                val floor = call.request.queryParameters["floor"]
                val status = call.request.queryParameters["status"]
                // Placeholder: select statements with filters
                call.respond(HttpStatusCode.OK, mapOf("residenceId" to residenceId, "statements" to emptyList<String>()))
            }

            // GET /api/residences/{id}/electricity/export-pdf : Generates the "Eco-Print" PDF
            get("/api/residences/{id}/electricity/export-pdf") {
                val residenceId = call.parameters["id"] ?: ""
                // Placeholder: fetch selected statements, format and generate Eco-Print PDF bytes
                call.respondBytes("PDF Content Placeholder".toByteArray(), ContentType.Application.Pdf)
            }

            // POST /api/logements/{id}/tickets : Open a maintenance ticket
            post("/api/logements/{id}/tickets") {
                val logementId = call.parameters["id"] ?: ""
                // Placeholder: receive TicketCreateRequest, save to Tickets table
                call.respond(HttpStatusCode.Created, mapOf("logementId" to logementId, "message" to "Ticket de maintenance ouvert"))
            }

            // PUT /api/tickets/{id}/status : Update status (OPEN -> IN_PROGRESS -> CLOSED)
            put("/api/tickets/{id}/status") {
                val ticketId = call.parameters["id"] ?: ""
                // Placeholder: receive TicketUpdateRequest, if status is CLOSED requires intervention_cost and triggers an EXPENSE in financial_transactions
                call.respond(HttpStatusCode.OK, mapOf("ticketId" to ticketId, "message" to "Ticket mis à jour"))
            }

            // GET /api/residences/{id}/transactions : Log or list operational expenses
            get("/api/residences/{id}/transactions") {
                val residenceId = call.parameters["id"] ?: ""
                // Placeholder: select from FinancialTransactions where residenceId = id
                call.respond(HttpStatusCode.OK, mapOf("residenceId" to residenceId, "transactions" to emptyList<String>()))
            }

            // GET /api/residences/{id}/dashboard : Returns Cashflow, occupancy rate, delinquency rate
            get("/api/residences/{id}/dashboard") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""

                // Context Guard & Middleware: verify user role inside residence_members
                val userRole = transaction {
                    // Query role in residence_members
                    exec("SELECT role FROM residence_members WHERE user_id = '$userId'::uuid AND residence_id = '$residenceId'::uuid AND status = 'ACCEPTED'") { rs ->
                        if (rs.next()) rs.getString(1) else null
                    }
                }

                if (userRole == null) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ErrorResponse("Accès interdit: Vous ne faites pas partie de cette résidence.")
                    )
                    return@get
                }

                val startDate = call.request.queryParameters["start_date"]
                val endDate = call.request.queryParameters["end_date"]

                // Placeholder: calculate Net Cashflow (Income - Expense), occupancy rate (occupied units / total units), delinquency rate
                call.respond(HttpStatusCode.OK, mapOf(
                    "residenceId" to residenceId,
                    "userRoleInResidence" to userRole,
                    "netCashflow" to 1250000.0, // Mock calculations in XOF
                    "occupancyRate" to 82.5,     // Percentage
                    "delinquencyRate" to 14.2,    // Percentage
                    "activeTickets" to 3
                ))
            }
        }
    }
}
