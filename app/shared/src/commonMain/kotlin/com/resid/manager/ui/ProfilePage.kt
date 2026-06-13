package com.resid.manager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.viewmodel.LoginViewModel

@Composable
fun ProfilePage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Mon Compte Utilisateur",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Prénom : ${uiState.firstName}", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Nom : ${uiState.lastName}", style = MaterialTheme.typography.titleMedium)
                    Text(text = "E-mail : ${uiState.loggedInUser?.email ?: "Non spécifié"}", style = MaterialTheme.typography.titleMedium)
                    Text(text = "ID Utilisateur : ${uiState.loggedInUser?.id ?: "Non spécifié"}", style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider()

                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Déconnexion")
                }
            }
        }
    }
}
