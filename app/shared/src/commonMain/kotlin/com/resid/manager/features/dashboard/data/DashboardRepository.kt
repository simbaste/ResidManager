package com.resid.manager.features.dashboard.data

import com.resid.manager.dto.DashboardDataDto
import com.resid.manager.network.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

interface DashboardRepository {
    suspend fun fetchDashboardData(
        token: String,
        residenceId: String,
        periodFilter: String,
        startDate: String,
        endDate: String
    ): Result<DashboardDataDto>
}

class DashboardRepositoryImpl : DashboardRepository {
    override suspend fun fetchDashboardData(
        token: String,
        residenceId: String,
        periodFilter: String,
        startDate: String,
        endDate: String
    ): Result<DashboardDataDto> {
        return try {
            val url = "${ApiClient.BASE_URL}/api/residences/$residenceId/dashboard?filter=$periodFilter&start_date=$startDate&end_date=$endDate"
            val response = ApiClient.httpClient.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<DashboardDataDto>())
            } else {
                Result.failure(Exception("Erreur serveur : code ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
