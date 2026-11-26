package com.resid.manager.features.finances.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.EntityTypeDto
import com.resid.manager.dto.FinanceTransactionDto
import com.resid.manager.dto.TransactionCategoryDto
import com.resid.manager.dto.TransactionTypeDto
import com.resid.manager.helpers.PreviewFinanceTransactionsData
import com.resid.manager.ui.theme.ResidTheme

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun TransactionsLedger(
    modifier: Modifier = Modifier,
    transactions: List<FinanceTransactionDto>,
    isLoading: Boolean,
    isSmallScreen: Boolean,
    currencySymbol: String = AppCurrency.DEFAULT.symbol,
    forcedWindowWidthSizeClass: WindowWidthSizeClass? = null,
    onTraceabilityClick: (Pair<String, String>) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(450.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(450.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Aucune transaction trouvée pour ces filtres.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val windowWidthClass = forcedWindowWidthSizeClass ?: run {
                    val sizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
                    sizeClass.widthSizeClass
                }
                val isExpanded = windowWidthClass == WindowWidthSizeClass.Expanded

                if (isExpanded) {
                    TransactionsTableContent(
                        transactions = transactions,
                        currencySymbol = currencySymbol,
                        isSmallScreen = isSmallScreen,
                        onTraceabilityClick = onTraceabilityClick
                    )
                } else {
                    TransactionsCardsContent(
                        transactions = transactions,
                        currencySymbol = currencySymbol,
                        isSmallScreen = isSmallScreen,
                        onTraceabilityClick = onTraceabilityClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionsTableContent(
    transactions: List<FinanceTransactionDto>,
    currencySymbol: String,
    isSmallScreen: Boolean,
    onTraceabilityClick: (Pair<String, String>) -> Unit,
) {
    Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
        Text(
            text = "Grand Livre Chronologique des Flux",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Header row of the ledger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Text(
                "Date",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Catégorie",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Description & Liens",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(2f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Montant",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1.2f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!isSmallScreen) Modifier.verticalScroll(rememberScrollState()) else Modifier
                )
        ) {
            transactions.forEach { tx ->
                val isIncome = tx.type == TransactionTypeDto.INCOME

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tx.transactionDate,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = transactionCategoryLabel(tx.category),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.weight(2f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tx.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (tx.relatedEntityType != null && tx.relatedEntityId != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.clickable {
                                    onTraceabilityClick(tx.relatedEntityType.name to tx.relatedEntityId)
                                }
                            ) {
                                Text(
                                    text = "Lien ${tx.relatedEntityType}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isIncome) "+ ${tx.amount} $currencySymbol" else "- ${tx.amount} $currencySymbol",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isIncome) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1.2f)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun TransactionsCardsContent(
    transactions: List<FinanceTransactionDto>,
    currencySymbol: String,
    isSmallScreen: Boolean,
    onTraceabilityClick: (Pair<String, String>) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .then(
                if (!isSmallScreen) Modifier.verticalScroll(rememberScrollState()) else Modifier
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        transactions.forEach { tx ->
            TransactionCompactCard(
                tx = tx,
                currencySymbol = currencySymbol,
                onTraceabilityClick = onTraceabilityClick
            )
        }
    }
}

@Composable
private fun TransactionCompactCard(
    tx: FinanceTransactionDto,
    currencySymbol: String,
    onTraceabilityClick: (Pair<String, String>) -> Unit,
) {
    val isIncome = tx.type == TransactionTypeDto.INCOME
    val (badgeBg, badgeText, badgeBorder) = transactionCategoryBadgeColors(tx.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Top Row: Badge + Date (left) and Amount (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (badgeBorder != null) {
                                    Modifier.border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = transactionCategoryLabel(tx.category),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeText
                        )
                    }

                    Text(
                        text = tx.transactionDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (isIncome) "+ ${tx.amount} $currencySymbol" else "- ${tx.amount} $currencySymbol",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // 2. Middle Section: Description
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = tx.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 3. Bottom Footer Row (Conditional / Traceability)
            if (tx.relatedEntityType != null && tx.relatedEntityId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (entityLabel, entityIcon) = when (tx.relatedEntityType) {
                        EntityTypeDto.BAIL -> "Lien Bail #${tx.relatedEntityId}" to Icons.Default.Description
                        EntityTypeDto.ELECTRICITY_STATEMENT -> "Relevé #${tx.relatedEntityId}" to Icons.Default.Bolt
                        EntityTypeDto.TICKET -> "Ticket #${tx.relatedEntityId}" to Icons.Default.Build
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                onTraceabilityClick(tx.relatedEntityType.name to tx.relatedEntityId)
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = entityIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = entityLabel.take(5),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = if (isIncome) "Entrée" else "Sortie",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun transactionCategoryLabel(category: TransactionCategoryDto): String = when (category) {
    TransactionCategoryDto.CLEANING -> "Nettoyage"
    TransactionCategoryDto.FUEL -> "Carburant"
    TransactionCategoryDto.SECURITY -> "Sécurité"
    TransactionCategoryDto.MAINTENANCE -> "Maintenance"
    TransactionCategoryDto.TAXES -> "Impôts"
    TransactionCategoryDto.DEPOSIT -> "Caution"
    TransactionCategoryDto.RENT -> "Loyer Reçu"
    TransactionCategoryDto.LEASE_PAYMENT -> "Paiement Bail"
    TransactionCategoryDto.ELECTRICITY -> "Électricité"
    TransactionCategoryDto.OTHER -> "Autre"
}

private data class BadgeColors(
    val background: Color,
    val text: Color,
    val border: Color? = null,
)

@Composable
private fun transactionCategoryBadgeColors(category: TransactionCategoryDto): BadgeColors {
    return when (category) {
        TransactionCategoryDto.ELECTRICITY -> BadgeColors(
            background = Color(0xFF00363A).copy(alpha = 0.5f),
            text = Color(0xFF80DEEA),
            border = Color(0xFF00838F).copy(alpha = 0.5f)
        )
        TransactionCategoryDto.MAINTENANCE,
        TransactionCategoryDto.CLEANING,
        TransactionCategoryDto.FUEL -> BadgeColors(
            background = Color(0xFF3E2723).copy(alpha = 0.5f),
            text = Color(0xFFFFCC80),
            border = Color(0xFFE65100).copy(alpha = 0.5f)
        )
        TransactionCategoryDto.RENT,
        TransactionCategoryDto.LEASE_PAYMENT -> BadgeColors(
            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            text = MaterialTheme.colorScheme.primary,
            border = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
        TransactionCategoryDto.DEPOSIT -> BadgeColors(
            background = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            text = MaterialTheme.colorScheme.onSecondaryContainer,
            border = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        )
        TransactionCategoryDto.SECURITY,
        TransactionCategoryDto.TAXES,
        TransactionCategoryDto.OTHER -> BadgeColors(
            background = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
            text = MaterialTheme.colorScheme.onTertiaryContainer,
            border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

@PreviewLightDark
@Composable
fun TransactionsLedgerExpandedPreview() {
    ResidTheme {
        TransactionsLedger(
            transactions = PreviewFinanceTransactionsData.euroTransactions,
            isLoading = false,
            isSmallScreen = true,
            forcedWindowWidthSizeClass = WindowWidthSizeClass.Expanded,
            onTraceabilityClick = { _ -> }
        )
    }
}

@PreviewLightDark
@Composable
fun TransactionsLedgerCompactPreview() {
    ResidTheme {
        TransactionsLedger(
            transactions = PreviewFinanceTransactionsData.euroTransactions,
            isLoading = false,
            isSmallScreen = true,
            forcedWindowWidthSizeClass = WindowWidthSizeClass.Compact,
            onTraceabilityClick = { _ -> }
        )
    }
}

