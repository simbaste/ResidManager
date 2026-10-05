package com.resid.manager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Reusable and configurable DateRangePickerField component.
 * Displays the selected start and end date, and opens a Material3 DateRangePicker modal when clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerField(
    startDate: String,
    endDate: String,
    onDateRangeSelected: (startDate: String, endDate: String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Période",
    placeholder: String = "Du AAAA-MM-JJ au AAAA-MM-JJ",
    yearRange: IntRange = 2020..2030,
    singleLine: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }

    val initialStartMillis = remember(startDate) { parseDateToUtcMillis(startDate) }
    val initialEndMillis = remember(endDate) { parseDateToUtcMillis(endDate) }

    val displayText = remember(startDate, endDate) {
        when {
            startDate.isNotBlank() && endDate.isNotBlank() -> "$startDate - $endDate"
            startDate.isNotBlank() -> "Du $startDate"
            endDate.isNotBlank() -> "Jusqu'au $endDate"
            else -> ""
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            textStyle = MaterialTheme.typography.bodyMedium,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = singleLine,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Clickable transparent overlay to open dialog easily
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialStartMillis,
            initialSelectedEndDateMillis = initialEndMillis,
            yearRange = yearRange
        )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        val endMillis = dateRangePickerState.selectedEndDateMillis
                        val newStart = startMillis?.let { formatUtcMillisToIsoDate(it) } ?: startDate
                        val newEnd = endMillis?.let { formatUtcMillisToIsoDate(it) } ?: (startMillis?.let { formatUtcMillisToIsoDate(it) } ?: endDate)
                        onDateRangeSelected(newStart, newEnd)
                        showDialog = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null
                ) {
                    Text(
                        text = "Appliquer",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = "Annuler")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "Sélectionner une période",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                },
                headline = {
                    val start = dateRangePickerState.selectedStartDateMillis?.let { formatUtcMillisToIsoDate(it) }
                    val end = dateRangePickerState.selectedEndDateMillis?.let { formatUtcMillisToIsoDate(it) }

                    val rangeHeadline = when {
                        start != null && end != null -> "$start  -  $end"
                        start != null -> "Du $start..."
                        else -> "Choisissez les dates de début et fin"
                    }

                    Text(
                        text = rangeHeadline,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                },
                showModeToggle = true,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
