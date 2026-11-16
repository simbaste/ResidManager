package com.resid.manager.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import com.resid.manager.dto.EquipmentDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.features.units.ui.components.EditUnitContent

@Composable
fun EditUnitDialog(
    residenceUnit: ResidenceUnitDto,
    availableEquipments: List<EquipmentDto> = emptyList(),
    currencySymbol: String = "XOF",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double, Double, Double, List<String>) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            EditUnitContent(
                residenceUnit = residenceUnit,
                availableEquipments = availableEquipments,
                currencySymbol = currencySymbol,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onDismiss = onDismiss,
                onSubmit = onSubmit
            )
        }
    )
}

