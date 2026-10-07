package com.resid.manager.service

import com.resid.manager.data.Currencies
import com.resid.manager.data.CurrencyEntity
import com.resid.manager.data.ElectricityStatement
import com.resid.manager.data.ElectricityStatements
import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.Lease
import com.resid.manager.data.LeaseStatus
import com.resid.manager.data.Leases
import com.resid.manager.data.Residence
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.ResidenceUnits
import com.resid.manager.data.Residences
import com.resid.manager.data.Role
import com.resid.manager.data.Users
import com.resid.manager.data.convert
import com.resid.manager.data.isAdmin
import com.resid.manager.data.isOwner
import com.resid.manager.dto.AssociatedResidenceItem
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.dto.MemberStatusUpdateRequest
import com.resid.manager.dto.ResidenceCreateRequest
import com.resid.manager.dto.ResidenceDirectoryDTO
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidenceSummaryItemDto
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

object ResidenceService {

    suspend fun isResidenceAdmin(userId: UUID, residenceId: UUID): Boolean = dbQuery {
        ResidenceMembers.select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.any { it[ResidenceMembers.role].isAdmin() }
    }

    suspend fun isResidenceOwner(userId: UUID, residenceId: UUID): Boolean = dbQuery {
        ResidenceMembers.select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.any { it[ResidenceMembers.role].isOwner() }
    }

    suspend fun getDirectory(userId: UUID): ResidenceDirectoryDTO = dbQuery {
        // Query memberships for this user
        val memberships = (ResidenceMembers innerJoin Residences innerJoin Currencies)
            .select(
                Residences.id,
                Residences.name,
                Residences.address,
                Residences.photoUrl,
                Currencies.code,
                Currencies.symbol,
                ResidenceMembers.role,
            )
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }
            .toList()

        val residenceIds = memberships.map { it[Residences.id].value }

        // Fetch unit counts for these residences in one query
        val unitCountsMap = if (residenceIds.isNotEmpty()) {
            ResidenceUnits
                .select(ResidenceUnits.residenceId, ResidenceUnits.id.count())
                .where { ResidenceUnits.residenceId inList residenceIds }
                .groupBy(ResidenceUnits.residenceId)
                .associate { it[ResidenceUnits.residenceId].value to it[ResidenceUnits.id.count()].toInt() }
        } else {
            emptyMap()
        }

        val ownedResidences = mutableListOf<ResidenceSummaryItemDto>()
        val associatedResidences = mutableListOf<AssociatedResidenceItem>()

        memberships.forEach { row ->
            val resId = row[Residences.id].value
            val role = row[ResidenceMembers.role]
            val totalUnits = unitCountsMap[resId] ?: 0

            if (role == Role.OWNER) {
                ownedResidences.add(
                    ResidenceSummaryItemDto(
                        id = resId.toString(),
                        name = row[Residences.name],
                        address = row[Residences.address],
                        photoUrl = row[Residences.photoUrl],
                        totalUnits = totalUnits,
                        currencyCode = row[Currencies.code].convert(),
                        currencySymbol = row[Currencies.symbol].convert(),
                        kWhPrice = row[Residences.kWhPrice]
                    )
                )
            } else {
                associatedResidences.add(
                    AssociatedResidenceItem(
                        id = resId.toString(),
                        name = row[Residences.name],
                        address = row[Residences.address],
                        photoUrl = row[Residences.photoUrl],
                        roleDto = role.convert(),
                        totalUnits = totalUnits,
                        currencySymbol = row[Currencies.symbol].convert(),
                        currencyCode = row[Currencies.code].convert(),
                        kWhPrice = row[Residences.kWhPrice]
                    )
                )
            }
        }

        ResidenceDirectoryDTO(
            ownedResidences = ownedResidences,
            associatedResidences = associatedResidences
        )
    }

    suspend fun getResidenceById(userId: UUID, residenceId: UUID): ResidenceSummaryItemDto = dbQuery {
        // Verify user is a member
        val isMember = ResidenceMembers.select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.count() > 0

        if (!isMember) {
            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit : Vous n'êtes pas membre de cette résidence.")
        }

        val res = Residence.findById(residenceId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

        val totalUnits = ResidenceUnits.select(ResidenceUnits.id)
            .where { ResidenceUnits.residenceId eq residenceId }
            .count()
            .toInt()

        ResidenceSummaryItemDto(
            id = res.id.value.toString(),
            name = res.name,
            address = res.address,
            photoUrl = res.photoUrl,
            totalUnits = totalUnits,
            currencyCode = res.currency.code.convert(),
            currencySymbol = res.currency.symbol.convert()
        )
    }

    suspend fun createResidence(userId: UUID, request: ResidenceCreateRequest): ResidenceSummaryItemDto = dbQuery {
        val selectedCurrency = CurrencyEntity.find { Currencies.code eq request.currency.convert() }.firstOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Devise non reconnue.")

        if (Residence.find { Residences.name eq request.name }.count() > 0) {
            throw HttpError(HttpStatusCode.Conflict, "Une résidence avec ce nom existe déjà.")
        }

        val now = LocalDateTime.now(Clock.systemUTC())
        val r = Residence.new {
            name = request.name
            address = request.address
            photoUrl = null
            currency = selectedCurrency
            kWhPrice = request.kWhPrice
            createdAt = now
            updatedAt = now
        }
        r.flush()

        ResidenceMembers.insert {
            it[ResidenceMembers.userId] = userId
            it[ResidenceMembers.residenceId] = r.id.value
            it[ResidenceMembers.role] = Role.OWNER
            it[ResidenceMembers.status] = InvitationStatus.ACCEPTED
            it[ResidenceMembers.createdAt] = now
            it[ResidenceMembers.updatedAt] = now
        }

        ResidenceSummaryItemDto(
            id = r.id.value.toString(),
            name = r.name,
            address = r.address,
            photoUrl = r.photoUrl,
            totalUnits = 0,
            currencyCode = r.currency.code.convert(),
            currencySymbol = r.currency.symbol.convert()
        )
    }

    suspend fun updateResidence(userId: UUID, residenceId: UUID, request: ResidenceCreateRequest): ResidenceSummaryItemDto = dbQuery {
        val dbResidence = Residence.findById(residenceId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

        if (!isResidenceAdmin(userId, residenceId)) {
            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit: Seuls les propriétaires et administrateurs peuvent modifier cette résidence.")
        }

        if (request.name.isBlank() || request.address.isBlank()) {
            throw HttpError(HttpStatusCode.BadRequest, "Veuillez remplir tous les champs obligatoires.")
        }
        if (request.kWhPrice < 0.0) {
            throw HttpError(HttpStatusCode.BadRequest, "Le prix du kWh doit être supérieur ou égal à 0.")
        }

        val selectedCurrency = CurrencyEntity.find { Currencies.code eq request.currency.convert() }.firstOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Devise non reconnue.")

        dbResidence.name = request.name
        dbResidence.address = request.address
        dbResidence.currency = selectedCurrency
        dbResidence.kWhPrice = request.kWhPrice
        dbResidence.updatedAt = LocalDateTime.now(Clock.systemUTC())
        dbResidence.flush()

        val totalUnits = ResidenceUnits.select(ResidenceUnits.id)
            .where { ResidenceUnits.residenceId eq residenceId }
            .count()
            .toInt()

        ResidenceSummaryItemDto(
            id = dbResidence.id.value.toString(),
            name = dbResidence.name,
            address = dbResidence.address,
            photoUrl = dbResidence.photoUrl,
            totalUnits = totalUnits,
            currencyCode = dbResidence.currency.code.convert(),
            currencySymbol = dbResidence.currency.symbol.convert(),
            kWhPrice = dbResidence.kWhPrice
        )
    }

    suspend fun deleteResidence(userId: UUID, residenceId: UUID) = dbQuery {
        Residence.findById(residenceId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

        if (!isResidenceOwner(userId, residenceId)) {
            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit: Seuls les propriétaires de cette résidence (OWNER) peuvent la supprimer.")
        }

        Residences.deleteWhere { id eq residenceId }
    }

    suspend fun searchResidences(searchName: String): List<ResidenceSummaryItemDto> = dbQuery {
        val query = if (searchName.isNotBlank()) {
            Residences.select(Residences.id, Residences.name, Residences.address, Residences.photoUrl, Residences.currencyId)
                .where { Residences.name.lowerCase() like "%${searchName.lowercase()}%" }
        } else {
            Residences.selectAll()
        }

        val matchedRows = query.toList()
        val residenceIds = matchedRows.map { it[Residences.id].value }

        val unitCountsMap = if (residenceIds.isNotEmpty()) {
            ResidenceUnits
                .select(ResidenceUnits.residenceId, ResidenceUnits.id.count())
                .where { ResidenceUnits.residenceId inList residenceIds }
                .groupBy(ResidenceUnits.residenceId)
                .associate { it[ResidenceUnits.residenceId].value to it[ResidenceUnits.id.count()].toInt() }
        } else {
            emptyMap()
        }

        matchedRows.map { row ->
            val resId = row[Residences.id].value
            val currency = CurrencyEntity[row[Residences.currencyId]]
            ResidenceSummaryItemDto(
                id = resId.toString(),
                name = row[Residences.name],
                address = row[Residences.address],
                photoUrl = row[Residences.photoUrl],
                totalUnits = unitCountsMap[resId] ?: 0,
                currencyCode = currency.code.convert(),
                currencySymbol = currency.symbol.convert(),
                kWhPrice = row[Residences.kWhPrice]
            )
        }
    }

    suspend fun exportElectricityPdf(residenceId: UUID, statementIdsParam: String?): Pair<ByteArray, String> = dbQuery {
        val dbResidence = Residence.findById(residenceId)
            ?: throw HttpError(HttpStatusCode.BadRequest, "Résidence introuvable.")

        val statementsToPrint = if (!statementIdsParam.isNullOrBlank()) {
            val uuids = statementIdsParam.split(",").mapNotNull {
                try { UUID.fromString(it.trim()) } catch (_: Exception) { null }
            }
            ElectricityStatement.find { ElectricityStatements.id inList uuids }.toList()
        } else {
            (ElectricityStatements innerJoin ResidenceUnits)
                .select(ElectricityStatements.columns)
                .where { ResidenceUnits.residenceId eq residenceId }
                .orderBy(ElectricityStatements.statementDate to SortOrder.DESC)
                .limit(4)
                .let { ElectricityStatement.wrapRows(it).toList() }
        }.map {
            val log = it.residenceUnit
            val activeLease = Lease.find {
                (Leases.residenceUnitId eq log.id) and (Leases.status eq LeaseStatus.SIGNED_ACTIVE)
            }.firstOrNull() ?: Lease.find {
                Leases.residenceUnitId eq log.id
            }.firstOrNull()

            val tenantName = activeLease?.let { lease ->
                "${lease.tenant.firstName} ${lease.tenant.lastName}".trim()
            }

            val dto = ElectricityStatementDto(
                id = it.id.value.toString(),
                unitId = it.residenceUnit.id.value.toString(),
                previousIndex = it.oldIndex,
                newIndex = it.newIndex,
                kWhPriceApplied = it.kWhPriceApplied,
                amountDue = it.amountDue,
                statementDate = it.statementDate.toString(),
                status = it.status.convert(),
                createdAt = it.createdAt.toString(),
                updatedAt = it.updatedAt.toString()
            )
            ElectricityReceiptItem(
                statement = dto,
                logementName = log.name,
                tenantName = tenantName
            )
        }

        val pdfBytes = PdfService.generateEcoPrintPdf(statementsToPrint, dbResidence.name)
        pdfBytes to dbResidence.name
    }

    suspend fun getResidenceMembers(
        userId: UUID,
        residenceId: UUID
    ): List<ResidenceMemberSummaryDto> = dbQuery {
        val isMember = ResidenceMembers
            .select(ResidenceMembers.userId)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }
            .count() > 0

        if (!isMember) {
            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit : vous ne faites pas partie de cette résidence.")
        }

        (Users innerJoin ResidenceMembers)
            .select(
                Users.id,
                Users.firstName,
                Users.lastName,
                Users.email,
                Users.phone,
                ResidenceMembers.role,
                ResidenceMembers.status
            )
            .where { ResidenceMembers.residenceId eq residenceId }
            .map { row ->
                ResidenceMemberSummaryDto(
                    userId = row[Users.id].value.toString(),
                    firstName = row[Users.firstName],
                    lastName = row[Users.lastName],
                    email = row[Users.email],
                    phone = row[Users.phone],
                    roleDto = row[ResidenceMembers.role].convert(),
                    status = row[ResidenceMembers.status].convert()
                )
            }
    }

    suspend fun adminUpdateMemberStatus(
        requesterId: UUID,
        residenceId: UUID,
        targetUserId: UUID,
        request: MemberStatusUpdateRequest
    ) = dbQuery {
        if (!isResidenceAdmin(requesterId, residenceId)) {
            throw HttpError(HttpStatusCode.Forbidden, "Accès interdit: Seuls les propriétaires et administrateurs peuvent accepter/modifier les membres.")
        }

        val targetRole = request.role
        val updated = ResidenceMembers.update({
            (ResidenceMembers.userId eq targetUserId) and
            (ResidenceMembers.residenceId eq residenceId)
        }) {
            it[status] = InvitationStatus.valueOf(request.status)
            if (targetRole != null) {
                it[role] = Role.valueOf(targetRole)
            }
            it[updatedAt] = LocalDateTime.now(Clock.systemUTC())
        }

        if (updated == 0) throw HttpError(HttpStatusCode.NotFound, "Membre introuvable.")
    }
}
