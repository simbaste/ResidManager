package com.resid.manager.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.resid.manager.data.DepositStatus
import com.resid.manager.data.EntityType
import com.resid.manager.data.FinancialTransaction
import com.resid.manager.data.FinancialTransactions
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
import com.resid.manager.dto.LeaseCategory
import com.resid.manager.dto.LeaseCreateRequest
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
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

object LeaseService {

    fun isResidenceManager(userId: UUID, residenceId: UUID): Boolean = transaction {
        ResidenceMembers.select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.any { it[ResidenceMembers.role].isManager() }
    }

    fun createLease(
        requesterId: UUID,
        unitId: UUID,
        request: LeaseCreateRequest
    ): LeaseDto = transaction {
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
            advanceMonths = newLease.advanceMonths,
            payments = payments,
        )
    }

    fun updateLease(
        requesterId: UUID,
        leaseId: UUID,
        request: LeaseUpdateRequest
    ): LeaseDto = transaction {
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

        if (newEnd.isBefore(newStart) || newEnd.isEqual(newStart)) {
            throw HttpError(HttpStatusCode.BadRequest, "La date de fin doit être strictement postérieure à la date de début.")
        }

        dbLease.startDate = newStart
        dbLease.endDate = newEnd
        dbLease.durationMonths = java.time.temporal.ChronoUnit.MONTHS.between(newStart, newEnd).toInt().coerceAtLeast(1)

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

            val requiredCaution = dbLease.depositAmount
            val requiredRent = dbLease.advanceMonths * (dbLease.residenceUnit.nominalRent + dbLease.residenceUnit.serviceCharges)

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

    fun getLeasesByResidence(
        residenceId: UUID,
        leaseId: UUID?,
    ): List<LeaseDto> = transaction {
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

    fun recordPayment(
        requesterId: UUID,
        leaseId: UUID,
        request: LeasePaymentRequest
    ): LeaseDto = transaction {
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

        val totalPaidCaution = previousPaidCaution + if (request.category == TransactionCategoryDto.DEPOSIT) request.amountPaid else 0.0
        val totalPaidRent = previousPaidRent + if (request.category == TransactionCategoryDto.RENT) request.amountPaid else 0.0

        val requiredCaution = dbLease.depositAmount
        val requiredRent = dbLease.advanceMonths * (dbLease.residenceUnit.nominalRent + dbLease.residenceUnit.serviceCharges)

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

    fun updateLeaseStatus(
        requesterId: UUID,
        leaseId: UUID,
        newStatus: LeaseStatusDto
    ): LeaseDto = transaction {
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
            return@transaction dbLease.toDto()
        }

        if (currentStatus == LeaseStatusDto.TERMINATED) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Transition impossible : un contrat de bail résilié (TERMINATED) ne peut plus changer de statut."
            )
        }

        when (newStatus) {
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
            tenantId = this.tenant.id.value.toString(),
            startDate = this.startDate.toString(),
            endDate = this.endDate.toString(),
            depositAmount = this.depositAmount,
            monthlyRentAtSign = this.residenceUnit.nominalRent,
            status = this.status.convert(),
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString(),
            paymentFrequencyDto = this.paymentFrequency.convert(),
            advanceMonths = this.advanceMonths,
            payments = previousPayments
        )
    }
}
