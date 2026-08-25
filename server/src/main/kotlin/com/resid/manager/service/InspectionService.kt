package com.resid.manager.service

import com.resid.manager.data.HttpError
import com.resid.manager.data.InspectionReport
import com.resid.manager.data.InspectionReportItems
import com.resid.manager.data.InspectionReports
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.Lease
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.User
import com.resid.manager.data.convert
import com.resid.manager.data.isManager
import com.resid.manager.dto.InspectionReportCreateRequest
import com.resid.manager.dto.InspectionReportDto
import com.resid.manager.dto.InspectionReportItemDto
import com.resid.manager.dto.InspectionReportUpdateRequest
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object InspectionService {

    fun isResidenceManager(userId: UUID, residenceId: UUID): Boolean = transaction {
        ResidenceMembers.select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.any { it[ResidenceMembers.role].isManager() }
    }

    fun isResidenceMember(userId: UUID, residenceId: UUID): Boolean = transaction {
        ResidenceMembers.select(ResidenceMembers.userId)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.count() > 0
    }

    fun createReport(
        inspectorId: UUID,
        request: InspectionReportCreateRequest
    ): InspectionReportDto = transaction {
        val leaseUuid = try {
            UUID.fromString(request.leaseId)
        } catch (e: Exception) {
            throw HttpError(HttpStatusCode.BadRequest, "ID de bail invalide : ${e.message}")
        }

        val dbLease = Lease.findById(leaseUuid)
            ?: throw HttpError(HttpStatusCode.NotFound, "Contrat de bail introuvable.")

        val residenceId = dbLease.residenceUnit.residence.id.value

        if (!isResidenceManager(inspectorId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les gestionnaires et administrateurs peuvent établir un état des lieux."
            )
        }

        val dbInspector = User.findById(inspectorId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Inspecteur introuvable.")

        // Check if report of this type already exists for this lease
        val reportType = request.type.convert()
        val alreadyExists = InspectionReport.find {
            (InspectionReports.leaseId eq leaseUuid) and
            (InspectionReports.type eq reportType)
        }.count() > 0

        if (alreadyExists) {
            throw HttpError(
                HttpStatusCode.Conflict,
                "Un état des lieux de type ${request.type.name} existe déjà pour ce contrat de bail."
            )
        }

        val parsedDate = try {
            LocalDate.parse(request.inspectionDate)
        } catch (e: Exception) {
            throw HttpError(HttpStatusCode.BadRequest, "Format de date d'inspection invalide : ${e.message}")
        }

        val now = LocalDateTime.now(Clock.systemUTC())
        val newReport = InspectionReport.new {
            this.lease = dbLease
            this.inspector = dbInspector
            this.type = reportType
            this.inspectionDate = parsedDate
            this.generalCondition = request.generalCondition.convert()
            this.electricityMeterIndex = request.electricityMeterIndex
            this.waterMeterIndex = request.waterMeterIndex
            this.gasMeterIndex = request.gasMeterIndex
            this.keysCount = request.keysCount
            this.comments = request.comments
            this.photoUrls = request.photoUrls.takeIf { it.isNotEmpty() }?.joinToString(",")
            this.createdAt = now
            this.updatedAt = now
        }
        newReport.flush()

        // Insert items using Exposed DSL for robust column mapping
        val createdItemDtos = request.items.map { itemDto ->
            val itemId = UUID.randomUUID()
            val photosStr = itemDto.photoUrls.takeIf { it.isNotEmpty() }?.joinToString(",")

            InspectionReportItems.insert {
                it[id] = itemId
                it[inspectionReportId] = newReport.id.value
                it[category] = itemDto.category.convert()
                it[name] = itemDto.name
                it[condition] = itemDto.condition.convert()
                it[quantity] = itemDto.quantity
                it[observations] = itemDto.observations
                it[photoUrls] = photosStr
                it[createdAt] = now
                it[updatedAt] = now
            }

            InspectionReportItemDto(
                id = itemId.toString(),
                category = itemDto.category,
                name = itemDto.name,
                condition = itemDto.condition,
                quantity = itemDto.quantity,
                observations = itemDto.observations,
                photoUrls = itemDto.photoUrls
            )
        }

        InspectionReportDto(
            id = newReport.id.value.toString(),
            leaseId = dbLease.id.value.toString(),
            inspectorId = dbInspector.id.value.toString(),
            type = request.type,
            inspectionDate = newReport.inspectionDate.toString(),
            generalCondition = request.generalCondition,
            electricityMeterIndex = newReport.electricityMeterIndex,
            waterMeterIndex = newReport.waterMeterIndex,
            gasMeterIndex = newReport.gasMeterIndex,
            keysCount = newReport.keysCount,
            comments = newReport.comments,
            photoUrls = request.photoUrls,
            items = createdItemDtos,
            createdAt = newReport.createdAt.toString(),
            updatedAt = newReport.updatedAt.toString()
        )
    }

    fun updateReport(
        requesterId: UUID,
        reportId: UUID,
        request: InspectionReportUpdateRequest
    ): InspectionReportDto = transaction {
        val dbReport = InspectionReport.findById(reportId)
            ?: throw HttpError(HttpStatusCode.NotFound, "État des lieux introuvable.")

        val residenceId = dbReport.lease.residenceUnit.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent modifier un état des lieux."
            )
        }

        val now = LocalDateTime.now(Clock.systemUTC())

        request.inspectionDate?.takeIf { it.isNotBlank() }?.let {
            dbReport.inspectionDate = try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                throw HttpError(HttpStatusCode.BadRequest, "Format de date invalide : ${e.message}")
            }
        }

        request.generalCondition?.let { dbReport.generalCondition = it.convert() }
        request.electricityMeterIndex?.let { dbReport.electricityMeterIndex = it }
        request.waterMeterIndex?.let { dbReport.waterMeterIndex = it }
        request.gasMeterIndex?.let { dbReport.gasMeterIndex = it }
        request.keysCount?.let { dbReport.keysCount = it }
        request.comments?.let { dbReport.comments = it }
        request.photoUrls?.let { dbReport.photoUrls = it.joinToString(",") }
        dbReport.updatedAt = now

        // Update items if supplied
        val updatedItems = request.items
        if (updatedItems != null) {
            InspectionReportItems.deleteWhere { inspectionReportId eq reportId }
            updatedItems.forEach { itemDto ->
                val itemId = UUID.randomUUID()
                val photosStr = itemDto.photoUrls.takeIf { it.isNotEmpty() }?.joinToString(",")

                InspectionReportItems.insert {
                    it[id] = itemId
                    it[inspectionReportId] = reportId
                    it[category] = itemDto.category.convert()
                    it[name] = itemDto.name
                    it[condition] = itemDto.condition.convert()
                    it[quantity] = itemDto.quantity
                    it[observations] = itemDto.observations
                    it[photoUrls] = photosStr
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }

        dbReport.flush()
        getReportById(requesterId, reportId)
    }

    fun getReportsByLease(
        requesterId: UUID,
        leaseId: UUID
    ): List<InspectionReportDto> = transaction {
        val dbLease = Lease.findById(leaseId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Contrat de bail introuvable.")

        val residenceId = dbLease.residenceUnit.residence.id.value
        val isTenant = dbLease.tenant.id.value == requesterId

        if (!isTenant && !isResidenceMember(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Vous n'avez pas accès aux états des lieux de ce bail."
            )
        }

        InspectionReports
            .select(InspectionReports.id)
            .where { InspectionReports.leaseId eq leaseId }
            .map { row ->
                val reportId = row[InspectionReports.id].value
                getReportById(requesterId, reportId)
            }
    }

    fun getReportById(
        requesterId: UUID,
        reportId: UUID
    ): InspectionReportDto = transaction {
        val dbReport = InspectionReport.findById(reportId)
            ?: throw HttpError(HttpStatusCode.NotFound, "État des lieux introuvable.")

        val dbLease = dbReport.lease
        val residenceId = dbLease.residenceUnit.residence.id.value
        val isTenant = dbLease.tenant.id.value == requesterId

        if (!isTenant && !isResidenceMember(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Vous n'avez pas accès à cet état des lieux."
            )
        }

        val itemsList = InspectionReportItems
            .selectAll()
            .where { InspectionReportItems.inspectionReportId eq reportId }
            .map { row ->
                InspectionReportItemDto(
                    id = row[InspectionReportItems.id].value.toString(),
                    category = row[InspectionReportItems.category].convert(),
                    name = row[InspectionReportItems.name],
                    condition = row[InspectionReportItems.condition].convert(),
                    quantity = row[InspectionReportItems.quantity],
                    observations = row[InspectionReportItems.observations],
                    photoUrls = row[InspectionReportItems.photoUrls]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                )
            }

        val photos = dbReport.photoUrls?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

        InspectionReportDto(
            id = dbReport.id.value.toString(),
            leaseId = dbLease.id.value.toString(),
            inspectorId = dbReport.inspector.id.value.toString(),
            type = dbReport.type.convert(),
            inspectionDate = dbReport.inspectionDate.toString(),
            generalCondition = dbReport.generalCondition.convert(),
            electricityMeterIndex = dbReport.electricityMeterIndex,
            waterMeterIndex = dbReport.waterMeterIndex,
            gasMeterIndex = dbReport.gasMeterIndex,
            keysCount = dbReport.keysCount,
            comments = dbReport.comments,
            photoUrls = photos,
            items = itemsList,
            createdAt = dbReport.createdAt.toString(),
            updatedAt = dbReport.updatedAt.toString()
        )
    }

    fun deleteReport(
        requesterId: UUID,
        reportId: UUID
    ) = transaction {
        val dbReport = InspectionReport.findById(reportId)
            ?: throw HttpError(HttpStatusCode.NotFound, "État des lieux introuvable.")

        val residenceId = dbReport.lease.residenceUnit.residence.id.value
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent supprimer un état des lieux."
            )
        }

        InspectionReports.deleteWhere { id eq reportId }
    }
}
