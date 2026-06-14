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
    val code = varchar("code", 3).uniqueIndex()
    val symbol = varchar("symbol", 10)
    val label = varchar("label", 100)
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
    val role = varchar("role", 20) // OWNER, ADMIN, MANAGER, STAFF, TENANT
    val status = varchar("status", 20) // PENDING_APPROVAL, INVITED, ACCEPTED
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(userId, residenceId)
}

object Logements : UUIDTable("logements") {
    val residenceId = reference("residence_id", Residences, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val floor = varchar("floor", 50)
    val type = varchar("type", 20) // Room, Studio, T2, etc.
    val nominalRent = double("nominal_rent")
    val serviceCharges = double("service_charges")
    val initialElectricityIndex = double("initial_electricity_index")
    val status = varchar("status", 20).default("AVAILABLE") // AVAILABLE, OCCUPIED, RESERVED
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object Baux : UUIDTable("baux") {
    val logementId = reference("logement_id", Logements, onDelete = ReferenceOption.RESTRICT)
    val tenantId = reference("tenant_id", Users, onDelete = ReferenceOption.RESTRICT)
    val durationMonths = integer("duration_months")
    val paymentFrequency = varchar("payment_frequency", 20) // MONTHLY, ANNUAL
    val depositAmount = double("deposit_amount")
    val depositStatus = varchar("deposit_status", 20).default("PENDING") // PENDING, PAID
    val status = varchar("status", 20).default("PENDING_PAYMENT") // PENDING_PAYMENT, PARTIALLY_PAID, PENDING_SIGNATURE, ACTIVE, TERMINATED
    val startDate = date("start_date")
    val endDate = date("end_date")
    val advanceMonths = integer("advance_months").default(1)
    val advancePaymentAmount = double("advance_payment_amount").default(0.0)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object ElectricityStatements : UUIDTable("electricity_statements") {
    val logementId = reference("logement_id", Logements, onDelete = ReferenceOption.CASCADE)
    val oldIndex = double("old_index")
    val newIndex = double("new_index")
    val kWhPriceApplied = double("kwh_price_applied")
    val amountDue = double("amount_due")
    val status = varchar("status", 20).default("UNPAID") // UNPAID, PAID
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
    val logementId = reference("logement_id", Logements, onDelete = ReferenceOption.CASCADE)
    val creatorId = reference("creator_id", Users, onDelete = ReferenceOption.CASCADE)
    val categoryId = reference("category_id", TicketCategories, onDelete = ReferenceOption.RESTRICT)
    val title = varchar("title", 255)
    val description = text("description")
    val urgency = varchar("urgency", 20) // LOW, MEDIUM, CRITICAL
    val status = varchar("status", 20).default("OPEN") // OPEN, IN_PROGRESS, CLOSED
    val interventionCost = double("intervention_cost").default(0.0)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object FinancialTransactions : UUIDTable("financial_transactions") {
    val residenceId = reference("residence_id", Residences, onDelete = ReferenceOption.CASCADE)
    val type = varchar("type", 10) // INCOME, EXPENSE
    val category = varchar("category", 50) // Rent, Electricity, Maintenance, Cleaning, etc.
    val amount = double("amount")
    val description = text("description")
    val relatedEntityType = varchar("related_entity_type", 50).nullable() // BAIL, ELECTRICITY_STATEMENT, TICKET
    val relatedEntityId = uuid("related_entity_id").nullable()
    val transactionDate = date("transaction_date")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object Equipements : UUIDTable("equipements") {
    val key = varchar("key", 50).uniqueIndex()
    val label = varchar("label", 100)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object LogementEquipements : Table("logement_equipements") {
    val logementId = reference("logement_id", Logements, onDelete = ReferenceOption.CASCADE)
    val equipementId = reference("equipement_id", Equipements, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(logementId, equipementId)
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
    var label by Currencies.label
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

class Logement(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Logement>(Logements)

    var residence by Residence referencedOn Logements.residenceId
    var name by Logements.name
    var floor by Logements.floor
    var type by Logements.type
    var nominalRent by Logements.nominalRent
    var serviceCharges by Logements.serviceCharges
    var initialElectricityIndex by Logements.initialElectricityIndex
    var status by Logements.status
    var createdAt by Logements.createdAt
    var updatedAt by Logements.updatedAt

    var equipements by Equipement via LogementEquipements
}

class Equipement(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Equipement>(Equipements)

    var key by Equipements.key
    var label by Equipements.label
    var createdAt by Equipements.createdAt
    var updatedAt by Equipements.updatedAt
}

class Lease(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Lease>(Baux)

    var logement by Logement referencedOn Baux.logementId
    var tenant by User referencedOn Baux.tenantId
    var durationMonths by Baux.durationMonths
    var paymentFrequency by Baux.paymentFrequency
    var depositAmount by Baux.depositAmount
    var depositStatus by Baux.depositStatus
    var status by Baux.status
    var startDate by Baux.startDate
    var endDate by Baux.endDate
    var advanceMonths by Baux.advanceMonths
    var advancePaymentAmount by Baux.advancePaymentAmount
    var createdAt by Baux.createdAt
    var updatedAt by Baux.updatedAt
}

class ElectricityStatement(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ElectricityStatement>(ElectricityStatements)

    var logement by Logement referencedOn ElectricityStatements.logementId
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

    var logement by Logement referencedOn Tickets.logementId
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
