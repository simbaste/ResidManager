package com.resid.manager.service

import com.resid.manager.data.*
import com.resid.manager.dto.FinanceTransactionDto
import org.jetbrains.exposed.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object FinanceOperationService {

    fun recordExpense(
        residenceId: UUID,
        categoryName: String,
        amount: Double,
        description: String,
        date: LocalDate
    ): FinanceTransactionDto {
        // 1. Validation
        if (amount <= 0.0) {
            throw IllegalArgumentException("Le montant doit être strictement supérieur à 0.")
        }

        val category = TransactionCategory.valueOf(categoryName)
        val validCategories = listOf(TransactionCategory.CLEANING, TransactionCategory.FUEL,
            TransactionCategory.SECURITY, TransactionCategory.MAINTENANCE, TransactionCategory.TAXES,
            TransactionCategory.OTHER)
        if (!validCategories.contains(category)) {
            throw IllegalArgumentException("Catégorie invalide. Choix possibles : $validCategories")
        }

        val dbResidence = Residence.findById(residenceId) ?: throw Exception("Résidence introuvable.")

        // 2. Insertion
        val tx = FinancialTransaction.new {
            this.residence = dbResidence
            this.type = TransactionType.EXPENSE
            this.category = category
            this.amount = amount
            this.description = description
            this.relatedEntityType = null
            this.relatedEntityId = null
            this.transactionDate = date
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }
        tx.flush()

        return FinanceTransactionDto(
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

    fun getTransactions(
        residenceId: UUID,
        typeParam: String?,
        categoryParam: String?,
        startDateParam: String?,
        endDateParam: String?,
        queryParam: String?
    ): List<FinanceTransactionDto> {
        var query = FinancialTransactions.select(FinancialTransactions.columns)
            .where { FinancialTransactions.residenceId eq residenceId }

        // Filter by flow type (INCOME or EXPENSE)
        typeParam?.ifBlank { null }?.let { TransactionType.valueOf(it.uppercase()) }?.let { type ->
            if (type == TransactionType.INCOME || type == TransactionType.EXPENSE) {
                query = query.andWhere { FinancialTransactions.type eq type }
            }
        }

        // Filter by category
        categoryParam?.ifBlank { null }?.let { TransactionCategory.valueOf(it.uppercase()) }?.let { cat ->
            query = query.andWhere { FinancialTransactions.category eq cat }
        }

        // Filter by date range
        if (!startDateParam.isNullOrBlank() && !endDateParam.isNullOrBlank()) {
            val start = LocalDate.parse(startDateParam)
            val end = LocalDate.parse(endDateParam)
            query = query.andWhere { FinancialTransactions.transactionDate.between(start, end) }
        }

        // Search in description
        queryParam?.ifBlank { null }?.let { text ->
            query = query.andWhere { FinancialTransactions.description.like("%$text%") }
        }

        // Sort by transactionDate descending
        query = query.orderBy(FinancialTransactions.transactionDate to SortOrder.DESC)

        return query.map { row ->
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
}
