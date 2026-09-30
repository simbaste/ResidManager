package com.resid.manager.features.leases.ui.components.wizard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.UserSearchDto

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
    onInlineFirstNameChanged: (String) -> Unit,
    onInlineLastNameChanged: (String) -> Unit,
    onInlineEmailChanged: (String) -> Unit,
    onInlinePhoneChanged: (String) -> Unit,
    onToggleInlineTenant: (Boolean) -> Unit,
    onUserQueryChanged: (String) -> Unit,
    onUserSelected: (UserSearchDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Étape 1 sur 4 : Sélection du Locataire", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        if (isInlineTenant) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nouveau locataire inline", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(value = inlineFirstName, onValueChange = onInlineFirstNameChanged, label = { Text("Prénom *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = inlineLastName, onValueChange = onInlineLastNameChanged, label = { Text("Nom *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = inlineEmail, onValueChange = onInlineEmailChanged, label = { Text("Email *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = inlinePhone, onValueChange = onInlinePhoneChanged, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = { onToggleInlineTenant(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Retour à la recherche")
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = userQuery,
                    onValueChange = onUserQueryChanged,
                    label = { Text("Saisissez le nom d'un locataire...") },
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
    }
}
