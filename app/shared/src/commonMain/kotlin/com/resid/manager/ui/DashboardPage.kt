package com.resid.manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.resid.manager.dto.UserRole
import com.resid.manager.dto.DashboardDataDto
import com.resid.manager.network.ApiClient
import com.resid.manager.viewmodel.LoginViewModel
import io.ktor.client.request.*
import io.ktor.client.call.body
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.launch

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
        val role = activeResidence.userRoleInResidence

        // Filter States
        var periodFilter by remember { mutableStateOf("MONTH") } // "MONTH", "QUARTER", "YEAR", "CUSTOM"
        var customStartText by remember { mutableStateOf("2025-02-01") }
        var customEndText by remember { mutableStateOf("2025-02-28") }

        var dashboardData by remember { mutableStateOf<DashboardDataDto?>(null) }
        var isLoadingData by remember { mutableStateOf(false) }

        val coroutineScope = rememberCoroutineScope()

        // LaunchedEffect triggers whenever period filter or custom dates change
        LaunchedEffect(activeResidence, periodFilter, customStartText, customEndText) {
            if (activeResidence == null) return@LaunchedEffect
            isLoadingData = true
            try {
                val url = "${ApiClient.BASE_URL}/api/residences/${activeResidence.residenceId}/dashboard?filter=$periodFilter&start_date=$customStartText&end_date=$customEndText"
                val response = ApiClient.httpClient.get(url) {
                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                }
                if (response.status == io.ktor.http.HttpStatusCode.OK) {
                    dashboardData = response.body()
                }
            } catch (e: Exception) {}
            isLoadingData = false
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Welcome Banner Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF006948)), // Brand deep green
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Bonjour, ${uiState.firstName} !",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Color(0xFF85F8C4),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Vous consultez la résidence : ${activeResidence.residenceName}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF85F8C4)
                            )
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(9999.dp)
                    ) {
                        Text(
                            text = "RÔLE : ${role.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // 2. Real-Time Period Selection Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Période de Consolidation Analytique", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filter chips Row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MONTH" to "Ce Mois", "QUARTER" to "Trimestre", "YEAR" to "Année Civile", "CUSTOM" to "Personnalisé").forEach { (key, label) ->
                                FilterChip(
                                    selected = periodFilter == key,
                                    onClick = { periodFilter = key },
                                    label = { Text(label) }
                                )
                            }
                        }

                        // Custom Date inputs (Conditional visibility!)
                        if (periodFilter == "CUSTOM") {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = customStartText,
                                    onValueChange = { customStartText = it },
                                    label = { Text("Du (AAAA-MM-JJ)") },
                                    modifier = Modifier.width(150.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = customEndText,
                                    onValueChange = { customEndText = it },
                                    label = { Text("Au (AAAA-MM-JJ)") },
                                    modifier = Modifier.width(150.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }

            // High delinquency rate alert (exceeds 10% alert constraint!)
            val data = dashboardData
            if (data != null && data.delinquencyRate > 10.0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
                    border = BorderStroke(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFBA1A1A), modifier = Modifier.size(28.dp))
                        Column {
                            val roundedDelinquency = (data.delinquencyRate * 10.0).toInt() / 10.0
                            Text("Alerte Délinquance Critique : Taux d'Impayés à $roundedDelinquency%", style = MaterialTheme.typography.titleSmall, color = Color(0xFFBA1A1A))
                            Text("Le taux d'impayés dépasse le seuil critique de 10%. Veuillez relancer les locataires débiteurs immédiatement.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBA1A1A))
                        }
                    }
                }
            }

            if (isLoadingData) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF006948))
                }
            } else if (data != null) {
                val cashflowVal = data.netCashflow.toLong()
                val revenuesVal = data.totalRevenuesCollected.toLong()
                val occupancyVal = (data.occupancyRate * 10.0).toInt() / 10.0
                val delinquencyVal = (data.delinquencyRate * 10.0).toInt() / 10.0

                // 3. Bento Box KPI Cards populated with dynamic calculations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // KPI 1: Cashflow Net
                    BentoKpiCard(
                        title = "CASHFLOW NET",
                        value = "$cashflowVal XOF",
                        subValue = "Flux net généré",
                        badgeText = if (data.netCashflow >= 0) "Positif" else "Déficitaire",
                        badgeColor = if (data.netCashflow >= 0) Color(0xFF006948) else Color(0xFFBA1A1A),
                        icon = Icons.Default.Star,
                        iconBg = (if (data.netCashflow >= 0) Color(0xFF006948) else Color(0xFFBA1A1A)).copy(alpha = 0.1f),
                        iconColor = if (data.netCashflow >= 0) Color(0xFF006948) else Color(0xFFBA1A1A),
                        modifier = Modifier.weight(1f)
                    )

                    // KPI 2: Revenues collected
                    BentoKpiCard(
                        title = "RECETTES COLLECTÉES",
                        value = "$revenuesVal XOF",
                        subValue = "Loyer + électricité encaissés",
                        icon = Icons.Default.CheckCircle,
                        iconBg = Color(0xFF006948).copy(alpha = 0.1f),
                        iconColor = Color(0xFF006948),
                        modifier = Modifier.weight(1f)
                    )

                    // KPI 3: Occupancy Rate (Progress Ring / Gauge visual)
                    BentoKpiCard(
                        title = "TAUX D'OCCUPATION",
                        value = "$occupancyVal %",
                        subValue = "Unités habitées",
                        progressValue = (data.occupancyRate / 100.0).toFloat(),
                        icon = Icons.Default.Home,
                        iconBg = Color(0xFF1E3A8A).copy(alpha = 0.1f),
                        iconColor = Color(0xFF1E3A8A),
                        modifier = Modifier.weight(1f)
                    )

                    // KPI 4: Delinquency Rate
                    BentoKpiCard(
                        title = "TAUX D'IMPAYÉS (DÉLINQUANCE)",
                        value = "$delinquencyVal %",
                        subValue = "Mensualités non soldées",
                        badgeText = if (data.delinquencyRate > 10.0) "CRITIQUE" else "Maîtrisé",
                        badgeColor = if (data.delinquencyRate > 10.0) Color(0xFFBA1A1A) else Color(0xFF006948),
                        icon = Icons.Default.Warning,
                        iconBg = (if (data.delinquencyRate > 10.0) Color(0xFFBA1A1A) else Color(0xFF006948)).copy(alpha = 0.1f),
                        iconColor = if (data.delinquencyRate > 10.0) Color(0xFFBA1A1A) else Color(0xFF006948),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Quick Actions Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Bolt",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(text = "Actions rapides d'exploitation", style = MaterialTheme.typography.titleLarge)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Nouveau Bail")
                            }
                        }

                        Button(
                            onClick = { viewModel.setShowCreateLogementDialog(true) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Nouveau Logement")
                            }
                        }

                        Button(
                            onClick = { },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Enregistrer Dépense")
                            }
                        }
                    }
                }
            }

            // 5. Dynamic Chart Section (Lower Bento Grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Card 1: Analyse des flux
                Card(
                    modifier = Modifier.weight(1.2f).height(320.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Analyse des flux mensuels", style = MaterialTheme.typography.titleMedium)
                            
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF006948))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Mois", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                }
                                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text("Année", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        // Dynamic Chart visualization based on fetched revenues
                        val flowPercentage = if (data != null && data.totalRevenuesCollected > 0.0) {
                            (data.netCashflow / data.totalRevenuesCollected).toFloat()
                        } else 0.5f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val heights = listOf(0.40f, 0.65f, 0.55f, maxOf(0.1f, flowPercentage), 0.75f)
                            val colors = listOf(
                                Color(0xFF006948).copy(alpha = 0.1f),
                                Color(0xFF006948).copy(alpha = 0.2f),
                                Color(0xFF006948).copy(alpha = 0.4f),
                                Color(0xFF006948).copy(alpha = 0.7f),
                                Color(0xFF006948)
                            )
                            
                            heights.forEachIndexed { i, h ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(h)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(colors[i])
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("Jan", "Fév", "Mar", "Avr", "Mai").forEach { m ->
                                Text(
                                    text = m,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // Card 2: Call to action optimization
                Card(
                    modifier = Modifier.weight(0.8f).height(320.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF006948).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = null,
                                tint = Color(0xFF006948),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "Prêt à optimiser ?", style = MaterialTheme.typography.titleMedium)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Consultez vos rapports détaillés pour identifier de nouvelles opportunités de croissance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { },
                            shape = RoundedCornerShape(9999.dp)
                        ) {
                            Text("Voir les rapports", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BentoKpiCard(
    title: String,
    value: String,
    subValue: String,
    badgeText: String? = null,
    badgeColor: Color = Color.Transparent,
    progressValue: Float? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(150.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                
                if (badgeText != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (progressValue != null) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressValue)
                                .fillMaxHeight()
                                .background(Color(0xFF006948))
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
