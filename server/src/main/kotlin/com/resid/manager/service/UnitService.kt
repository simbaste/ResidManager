package com.resid.manager.service

import com.resid.manager.data.Equipment
import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.Lease
import com.resid.manager.data.LeaseStatus
import com.resid.manager.data.Leases
import com.resid.manager.data.Residence
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.ResidenceUnit
import com.resid.manager.data.ResidenceUnits
import com.resid.manager.data.UnitStatus
import com.resid.manager.data.convert
import com.resid.manager.data.isManager
import com.resid.manager.dto.EquipmentDto
import com.resid.manager.dto.ResidenceUnitCreateRequest
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UnitStatusDto
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

object UnitService {

    suspend fun isResidenceManager(userId: UUID, residenceId: UUID): Boolean = dbQuery {
        ResidenceMembers.select(ResidenceMembers.role)
            .where {
                (ResidenceMembers.userId eq userId) and
                (ResidenceMembers.residenceId eq residenceId) and
                (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
            }.any { it[ResidenceMembers.role].isManager() }
    }

    suspend fun createUnit(
        requesterId: UUID,
        residenceId: UUID,
        request: ResidenceUnitCreateRequest
    ): ResidenceUnitDto = dbQuery {
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires de cette résidence peuvent ajouter des logements."
            )
        }

        if (request.name.isBlank() || request.floor.isBlank() || request.type.isBlank()) {
            throw HttpError(HttpStatusCode.BadRequest, "Veuillez remplir tous les champs obligatoires.")
        }
        if (request.nominalRent < 0.0 || request.serviceCharges < 0.0 || request.initialElectricityIndex < 0.0) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Les montants financiers et d'index d'électricité doivent être supérieurs ou égaux à 0."
            )
        }

        val activeRes = Residence.findById(residenceId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

        val alreadyExists = ResidenceUnit.find {
            (ResidenceUnits.residenceId eq residenceId) and
                    (ResidenceUnits.name eq request.name)
        }.count() > 0
        if (alreadyExists) throw HttpError(HttpStatusCode.Conflict, "Un logement existe déjà avec ce nom dans cette résidence")

        val now = LocalDateTime.now(Clock.systemUTC())
        val newResidenceUnit = ResidenceUnit.new {
            this.residence = activeRes
            this.name = request.name
            this.floor = request.floor
            this.type = request.type
            this.nominalRent = request.nominalRent
            this.serviceCharges = request.serviceCharges
            this.initialElectricityIndex = request.initialElectricityIndex
            this.createdAt = now
            this.updatedAt = now
            this.status = UnitStatus.AVAILABLE
        }

        if (request.equipmentIds.isNotEmpty()) {
            val selectedEq = request.equipmentIds.mapNotNull { eqId ->
                try {
                    Equipment.findById(UUID.fromString(eqId))
                } catch (_: Exception) {
                    null
                }
            }
            newResidenceUnit.equipments = SizedCollection(selectedEq)
        }

        newResidenceUnit.flush()

        ResidenceUnitDto(
            id = newResidenceUnit.id.value.toString(),
            residenceId = newResidenceUnit.residence.id.value.toString(),
            name = newResidenceUnit.name,
            floor = newResidenceUnit.floor,
            type = newResidenceUnit.type,
            nominalRent = newResidenceUnit.nominalRent,
            serviceCharges = newResidenceUnit.serviceCharges,
            initialElectricityIndex = newResidenceUnit.initialElectricityIndex,
            status = newResidenceUnit.status.convert(),
            equipments = newResidenceUnit.equipments.map { eq ->
                EquipmentDto(id = eq.id.value.toString(), key = eq.key, label = eq.label)
            }
        )
    }

    suspend fun updateUnit(
        requesterId: UUID,
        residenceId: UUID,
        unitId: UUID,
        request: ResidenceUnitCreateRequest
    ): ResidenceUnitDto = dbQuery {
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent modifier des logements."
            )
        }

        if (request.name.isBlank() || request.floor.isBlank() || request.type.isBlank()) {
            throw HttpError(HttpStatusCode.BadRequest, "Veuillez remplir tous les champs obligatoires.")
        }
        if (request.nominalRent < 0.0 || request.serviceCharges < 0.0 || request.initialElectricityIndex < 0.0) {
            throw HttpError(
                HttpStatusCode.BadRequest,
                "Les montants financiers et d'index d'électricité doivent être supérieurs ou égaux à 0."
            )
        }

        val dbUnit = ResidenceUnit.findById(unitId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Logement introuvable.")

        if (dbUnit.residence.id.value != residenceId) {
            throw HttpError(HttpStatusCode.BadRequest, "Ce logement n'appartient pas à la résidence spécifiée.")
        }

        val alreadyExists = ResidenceUnit.find {
            (ResidenceUnits.residenceId eq residenceId) and
            (ResidenceUnits.name eq request.name) and
            (ResidenceUnits.id neq unitId)
        }.count() > 0
        if (alreadyExists) throw HttpError(HttpStatusCode.Conflict, "Un logement existe déjà avec ce nom dans cette résidence")

        dbUnit.name = request.name
        dbUnit.floor = request.floor
        dbUnit.type = request.type
        dbUnit.nominalRent = request.nominalRent
        dbUnit.serviceCharges = request.serviceCharges
        dbUnit.initialElectricityIndex = request.initialElectricityIndex
        dbUnit.updatedAt = LocalDateTime.now(Clock.systemUTC())

        if (request.equipmentIds.isNotEmpty()) {
            val selectedEq = request.equipmentIds.mapNotNull { eqId ->
                try {
                    Equipment.findById(UUID.fromString(eqId))
                } catch (_: Exception) {
                    null
                }
            }
            dbUnit.equipments = SizedCollection(selectedEq)
        }

        dbUnit.flush()

        ResidenceUnitDto(
            id = dbUnit.id.value.toString(),
            residenceId = dbUnit.residence.id.value.toString(),
            name = dbUnit.name,
            floor = dbUnit.floor,
            type = dbUnit.type,
            nominalRent = dbUnit.nominalRent,
            serviceCharges = dbUnit.serviceCharges,
            initialElectricityIndex = dbUnit.initialElectricityIndex,
            status = dbUnit.status.convert(),
            photoUrl = dbUnit.photoUrl,
            equipments = dbUnit.equipments.map { eq ->
                EquipmentDto(id = eq.id.value.toString(), key = eq.key, label = eq.label)
            }
        )
    }

    suspend fun getUnits(
        residenceId: UUID,
        unitId: UUID?,
    ): List<ResidenceUnitDto> = dbQuery {
        Residence.findById(residenceId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

        val units = ResidenceUnit.find {
            (ResidenceUnits.residenceId eq residenceId) and
                    (if (unitId != null) ResidenceUnits.id eq unitId else Op.TRUE)
        }.toList()

        val unitIds = units.map { it.id.value }
        val activeLeasesMap: Map<UUID, String> = if (unitIds.isNotEmpty()) {
            Lease.find {
                (Leases.residenceUnitId inList unitIds) and (Leases.status eq LeaseStatus.SIGNED_ACTIVE)
            }.associate { lease ->
                val tenantName = lease.tenant?.let { "${it.firstName} ${it.lastName}".trim() } ?: ""
                lease.residenceUnit.id.value to tenantName
            }
        } else {
            emptyMap()
        }

        units.map {
            ResidenceUnitDto(
                id = it.id.value.toString(),
                residenceId = it.residence.id.value.toString(),
                name = it.name,
                floor = it.floor,
                type = it.type,
                nominalRent = it.nominalRent,
                serviceCharges = it.serviceCharges,
                initialElectricityIndex = it.initialElectricityIndex,
                status = it.status.convert(),
                photoUrl = it.photoUrl,
                equipments = it.equipments.map { eq ->
                    EquipmentDto(id = eq.id.value.toString(), key = eq.key, label = eq.label)
                },
                currentTenantName = activeLeasesMap[it.id.value]?.takeIf { name -> name.isNotBlank() }
            )
        }
    }

    suspend fun searchUnits(
        residenceId: UUID,
        query: String?,
        memberId: UUID?,
        status: UnitStatusDto?
    ): List<ResidenceUnitDto> = dbQuery {
        Residence.findById(residenceId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

        var conditions: Op<Boolean> = ResidenceUnits.residenceId eq residenceId

        if (status != null) {
            conditions = conditions and (ResidenceUnits.status eq status.convert())
        }

        val searchPattern = query?.trim()?.takeIf { it.isNotBlank() }?.let { "%${it.lowercase()}%" }
        if (searchPattern != null) {
            conditions = conditions and (
                (ResidenceUnits.name.lowerCase() like searchPattern) or
                (ResidenceUnits.floor.lowerCase() like searchPattern) or
                (ResidenceUnits.type.lowerCase() like searchPattern)
            )
        }

        if (memberId != null) {
            val leasedUnitIds = Leases
                .select(Leases.residenceUnitId)
                .where {
                    (Leases.tenantId eq memberId) and
                    (Leases.status eq LeaseStatus.SIGNED_ACTIVE)
                }
                .map { it[Leases.residenceUnitId].value }

            conditions = conditions and (ResidenceUnits.id inList leasedUnitIds)
        }

        val units = ResidenceUnit.find { conditions }.toList()
        val unitIds = units.map { it.id.value }
        val activeLeasesMap: Map<UUID, String> = if (unitIds.isNotEmpty()) {
            Lease.find {
                (Leases.residenceUnitId inList unitIds) and (Leases.status eq LeaseStatus.SIGNED_ACTIVE)
            }.associate { lease ->
                val tenantName = lease.tenant?.let { "${it.firstName} ${it.lastName}".trim() } ?: ""
                lease.residenceUnit.id.value to tenantName
            }
        } else {
            emptyMap()
        }

        units.map {
            ResidenceUnitDto(
                id = it.id.value.toString(),
                residenceId = it.residence.id.value.toString(),
                name = it.name,
                floor = it.floor,
                type = it.type,
                nominalRent = it.nominalRent,
                serviceCharges = it.serviceCharges,
                initialElectricityIndex = it.initialElectricityIndex,
                status = it.status.convert(),
                photoUrl = it.photoUrl,
                equipments = it.equipments.map { eq ->
                    EquipmentDto(id = eq.id.value.toString(), key = eq.key, label = eq.label)
                },
                currentTenantName = activeLeasesMap[it.id.value]?.takeIf { name -> name.isNotBlank() }
            )
        }
    }

    suspend fun deleteUnit(
        requesterId: UUID,
        residenceId: UUID,
        unitId: UUID
    ) = dbQuery {
        if (!isResidenceManager(requesterId, residenceId)) {
            throw HttpError(
                HttpStatusCode.Forbidden,
                "Accès interdit : Seuls les administrateurs et gestionnaires peuvent supprimer des logements."
            )
        }

        val dbUnit = ResidenceUnit.findById(unitId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Logement introuvable.")

        if (dbUnit.residence.id.value != residenceId) {
            throw HttpError(HttpStatusCode.BadRequest, "Ce logement n'appartient pas à la résidence spécifiée.")
        }

        // Check if there are active leases on this unit
        val hasActiveLease = Leases
            .select(Leases.id)
            .where {
                (Leases.residenceUnitId eq unitId) and
                (Leases.status eq LeaseStatus.SIGNED_ACTIVE)
            }
            .count() > 0

        if (hasActiveLease) {
            throw HttpError(
                HttpStatusCode.Conflict,
                "Impossible de supprimer ce logement car un contrat de bail est actuellement actif."
            )
        }

        ResidenceUnits.deleteWhere { id eq unitId }
    }

    suspend fun getUnitPhotos(userId: UUID, residenceId: UUID, unitId: UUID): List<com.resid.manager.dto.ResidencePhotoDto> = dbQuery {
        val dbUnit = ResidenceUnit.findById(unitId)
            ?: throw HttpError(HttpStatusCode.NotFound, "Logement introuvable.")
        if (dbUnit.residence.id.value != residenceId) {
            throw HttpError(HttpStatusCode.BadRequest, "Ce logement n'appartient pas à la résidence spécifiée.")
        }

        val photos = com.resid.manager.data.ResidenceUnitPhotos.select(
            com.resid.manager.data.ResidenceUnitPhotos.url,
            com.resid.manager.data.ResidenceUnitPhotos.description
        ).where {
            com.resid.manager.data.ResidenceUnitPhotos.residenceUnitId eq unitId
        }.orderBy(com.resid.manager.data.ResidenceUnitPhotos.sortOrder)
        .map {
            com.resid.manager.dto.ResidencePhotoDto(
                url = it[com.resid.manager.data.ResidenceUnitPhotos.url],
                description = it[com.resid.manager.data.ResidenceUnitPhotos.description]
            )
        }

        if (photos.isEmpty() && !dbUnit.photoUrl.isNullOrBlank()) {
            listOf(com.resid.manager.dto.ResidencePhotoDto(url = dbUnit.photoUrl!!, description = "Photo principale"))
        } else {
            photos
        }
    }

    suspend fun updateUnitPhotos(
        userId: UUID,
        residenceId: UUID,
        unitId: UUID,
        newPhotos: List<com.resid.manager.dto.ResidencePhotoDto>
    ): List<com.resid.manager.dto.ResidencePhotoDto> {
        val (photosToDeleteUrls, newPhotosList) = dbQuery {
            if (!isResidenceManager(userId, residenceId)) {
                throw HttpError(
                    HttpStatusCode.Forbidden,
                    "Accès interdit : Seuls les administrateurs et gestionnaires peuvent modifier les photos de ce logement."
                )
            }

            val dbUnit = ResidenceUnit.findById(unitId)
                ?: throw HttpError(HttpStatusCode.NotFound, "Logement introuvable.")
            if (dbUnit.residence.id.value != residenceId) {
                throw HttpError(HttpStatusCode.BadRequest, "Ce logement n'appartient pas à la résidence spécifiée.")
            }

            val existingPhotos = com.resid.manager.data.ResidenceUnitPhotos.select(
                com.resid.manager.data.ResidenceUnitPhotos.id,
                com.resid.manager.data.ResidenceUnitPhotos.url
            ).where {
                com.resid.manager.data.ResidenceUnitPhotos.residenceUnitId eq unitId
            }.map {
                it[com.resid.manager.data.ResidenceUnitPhotos.id].value to it[com.resid.manager.data.ResidenceUnitPhotos.url]
            }

            val newUrls = newPhotos.map { it.url }.toSet()
            val photosToDelete = existingPhotos.filter { it.second !in newUrls }
            val deleteUrls = photosToDelete.map { it.second }

            if (photosToDelete.isNotEmpty()) {
                val deleteIds = photosToDelete.map { it.first }
                com.resid.manager.data.ResidenceUnitPhotos.deleteWhere {
                    com.resid.manager.data.ResidenceUnitPhotos.id inList deleteIds
                }
            }

            val now = LocalDateTime.now(Clock.systemUTC())
            newPhotos.forEachIndexed { index, photoDto ->
                val existing = existingPhotos.find { it.second == photoDto.url }
                if (existing != null) {
                    com.resid.manager.data.ResidenceUnitPhotos.update({ com.resid.manager.data.ResidenceUnitPhotos.id eq existing.first }) {
                        it[description] = photoDto.description
                        it[sortOrder] = index
                        it[updatedAt] = now
                    }
                } else {
                    com.resid.manager.data.ResidenceUnitPhotos.insert {
                        it[id] = UUID.randomUUID()
                        it[com.resid.manager.data.ResidenceUnitPhotos.residenceUnitId] = unitId
                        it[url] = photoDto.url
                        it[description] = photoDto.description
                        it[sortOrder] = index
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                }
            }

            dbUnit.photoUrl = newPhotos.firstOrNull()?.url
            dbUnit.updatedAt = now
            dbUnit.flush()

            Pair(deleteUrls, newPhotos)
        }

        val r2Service = com.resid.manager.storage.R2StorageService()
        photosToDeleteUrls.forEach { url ->
            try {
                r2Service.deleteFileByUrl(url)
            } catch (e: Exception) {
                println("Avertissement suppression R2 pour $url: ${e.message}")
            }
        }

        return newPhotosList
    }
}
