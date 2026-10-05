package com.resid.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.resid.manager.getDefaultCountryCode

/**
 * Reusable phone number field with country code prefix selector and automatic grouping formatter (e.g. 01 02 03 04 05).
 * - Transmits the full international phone number (e.g. "+2250102030405") via [onPhoneChanged].
 * - Supports custom label, error state, and keyboard actions.
 */
@Composable
fun PhoneTextField(
    value: String,
    onPhoneChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Numéro de téléphone",
    placeholder: String = "01 02 03 04 05",
    isError: Boolean = false,
    errorMessage: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    val detectedIso = remember { getDefaultCountryCode() }
    val initialCountry = remember {
        SupportedCountryPhoneCodes.firstOrNull { value.startsWith(it.dialCode) }
            ?: findCountryOrDefault(detectedIso)
    }

    var selectedCountry by remember { mutableStateOf(initialCountry) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val visualTransformation = remember { PhoneVisualTransformation(groupSize = 2) }

    // Strip country dial code to keep only local number in the input box
    var localNumber by remember(value, selectedCountry) {
        val raw = if (value.startsWith(selectedCountry.dialCode)) {
            value.removePrefix(selectedCountry.dialCode).trim()
        } else {
            value.trim()
        }
        mutableStateOf(raw.filter { it.isDigit() })
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Country Prefix Picker
            Box {
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .clickable { expandedDropdown = true }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedCountry.iso,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = selectedCountry.dialCode,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Sélectionner pays",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier
                        .width(260.dp)
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SupportedCountryPhoneCodes.forEach { country ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${country.name} (${country.iso})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = country.dialCode,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                selectedCountry = country
                                expandedDropdown = false
                                val fullPhone = if (localNumber.isNotBlank()) {
                                    "${country.dialCode}$localNumber"
                                } else ""
                                onPhoneChanged(fullPhone)
                            }
                        )
                    }
                }
            }

            // Local Phone Number Input with grouping visual transformation
            AppTextField(
                value = localNumber,
                onValueChange = { input ->
                    val digitsOnly = input.filter { it.isDigit() }.take(15)
                    localNumber = digitsOnly
                    val fullNumber = if (digitsOnly.isNotBlank()) {
                        "${selectedCountry.dialCode}$digitsOnly"
                    } else ""
                    onPhoneChanged(fullNumber)
                },
                label = label,
                placeholder = placeholder,
                isError = isError,
                singleLine = true,
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                imeAction = imeAction,
                onImeAction = onImeAction,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Téléphone",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
