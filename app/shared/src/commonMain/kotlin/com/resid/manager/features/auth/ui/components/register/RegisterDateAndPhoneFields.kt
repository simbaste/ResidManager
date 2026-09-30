package com.resid.manager.features.auth.ui.components.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.sp
import com.resid.manager.getDefaultCountryCode
import com.resid.manager.ui.components.SupportedCountryPhoneCodes
import com.resid.manager.ui.components.findCountryOrDefault
import com.resid.manager.ui.theme.residColors

/**
 * Material 3 Date Picker field for Date of Birth.
 * Clickable input opening a multiplatform DatePickerDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDateOfBirthPickerField(
    value: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    // Parse existing YYYY-MM-DD to UTC epoch millis
    val initialSelectedMillis = remember(value) {
        parseDateToUtcMillis(value)
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedMillis,
        yearRange = 1920..2026
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "DATE DE NAISSANCE",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        "AAAA-MM-JJ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.residColors.inputBackground,
                    unfocusedContainerColor = MaterialTheme.residColors.inputBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.residColors.inputBorder,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Sélectionner la date de naissance",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendrier",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            // Transparent overlay to make the whole field trigger the date picker dialog
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDatePicker = true }
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(formatUtcMillisToIsoDate(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = "Confirmer",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(
                        text = "Annuler",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.residColors.cardBackground
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                    todayContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

/**
 * Responsive Phone Number input with Country Code Selector (Flag + ISO + Dial Code).
 * Defaults to the user's location country (best effort) or existing country in phone value.
 */
@Composable
fun RegisterPhoneNumberField(
    value: String,
    onPhoneChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next
) {
    val detectedIso = remember { getDefaultCountryCode() }
    val initialCountry = remember {
        SupportedCountryPhoneCodes.firstOrNull { value.startsWith(it.dialCode) }
            ?: findCountryOrDefault(detectedIso)
    }

    var selectedCountry by remember { mutableStateOf(initialCountry) }
    var expanded by remember { mutableStateOf(false) }

    var localNumber by remember(value) {
        val numberPart = if (value.startsWith(selectedCountry.dialCode)) {
            value.removePrefix(selectedCountry.dialCode).trim()
        } else {
            value.trim()
        }
        mutableStateOf(numberPart)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "TÉLÉPHONE",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .background(MaterialTheme.residColors.inputBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.residColors.inputBorder, RoundedCornerShape(12.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = selectedCountry.iso,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = selectedCountry.dialCode,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Choisir le pays",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(MaterialTheme.residColors.cardBackground)
                        .widthIn(min = 220.dp, max = 300.dp)
                ) {
                    SupportedCountryPhoneCodes.forEach { country ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (country.iso == selectedCountry.iso) {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = country.iso,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (country.iso == selectedCountry.iso) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                    Text(
                                        text = "${country.name} (${country.dialCode})",
                                        color = if (country.iso == selectedCountry.iso) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (country.iso == selectedCountry.iso) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                selectedCountry = country
                                expanded = false
                                val fullPhone = if (localNumber.isNotBlank()) {
                                    "${country.dialCode}${localNumber.filter { it.isDigit() }}"
                                } else {
                                    country.dialCode
                                }
                                onPhoneChanged(fullPhone)
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = localNumber,
                onValueChange = { input ->
                    localNumber = input
                    val digits = input.filter { it.isDigit() }
                    val fullNumber = if (digits.isNotBlank()) {
                        "${selectedCountry.dialCode}$digits"
                    } else {
                        ""
                    }
                    onPhoneChanged(fullNumber)
                },
                placeholder = {
                    Text(
                        "6 12 34 56 78",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.residColors.inputBackground,
                    unfocusedContainerColor = MaterialTheme.residColors.inputBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.residColors.inputBorder,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Téléphone",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = imeAction
                )
            )
        }
    }
}

/**
 * Formats epoch UTC milliseconds into ISO date format "YYYY-MM-DD" across all Kotlin Multiplatform targets.
 */
private fun formatUtcMillisToIsoDate(utcMillis: Long): String {
    var days = (utcMillis / 86400000L).toInt()

    var year = 1970
    while (true) {
        val daysInYear = if (isLeapYear(year)) 366 else 365
        if (days >= daysInYear) {
            days -= daysInYear
            year++
        } else if (days < 0) {
            year--
            days += if (isLeapYear(year)) 366 else 365
        } else {
            break
        }
    }

    val daysInMonths = intArrayOf(
        31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    )

    var month = 1
    for (dim in daysInMonths) {
        if (days >= dim) {
            days -= dim
            month++
        } else {
            break
        }
    }
    val day = days + 1

    val mStr = if (month < 10) "0$month" else "$month"
    val dStr = if (day < 10) "0$day" else "$day"
    return "$year-$mStr-$dStr"
}

/**
 * Parses "YYYY-MM-DD" into UTC epoch milliseconds.
 */
private fun parseDateToUtcMillis(isoDate: String): Long? {
    val parts = isoDate.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null

    var days = 0L
    if (year >= 1970) {
        for (y in 1970 until year) {
            days += if (isLeapYear(y)) 366 else 365
        }
    } else {
        for (y in year until 1970) {
            days -= if (isLeapYear(y)) 366 else 365
        }
    }

    val daysInMonths = intArrayOf(
        31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    )

    for (m in 1 until month) {
        days += daysInMonths[m - 1]
    }
    days += (day - 1)

    return days * 86400000L
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}
