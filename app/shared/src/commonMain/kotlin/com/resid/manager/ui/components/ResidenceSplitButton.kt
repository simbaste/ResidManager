package com.resid.manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.ui.theme.ResidTheme

@Composable
fun ResidenceSplitButton(
    selectedResidence: ResidenceContext?,
    residences: List<ResidenceContext>,
    onResidenceSelected: (ResidenceContext) -> Unit,
    onAddResidenceClick: () -> Unit,
    onJoinResidenceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    val splitButtonColor = MaterialTheme.colorScheme.primary
    val splitButtonShape = RoundedCornerShape(9999.dp)
    val splitButtonBg = Color.Transparent

    Box(modifier = modifier) {
        Surface(
            shape = splitButtonShape,
            color = splitButtonBg,
            border = BorderStroke(1.dp, splitButtonColor.copy(alpha = 0.2f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left section: Residence icon and name (clickable)
                Row(
                    modifier = Modifier
                        .clickable { showDropdown = !showDropdown }
                        .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = splitButtonColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = selectedResidence?.residenceName ?: "No Active Residence",
                        style = MaterialTheme.typography.bodyMedium,
                        color = splitButtonColor
                    )
                }

                // Split divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(splitButtonColor.copy(alpha = 0.2f))
                )

                // Right section: Arrow dropdown trigger (clickable)
                Box(
                    modifier = Modifier
                        .clickable { showDropdown = !showDropdown }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Sélectionner une résidence",
                        tint = splitButtonColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            if (residences.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Créer une résidence...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showDropdown = false
                        onAddResidenceClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Rejoindre une résidence...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showDropdown = false
                        onJoinResidenceClick()
                    }
                )
            } else {
                residences.forEach { residence ->
                    DropdownMenuItem(
                        text = { Text(residence.residenceName) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onResidenceSelected(residence)
                            showDropdown = false
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Créer une résidence") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showDropdown = false
                        onAddResidenceClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Rejoindre une résidence") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showDropdown = false
                        onJoinResidenceClick()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ResidenceSplitButtonPreview() {
    val sampleResidences = listOf(
        ResidenceContext(
            residenceId = "1",
            residenceName = "Les Jardins d'Ivoire",
            residenceAddress = "Cocody Danga, Abidjan",
            userRoleInResidence = UserRole.OWNER,
            totalUnits = 24,
            currencySymbol = "FCFA",
            currencyCode = "XOF"
        )
    )

    ResidTheme(darkTheme = false) {
        ResidenceSplitButton(
            selectedResidence = sampleResidences.first(),
            residences = sampleResidences,
            onResidenceSelected = {},
            onAddResidenceClick = {},
            onJoinResidenceClick = {}
        )
    }
}
