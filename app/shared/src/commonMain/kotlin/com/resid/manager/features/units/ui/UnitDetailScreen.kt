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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.features.units.ui.components.UnitDetailHeader
import com.resid.manager.features.units.ui.components.UnitDetailMediaGallery
import com.resid.manager.features.units.ui.components.UnitDetailTechSpecs
import com.resid.manager.features.units.ui.components.UnitDetailTenantAndStats

@Composable
fun UnitDetailScreen(
    residenceUnit: ResidenceUnitDto,
    activeResidence: ResidenceContext?,
    isAuthorized: Boolean,
    leases: List<LeaseDto>,
    members: List<ResidenceMemberSummaryDto>,
    onBackClick: () -> Unit,
    onAssignTenantClick: (String) -> Unit,
    onEditClick: (ResidenceUnitDto) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeLease = leases.firstOrNull { it.residenceUnitId == residenceUnit.id }
    val activeTenant = activeLease?.let { lease -> members.firstOrNull { it.userId == lease.tenantId } }

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            UnitDetailTechSpecs(
                residenceUnit = residenceUnit,
                modifier = Modifier.weight(7f),
            )

            UnitDetailTenantAndStats(
                residenceUnit = residenceUnit,
                activeLease = activeLease,
                activeTenant = activeTenant,
                isAuthorized = isAuthorized,
                onAssignTenantClick = { onAssignTenantClick(residenceUnit.id) },
                onEditClick = { onEditClick(residenceUnit) },
                onDeleteClick = { onDeleteClick(residenceUnit.id) },
                modifier = Modifier.weight(5f),
            )
        }

        UnitDetailMediaGallery()
    }
}
