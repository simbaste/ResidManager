package com.resid.manager.features.units.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UnitStatusDto

@Composable
fun UnitsGridHeader(
    isAuthorized: Boolean,
    onAddUnitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Gestion des Logements",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF006948)
            )
            Text(
                text = "Visualisez et gérez l'état d'occupation de vos unités immobilières.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        if (isAuthorized) {
            Button(
                onClick = onAddUnitClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Text("Ajouter un logement", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun UnitsStatsRow(
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

        BentoMiniStatCard(title = "Total Unités", value = total.toString().padStart(2, '0'), borderColor = Color(0xFF8E9193), modifier = Modifier.weight(1f))
        BentoMiniStatCard(title = "Disponibles", value = available.toString().padStart(2, '0'), borderColor = Color(0xFF006948), modifier = Modifier.weight(1f))
        BentoMiniStatCard(title = "Occupés", value = occupied.toString().padStart(2, '0'), borderColor = Color(0xFF3F465C), modifier = Modifier.weight(1f))
        BentoMiniStatCard(title = "En Retard", value = "00", borderColor = Color(0xFFBA1A1A), modifier = Modifier.weight(1f))
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxSize(),
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
