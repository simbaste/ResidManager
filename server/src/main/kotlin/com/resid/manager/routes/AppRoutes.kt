package com.resid.manager.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.resid.manager.data.*
import com.resid.manager.dto.*
import com.resid.manager.service.ElectricityService
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
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun Application.configureAppRoutes() {
    routing {
        // -----------------------------------------------------------------
        // SECTION 2: SECURED ROUTES (JWT AUTHENTICATED)
        // -----------------------------------------------------------------
        // GET /api/equipements : List predefined equipments (Publicly accessible helper)
        get("/api/equipements") {
            try {
                val list = transaction {
                    Equipment.all().map {
                        EquipmentDto(id = it.id.value.toString(), key = it.key, label = it.label)
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

        authenticate("auth-jwt") {
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
                                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
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
                                ResidenceMemberSummaryDto(
                                    userId = row[Users.id].value.toString(),
                                    firstName = row[Users.firstName],
                                    lastName = row[Users.lastName],
                                    email = row[Users.email],
                                    phone = row[Users.phone],
                                    roleDto = row[ResidenceMembers.role].convert(),
                                    status = row[ResidenceMembers.status].convert()
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
                        !ResidenceMembers
                            .select(ResidenceMembers.userId)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                ((ResidenceMembers.residenceId) eq UUID.fromString(residenceId)) and
                                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }
                            .empty()
                    }

                    if (!isMember) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : vous ne faites pas partie de cette résidence."))
                        return@get
                    }

                    val list = transaction {
                        ResidenceUnit.find { ResidenceUnits.residenceId eq UUID.fromString(residenceId) }.map {
                            ResidenceUnitDto(
                                id = it.id.value.toString(),
                                residenceId = it.residence.id.value.toString(),
                                name = it.name,
                                floor = it.floor,
                                type = it.type,
                                nominalRent = it.nominalRent,
                                serviceCharges = it.serviceCharges,
                                initialElectricityIndex = it.initialElectricityIndex,
                                status = it.status.convert(),
                                equipments = it.equipments.map { eq ->
                                    EquipmentDto(id = eq.id.value.toString(), key = eq.key, label = eq.label)
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
                        ResidenceMembers
                            .select(ResidenceMembers.role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                        (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and
                                        (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }
                            .singleOrNull()?.get(ResidenceMembers.role)
                    }

                    if (userRole?.isManager() != true) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : Seuls les administrateurs et gestionnaires de cette résidence peuvent ajouter des logements."))
                        return@post
                    }

                    val request = call.receive<ResidenceUnitCreateRequest>()

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
                        
                        val newResidenceUnit = ResidenceUnit.new {
                            residence = activeRes
                            name = request.name
                            floor = request.floor
                            type = request.type
                            nominalRent = request.nominalRent
                            serviceCharges = request.serviceCharges
                            initialElectricityIndex = request.initialElectricityIndex
                            createdAt = LocalDateTime.now(Clock.systemUTC())
                            updatedAt = LocalDateTime.now(Clock.systemUTC())
                            status = UnitStatus.AVAILABLE // Forced initial business state
                        }

                        // Attach selected equipments
                        if (request.equipementIds.isNotEmpty()) {
                            val selectedEq = request.equipementIds.mapNotNull { eqId ->
                                Equipment.findById(UUID.fromString(eqId))
                            }
                            newResidenceUnit.equipments = SizedCollection(selectedEq)
                        }

                        newResidenceUnit.flush()

                        ResidenceUnitDto(
                            id = newResidenceUnit.id.value.toString(),
                            residenceId = newResidenceUnit.residence.id.value.toString(),
                            name = newResidenceUnit.name,
                            floor = newResidenceUnit.floor,
                            type = newResidenceUnit.type,
                            nominalRent = newResidenceUnit.nominalRent,
                            serviceCharges = newResidenceUnit.serviceCharges,
                            initialElectricityIndex = newResidenceUnit.initialElectricityIndex,
                            status = newResidenceUnit.status.convert(),
                            equipments = newResidenceUnit.equipments.map { eq ->
                                EquipmentDto(id = eq.id.value.toString(), key = eq.key, label = eq.label)
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

            // DELETE /api/residences/{id}/logements/{residenceUnitId} : Delete a housing unit (logement)
            delete("/api/residences/{id}/logements/{residenceUnitId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""
                val residenceUnitId = call.parameters["residenceUnitId"] ?: ""

                try {
                    // Check user roles: OWNER, ADMIN, RESIDENCE_MANAGER
                    val userRole = transaction {
                        ResidenceMembers
                            .select(ResidenceMembers.role)
                            .where { 
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and 
                                (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and 
                                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }
                            .singleOrNull()?.get(ResidenceMembers.role)
                    }

                    if (userRole?.isManager() != true) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : Seuls les administrateurs et gestionnaires peuvent supprimer des logements."))
                        return@delete
                    }

                    // Perform deletion inside transaction using Exposed DSL
                    transaction {
                        ResidenceUnits.deleteWhere { ResidenceUnits.id eq UUID.fromString(residenceUnitId) }
                    }

                    call.respond(HttpStatusCode.OK, mapOf("message" to "Le logement a été supprimé de la base de données avec succès !"))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la suppression du logement : ${e.message}")
                    )
                }
            }

            // PUT /api/residences/{id}/logements/{residenceUnitId} : Update a housing unit (logement)
            put("/api/residences/{id}/logements/{residenceUnitId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""
                val residenceUnitId = call.parameters["residenceUnitId"] ?: ""

                try {
                    // Check user roles: OWNER, ADMIN, RESIDENCE_MANAGER
                    val userRole = transaction {
                        ResidenceMembers
                            .select(ResidenceMembers.role)
                            .where { 
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and 
                                (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and 
                                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }
                            .singleOrNull()?.get(ResidenceMembers.role)
                    }

                    if (userRole?.isManager() != true) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : Seuls les administrateurs et gestionnaires peuvent modifier des logements."))
                        return@put
                    }

                    val request = call.receive<ResidenceUnitCreateRequest>()

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
                        val dbResidenceUnit = ResidenceUnit.findById(UUID.fromString(residenceUnitId)) ?: throw Exception("Logement introuvable.")
                        
                        dbResidenceUnit.name = request.name
                        dbResidenceUnit.floor = request.floor
                        dbResidenceUnit.type = request.type
                        dbResidenceUnit.nominalRent = request.nominalRent
                        dbResidenceUnit.serviceCharges = request.serviceCharges
                        dbResidenceUnit.initialElectricityIndex = request.initialElectricityIndex
                        dbResidenceUnit.updatedAt = LocalDateTime.now(Clock.systemUTC())

                        // Update selected equipments
                        val selectedEq = request.equipementIds.mapNotNull { eqId ->
                            Equipment.findById(UUID.fromString(eqId))
                        }
                        dbResidenceUnit.equipments = SizedCollection(selectedEq)

                        dbResidenceUnit.flush()

                        ResidenceUnitDto(
                            id = dbResidenceUnit.id.value.toString(),
                            residenceId = dbResidenceUnit.residence.id.value.toString(),
                            name = dbResidenceUnit.name,
                            floor = dbResidenceUnit.floor,
                            type = dbResidenceUnit.type,
                            nominalRent = dbResidenceUnit.nominalRent,
                            serviceCharges = dbResidenceUnit.serviceCharges,
                            initialElectricityIndex = dbResidenceUnit.initialElectricityIndex,
                            status = dbResidenceUnit.status.convert(),
                            equipments = dbResidenceUnit.equipments.map { eq ->
                                EquipmentDto(id = eq.id.value.toString(), key = eq.key, label = eq.label)
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
                val residenceUnitId = call.parameters["id"] ?: ""

                try {
                    val request = call.receive<LeaseCreateRequest>()
                    val parsedStart = LocalDate.parse(request.startDate)
                    val parsedEnd = LocalDate.parse(request.endDate)
                    val duration = java.time.temporal.ChronoUnit.MONTHS.between(parsedStart, parsedEnd).toInt()

                    val createdLeaseDto = transaction {
                        // 1. Fetch logement & verify status == "AVAILABLE"
                        val dbResidenceUnit = ResidenceUnit.findById(UUID.fromString(residenceUnitId)) 
                            ?: throw Exception("Logement introuvable.")
                        
                        if (dbResidenceUnit.status != UnitStatus.AVAILABLE) {
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
                                    createdAt = LocalDateTime.now(Clock.systemUTC())
                                    updatedAt = LocalDateTime.now(Clock.systemUTC())
                                }
                                newUser.flush()

                                // Instantly register as a member of this residence
                                ResidenceMembers.insert {
                                    it[userId] = newUser.id.value
                                    it[residenceId] = dbResidenceUnit.residence.id.value
                                    it[role] = Role.TENANT
                                    it[status] = InvitationStatus.ACCEPTED
                                    it[createdAt] = LocalDateTime.now(Clock.systemUTC())
                                }
                                newUser
                            }
                        } else {
                            throw Exception("Veuillez sélectionner un locataire existant ou remplir le formulaire d'inscription inline.")
                        }

                        // 3. Check if tenant already has an ongoing lease (not TERMINATED)
                        val hasOngoingLease = Leases
                            .select(Leases.id)
                            .where { 
                                (Leases.tenantId eq dbTenant.id.value) and 
                                (Leases.status neq LeaseStatus.TERMINATED)
                            }
                            .count() > 0

                        if (hasOngoingLease) {
                            throw Exception("Ce locataire possède déjà un contrat de bail en cours dans l'application.")
                        }

                        // 4. Calculate Total Requirement and Initial Status based on Advanced Payments
                        val paymentFrequencyDto = PaymentFrequency.valueOf(request.paymentFrequency)
                        val isMonthly = paymentFrequencyDto == PaymentFrequency.MONTHLY
                        val rentAndCharges = dbResidenceUnit.nominalRent + dbResidenceUnit.serviceCharges
                        val advanceMonths = if (isMonthly) 1 else (request.advanceMonths ?: 12)
                        
                        val requiredFirstRent = advanceMonths * rentAndCharges
                        val totalRequiredToPay = request.depositAmount + requiredFirstRent
                        val initialPayment = request.advancePaymentAmount ?: 0.0

                        val initialStatus = if (initialPayment >= totalRequiredToPay) {
                            LeaseStatus.PENDING_SIGNATURE
                        } else if (initialPayment > 0.0) {
                            LeaseStatus.DOWN_PAYMENT_PAID
                        } else {
                            LeaseStatus.PENDING_PAYMENT
                        }

                        // Create Lease record
                        val newLease = Lease.new {
                            this.residenceUnit = dbResidenceUnit
                            this.tenant = dbTenant
                            this.durationMonths = if (duration <= 0) 12 else duration
                            this.paymentFrequency = paymentFrequencyDto
                            this.depositAmount = request.depositAmount
                            this.depositStatus = if (initialPayment >= request.depositAmount) DepositStatus.PAID else DepositStatus.PENDING
                            this.status = initialStatus
                            this.startDate = parsedStart
                            this.endDate = parsedEnd
                            this.advanceMonths = advanceMonths
                            this.advancePaymentAmount = initialPayment
                            this.createdAt = LocalDateTime.now(Clock.systemUTC())
                            this.updatedAt = LocalDateTime.now(Clock.systemUTC())
                        }

                        // 5. Update logement status to OCCUPIED
                        dbResidenceUnit.status = UnitStatus.OCCUPIED
                        dbResidenceUnit.flush()
                        newLease.flush()

                        // 6. Generate financial transaction entries if an advance payment was actually made!
                        if (initialPayment > 0.0) {
                            val paidCaution = minOf(initialPayment, request.depositAmount)
                            val paidRent = maxOf(0.0, initialPayment - request.depositAmount)

                            if (paidCaution > 0.0) {
                                FinancialTransaction.new {
                                    this.residence = dbResidenceUnit.residence
                                    this.type = TransactionType.INCOME
                                    this.category = TransactionCategory.DEPOSIT
                                    this.amount = paidCaution
                                    this.description = "Acompte caution à la signature pour le logement ${dbResidenceUnit.name}"
                                    this.relatedEntityType = EntityType.BAIL
                                    this.relatedEntityId = newLease.id.value
                                    this.transactionDate = LocalDate.now()
                                    this.createdAt = LocalDateTime.now(Clock.systemUTC())
                                    this.updatedAt = LocalDateTime.now(Clock.systemUTC())
                                }
                            }

                            if (paidRent > 0.0) {
                                FinancialTransaction.new {
                                    this.residence = dbResidenceUnit.residence
                                    this.type = TransactionType.INCOME
                                    this.category = TransactionCategory.RENT
                                    this.amount = paidRent
                                    this.description = "Acompte loyer d'avance à la signature pour le logement ${dbResidenceUnit.name}"
                                    this.relatedEntityType = EntityType.BAIL
                                    this.relatedEntityId = newLease.id.value
                                    this.transactionDate = LocalDate.now()
                                    this.createdAt = LocalDateTime.now(Clock.systemUTC())
                                    this.updatedAt = LocalDateTime.now(Clock.systemUTC())
                                }
                            }
                        }

                        LeaseDto(
                            id = newLease.id.value.toString(),
                            residenceUnitId = newLease.residenceUnit.id.value.toString(),
                            tenantId = newLease.tenant.id.value.toString(),
                            startDate = newLease.startDate.toString(),
                            endDate = newLease.endDate.toString(),
                            depositAmount = newLease.depositAmount,
                            monthlyRentAtSign = request.monthlyRentAtSign,
                            status = initialStatus.convert(),
                            createdAt = newLease.createdAt.toString(),
                            updatedAt = newLease.updatedAt.toString(),
                            paymentFrequencyDto = newLease.paymentFrequency.convert(),
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
                                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }
                            .empty()
                    }

                    if (!isMember) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : vous ne faites pas partie de cette résidence."))
                        return@get
                    }

                    val leasesList = transaction {
                        // Find baux for all logements in this residence
                        Lease.all().filter { it.residenceUnit.residence.id.value == UUID.fromString(residenceId) }.map { lease ->
                            val previousPayments = FinancialTransaction.find { 
                                (FinancialTransactions.relatedEntityType eq EntityType.BAIL) and
                                (FinancialTransactions.relatedEntityId eq lease.id.value) 
                            }.map { tx ->
                                LeasePaymentDto(
                                    id = tx.id.value.toString(),
                                    category = if (tx.category == TransactionCategory.DEPOSIT) LeaseCategory.DEPOSIT else LeaseCategory.RENT,
                                    amount = tx.amount,
                                    description = tx.description,
                                    transactionDate = tx.transactionDate.toString()
                                )
                            }
                            LeaseDto(
                                id = lease.id.value.toString(),
                                residenceUnitId = lease.residenceUnit.id.value.toString(),
                                tenantId = lease.tenant.id.value.toString(),
                                startDate = lease.startDate.toString(),
                                endDate = lease.endDate.toString(),
                                depositAmount = lease.depositAmount,
                                monthlyRentAtSign = lease.residenceUnit.nominalRent,
                                status = lease.status.convert(),
                                createdAt = lease.createdAt.toString(),
                                updatedAt = lease.updatedAt.toString(),
                                paymentFrequencyDto = lease.paymentFrequency.convert(),
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

                    if (request.category !in listOf(TransactionCategoryDto.DEPOSIT,
                            TransactionCategoryDto.RENT)) throw Exception("Catégorie de paiement invalide pour cette opération.")

                    val updatedLeaseDto = transaction {
                        // 1. Fetch lease record
                        val dbLease = Lease.findById(UUID.fromString(leaseId)) 
                            ?: throw Exception("Contrat de bail introuvable.")

                        // 2. Fetch all previous caution (Deposit) payments registered
                        val previousPaidCaution = FinancialTransaction.find { 
                            (FinancialTransactions.relatedEntityType eq EntityType.BAIL) and
                            (FinancialTransactions.relatedEntityId eq UUID.fromString(leaseId)) and 
                            (FinancialTransactions.type eq TransactionType.INCOME) and
                            (FinancialTransactions.category eq TransactionCategory.DEPOSIT)
                        }.sumOf { it.amount }

                        // 3. Fetch all previous rent payments registered
                        val previousPaidRent = FinancialTransaction.find { 
                            (FinancialTransactions.relatedEntityType eq EntityType.BAIL) and
                            (FinancialTransactions.relatedEntityId eq UUID.fromString(leaseId)) and 
                            (FinancialTransactions.type eq TransactionType.INCOME) and
                            ((FinancialTransactions.category eq TransactionCategory.RENT) or (FinancialTransactions.category eq TransactionCategory.LEASE_PAYMENT))
                        }.sumOf { it.amount }

                        // 4. Incorporate the new incoming payment
                        val totalPaidCaution = previousPaidCaution + if (request.category == TransactionCategoryDto.DEPOSIT) request.amountPaid else 0.0
                        val totalPaidRent = previousPaidRent + if (request.category == TransactionCategoryDto.RENT) request.amountPaid else 0.0

                        // 5. Calculate required amounts
                        val requiredCaution = dbLease.depositAmount
                        val requiredRent = dbLease.advanceMonths * (dbLease.residenceUnit.nominalRent + dbLease.residenceUnit.serviceCharges)

                        // 6. Determine status update based on dual ledger
                        val newStatus = if (totalPaidCaution >= requiredCaution && totalPaidRent >= requiredRent) {
                            LeaseStatus.PENDING_SIGNATURE
                        } else if (totalPaidCaution > 0.0 || totalPaidRent > 0.0) {
                            LeaseStatus.DOWN_PAYMENT_PAID
                        } else {
                            LeaseStatus.PENDING_PAYMENT
                        }

                        // Map status back to the database record
                        dbLease.status = newStatus
                        if (totalPaidCaution >= requiredCaution) {
                            dbLease.depositStatus = DepositStatus.PAID
                        }
                        dbLease.updatedAt = LocalDateTime.now(Clock.systemUTC())
                        dbLease.flush()

                        // 7. Generate financial transaction entry
                        FinancialTransaction.new {
                            this.residence = dbLease.residenceUnit.residence
                            this.type = TransactionType.INCOME
                            this.category = request.category.convert()
                            this.amount = request.amountPaid
                            this.description = if (request.category == TransactionCategoryDto.DEPOSIT) {
                                "Versement partiel caution pour le logement ${dbLease.residenceUnit.name}"
                            } else {
                                "Versement partiel loyer pour le logement ${dbLease.residenceUnit.name}"
                            }
                            this.relatedEntityType = EntityType.BAIL
                            this.relatedEntityId = UUID.fromString(leaseId)
                            this.transactionDate = LocalDate.now()
                            this.createdAt = LocalDateTime.now(Clock.systemUTC())
                            this.updatedAt = LocalDateTime.now(Clock.systemUTC())
                        }

                        val previousPayments = FinancialTransaction.find { 
                            (FinancialTransactions.relatedEntityType eq EntityType.BAIL) and
                            (FinancialTransactions.relatedEntityId eq dbLease.id.value) 
                        }.map { tx ->
                            LeasePaymentDto(
                                id = tx.id.value.toString(),
                                category = if (tx.category == TransactionCategory.DEPOSIT) LeaseCategory.DEPOSIT else LeaseCategory.RENT,
                                amount = tx.amount,
                                description = tx.description,
                                transactionDate = tx.transactionDate.toString()
                            )
                        }

                        LeaseDto(
                            id = dbLease.id.value.toString(),
                            residenceUnitId = dbLease.residenceUnit.id.value.toString(),
                            tenantId = dbLease.tenant.id.value.toString(),
                            startDate = dbLease.startDate.toString(),
                            endDate = dbLease.endDate.toString(),
                            depositAmount = dbLease.depositAmount,
                            monthlyRentAtSign = dbLease.residenceUnit.nominalRent,
                            status = newStatus.convert(),
                            createdAt = dbLease.createdAt.toString(),
                            updatedAt = dbLease.updatedAt.toString(),
                            paymentFrequencyDto = dbLease.paymentFrequency.convert(),
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
                            dbLease.status = it.convert()
                            if (it == LeaseStatusDto.TERMINATED) {
                                dbLease.residenceUnit.status = UnitStatus.AVAILABLE
                                dbLease.residenceUnit.flush()
                            }
                        }
                        dbLease.updatedAt = LocalDateTime.now(Clock.systemUTC())
                        dbLease.flush()

                        val previousPayments = FinancialTransaction.find { 
                            (FinancialTransactions.relatedEntityType eq EntityType.BAIL) and
                            (FinancialTransactions.relatedEntityId eq dbLease.id.value) 
                        }.map { tx ->
                            LeasePaymentDto(
                                id = tx.id.value.toString(),
                                category = if (tx.category == TransactionCategory.DEPOSIT) LeaseCategory.DEPOSIT else LeaseCategory.RENT,
                                amount = tx.amount,
                                description = tx.description,
                                transactionDate = tx.transactionDate.toString()
                            )
                        }

                        LeaseDto(
                            id = dbLease.id.value.toString(),
                            residenceUnitId = dbLease.residenceUnit.id.value.toString(),
                            tenantId = dbLease.tenant.id.value.toString(),
                            startDate = dbLease.startDate.toString(),
                            endDate = dbLease.endDate.toString(),
                            depositAmount = dbLease.depositAmount,
                            monthlyRentAtSign = dbLease.residenceUnit.nominalRent,
                            status = dbLease.status.convert(),
                            createdAt = dbLease.createdAt.toString(),
                            updatedAt = dbLease.updatedAt.toString(),
                            paymentFrequencyDto = dbLease.paymentFrequency.convert(),
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

                        stmt.status = request.status?.convert() ?: ElectricityStatus.PAID
                        stmt.updatedAt = LocalDateTime.now(Clock.systemUTC())
                        stmt.flush()

                        // Update description of original Financial Transaction to reflect payment
                        val tx = FinancialTransaction.find { 
                            (FinancialTransactions.relatedEntityType eq EntityType.ELECTRICITY_STATEMENT) and
                            (FinancialTransactions.relatedEntityId eq stmt.id.value) 
                        }.firstOrNull()

                        if (tx != null && !tx.description.startsWith("[PAID]")) {
                            tx.description = "[PAID] " + tx.description
                            tx.updatedAt = LocalDateTime.now(Clock.systemUTC())
                            tx.flush()
                        }

                        ElectricityStatementDto(
                            id = stmt.id.value.toString(),
                            residenceUnitId = stmt.residenceUnit.id.value.toString(),
                            previousIndex = stmt.oldIndex,
                            newIndex = stmt.newIndex,
                            kWhPriceApplied = stmt.kWhPriceApplied,
                            amountDue = stmt.amountDue,
                            statementDate = stmt.statementDate.toString(),
                            status = stmt.status.convert(),
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
                val residenceUnitId = call.parameters["id"] ?: ""
                try {
                    val previous = transaction {
                        ElectricityService.getPreviousIndex(UUID.fromString(residenceUnitId))
                    }
                    call.respond(HttpStatusCode.OK, mapOf("previousIndex" to previous))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Erreur de chargement de l'index précédent."))
                }
            }

            // POST /api/logements/{id}/electricity : Enter new meter index and generate statement
            post("/api/logements/{id}/electricity") {
                val residenceUnitId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<ElectricityStatementCreateRequest>()
                    val created = transaction {
                        ElectricityService.submitStatement(
                            residenceUnitId = UUID.fromString(residenceUnitId),
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
                val residenceUnitParam = call.request.queryParameters["residenceUnitId"]
                val floorParam = call.request.queryParameters["floor"]
                val tenantParam = call.request.queryParameters["tenantName"]

                try {
                    val statements = transaction {
                        val list = ElectricityStatement.all().filter { 
                            it.residenceUnit.residence.id.value == UUID.fromString(residenceId) 
                        }
                        
                        var filtered = list

                        statusParam?.ifBlank { null }?.let { status ->
                            val statusEnum = ElectricityStatus.valueOf(status)
                            filtered = filtered.filter { it.status == statusEnum }
                        }

                        residenceUnitParam?.ifBlank { null }?.let { logId ->
                            filtered = filtered.filter { it.residenceUnit.id.value == UUID.fromString(logId) }
                        }

                        floorParam?.ifBlank { null }?.let { floor ->
                            filtered = filtered.filter { it.residenceUnit.floor.contains(floor, ignoreCase = true) }
                        }

                        tenantParam?.ifBlank { null }?.let { tenantName ->
                            filtered = filtered.filter { stmt ->
                                val activeLease = Lease.find { 
                                    (Leases.residenceUnitId eq stmt.residenceUnit.id) and (Leases.status eq LeaseStatus.SIGNED_ACTIVE)
                                }.firstOrNull()
                                val tenant = activeLease?.tenant
                                val fullName = "${tenant?.firstName} ${tenant?.lastName}"
                                fullName.contains(tenantName, ignoreCase = true)
                            }
                        }

                        filtered.sortedByDescending { it.statementDate }.map {
                            ElectricityStatementDto(
                                id = it.id.value.toString(),
                                residenceUnitId = it.residenceUnit.id.value.toString(),
                                previousIndex = it.oldIndex,
                                newIndex = it.newIndex,
                                kWhPriceApplied = it.kWhPriceApplied,
                                amountDue = it.amountDue,
                                statementDate = it.statementDate.toString(),
                                status = it.status.convert(),
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
                            this.createdAt = LocalDateTime.now(Clock.systemUTC())
                            this.updatedAt = LocalDateTime.now(Clock.systemUTC())
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
                        entity.updatedAt = LocalDateTime.now(Clock.systemUTC())
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
                        Ticket.all().filter { it.residenceUnit.residence.id.value == UUID.fromString(residenceId) }.map {
                            TicketDto(
                                id = it.id.value.toString(),
                                residenceUnitId = it.residenceUnit.id.value.toString(),
                                creatorId = it.creator.id.value.toString(),
                                category = TicketCategoryDto(
                                    id = it.category.id.value.toString(),
                                    key = it.category.key,
                                    label = it.category.label,
                                    residenceId = it.category.residence?.id?.value?.toString()
                                ),
                                title = it.title,
                                description = it.description,
                                urgency = it.urgency.convert(),
                                status = it.status.convert(),
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
                val residenceUnitId = call.parameters["id"] ?: ""
                try {
                    val request = call.receive<TicketCreateRequest>()
                    val principal = call.principal<JWTPrincipal>()
                    val creatorIdStr = principal?.payload?.getClaim("userId")?.asString() 
                        ?: throw Exception("Utilisateur non authentifié.")

                    val created = transaction {
                        TicketService.createTicket(
                            residenceUnitId = UUID.fromString(residenceUnitId),
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
                            categoryName = request.category,
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
                    ResidenceMembers
                        .select(ResidenceMembers.role)
                        .where {
                            (ResidenceMembers.userId eq UUID.fromString(userId)) and
                            (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and
                            (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                        }
                        .singleOrNull()?.get(ResidenceMembers.role)
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
