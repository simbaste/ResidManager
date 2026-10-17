package com.resid.manager.features.auth.ui.components.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.resid.manager.ui.components.imageDropTarget
import com.resid.manager.ui.components.rememberImagePickerLauncher
import com.resid.manager.ui.theme.residColors
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
fun RegisterAvatarPicker(
    avatarBytes: ByteArray?,
    onAvatarSelected: (ByteArray) -> Unit,
    onRemoveAvatar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDraggingOver by remember { mutableStateOf(false) }
    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    androidx.compose.runtime.LaunchedEffect(avatarBytes) {
        if (avatarBytes == null) {
            avatarBitmap = null
        }
    }

    val imagePicker = rememberImagePickerLauncher { bytes ->
        try {
            avatarBitmap = bytes.decodeToImageBitmap()
        } catch (_: Exception) {
            avatarBitmap = null
        }
        onAvatarSelected(bytes)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    if (isDraggingOver) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                )
                .border(
                    width = if (isDraggingOver) 2.5.dp else 2.dp,
                    color = if (isDraggingOver || avatarBitmap != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.residColors.cardBorder,
                    shape = CircleShape
                )
                .imageDropTarget(
                    onImageDropped = { bytes ->
                        try {
                            avatarBitmap = bytes.decodeToImageBitmap()
                        } catch (_: Exception) {
                            avatarBitmap = null
                        }
                        onAvatarSelected(bytes)
                    },
                    onDragStateChanged = { dragging ->
                        isDraggingOver = dragging
                    }
                )
                .clickable { imagePicker.launch() },
            contentAlignment = Alignment.Center
        ) {
            if (avatarBitmap != null) {
                Image(
                    bitmap = avatarBitmap!!,
                    contentDescription = "Photo de profil sélectionnée",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Ajouter une photo de profil",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Hover / Action Overlay with Camera Icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (avatarBitmap != null) 0.25f else 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Changer la photo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
