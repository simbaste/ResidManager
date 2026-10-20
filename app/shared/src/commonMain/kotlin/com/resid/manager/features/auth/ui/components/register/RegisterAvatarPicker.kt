package com.resid.manager.features.auth.ui.components.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resid.manager.ui.components.imageDropTarget
import com.resid.manager.ui.components.rememberImagePickerLauncher
import com.resid.manager.ui.theme.residColors
import com.resid.manager.util.ImageFormatDetector

@Composable
fun RegisterAvatarPicker(
    avatarBytes: ByteArray?,
    onAvatarSelected: (ByteArray, String, String) -> Unit,
    onRemoveAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    onInvalidFormat: (String) -> Unit = {}
) {
    var isDraggingOver by remember { mutableStateOf(false) }

    fun processBytes(bytes: ByteArray) {
        val format = ImageFormatDetector.detect(bytes)
        if (format.isHeic) {
            onInvalidFormat("Le format HEIC n'est pas supporté sur cette plateforme. Veuillez sélectionner une image JPEG, PNG ou WebP.")
            return
        }
        onAvatarSelected(bytes, format.mimeType, format.extension)
    }

    val imagePicker = rememberImagePickerLauncher { bytes ->
        processBytes(bytes)
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
                    color = if (isDraggingOver || avatarBytes != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.residColors.cardBorder,
                    shape = CircleShape
                )
                .imageDropTarget(
                    onImageDropped = { bytes ->
                        processBytes(bytes)
                    },
                    onDragStateChanged = { dragging ->
                        isDraggingOver = dragging
                    }
                )
                .clickable { imagePicker.launch() },
            contentAlignment = Alignment.Center
        ) {
            if (avatarBytes != null) {
                AsyncImage(
                    model = avatarBytes,
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
                    .background(Color.Black.copy(alpha = if (avatarBytes != null) 0.25f else 0.12f), CircleShape),
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

        if (avatarBytes != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clickable { onRemoveAvatar() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Supprimer la photo",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

