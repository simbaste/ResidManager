package com.resid.manager.routes

import com.resid.manager.data.Currencies
import com.resid.manager.data.CurrencyEntity
import com.resid.manager.data.ElectricityStatement
import com.resid.manager.data.HttpError
import com.resid.manager.data.InvitationStatus
import com.resid.manager.data.Logement
import com.resid.manager.data.Logements
import com.resid.manager.data.Residence
import com.resid.manager.data.ResidenceMembers
import com.resid.manager.data.ResidenceMembers.role
import com.resid.manager.data.ResidenceMembers.status
import com.resid.manager.data.Residences
import com.resid.manager.data.Role
import com.resid.manager.data.convert
import com.resid.manager.data.isAdmin
import com.resid.manager.data.isOwner
import com.resid.manager.dto.AssociatedResidenceItem
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.MemberStatusUpdateRequest
import com.resid.manager.dto.ResidenceCreateRequest
import com.resid.manager.dto.ResidenceDirectoryDTO
import com.resid.manager.dto.ResidenceSummaryItemDto
import com.resid.manager.service.PdfService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

fun Route.residencesRoutes() {
    route("/api/residences") {
        // GET /api/residences : List all residences

        // GET /api/residences/{id}/electricity/export-pdf : Generates the "Eco-Print" PDF (Publicly accessible for browser tabs printing)
        get("/{id}/electricity/export-pdf") {
            val residenceId = call.parameters["id"] ?: ""
            val statementIdsParam = call.request.queryParameters["ids"]

            try {
                val pdfBytes = transaction {
                    val dbResidence = Residence.findById(UUID.fromString(residenceId))
                        ?: throw HttpError(HttpStatusCode.BadRequest,"Résidence introuvable.")

                    val statementsToPrint = if (!statementIdsParam.isNullOrBlank()) {
                        val uuids = statementIdsParam.split(",").map { UUID.fromString(it.trim()) }
                        ElectricityStatement.all().filter { it.id.value in uuids }
                    } else {
                        ElectricityStatement.all()
                            .filter { it.logement.residence.id.value == UUID.fromString(residenceId) }
                            .sortedByDescending { it.statementDate }
                            .take(4)
                    }.map {
                        ElectricityStatementDto(
                            id = it.id.value.toString(),
                            logementId = it.logement.id.value.toString(),
                            previousIndex = it.oldIndex,
                            newIndex = it.newIndex,
                            kWhPriceApplied = it.kWhPriceApplied,
                            amountDue = it.amountDue,
                            statementDate = it.statementDate.toString(),
                            status = it.status.convert(),
                            createdAt = it.createdAt.toString(),
                            updatedAt = it.updatedAt.toString()
                        )
                    }

                    PdfService.generateEcoPrintPdf(statementsToPrint, dbResidence.name)
                }

                call.respondBytes(pdfBytes, ContentType.Application.Pdf)
            } catch (e: HttpError) {
                call.respond(e.code, ErrorResponse(e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur lors de l'export PDF : ${e.message}"))
            }
        }

        authenticate("auth-jwt") {
            // GET /api/residences : List residences for the directory
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""

                try {
                    val directoryDto = transaction {
                        // 1. Query owned residences (where role is m.role = 'OWNER')
                        val ownedList = Residence.all().filter { res ->
                            ResidenceMembers.select(role).where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                        (ResidenceMembers.residenceId eq res.id.value) and
                                        (role eq Role.OWNER) and
                                        (status eq InvitationStatus.ACCEPTED)
                            }.count() > 0
                        }.map { res ->
                            val totalUnits = Logement.find { Logements.residenceId eq res.id.value }.count()
                            ResidenceSummaryItemDto(
                                id = res.id.value.toString(),
                                name = res.name,
                                address = res.address,
                                photoUrl = res.photoUrl,
                                totalUnits = totalUnits.toInt(),
                                currencyCode = res.currency.code.convert(),
                                currencySymbol = res.currency.symbol.convert()
                            )
                        }

                        // 2. Query associated residences (where role is not m.role = 'OWNER')
                        val associatedList = Residence.all().filter { res ->
                            ResidenceMembers.select(role).where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                        (ResidenceMembers.residenceId eq res.id.value) and
                                        (role neq Role.OWNER) and
                                        (status eq InvitationStatus.ACCEPTED)
                            }.count() > 0
                        }.map { res ->
                            val role = ResidenceMembers.select(role).where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                        (ResidenceMembers.residenceId eq res.id.value)
                            }.map { it[role] }.first()

                            val totalUnits = Logement.find { Logements.residenceId eq res.id.value }.count()
                            AssociatedResidenceItem(
                                id = res.id.value.toString(),
                                name = res.name,
                                address = res.address,
                                photoUrl = res.photoUrl,
                                roleDto = role.convert(),
                                totalUnits = totalUnits.toInt(),
                                currencySymbol = res.currency.symbol.convert(),
                                currencyCode = res.currency.code.convert()
                            )
                        }

                        ResidenceDirectoryDTO(ownedResidences = ownedList, associatedResidences = associatedList)
                    }

                    call.respond(HttpStatusCode.OK, directoryDto)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la récupération de l'annuaire : ${e.message}")
                    )
                }
            }

            // Get /api/residences/{residenceId}
            get("/{residenceId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["residenceId"] ?: throw HttpError(HttpStatusCode.BadRequest, "ID de résidence manquant.")

                try {
                    transaction {
                        ResidenceMembers
                            .select(role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                        (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and
                                        (status eq InvitationStatus.ACCEPTED)
                            }
                            .singleOrNull()?.get(role)
                    } ?: throw HttpError(HttpStatusCode.Forbidden, "Accès interdit : Vous n'êtes pas membre de cette résidence.")

                    val residenceDto = transaction {
                        val res = Residence.findById(UUID.fromString(residenceId))
                            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")

                        val totalUnits = Logement.find { Logements.residenceId eq res.id.value }.count()
                        ResidenceSummaryItemDto(
                            id = res.id.value.toString(),
                            name = res.name,
                            address = res.address,
                            photoUrl = res.photoUrl,
                            totalUnits = totalUnits.toInt(),
                            currencyCode = res.currency.code.convert(),
                            currencySymbol = res.currency.symbol.convert()
                        )
                    }
                    call.respond(HttpStatusCode.OK, residenceDto)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur lors de la récupération de la résidence : ${e.message}"))
                }
            }

            // POST /api/residences : Create a residence
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""

                try {
                    val request = try {
                        call.receive<ResidenceCreateRequest>()
                    } catch (e: ContentTransformationException) {
                        throw HttpError(HttpStatusCode.BadRequest, "Parametre invalide ${e.message}")
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Erreur lors de la lecture de la requête : ${e.message}")
                    }

                    val newResidence = transaction {
                        val selectedCurrency = CurrencyEntity.find { Currencies.code eq request.currency.convert() }.first()

                        if (Residence.find { Residences.name eq request.name }.count() > 0) {
                            throw HttpError(HttpStatusCode.Conflict, "Une résidence avec ce nom existe déjà.")
                        }

                        // 1. Insert residence
                        val r = Residence.new {
                            name = request.name
                            address = request.address
                            photoUrl = null
                            currency = selectedCurrency
                            kWhPrice = request.kWhPrice
                            createdAt = LocalDateTime.now(Clock.systemUTC())
                            updatedAt = LocalDateTime.now(Clock.systemUTC())
                        }

                        // Force flush the residence insert to database to satisfy the foreign key constraint
                        r.flush()

                        // 2. Insert member record linking user to this residence as OWNER with ACCEPTED status
                        ResidenceMembers.insert {
                            it[ResidenceMembers.userId] = UUID.fromString(userId)
                            it[ResidenceMembers.residenceId] = r.id.value
                            it[ResidenceMembers.role] = Role.OWNER
                            it[ResidenceMembers.status] = InvitationStatus.ACCEPTED
                            it[ResidenceMembers.createdAt] = LocalDateTime.now(Clock.systemUTC())
                        }
                        r
                    }

                    val summary = ResidenceSummaryItemDto(
                        id = newResidence.id.value.toString(),
                        name = newResidence.name,
                        address = newResidence.address,
                        photoUrl = newResidence.photoUrl,
                        totalUnits = 0,
                        currencyCode = transaction { newResidence.currency.code }.convert(),
                        currencySymbol = transaction { newResidence.currency.symbol }.convert(),
                    )

                    call.respond(HttpStatusCode.Created, summary)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur lors de la création de la résidence : ${e.message}"))
                }
            }

            // DELETE /api/residences/{id} delete a residence
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: throw HttpError(HttpStatusCode.BadRequest, "ID de résidence manquant.")

                try {
                    // Check if residence exist
                    transaction {
                        Residence.findById(UUID.fromString(residenceId))
                            ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")
                    }
                    // 1. Verify that the caller is the OWNER of this residence
                    val userRole = transaction {
                        ResidenceMembers
                            .select(role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                        (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and
                                        (status eq InvitationStatus.ACCEPTED)
                            }
                            .singleOrNull()?.get(role)
                    }

                    if (userRole?.isOwner() != true) throw HttpError(HttpStatusCode.Forbidden, "Accès interdit: Seuls les propriétaires de cette résidence (OWNER) peuvent la supprimer.")

                    // 2. Perform the deletion inside a transaction using Exposed DSL
                    transaction {
                        Residences.deleteWhere { Residences.id eq UUID.fromString(residenceId) }
                    }

                    call.respond(HttpStatusCode.OK, mapOf("message" to "La résidence a été supprimée avec succès !"))
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la suppression de la résidence : ${e.message}")
                    )
                }
            }

            // PUT /api/residences/{id} : Update a residence
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: throw HttpError(HttpStatusCode.BadRequest, "ID de résidence manquant.")

                try {
                    // Check if residence exist
                    val dbResidence = transaction {
                        Residence.findById(UUID.fromString(residenceId)) ?: throw HttpError(HttpStatusCode.NotFound, "Résidence introuvable.")
                    }
                    // Check if sender is OWNER or ADMIN
                    val userRole = transaction {
                        ResidenceMembers
                            .select(role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                        (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and
                                        (status eq InvitationStatus.ACCEPTED)
                            }
                            .singleOrNull()?.get(role)
                    }

                    // TODO: Verify if the user doesn't have super admin role
                    if (userRole?.isAdmin() != true) throw HttpError(HttpStatusCode.Forbidden, "Accès interdit: Seuls les propriétaires et administrateurs peuvent modifier cette résidence.")

                    val request = try {
                        call.receive<ResidenceCreateRequest>()
                    } catch (e: Exception) {
                        throw HttpError(HttpStatusCode.BadRequest, "Paramètre invalide : ${e.message}")
                    }

                    // Validation
                    if (request.name.isBlank() || request.address.isBlank()) throw HttpError(HttpStatusCode.BadRequest, "Veuillez remplir tous les champs obligatoires.")

                    if (request.kWhPrice < 0.0) throw HttpError(HttpStatusCode.BadRequest, "Le prix du kWh doit être supérieur ou égal à 0.")

                    val updatedSummary = transaction {
                        val selectedCurrency = CurrencyEntity.find { Currencies.code eq request.currency.convert() }.first()

                        dbResidence.name = request.name
                        dbResidence.address = request.address
                        dbResidence.currency = selectedCurrency
                        dbResidence.kWhPrice = request.kWhPrice
                        dbResidence.updatedAt = LocalDateTime.now(Clock.systemUTC())

                        dbResidence.flush()

                        val totalUnits = Logement.find { Logements.residenceId eq dbResidence.id.value }.count()

                        ResidenceSummaryItemDto(
                            id = dbResidence.id.value.toString(),
                            name = dbResidence.name,
                            address = dbResidence.address,
                            photoUrl = dbResidence.photoUrl,
                            totalUnits = totalUnits.toInt(),
                            currencyCode = dbResidence.currency.code.convert(),
                            currencySymbol = dbResidence.currency.symbol.convert()
                        )
                    }

                    call.respond(HttpStatusCode.OK, updatedSummary)
                } catch (e: HttpError) {
                    call.respond(e.code, ErrorResponse(e.message))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la modification de la résidence : ${e.message}")
                    )
                }
            }

            // GET /api/residences/search?name=... : Search for a residence by name
            get("/search") {
                val searchName = call.request.queryParameters["name"] ?: ""

                try {
                    val results = transaction {
                        Residence.all().filter {
                            it.name.contains(searchName, ignoreCase = true)
                        }.map { res ->
                            val totalUnits = Logement.find { Logements.residenceId eq res.id.value }.count()
                            ResidenceSummaryItemDto(
                                id = res.id.value.toString(),
                                name = res.name,
                                address = res.address,
                                photoUrl = res.photoUrl,
                                totalUnits = totalUnits.toInt(),
                                currencyCode = res.currency.code.convert(),
                                currencySymbol = res.currency.symbol.convert()
                            )
                        }
                    }

                    call.respond(HttpStatusCode.OK, results)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la recherche des résidences : ${e.message}")
                    )
                }
            }

            // POST /api/residences/{id}/join : Request to join a residence
            post("/{id}/join") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""

                try {
                    transaction {
                        ResidenceMembers.insert {
                            it[ResidenceMembers.userId] = UUID.fromString(userId)
                            it[ResidenceMembers.residenceId] = UUID.fromString(residenceId)
                            it[ResidenceMembers.role] = Role.TENANT
                            it[ResidenceMembers.status] = InvitationStatus.PENDING_APPROVAL
                            it[ResidenceMembers.createdAt] = LocalDateTime.now(Clock.systemUTC())
                        }
                    }
                    call.respond(HttpStatusCode.OK, mapOf("residenceId" to residenceId, "status" to "Demande envoyée"))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la demande d'adhésion : ${e.message}")
                    )
                }
            }

            // POST /api/residences/{id}/members/{user_id}/status : Accept/Refuse a member or request
            post("/{id}/members/{user_id}/status") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""
                val targetUserId = call.parameters["user_id"] ?: ""

                try {
                    // Check if sender is OWNER or ADMIN
                    val userRole = transaction {
                        ResidenceMembers
                            .select(ResidenceMembers.role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(userId)) and
                                        (ResidenceMembers.residenceId eq UUID.fromString(residenceId)) and
                                        (ResidenceMembers.status eq InvitationStatus.ACCEPTED)
                            }
                            .singleOrNull()?.get(ResidenceMembers.role)
                    }

                    if (userRole?.isAdmin() != true) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit: Seuls les propriétaires et administrateurs peuvent accepter/modifier les membres."))
                        return@post
                    }

                    val request = call.receive<MemberStatusUpdateRequest>()

                    transaction {
                        ResidenceMembers.update({
                            (ResidenceMembers.userId eq UUID.fromString(targetUserId)) and
                                    (ResidenceMembers.residenceId eq UUID.fromString(residenceId))
                        }) {
                            it[status] = InvitationStatus.valueOf(request.status)
                            val newRole = request.role
                            if (newRole != null) {
                                it[role] = Role.valueOf(newRole)
                            }
                        }
                    }

                    call.respond(HttpStatusCode.OK, mapOf("residenceId" to residenceId, "targetUserId" to targetUserId, "status" to "Statut mis à jour"))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la mise à jour du membre : ${e.message}")
                    )
                }
            }

            // PUT /api/residences/{id}/members/{user_id}/status : Accept/Decline invitation or update status
            put("/{id}/members/{user_id}/status") {
                val principal = call.principal<JWTPrincipal>()
                val callerId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                val residenceId = call.parameters["id"] ?: ""
                val targetUserId = call.parameters["user_id"] ?: ""

                try {
                    val request = call.receive<MemberStatusUpdateRequest>()

                    // 1. Fetch the target member's current status and role
                    val existingMember = transaction {
                        ResidenceMembers
                            .select(ResidenceMembers.status, ResidenceMembers.role)
                            .where {
                                (ResidenceMembers.userId eq UUID.fromString(targetUserId)) and
                                        (ResidenceMembers.residenceId eq UUID.fromString(residenceId))
                            }
                            .singleOrNull()
                    }

                    if (existingMember == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Aucune invitation ou inscription trouvée pour cet utilisateur."))
                        return@put
                    }

                    // 2. Validate permissions: Only OWNER, ADMIN, or the INVITED user themselves can modify it
                    val isSelf = callerId == targetUserId

                    if (!isSelf) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Accès interdit : Seul l'utilisateur invité lui-même peut modifier le statut de cette invitation."))
                        return@put
                    }

                    // 3. Perform update
                    transaction {
                        ResidenceMembers.update({
                            (ResidenceMembers.userId eq UUID.fromString(targetUserId)) and
                                    (ResidenceMembers.residenceId eq UUID.fromString(residenceId))
                        }) {
                            it[status] = InvitationStatus.valueOf(request.status)
                        }
                    }

                    call.respond(HttpStatusCode.OK, mapOf(
                        "residenceId" to residenceId,
                        "userId" to targetUserId,
                        "status" to request.status,
                        "message" to "Le statut de l'invitation a été mis à jour avec succès !"
                    ))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Erreur lors de la mise à jour de l'invitation : ${e.message}")
                    )
                }
            }
        }

    }
}