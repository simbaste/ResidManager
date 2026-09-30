package com.resid.manager.features.finances.usecase

import com.resid.manager.dto.ExpenseRecordRequest
import com.resid.manager.dto.TransactionCategoryDto
import com.resid.manager.features.finances.data.FinanceRepository

class RecordExpenseUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(
        token: String,
        residenceId: String,
        category: TransactionCategoryDto,
        amount: Double,
        description: String,
        transactionDate: String
    ): Result<Unit> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres obligatoires manquants"))
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Le montant de la dépense doit être supérieur à zéro"))
        }
        if (description.isBlank()) {
            return Result.failure(IllegalArgumentException("La description de la dépense est obligatoire"))
        }
        if (transactionDate.isBlank()) {
            return Result.failure(IllegalArgumentException("La date de l'opération est obligatoire"))
        }

        val request = ExpenseRecordRequest(
            category = category,
            amount = amount,
            description = description.trim(),
            transactionDate = transactionDate.trim()
        )
        return financeRepository.recordExpense(token, residenceId, request)
    }
}
