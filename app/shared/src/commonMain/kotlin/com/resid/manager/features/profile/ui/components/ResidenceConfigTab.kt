package com.resid.manager.features.profile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.TicketCategoryDto
import com.resid.manager.ui.components.AppTextField
import com.resid.manager.ui.theme.ResidTheme

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ResidenceConfigTab(
    categories: List<TicketCategoryDto>,
    isLoadingCategories: Boolean,
    formCategoryKey: String,
    formCategoryLabel: String,
    isSubmittingCategory: Boolean,
    categoryError: String?,
    selectedCurrency: String,
    isSubmittingCurrency: Boolean,
    currencySuccess: Boolean,
    currencyErrorMsg: String?,
    onCategoryKeyChanged: (String) -> Unit,
    onCategoryLabelChanged: (String) -> Unit,
    onSubmitCategory: () -> Unit,
    onSelectCurrency: (String) -> Unit,
    onSubmitCurrency: () -> Unit,
    onEditCategoryClick: (TicketCategoryDto) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(maxWidth, maxHeight)
        )
        val isMedium = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact || maxWidth < 600.dp

        if (isCompact) {
            // Layout Compact (Mobile): Single column taking full width, scrollable vertically
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Card 1: Category Creation Form
                CategoryCreationForm(
                    formCategoryKey,
                    onCategoryKeyChanged,
                    formCategoryLabel,
                    onCategoryLabelChanged,
                    categoryError,
                    onSubmitCategory,
                    isSubmittingCategory
                )

                // Card 2: Currency Selection Form
                CurrencySelectionForm(
                    selectedCurrency,
                    onSelectCurrency,
                    currencyErrorMsg,
                    currencySuccess,
                    onSubmitCurrency,
                    isSubmittingCurrency
                )

                // Categories Listing at the end of the column taking available screen width
                CategoriesListingCompact(
                    modifier = Modifier.fillMaxWidth(),
                    isLoadingCategories = isLoadingCategories,
                    categories = categories,
                    onEditCategoryClick = onEditCategoryClick
                )
            }
        } else {
            // Layout Non-Compact (Medium & Expanded): Horizontal arrangement / scroll
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Scrollable Left Column (Saisie Catégories & Saisie Devise)
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Card 1: Category Creation Form
                    CategoryCreationForm(
                        formCategoryKey,
                        onCategoryKeyChanged,
                        formCategoryLabel,
                        onCategoryLabelChanged,
                        categoryError,
                        onSubmitCategory,
                        isSubmittingCategory
                    )

                    // Card 2: Currency Selection Form
                    CurrencySelectionForm(
                        selectedCurrency,
                        onSelectCurrency,
                        currencyErrorMsg,
                        currencySuccess,
                        onSubmitCurrency,
                        isSubmittingCurrency
                    )
                }

                // Categories Listing (Right Column, expands)
                if (isMedium) {
                    CategoriesListing(
                        modifier = Modifier
                            .width(420.dp)
                            .fillMaxHeight(),
                        isLoadingCategories = isLoadingCategories,
                        categories = categories,
                        onEditCategoryClick = onEditCategoryClick
                    )
                } else {
                    CategoriesListing(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        isLoadingCategories = isLoadingCategories,
                        categories = categories,
                        onEditCategoryClick = onEditCategoryClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCreationForm(
    formCategoryKey: String,
    onCategoryKeyChanged: (String) -> Unit,
    formCategoryLabel: String,
    onCategoryLabelChanged: (String) -> Unit,
    categoryError: String?,
    onSubmitCategory: () -> Unit,
    isSubmittingCategory: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Nouvelle Catégorie",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF006948)
            )
            Text(
                "Ajoutez une catégorie de pannes de maintenance personnalisée.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            HorizontalDivider()

            AppTextField(
                value = formCategoryKey,
                onValueChange = onCategoryKeyChanged,
                label = "Clé Unique (ex: PAINTING) *",
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )

            AppTextField(
                value = formCategoryLabel,
                onValueChange = onCategoryLabelChanged,
                label = "Libellé d'affichage (ex: Peinture) *",
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth()
            )

            categoryError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = onSubmitCategory,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                modifier = Modifier.fillMaxWidth().height(44.dp),
                enabled = !isSubmittingCategory,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmittingCategory) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Ajouter la catégorie")
                }
            }
        }
    }
}

@Composable
private fun CategoriesListing(
    modifier: Modifier = Modifier,
    isLoadingCategories: Boolean,
    categories: List<TicketCategoryDto>,
    onEditCategoryClick: (TicketCategoryDto) -> Unit
) {
    Card(
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Catégories d'incidents existantes",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF006948)
            )
            HorizontalDivider()

            if (isLoadingCategories) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF006948))
                }
            } else if (categories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Aucune catégorie disponible.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isGlobal = cat.residenceId == null

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cat.label, style = MaterialTheme.typography.titleMedium)
                                    if (isGlobal) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(
                                                    0xFFE0E7FF
                                                )
                                            ),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "Global",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF1E3A8A),
                                                modifier = Modifier.padding(
                                                    horizontal = 6.dp,
                                                    vertical = 2.dp
                                                )
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "Clé technique : ${cat.key}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            if (!isGlobal) {
                                IconButton(onClick = { onEditCategoryClick(cat) }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoriesListingCompact(
    modifier: Modifier = Modifier,
    isLoadingCategories: Boolean,
    categories: List<TicketCategoryDto>,
    onEditCategoryClick: (TicketCategoryDto) -> Unit
) {
    Card(
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Catégories d'incidents existantes",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF006948)
            )
            HorizontalDivider()

            if (isLoadingCategories) {
                Box(modifier = Modifier.fillMaxWidth().height(500.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF006948))
                }
            } else if (categories.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(500.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Aucune catégorie disponible.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isGlobal = cat.residenceId == null

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cat.label, style = MaterialTheme.typography.titleMedium)
                                    if (isGlobal) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(
                                                    0xFFE0E7FF
                                                )
                                            ),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "Global",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF1E3A8A),
                                                modifier = Modifier.padding(
                                                    horizontal = 6.dp,
                                                    vertical = 2.dp
                                                )
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "Clé technique : ${cat.key}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            if (!isGlobal) {
                                IconButton(onClick = { onEditCategoryClick(cat) }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencySelectionForm(
    selectedCurrency: String,
    onSelectCurrency: (String) -> Unit,
    currencyErrorMsg: String?,
    currencySuccess: Boolean,
    onSubmitCurrency: () -> Unit,
    isSubmittingCurrency: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Devise de la Résidence",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF006948)
            )
            Text(
                "Sélectionnez la devise pour les loyers, l'électricité et les transactions financières.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            HorizontalDivider()

            Text(
                "Devise de la Résidence",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF006948)
            )
            Text(
                "Sélectionnez la devise pour les loyers, l'électricité et les transactions financières.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            HorizontalDivider()

            var expandedCurrencyDropdown by remember { mutableStateOf(false) }
            val currentCurrency = AppCurrency.fromCode(selectedCurrency)
            val displayCodeName = currentCurrency.displayName

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandedCurrencyDropdown = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(displayCodeName)
                }
                DropdownMenu(
                    expanded = expandedCurrencyDropdown,
                    onDismissRequest = { expandedCurrencyDropdown = false },
                    modifier = Modifier.width(260.dp)
                ) {
                    AppCurrency.entries.forEach { appCurrency ->
                        DropdownMenuItem(
                            text = { Text(appCurrency.shortLabel) },
                            onClick = {
                                onSelectCurrency(appCurrency.code)
                                expandedCurrencyDropdown = false
                            }
                        )
                    }
                }
            }

            currencyErrorMsg?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (currencySuccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F7F0)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF006948)
                        )
                        Text(
                            "Devise mise à jour !",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF006948)
                        )
                    }
                }
            }

            Button(
                onClick = onSubmitCurrency,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                modifier = Modifier.fillMaxWidth().height(44.dp),
                enabled = !isSubmittingCurrency,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmittingCurrency) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Sauvegarder la devise")
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun ResidenceConfigTabPreview() {
    ResidTheme {
        ResidenceConfigTab(
            categories = listOf(),
            isLoadingCategories = false,
            formCategoryKey = "",
            formCategoryLabel = "",
            isSubmittingCategory = false,
            categoryError = null,
            selectedCurrency = "EUR",
            isSubmittingCurrency = false,
            currencySuccess = false,
            currencyErrorMsg = null,
            onCategoryKeyChanged = {},
            onCategoryLabelChanged = {},
            onSubmitCategory = {},
            onSelectCurrency = {},
            onSubmitCurrency = {},
            onEditCategoryClick = {}
        )
    }
}
