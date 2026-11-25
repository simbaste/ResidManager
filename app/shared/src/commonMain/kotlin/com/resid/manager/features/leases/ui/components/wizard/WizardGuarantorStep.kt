package com.resid.manager.features.leases.ui.components.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.GuarantorDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardGuarantorStep(
    existingGuarantors: List<GuarantorDto>,
    selectedGuarantorId: String?,
    isReusingGuarantor: Boolean,
    guarantorLastName: String,
    guarantorFirstName: String,
    guarantorPhone: String,
    guarantorMonthlySalary: String,
    guarantorRelationship: String,
    guarantorJobTitle: String,
    guarantorEmployer: String,
    currencySymbol: String,
    guarantorLastNameError: String? = null,
    guarantorFirstNameError: String? = null,
    guarantorPhoneError: String? = null,
    onSelectExistingGuarantor: (GuarantorDto?) -> Unit,
    onToggleReuseGuarantor: (Boolean) -> Unit,
    onGuarantorLastNameChanged: (String) -> Unit,
    onGuarantorFirstNameChanged: (String) -> Unit,
    onGuarantorPhoneChanged: (String) -> Unit,
    onGuarantorMonthlySalaryChanged: (String) -> Unit,
    onGuarantorRelationshipChanged: (String) -> Unit,
    onGuarantorJobTitleChanged: (String) -> Unit,
    onGuarantorEmployerChanged: (String) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector: Without Guarantor, Existing, New
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = false, // Just a button effectively for skipping
                onClick = onSkip,
                label = { Text("Passer cette étape") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PersonOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
             FilterChip(
                selected = isReusingGuarantor && existingGuarantors.isNotEmpty(),
                onClick = { onToggleReuseGuarantor(true) },
                label = { Text("Garant existant") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isReusingGuarantor && existingGuarantors.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                enabled = existingGuarantors.isNotEmpty(),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            FilterChip(
                selected = !isReusingGuarantor,
                onClick = { onToggleReuseGuarantor(false) },
                label = { Text("Nouveau garant") },
                leadingIcon = {
                    Icon(
                        imageVector = if (!isReusingGuarantor) Icons.Default.CheckCircle else Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        if (isReusingGuarantor && existingGuarantors.isNotEmpty()) {
            // Existing Guarantor Selection
             Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    existingGuarantors.forEach { g ->
                        val isSelected = g.id == selectedGuarantorId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectExistingGuarantor(g) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if(isSelected) Icons.Default.CheckCircle else Icons.Outlined.Person,
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp),
                                    tint = if(isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "${g.firstName} ${g.lastName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(text = g.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (g != existingGuarantors.last()) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        } else {
            // New Guarantor Inline Creation
            OutlinedTextField(
                value = guarantorLastName,
                onValueChange = onGuarantorLastNameChanged,
                label = { Text("Nom de famille *") },
                isError = guarantorLastNameError != null,
                supportingText = guarantorLastNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = guarantorFirstName,
                onValueChange = onGuarantorFirstNameChanged,
                label = { Text("Prénom(s) *") },
                isError = guarantorFirstNameError != null,
                supportingText = guarantorFirstNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = guarantorPhone,
                onValueChange = onGuarantorPhoneChanged,
                label = { Text("Numéro de téléphone *") },
                isError = guarantorPhoneError != null,
                supportingText = guarantorPhoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = guarantorRelationship,
                onValueChange = onGuarantorRelationshipChanged,
                label = { Text("Lien de parenté (ex: Père, Mère...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = guarantorMonthlySalary,
                onValueChange = onGuarantorMonthlySalaryChanged,
                label = { Text("Revenus mensuels nets ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Informations complémentaires (Accodrion pour profession etc.)
            var expandedMoreInfo by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedMoreInfo = !expandedMoreInfo }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Informations professionnelles",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (expandedMoreInfo) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Étendre/Réduire"
                        )
                    }

                    AnimatedVisibility(visible = expandedMoreInfo) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = guarantorJobTitle,
                                onValueChange = onGuarantorJobTitleChanged,
                                label = { Text("Profession") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = guarantorEmployer,
                                onValueChange = onGuarantorEmployerChanged,
                                label = { Text("Employeur") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedButton(
                                onClick = { /* Upload logic */ },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Joindre les fiches de paie")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
