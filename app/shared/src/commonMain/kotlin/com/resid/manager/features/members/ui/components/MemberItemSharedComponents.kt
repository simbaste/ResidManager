package com.resid.manager.features.members.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resid.manager.dto.InvitationStatusDto
import com.resid.manager.dto.RoleDto

data class BadgeColors(val background: Color, val content: Color)

@Composable
fun memberRoleColors(role: RoleDto): BadgeColors {
    return when (role) {
        RoleDto.OWNER, RoleDto.ADMIN -> BadgeColors(
            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            content = MaterialTheme.colorScheme.primary,
        )
        RoleDto.TENANT -> BadgeColors(
            background = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        RoleDto.MANAGER -> BadgeColors(
            background = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
            content = MaterialTheme.colorScheme.onErrorContainer,
        )
        RoleDto.STAFF -> BadgeColors(
            background = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
fun memberStatusColors(status: InvitationStatusDto): BadgeColors {
    return when (status) {
        InvitationStatusDto.ACCEPTED -> BadgeColors(
            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            content = MaterialTheme.colorScheme.primary,
        )
        InvitationStatusDto.INVITED, InvitationStatusDto.PENDING_APPROVAL -> BadgeColors(
            background = Color(0xFFF59E0B).copy(alpha = 0.18f),
            content = Color(0xFFF59E0B),
        )
        InvitationStatusDto.DECLINED -> BadgeColors(
            background = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
            content = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
fun memberAvatarColors(role: RoleDto): BadgeColors {
    return when (role) {
        RoleDto.OWNER -> BadgeColors(
            background = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        RoleDto.ADMIN -> BadgeColors(
            background = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        RoleDto.TENANT -> BadgeColors(
            background = MaterialTheme.colorScheme.secondary,
            content = MaterialTheme.colorScheme.onSecondary,
        )
        RoleDto.MANAGER -> BadgeColors(
            background = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
        )
        RoleDto.STAFF -> BadgeColors(
            background = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
fun MemberAvatar(
    firstName: String,
    lastName: String,
    role: RoleDto,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val initials = rememberMemberInitials(firstName, lastName)
    val (bg, fg) = memberAvatarColors(role)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
            fontSize = if (size < 40.dp) 12.sp else 15.sp,
        )
    }
}

fun rememberMemberInitials(firstName: String, lastName: String): String {
    val f = firstName.trim().firstOrNull()?.uppercaseChar() ?: ""
    val l = lastName.trim().firstOrNull()?.uppercaseChar() ?: ""
    val combined = "$f$l"
    return combined.ifBlank { "M" }
}

@Composable
fun RoleBadge(
    role: RoleDto,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) = memberRoleColors(role)
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(9999.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.2f)),
        modifier = modifier,
    ) {
        Text(
            text = role.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun StatusBadge(
    status: InvitationStatusDto,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) = memberStatusColors(status)
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(9999.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.2f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = fg,
            )
        }
    }
}
