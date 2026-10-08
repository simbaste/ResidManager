package com.resid.manager.features.finances.usecase

import com.resid.manager.dto.FinanceTransactionDto
import com.resid.manager.features.finances.data.FinanceRepository

class FetchTransactionsUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(
        token: String,
        residenceId: String,
        type: String,
        category: String,
        startDate: String,
        endDate: String,
        query: String
    ): Result<List<FinanceTransactionDto>> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de résidence manquant"))
        }
        return financeRepository.fetchTransactions(token, residenceId, type, category, startDate, endDate, query)
    }
}
