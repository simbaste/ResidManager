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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.resid.manager.dto.UserSearchDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardTenantStep(
    isInlineTenant: Boolean,
    inlineFirstName: String,
    inlineLastName: String,
    inlineEmail: String,
    inlinePhone: String,
    userQuery: String,
    userResults: List<UserSearchDto>,
    isSearchingUsers: Boolean,
    draftTenantId: String?,
    draftTenantName: String,
    inlineFirstNameError: String? = null,
    inlineLastNameError: String? = null,
    inlineEmailError: String? = null,
    onInlineFirstNameChanged: (String) -> Unit,
    onInlineLastNameChanged: (String) -> Unit,
    onInlineEmailChanged: (String) -> Unit,
    onInlinePhoneChanged: (String) -> Unit,
    onToggleInlineTenant: (Boolean) -> Unit,
    onUserQueryChanged: (String) -> Unit,
    onUserSelected: (UserSearchDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector: Existing vs New Tenant
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !isInlineTenant,
                onClick = { onToggleInlineTenant(false) },
                label = { Text("Locataire existant") },
                leadingIcon = {
                    Icon(
                        imageVector = if (!isInlineTenant) Icons.Default.CheckCircle else Icons.Default.Person,
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

            FilterChip(
                selected = isInlineTenant,
                onClick = { onToggleInlineTenant(true) },
                label = { Text("Nouveau locataire") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isInlineTenant) Icons.Default.CheckCircle else Icons.Default.PersonAdd,
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

        if (!isInlineTenant) {
            // Existing Tenant Search
            OutlinedTextField(
                value = userQuery,
                onValueChange = onUserQueryChanged,
                label = { Text("Rechercher un membre (Nom, Email...)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    if (isSearchingUsers) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                },
                singleLine = true
            )

            if (userResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        userResults.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserSelected(user) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        modifier = Modifier.padding(8.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = user.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(text = user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (user != userResults.last()) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            } else if (draftTenantId != null && userQuery == draftTenantName) {
                // Showing selected tenant
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Locataire sélectionné : $draftTenantName", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            // New Tenant Inline Creation
            var expandedMoreInfo by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = inlineLastName,
                onValueChange = onInlineLastNameChanged,
                label = { Text("Nom de famille *") },
                isError = inlineLastNameError != null,
                supportingText = inlineLastNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = inlineFirstName,
                onValueChange = onInlineFirstNameChanged,
                label = { Text("Prénom(s) *") },
                isError = inlineFirstNameError != null,
                supportingText = inlineFirstNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = inlineEmail,
                onValueChange = onInlineEmailChanged,
                label = { Text("Adresse email *") },
                isError = inlineEmailError != null,
                supportingText = inlineEmailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = inlinePhone,
                onValueChange = onInlinePhoneChanged,
                label = { Text("Numéro de téléphone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Informations complémentaires (Accodrion pour pièces d'identité, profession etc.)
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
                            text = "Informations complémentaires (Optionnel)",
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
                                value = "",
                                onValueChange = { },
                                label = { Text("Profession") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Simplified Dropdown for ID Type (using textfield for mock)
                                OutlinedTextField(
                                    value = "Carte d'identité",
                                    onValueChange = { },
                                    label = { Text("Type de pièce") },
                                    modifier = Modifier.weight(1f),
                                    readOnly = true,
                                    trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }
                                )
                                OutlinedTextField(
                                    value = "",
                                    onValueChange = { },
                                    label = { Text("N° de pièce") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            OutlinedButton(
                                onClick = { /* Upload logic */ },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Joindre le justificatif d'identité")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
