package com.resid.manager.data

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.UUID

// =========================================================================
// SECTION 1: Exposed DSL Table Definitions
// =========================================================================

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val birthDate = date("birth_date").nullable()
    val phone = varchar("phone", 20).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object Currencies : UUIDTable("currencies") {
    val code = enumerationByName("code", 10, CurrencyCode::class)
    val symbol = enumerationByName("symbol", 10, CurrencySymbol::class)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object Residences : UUIDTable("residences") {
    val name = varchar("name", 255).uniqueIndex()
    val address = text("address")
    val photoUrl = text("photo_url").nullable()
    val currencyId = reference("currency_id", Currencies, onDelete = ReferenceOption.RESTRICT)
    val kWhPrice = double("kwh_price")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object ResidenceMembers : Table("residence_members") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val residenceId = reference("residence_id", Residences, onDelete = ReferenceOption.CASCADE)
    val role = enumerationByName("role", ENUM_NAME_COLUMN_LENGTH, Role::class)
    val status = enumerationByName("status", ENUM_NAME_COLUMN_LENGTH, InvitationStatus::class)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    override val primaryKey = PrimaryKey(userId, residenceId)
}

object ResidenceApplications: Table("residence_applications") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val residenceId = reference("residence_id", Residences, onDelete = ReferenceOption.CASCADE)
    val role = enumerationByName("role", ENUM_NAME_COLUMN_LENGTH, Role::class)
    val status = enumerationByName("status", ENUM_NAME_COLUMN_LENGTH, ApplicationStatus::class)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    override val primaryKey = PrimaryKey(userId, residenceId)
}

object ResidenceUnits : UUIDTable("residence_units") {
    val residenceId = reference("residence_id", Residences, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val floor = varchar("floor", 50)
    val type = varchar("type", 20) // Room, Studio, T2, etc.
    val nominalRent = double("nominal_rent")
    val serviceCharges = double("service_charges")
    val initialElectricityIndex = double("initial_electricity_index")
    val status = enumerationByName("status", ENUM_NAME_COLUMN_LENGTH, UnitStatus::class).default(UnitStatus.AVAILABLE)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object Leases : UUIDTable("leases") {
    val residenceUnitId = reference("residence_unit_id", ResidenceUnits, onDelete = ReferenceOption.RESTRICT)
    val tenantId = reference("tenant_id", Users, onDelete = ReferenceOption.RESTRICT)
    val durationMonths = integer("duration_months")
    val paymentFrequencyDto = enumerationByName("payment_frequency", ENUM_NAME_COLUMN_LENGTH, PaymentFrequency::class)
    val depositAmount = double("deposit_amount")
    val depositStatusDto = enumerationByName("deposit_status", ENUM_NAME_COLUMN_LENGTH, DepositStatus::class).default(DepositStatus.PENDING)
    val status = enumerationByName("status", ENUM_NAME_COLUMN_LENGTH, LeaseStatus::class).default(LeaseStatus.PENDING_PAYMENT) // PENDING_PAYMENT, PARTIALLY_PAID, PENDING_SIGNATURE, ACTIVE, TERMINATED
    val startDate = date("start_date")
    val endDate = date("end_date")
    val advanceMonths = integer("advance_months").default(1)
    val advancePaymentAmount = double("advance_payment_amount").default(0.0)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object ElectricityStatements : UUIDTable("electricity_statements") {
    val residenceUnitId = reference("residence_unit_id", ResidenceUnits, onDelete = ReferenceOption.CASCADE)
    val oldIndex = double("old_index")
    val newIndex = double("new_index")
    val kWhPriceApplied = double("kwh_price_applied")
    val amountDue = double("amount_due")
    val status = enumerationByName("status", ENUM_NAME_COLUMN_LENGTH, ElectricityStatus::class).default(ElectricityStatus.UNPAID)
    val statementDate = date("statement_date")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object TicketCategories : UUIDTable("ticket_categories") {
    val residenceId = reference("residence_id", Residences, onDelete = ReferenceOption.CASCADE).nullable()
    val key = varchar("key", 50).uniqueIndex()
    val label = varchar("label", 100)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object Tickets : UUIDTable("tickets") {
    val residenceUnitId = reference("residence_unit_id", ResidenceUnits, onDelete = ReferenceOption.CASCADE)
    val creatorId = reference("creator_id", Users, onDelete = ReferenceOption.CASCADE)
    val categoryId = reference("category_id", TicketCategories, onDelete = ReferenceOption.RESTRICT)
    val title = varchar("title", 255)
    val description = text("description")
    val urgency = enumerationByName("urgency", ENUM_NAME_COLUMN_LENGTH, TicketUrgency::class)
    val status = enumerationByName("status", ENUM_NAME_COLUMN_LENGTH, TicketStatus::class).default(TicketStatus.OPEN)
    val interventionCost = double("intervention_cost").default(0.0)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object FinancialTransactions : UUIDTable("financial_transactions") {
    val residenceId = reference("residence_id", Residences, onDelete = ReferenceOption.CASCADE)
    val type = enumerationByName("type", ENUM_NAME_COLUMN_LENGTH, TransactionType::class)
    val category = enumerationByName("category", ENUM_NAME_COLUMN_LENGTH, TransactionCategory::class)
    val amount = double("amount")
    val description = text("description")
    val relatedEntityType = enumerationByName("related_entity_type", ENUM_NAME_COLUMN_LENGTH, EntityType::class).nullable()
    val relatedEntityId = uuid("related_entity_id").nullable()
    val transactionDate = date("transaction_date")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object Equipments : UUIDTable("equipments") {
    val key = varchar("key", 50).uniqueIndex()
    val label = varchar("label", 100)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object ResidenceUnitEquipments : Table("residence_unit_equipments") {
    val residenceUnitId = reference("residence_unit_id", ResidenceUnits, onDelete = ReferenceOption.CASCADE)
    val equipmentId = reference("equipment_id", Equipments, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(residenceUnitId, equipmentId)
}


// =========================================================================
// SECTION 2: JetBrains Exposed DAO Entity Classes
// =========================================================================

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var email by Users.email
    var passwordHash by Users.passwordHash
    var firstName by Users.firstName
    var lastName by Users.lastName
    var birthDate by Users.birthDate
    var phone by Users.phone
    var createdAt by Users.createdAt
    var updatedAt by Users.updatedAt
}

class CurrencyEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CurrencyEntity>(Currencies)

    var code by Currencies.code
    var symbol by Currencies.symbol
    var createdAt by Currencies.createdAt
    var updatedAt by Currencies.updatedAt
}

class Residence(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Residence>(Residences)

    var name by Residences.name
    var address by Residences.address
    var photoUrl by Residences.photoUrl
    var currency by CurrencyEntity referencedOn Residences.currencyId
    var kWhPrice by Residences.kWhPrice
    var createdAt by Residences.createdAt
    var updatedAt by Residences.updatedAt
}

class ResidenceUnit(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ResidenceUnit>(ResidenceUnits)

    var residence by Residence referencedOn ResidenceUnits.residenceId
    var name by ResidenceUnits.name
    var floor by ResidenceUnits.floor
    var type by ResidenceUnits.type
    var nominalRent by ResidenceUnits.nominalRent
    var serviceCharges by ResidenceUnits.serviceCharges
    var initialElectricityIndex by ResidenceUnits.initialElectricityIndex
    var status by ResidenceUnits.status
    var createdAt by ResidenceUnits.createdAt
    var updatedAt by ResidenceUnits.updatedAt

    var equipments by Equipment via ResidenceUnitEquipments
}

class Equipment(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Equipment>(Equipments)

    var key by Equipments.key
    var label by Equipments.label
    var createdAt by Equipments.createdAt
    var updatedAt by Equipments.updatedAt
}

class Lease(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Lease>(Leases)

    var residenceUnit by ResidenceUnit referencedOn Leases.residenceUnitId
    var tenant by User referencedOn Leases.tenantId
    var durationMonths by Leases.durationMonths
    var paymentFrequency by Leases.paymentFrequencyDto
    var depositAmount by Leases.depositAmount
    var depositStatus by Leases.depositStatusDto
    var status by Leases.status
    var startDate by Leases.startDate
    var endDate by Leases.endDate
    var advanceMonths by Leases.advanceMonths
    var advancePaymentAmount by Leases.advancePaymentAmount
    var createdAt by Leases.createdAt
    var updatedAt by Leases.updatedAt
}

class ElectricityStatement(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ElectricityStatement>(ElectricityStatements)

    var residenceUnit by ResidenceUnit referencedOn ElectricityStatements.residenceUnitId
    var oldIndex by ElectricityStatements.oldIndex
    var newIndex by ElectricityStatements.newIndex
    var kWhPriceApplied by ElectricityStatements.kWhPriceApplied
    var amountDue by ElectricityStatements.amountDue
    var status by ElectricityStatements.status
    var statementDate by ElectricityStatements.statementDate
    var createdAt by ElectricityStatements.createdAt
    var updatedAt by ElectricityStatements.updatedAt
}

class TicketCategoryEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TicketCategoryEntity>(TicketCategories)

    var residence by Residence optionalReferencedOn TicketCategories.residenceId
    var key by TicketCategories.key
    var label by TicketCategories.label
    var createdAt by TicketCategories.createdAt
    var updatedAt by TicketCategories.updatedAt
}

class Ticket(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Ticket>(Tickets)

    var residenceUnit by ResidenceUnit referencedOn Tickets.residenceUnitId
    var creator by User referencedOn Tickets.creatorId
    var category by TicketCategoryEntity referencedOn Tickets.categoryId
    var title by Tickets.title
    var description by Tickets.description
    var urgency by Tickets.urgency
    var status by Tickets.status
    var interventionCost by Tickets.interventionCost
    var createdAt by Tickets.createdAt
    var updatedAt by Tickets.updatedAt
}

class FinancialTransaction(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<FinancialTransaction>(FinancialTransactions)

    var residence by Residence referencedOn FinancialTransactions.residenceId
    var type by FinancialTransactions.type
    var category by FinancialTransactions.category
    var amount by FinancialTransactions.amount
    var description by FinancialTransactions.description
    var relatedEntityType by FinancialTransactions.relatedEntityType
    var relatedEntityId by FinancialTransactions.relatedEntityId
    var transactionDate by FinancialTransactions.transactionDate
    var createdAt by FinancialTransactions.createdAt
    var updatedAt by FinancialTransactions.updatedAt
}
