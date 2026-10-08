package com.resid.manager.features.profile.data

import com.resid.manager.dto.CurrencyCodeDto
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.ResidenceCreateRequest
import com.resid.manager.dto.TicketCategoryDto
import com.resid.manager.dto.TicketCategoryRequest
import com.resid.manager.dto.UserDto
import com.resid.manager.dto.UserUpdateRequest
import com.resid.manager.network.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

interface ProfileRepository {
    suspend fun updateProfile(token: String, request: UserUpdateRequest): Result<UserDto>
    suspend fun fetchCategories(token: String, residenceId: String): Result<List<TicketCategoryDto>>
    suspend fun addCategory(token: String, residenceId: String, key: String, label: String): Result<TicketCategoryDto>
    suspend fun updateCategory(token: String, categoryId: String, label: String): Result<TicketCategoryDto>
    suspend fun updateCurrency(token: String, residenceId: String, currencyCode: String): Result<Unit>
}

class ProfileRepositoryImpl : ProfileRepository {
    override suspend fun updateProfile(token: String, request: UserUpdateRequest): Result<UserDto> {
        return try {
            val response = ApiClient.httpClient.put("${ApiClient.BASE_URL}/api/users/me") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val err = response.body<ErrorResponse>()
                Result.failure(Exception(err.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchCategories(token: String, residenceId: String): Result<List<TicketCategoryDto>> {
        return try {
            val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/residences/$residenceId/ticket-categories") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val err = response.body<ErrorResponse>()
                Result.failure(Exception(err.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCategory(
        token: String,
        residenceId: String,
        key: String,
        label: String
    ): Result<TicketCategoryDto> {
        return try {
            val response = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/residences/$residenceId/ticket-categories") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(TicketCategoryRequest(key, label))
            }
            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val err = response.body<ErrorResponse>()
                Result.failure(Exception(err.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(
        token: String,
        categoryId: String,
        label: String
    ): Result<TicketCategoryDto> {
        return try {
            val response = ApiClient.httpClient.put("${ApiClient.BASE_URL}/api/ticket-categories/$categoryId") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(TicketCategoryRequest(key = "", label = label))
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                val err = response.body<ErrorResponse>()
                Result.failure(Exception(err.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCurrency(token: String, residenceId: String, currencyCode: String): Result<Unit> {
        return try {
            val curr = try {
                CurrencyCodeDto.valueOf(currencyCode)
            } catch (_: Exception) {
                CurrencyCodeDto.XOF
            }
            val req = ResidenceCreateRequest(
                name = "",
                address = "",
                currency = curr,
                kWhPrice = 0.0
            )
            val response = ApiClient.httpClient.put("${ApiClient.BASE_URL}/api/residences/$residenceId/currency") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(req)
            }
            if (response.status == HttpStatusCode.OK) {
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
