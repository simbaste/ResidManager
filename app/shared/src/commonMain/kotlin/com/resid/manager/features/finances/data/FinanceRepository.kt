package com.resid.manager.features.finances.data

import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.ExpenseRecordRequest
import com.resid.manager.dto.FinanceTransactionDto
import com.resid.manager.network.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

interface FinanceRepository {
    suspend fun fetchTransactions(
        token: String,
        residenceId: String,
        type: String,
        category: String,
        startDate: String,
        endDate: String,
        query: String
    ): Result<List<FinanceTransactionDto>>

    suspend fun recordExpense(
        token: String,
        residenceId: String,
        request: ExpenseRecordRequest
    ): Result<Unit>
}

class FinanceRepositoryImpl : FinanceRepository {
    override suspend fun fetchTransactions(
        token: String,
        residenceId: String,
        type: String,
        category: String,
        startDate: String,
        endDate: String,
        query: String
    ): Result<List<FinanceTransactionDto>> {
        return try {
            val url = "${ApiClient.BASE_URL}/api/residences/$residenceId/transactions?" +
                    "type=${if (type != "ALL") type else ""}&" +
                    "category=${if (category != "ALL") category else ""}&" +
                    "start_date=$startDate&" +
                    "end_date=$endDate&" +
                    "q=$query"

            val response = ApiClient.httpClient.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                val raw: List<FinanceTransactionDto> = response.body()
                val sorted = raw.sortedWith(
                    compareByDescending<FinanceTransactionDto> { it.transactionDate }
                        .thenByDescending { it.createdAt }
                )
                Result.success(sorted)
            } else {
                val err = response.body<ErrorResponse>()
                Result.failure(Exception(err.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordExpense(
        token: String,
        residenceId: String,
        request: ExpenseRecordRequest
    ): Result<Unit> {
        return try {
            val response = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/residences/$residenceId/transactions") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }
            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(Unit)
            } else {
                val err = response.body<ErrorResponse>()
                Result.failure(Exception(err.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
