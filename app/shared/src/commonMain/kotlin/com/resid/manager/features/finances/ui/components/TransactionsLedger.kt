package com.resid.manager.features.finances.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.FinanceTransactionDto
import com.resid.manager.dto.TransactionCategoryDto
import com.resid.manager.dto.TransactionTypeDto

@Composable
fun TransactionsLedger(
    transactions: List<FinanceTransactionDto>,
    isLoading: Boolean,
    onTraceabilityClick: (Pair<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF006948))
            }
        } else if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Aucune transaction trouvée pour ces filtres.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                Text(
                    text = "Grand Livre Chronologique des Flux",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF006948)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Header row of the ledger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text("Date", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text("Catégorie", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text("Description & Liens", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(2f))
                    Text("Montant", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1.2f))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    transactions.forEach { tx ->
                        val isIncome = tx.type == TransactionTypeDto.INCOME

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tx.transactionDate, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(
                                text = when (tx.category) {
                                    TransactionCategoryDto.CLEANING -> "Nettoyage"
                                    TransactionCategoryDto.FUEL -> "Carburant"
                                    TransactionCategoryDto.SECURITY -> "Sécurité"
                                    TransactionCategoryDto.MAINTENANCE -> "Maintenance"
                                    TransactionCategoryDto.TAXES -> "Impôts"
                                    TransactionCategoryDto.DEPOSIT -> "Caution Recue"
                                    TransactionCategoryDto.RENT -> "Loyer Recu"
                                    TransactionCategoryDto.ELECTRICITY -> "Électricité"
                                    else -> tx.category.name
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                modifier = Modifier.weight(2f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tx.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))

                                if (tx.relatedEntityType != null && tx.relatedEntityId != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E7FF)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.clickable {
                                            onTraceabilityClick(tx.relatedEntityType.name to tx.relatedEntityId)
                                        }
                                    ) {
                                        Text(
                                            text = "Lien ${tx.relatedEntityType}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF1E3A8A),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isIncome) "+ ${tx.amount} XOF" else "- ${tx.amount} XOF",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isIncome) Color(0xFF006948) else Color(0xFFBA1A1A),
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}
