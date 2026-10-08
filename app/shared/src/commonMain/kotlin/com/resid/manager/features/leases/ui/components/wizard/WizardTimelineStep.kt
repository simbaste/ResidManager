package com.resid.manager.features.leases.ui.components.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.ui.components.AppDatePickerField
import com.resid.manager.ui.components.DatePickerMode

@Composable
fun WizardTimelineStep(
    draftStartDate: String,
    draftEndDate: String,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Étape 4 sur 4 : Calendrier & Durée", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        AppDatePickerField(
            mode = DatePickerMode.RANGE,
            startDate = draftStartDate,
            endDate = draftEndDate,
            onDateRangeSelected = { start, end ->
                onStartDateChanged(start)
                onEndDateChanged(end)
            },
            label = "Période du bail *",
            modifier = Modifier.fillMaxWidth()
        )

        val computedDuration = remember(draftStartDate, draftEndDate) {
            try {
                val startParts = draftStartDate.split("-")
                val endParts = draftEndDate.split("-")
                val startYear = startParts[0].toInt()
                val startMonth = startParts[1].toInt()
                val endYear = endParts[0].toInt()
                val endMonth = endParts[1].toInt()
                val totalMonths = (endYear - startYear) * 12 + (endMonth - startMonth)
                if (totalMonths > 0) totalMonths else -1
            } catch (_: Exception) {
                -1
            }
        }

        if (computedDuration > 0) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    text = "Durée calculée du contrat : $computedDuration mois",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
