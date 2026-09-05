package com.resid.manager.service

import com.resid.manager.data.FinancialTransaction
import com.resid.manager.data.FinancialTransactions
import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.Residence
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.TransactionCategory
import com.resid.manager.data.TransactionType
import com.resid.manager.data.convert
import com.resid.manager.data.isManager
import com.resid.manager.dto.ExpenseRecordRequest
import com.resid.manager.dto.FinanceTransactionDto
import com.resid.manager.dto.TransactionCategoryDto
import com.resid.manager.dto.TransactionTypeDto
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object FinancialTransactionService {

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

    suspend fun recordExpense(
        requesterId: UUID,
        residenceId: UUID,
        request: ExpenseRecordRequest
    ): FinanceTransactionDto = dbQuery {
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent enregistrer des dépenses."
            )
        }

        if (request.amount <= 0.0) {
            throw HttpError(HttpStatusCode.BadRequest, "Le montant doit être strictement supérieur à 0.")
        }

        val category = request.category.convert()

        val validExpenseCategories = listOf(
            TransactionCategory.CLEANING,
            TransactionCategory.FUEL,
            TransactionCategory.SECURITY,
            TransactionCategory.MAINTENANCE,
            TransactionCategory.TAXES,
            TransactionCategory.OTHER
        )

        if (category !in validExpenseCategories) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Catégorie de dépense invalide. Choix possibles : ${validExpenseCategories.joinToString { it.name }}"
            )
        }

        val parsedDate = try {
            LocalDate.parse(request.transactionDate)
        } catch (e: Exception) {
            throw HttpError(HttpStatusCode.BadRequest, "Format de date invalide : ${e.message}")
        }

        val dbResidence = Residence.findById(residenceId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

        val now = LocalDateTime.now(Clock.systemUTC())
        val tx = FinancialTransaction.new {
            this.residence = dbResidence
            this.type = TransactionType.EXPENSE
            this.category = category
            this.amount = request.amount
            this.description = request.description
            this.relatedEntityType = null
            this.relatedEntityId = null
            this.transactionDate = parsedDate
            this.createdAt = now
            this.updatedAt = now
        }
        tx.flush()

        FinanceTransactionDto(
            id = tx.id.value.toString(),
            residenceId = tx.residence.id.value.toString(),
            type = tx.type.convert(),
            category = tx.category.convert(),
            amount = tx.amount,
            description = tx.description,
            relatedEntityType = tx.relatedEntityType?.convert(),
            relatedEntityId = tx.relatedEntityId?.toString(),
            transactionDate = tx.transactionDate.toString(),
            createdAt = tx.createdAt.toString()
        )
    }

    suspend fun getTransactions(
        requesterId: UUID,
        residenceId: UUID,
        type: TransactionTypeDto?,
        category: TransactionCategoryDto?,
        startDate: String?,
        endDate: String?,
        queryText: String?
    ): List<FinanceTransactionDto> = dbQuery {
        if (!isResidenceMember(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Vous ne faites pas partie de cette résidence."
            )
        }

        var query = FinancialTransactions.select(FinancialTransactions.columns)
            .where { FinancialTransactions.residenceId eq residenceId }

        if (type != null) {
            query = query.andWhere { FinancialTransactions.type eq type.convert() }
        }

        if (category != null) {
            query = query.andWhere { FinancialTransactions.category eq category.convert() }
        }

        if (!startDate.isNullOrBlank() && !endDate.isNullOrBlank()) {
            val start = try {
                LocalDate.parse(startDate)
            } catch (e: Exception) {
                throw HttpError(HttpStatusCode.BadRequest, "Date de début invalide : ${e.message}")
            }
            val end = try {
                LocalDate.parse(endDate)
            } catch (e: Exception) {
                throw HttpError(HttpStatusCode.BadRequest, "Date de fin invalide : ${e.message}")
            }
            query = query.andWhere { FinancialTransactions.transactionDate.between(start, end) }
        }

        queryText?.trim()?.takeIf { it.isNotBlank() }?.let { text ->
            query = query.andWhere { FinancialTransactions.description.like("%$text%") }
        }

        query = query.orderBy(FinancialTransactions.transactionDate to SortOrder.DESC)

        query.map { row ->
            FinanceTransactionDto(
                id = row[FinancialTransactions.id].value.toString(),
                residenceId = row[FinancialTransactions.residenceId].value.toString(),
                type = row[FinancialTransactions.type].convert(),
                category = row[FinancialTransactions.category].convert(),
                amount = row[FinancialTransactions.amount],
                description = row[FinancialTransactions.description],
                relatedEntityType = row[FinancialTransactions.relatedEntityType]?.convert(),
                relatedEntityId = row[FinancialTransactions.relatedEntityId]?.toString(),
                transactionDate = row[FinancialTransactions.transactionDate].toString(),
                createdAt = row[FinancialTransactions.createdAt].toString()
            )
        }
    }

    suspend fun getTransactionById(
        requesterId: UUID,
        transactionId: UUID
    ): FinanceTransactionDto = dbQuery {
        val tx = FinancialTransaction.findById(transactionId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Transaction financière introuvable.")

        val residenceId = tx.residence.id.value
        if (!isResidenceMember(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Vous ne faites pas partie de cette résidence."
            )
        }

        tx.toDto()
    }

    suspend fun updateTransaction(
        requesterId: UUID,
        transactionId: UUID,
        request: com.resid.manager.dto.TransactionUpdateRequest
    ): FinanceTransactionDto = dbQuery {
        val tx = FinancialTransaction.findById(transactionId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Transaction financière introuvable.")

        val residenceId = tx.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent modifier une transaction financière."
            )
        }

        request.amount?.let {
            if (it <= 0.0) throw HttpError(HttpStatusCode.BadRequest, "Le montant doit être strictement supérieur à 0.")
            tx.amount = it
        }

        request.category?.takeIf { it.isNotBlank() }?.let { catStr ->
            val newCategory = try {
                TransactionCategory.valueOf(catStr.trim().uppercase())
            } catch (e: Exception) {
                throw HttpError(HttpStatusCode.BadRequest, "Catégorie inconnue : '$catStr'.")
            }
            tx.category = newCategory
        }

        request.description?.takeIf { it.isNotBlank() }?.let {
            tx.description = it
        }

        request.transactionDate?.takeIf { it.isNotBlank() }?.let { dateStr ->
            tx.transactionDate = try {
                LocalDate.parse(dateStr)
            } catch (e: Exception) {
                throw HttpError(HttpStatusCode.BadRequest, "Format de date invalide : ${e.message}")
            }
        }

        val now = LocalDateTime.now(Clock.systemUTC())
        tx.updatedAt = now
        tx.flush()

        tx.toDto()
    }

    suspend fun deleteTransaction(
        requesterId: UUID,
        transactionId: UUID
    ) = dbQuery {
        val tx = FinancialTransaction.findById(transactionId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Transaction financière introuvable.")

        val residenceId = tx.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent supprimer une écriture financière."
            )
        }

        tx.delete()
    }

    private fun FinancialTransaction.toDto(): FinanceTransactionDto {
        return FinanceTransactionDto(
            id = this.id.value.toString(),
            residenceId = this.residence.id.value.toString(),
            type = this.type.convert(),
            category = this.category.convert(),
            amount = this.amount,
            description = this.description,
            relatedEntityType = this.relatedEntityType?.convert(),
            relatedEntityId = this.relatedEntityId?.toString(),
            transactionDate = this.transactionDate.toString(),
            createdAt = this.createdAt.toString()
        )
    }
}
