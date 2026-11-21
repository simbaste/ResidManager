package com.resid.manager.features.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.DashboardDataDto

@Composable
fun DashboardChartsSection(
    isCompact: Boolean,
    data: DashboardDataDto?,
    onViewReportsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isCompact) {
        DashboardChartSectionCompact(
            modifier = modifier,
            data = data,
            onViewReportsClick = onViewReportsClick
        )
    } else {
        DashboardChartSectionDefault(
            modifier = modifier,
            data = data,
            onViewReportsClick = onViewReportsClick
        )
    }
}

@Composable
fun DashboardChartSectionCompact(
    modifier: Modifier = Modifier,
    data: DashboardDataDto?,
    onViewReportsClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Analyse des flux mensuels
        Card(
            modifier = Modifier
                .widthIn(min = 320.dp, max = 420.dp)
                .height(320.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Analyse des flux mensuels", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF006948))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Mois", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Année", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Dynamic Chart visualization based on fetched revenues
                val flowPercentage = if (data != null && data.totalRevenuesCollected > 0.0) {
                    (data.netCashflow / data.totalRevenuesCollected).toFloat()
                } else 0.5f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val heights = listOf(0.40f, 0.65f, 0.55f, maxOf(0.1f, flowPercentage), 0.75f)
                    val colors = listOf(
                        Color(0xFF006948).copy(alpha = 0.1f),
                        Color(0xFF006948).copy(alpha = 0.2f),
                        Color(0xFF006948).copy(alpha = 0.4f),
                        Color(0xFF006948).copy(alpha = 0.7f),
                        Color(0xFF006948)
                    )

                    heights.forEachIndexed { i, h ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(h)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(colors[i])
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Jan", "Fév", "Mar", "Avr", "Mai").forEach { m ->
                        Text(
                            text = m,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Card 2: Call to action optimization
        Card(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 320.dp)
                .height(320.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF006948).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = Color(0xFF006948),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Prêt à optimiser ?", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Consultez vos rapports détaillés pour identifier de nouvelles opportunités de croissance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onViewReportsClick,
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text("Voir les rapports", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun DashboardChartSectionDefault(
    modifier: Modifier = Modifier,
    data: DashboardDataDto?,
    onViewReportsClick: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Card 1: Analyse des flux mensuels
        Card(
            modifier = Modifier.weight(1.2f).height(320.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(text = "Analyse des flux mensuels", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF006948))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Mois", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Année", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Dynamic Chart visualization based on fetched revenues
                val flowPercentage = if (data != null && data.totalRevenuesCollected > 0.0) {
                    (data.netCashflow / data.totalRevenuesCollected).toFloat()
                } else 0.5f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val heights = listOf(0.40f, 0.65f, 0.55f, maxOf(0.1f, flowPercentage), 0.75f)
                    val colors = listOf(
                        Color(0xFF006948).copy(alpha = 0.1f),
                        Color(0xFF006948).copy(alpha = 0.2f),
                        Color(0xFF006948).copy(alpha = 0.4f),
                        Color(0xFF006948).copy(alpha = 0.7f),
                        Color(0xFF006948)
                    )

                    heights.forEachIndexed { i, h ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(h)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(colors[i])
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Jan", "Fév", "Mar", "Avr", "Mai").forEach { m ->
                        Text(
                            text = m,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Card 2: Call to action optimization
        Card(
            modifier = Modifier.weight(0.8f).height(320.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF006948).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = Color(0xFF006948),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Prêt à optimiser ?", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Consultez vos rapports détaillés pour identifier de nouvelles opportunités de croissance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onViewReportsClick,
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text("Voir les rapports", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun DashboardChartsSectionPreview() {
    val sampleData = DashboardDataDto(
        residenceId = "residence-1",
        totalRevenuesCollected = 1250000.0,
        totalExpensesIncurred = 450000.0,
        netCashflow = 800000.0,
        delinquencyRate = 4.5,
        occupancyRate = 92.0
    )
    DashboardChartsSection(
        isCompact = true,
        data = sampleData,
        onViewReportsClick = {}
    )
}
