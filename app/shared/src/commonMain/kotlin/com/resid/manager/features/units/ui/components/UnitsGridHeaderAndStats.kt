package com.resid.manager.features.units.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UnitStatusDto
import com.resid.manager.ui.theme.ResidTheme

@Composable
fun UnitsStatsRow(
    isCompact: Boolean,
    units: List<ResidenceUnitDto>,
    modifier: Modifier = Modifier
) {
    if (isCompact) {
        UnitsStatsRowCompact(
            units = units,
            modifier = modifier,
        )
    } else {
        UnitsStatsRowDefault(
            units = units,
            modifier = modifier,
        )
    }
}

@Composable
fun UnitsStatsRowDefault(
    units: List<ResidenceUnitDto>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val total = units.size
        val available = units.count { it.status == UnitStatusDto.AVAILABLE }
        val occupied = units.count { it.status == UnitStatusDto.OCCUPIED }

        BentoMiniStatCard(title = "Total Unités", value = total.toString().padStart(2, '0'), borderColor = Color(0xFF8E9193))
        BentoMiniStatCard(title = "Disponibles", value = available.toString().padStart(2, '0'), borderColor = Color(0xFF006948))
        BentoMiniStatCard(title = "Occupés", value = occupied.toString().padStart(2, '0'), borderColor = Color(0xFF3F465C))
        BentoMiniStatCard(title = "En Retard", value = "00", borderColor = Color(0xFFBA1A1A))
    }
}

@Composable
fun UnitsStatsRowCompact(
    units: List<ResidenceUnitDto>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        val total = units.size
        val available = units.count { it.status == UnitStatusDto.AVAILABLE }
        val occupied = units.count { it.status == UnitStatusDto.OCCUPIED }

        BentoMiniStatCard(title = "Total Unités", value = total.toString().padStart(2, '0'), borderColor = Color(0xFF8E9193))
        BentoMiniStatCard(title = "Disponibles", value = available.toString().padStart(2, '0'), borderColor = Color(0xFF006948))
        BentoMiniStatCard(title = "Occupés", value = occupied.toString().padStart(2, '0'), borderColor = Color(0xFF3F465C))
        BentoMiniStatCard(title = "En Retard", value = "00", borderColor = Color(0xFFBA1A1A))
    }
}

@PreviewLightDark
@Composable
fun UnitsStatsRowPreview() {
    ResidTheme {
        UnitsStatsRow(
            isCompact = true,
            units = listOf(
                ResidenceUnitDto(
                    id = "123",
                    residenceId = "12345",
                    name = "Logement 1",
                    floor = "1",
                    type = "Appartement",
                    nominalRent = 500.0,
                    serviceCharges = 100.0,
                    initialElectricityIndex = 0.0,
                    status = UnitStatusDto.OCCUPIED
                ),
            )
        )
    }
}

@Composable
fun BentoMiniStatCard(
    title: String,
    value: String,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
