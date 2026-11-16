package com.resid.manager.features.residences.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.resid.manager.features.residences.mvi.ResidenceSortOption
import com.resid.manager.features.residences.mvi.ResidenceViewMode
import com.resid.manager.ui.components.AppFilterBar
import com.resid.manager.ui.components.FilterTabItem
import com.resid.manager.ui.components.SortOptionItem

@Composable
fun ResidenceFilterBar(
    selectedTabIndex: Int,
    ownedCount: Int,
    associatedCount: Int,
    onTabSelected: (Int) -> Unit,
    sortOption: ResidenceSortOption,
    onSortOptionSelected: (ResidenceSortOption) -> Unit,
    viewMode: ResidenceViewMode,
    onViewModeSelected: (ResidenceViewMode) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val tabs = listOf(
        FilterTabItem(
            label = "Propriétés gérées",
            count = ownedCount,
            shortLabel = "Gérées"
        ),
        FilterTabItem(
            label = "Propriétés associées",
            count = associatedCount,
            shortLabel = "Associées"
        )
    )

    val sortOptions = ResidenceSortOption.entries.map {
        SortOptionItem(value = it, label = it.label)
    }

    AppFilterBar(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        sortOptions = sortOptions,
        selectedSortOption = sortOption,
        onSortOptionSelected = onSortOptionSelected,
        viewMode = viewMode,
        onViewModeSelected = onViewModeSelected,
        isCompact = isCompact,
        modifier = modifier
    )
}
