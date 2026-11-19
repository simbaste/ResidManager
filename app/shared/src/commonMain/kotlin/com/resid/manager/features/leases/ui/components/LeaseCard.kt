package com.resid.manager.features.leases.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.InvitationStatusDto
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.RoleDto
import com.resid.manager.dto.UnitStatusDto
import com.resid.manager.ui.components.toFormattedAmount
import com.resid.manager.ui.theme.ResidTheme

private data class LeaseStatusVisual(
    val accentColor: Color,
    val tabBorderColor: Color,
    val tabIcon: ImageVector,
    val badgeBg: Color,
    val badgeBorder: Color,
    val badgeText: String,
    val actionButtonText: String,
    val actionButtonIcon: ImageVector,
    val isPrimaryAction: Boolean
)

@Composable
private fun getLeaseStatusVisual(status: LeaseStatusDto): LeaseStatusVisual {
    val emerald = MaterialTheme.colorScheme.primary
    val amber = Color(0xFFF59E0B)
    val red = MaterialTheme.colorScheme.error
    val slate = MaterialTheme.colorScheme.secondary

    return when (status) {
        LeaseStatusDto.DRAFT -> LeaseStatusVisual(
            accentColor = amber,
            tabBorderColor = amber.copy(alpha = 0.4f),
            tabIcon = Icons.Default.Edit,
            badgeBg = amber.copy(alpha = 0.12f),
            badgeBorder = amber.copy(alpha = 0.35f),
            badgeText = "BROUILLON",
            actionButtonText = "Reprendre",
            actionButtonIcon = Icons.Default.Edit,
            isPrimaryAction = true
        )
        LeaseStatusDto.SIGNED_ACTIVE -> LeaseStatusVisual(
            accentColor = emerald,
            tabBorderColor = emerald.copy(alpha = 0.4f),
            tabIcon = Icons.Default.FolderOpen,
            badgeBg = emerald.copy(alpha = 0.12f),
            badgeBorder = emerald.copy(alpha = 0.35f),
            badgeText = "EN COURS",
            actionButtonText = "Consulter",
            actionButtonIcon = Icons.Default.FolderOpen,
            isPrimaryAction = false
        )
        LeaseStatusDto.PENDING_SIGNATURE -> LeaseStatusVisual(
            accentColor = amber,
            tabBorderColor = amber.copy(alpha = 0.45f),
            tabIcon = Icons.Default.FolderSpecial,
            badgeBg = amber.copy(alpha = 0.12f),
            badgeBorder = amber.copy(alpha = 0.4f),
            badgeText = "EN ATTENTE SIGNATURE",
            actionButtonText = "Finaliser",
            actionButtonIcon = Icons.AutoMirrored.Filled.ArrowForward,
            isPrimaryAction = true
        )
        LeaseStatusDto.PENDING_PAYMENT -> LeaseStatusVisual(
            accentColor = red,
            tabBorderColor = red.copy(alpha = 0.4f),
            tabIcon = Icons.Default.Folder,
            badgeBg = red.copy(alpha = 0.12f),
            badgeBorder = red.copy(alpha = 0.35f),
            badgeText = "EN ATTENTE PAIEMENT",
            actionButtonText = "Régulariser",
            actionButtonIcon = Icons.AutoMirrored.Filled.ArrowForward,
            isPrimaryAction = true
        )
        LeaseStatusDto.DOWN_PAYMENT_PAID, LeaseStatusDto.PARTIALLY_PAID -> LeaseStatusVisual(
            accentColor = amber,
            tabBorderColor = amber.copy(alpha = 0.45f),
            tabIcon = Icons.Default.FolderSpecial,
            badgeBg = amber.copy(alpha = 0.12f),
            badgeBorder = amber.copy(alpha = 0.4f),
            badgeText = "ACOMPTE ENREGISTRÉ",
            actionButtonText = "Finaliser",
            actionButtonIcon = Icons.AutoMirrored.Filled.ArrowForward,
            isPrimaryAction = true
        )
        LeaseStatusDto.TERMINATED -> LeaseStatusVisual(
            accentColor = slate,
            tabBorderColor = slate.copy(alpha = 0.35f),
            tabIcon = Icons.Default.Archive,
            badgeBg = slate.copy(alpha = 0.12f),
            badgeBorder = slate.copy(alpha = 0.3f),
            badgeText = "CLÔTURÉ",
            actionButtonText = "Historique",
            actionButtonIcon = Icons.Default.Folder,
            isPrimaryAction = false
        )
    }
}

@Composable
fun LeaseCard(
    modifier: Modifier = Modifier,
    lease: LeaseDto,
    matchedUnit: ResidenceUnitDto?,
    matchedTenant: ResidenceMemberSummaryDto?,
    currencySymbol: String = AppCurrency.DEFAULT.symbol,
    onClick: () -> Unit,
) {
    val visual = getLeaseStatusVisual(lease.status)
    val unitName = matchedUnit?.name ?: "Logement ${lease.residenceUnitId.take(5)}"
    val folderLabel = "Dossier $unitName"
    val contractId = "#CTR-${lease.id.takeLast(4).uppercase()}"

    val tenantFullName = matchedTenant?.let { "${it.firstName} ${it.lastName}".trim() }
        ?.takeIf { it.isNotBlank() } ?: "Locataire non assigné"
    val tenantInitials = matchedTenant?.let {
        "${it.firstName.firstOrNull() ?: ""}${it.lastName.firstOrNull() ?: ""}".uppercase()
    }?.takeIf { it.isNotBlank() } ?: unitName.take(2).uppercase()

    val unitSubtext = buildString {
        if (!matchedUnit?.type.isNullOrBlank()) append(matchedUnit.type)
        if (!matchedUnit?.floor.isNullOrBlank()) {
            if (isNotEmpty()) append(" • ")
            append("Étage ${matchedUnit.floor}")
        }
        if (isEmpty()) append("Unité résidentielle")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // --- 1. Folder Tab Header Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Physical Folder Tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                    .border(
                        BorderStroke(1.dp, visual.tabBorderColor),
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = visual.tabIcon,
                        contentDescription = null,
                        tint = visual.accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = folderLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = visual.accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = contractId,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Status Badge Chip
            Box(
                modifier = Modifier
                    .padding(bottom = 3.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(visual.badgeBg)
                    .border(BorderStroke(1.dp, visual.badgeBorder), RoundedCornerShape(9999.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(visual.accentColor)
                    )
                    Text(
                        text = visual.badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = visual.accentColor
                    )
                }
            }
        }

        // --- 2. Folder Envelope Card Body ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            border = BorderStroke(1.dp, visual.tabBorderColor)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top accent gradient line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    visual.accentColor,
                                    visual.accentColor.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Tenant Profile Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Avatar Pill / Monogram
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        BorderStroke(1.dp, visual.accentColor.copy(alpha = 0.35f)),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tenantInitials,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = visual.accentColor
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = tenantFullName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = unitSubtext,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Financial Bento Grid (2 Columns: Monthly Rent & Deposit)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .border(
                                BorderStroke(1.dp, visual.tabBorderColor.copy(alpha = 0.25f)),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LOYER MENSUEL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = (lease.monthlyRentAtSign ?: 0.0).toFormattedAmount(currencySymbol),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = visual.accentColor
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lease.status == LeaseStatusDto.TERMINATED) "CAUTION RESTITUÉE" else "CAUTION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = (lease.depositAmount ?: 0.0).toFormattedAmount(currencySymbol),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Date Period Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = visual.accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${lease.startDate}  →  ${lease.endDate}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Text(
                            text = if (lease.status == LeaseStatusDto.TERMINATED) "Archivé" else "Période active",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Bottom Action Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Action Button Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (visual.isPrimaryAction) visual.accentColor.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (visual.isPrimaryAction) visual.accentColor.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onClick() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = visual.actionButtonIcon,
                                    contentDescription = null,
                                    tint = if (visual.isPrimaryAction) visual.accentColor else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = visual.actionButtonText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (visual.isPrimaryAction) visual.accentColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Utility icon buttons (Download & Print)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onClick,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Télécharger",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onClick,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "Imprimer",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Empty / Add Folder creator card styled after the Stitch Modern HTML redesign.
 */
@Composable
fun LeaseAddFolderCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emerald = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Tab Header for Add Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = emerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Nouveau Dossier",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Dashed / Outlined Folder Envelope Body
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(emerald.copy(alpha = 0.12f))
                        .border(BorderStroke(1.dp, emerald.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = emerald,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Créer un Nouveau Bail",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Chemise locative pour lot vacant, locataire & clauses du bail.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun LeaseCardPreview() {
    ResidTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LeaseCard(
                lease = LeaseDto(
                    id = "202601",
                    residenceUnitId = "unit-1",
                    tenantId = "tenant-1",
                    startDate = "01/11/26",
                    endDate = "01/11/27",
                    monthlyRentAtSign = 500.0,
                    depositAmount = 500.0,
                    status = LeaseStatusDto.SIGNED_ACTIVE,
                    createdAt = "2026-01-01",
                    updatedAt = "2026-01-01"
                ),
                matchedUnit = ResidenceUnitDto(
                    id = "unit-1",
                    residenceId = "res-1",
                    name = "A1",
                    floor = "RDC",
                    type = "Studio",
                    nominalRent = 500.0,
                    serviceCharges = 50.0,
                    initialElectricityIndex = 0.0,
                    status = UnitStatusDto.OCCUPIED
                ),
                matchedTenant = ResidenceMemberSummaryDto(
                    userId = "tenant-1",
                    firstName = "Herve",
                    lastName = "Nsigne",
                    email = "herve.nsigne@example.com",
                    phone = "+33612345678",
                    roleDto = RoleDto.TENANT,
                    status = InvitationStatusDto.ACCEPTED
                ),
                currencySymbol = "€",
                onClick = {}
            )

            LeaseAddFolderCard(onClick = {})
        }
    }
}


