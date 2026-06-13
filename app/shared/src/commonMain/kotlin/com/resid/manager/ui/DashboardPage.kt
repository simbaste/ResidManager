package com.resid.manager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.UserRole
import com.resid.manager.viewmodel.AppScreen
import com.resid.manager.viewmodel.LoginViewModel

@Composable
fun DashboardPage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeResidence = uiState.selectedResidenceContext

    if (activeResidence == null) {
        // No Residence / Empty State Onboarding
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.widthIn(max = 550.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Bienvenue dans Resid Manager",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Pour commencer, vous devez soit créer votre propre résidence, soit en rechercher une existante pour envoyer une demande d'adhésion.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setShowCreateResidenceDialog(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Créer une résidence")
                        }

                        Button(
                            onClick = { viewModel.setShowJoinResidenceDialog(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Rejoindre une résidence")
                        }
                    }
                }
            }
        }
    } else {
        // Active Residence - Display personalized metrics and scoped widgets
        val role = activeResidence.userRoleInResidence
        val isManagement = role == UserRole.ADMIN || role == UserRole.RESIDENCE_MANAGER // elevated management tier

        var showActionMessage by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Personalized Greeting
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Bonjour, ${uiState.firstName} !",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Vous consultez actuellement la résidence : ${activeResidence.residenceName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Rôle : ${role.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Core Metric Cards Row (Visibility scoped by role)
            if (isManagement) {
                // Show elevated Tier Cards (Financial & Operational metrics)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(title = "Cashflow Net", value = "1 250 000 XOF", desc = "Ce mois-ci", modifier = Modifier.weight(1f))
                    MetricCard(title = "Taux d'occupation", value = "82.5 %", desc = "Unités louées", modifier = Modifier.weight(1f))
                    MetricCard(title = "Impayés / Délinquance", value = "14.2 %", desc = "Loyers dus", modifier = Modifier.weight(1f))
                    MetricCard(title = "Tickets Actifs", value = "3", desc = "En cours d'intervention", modifier = Modifier.weight(1f))
                }
            } else {
                // Hide financial metrics for regular users. Show operational metrics instead!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(title = "Tickets Actifs", value = "3", desc = "En attente de traitement", modifier = Modifier.weight(1f))
                    MetricCard(title = "Dernier relevé élec.", value = "Relevé OK", desc = "Consulter l'historique", modifier = Modifier.weight(1f))
                }
            }

            // Quick Actions Section (Dynamic visibility scoped by role)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "Actions rapides", style = MaterialTheme.typography.titleLarge)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (role) {
                            UserRole.ADMIN -> {
                                // Creator / Admin Quick Actions (full suite)
                                Button(onClick = { showActionMessage = "Formulaire de création de bail ouvert" }, modifier = Modifier.weight(1f)) {
                                    Text("Nouveau Bail")
                                }
                                Button(onClick = { viewModel.setShowCreateLogementDialog(true) }, modifier = Modifier.weight(1f)) {
                                    Text("Nouveau Logement")
                                }
                                Button(onClick = { showActionMessage = "Modal d'enregistrement de dépense ouvert" }, modifier = Modifier.weight(1f)) {
                                    Text("Enregistrer Dépense")
                                }
                            }
                            UserRole.RESIDENCE_MANAGER -> {
                                // Manager / Staff Quick Actions
                                Button(onClick = { viewModel.setShowCreateLogementDialog(true) }, modifier = Modifier.weight(1f)) {
                                    Text("Nouveau Logement")
                                }
                                Button(onClick = { showActionMessage = "Formulaire d'ouverture de ticket de maintenance ouvert" }, modifier = Modifier.weight(1f)) {
                                    Text("Ouvrir un Ticket")
                                }
                            }
                            UserRole.TENANT -> {
                                // Tenant Quick Actions
                                Button(
                                    onClick = { showActionMessage = "Formulaire de signalement d'incident (Maintenance) ouvert" },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text("Signaler un Incident / Ouvrir un Ticket", color = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    }

                    if (showActionMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Action exécutée: ${showActionMessage!!} (Bientôt disponible à l'étape 3 !)",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}
