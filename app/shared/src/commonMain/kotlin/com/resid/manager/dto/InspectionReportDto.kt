package com.resid.manager.dto

import kotlinx.serialization.Serializable

@Serializable
enum class InspectionTypeDto {
    MOVE_IN,
    MOVE_OUT
}

@Serializable
enum class ConditionRatingDto {
    NEW,
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    BROKEN,
    MISSING
}

@Serializable
enum class InspectionItemCategoryDto {
    ROOM_LIVING_ROOM,
    ROOM_BEDROOM,
    ROOM_KITCHEN,
    ROOM_BATHROOM,
    ROOM_BALCONY,
    ROOM_CORRIDOR,
    FURNITURE,
    APPLIANCE,
    UTENSIL,
    OTHER
}

@Serializable
data class InspectionReportItemDto(
    val id: String? = null,
    val category: InspectionItemCategoryDto,
    val name: String,
    val condition: ConditionRatingDto,
    val quantity: Int = 1,
    val observations: String? = null,
    val photoUrls: List<String> = emptyList()
)

@Serializable
data class InspectionReportDto(
    val id: String,
    val leaseId: String,
    val inspectorId: String,
    val type: InspectionTypeDto,
    val inspectionDate: String,
    val generalCondition: ConditionRatingDto,
    val electricityMeterIndex: Double,
    val waterMeterIndex: Double? = null,
    val gasMeterIndex: Double? = null,
    val keysCount: Int = 1,
    val comments: String? = null,
    val photoUrls: List<String> = emptyList(),
    val items: List<InspectionReportItemDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class InspectionReportCreateRequest(
    val leaseId: String,
    val type: InspectionTypeDto,
    val inspectionDate: String,
    val generalCondition: ConditionRatingDto = ConditionRatingDto.GOOD,
    val electricityMeterIndex: Double,
    val waterMeterIndex: Double? = null,
    val gasMeterIndex: Double? = null,
    val keysCount: Int = 1,
    val comments: String? = null,
    val photoUrls: List<String> = emptyList(),
    val items: List<InspectionReportItemDto> = emptyList()
)

@Serializable
data class InspectionReportUpdateRequest(
    val inspectionDate: String? = null,
    val generalCondition: ConditionRatingDto? = null,
    val electricityMeterIndex: Double? = null,
    val waterMeterIndex: Double? = null,
    val gasMeterIndex: Double? = null,
    val keysCount: Int? = null,
    val comments: String? = null,
    val photoUrls: List<String>? = null,
    val items: List<InspectionReportItemDto>? = null
)
