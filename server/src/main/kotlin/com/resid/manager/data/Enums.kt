package com.resid.manager.data

import com.resid.manager.dto.ApplicationStatusDto
import com.resid.manager.dto.ConditionRatingDto
import com.resid.manager.dto.CurrencyCodeDto
import com.resid.manager.dto.CurrencySymbolDto
import com.resid.manager.dto.ElectricityStatusDto
import com.resid.manager.dto.EntityTypeDto
import com.resid.manager.dto.InspectionItemCategoryDto
import com.resid.manager.dto.InspectionTypeDto
import com.resid.manager.dto.InvitationStatusDto
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.dto.PaymentFrequencyDto
import com.resid.manager.dto.RoleDto
import com.resid.manager.dto.TicketStatusDto
import com.resid.manager.dto.TicketUrgencyDto
import com.resid.manager.dto.TransactionCategoryDto
import com.resid.manager.dto.TransactionTypeDto
import com.resid.manager.dto.UnitStatusDto

const val ENUM_NAME_COLUMN_LENGTH = 20

enum class Role {
    OWNER, ADMIN, MANAGER, STAFF, TENANT
}

fun Role.convert(): RoleDto {
    return when (this) {
        Role.OWNER -> RoleDto.OWNER
        Role.ADMIN -> RoleDto.ADMIN
        Role.MANAGER -> RoleDto.MANAGER
        Role.STAFF -> RoleDto.STAFF
        Role.TENANT -> RoleDto.TENANT
    }
}

fun RoleDto.convert(): Role {
    return when (this) {
        RoleDto.OWNER -> Role.OWNER
        RoleDto.ADMIN -> Role.ADMIN
        RoleDto.MANAGER -> Role.MANAGER
        RoleDto.STAFF -> Role.STAFF
        RoleDto.TENANT -> Role.TENANT
    }
}

fun Role.isOwner(): Boolean {
    return this == Role.OWNER
}

fun Role.isAdmin(): Boolean {
    return this == Role.OWNER || this == Role.ADMIN
}

fun Role.isManager(): Boolean {
    return this == Role.OWNER || this == Role.ADMIN || this == Role.MANAGER
}

enum class InvitationStatus {
    PENDING_APPROVAL, INVITED, ACCEPTED, DECLINED
}

fun InvitationStatus.convert(): InvitationStatusDto {
    return when (this) {
        InvitationStatus.PENDING_APPROVAL -> InvitationStatusDto.PENDING_APPROVAL
        InvitationStatus.INVITED -> InvitationStatusDto.INVITED
        InvitationStatus.ACCEPTED -> InvitationStatusDto.ACCEPTED
        InvitationStatus.DECLINED -> InvitationStatusDto.DECLINED
    }
}

fun InvitationStatusDto.convert(): InvitationStatus {
    return when (this) {
        InvitationStatusDto.PENDING_APPROVAL -> InvitationStatus.PENDING_APPROVAL
        InvitationStatusDto.INVITED -> InvitationStatus.INVITED
        InvitationStatusDto.ACCEPTED -> InvitationStatus.ACCEPTED
        InvitationStatusDto.DECLINED -> InvitationStatus.DECLINED
    }
}

enum class ApplicationStatus {
    PENDING, APPROVED, REJECTED
}

fun ApplicationStatus.convert(): ApplicationStatusDto {
    return when (this) {
        ApplicationStatus.PENDING -> ApplicationStatusDto.PENDING
        ApplicationStatus.APPROVED -> ApplicationStatusDto.APPROVED
        ApplicationStatus.REJECTED -> ApplicationStatusDto.REJECTED
    }
}

fun ApplicationStatusDto.convert(): ApplicationStatus {
    return when (this) {
        ApplicationStatusDto.PENDING -> ApplicationStatus.PENDING
        ApplicationStatusDto.APPROVED -> ApplicationStatus.APPROVED
        ApplicationStatusDto.REJECTED -> ApplicationStatus.REJECTED
    }
}

enum class UnitStatus {
    AVAILABLE, OCCUPIED, RESERVED
}

fun UnitStatus.convert(): UnitStatusDto {
    return when (this) {
        UnitStatus.AVAILABLE -> UnitStatusDto.AVAILABLE
        UnitStatus.OCCUPIED -> UnitStatusDto.OCCUPIED
        UnitStatus.RESERVED -> UnitStatusDto.RESERVED
    }
}

fun UnitStatusDto.convert(): UnitStatus {
    return when (this) {
        UnitStatusDto.AVAILABLE -> UnitStatus.AVAILABLE
        UnitStatusDto.OCCUPIED -> UnitStatus.OCCUPIED
        UnitStatusDto.RESERVED -> UnitStatus.RESERVED
    }
}

enum class PaymentFrequency {
    MONTHLY, ANNUAL
}

fun PaymentFrequency.convert(): PaymentFrequencyDto {
    return when (this) {
        PaymentFrequency.MONTHLY -> PaymentFrequencyDto.MONTHLY
        PaymentFrequency.ANNUAL -> PaymentFrequencyDto.ANNUAL
    }
}

enum class DepositStatus {
    PENDING, PAID
}

enum class LeaseStatus {
    PENDING_PAYMENT, DOWN_PAYMENT_PAID, PARTIALLY_PAID, PENDING_SIGNATURE, SIGNED_ACTIVE, TERMINATED
}

fun LeaseStatus.convert(): LeaseStatusDto {
    return when (this) {
        LeaseStatus.PENDING_PAYMENT -> LeaseStatusDto.PENDING_PAYMENT
        LeaseStatus.DOWN_PAYMENT_PAID -> LeaseStatusDto.DOWN_PAYMENT_PAID
        LeaseStatus.PARTIALLY_PAID -> LeaseStatusDto.PARTIALLY_PAID
        LeaseStatus.PENDING_SIGNATURE -> LeaseStatusDto.PENDING_SIGNATURE
        LeaseStatus.SIGNED_ACTIVE -> LeaseStatusDto.SIGNED_ACTIVE
        LeaseStatus.TERMINATED -> LeaseStatusDto.TERMINATED
    }
}

fun LeaseStatusDto.convert(): LeaseStatus {
    return when (this) {
        LeaseStatusDto.PENDING_PAYMENT -> LeaseStatus.PENDING_PAYMENT
        LeaseStatusDto.DOWN_PAYMENT_PAID -> LeaseStatus.DOWN_PAYMENT_PAID
        LeaseStatusDto.PARTIALLY_PAID -> LeaseStatus.PARTIALLY_PAID
        LeaseStatusDto.PENDING_SIGNATURE -> LeaseStatus.PENDING_SIGNATURE
        LeaseStatusDto.SIGNED_ACTIVE -> LeaseStatus.SIGNED_ACTIVE
        LeaseStatusDto.TERMINATED -> LeaseStatus.TERMINATED
    }
}

enum class ElectricityStatus {
    UNPAID, PAID
}

fun ElectricityStatus.convert(): ElectricityStatusDto {
    return when (this) {
        ElectricityStatus.UNPAID -> ElectricityStatusDto.UNPAID
        ElectricityStatus.PAID -> ElectricityStatusDto.PAID
    }
}

fun ElectricityStatusDto.convert(): ElectricityStatus {
    return when (this) {
        ElectricityStatusDto.UNPAID -> ElectricityStatus.UNPAID
        ElectricityStatusDto.PAID -> ElectricityStatus.PAID
    }
}

enum class TicketUrgency {
    LOW, MEDIUM, CRITICAL
}

fun TicketUrgency.convert(): TicketUrgencyDto {
    return when (this) {
        TicketUrgency.LOW -> TicketUrgencyDto.LOW
        TicketUrgency.MEDIUM -> TicketUrgencyDto.MEDIUM
        TicketUrgency.CRITICAL -> TicketUrgencyDto.CRITICAL
    }
}

fun TicketUrgencyDto.convert(): TicketUrgency {
    return when (this) {
        TicketUrgencyDto.LOW -> TicketUrgency.LOW
        TicketUrgencyDto.MEDIUM -> TicketUrgency.MEDIUM
        TicketUrgencyDto.CRITICAL -> TicketUrgency.CRITICAL
    }
}

enum class TicketStatus {
    OPEN, IN_PROGRESS, CLOSED
}

fun TicketStatus.convert(): TicketStatusDto {
    return when (this) {
        TicketStatus.OPEN -> TicketStatusDto.OPEN
        TicketStatus.IN_PROGRESS -> TicketStatusDto.IN_PROGRESS
        TicketStatus.CLOSED -> TicketStatusDto.CLOSED
    }
}

fun TicketStatusDto.convert(): TicketStatus {
    return when (this) {
        TicketStatusDto.OPEN -> TicketStatus.OPEN
        TicketStatusDto.IN_PROGRESS -> TicketStatus.IN_PROGRESS
        TicketStatusDto.CLOSED -> TicketStatus.CLOSED
    }
}

enum class TransactionType {
    INCOME, EXPENSE
}

fun TransactionType.convert(): TransactionTypeDto {
    return when (this) {
        TransactionType.INCOME -> TransactionTypeDto.INCOME
        TransactionType.EXPENSE -> TransactionTypeDto.EXPENSE
    }
}

enum class TransactionCategory {
    RENT, DEPOSIT, LEASE_PAYMENT, ELECTRICITY, MAINTENANCE, CLEANING, FUEL, SECURITY, TAXES, OTHER
}

fun TransactionCategory.convert(): TransactionCategoryDto {
    return when (this) {
        TransactionCategory.RENT -> TransactionCategoryDto.RENT
        TransactionCategory.DEPOSIT -> TransactionCategoryDto.DEPOSIT
        TransactionCategory.LEASE_PAYMENT -> TransactionCategoryDto.LEASE_PAYMENT
        TransactionCategory.ELECTRICITY -> TransactionCategoryDto.ELECTRICITY
        TransactionCategory.MAINTENANCE -> TransactionCategoryDto.MAINTENANCE
        TransactionCategory.CLEANING -> TransactionCategoryDto.CLEANING
        TransactionCategory.FUEL -> TransactionCategoryDto.FUEL
        TransactionCategory.SECURITY -> TransactionCategoryDto.SECURITY
        TransactionCategory.TAXES -> TransactionCategoryDto.TAXES
        TransactionCategory.OTHER -> TransactionCategoryDto.OTHER
    }
}

fun TransactionCategoryDto.convert(): TransactionCategory {
    return when (this) {
        TransactionCategoryDto.RENT -> TransactionCategory.RENT
        TransactionCategoryDto.DEPOSIT -> TransactionCategory.DEPOSIT
        TransactionCategoryDto.LEASE_PAYMENT -> TransactionCategory.LEASE_PAYMENT
        TransactionCategoryDto.ELECTRICITY -> TransactionCategory.ELECTRICITY
        TransactionCategoryDto.MAINTENANCE -> TransactionCategory.MAINTENANCE
        TransactionCategoryDto.CLEANING -> TransactionCategory.CLEANING
        TransactionCategoryDto.FUEL -> TransactionCategory.FUEL
        TransactionCategoryDto.SECURITY -> TransactionCategory.SECURITY
        TransactionCategoryDto.TAXES -> TransactionCategory.TAXES
        TransactionCategoryDto.OTHER -> TransactionCategory.OTHER
    }
}

enum class EntityType {
    BAIL, ELECTRICITY_STATEMENT, TICKET
}

fun EntityType.convert(): EntityTypeDto {
    return when (this) {
        EntityType.BAIL -> EntityTypeDto.BAIL
        EntityType.ELECTRICITY_STATEMENT -> EntityTypeDto.ELECTRICITY_STATEMENT
        EntityType.TICKET -> EntityTypeDto.TICKET
    }
}

enum class CurrencyCode {
    XOF,
    EUR,
    USD
}

fun CurrencyCode.convert(): CurrencyCodeDto {
    return when (this) {
        CurrencyCode.XOF -> CurrencyCodeDto.XOF
        CurrencyCode.EUR -> CurrencyCodeDto.EUR
        CurrencyCode.USD -> CurrencyCodeDto.USD
    }
}

enum class InspectionType {
    MOVE_IN,
    MOVE_OUT
}

fun InspectionType.convert(): InspectionTypeDto {
    return when (this) {
        InspectionType.MOVE_IN -> InspectionTypeDto.MOVE_IN
        InspectionType.MOVE_OUT -> InspectionTypeDto.MOVE_OUT
    }
}

fun InspectionTypeDto.convert(): InspectionType {
    return when (this) {
        InspectionTypeDto.MOVE_IN -> InspectionType.MOVE_IN
        InspectionTypeDto.MOVE_OUT -> InspectionType.MOVE_OUT
    }
}

enum class ConditionRating {
    NEW,
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    BROKEN,
    MISSING
}

fun ConditionRating.convert(): ConditionRatingDto {
    return when (this) {
        ConditionRating.NEW -> ConditionRatingDto.NEW
        ConditionRating.EXCELLENT -> ConditionRatingDto.EXCELLENT
        ConditionRating.GOOD -> ConditionRatingDto.GOOD
        ConditionRating.FAIR -> ConditionRatingDto.FAIR
        ConditionRating.POOR -> ConditionRatingDto.POOR
        ConditionRating.BROKEN -> ConditionRatingDto.BROKEN
        ConditionRating.MISSING -> ConditionRatingDto.MISSING
    }
}

fun ConditionRatingDto.convert(): ConditionRating {
    return when (this) {
        ConditionRatingDto.NEW -> ConditionRating.NEW
        ConditionRatingDto.EXCELLENT -> ConditionRating.EXCELLENT
        ConditionRatingDto.GOOD -> ConditionRating.GOOD
        ConditionRatingDto.FAIR -> ConditionRating.FAIR
        ConditionRatingDto.POOR -> ConditionRating.POOR
        ConditionRatingDto.BROKEN -> ConditionRating.BROKEN
        ConditionRatingDto.MISSING -> ConditionRating.MISSING
    }
}

enum class InspectionItemCategory {
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

fun InspectionItemCategory.convert(): InspectionItemCategoryDto {
    return when (this) {
        InspectionItemCategory.ROOM_LIVING_ROOM -> InspectionItemCategoryDto.ROOM_LIVING_ROOM
        InspectionItemCategory.ROOM_BEDROOM -> InspectionItemCategoryDto.ROOM_BEDROOM
        InspectionItemCategory.ROOM_KITCHEN -> InspectionItemCategoryDto.ROOM_KITCHEN
        InspectionItemCategory.ROOM_BATHROOM -> InspectionItemCategoryDto.ROOM_BATHROOM
        InspectionItemCategory.ROOM_BALCONY -> InspectionItemCategoryDto.ROOM_BALCONY
        InspectionItemCategory.ROOM_CORRIDOR -> InspectionItemCategoryDto.ROOM_CORRIDOR
        InspectionItemCategory.FURNITURE -> InspectionItemCategoryDto.FURNITURE
        InspectionItemCategory.APPLIANCE -> InspectionItemCategoryDto.APPLIANCE
        InspectionItemCategory.UTENSIL -> InspectionItemCategoryDto.UTENSIL
        InspectionItemCategory.OTHER -> InspectionItemCategoryDto.OTHER
    }
}

fun InspectionItemCategoryDto.convert(): InspectionItemCategory {
    return when (this) {
        InspectionItemCategoryDto.ROOM_LIVING_ROOM -> InspectionItemCategory.ROOM_LIVING_ROOM
        InspectionItemCategoryDto.ROOM_BEDROOM -> InspectionItemCategory.ROOM_BEDROOM
        InspectionItemCategoryDto.ROOM_KITCHEN -> InspectionItemCategory.ROOM_KITCHEN
        InspectionItemCategoryDto.ROOM_BATHROOM -> InspectionItemCategory.ROOM_BATHROOM
        InspectionItemCategoryDto.ROOM_BALCONY -> InspectionItemCategory.ROOM_BALCONY
        InspectionItemCategoryDto.ROOM_CORRIDOR -> InspectionItemCategory.ROOM_CORRIDOR
        InspectionItemCategoryDto.FURNITURE -> InspectionItemCategory.FURNITURE
        InspectionItemCategoryDto.APPLIANCE -> InspectionItemCategory.APPLIANCE
        InspectionItemCategoryDto.UTENSIL -> InspectionItemCategory.UTENSIL
        InspectionItemCategoryDto.OTHER -> InspectionItemCategory.OTHER
    }
}

fun CurrencyCodeDto.convert(): CurrencyCode {
    return when (this) {
        CurrencyCodeDto.XOF -> CurrencyCode.XOF
        CurrencyCodeDto.EUR -> CurrencyCode.EUR
        CurrencyCodeDto.USD -> CurrencyCode.USD
    }
}

enum class CurrencySymbol {
    FRANC_CFA,
    EURO,
    DOLLAR_US
}

fun CurrencySymbol.convert(): CurrencySymbolDto {
    return when (this) {
        CurrencySymbol.FRANC_CFA -> CurrencySymbolDto.FRANC_CFA
        CurrencySymbol.EURO -> CurrencySymbolDto.EURO
        CurrencySymbol.DOLLAR_US -> CurrencySymbolDto.DOLLAR_US
    }
}

fun CurrencySymbolDto.convert(): CurrencySymbol {
    return when (this) {
        CurrencySymbolDto.FRANC_CFA -> CurrencySymbol.FRANC_CFA
        CurrencySymbolDto.EURO -> CurrencySymbol.EURO
        CurrencySymbolDto.DOLLAR_US -> CurrencySymbol.DOLLAR_US
    }
}