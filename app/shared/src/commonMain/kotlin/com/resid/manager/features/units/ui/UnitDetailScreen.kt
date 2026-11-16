package com.resid.manager.features.units.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.EquipmentDto
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidencePhotoDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UnitStatusDto
import com.resid.manager.dto.UserRole
import com.resid.manager.features.residences.usecase.PhotoUploadItem
import com.resid.manager.features.units.ui.components.UnitDetailHeader
import com.resid.manager.features.units.ui.components.UnitDetailTechSpecs
import com.resid.manager.features.units.ui.components.UnitDetailTenantAndStats
import com.resid.manager.features.units.ui.components.UnitPhotoCarousel
import com.resid.manager.ui.components.AppGalleryDialog
import com.resid.manager.ui.components.GalleryPhotoItem
import com.resid.manager.ui.theme.ResidTheme

@Composable
fun UnitDetailScreen(
    modifier: Modifier = Modifier,
    residenceUnit: ResidenceUnitDto,
    activeResidence: ResidenceContext?,
    isAuthorized: Boolean,
    leases: List<LeaseDto>,
    members: List<ResidenceMemberSummaryDto>,
    photos: List<ResidencePhotoDto> = emptyList(),
    isSavingPhotos: Boolean = false,
    errorMessage: String? = null,
    onBackClick: () -> Unit,
    onAssignTenantClick: (String) -> Unit,
    onEditClick: (ResidenceUnitDto) -> Unit,
    onDeleteClick: (String) -> Unit,
    onSavePhotos: (List<PhotoUploadItem>) -> Unit = {},
) {
    val activeLease = leases.firstOrNull { it.residenceUnitId == residenceUnit.id }
    val activeTenant = activeLease?.let { lease -> members.firstOrNull { it.userId == lease.tenantId } }

    var showGalleryDialog by remember { mutableStateOf(false) }

    val photoUrls = remember(photos, residenceUnit.photoUrl) {
        if (photos.isNotEmpty()) {
            photos.map { it.url }
        } else if (!residenceUnit.photoUrl.isNullOrBlank()) {
            listOf(residenceUnit.photoUrl)
        } else {
            emptyList()
        }
    }

    val photoDescriptions = remember(photos) {
        if (photos.isNotEmpty()) {
            photos.map { it.description }
        } else {
            listOf("Photo principale")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        UnitDetailHeader(
            residenceUnit = residenceUnit,
            residenceName = activeResidence?.residenceName,
            onBackClick = onBackClick,
        )

        // Carousel Photos interactif avec Lightbox plein écran au clic
        UnitPhotoCarousel(
            unitName = residenceUnit.name,
            photos = photoUrls,
            photoDescriptions = photoDescriptions,
            isAuthorized = isAuthorized,
            onEditGalleryClick = { showGalleryDialog = true }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val effectiveCurrencySymbol = activeResidence?.currencySymbol ?: com.resid.manager.dto.AppCurrency.DEFAULT.symbol

            UnitDetailTechSpecs(
                residenceUnit = residenceUnit,
                currencySymbol = effectiveCurrencySymbol,
                modifier = Modifier.weight(7f),
            )

            UnitDetailTenantAndStats(
                residenceUnit = residenceUnit,
                activeLease = activeLease,
                activeTenant = activeTenant,
                isAuthorized = isAuthorized,
                currencySymbol = effectiveCurrencySymbol,
                onAssignTenantClick = { onAssignTenantClick(residenceUnit.id) },
                onEditClick = { onEditClick(residenceUnit) },
                onDeleteClick = { onDeleteClick(residenceUnit.id) },
                modifier = Modifier.weight(5f),
            )
        }
    }

    if (showGalleryDialog) {
        val initialItems = remember(photos, residenceUnit.photoUrl) {
            if (photos.isNotEmpty()) {
                photos.mapIndexed { idx, p ->
                    GalleryPhotoItem(
                        id = "unit_photo_$idx",
                        url = p.url,
                        description = p.description
                    )
                }
            } else if (!residenceUnit.photoUrl.isNullOrBlank()) {
                listOf(
                    GalleryPhotoItem(
                        id = "primary",
                        url = residenceUnit.photoUrl,
                        description = "Photo principale"
                    )
                )
            } else {
                emptyList()
            }
        }

        val unitTags = listOf("Pièce principale", "Chambre", "Cuisine", "Salle de bain", "Balcon / Terrasse", "Entrée", "Vue extérieure")

        AppGalleryDialog(
            title = "Galerie du logement - ${residenceUnit.name}",
            initialPhotos = initialItems,
            suggestedTags = unitTags,
            defaultDescription = "Logement",
            isLoading = isSavingPhotos,
            errorMessage = errorMessage,
            onDismiss = { showGalleryDialog = false },
            onSave = { items ->
                val uploadItems = items.map {
                    PhotoUploadItem(
                        url = it.url,
                        bytes = it.bytes,
                        description = it.description
                    )
                }
                onSavePhotos(uploadItems)
                showGalleryDialog = false
            }
        )
    }
}

@PreviewLightDark
@Composable
fun UnitDetailScreenPreview() {
    ResidTheme {
        UnitDetailScreen(
            residenceUnit = ResidenceUnitDto(
                id = "123",
                residenceId = "12345",
                name = "Logement 1",
                floor = "1",
                type = "Appartement",
                nominalRent = 500.0,
                serviceCharges = 100.0,
                initialElectricityIndex = 0.0,
                equipments = listOf(
                    EquipmentDto(
                        id = "12",
                        key = "clim",
                        label = "Climatisé"
                    ),
                    EquipmentDto(
                        id = "34",
                        key = "Fibre",
                        label = "Fibre incluse"
                    ),
                    EquipmentDto(
                        id = "56",
                        key = "eau",
                        label = "Eau autonome"
                    ),
                ),
                status = UnitStatusDto.OCCUPIED
            ),
            activeResidence = ResidenceContext(
                residenceId = "12345",
                residenceName = "Residence A",
                currencySymbol = "€",
                userRoleInResidence = UserRole.OWNER,
                residenceAddress = "1 reu du test"
            ),
            isAuthorized = true,
            leases = emptyList(),
            members = emptyList(),
            photos = emptyList(),
            onBackClick = { },
            onAssignTenantClick = {  },
            onEditClick = { },
            onDeleteClick = { },
            onSavePhotos = { }
        )
    }
}
