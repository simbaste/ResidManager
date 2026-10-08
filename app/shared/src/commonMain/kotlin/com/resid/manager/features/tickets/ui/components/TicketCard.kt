package com.resid.manager.features.tickets.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.TicketDto
import com.resid.manager.dto.TicketStatusDto
import com.resid.manager.dto.TicketUrgencyDto

@Composable
fun TicketCard(
    ticket: TicketDto,
    unitName: String,
    isAuthorized: Boolean,
    onTakeChargeClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusBg) = when (ticket.status) {
        TicketStatusDto.OPEN -> Color(0xFFBA1A1A) to Color(0xFFFDE8E8)
        TicketStatusDto.IN_PROGRESS -> Color(0xFFD97706) to Color(0xFFFEF3C7)
        TicketStatusDto.CLOSED -> Color(0xFF006948) to Color(0xFFE6F7F0)
    }

    val (urgencyColor, urgencyBg) = when (ticket.urgency) {
        TicketUrgencyDto.LOW -> Color(0xFF475569) to Color(0xFFF1F5F9)
        TicketUrgencyDto.MEDIUM -> Color(0xFFD97706) to Color(0xFFFEF3C7)
        TicketUrgencyDto.CRITICAL -> Color(0xFFBA1A1A) to Color(0xFFFDE8E8)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = statusBg),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = ticket.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = urgencyBg),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, urgencyColor.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "Urgence: ${ticket.urgency.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = urgencyColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(ticket.title, style = MaterialTheme.typography.titleLarge, color = Color(0xFF006948))
                Text("Unité : $unitName | Catégorie : ${ticket.category.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }

            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (ticket.status == TicketStatusDto.CLOSED || ticket.interventionCost > 0.0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Coût cumulé facturé :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    Text("${ticket.interventionCost} XOF", style = MaterialTheme.typography.titleLarge, color = Color(0xFF006948))
                }
            }

            // State machine actions
            if (ticket.status != TicketStatusDto.CLOSED && isAuthorized) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ticket.status == TicketStatusDto.OPEN) {
                        Button(
                            onClick = onTakeChargeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Prendre en charge")
                            }
                        }
                    } else if (ticket.status == TicketStatusDto.IN_PROGRESS) {
                        Button(
                            onClick = onCloseClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948))
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Clôturer l'incident")
                            }
                        }
                    }
                }
            }
        }
    }
}
