package com.resid.manager.features.units.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Adaptatif Light/Dark mode
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 600.dp
            val effectiveCurrencySymbol = activeResidence?.currencySymbol ?: com.resid.manager.dto.AppCurrency.DEFAULT.symbol
            
            // Adjust padding and spacing based on screen size
            val horizontalPadding = if (isCompact) 16.dp else 32.dp
            val verticalPadding = if (isCompact) 16.dp else 24.dp
            val contentSpacing = if (isCompact) 16.dp else 24.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(contentSpacing),
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
                    isCompact = isCompact,
                    isAuthorized = isAuthorized,
                    onEditGalleryClick = { showGalleryDialog = true }
                )

                if (isCompact) {
                    var selectedTabIndex by remember { mutableStateOf(0) }
                    
                    MobileSegmentedTabs(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = { selectedTabIndex = it }
                    )

                    when (selectedTabIndex) {
                        0 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                UnitFinancialOverviewCard(
                                    residenceUnit = residenceUnit,
                                    currencySymbol = effectiveCurrencySymbol
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
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        1 -> {
                            UnitDetailTechSpecs(
                                residenceUnit = residenceUnit,
                                currencySymbol = effectiveCurrencySymbol,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    UnitFinancialOverviewCard(
                        residenceUnit = residenceUnit,
                        currencySymbol = effectiveCurrencySymbol
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp), // gap-5
                    ) {
                        // Colonne de gauche: Locataire & Bail (col-span-6)
                        UnitDetailTenantAndStats(
                            residenceUnit = residenceUnit,
                            activeLease = activeLease,
                            activeTenant = activeTenant,
                            isAuthorized = isAuthorized,
                            currencySymbol = effectiveCurrencySymbol,
                            onAssignTenantClick = { onAssignTenantClick(residenceUnit.id) },
                            onEditClick = { onEditClick(residenceUnit) },
                            onDeleteClick = { onDeleteClick(residenceUnit.id) },
                            modifier = Modifier.weight(1f),
                        )
                        
                        // Colonne de droite: Fiche technique (col-span-6)
                        UnitDetailTechSpecs(
                            residenceUnit = residenceUnit,
                            currencySymbol = effectiveCurrencySymbol,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
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

@Composable
fun UnitFinancialOverviewCard(
    residenceUnit: ResidenceUnitDto,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val loyerNet = residenceUnit.nominalRent
    val chargesFixes = residenceUnit.serviceCharges
    val totalMois = loyerNet + chargesFixes

    // On suppose un taux d'occupation fictif ou calculé selon le statut pour l'exemple, 
    // ou récupéré depuis des vraies statistiques si existantes.
    val tauxOccupation = if (residenceUnit.status == UnitStatusDto.OCCUPIED) "100%" else "0%"
    val cumulAnnuelFictif = totalMois * 12

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface, // bg-[#0b1c30]
        shape = RoundedCornerShape(12.dp), // rounded-xl
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.inverseSurface ) // border-[#173352]
    ) {
        Column(
            modifier = Modifier.padding(20.dp) // p-5
        ) {
            // Header: Aperçu Financier
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp) // pb-3
                    .border(androidx.compose.foundation.BorderStroke(1.dp, Color.Transparent))
                    // .border(bottom = 1.dp, color = Color(0xFF142D4A)) -> Il faudrait utiliser un divider ou drawBehind
            ) {
                // On utilise une astuce avec Column pour le trait du bas si Divider n'est pas utilisé directement
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info, // Placeholder pour l'icone
                            contentDescription = null,
                            tint = Color(0xFF34D399), // emerald-400
                            modifier = Modifier.padding(2.dp)
                        )
                        Text(
                            text = "APERÇU FINANCIER & RENTABILITÉ",
                            color = Color(0xFFCBD5E1), // text-slate-300
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Cumul annuel perçu : ",
                            color = Color(0xFF94A3B8), // text-slate-400
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$cumulAnnuelFictif $currencySymbol",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Ligne séparatrice simulée
            HorizontalDivider(color = Color(0xFF142D4A), thickness = 1.dp)

            Spacer(modifier = Modifier.height(14.dp)) // pt-3.5

            // Grille des valeurs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp) // gap-3.5
            ) {
                FinancialMetricBox(
                    label = "Loyer net",
                    value = "$loyerNet $currencySymbol",
                    valueColor = Color(0xFF34D399), // text-emerald-400
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricBox(
                    label = "Charges fixes",
                    value = "$chargesFixes $currencySymbol",
                    valueColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricBox(
                    label = "Total / mois",
                    value = "$totalMois $currencySymbol",
                    valueColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricBox(
                    label = "Taux occupation",
                    value = tauxOccupation,
                    valueColor = Color(0xFF34D399), // text-emerald-400
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FinancialMetricBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF081525), // bg-[#081525]
        shape = RoundedCornerShape(8.dp), // rounded-lg
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF142E4D)) // border-[#142e4d]
    ) {
        Column(
            modifier = Modifier.padding(12.dp) // p-3
        ) {
            Text(
                text = label,
                color = Color(0xFF94A3B8), // text-slate-400
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 20.sp, // text-xl
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 2.dp) // mt-0.5
            )
        }
    }
}

@Composable
fun MobileSegmentedTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Container reproduisant le bg-brand-navySoft et rounded-xl du HTML
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant, // Adaptation thème
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp), // p-1
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Tab 0 : Location
        SegmentedTabItem(
            text = "Location",
            isSelected = selectedTabIndex == 0,
            onClick = { onTabSelected(0) },
            modifier = Modifier.weight(1f)
        )
        
        // Tab 1 : Fiche technique
        SegmentedTabItem(
            text = "Fiche technique",
            isSelected = selectedTabIndex == 1,
            onClick = { onTabSelected(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentedTabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    
    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary, // border-brand-border
            shape = RoundedCornerShape(8.dp) // rounded-lg
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color = backgroundColor)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp), // py-2.5
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp, // text-xs
            fontWeight = FontWeight.SemiBold
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
