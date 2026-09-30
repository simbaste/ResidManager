package com.resid.manager.features.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.DashboardDataDto

@Composable
fun DashboardBentoKpis(
    data: DashboardDataDto,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val cashflowVal = data.netCashflow.toLong()
    val revenuesVal = data.totalRevenuesCollected.toLong()
    val occupancyVal = (data.occupancyRate * 10.0).toInt() / 10.0
    val delinquencyVal = (data.delinquencyRate * 10.0).toInt() / 10.0

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KPI 1: Cashflow Net
        BentoKpiCard(
            title = "CASHFLOW NET",
            value = "$cashflowVal $currencySymbol",
            subValue = "Flux net généré",
            badgeText = if (data.netCashflow >= 0) "Positif" else "Déficitaire",
            badgeColor = if (data.netCashflow >= 0) Color(0xFF006948) else Color(0xFFBA1A1A),
            icon = Icons.Default.Star,
            iconBg = (if (data.netCashflow >= 0) Color(0xFF006948) else Color(0xFFBA1A1A)).copy(alpha = 0.1f),
            iconColor = if (data.netCashflow >= 0) Color(0xFF006948) else Color(0xFFBA1A1A),
            modifier = Modifier.weight(1f)
        )

        // KPI 2: Revenues collected
        BentoKpiCard(
            title = "RECETTES COLLECTÉES",
            value = "$revenuesVal $currencySymbol",
            subValue = "Loyer + électricité encaissés",
            icon = Icons.Default.CheckCircle,
            iconBg = Color(0xFF006948).copy(alpha = 0.1f),
            iconColor = Color(0xFF006948),
            modifier = Modifier.weight(1f)
        )

        // KPI 3: Occupancy Rate
        BentoKpiCard(
            title = "TAUX D'OCCUPATION",
            value = "$occupancyVal %",
            subValue = "Unités habitées",
            progressValue = (data.occupancyRate / 100.0).toFloat(),
            icon = Icons.Default.Home,
            iconBg = Color(0xFF1E3A8A).copy(alpha = 0.1f),
            iconColor = Color(0xFF1E3A8A),
            modifier = Modifier.weight(1f)
        )

        // KPI 4: Delinquency Rate
        BentoKpiCard(
            title = "TAUX D'IMPAYÉS (DÉLINQUANCE)",
            value = "$delinquencyVal %",
            subValue = "Mensualités non soldées",
            badgeText = if (data.delinquencyRate > 10.0) "CRITIQUE" else "Maîtrisé",
            badgeColor = if (data.delinquencyRate > 10.0) Color(0xFFBA1A1A) else Color(0xFF006948),
            icon = Icons.Default.Warning,
            iconBg = (if (data.delinquencyRate > 10.0) Color(0xFFBA1A1A) else Color(0xFF006948)).copy(alpha = 0.1f),
            iconColor = if (data.delinquencyRate > 10.0) Color(0xFFBA1A1A) else Color(0xFF006948),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BentoKpiCard(
    title: String,
    value: String,
    subValue: String,
    badgeText: String? = null,
    badgeColor: Color = Color.Transparent,
    progressValue: Float? = null,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(150.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }

                if (badgeText != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (progressValue != null) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressValue)
                                .fillMaxHeight()
                                .background(Color(0xFF006948))
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
