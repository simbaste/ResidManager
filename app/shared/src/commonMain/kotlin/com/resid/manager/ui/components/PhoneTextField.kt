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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.resid.manager.getDefaultCountryCode
import com.resid.manager.ui.theme.ResidTheme

/**
 * 1. Base Phone Number Field (without country code picker).
 * Formats local digits with [PhoneVisualTransformation] (e.g. 01 02 03 04 05),
 * filters non-digits, and sets telephone keyboard options and icon.
 */
@Composable
fun RawPhoneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Numéro de téléphone",
    placeholder: String = "01 02 03 04 05",
    isError: Boolean = false,
    errorMessage: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    val visualTransformation = remember { PhoneVisualTransformation(groupSize = 2) }

    AppTextField(
        value = value,
        onValueChange = { input ->
            val digitsOnly = input.filter { it.isDigit() }.take(15)
            onValueChange(digitsOnly)
        },
        label = label,
        placeholder = placeholder,
        isError = isError,
        errorMessage = errorMessage,
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
        modifier = modifier.semantics {
            contentType = ContentType.PhoneNumberNational
        }
    )
}

/**
 * 2. Country Prefix Dropdown Selector.
 * Allows choosing a country code from [SupportedCountryPhoneCodes].
 */
@Composable
fun CountryCodeDropdown(
    selectedCountry: CountryPhoneCode,
    onCountrySelected: (CountryPhoneCode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp),
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
            expanded = expanded,
            onDismissRequest = { expanded = false },
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
                        onCountrySelected(country)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 3. Complete International Phone Number Field.
 * Combines [CountryCodeDropdown] and [RawPhoneTextField].
 * - Transmits the full international phone number (e.g. "+2250102030405") via [onPhoneChanged].
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
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentType = ContentType.PhoneNumber
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountryCodeDropdown(
                selectedCountry = selectedCountry,
                onCountrySelected = { country ->
                    selectedCountry = country
                    val fullPhone = if (localNumber.isNotBlank()) {
                        "${country.dialCode}$localNumber"
                    } else ""
                    onPhoneChanged(fullPhone)
                }
            )

            RawPhoneTextField(
                value = localNumber,
                onValueChange = { digitsOnly ->
                    localNumber = digitsOnly
                    val fullNumber = if (digitsOnly.isNotBlank()) {
                        "${selectedCountry.dialCode}$digitsOnly"
                    } else ""
                    onPhoneChanged(fullNumber)
                },
                label = "",
                placeholder = placeholder,
                isError = isError,
                imeAction = imeAction,
                onImeAction = onImeAction,
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

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun PhoneTextFieldPreview() {
    ResidTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            PhoneTextField(
                value = "+22501020304",
                onPhoneChanged = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun RawPhoneTextFieldPreview() {
    ResidTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            RawPhoneTextField(
                value = "0708091011",
                onValueChange = {}
            )
        }
    }
}
