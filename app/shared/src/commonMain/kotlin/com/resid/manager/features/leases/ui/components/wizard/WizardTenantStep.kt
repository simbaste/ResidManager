package com.resid.manager.features.leases.ui.components.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.GuarantorDto
import com.resid.manager.dto.UserSearchDto
import com.resid.manager.ui.components.AmountTextField
import com.resid.manager.ui.components.AppTextField
import com.resid.manager.ui.components.PhoneTextField

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
    currencySymbol: String = com.resid.manager.dto.AppCurrency.DEFAULT.symbol,
    onInlineFirstNameChanged: (String) -> Unit,
    onInlineLastNameChanged: (String) -> Unit,
    onInlineEmailChanged: (String) -> Unit,
    onInlinePhoneChanged: (String) -> Unit,
    onToggleInlineTenant: (Boolean) -> Unit,
    onUserQueryChanged: (String) -> Unit,
    onUserSelected: (UserSearchDto) -> Unit,
    onSelectExistingGuarantor: (GuarantorDto?) -> Unit,
    onToggleReuseGuarantor: (Boolean) -> Unit,
    onGuarantorLastNameChanged: (String) -> Unit,
    onGuarantorFirstNameChanged: (String) -> Unit,
    onGuarantorPhoneChanged: (String) -> Unit,
    onGuarantorMonthlySalaryChanged: (String) -> Unit,
    onGuarantorRelationshipChanged: (String) -> Unit,
    onGuarantorJobTitleChanged: (String) -> Unit,
    onGuarantorEmployerChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Étape 1 sur 4 : Sélection du Locataire", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        if (isInlineTenant) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nouveau locataire inline", style = MaterialTheme.typography.titleSmall)
                AppTextField(
                    value = inlineFirstName,
                    onValueChange = onInlineFirstNameChanged,
                    label = "Prénom *",
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    modifier = Modifier.fillMaxWidth()
                )
                AppTextField(
                    value = inlineLastName,
                    onValueChange = onInlineLastNameChanged,
                    label = "Nom *",
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    modifier = Modifier.fillMaxWidth()
                )
                AppTextField(
                    value = inlineEmail,
                    onValueChange = onInlineEmailChanged,
                    label = "Email *",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    modifier = Modifier.fillMaxWidth()
                )
                com.resid.manager.ui.components.PhoneTextField(
                    value = inlinePhone,
                    onPhoneChanged = onInlinePhoneChanged,
                    label = "Téléphone",
                    imeAction = ImeAction.Done,
                    onImeAction = { focusManager.clearFocus() },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onToggleInlineTenant(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Retour à la recherche")
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = userQuery,
                    onValueChange = onUserQueryChanged,
                    label = "Saisissez le nom d'un locataire...",
                    imeAction = ImeAction.Search,
                    onImeAction = { focusManager.clearFocus() },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSearchingUsers) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                if (userResults.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            userResults.forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onUserSelected(user) }
                                        .padding(12.dp)
                                ) {
                                    Text(text = "${user.name} (${user.email})")
                                }
                            }
                        }
                    }
                }

                if (draftTenantId != null) {
                    Text("Sélectionné : $draftTenantName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }

                Button(
                    onClick = { onToggleInlineTenant(true) },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("+ Ajouter un nouveau locataire")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // SECTION GARANT
        Text("Informations du Garant", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        if (existingGuarantors.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isReusingGuarantor,
                    onClick = { onToggleReuseGuarantor(true) },
                    label = { Text("Garant existant (${existingGuarantors.size})") }
                )
                FilterChip(
                    selected = !isReusingGuarantor,
                    onClick = {
                        onToggleReuseGuarantor(false)
                        onSelectExistingGuarantor(null)
                    },
                    label = { Text("Nouveau garant") }
                )
            }

            if (isReusingGuarantor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sélectionnez le garant à réutiliser :", style = MaterialTheme.typography.bodySmall)
                    existingGuarantors.forEach { g ->
                        val isThisSelected = selectedGuarantorId == g.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectExistingGuarantor(g) },
                            colors = if (isThisSelected) {
                                androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            } else {
                                androidx.compose.material3.CardDefaults.cardColors()
                            }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${g.firstName} ${g.lastName}", style = MaterialTheme.typography.titleSmall)
                                Text("Tél : ${g.phone}" + if (!g.relationship.isNullOrBlank()) " • ${g.relationship}" else "", style = MaterialTheme.typography.bodySmall)
                                if (!g.jobTitle.isNullOrBlank()) {
                                    Text("Profession : ${g.jobTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isReusingGuarantor) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(
                    value = guarantorLastName,
                    onValueChange = onGuarantorLastNameChanged,
                    label = "Nom du garant *",
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    modifier = Modifier.fillMaxWidth()
                )
                AppTextField(
                    value = guarantorFirstName,
                    onValueChange = onGuarantorFirstNameChanged,
                    label = "Prénom du garant *",
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    modifier = Modifier.fillMaxWidth()
                )
                PhoneTextField(
                    value = guarantorPhone,
                    onPhoneChanged = onGuarantorPhoneChanged,
                    label = "Téléphone du garant *",
                    imeAction = ImeAction.Done,
                    onImeAction = { focusManager.clearFocus() },
                    modifier = Modifier.fillMaxWidth()
                )

                // Accordéon repliable : Plus d'informations sur le garant
                var expandedGuarantorDetails by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { expandedGuarantorDetails = !expandedGuarantorDetails },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (expandedGuarantorDetails) "Masquer les informations complémentaires" else "+ Plus d'informations sur le garant (Optionnel)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = if (expandedGuarantorDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }

                AnimatedVisibility(visible = expandedGuarantorDetails) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AmountTextField(
                            value = guarantorMonthlySalary,
                            onValueChange = onGuarantorMonthlySalaryChanged,
                            label = "Salaire / Revenu mensuel ($currencySymbol)",
                            currencySymbol = currencySymbol,
                            placeholder = "0.0",
                            imeAction = ImeAction.Next,
                            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        AppTextField(
                            value = guarantorRelationship,
                            onValueChange = onGuarantorRelationshipChanged,
                            label = "Lien avec le locataire (ex: Parent, Conjoint, Ami, Employeur)",
                            imeAction = ImeAction.Next,
                            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        AppTextField(
                            value = guarantorJobTitle,
                            onValueChange = onGuarantorJobTitleChanged,
                            label = "Emploi / Profession",
                            imeAction = ImeAction.Next,
                            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        AppTextField(
                            value = guarantorEmployer,
                            onValueChange = onGuarantorEmployerChanged,
                            label = "Employeur / Entreprise",
                            imeAction = ImeAction.Done,
                            onImeAction = { focusManager.clearFocus() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
