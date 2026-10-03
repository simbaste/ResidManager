package com.resid.manager.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.resid.manager.ui.theme.ResidTheme

data class ResidAppBarAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val contentDescription: String? = title,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidTopAppBar(
    title: String,
    actions: List<ResidAppBarAction> = emptyList(),
    modifier: Modifier = Modifier,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    navigationIcon: @Composable () -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isSmallScreen = maxWidth < 600.dp

        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = if (isSmallScreen) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            navigationIcon = navigationIcon,
            actions = {
                actions.forEach { action ->
                    if (isSmallScreen) {
                        IconButton(onClick = action.onClick) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.contentDescription,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        Button(onClick = action.onClick) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = action.title)
                        }
                    }
                }
            },
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ResidTopAppBarPreview() {
    ResidTheme(darkTheme = false) {
        ResidTopAppBar(
            title = "Annuaire de mes Résidences",
            actions = listOf(
                ResidAppBarAction(
                    title = "Créer une résidence",
                    icon = Icons.Default.Add,
                    onClick = {},
                ),
            ),
        )
    }
}
