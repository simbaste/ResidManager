package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
data class LeaseDraftInitRequest(
    val residenceUnitId: String
)

@Serializable
data class LeaseDraftStepUpdateRequest(
    val step: String, // TENANT, GUARANTOR, UNIT, FINANCIALS, TIMELINE, INVENTORY
    val tenantId: String? = null,
    val inlineTenant: InlineTenantCreateRequest? = null,
    val guarantorId: String? = null,
    val guarantor: GuarantorDto? = null,
    val depositAmount: Double? = null,
    val paymentFrequency: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val advanceMonths: Int? = null,
    val advancePaymentAmount: Double? = null
)
