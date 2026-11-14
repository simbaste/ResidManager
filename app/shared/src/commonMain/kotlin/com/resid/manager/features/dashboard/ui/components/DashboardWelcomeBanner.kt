package com.resid.manager.features.dashboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.resid.manager.ui.theme.ResidTheme

@Composable
fun DashboardWelcomeBanner(
    isCompact: Boolean,
    firstName: String,
    residenceName: String,
    roleName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF006948)), // Brand deep green
        shape = RoundedCornerShape(24.dp)
    ) {
        if (isCompact) {
            DashboardWelcomeBannerCompact(
                firstName = firstName,
                residenceName = residenceName,
                roleName = roleName
            )
        } else {
            DashboardWelcomeBannerDefault(
                firstName = firstName,
                residenceName = residenceName,
                roleName = roleName
            )
        }
    }
}

@Composable
private fun DashboardWelcomeBannerDefault(
    firstName: String,
    residenceName: String,
    roleName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = 10.dp,
        ).fillMaxWidth().then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Bonjour, $firstName !",
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
                    text = "Vous consultez la résidence : $residenceName",
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
                text = "RÔLE : $roleName",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun DashboardWelcomeBannerCompact(
    firstName: String,
    residenceName: String,
    roleName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier.padding(
            horizontal = 8.dp,
            vertical = 10.dp,
        ).fillMaxWidth().then(modifier),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .wrapContentWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shape = RoundedCornerShape(9999.dp)
        ) {
            Text(
                text = "RÔLE : $roleName",
                textAlign = TextAlign.Center,
                maxLines = 2,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Bonjour, $firstName !",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Vous consultez la résidence : $residenceName",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF85F8C4)
                )
            }
        }

    }
}

@PreviewLightDark
@Composable
fun DashboardWelcomeBannerPreview() {
    ResidTheme {
        DashboardWelcomeBanner(
            isCompact = true,
            firstName = "Amadou",
            residenceName = "Résidence Les Palmiers",
            roleName = "Propriétaire"
        )
    }
}
