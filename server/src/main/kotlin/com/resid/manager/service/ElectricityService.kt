package com.resid.manager.service

import com.resid.manager.data.*
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.dto.StatementStatus
import org.jetbrains.exposed.sql.SortOrder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object ElectricityService {

    fun getPreviousIndex(logementId: UUID): Double {
        // Rechercher dans ElectricityStatements le dernier relevé validé (trié par statementDate décroissant)
        val lastStatement = ElectricityStatement.find { 
            ElectricityStatements.logementId eq logementId 
        }.orderBy(ElectricityStatements.statementDate to SortOrder.DESC).firstOrNull()

        return if (lastStatement != null) {
            lastStatement.newIndex
        } else {
            // Si aucun relevé n'existe, récupérer initialElectricityIndex dans table Logements
            val logement = Logement.findById(logementId) ?: throw Exception("Logement introuvable.")
            logement.initialElectricityIndex
        }
    }

    fun submitStatement(
        logementId: UUID, 
        newIndex: Double, 
        kWhPriceApplied: Double, 
        dateStr: String
    ): ElectricityStatementDto {
        val parsedDate = LocalDate.parse(dateStr)
        val oldIndex = getPreviousIndex(logementId)

        // 1. Validation stricte
        if (newIndex < oldIndex) {
            throw IllegalArgumentException("Validation Error: New Index ($newIndex) cannot be less than Previous Index ($oldIndex).")
        }

        // 2. Calcul de la facture
        val amount = (newIndex - oldIndex) * kWhPriceApplied

        val dbLogement = Logement.findById(logementId) ?: throw Exception("Logement introuvable.")

        // 3. Persistance du relevé d'électricité
        val statement = ElectricityStatement.new {
            this.logement = dbLogement
            this.oldIndex = oldIndex
            this.newIndex = newIndex
            this.kWhPriceApplied = kWhPriceApplied
            this.amountDue = amount
            this.status = "UNPAID"
            this.statementDate = parsedDate
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }
        statement.flush()

        // 4. Insertion automatique de l'écriture correspondante dans la table FinancialTransactions
        FinancialTransaction.new {
            this.residence = dbLogement.residence
            this.type = "INCOME"
            this.category = "Electricity"
            this.amount = amount
            this.description = "Facture d'électricité relevé logement ${dbLogement.name} ($oldIndex -> $newIndex)"
            this.relatedEntityType = "ELECTRICITY_STATEMENT"
            this.relatedEntityId = statement.id.value
            this.transactionDate = LocalDate.now()
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }

        return ElectricityStatementDto(
            id = statement.id.value.toString(),
            logementId = statement.logement.id.value.toString(),
            previousIndex = statement.oldIndex,
            newIndex = statement.newIndex,
            kWhPriceApplied = statement.kWhPriceApplied,
            amountDue = statement.amountDue,
            statementDate = statement.statementDate.toString(),
            status = StatementStatus.UNPAID,
            createdAt = statement.createdAt.toString(),
            updatedAt = statement.updatedAt.toString()
            )
    }
}
