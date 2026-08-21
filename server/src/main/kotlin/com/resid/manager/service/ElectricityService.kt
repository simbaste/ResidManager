package com.resid.manager.service

import com.resid.manager.data.*
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.dto.ElectricityStatusDto
import org.jetbrains.exposed.sql.SortOrder
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object ElectricityService {

    fun getPreviousIndex(residenceUnitId: UUID): Double {
        // Rechercher dans ElectricityStatements le dernier relevé validé (trié par statementDate décroissant)
        val lastStatement = ElectricityStatement.find { 
            ElectricityStatements.residenceUnitId eq residenceUnitId
        }.orderBy(ElectricityStatements.statementDate to SortOrder.DESC).firstOrNull()

        return if (lastStatement != null) {
            lastStatement.newIndex
        } else {
            // Si aucun relevé n'existe, récupérer initialElectricityIndex dans table Logements
            val dbResidenceUnit = ResidenceUnit.findById(residenceUnitId) ?: throw Exception("Logement introuvable.")
            dbResidenceUnit.initialElectricityIndex
        }
    }

    fun submitStatement(
        residenceUnitId: UUID, 
        newIndex: Double, 
        kWhPriceApplied: Double, 
        dateStr: String
    ): ElectricityStatementDto {
        val parsedDate = LocalDate.parse(dateStr)
        val oldIndex = getPreviousIndex(residenceUnitId)

        // 1. Validation stricte
        if (newIndex < oldIndex) {
            throw IllegalArgumentException("Validation Error: New Index ($newIndex) cannot be less than Previous Index ($oldIndex).")
        }

        // 2. Calcul de la facture
        val amount = (newIndex - oldIndex) * kWhPriceApplied

        val dbResidenceUnit = ResidenceUnit.findById(residenceUnitId) ?: throw Exception("Logement introuvable.")

        // 3. Persistance du relevé d'électricité
        val statement = ElectricityStatement.new {
            this.residenceUnit = dbResidenceUnit
            this.oldIndex = oldIndex
            this.newIndex = newIndex
            this.kWhPriceApplied = kWhPriceApplied
            this.amountDue = amount
            this.status = ElectricityStatus.UNPAID
            this.statementDate = parsedDate
            this.createdAt = LocalDateTime.now(Clock.systemUTC())
            this.updatedAt = LocalDateTime.now(Clock.systemUTC())
        }
        statement.flush()

        // 4. Insertion automatique de l'écriture correspondante dans la table FinancialTransactions
        FinancialTransaction.new {
            this.residence = dbResidenceUnit.residence
            this.type = TransactionType.INCOME
            this.category = TransactionCategory.ELECTRICITY
            this.amount = amount
            this.description = "Facture d'électricité relevé logement ${dbResidenceUnit.name} ($oldIndex -> $newIndex)"
            this.relatedEntityType = EntityType.ELECTRICITY_STATEMENT
            this.relatedEntityId = statement.id.value
            this.transactionDate = LocalDate.now()
            this.createdAt = LocalDateTime.now(Clock.systemUTC())
            this.updatedAt = LocalDateTime.now(Clock.systemUTC())
        }

        return ElectricityStatementDto(
            id = statement.id.value.toString(),
            residenceUnitId = statement.residenceUnit.id.value.toString(),
            previousIndex = statement.oldIndex,
            newIndex = statement.newIndex,
            kWhPriceApplied = statement.kWhPriceApplied,
            amountDue = statement.amountDue,
            statementDate = statement.statementDate.toString(),
            status = ElectricityStatusDto.UNPAID,
            createdAt = statement.createdAt.toString(),
            updatedAt = statement.updatedAt.toString()
            )
    }
}
