package com.resid.manager.service

import com.resid.manager.data.ElectricityStatement
import com.resid.manager.data.ElectricityStatements
import com.resid.manager.data.ElectricityStatus
import com.resid.manager.data.EntityType
import com.resid.manager.data.FinancialTransaction
import com.resid.manager.data.FinancialTransactions
import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.LeaseStatus
import com.resid.manager.data.Leases
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.ResidenceUnit
import com.resid.manager.data.ResidenceUnits
import com.resid.manager.data.TransactionCategory
import com.resid.manager.data.TransactionType
import com.resid.manager.data.convert
import com.resid.manager.data.isManager
import com.resid.manager.dto.ElectricityStatementCreateRequest
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.dto.ElectricityStatementUpdateRequest
import com.resid.manager.dto.ElectricityStatusDto
import com.resid.manager.dto.ElectricityStatusUpdateRequest
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object ElectricityService {

    suspend fun isResidenceManager(userId: UUID, residenceId: UUID): Boolean = dbQuery {
        ResidenceMembers.select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.any { it[ResidenceMembers.role].isManager() }
    }

    suspend fun isResidenceMember(userId: UUID, residenceId: UUID): Boolean = dbQuery {
        ResidenceMembers.select(ResidenceMembers.userId)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.count() > 0
    }

    suspend fun getPreviousIndex(residenceUnitId: UUID): Double = dbQuery {
        val lastStatement = ElectricityStatement.find { 
            ElectricityStatements.residenceUnitId eq residenceUnitId
        }.orderBy(ElectricityStatements.statementDate to SortOrder.DESC).firstOrNull()

        if (lastStatement != null) {
            lastStatement.newIndex
        } else {
            val dbResidenceUnit = ResidenceUnit.findById(residenceUnitId)
                ?: throw HttpError(HttpStatusCode.NotFound, "Logement introuvable.")
            dbResidenceUnit.initialElectricityIndex
        }
    }

    suspend fun createStatement(
        requesterId: UUID,
        request: ElectricityStatementCreateRequest
    ): ElectricityStatementDto = dbQuery {
        val unitUuid = try {
            UUID.fromString(request.unitId)
        } catch (e: Exception) {
            throw HttpError(HttpStatusCode.BadRequest, "ID de logement invalide : ${e.message}")
        }

        val dbResidenceUnit = ResidenceUnit.findById(unitUuid)
            ?: throw HttpError(HttpStatusCode.NotFound, "Logement introuvable.")

        val residenceId = dbResidenceUnit.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les gestionnaires et administrateurs peuvent créer un relevé d'électricité."
            )
        }

        val parsedDate = try {
            LocalDate.parse(request.statementDate)
        } catch (e: Exception) {
            throw HttpError(HttpStatusCode.BadRequest, "Format de date invalide : ${e.message}")
        }

        val previousIndex = request.previousIndex
        val newIndex = request.newIndex
        val price = request.kWhPriceApplied

        if (newIndex < previousIndex) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Validation Error: Le nouvel index ($newIndex) ne peut pas être inférieur à l'index précédent ($previousIndex)."
            )
        }

        if (price < 0.0) {
            throw HttpError(HttpStatusCode.BadRequest, "Le prix du kWh doit être supérieur ou égal à 0.")
        }

        val amount = (newIndex - previousIndex) * price
        val now = LocalDateTime.now(Clock.systemUTC())

        val statement = ElectricityStatement.new {
            this.residenceUnit = dbResidenceUnit
            this.oldIndex = previousIndex
            this.newIndex = newIndex
            this.kWhPriceApplied = price
            this.amountDue = amount
            this.status = ElectricityStatus.UNPAID
            this.statementDate = parsedDate
            this.createdAt = now
            this.updatedAt = now
        }
        statement.flush()

        // Generate matching FinancialTransaction income
        FinancialTransaction.new {
            this.residence = dbResidenceUnit.residence
            this.type = TransactionType.INCOME
            this.category = TransactionCategory.ELECTRICITY
            this.amount = amount
            this.description = "Facture d'électricité relevé logement ${dbResidenceUnit.name} ($previousIndex -> $newIndex)"
            this.relatedEntityType = EntityType.ELECTRICITY_STATEMENT
            this.relatedEntityId = statement.id.value
            this.transactionDate = LocalDate.now()
            this.createdAt = now
            this.updatedAt = now
        }

        statement.toDto()
    }

    suspend fun updateStatement(
        requesterId: UUID,
        statementId: UUID,
        request: ElectricityStatementUpdateRequest
    ): ElectricityStatementDto = dbQuery {
        val stmt = ElectricityStatement.findById(statementId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Relevé d'électricité introuvable.")

        val residenceId = stmt.residenceUnit.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les gestionnaires et administrateurs peuvent modifier un relevé d'électricité."
            )
        }

        val now = LocalDateTime.now(Clock.systemUTC())

        request.statementDate?.takeIf { it.isNotBlank() }?.let {
            stmt.statementDate = try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                throw HttpError(HttpStatusCode.BadRequest, "Format de date invalide : ${e.message}")
            }
        }

        request.previousIndex?.let {
            if (it < 0.0) throw HttpError(HttpStatusCode.BadRequest, "L'index précédent doit être supérieur ou égal à 0.")
            stmt.oldIndex = it
        }

        request.newIndex?.let {
            if (it < stmt.oldIndex) throw HttpError(HttpStatusCode.BadRequest, "Le nouvel index ne peut pas être inférieur à l'index précédent.")
            stmt.newIndex = it
        }

        request.kWhPriceApplied?.let {
            if (it < 0.0) throw HttpError(HttpStatusCode.BadRequest, "Le prix du kWh doit être supérieur ou égal à 0.")
            stmt.kWhPriceApplied = it
        }

        // Recalculate amount due
        val newAmount = (stmt.newIndex - stmt.oldIndex) * stmt.kWhPriceApplied
        stmt.amountDue = newAmount

        request.status?.let {
            stmt.status = it.convert()
        }

        stmt.updatedAt = now
        stmt.flush()

        // Synchronize related FinancialTransaction
        val tx = FinancialTransaction.find {
            (FinancialTransactions.relatedEntityType eq EntityType.ELECTRICITY_STATEMENT) and
            (FinancialTransactions.relatedEntityId eq statementId)
        }.firstOrNull()

        if (tx != null) {
            tx.amount = newAmount
            tx.description = "${if (stmt.status == ElectricityStatus.PAID) "[PAID] " else ""}Facture d'électricité relevé logement ${stmt.residenceUnit.name} (${stmt.oldIndex} -> ${stmt.newIndex})"
            tx.updatedAt = now
            tx.flush()
        }

        stmt.toDto()
    }

    suspend fun updateStatementStatus(
        requesterId: UUID,
        statementId: UUID,
        request: ElectricityStatusUpdateRequest
    ): ElectricityStatementDto = dbQuery {
        val stmt = ElectricityStatement.findById(statementId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Relevé d'électricité introuvable.")

        val residenceId = stmt.residenceUnit.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les gestionnaires et administrateurs peuvent modifier le statut d'un relevé."
            )
        }

        val now = LocalDateTime.now(Clock.systemUTC())
        stmt.status = request.status.convert()
        stmt.updatedAt = now
        stmt.flush()

        // Update financial transaction prefix
        val tx = FinancialTransaction.find {
            (FinancialTransactions.relatedEntityType eq EntityType.ELECTRICITY_STATEMENT) and
            (FinancialTransactions.relatedEntityId eq statementId)
        }.firstOrNull()

        if (tx != null) {
            val cleanDesc = tx.description.removePrefix("[PAID] ").trim()
            tx.description = if (stmt.status == ElectricityStatus.PAID) "[PAID] $cleanDesc" else cleanDesc
            tx.updatedAt = now
            tx.flush()
        }

        stmt.toDto()
    }

    suspend fun getStatements(
        requesterId: UUID,
        residenceUnitId: UUID?,
        residenceId: UUID?,
        tenantId: UUID?,
        status: ElectricityStatusDto?,
        floor: String?
    ): List<ElectricityStatementDto> = dbQuery {
        var conditions: Op<Boolean> = Op.TRUE

        if (residenceUnitId != null) {
            val dbResidenceUnit = ResidenceUnit.findById(residenceUnitId)
                ?: throw HttpError(HttpStatusCode.NotFound, "Logement introuvable.")

            val resId = dbResidenceUnit.residence.id.value
            val isTenant = Leases.select(Leases.id).where {
                (Leases.residenceUnitId eq residenceUnitId) and
                (Leases.tenantId eq requesterId) and
                (Leases.status eq LeaseStatus.SIGNED_ACTIVE)
            }.count() > 0

            if (!isTenant && !isResidenceMember(requesterId, resId)) {
                throw HttpError(
                    HttpStatusCode.Forbidden,
                    "Accès interdit : Vous ne faites pas partie de cette résidence."
                )
            }
            conditions = conditions and (ElectricityStatements.residenceUnitId eq residenceUnitId)
        }

        if (residenceId != null) {
            if (!isResidenceMember(requesterId, residenceId)) {
                throw HttpError(
                    HttpStatusCode.Forbidden,
                    "Accès interdit : Vous ne faites pas partie de cette résidence."
                )
            }
            conditions = conditions and (ResidenceUnits.residenceId eq residenceId)
        }

        if (status != null) {
            conditions = conditions and (ElectricityStatements.status eq status.convert())
        }

        if (!floor.isNullOrBlank()) {
            conditions = conditions and (ResidenceUnits.floor eq floor)
        }

        if (tenantId != null) {
            val leasedUnitIds = Leases
                .select(Leases.residenceUnitId)
                .where {
                    (Leases.tenantId eq tenantId) and
                    (Leases.status eq LeaseStatus.SIGNED_ACTIVE)
                }
                .map { it[Leases.residenceUnitId].value }

            conditions = conditions and (ElectricityStatements.residenceUnitId inList leasedUnitIds)
        }

        (ElectricityStatements innerJoin ResidenceUnits)
            .select(ElectricityStatements.columns)
            .where { conditions }
            .orderBy(ElectricityStatements.statementDate to SortOrder.DESC)
            .let { ElectricityStatement.wrapRows(it).toList() }
            .map { it.toDto() }
    }

    suspend fun deleteStatement(statementId: UUID) = dbQuery {
        ElectricityStatement.findById(statementId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Relevé d'électricité introuvable.")
        ElectricityStatements.deleteWhere {
            id eq statementId
        }
    }

    private fun ElectricityStatement.toDto(): ElectricityStatementDto {
        return ElectricityStatementDto(
            id = this.id.value.toString(),
            unitId = this.residenceUnit.id.value.toString(),
            previousIndex = this.oldIndex,
            newIndex = this.newIndex,
            kWhPriceApplied = this.kWhPriceApplied,
            amountDue = this.amountDue,
            statementDate = this.statementDate.toString(),
            status = this.status.convert(),
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
}
