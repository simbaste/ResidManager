package com.resid.manager.features.dashboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.resid.manager.ui.components.AppDatePickerField
import com.resid.manager.ui.components.DatePickerMode
import com.resid.manager.ui.theme.ResidTheme

@Composable
fun DashboardPeriodFilterBar(
    isCompact: Boolean,
    periodFilter: String,
    customStartText: String,
    customEndText: String,
    onPeriodFilterChanged: (String) -> Unit,
    onCustomStartChanged: (String) -> Unit,
    onCustomEndChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Période de Consolidation Analytique",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (isCompact) {
                DashboardPeriodFilterBarCompact(
                    periodFilter = periodFilter,
                    customStartText = customStartText,
                    customEndText = customEndText,
                    onPeriodFilterChanged = onPeriodFilterChanged,
                    onCustomStartChanged = onCustomStartChanged,
                    onCustomEndChanged = onCustomEndChanged
                )
             } else {
                 DashboardPeriodFilterBarDefault(
                     periodFilter = periodFilter,
                     customStartText = customStartText,
                     customEndText = customEndText,
                     onPeriodFilterChanged = onPeriodFilterChanged,
                     onCustomStartChanged = onCustomStartChanged,
                     onCustomEndChanged = onCustomEndChanged
                 )
            }
        }
    }
}

@Composable
fun DashboardPeriodFilterBarCompact(
    periodFilter: String,
    customStartText: String,
    customEndText: String,
    onPeriodFilterChanged: (String) -> Unit,
    onCustomStartChanged: (String) -> Unit,
    onCustomEndChanged: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Filter chips Row
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "MONTH" to "Ce Mois",
                "QUARTER" to "Trimestre",
                "YEAR" to "Année Civile",
                "CUSTOM" to "Personnalisé"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = periodFilter == key,
                    onClick = { onPeriodFilterChanged(key) },
                    label = { Text(label) }
                )
            }
        }

        // Custom Date inputs with AppDatePickerField
        if (periodFilter == "CUSTOM") {
            AppDatePickerField(
                mode = DatePickerMode.RANGE,
                startDate = customStartText,
                endDate = customEndText,
                onDateRangeSelected = { start, end ->
                    onCustomStartChanged(start)
                    onCustomEndChanged(end)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DashboardPeriodFilterBarDefault(
    periodFilter: String,
    customStartText: String,
    customEndText: String,
    onPeriodFilterChanged: (String) -> Unit,
    onCustomStartChanged: (String) -> Unit,
    onCustomEndChanged: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center
    ) {
        // Filter chips Row
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "MONTH" to "Ce Mois",
                "QUARTER" to "Trimestre",
                "YEAR" to "Année Civile",
                "CUSTOM" to "Personnalisé"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = periodFilter == key,
                    onClick = { onPeriodFilterChanged(key) },
                    label = { Text(label) }
                )
            }
        }

        // Custom Date inputs with AppDatePickerField
        if (periodFilter == "CUSTOM") {
            AppDatePickerField(
                mode = DatePickerMode.RANGE,
                startDate = customStartText,
                endDate = customEndText,
                onDateRangeSelected = { start, end ->
                    onCustomStartChanged(start)
                    onCustomEndChanged(end)
                },
                modifier = Modifier.width(280.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
fun DashboardPeriodFilterBarPreview() {
    ResidTheme {
        DashboardPeriodFilterBar(
            isCompact = true,
            periodFilter = "MONTH",
            customStartText = "2025-02-01",
            customEndText = "2025-02-28",
            onPeriodFilterChanged = {},
            onCustomStartChanged = {},
            onCustomEndChanged = {}
        )
    }
}
