package com.resid.manager.features.residences.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidencePhotoDto
import com.resid.manager.dto.UserRole
import com.resid.manager.features.residences.ui.components.ResidenceMetricCard
import com.resid.manager.features.residences.ui.components.ResidencePhotoCarousel
import com.resid.manager.ui.theme.ResidTheme

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ResidenceDetailScreen(
    residence: ResidenceContext,
    isActive: Boolean,
    onBackClick: () -> Unit,
    onActivateResidence: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNavigateToUnits: () -> Unit,
    modifier: Modifier = Modifier,
    galleryPhotos: List<ResidencePhotoDto> = emptyList(),
    isOwnerOrAdmin: Boolean = residence.userRoleInResidence == UserRole.OWNER || residence.userRoleInResidence == UserRole.ADMIN,
    onEditGalleryClick: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Breadcrumb Return Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBackClick() }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Retour à l'annuaire des résidences",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 2. Hero Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Left: Title & Badges
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlowRow(
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = residence.residenceName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "RÉSIDENCE ACTIVE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = residence.userRoleInResidence.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                            }

                            // Meta row
                            FlowRow(
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = residence.residenceAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text("•", color = MaterialTheme.colorScheme.outlineVariant)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Tarif : ${residence.kWhPrice} ${residence.currencySymbol} / kWh",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Right: Actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isActive) {
                                OutlinedButton(
                                    onClick = onActivateResidence,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Définir comme active")
                                }
                            }

                            if (isOwnerOrAdmin) {
                                IconButton(
                                    onClick = onEditClick,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Modifier",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onDeleteClick,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = onNavigateToUnits,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MeetingRoom,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Voir les logements", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Photo Gallery Carousel Section
            val photosList = remember(galleryPhotos, residence.photoUrl) {
                if (galleryPhotos.isNotEmpty()) {
                    galleryPhotos.map { it.url }
                } else if (!residence.photoUrl.isNullOrBlank()) {
                    listOf(residence.photoUrl)
                } else {
                    emptyList()
                }
            }
            val photoDescriptionsList = remember(galleryPhotos) {
                galleryPhotos.map { it.description }
            }

            ResidencePhotoCarousel(
                photos = photosList,
                photoDescriptions = photoDescriptionsList,
                residenceName = residence.residenceName,
                onEditGalleryClick = onEditGalleryClick
            )

            // 4. Key Performance Indicators (KPI Cards)
            if (isCompact) {
                IndicatorCompact(residence)
            } else {
                IndicatorDefault(residence)
            }
        }
    }
}

@Composable
fun IndicatorCompact(residence: ResidenceContext) {
    Text(
        text = "Indicateurs clés",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ResidenceMetricCard(
            label = "Total Logements",
            value = "${residence.totalUnits} unités",
            icon = Icons.Default.MeetingRoom,
            subText = "100% opérationnel",
            progress = 1.0f,
        )
        ResidenceMetricCard(
            label = "Taux d'occupation",
            value = "100%",
            icon = Icons.Default.PieChart,
            subText = "${residence.totalUnits} occupés",
            progress = 1.0f,
        )
        ResidenceMetricCard(
            label = "Tarif Électricité",
            value = "${residence.kWhPrice}",
            unit = "${residence.currencySymbol} / kWh",
            icon = Icons.Default.ElectricBolt,
            subText = "Référence compteur",
        )
        ResidenceMetricCard(
            label = "Trousseaux certifiés",
            value = "4",
            unit = "clés",
            icon = Icons.Default.Key,
            subText = "Accès sécurisés",
        )
    }
}

@Composable
fun IndicatorDefault(residence: ResidenceContext) {
    Text(
        text = "Indicateurs clés",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ResidenceMetricCard(
            label = "Total Logements",
            value = "${residence.totalUnits} unités",
            icon = Icons.Default.MeetingRoom,
            subText = "100% opérationnel",
            progress = 1.0f,
            modifier = Modifier.weight(1f)
        )
        ResidenceMetricCard(
            label = "Taux d'occupation",
            value = "100%",
            icon = Icons.Default.PieChart,
            subText = "${residence.totalUnits} occupés",
            progress = 1.0f,
            modifier = Modifier.weight(1f)
        )
        ResidenceMetricCard(
            label = "Tarif Électricité",
            value = "${residence.kWhPrice}",
            unit = "${residence.currencySymbol} / kWh",
            icon = Icons.Default.ElectricBolt,
            subText = "Référence compteur",
            modifier = Modifier.weight(1f)
        )
        ResidenceMetricCard(
            label = "Trousseaux certifiés",
            value = "4",
            unit = "clés",
            icon = Icons.Default.Key,
            subText = "Accès sécurisés",
            modifier = Modifier.weight(1f)
        )
    }
}

@PreviewLightDark
@Composable
fun ResidenceDetailScreenPreview() {
    ResidTheme {
        ResidenceDetailScreen(
            residence = ResidenceContext(
                residenceId = "1",
                residenceName = "Les Jardins d'Ivoire",
                residenceAddress = "Cocody Danga, Abidjan",
                userRoleInResidence = UserRole.OWNER,
                totalUnits = 24,
            ),
            isActive = true,
            onBackClick = {},
            onActivateResidence = {},
            onEditClick = {},
            onDeleteClick = {},
            onNavigateToUnits = {}
        )
    }
}