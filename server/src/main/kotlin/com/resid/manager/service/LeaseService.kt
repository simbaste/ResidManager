package com.resid.manager.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.resid.manager.data.DepositStatus
import com.resid.manager.data.EntityType
import com.resid.manager.data.FinancialTransaction
import com.resid.manager.data.FinancialTransactions
import com.resid.manager.data.Guarantor
import com.resid.manager.data.Guarantors
import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.Lease
import com.resid.manager.data.LeaseStatus
import com.resid.manager.data.Leases
import com.resid.manager.data.PaymentFrequency
import com.resid.manager.data.Residence
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.ResidenceUnit
import com.resid.manager.data.ResidenceUnits
import com.resid.manager.data.Role
import com.resid.manager.data.TransactionCategory
import com.resid.manager.data.TransactionType
import com.resid.manager.data.UnitStatus
import com.resid.manager.data.User
import com.resid.manager.data.Users
import com.resid.manager.data.convert
import com.resid.manager.data.isManager
import com.resid.manager.dto.GuarantorDto
import com.resid.manager.dto.LeaseCategory
import com.resid.manager.dto.LeaseCreateRequest
import com.resid.manager.dto.LeaseDraftInitRequest
import com.resid.manager.dto.LeaseDraftStepUpdateRequest
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeasePaymentDto
import com.resid.manager.dto.LeasePaymentRequest
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.dto.LeaseUpdateRequest
import com.resid.manager.dto.TransactionCategoryDto
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

object LeaseService {

    suspend fun isResidenceManager(userId: UUID, residenceId: UUID): Boolean = dbQuery {
        ResidenceMembers.select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.any { it[ResidenceMembers.role].isManager() }
    }

    suspend fun createDraftLease(requesterId: UUID, request: LeaseDraftInitRequest): LeaseDto = dbQuery {
        val unitId = UUID.fromString(request.residenceUnitId)
        val dbResidenceUnit = ResidenceUnit.findById(unitId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Logement introuvable.")

        val residenceId = dbResidenceUnit.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent créer des contrats de bail."
            )
        }

        val now = LocalDateTime.now()
        val newDraft = Lease.new {
            this.residenceUnit = dbResidenceUnit
            this.status = LeaseStatus.DRAFT
            this.createdAt = now
            this.updatedAt = now
        }
        
        newDraft.flush()
        newDraft.toDto()
    }

    suspend fun updateDraftLeaseStep(
        requesterId: UUID,
        leaseId: UUID,
        request: LeaseDraftStepUpdateRequest
    ): LeaseDto = dbQuery {
        val dbLease = Lease.findById(leaseId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Contrat de bail introuvable.")

        val residenceId = dbLease.residenceUnit.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent modifier un contrat de bail."
            )
        }

        // Allow draft step updates on all pre-signature statuses
        val editableStatuses = listOf(
            LeaseStatus.DRAFT,
            LeaseStatus.PENDING_PAYMENT,
            LeaseStatus.PARTIALLY_PAID,
            LeaseStatus.DOWN_PAYMENT_PAID,
            LeaseStatus.PENDING_SIGNATURE
        )
        if (dbLease.status !in editableStatuses) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Impossible de modifier un contrat déjà signé ou clôturé (${dbLease.status.name})."
            )
        }

        val now = LocalDateTime.now()
        
        when (request.step) {
            "TENANT" -> {
                val dbTenant = if (request.tenantId != null) {
                    User.findById(UUID.fromString(request.tenantId))
                        ?: throw HttpError(HttpStatusCode.NotFound, "Locataire introuvable.")
                } else if (request.inlineTenant != null) {
                    val t = request.inlineTenant
                    val existingUser = User.find { Users.email eq t.email.trim().lowercase() }.firstOrNull()
                    if (existingUser != null) {
                        existingUser
                    } else {
                        val hashedPwd = BCrypt.withDefaults().hashToString(12, "Test1234_".toCharArray())
                        User.new {
                            this.firstName = t.firstName.trim()
                            this.lastName = t.lastName.trim()
                            this.email = t.email.trim().lowercase()
                            this.phone = t.phone?.trim()
                            this.passwordHash = hashedPwd
                            this.createdAt = now
                            this.updatedAt = now
                        }
                    }
                } else {
                    throw HttpError(HttpStatusCode.BadRequest, "Veuillez spécifier un locataire.")
                }
                
                // Assure residence membership for tenant
                val existingMember = ResidenceMembers.select(ResidenceMembers.role)
                    .where { (ResidenceMembers.residenceId eq residenceId) and (ResidenceMembers.userId eq dbTenant.id) }
                    .singleOrNull()
                
                if (existingMember == null) {
                    ResidenceMembers.insert {
                        it[this.userId] = dbTenant.id
                        it[this.residenceId] = residenceId
                        it[role] = Role.TENANT
                        it[status] = InvitationStatus.ACCEPTED
                    }
                }
                dbLease.tenant = dbTenant
            }
            "GUARANTOR" -> {
                if (request.guarantorId != null) {
                    val dbGuarantor = Guarantor.findById(UUID.fromString(request.guarantorId))
                        ?: throw HttpError(HttpStatusCode.NotFound, "Garant introuvable.")
                    dbLease.guarantor = dbGuarantor
                } else if (request.guarantor != null) {
                    val g = request.guarantor
                    val dbGuarantor = Guarantor.new {
                        this.tenant = dbLease.tenant ?: throw HttpError(HttpStatusCode.BadRequest, "Locataire non défini sur le brouillon")
                        this.firstName = g.firstName
                        this.lastName = g.lastName
                        this.phone = g.phone
                        this.monthlySalary = g.monthlySalary
                        this.relationship = g.relationship
                        this.jobTitle = g.jobTitle
                        this.employer = g.employer
                        this.createdAt = now
                        this.updatedAt = now
                    }
                    dbLease.guarantor = dbGuarantor
                }
            }
            "UNIT" -> {
                if (request.residenceUnitId != null) {
                    val newUnitId = UUID.fromString(request.residenceUnitId)
                    if (dbLease.residenceUnit.id.value != newUnitId) {
                        val newUnit = ResidenceUnit.findById(newUnitId)
                            ?: throw HttpError(HttpStatusCode.NotFound, "Nouveau logement introuvable.")
                        if (newUnit.residence.id.value != residenceId) {
                            throw HttpError(HttpStatusCode.BadRequest, "Le logement doit appartenir à la même résidence.")
                        }
                        // Check if the new unit is already occupied by an active lease
                        val activeLeaseExists = Leases.select(Leases.id).where {
                            (Leases.residenceUnitId eq newUnitId) and
                            (Leases.id neq leaseId) and
                            (Leases.status eq LeaseStatus.SIGNED_ACTIVE)
                        }.count() > 0

                        if (activeLeaseExists) {
                            throw HttpError(HttpStatusCode.Conflict, "Ce logement est déjà occupé par un bail actif.")
                        }

                        dbLease.residenceUnit = newUnit
                    }
                }
            }
            "FINANCIALS" -> {
                if (request.depositAmount != null) dbLease.depositAmount = request.depositAmount
                if (request.paymentFrequency != null) {
                    dbLease.paymentFrequency = try {
                        PaymentFrequency.valueOf(request.paymentFrequency)
                    } catch (e: Exception) {
                        PaymentFrequency.MONTHLY
                    }
                }
                if (request.advanceMonths != null) dbLease.advanceMonths = request.advanceMonths
                if (request.advancePaymentAmount != null) dbLease.advancePaymentAmount = request.advancePaymentAmount
            }
            "TIMELINE" -> {
                if (request.startDate != null) dbLease.startDate = LocalDate.parse(request.startDate)
                if (request.endDate != null) dbLease.endDate = LocalDate.parse(request.endDate)
            }
            "INVENTORY" -> {
                // Custom inventory setup if needed on draft step
            }
        }

        // Recalculate pre-signature status:
        // Keep DRAFT until all required steps (tenant, dates, deposit, frequency) are fully completed
        val isComplete = dbLease.tenant != null &&
                dbLease.startDate != null &&
                dbLease.endDate != null &&
                dbLease.depositAmount != null &&
                dbLease.paymentFrequency != null

        if (!isComplete) {
            dbLease.status = LeaseStatus.DRAFT
        } else if (dbLease.status != LeaseStatus.DRAFT) {
            // If already transitioned beyond DRAFT and edited, recalculate financial target status
            val rentAndCharges = dbLease.residenceUnit.nominalRent + dbLease.residenceUnit.serviceCharges
            val advanceMonths = dbLease.advanceMonths ?: 1
            val totalExpected = (dbLease.depositAmount ?: 0.0) + (advanceMonths * rentAndCharges)
            val advancePayment = dbLease.advancePaymentAmount ?: 0.0

            dbLease.status = when {
                advancePayment >= totalExpected -> LeaseStatus.PENDING_SIGNATURE
                advancePayment > 0.0 -> LeaseStatus.PARTIALLY_PAID
                else -> LeaseStatus.PENDING_PAYMENT
            }
            dbLease.depositStatus = if (advancePayment >= (dbLease.depositAmount ?: 0.0)) DepositStatus.PAID else DepositStatus.PENDING
        }
        
        dbLease.updatedAt = now
        dbLease.flush()
        dbLease.toDto()
    }

    suspend fun activateDraftLease(requesterId: UUID, leaseId: UUID): LeaseDto = dbQuery {
        val dbLease = Lease.findById(leaseId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Contrat de bail introuvable.")

        val residenceId = dbLease.residenceUnit.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent modifier un contrat de bail."
            )
        }

        val editableStatuses = listOf(
            LeaseStatus.DRAFT,
            LeaseStatus.PENDING_PAYMENT,
            LeaseStatus.PARTIALLY_PAID,
            LeaseStatus.DOWN_PAYMENT_PAID,
            LeaseStatus.PENDING_SIGNATURE
        )
        if (dbLease.status !in editableStatuses) {
            throw HttpError(HttpStatusCode.BadRequest, "Le contrat ne peut plus être activé (${dbLease.status.name}).")
        }
        
        // Ensure all required fields are set before activating
        if (dbLease.tenant == null || dbLease.startDate == null || dbLease.endDate == null || dbLease.depositAmount == null || dbLease.paymentFrequency == null) {
            throw HttpError(HttpStatusCode.BadRequest, "Veuillez remplir toutes les informations obligatoires (Locataire, Dates, Montants, Fréquence).")
        }

        val now = LocalDateTime.now()
        
        dbLease.depositStatus = if ((dbLease.advancePaymentAmount ?: 0.0) >= (dbLease.depositAmount ?: 0.0)) DepositStatus.PAID else DepositStatus.PENDING
        
        val totalExpectedInitialPayment = (dbLease.depositAmount ?: 0.0) + (dbLease.residenceUnit.nominalRent * (dbLease.advanceMonths ?: 1))
        
        dbLease.status = when {
            (dbLease.advancePaymentAmount ?: 0.0) >= totalExpectedInitialPayment -> LeaseStatus.PENDING_SIGNATURE
            (dbLease.advancePaymentAmount ?: 0.0) > 0.0 -> LeaseStatus.PARTIALLY_PAID
            else -> LeaseStatus.PENDING_PAYMENT
        }

        dbLease.updatedAt = now
        dbLease.flush()
        
        // Update unit status
        dbLease.residenceUnit.status = UnitStatus.OCCUPIED
        dbLease.residenceUnit.flush()

        // Optionally, record initial payment transaction if > 0
        if ((dbLease.advancePaymentAmount ?: 0.0) > 0.0) {
            FinancialTransaction.new {
                this.residence = dbLease.residenceUnit.residence
                this.type = TransactionType.INCOME
                this.category = TransactionCategory.LEASE_PAYMENT
                this.amount = dbLease.advancePaymentAmount ?: 0.0
                this.description = "Paiement initial (caution + loyer d'avance) pour le bail ${dbLease.id.value}"
                this.relatedEntityType = EntityType.BAIL
                this.relatedEntityId = leaseId
                this.transactionDate = now.toLocalDate()
            }
        }
        
        dbLease.toDto()
    }

    suspend fun createLease(
        requesterId: UUID,
        unitId: UUID,
        request: LeaseCreateRequest
    ): LeaseDto = dbQuery {
        val dbResidenceUnit = ResidenceUnit.findById(unitId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Logement introuvable.")

        val residenceId = dbResidenceUnit.residence.id.value

        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent créer des contrats de bail."
            )
        }

        if (dbResidenceUnit.status != UnitStatus.AVAILABLE) {
            throw HttpError(HttpStatusCode.Conflict, "Ce logement n'est plus disponible pour une location.")
        }

        val parsedStart = try {
            LocalDate.parse(request.startDate)
        } catch (e: Exception) {
            throw HttpError(HttpStatusCode.BadRequest, "Format de date de début invalide : ${e.message}")
        }

        val parsedEnd = try {
            LocalDate.parse(request.endDate)
        } catch (e: Exception) {
            throw HttpError(HttpStatusCode.BadRequest, "Format de date de fin invalide : ${e.message}")
        }

        val duration = ChronoUnit.MONTHS.between(parsedStart, parsedEnd).toInt()

        // 1. Fetch or create tenant user
        val dbTenant = if (!request.tenantId.isNullOrBlank()) {
            val tenantUuid = try {
                UUID.fromString(request.tenantId)
            } catch (e: Exception) {
                throw HttpError(HttpStatusCode.BadRequest, "ID locataire invalide : ${e.message}")
            }
            User.findById(tenantUuid) ?: throw HttpError(HttpStatusCode.NotFound, "Locataire (utilisateur) introuvable.")
        } else if (request.inlineTenant != null) {
            val inline = request.inlineTenant!!
            val existingUser = User.find { Users.email eq inline.email }.firstOrNull()
            if (existingUser != null) {
                existingUser
            } else {
                val tempPasswordHash = BCrypt.withDefaults().hashToString(12, "Locataire123!".toCharArray())
                val now = LocalDateTime.now(Clock.systemUTC())
                val newUser = User.new {
                    email = inline.email
                    passwordHash = tempPasswordHash
                    firstName = inline.firstName
                    lastName = inline.lastName
                    birthDate = null
                    phone = inline.phone
                    createdAt = now
                    updatedAt = now
                }
                newUser.flush()

                // Register as tenant member of this residence
                ResidenceMembers.insert {
                    it[userId] = newUser.id.value
                    it[this.residenceId] = residenceId
                    it[role] = Role.TENANT
                    it[status] = InvitationStatus.ACCEPTED
                    it[createdAt] = now
                    it[updatedAt] = now
                }
                newUser
            }
        } else {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Veuillez sélectionner un locataire existant ou remplir le formulaire d'inscription inline."
            )
        }

        // 2. Check if tenant already has an ongoing lease (not TERMINATED)
        val hasOngoingLease = Leases
            .select(Leases.id)
            .where {
                (Leases.tenantId eq dbTenant.id.value) and
                (Leases.status neq LeaseStatus.TERMINATED)
            }
            .count() > 0

        if (hasOngoingLease) {
            throw HttpError(
                HttpStatusCode.Conflict,
                "Ce locataire possède déjà un contrat de bail en cours dans l'application."
            )
        }

        // 3. Calculate Total Requirement and Initial Status based on Advanced Payments
        val paymentFrequencyDto = try {
            PaymentFrequency.valueOf(request.paymentFrequency)
        } catch (e: Exception) {
            PaymentFrequency.MONTHLY
        }
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

        val now = LocalDateTime.now(Clock.systemUTC())

        // 3.1 Fetch or create guarantor
        val targetGuarantor: Guarantor? = if (!request.guarantorId.isNullOrBlank()) {
            val gid = try { UUID.fromString(request.guarantorId) } catch (_: Exception) { null }
            gid?.let { Guarantor.findById(it) }
        } else if (request.guarantor != null && request.guarantor!!.lastName.isNotBlank() && request.guarantor!!.firstName.isNotBlank()) {
            val gDto = request.guarantor!!
            Guarantor.new {
                this.tenant = dbTenant
                this.lastName = gDto.lastName.trim()
                this.firstName = gDto.firstName.trim()
                this.phone = gDto.phone.trim()
                this.monthlySalary = gDto.monthlySalary
                this.relationship = gDto.relationship?.trim()?.takeIf { it.isNotBlank() }
                this.jobTitle = gDto.jobTitle?.trim()?.takeIf { it.isNotBlank() }
                this.employer = gDto.employer?.trim()?.takeIf { it.isNotBlank() }
                this.createdAt = now
                this.updatedAt = now
            }
        } else {
            null
        }

        // Create Lease record
        val newLease = Lease.new {
            this.residenceUnit = dbResidenceUnit
            this.tenant = dbTenant
            this.guarantor = targetGuarantor
            this.durationMonths = if (duration <= 0) 12 else duration
            this.paymentFrequency = paymentFrequencyDto
            this.depositAmount = request.depositAmount
            this.depositStatus = if (initialPayment >= request.depositAmount) DepositStatus.PAID else DepositStatus.PENDING
            this.status = initialStatus
            this.startDate = parsedStart
            this.endDate = parsedEnd
            this.advanceMonths = advanceMonths
            this.advancePaymentAmount = initialPayment
            this.createdAt = now
            this.updatedAt = now
        }

        // 4. Update unit status to OCCUPIED
        dbResidenceUnit.status = UnitStatus.OCCUPIED
        dbResidenceUnit.flush()
        newLease.flush()

        // 5. Generate financial transaction entries if an advance payment was made
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
                    this.createdAt = now
                    this.updatedAt = now
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
                    this.createdAt = now
                    this.updatedAt = now
                }
            }
        }

        val payments = FinancialTransaction.find {
            (FinancialTransactions.relatedEntityType eq EntityType.BAIL) and
                    (FinancialTransactions.relatedEntityId eq newLease.id.value)
        }.map { tx ->
            LeasePaymentDto(
                id = tx.id.value.toString(),
                category = if (tx.category == TransactionCategory.DEPOSIT) LeaseCategory.DEPOSIT else LeaseCategory.RENT,
                amount = tx.amount,
                description = tx.description,
                transactionDate = tx.transactionDate.toString()
            )
        }

        val guarantorDto = newLease.guarantor?.let { g ->
            com.resid.manager.dto.GuarantorDto(
                id = g.id.value.toString(),
                tenantId = g.tenant.id.value.toString(),
                lastName = g.lastName,
                firstName = g.firstName,
                phone = g.phone,
                monthlySalary = g.monthlySalary,
                relationship = g.relationship,
                jobTitle = g.jobTitle,
                employer = g.employer
            )
        }

        LeaseDto(
            id = newLease.id.value.toString(),
            residenceUnitId = newLease.residenceUnit.id.value.toString(),
            tenantId = newLease.tenant?.id?.value?.toString(),
            startDate = newLease.startDate?.toString(),
            endDate = newLease.endDate?.toString(),
            depositAmount = newLease.depositAmount,
            monthlyRentAtSign = request.monthlyRentAtSign,
            status = initialStatus.convert(),
            createdAt = newLease.createdAt.toString(),
            updatedAt = newLease.updatedAt.toString(),
            paymentFrequencyDto = newLease.paymentFrequency?.convert(),
            advanceMonths = newLease.advanceMonths,
            payments = payments,
            guarantor = guarantorDto
        )
    }

    suspend fun updateLease(
        requesterId: UUID,
        leaseId: UUID,
        request: LeaseUpdateRequest
    ): LeaseDto = dbQuery {
        val dbLease = Lease.findById(leaseId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Contrat de bail introuvable.")

        val residenceId = dbLease.residenceUnit.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent modifier un contrat de bail."
            )
        }

        // Cannot edit a terminated lease
        if (dbLease.status == LeaseStatus.TERMINATED) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Impossible de modifier un contrat de bail résilié (TERMINATED)."
            )
        }

        // If lease is already SIGNED_ACTIVE, core contract terms cannot be modified
        val isSignedOrActive = dbLease.status == LeaseStatus.SIGNED_ACTIVE
        if (isSignedOrActive) {
            if (request.startDate != null || request.endDate != null ||
                request.depositAmount != null || request.advanceMonths != null ||
                request.paymentFrequency != null) {
                throw HttpError(
                    HttpStatusCode.BadRequest,
                    "Impossible de modifier les clauses initiales (dates, loyer, caution) d'un contrat de bail déjà signé/actif."
                )
            }
        }

        // 1. Update dates if supplied
        var newStart = dbLease.startDate
        var newEnd = dbLease.endDate

        request.startDate?.takeIf { it.isNotBlank() }?.let {
            newStart = try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                throw HttpError(HttpStatusCode.BadRequest, "Format de date de début invalide : ${e.message}")
            }
        }

        request.endDate?.takeIf { it.isNotBlank() }?.let {
            newEnd = try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                throw HttpError(HttpStatusCode.BadRequest, "Format de date de fin invalide : ${e.message}")
            }
        }

        if (newEnd != null && newStart != null) {
            if (newEnd.isBefore(newStart) || newEnd.isEqual(newStart)) {
                throw HttpError(HttpStatusCode.BadRequest, "La date de fin doit être strictement postérieure à la date de début.")
            }
        }

        dbLease.startDate = newStart
        dbLease.endDate = newEnd
        if (newStart != null && newEnd != null) {
            dbLease.durationMonths = java.time.temporal.ChronoUnit.MONTHS.between(newStart, newEnd).toInt().coerceAtLeast(1)
        }

        // 2. Update financial parameters
        request.depositAmount?.let {
            if (it < 0.0) throw HttpError(HttpStatusCode.BadRequest, "Le montant de la caution doit être supérieur ou égal à 0.")
            dbLease.depositAmount = it
        }

        request.advanceMonths?.let {
            if (it < 1) throw HttpError(HttpStatusCode.BadRequest, "Le nombre de mois d'avance doit être au moins de 1.")
            dbLease.advanceMonths = it
        }

        request.paymentFrequency?.takeIf { it.isNotBlank() }?.let { freqStr ->
            dbLease.paymentFrequency = try {
                PaymentFrequency.valueOf(freqStr)
            } catch (e: Exception) {
                throw HttpError(HttpStatusCode.BadRequest, "Fréquence de paiement invalide : $freqStr")
            }
        }

        // 3. Recalculate payment fulfillment & update status accordingly if payment is in progress
        if (dbLease.status in listOf(LeaseStatus.PENDING_PAYMENT, LeaseStatus.DOWN_PAYMENT_PAID, LeaseStatus.PENDING_SIGNATURE)) {
            val totalPaidCaution = FinancialTransaction.find {
                (FinancialTransactions.relatedEntityType eq EntityType.BAIL) and
                (FinancialTransactions.relatedEntityId eq leaseId) and
                (FinancialTransactions.type eq TransactionType.INCOME) and
                (FinancialTransactions.category eq TransactionCategory.DEPOSIT)
            }.sumOf { it.amount }

            val totalPaidRent = FinancialTransaction.find {
                (FinancialTransactions.relatedEntityType eq EntityType.BAIL) and
                (FinancialTransactions.relatedEntityId eq leaseId) and
                (FinancialTransactions.type eq TransactionType.INCOME) and
                ((FinancialTransactions.category eq TransactionCategory.RENT) or (FinancialTransactions.category eq TransactionCategory.LEASE_PAYMENT))
            }.sumOf { it.amount }

            val requiredCaution = dbLease.depositAmount ?: 0.0
            val requiredRent = (dbLease.advanceMonths ?: 1) * (dbLease.residenceUnit.nominalRent + dbLease.residenceUnit.serviceCharges)

            dbLease.status = if (totalPaidCaution >= requiredCaution && totalPaidRent >= requiredRent) {
                LeaseStatus.PENDING_SIGNATURE
            } else if (totalPaidCaution > 0.0 || totalPaidRent > 0.0) {
                LeaseStatus.DOWN_PAYMENT_PAID
            } else {
                LeaseStatus.PENDING_PAYMENT
            }

            dbLease.depositStatus = if (totalPaidCaution >= requiredCaution) DepositStatus.PAID else DepositStatus.PENDING
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
            tenantId = dbLease.tenant?.id?.value?.toString(),
            startDate = dbLease.startDate?.toString(),
            endDate = dbLease.endDate?.toString(),
            depositAmount = dbLease.depositAmount,
            monthlyRentAtSign = dbLease.residenceUnit.nominalRent,
            status = dbLease.status.convert(),
            createdAt = dbLease.createdAt.toString(),
            updatedAt = dbLease.updatedAt.toString(),
            paymentFrequencyDto = dbLease.paymentFrequency?.convert(),
            advanceMonths = dbLease.advanceMonths,
            payments = previousPayments
        )
    }

    suspend fun getLeasesByResidence(
        residenceId: UUID,
        leaseId: UUID?,
    ): List<LeaseDto> = dbQuery {
        Residence.findById(residenceId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

        (Leases innerJoin ResidenceUnits)
            .select(Leases.columns)
            .where {
                (ResidenceUnits.residenceId eq residenceId) and
                        (if (leaseId != null) Leases.id eq leaseId else Op.TRUE)
            }
            .let { Lease.wrapRows(it).toList() }
            .map { lease ->
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

                val guarantorDto = lease.guarantor?.let { g ->
                    com.resid.manager.dto.GuarantorDto(
                        id = g.id.value.toString(),
                        tenantId = g.tenant.id.value.toString(),
                        lastName = g.lastName,
                        firstName = g.firstName,
                        phone = g.phone,
                        monthlySalary = g.monthlySalary,
                        relationship = g.relationship,
                        jobTitle = g.jobTitle,
                        employer = g.employer
                    )
                }

                LeaseDto(
                    id = lease.id.value.toString(),
                    residenceUnitId = lease.residenceUnit.id.value.toString(),
                    tenantId = lease.tenant?.id?.value?.toString(),
                    startDate = lease.startDate?.toString(),
                    endDate = lease.endDate?.toString(),
                    depositAmount = lease.depositAmount,
                    monthlyRentAtSign = lease.residenceUnit.nominalRent,
                    status = lease.status.convert(),
                    createdAt = lease.createdAt.toString(),
                    updatedAt = lease.updatedAt.toString(),
                    paymentFrequencyDto = lease.paymentFrequency?.convert(),
                    advanceMonths = lease.advanceMonths,
                    payments = previousPayments,
                    guarantor = guarantorDto
                )
            }
    }

    suspend fun getGuarantorsByTenant(tenantId: UUID): List<com.resid.manager.dto.GuarantorDto> = dbQuery {
        Guarantor.find { Guarantors.tenantId eq tenantId }
            .orderBy(Guarantors.updatedAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .map { g ->
                com.resid.manager.dto.GuarantorDto(
                    id = g.id.value.toString(),
                    tenantId = g.tenant.id.value.toString(),
                    lastName = g.lastName,
                    firstName = g.firstName,
                    phone = g.phone,
                    monthlySalary = g.monthlySalary,
                    relationship = g.relationship,
                    jobTitle = g.jobTitle,
                    employer = g.employer
                )
            }
    }

    suspend fun recordPayment(
        requesterId: UUID,
        leaseId: UUID,
        request: LeasePaymentRequest
    ): LeaseDto = dbQuery {
        val dbLease = Lease.findById(leaseId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Contrat de bail introuvable.")

        val residenceId = dbLease.residenceUnit.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent enregistrer des paiements de bail."
            )
        }

        if (dbLease.status == LeaseStatus.TERMINATED) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Impossible d'enregistrer un paiement pour un contrat de bail résilié (TERMINATED)."
            )
        }

        if (request.category !in listOf(TransactionCategoryDto.DEPOSIT, TransactionCategoryDto.RENT)) {
            throw HttpError(HttpStatusCode.BadRequest, "Catégorie de paiement invalide pour cette opération.")
        }

        // 1. Fetch previous payments
        val previousPaidCaution = FinancialTransaction.find {
            (FinancialTransactions.relatedEntityType eq EntityType.BAIL) and
            (FinancialTransactions.relatedEntityId eq leaseId) and
            (FinancialTransactions.type eq TransactionType.INCOME) and
            (FinancialTransactions.category eq TransactionCategory.DEPOSIT)
        }.sumOf { it.amount }

        val previousPaidRent = FinancialTransaction.find {
            (FinancialTransactions.relatedEntityType eq EntityType.BAIL) and
            (FinancialTransactions.relatedEntityId eq leaseId) and
            (FinancialTransactions.type eq TransactionType.INCOME) and
            ((FinancialTransactions.category eq TransactionCategory.RENT) or (FinancialTransactions.category eq TransactionCategory.LEASE_PAYMENT))
        }.sumOf { it.amount }

        val requiredCaution = dbLease.depositAmount ?: 0.0
        val requiredRent = (dbLease.advanceMonths ?: 1) * (dbLease.residenceUnit.nominalRent + dbLease.residenceUnit.serviceCharges)

        if (request.category == TransactionCategoryDto.DEPOSIT) {
            val remainingCaution = maxOf(0.0, requiredCaution - previousPaidCaution)
            if (request.amountPaid > remainingCaution) {
                throw HttpError(
                    HttpStatusCode.BadRequest,
                    "Le montant saisi (${request.amountPaid}) dépasse le solde restant de la caution ($remainingCaution)."
                )
            }
        } else if (request.category == TransactionCategoryDto.RENT) {
            val remainingRent = maxOf(0.0, requiredRent - previousPaidRent)
            if (request.amountPaid > remainingRent) {
                throw HttpError(
                    HttpStatusCode.BadRequest,
                    "Le montant saisi (${request.amountPaid}) dépasse le solde restant des loyers d'avance ($remainingRent)."
                )
            }
        }

        val totalPaidCaution = previousPaidCaution + if (request.category == TransactionCategoryDto.DEPOSIT) request.amountPaid else 0.0
        val totalPaidRent = previousPaidRent + if (request.category == TransactionCategoryDto.RENT) request.amountPaid else 0.0

        val newStatus = if (totalPaidCaution >= requiredCaution && totalPaidRent >= requiredRent) {
            LeaseStatus.PENDING_SIGNATURE
        } else if (totalPaidCaution > 0.0 || totalPaidRent > 0.0) {
            LeaseStatus.DOWN_PAYMENT_PAID
        } else {
            LeaseStatus.PENDING_PAYMENT
        }

        dbLease.status = newStatus
        if (totalPaidCaution >= requiredCaution) {
            dbLease.depositStatus = DepositStatus.PAID
        }
        val now = LocalDateTime.now(Clock.systemUTC())
        dbLease.updatedAt = now
        dbLease.flush()

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
            this.relatedEntityId = leaseId
            this.transactionDate = LocalDate.now()
            this.createdAt = now
            this.updatedAt = now
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
            tenantId = dbLease.tenant?.id?.value?.toString(),
            startDate = dbLease.startDate?.toString(),
            endDate = dbLease.endDate?.toString(),
            depositAmount = dbLease.depositAmount,
            monthlyRentAtSign = dbLease.residenceUnit.nominalRent,
            status = newStatus.convert(),
            createdAt = dbLease.createdAt.toString(),
            updatedAt = dbLease.updatedAt.toString(),
            paymentFrequencyDto = dbLease.paymentFrequency?.convert(),
            advanceMonths = dbLease.advanceMonths,
            payments = previousPayments
        )
    }

    suspend fun updateLeaseStatus(
        requesterId: UUID,
        leaseId: UUID,
        newStatus: LeaseStatusDto
    ): LeaseDto = dbQuery {
        val dbLease = Lease.findById(leaseId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Contrat de bail introuvable.")

        val residenceId = dbLease.residenceUnit.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent modifier le statut d'un bail."
            )
        }

        val currentStatus = dbLease.status.convert()

        // -------------------------------------------------------------
        // Business Rules & State Transition Validation
        // -------------------------------------------------------------
        if (currentStatus == newStatus) {
            // No-op transition
            return@dbQuery dbLease.toDto()
        }

        if (currentStatus == LeaseStatusDto.TERMINATED) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Transition impossible : un contrat de bail résilié (TERMINATED) ne peut plus changer de statut."
            )
        }

        when (newStatus) {
            LeaseStatusDto.DRAFT -> {
                throw HttpError(
                    HttpStatusCode.BadRequest,
                    "Transition impossible : un contrat existant ne peut pas redevenir un brouillon (DRAFT)."
                )
            }
            LeaseStatusDto.SIGNED_ACTIVE -> {
                // To activate/sign the lease, payments must be satisfied (PENDING_SIGNATURE)
                // Or transitioning from an earlier payment step if manager explicitly validates full manual payment
                if (currentStatus != LeaseStatusDto.PENDING_SIGNATURE && currentStatus != LeaseStatusDto.DOWN_PAYMENT_PAID && currentStatus != LeaseStatusDto.PENDING_PAYMENT) {
                    throw HttpError(
                        HttpStatusCode.BadRequest,
                        "Transition impossible vers SIGNED_ACTIVE depuis l'état $currentStatus."
                    )
                }

                // Verify the unit is not occupied by another active lease
                val otherActiveLease = Leases
                    .select(Leases.id)
                    .where {
                        (Leases.residenceUnitId eq dbLease.residenceUnit.id.value) and
                        (Leases.id neq leaseId) and
                        (Leases.status eq LeaseStatus.SIGNED_ACTIVE)
                    }
                    .count() > 0

                if (otherActiveLease) {
                    throw HttpError(
                        HttpStatusCode.Conflict,
                        "Impossible d'activer ce bail : un autre contrat de bail actif existe déjà sur ce logement."
                    )
                }

                dbLease.residenceUnit.status = UnitStatus.OCCUPIED
                dbLease.residenceUnit.flush()
            }

            LeaseStatusDto.TERMINATED -> {
                // Free up the unit back to AVAILABLE
                dbLease.residenceUnit.status = UnitStatus.AVAILABLE
                dbLease.residenceUnit.flush()
            }

            LeaseStatusDto.DOWN_PAYMENT_PAID, LeaseStatusDto.PENDING_PAYMENT, LeaseStatusDto.PENDING_SIGNATURE -> {
                // If the lease is already SIGNED_ACTIVE, we cannot revert back to unconfirmed payment steps
                if (currentStatus == LeaseStatusDto.SIGNED_ACTIVE) {
                    throw HttpError(
                        HttpStatusCode.BadRequest,
                        "Transition impossible : un contrat de bail déjà signé et actif (SIGNED_ACTIVE) ne peut pas revenir à l'état $newStatus."
                    )
                }
            }

            LeaseStatusDto.PARTIALLY_PAID -> {
                if (currentStatus == LeaseStatusDto.SIGNED_ACTIVE) {
                    throw HttpError(
                        HttpStatusCode.BadRequest,
                        "Transition impossible : un contrat actif ne peut pas rétrograder à PARTIALLY_PAID."
                    )
                }
            }
        }

        dbLease.status = newStatus.convert()
        dbLease.updatedAt = LocalDateTime.now(Clock.systemUTC())
        dbLease.flush()

        dbLease.toDto()
    }

    suspend fun deleteLease(
        requesterId: UUID,
        leaseId: UUID
    ): Unit = dbQuery {
        val dbLease = Lease.findById(leaseId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Contrat de bail introuvable.")

        val residenceId = dbLease.residenceUnit.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent supprimer un contrat de bail."
            )
        }

        // Security check: Delete is only allowed for unsigned leases
        if (dbLease.status == LeaseStatus.SIGNED_ACTIVE || dbLease.status == LeaseStatus.TERMINATED) {
            throw HttpError(
                HttpStatusCode.Conflict,
                "Impossible de supprimer un contrat de bail signé, actif ou clôturé."
            )
        }

        // Clean up any financial transactions recorded for this draft/pending lease
        val transactions = FinancialTransaction.find {
            (FinancialTransactions.relatedEntityType eq EntityType.BAIL) and
            (FinancialTransactions.relatedEntityId eq leaseId)
        }
        transactions.forEach { it.delete() }

        // Free the unit if it was set to OCCUPIED by an advance draft activation
        val unit = dbLease.residenceUnit
        if (unit.status == UnitStatus.OCCUPIED) {
            val otherActive = Leases.select(Leases.id).where {
                (Leases.residenceUnitId eq unit.id.value) and
                (Leases.id neq leaseId) and
                (Leases.status eq LeaseStatus.SIGNED_ACTIVE)
            }.count() > 0

            if (!otherActive) {
                unit.status = UnitStatus.AVAILABLE
                unit.flush()
            }
        }

        dbLease.delete()
    }

    private fun Guarantor.toDto(): GuarantorDto {
        return GuarantorDto(
            id = this.id.value.toString(),
            tenantId = this.tenant.id.value.toString(),
            lastName = this.lastName,
            firstName = this.firstName,
            phone = this.phone,
            monthlySalary = this.monthlySalary,
            relationship = this.relationship,
            jobTitle = this.jobTitle,
            employer = this.employer
        )
    }

    private fun Lease.toDto(): LeaseDto {
        val leaseEntityId = this.id.value
        val previousPayments = FinancialTransaction.find {
            (FinancialTransactions.relatedEntityType eq EntityType.BAIL) and
            (FinancialTransactions.relatedEntityId eq leaseEntityId)
        }.map { tx ->
            LeasePaymentDto(
                id = tx.id.value.toString(),
                category = if (tx.category == TransactionCategory.DEPOSIT) LeaseCategory.DEPOSIT else LeaseCategory.RENT,
                amount = tx.amount,
                description = tx.description,
                transactionDate = tx.transactionDate.toString()
            )
        }

        return LeaseDto(
            id = this.id.value.toString(),
            residenceUnitId = this.residenceUnit.id.value.toString(),
            tenantId = this.tenant?.id?.value?.toString(),
            startDate = this.startDate?.toString(),
            endDate = this.endDate?.toString(),
            depositAmount = this.depositAmount,
            monthlyRentAtSign = this.residenceUnit.nominalRent,
            status = this.status.convert(),
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString(),
            paymentFrequencyDto = this.paymentFrequency?.convert(),
            advanceMonths = this.advanceMonths,
            payments = previousPayments,
            guarantor = this.guarantor?.toDto()
        )
    }
}
