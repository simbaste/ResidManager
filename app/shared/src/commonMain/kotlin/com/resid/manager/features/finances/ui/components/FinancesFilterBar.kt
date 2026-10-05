package com.resid.manager.features.finances.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FinancesFilterBar(
    filterType: String,
    filterQueryText: String,
    onFilterTypeChanged: (String) -> Unit,
    onFilterQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Recherche & Filtres Grand Livre", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flow toggles
                Row(modifier = Modifier.weight(1f)) {
                    listOf("ALL" to "Tous", "INCOME" to "Revenus (+)", "EXPENSE" to "Dépenses (-)").forEach { (key, label) ->
                        FilterChip(
                            selected = filterType == key,
                            onClick = { onFilterTypeChanged(key) },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                // Keyword description search
                com.resid.manager.ui.components.AppTextField(
                    value = filterQueryText,
                    onValueChange = onFilterQueryChanged,
                    label = "Rechercher dans description...",
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
