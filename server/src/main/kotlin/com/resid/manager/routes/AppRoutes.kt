package com.resid.manager.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.resid.manager.auth.JwtConfig
import com.resid.manager.data.*
import com.resid.manager.dto.*
import com.resid.manager.service.ElectricityService
import com.resid.manager.service.PdfService
import com.resid.manager.service.TicketService
import com.resid.manager.service.DashboardService
import com.resid.manager.service.FinanceOperationService
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
                        updatedAt = LocalDateTime.now()
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
        // GET /api/equipements : List predefined equipments (Publicly accessible helper)
        get("/api/equipements") {
            try {
                val list = transaction {
                    Equipement.all().map {
                        EquipementDto(id = it.id.value.toString(), key = it.key, label = it.label)
                    }
                }
                call.respond(HttpStatusCode.OK, list)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Erreur lors de la récupération des équipements : ${e.message}")
                )
            }
        }

        // GET /api/residences/{id}/electricity/export-pdf : Generates the "Eco-Print" PDF (Publicly accessible for browser tabs printing)
        get("/api/residences/{id}/electricity/export-pdf") {
            val residenceId = call.parameters["id"] ?: ""
            val statementIdsParam = call.request.queryParameters["ids"]

            try {
                val pdfBytes = transaction {
                    val dbResidence = Residence.findById(UUID.fromString(residenceId))
                        ?: throw Exception("Résidence introuvable.")

                    val statementsToPrint = if (!statementIdsParam.isNullOrBlank()) {
                        val uuids = statementIdsParam.split(",").map { UUID.fromString(it.trim()) }
                        ElectricityStatement.all().filter { it.id.value in uuids }
                    } else {
                        ElectricityStatement.all()
                            .filter { it.logement.residence.id.value == UUID.fromString(residenceId) }
                            .sortedByDescending { it.statementDate }
                            .take(4)
                    }.map {
                        ElectricityStatementDto(
                            id = it.id.value.toString(),
                            logementId = it.logement.id.value.toString(),
                            previousIndex = it.oldIndex,
                            newIndex = it.newIndex,
                            kWhPriceApplied = it.kWhPriceApplied,
                            amountDue = it.amountDue,
                            statementDate = it.statementDate.toString(),
                            status = if (it.status == "PAID") StatementStatus.PAID else StatementStatus.UNPAID,
                            createdAt = it.createdAt.toString(),
                            updatedAt = it.updatedAt.toString()
                        )
                    }

                    PdfService.generateEcoPrintPdf(statementsToPrint, dbResidence.name)
                }

                call.respondBytes(pdfBytes, ContentType.Application.Pdf)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Erreur lors de l'export PDF."))
            }
        }

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
                        val ownedList = Residence.all().filter { res ->
                            ResidenceMembers.select(ResidenceMembers.role).where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                (ResidenceMembers.residenceId eq res.id.value) and
                                (ResidenceMembers.role eq "OWNER") and
                                (ResidenceMembers.status eq "ACCEPTED")
                            }.count() > 0
                        }.map { res ->
                            val totalUnits = Logement.find { Logements.residenceId eq res.id.value }.count()
                            ResidenceSummaryItem(
                                id = res.id.value.toString(),
                                name = res.name,
                                address = res.address,
                                photoUrl = res.photoUrl,
                                totalUnits = totalUnits.toInt(),
                                currencySymbol = res.currency.symbol,
                                currencyCode = res.currency.code
                            )
                        }

                        // 2. Query associated residences (where role is not m.role = 'OWNER')
                        val associatedList = Residence.all().filter { res ->
                            ResidenceMembers.select(ResidenceMembers.role).where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                (ResidenceMembers.residenceId eq res.id.value) and
                                (ResidenceMembers.role neq "OWNER") and
                                (ResidenceMembers.status eq "ACCEPTED")
                            }.count() > 0
                        }.map { res ->
                            val roleStr = ResidenceMembers.select(ResidenceMembers.role).where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                (ResidenceMembers.residenceId eq res.id.value)
                            }.map { it[ResidenceMembers.role] }.first()
                            
                            val totalUnits = Logement.find { Logements.residenceId eq res.id.value }.count()
                            AssociatedResidenceItem(
                                id = res.id.value.toString(),
                                name = res.name,
                                address = res.address,
                                photoUrl = res.photoUrl,
                                role = roleStr,
                                totalUnits = totalUnits.toInt(),
                                currencySymbol = res.currency.symbol,
                                currencyCode = res.currency.code
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
                        val selectedCurrency = CurrencyEntity.find { Currencies.code eq request.defaultCurrency.uppercase() }.firstOrNull()
                            ?: CurrencyEntity.find { Currencies.code eq "XOF" }.first()

                        // 1. Insert residence
                        val r = Residence.new {
                            name = request.name
                            address = request.address
                            photoUrl = null
                            currency = selectedCurrency
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
                        totalUnits = 0,
                        currencySymbol = transaction { newResidence.currency.symbol },
                        currencyCode = transaction { newResidence.currency.code }
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

            // PUT /api/residences/{id}/currency : Update ONLY residence currency
            put("/api/residences/{id}/currency") {
                val residenceId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<Map<String, String>>()
                    val code = request["currencyCode"] ?: throw Exception("Code devise manquant.")
                    
                    val updated = transaction {
                        val dbResidence = Residence.findById(UUID.fromString(residenceId))
                            ?: throw Exception("Résidence introuvable.")

                        val selectedCurrency = CurrencyEntity.find { Currencies.code eq code.uppercase() }.firstOrNull()
                            ?: throw Exception("Devise introuvable.")

                        dbResidence.currency = selectedCurrency
                        dbResidence.updatedAt = LocalDateTime.now()
                        dbResidence.flush()

                        val totalUnits = Logement.find { Logements.residenceId eq dbResidence.id.value }.count()
                        ResidenceSummaryItem(
                            id = dbResidence.id.value.toString(),
                            name = dbResidence.name,
                            address = dbResidence.address,
                            photoUrl = dbResidence.photoUrl,
                            totalUnits = totalUnits.toInt(),
                            currencySymbol = dbResidence.currency.symbol,
                            currencyCode = dbResidence.currency.code
                        )
                    }
                    call.respond(HttpStatusCode.OK, updated)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur lors du changement de la devise."))
                }
            }

            // PUT /api/residences/{id} : Update a residence
            put("/api/residences/{id}") {
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
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit: Seuls les propriétaires et administrateurs peuvent modifier cette résidence."))
                        return@put
                    }

                    val request = call.receive<ResidenceCreateRequest>()

                    // Validation
                    if (request.name.isBlank() || request.address.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Veuillez remplir tous les champs obligatoires."))
                        return@put
                    }
                    if (request.kWhPrice < 0.0) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Le prix du kWh doit être supérieur ou égal à 0."))
                        return@put
                    }

                    val updatedSummary = transaction {
                        val dbResidence = Residence.findById(UUID.fromString(residenceId)) ?: throw Exception("Résidence introuvable.")
                        val selectedCurrency = CurrencyEntity.find { Currencies.code eq request.defaultCurrency.uppercase() }.firstOrNull()
                            ?: CurrencyEntity.find { Currencies.code eq "XOF" }.first()

                        dbResidence.name = request.name
                        dbResidence.address = request.address
                        dbResidence.currency = selectedCurrency
                        dbResidence.kWhPrice = request.kWhPrice
                        dbResidence.updatedAt = LocalDateTime.now()

                        dbResidence.flush()

                        val totalUnits = Logement.find { Logements.residenceId eq dbResidence.id.value }.count()

                        ResidenceSummaryItem(
                            id = dbResidence.id.value.toString(),
                            name = dbResidence.name,
                            address = dbResidence.address,
                            photoUrl = dbResidence.photoUrl,
                            totalUnits = totalUnits.toInt(),
                            currencySymbol = dbResidence.currency.symbol,
                            currencyCode = dbResidence.currency.code
                        )
                    }

                    call.respond(HttpStatusCode.OK, updatedSummary)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la modification de la résidence : ${e.message}")
                    )
                }
            }

            // GET /api/residences/search?name=... : Search for a residence by name
            get("/api/residences/search") {
                val searchName = call.request.queryParameters["name"] ?: ""

                try {
                    val results = transaction {
                        Residence.all().filter { 
                            it.name.contains(searchName, ignoreCase = true) 
                        }.map { res ->
                            val totalUnits = Logement.find { Logements.residenceId eq res.id.value }.count()
                            ResidenceSummaryItem(
                                id = res.id.value.toString(),
                                name = res.name,
                                address = res.address,
                                photoUrl = res.photoUrl,
                                totalUnits = totalUnits.toInt(),
                                currencySymbol = res.currency.symbol,
                                currencyCode = res.currency.code
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

            // GET /api/users/tenant/search?q=name Search User by his name or his residence role
            get("/api/users/search") {
                val query = call.request.queryParameters["q"] ?: ""
                val residenceId = call.request.queryParameters["residenceId"] ?: ""
                val role = call.request.queryParameters["role"] ?: ""
                try {
                    val users = transaction {
                        if (residenceId.isNotBlank()) {
                            (Users innerJoin ResidenceMembers)
                                .select(Users.id, Users.firstName, Users.lastName, Users.email, ResidenceMembers.role)
                                .where {
                                    ((Users.email like "%$query%") or
                                        (Users.firstName like "%$query%") or
                                        (Users.lastName like "%$query%")) and
                                     ((ResidenceMembers.residenceId eq UUID.fromString(residenceId)) or
                                        (ResidenceMembers.role like  role))
                                }
                                .withDistinct()
                                .map { row ->
                                    UserSearchDto(
                                        id = row[Users.id].value.toString(),
                                        email = row[Users.email],
                                        name = "${row[Users.firstName]} ${row[Users.lastName]}",
                                        role = row[ResidenceMembers.role]
                                    )
                                }
                        } else {
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

                    }
                    call.respond(HttpStatusCode.OK, users)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur lors de la recherche de locataires: ${e.message}"))
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
                                status = it.status,
                                equipements = it.equipements.map { eq ->
                                    EquipementDto(id = eq.id.value.toString(), key = eq.key, label = eq.label)
                                }
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

                        // Attach selected equipments
                        if (request.equipementIds.isNotEmpty()) {
                            val selectedEq = request.equipementIds.mapNotNull { eqId ->
                                Equipement.findById(UUID.fromString(eqId))
                            }
                            newLogement.equipements = SizedCollection(selectedEq)
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
                            status = newLogement.status,
                            equipements = newLogement.equipements.map { eq ->
                                EquipementDto(id = eq.id.value.toString(), key = eq.key, label = eq.label)
                            }
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

            // DELETE /api/residences/{id}/logements/{logementId} : Delete a housing unit (logement)
            delete("/api/residences/{id}/logements/{logementId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""
                val logementId = call.parameters["logementId"] ?: ""

                try {
                    // Check user roles: OWNER, ADMIN, RESIDENCE_MANAGER
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

                    if (userRole == null || (userRole != "OWNER" && userRole != "ADMIN" && userRole != "RESIDENCE_MANAGER")) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : Seuls les administrateurs et gestionnaires peuvent supprimer des logements."))
                        return@delete
                    }

                    // Perform deletion inside transaction using Exposed DSL
                    transaction {
                        Logements.deleteWhere { Logements.id eq UUID.fromString(logementId) }
                    }

                    call.respond(HttpStatusCode.OK, mapOf("message" to "Le logement a été supprimé de la base de données avec succès !"))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la suppression du logement : ${e.message}")
                    )
                }
            }

            // PUT /api/residences/{id}/logements/{logementId} : Update a housing unit (logement)
            put("/api/residences/{id}/logements/{logementId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""
                val logementId = call.parameters["logementId"] ?: ""

                try {
                    // Check user roles: OWNER, ADMIN, RESIDENCE_MANAGER
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

                    if (userRole == null || (userRole != "OWNER" && userRole != "ADMIN" && userRole != "RESIDENCE_MANAGER")) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : Seuls les administrateurs et gestionnaires peuvent modifier des logements."))
                        return@put
                    }

                    val request = call.receive<LogementCreateRequest>()

                    // Validation
                    if (request.name.isBlank() || request.floor.isBlank() || request.type.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Veuillez remplir tous les champs obligatoires."))
                        return@put
                    }
                    if (request.nominalRent < 0.0 || request.serviceCharges < 0.0 || request.initialElectricityIndex < 0.0) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Les montants financiers et d'index d'électricité doivent être supérieurs ou égaux à 0."))
                        return@put
                    }

                    val updatedDto = transaction {
                        val dbLogement = Logement.findById(UUID.fromString(logementId)) ?: throw Exception("Logement introuvable.")
                        
                        dbLogement.name = request.name
                        dbLogement.floor = request.floor
                        dbLogement.type = request.type
                        dbLogement.nominalRent = request.nominalRent
                        dbLogement.serviceCharges = request.serviceCharges
                        dbLogement.initialElectricityIndex = request.initialElectricityIndex
                        dbLogement.updatedAt = LocalDateTime.now()

                        // Update selected equipments
                        val selectedEq = request.equipementIds.mapNotNull { eqId ->
                            Equipement.findById(UUID.fromString(eqId))
                        }
                        dbLogement.equipements = SizedCollection(selectedEq)

                        dbLogement.flush()

                        LogementDto(
                            id = dbLogement.id.value.toString(),
                            residenceId = dbLogement.residence.id.value.toString(),
                            name = dbLogement.name,
                            floor = dbLogement.floor,
                            type = dbLogement.type,
                            nominalRent = dbLogement.nominalRent,
                            serviceCharges = dbLogement.serviceCharges,
                            initialElectricityIndex = dbLogement.initialElectricityIndex,
                            status = dbLogement.status,
                            equipements = dbLogement.equipements.map { eq ->
                                EquipementDto(id = eq.id.value.toString(), key = eq.key, label = eq.label)
                            }
                        )
                    }

                    call.respond(HttpStatusCode.OK, updatedDto)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la modification du logement : ${e.message}")
                    )
                }
            }

            // POST /api/logements/{id}/baux : Create a lease agreement
            post("/api/logements/{id}/baux") {
                val logementId = call.parameters["id"] ?: ""

                try {
                    val request = call.receive<LeaseCreateRequest>()
                    val parsedStart = LocalDate.parse(request.startDate)
                    val parsedEnd = LocalDate.parse(request.endDate)
                    val duration = java.time.temporal.ChronoUnit.MONTHS.between(parsedStart, parsedEnd).toInt()

                    val createdLeaseDto = transaction {
                        // 1. Fetch logement & verify status == "AVAILABLE"
                        val dbLogement = Logement.findById(UUID.fromString(logementId)) 
                            ?: throw Exception("Logement introuvable.")
                        
                        if (dbLogement.status != "AVAILABLE") {
                            throw Exception("Ce logement n'est plus disponible pour une location.")
                        }

                        // 2. Fetch or create tenant user
                        val dbTenant = if (request.tenantId != null) {
                            User.findById(UUID.fromString(request.tenantId))
                                ?: throw Exception("Locataire (utilisateur) introuvable.")
                        } else if (request.inlineTenant != null) {
                            val inline = request.inlineTenant!!
                            // Check if email already exists
                            val existingUser = User.find { Users.email eq inline.email }.firstOrNull()
                            if (existingUser != null) {
                                existingUser
                            } else {
                                val tempPasswordHash = BCrypt.withDefaults().hashToString(12, "Locataire123!".toCharArray())
                                val newUser = User.new {
                                    email = inline.email
                                    passwordHash = tempPasswordHash
                                    firstName = inline.firstName
                                    lastName = inline.lastName
                                    birthDate = null
                                    phone = inline.phone
                                    createdAt = LocalDateTime.now()
                                    updatedAt = LocalDateTime.now()
                                }
                                newUser.flush()

                                // Instantly register as a member of this residence
                                ResidenceMembers.insert {
                                    it[userId] = newUser.id.value
                                    it[residenceId] = dbLogement.residence.id.value
                                    it[role] = "TENANT"
                                    it[status] = "ACCEPTED"
                                    it[createdAt] = LocalDateTime.now()
                                }
                                newUser
                            }
                        } else {
                            throw Exception("Veuillez sélectionner un locataire existant ou remplir le formulaire d'inscription inline.")
                        }

                        // 3. Check if tenant already has an ongoing lease (not TERMINATED)
                        val hasOngoingLease = Baux
                            .select(Baux.id)
                            .where { 
                                (Baux.tenantId eq dbTenant.id.value) and 
                                (Baux.status neq "TERMINATED") 
                            }
                            .count() > 0

                        if (hasOngoingLease) {
                            throw Exception("Ce locataire possède déjà un contrat de bail en cours dans l'application.")
                        }

                        // 4. Calculate Total Requirement and Initial Status based on Advanced Payments
                        val isMonthly = request.paymentFrequency == "MONTHLY"
                        val rentAndCharges = dbLogement.nominalRent + dbLogement.serviceCharges
                        val advanceMonths = if (isMonthly) 1 else (request.advanceMonths ?: 12)
                        
                        val requiredFirstRent = advanceMonths * rentAndCharges
                        val totalRequiredToPay = request.depositAmount + requiredFirstRent
                        val initialPayment = request.advancePaymentAmount ?: 0.0

                        val initialStatusStr = if (initialPayment >= totalRequiredToPay) {
                            "PENDING_SIGNATURE"
                        } else if (initialPayment > 0.0) {
                            "DOWN_PAYMENT_PAID"
                        } else {
                            "PENDING_PAYMENT"
                        }

                        // Create Lease record
                        val newLease = Lease.new {
                            this.logement = dbLogement
                            this.tenant = dbTenant
                            this.durationMonths = if (duration <= 0) 12 else duration
                            this.paymentFrequency = request.paymentFrequency
                            this.depositAmount = request.depositAmount
                            this.depositStatus = if (initialPayment >= request.depositAmount) "PAID" else "PENDING"
                            this.status = initialStatusStr
                            this.startDate = parsedStart
                            this.endDate = parsedEnd
                            this.advanceMonths = advanceMonths
                            this.advancePaymentAmount = initialPayment
                            this.createdAt = LocalDateTime.now()
                            this.updatedAt = LocalDateTime.now()
                        }

                        // 5. Update logement status to OCCUPIED
                        dbLogement.status = "OCCUPIED"
                        dbLogement.flush()
                        newLease.flush()

                        // 6. Generate financial transaction entries if an advance payment was actually made!
                        if (initialPayment > 0.0) {
                            val paidCaution = minOf(initialPayment, request.depositAmount)
                            val paidRent = maxOf(0.0, initialPayment - request.depositAmount)

                            if (paidCaution > 0.0) {
                                FinancialTransaction.new {
                                    this.residence = dbLogement.residence
                                    this.type = "INCOME"
                                    this.category = "Deposit"
                                    this.amount = paidCaution
                                    this.description = "Acompte caution à la signature pour le logement ${dbLogement.name}"
                                    this.relatedEntityType = "BAIL"
                                    this.relatedEntityId = newLease.id.value
                                    this.transactionDate = LocalDate.now()
                                    this.createdAt = LocalDateTime.now()
                                    this.updatedAt = LocalDateTime.now()
                                }
                            }

                            if (paidRent > 0.0) {
                                FinancialTransaction.new {
                                    this.residence = dbLogement.residence
                                    this.type = "INCOME"
                                    this.category = "Rent"
                                    this.amount = paidRent
                                    this.description = "Acompte loyer d'avance à la signature pour le logement ${dbLogement.name}"
                                    this.relatedEntityType = "BAIL"
                                    this.relatedEntityId = newLease.id.value
                                    this.transactionDate = LocalDate.now()
                                    this.createdAt = LocalDateTime.now()
                                    this.updatedAt = LocalDateTime.now()
                                }
                            }
                        }

                        val leaseStatusEnum = try {
                            LeaseStatus.valueOf(initialStatusStr)
                        } catch (e: Exception) {
                            LeaseStatus.PENDING_PAYMENT
                        }

                        LeaseDto(
                            id = newLease.id.value.toString(),
                            logementId = newLease.logement.id.value.toString(),
                            tenantId = newLease.tenant.id.value.toString(),
                            startDate = newLease.startDate.toString(),
                            endDate = newLease.endDate.toString(),
                            depositAmount = newLease.depositAmount,
                            monthlyRentAtSign = request.monthlyRentAtSign,
                            status = leaseStatusEnum,
                            createdAt = newLease.createdAt.toString(),
                            updatedAt = newLease.updatedAt.toString(),
                            paymentFrequency = newLease.paymentFrequency,
                            advanceMonths = newLease.advanceMonths
                        )
                    }

                    call.respond(HttpStatusCode.Created, createdLeaseDto)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la création du contrat de bail : ${e.message}")
                    )
                }
            }

            // GET /api/residences/{id}/baux : List all lease agreements for a residence
            get("/api/residences/{id}/baux") {
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

                    val leasesList = transaction {
                        // Find baux for all logements in this residence
                        Lease.all().filter { it.logement.residence.id.value == UUID.fromString(residenceId) }.map { lease ->
                            val previousPayments = FinancialTransaction.find { 
                                (FinancialTransactions.relatedEntityType eq "BAIL") and 
                                (FinancialTransactions.relatedEntityId eq lease.id.value) 
                            }.map { tx ->
                                LeasePaymentDto(
                                    id = tx.id.value.toString(),
                                    category = if (tx.category == "Deposit") "CAUTION" else "LOYER",
                                    amount = tx.amount,
                                    description = tx.description,
                                    transactionDate = tx.transactionDate.toString()
                                )
                            }
                            LeaseDto(
                                id = lease.id.value.toString(),
                                logementId = lease.logement.id.value.toString(),
                                tenantId = lease.tenant.id.value.toString(),
                                startDate = lease.startDate.toString(),
                                endDate = lease.endDate.toString(),
                                depositAmount = lease.depositAmount,
                                monthlyRentAtSign = lease.logement.nominalRent,
                                status = try {
                                    LeaseStatus.valueOf(lease.status)
                                } catch (e: Exception) {
                                    LeaseStatus.PENDING_PAYMENT
                                },
                                createdAt = lease.createdAt.toString(),
                                updatedAt = lease.updatedAt.toString(),
                                paymentFrequency = lease.paymentFrequency,
                                advanceMonths = lease.advanceMonths,
                                payments = previousPayments
                            )
                        }
                    }

                    call.respond(HttpStatusCode.OK, leasesList)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération des contrats de bail : ${e.message}")
                    )
                }
            }

            // PUT /api/baux/{id}/payment : Log a deposit, down payment, or lease balance payment
            put("/api/baux/{id}/payment") {
                val leaseId = call.parameters["id"] ?: ""

                try {
                    val request = call.receive<LeasePaymentRequest>()

                    val updatedLeaseDto = transaction {
                        // 1. Fetch lease record
                        val dbLease = Lease.findById(UUID.fromString(leaseId)) 
                            ?: throw Exception("Contrat de bail introuvable.")

                        // 2. Fetch all previous caution (Deposit) payments registered
                        val previousPaidCaution = FinancialTransaction.find { 
                            (FinancialTransactions.relatedEntityType eq "BAIL") and 
                            (FinancialTransactions.relatedEntityId eq UUID.fromString(leaseId)) and 
                            (FinancialTransactions.type eq "INCOME") and
                            (FinancialTransactions.category eq "Deposit")
                        }.sumOf { it.amount }

                        // 3. Fetch all previous rent payments registered
                        val previousPaidRent = FinancialTransaction.find { 
                            (FinancialTransactions.relatedEntityType eq "BAIL") and 
                            (FinancialTransactions.relatedEntityId eq UUID.fromString(leaseId)) and 
                            (FinancialTransactions.type eq "INCOME") and
                            ((FinancialTransactions.category eq "Rent") or (FinancialTransactions.category eq "Lease Payment"))
                        }.sumOf { it.amount }

                        // 4. Incorporate the new incoming payment
                        val totalPaidCaution = previousPaidCaution + if (request.category == "CAUTION") request.amountPaid else 0.0
                        val totalPaidRent = previousPaidRent + if (request.category == "LOYER") request.amountPaid else 0.0

                        // 5. Calculate required amounts
                        val requiredCaution = dbLease.depositAmount
                        val requiredRent = dbLease.advanceMonths * (dbLease.logement.nominalRent + dbLease.logement.serviceCharges)

                        // 6. Determine status update based on dual ledger
                        val newStatusStr = if (totalPaidCaution >= requiredCaution && totalPaidRent >= requiredRent) {
                            "PENDING_SIGNATURE"
                        } else if (totalPaidCaution > 0.0 || totalPaidRent > 0.0) {
                            "DOWN_PAYMENT_PAID"
                        } else {
                            "PENDING_PAYMENT"
                        }

                        // Map status back to the database record
                        dbLease.status = newStatusStr
                        if (totalPaidCaution >= requiredCaution) {
                            dbLease.depositStatus = "PAID"
                        }
                        dbLease.updatedAt = LocalDateTime.now()
                        dbLease.flush()

                        // 7. Generate financial transaction entry
                        FinancialTransaction.new {
                            this.residence = dbLease.logement.residence
                            this.type = "INCOME"
                            this.category = if (request.category == "CAUTION") "Deposit" else "Rent"
                            this.amount = request.amountPaid
                            this.description = if (request.category == "CAUTION") {
                                "Versement partiel caution pour le logement ${dbLease.logement.name}"
                            } else {
                                "Versement partiel loyer pour le logement ${dbLease.logement.name}"
                            }
                            this.relatedEntityType = "BAIL"
                            this.relatedEntityId = UUID.fromString(leaseId)
                            this.transactionDate = LocalDate.now()
                            this.createdAt = LocalDateTime.now()
                            this.updatedAt = LocalDateTime.now()
                        }

                        val leaseStatusEnum = try {
                            LeaseStatus.valueOf(newStatusStr)
                        } catch (e: Exception) {
                            LeaseStatus.PENDING_PAYMENT
                        }

                        val previousPayments = FinancialTransaction.find { 
                            (FinancialTransactions.relatedEntityType eq "BAIL") and 
                            (FinancialTransactions.relatedEntityId eq dbLease.id.value) 
                        }.map { tx ->
                            LeasePaymentDto(
                                id = tx.id.value.toString(),
                                category = if (tx.category == "Deposit") "CAUTION" else "LOYER",
                                amount = tx.amount,
                                description = tx.description,
                                transactionDate = tx.transactionDate.toString()
                            )
                        }

                        LeaseDto(
                            id = dbLease.id.value.toString(),
                            logementId = dbLease.logement.id.value.toString(),
                            tenantId = dbLease.tenant.id.value.toString(),
                            startDate = dbLease.startDate.toString(),
                            endDate = dbLease.endDate.toString(),
                            depositAmount = dbLease.depositAmount,
                            monthlyRentAtSign = dbLease.logement.nominalRent,
                            status = leaseStatusEnum,
                            createdAt = dbLease.createdAt.toString(),
                            updatedAt = dbLease.updatedAt.toString(),
                            paymentFrequency = dbLease.paymentFrequency,
                            advanceMonths = dbLease.advanceMonths,
                            payments = previousPayments
                        )
                    }

                    call.respond(HttpStatusCode.OK, updatedLeaseDto)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de l'enregistrement du paiement : ${e.message}")
                    )
                }
            }

            // PUT /api/baux/{id}/status : Update lease status (e.g. to SIGNED_ACTIVE or TERMINATED)
            put("/api/baux/{id}/status") {
                val leaseId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<LeaseUpdateRequest>()
                    val updatedLeaseDto = transaction {
                        val dbLease = Lease.findById(UUID.fromString(leaseId)) 
                            ?: throw Exception("Contrat de bail introuvable.")

                        request.status?.let { 
                            dbLease.status = it.name 
                            if (it == LeaseStatus.TERMINATED) {
                                dbLease.logement.status = "AVAILABLE"
                                dbLease.logement.flush()
                            }
                        }
                        dbLease.updatedAt = LocalDateTime.now()
                        dbLease.flush()

                        val previousPayments = FinancialTransaction.find { 
                            (FinancialTransactions.relatedEntityType eq "BAIL") and 
                            (FinancialTransactions.relatedEntityId eq dbLease.id.value) 
                        }.map { tx ->
                            LeasePaymentDto(
                                id = tx.id.value.toString(),
                                category = if (tx.category == "Deposit") "CAUTION" else "LOYER",
                                amount = tx.amount,
                                description = tx.description,
                                transactionDate = tx.transactionDate.toString()
                            )
                        }

                        LeaseDto(
                            id = dbLease.id.value.toString(),
                            logementId = dbLease.logement.id.value.toString(),
                            tenantId = dbLease.tenant.id.value.toString(),
                            startDate = dbLease.startDate.toString(),
                            endDate = dbLease.endDate.toString(),
                            depositAmount = dbLease.depositAmount,
                            monthlyRentAtSign = dbLease.logement.nominalRent,
                            status = try {
                                LeaseStatus.valueOf(dbLease.status)
                            } catch (e: Exception) {
                                LeaseStatus.PENDING_PAYMENT
                            },
                            createdAt = dbLease.createdAt.toString(),
                            updatedAt = dbLease.updatedAt.toString(),
                            paymentFrequency = dbLease.paymentFrequency,
                            advanceMonths = dbLease.advanceMonths,
                            payments = previousPayments
                        )
                    }
                    call.respond(HttpStatusCode.OK, updatedLeaseDto)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la mise à jour du statut du bail : ${e.message}")
                    )
                }
            }

            // PUT /api/electricity/statements/{id}/status : Mark an electricity statement as PAID
            put("/api/electricity/statements/{id}/status") {
                val statementId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<ElectricityStatementUpdateRequest>()
                    val updated = transaction {
                        val stmt = ElectricityStatement.findById(UUID.fromString(statementId))
                            ?: throw Exception("Relevé d'électricité introuvable.")

                        stmt.status = request.status?.name ?: "PAID"
                        stmt.updatedAt = LocalDateTime.now()
                        stmt.flush()

                        // Update description of original Financial Transaction to reflect payment
                        val tx = FinancialTransaction.find { 
                            (FinancialTransactions.relatedEntityType eq "ELECTRICITY_STATEMENT") and 
                            (FinancialTransactions.relatedEntityId eq stmt.id.value) 
                        }.firstOrNull()

                        if (tx != null && !tx.description.startsWith("[PAYÉ]")) {
                            tx.description = "[PAYÉ] " + tx.description
                            tx.updatedAt = LocalDateTime.now()
                            tx.flush()
                        }

                        ElectricityStatementDto(
                            id = stmt.id.value.toString(),
                            logementId = stmt.logement.id.value.toString(),
                            previousIndex = stmt.oldIndex,
                            newIndex = stmt.newIndex,
                            kWhPriceApplied = stmt.kWhPriceApplied,
                            amountDue = stmt.amountDue,
                            statementDate = stmt.statementDate.toString(),
                            status = if (stmt.status == "PAID") StatementStatus.PAID else StatementStatus.UNPAID,
                            createdAt = stmt.createdAt.toString(),
                            updatedAt = stmt.updatedAt.toString()
                        )
                    }
                    call.respond(HttpStatusCode.OK, updated)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur de mise à jour."))
                }
            }

            // GET /api/logements/{id}/electricity/previous : Fetch the previous (locked/read-only) meter index
            get("/api/logements/{id}/electricity/previous") {
                val logementId = call.parameters["id"] ?: ""
                try {
                    val previous = transaction {
                        ElectricityService.getPreviousIndex(UUID.fromString(logementId))
                    }
                    call.respond(HttpStatusCode.OK, mapOf("previousIndex" to previous))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Erreur de chargement de l'index précédent."))
                }
            }

            // POST /api/logements/{id}/electricity : Enter new meter index and generate statement
            post("/api/logements/{id}/electricity") {
                val logementId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<ElectricityStatementCreateRequest>()
                    val created = transaction {
                        ElectricityService.submitStatement(
                            logementId = UUID.fromString(logementId),
                            newIndex = request.newIndex,
                            kWhPriceApplied = request.kWhPriceApplied,
                            dateStr = request.statementDate
                        )
                    }
                    call.respond(HttpStatusCode.Created, created)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur de validation de l'index."))
                }
            }

            // GET /api/residences/{id}/electricity/statements : List statements with filters
            get("/api/residences/{id}/electricity/statements") {
                val residenceId = call.parameters["id"] ?: ""
                val statusParam = call.request.queryParameters["status"]
                val logementParam = call.request.queryParameters["logementId"]
                val floorParam = call.request.queryParameters["floor"]
                val tenantParam = call.request.queryParameters["tenantName"]

                try {
                    val statements = transaction {
                        val list = ElectricityStatement.all().filter { 
                            it.logement.residence.id.value == UUID.fromString(residenceId) 
                        }
                        
                        var filtered = list

                        statusParam?.ifBlank { null }?.let { status ->
                            filtered = filtered.filter { it.status.uppercase() == status.uppercase() }
                        }

                        logementParam?.ifBlank { null }?.let { logId ->
                            filtered = filtered.filter { it.logement.id.value == UUID.fromString(logId) }
                        }

                        floorParam?.ifBlank { null }?.let { floor ->
                            filtered = filtered.filter { it.logement.floor.contains(floor, ignoreCase = true) }
                        }

                        tenantParam?.ifBlank { null }?.let { tenantName ->
                            filtered = filtered.filter { stmt ->
                                val activeLease = Lease.find { 
                                    (Baux.logementId eq stmt.logement.id) and (Baux.status eq "SIGNED_ACTIVE") 
                                }.firstOrNull()
                                val tenant = activeLease?.tenant
                                val fullName = "${tenant?.firstName} ${tenant?.lastName}"
                                fullName.contains(tenantName, ignoreCase = true)
                            }
                        }

                        filtered.sortedByDescending { it.statementDate }.map {
                            ElectricityStatementDto(
                                id = it.id.value.toString(),
                                logementId = it.logement.id.value.toString(),
                                previousIndex = it.oldIndex,
                                newIndex = it.newIndex,
                                kWhPriceApplied = it.kWhPriceApplied,
                                amountDue = it.amountDue,
                                statementDate = it.statementDate.toString(),
                                status = if (it.status == "PAID") StatementStatus.PAID else StatementStatus.UNPAID,
                                createdAt = it.createdAt.toString(),
                                updatedAt = it.updatedAt.toString()
                            )
                        }
                    }

                    call.respond(HttpStatusCode.OK, statements)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Erreur lors de la récupération des relevés."))
                }
            }



            // GET /api/residences/{id}/ticket-categories : List all ticket categories (global and custom to residence)
            get("/api/residences/{id}/ticket-categories") {
                val residenceId = call.parameters["id"] ?: ""
                try {
                    val list = transaction {
                        TicketCategoryEntity.all().filter { 
                            it.residence?.id?.value == null || it.residence?.id?.value == UUID.fromString(residenceId) 
                        }.map {
                            TicketCategoryDto(
                                id = it.id.value.toString(),
                                key = it.key,
                                label = it.label,
                                residenceId = it.residence?.id?.value?.toString()
                            )
                        }
                    }
                    call.respond(HttpStatusCode.OK, list)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Erreur de chargement des catégories."))
                }
            }

            // POST /api/residences/{id}/ticket-categories : Create custom ticket category
            post("/api/residences/{id}/ticket-categories") {
                val residenceId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<TicketCategoryDto>()
                    val created = transaction {
                        val dbResidence = Residence.findById(UUID.fromString(residenceId))
                            ?: throw Exception("Résidence introuvable.")

                        // Check if key already exists
                        val exists = TicketCategoryEntity.find { TicketCategories.key eq request.key.uppercase() }.count() > 0
                        if (exists) throw Exception("Cette clé de catégorie existe déjà.")

                        val entity = TicketCategoryEntity.new {
                            this.residence = dbResidence
                            this.key = request.key.uppercase()
                            this.label = request.label
                            this.createdAt = LocalDateTime.now()
                            this.updatedAt = LocalDateTime.now()
                        }
                        entity.flush()

                        TicketCategoryDto(
                            id = entity.id.value.toString(),
                            key = entity.key,
                            label = entity.label,
                            residenceId = entity.residence?.id?.value?.toString()
                        )
                    }
                    call.respond(HttpStatusCode.Created, created)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur de création de catégorie."))
                }
            }

            // PUT /api/ticket-categories/{id} : Update ticket category
            put("/api/ticket-categories/{id}") {
                val categoryId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<TicketCategoryDto>()
                    val updated = transaction {
                        val entity = TicketCategoryEntity.findById(UUID.fromString(categoryId))
                            ?: throw Exception("Catégorie de ticket introuvable.")

                        entity.label = request.label
                        entity.updatedAt = LocalDateTime.now()
                        entity.flush()

                        TicketCategoryDto(
                            id = entity.id.value.toString(),
                            key = entity.key,
                            label = entity.label,
                            residenceId = entity.residence?.id?.value?.toString()
                        )
                    }
                    call.respond(HttpStatusCode.OK, updated)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur de modification de catégorie."))
                }
            }

            // DELETE /api/ticket-categories/{id} : Delete custom ticket category
            delete("/api/ticket-categories/{id}") {
                val categoryId = call.parameters["id"] ?: ""
                try {
                    val success = transaction {
                        val entity = TicketCategoryEntity.findById(UUID.fromString(categoryId))
                            ?: throw Exception("Catégorie de ticket introuvable.")

                        if (entity.residence == null) {
                            throw Exception("Action interdite : Les catégories globales par défaut ne peuvent pas être supprimées.")
                        }

                        entity.delete()
                        true
                    }
                    call.respond(HttpStatusCode.OK, mapOf("success" to success, "message" to "Catégorie supprimée avec succès."))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur de suppression de catégorie."))
                }
            }

            // GET /api/residences/{id}/tickets : List all tickets for this residence
            get("/api/residences/{id}/tickets") {
                val residenceId = call.parameters["id"] ?: ""
                try {
                    val ticketsList = transaction {
                        Ticket.all().filter { it.logement.residence.id.value == UUID.fromString(residenceId) }.map {
                            TicketDto(
                                id = it.id.value.toString(),
                                logementId = it.logement.id.value.toString(),
                                creatorId = it.creator.id.value.toString(),
                                category = TicketCategoryDto(
                                    id = it.category.id.value.toString(),
                                    key = it.category.key,
                                    label = it.category.label,
                                    residenceId = it.category.residence?.id?.value?.toString()
                                ),
                                title = it.title,
                                description = it.description,
                                urgency = TicketUrgency.valueOf(it.urgency),
                                status = TicketStatus.valueOf(it.status),
                                interventionCost = it.interventionCost,
                                createdAt = it.createdAt.toString(),
                                updatedAt = it.updatedAt.toString()
                            )
                        }
                    }
                    call.respond(HttpStatusCode.OK, ticketsList)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Erreur de chargement des tickets."))
                }
            }

            // POST /api/logements/{id}/tickets : Open a maintenance ticket
            post("/api/logements/{id}/tickets") {
                val logementId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<TicketCreateRequest>()
                    val principal = call.principal<JWTPrincipal>()
                    val creatorIdStr = principal?.payload?.getClaim("userId")?.asString() 
                        ?: throw Exception("Utilisateur non authentifié.")

                    val created = transaction {
                        TicketService.createTicket(
                            logementId = UUID.fromString(logementId),
                            creatorId = UUID.fromString(creatorIdStr),
                            categoryId = UUID.fromString(request.categoryId),
                            title = request.title,
                            description = request.description,
                            urgency = request.urgency
                        )
                    }
                    call.respond(HttpStatusCode.Created, created)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur lors de la création du ticket."))
                }
            }

            // PUT /api/tickets/{id}/status : Update status (OPEN -> IN_PROGRESS -> CLOSED)
            put("/api/tickets/{id}/status") {
                val ticketId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<TicketUpdateRequest>()
                    val principal = call.principal<JWTPrincipal>()
                    val updaterIdStr = principal?.payload?.getClaim("userId")?.asString()
                        ?: throw Exception("Utilisateur non authentifié.")

                    val updated = transaction {
                        TicketService.updateTicketStatus(
                            ticketId = UUID.fromString(ticketId),
                            newStatus = request.status ?: throw Exception("Statut de transition manquant."),
                            cost = request.interventionCost,
                            comment = request.comment,
                            updaterUserId = UUID.fromString(updaterIdStr)
                        )
                    }
                    call.respond(HttpStatusCode.OK, updated)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur lors de la transition d'état du ticket."))
                }
            }

            // GET /api/residences/{id}/transactions : List operations ledger
            get("/api/residences/{id}/transactions") {
                val residenceId = call.parameters["id"] ?: ""
                val typeParam = call.request.queryParameters["type"]
                val categoryParam = call.request.queryParameters["category"]
                val startDateParam = call.request.queryParameters["start_date"]
                val endDateParam = call.request.queryParameters["end_date"]
                val queryParam = call.request.queryParameters["q"]

                try {
                    val list = transaction {
                        FinanceOperationService.getTransactions(
                            residenceId = UUID.fromString(residenceId),
                            typeParam = typeParam,
                            categoryParam = categoryParam,
                            startDateParam = startDateParam,
                            endDateParam = endDateParam,
                            queryParam = queryParam
                        )
                    }
                    call.respond(HttpStatusCode.OK, list)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Erreur de chargement du grand livre."))
                }
            }

            // POST /api/residences/{id}/transactions : Record manual operational expense
            post("/api/residences/{id}/transactions") {
                val residenceId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<ExpenseRecordRequest>()
                    val created = transaction {
                        FinanceOperationService.recordExpense(
                            residenceId = UUID.fromString(residenceId),
                            category = request.category,
                            amount = request.amount,
                            description = request.description,
                            date = LocalDate.parse(request.transactionDate)
                        )
                    }
                    call.respond(HttpStatusCode.Created, created)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Erreur de saisie de la dépense."))
                }
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

                val filterParam = call.request.queryParameters["filter"] ?: "MONTH"
                val startDate = call.request.queryParameters["start_date"]
                val endDate = call.request.queryParameters["end_date"]

                try {
                    val data = DashboardService.getDashboardData(
                        residenceId = UUID.fromString(residenceId),
                        filterType = filterParam,
                        customStart = startDate,
                        customEnd = endDate
                    )
                    call.respond(HttpStatusCode.OK, data)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Erreur lors du calcul analytique du tableau de bord."))
                }
            }
        }
    }
}
