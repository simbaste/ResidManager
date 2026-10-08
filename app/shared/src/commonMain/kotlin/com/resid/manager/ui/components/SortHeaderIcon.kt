package com.resid.manager.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable table header sorting icon for data tables and lists.
 * - Displays [Icons.Default.ArrowDropUp] if active and ascending.
 * - Displays [Icons.Default.ArrowDropDown] if active and descending.
 * - Displays [Icons.Default.UnfoldMore] if inactive to indicate sortability.
 */
@Composable
fun SortHeaderIcon(
    isCurrentSort: Boolean,
    sortAscending: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.onSurface,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
    size: Dp = 18.dp
) {
    if (isCurrentSort) {
        Icon(
            imageVector = if (sortAscending) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
            contentDescription = if (sortAscending) "Tri croissant" else "Tri décroissant",
            tint = activeColor,
            modifier = modifier.size(size)
        )
    } else {
        Icon(
            imageVector = Icons.Default.UnfoldMore,
            contentDescription = "Trier",
            tint = inactiveColor,
            modifier = modifier.size(size)
        )
    }
}
