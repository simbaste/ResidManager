package com.resid.manager.features.auth.ui.components.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resid.manager.ui.components.imageDropTarget
import com.resid.manager.ui.components.rememberImagePickerLauncher
import com.resid.manager.ui.theme.residColors

/**
 * Profile Photo Upload Section with Click-to-Pick and Drag-and-Drop (Images only).
 */
@Composable
fun RegisterProfilePhotoSection(
    selectedImage: ImageBitmap?,
    onImageSelected: (ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDraggingOver by remember { mutableStateOf(false) }
    val imagePicker = rememberImagePickerLauncher { bytes ->
        onImageSelected(bytes)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isDraggingOver) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.residColors.inputBackground,
                RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isDraggingOver) 2.dp else 1.dp,
                color = if (isDraggingOver) MaterialTheme.colorScheme.primary else MaterialTheme.residColors.inputBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .imageDropTarget(
                onImageDropped = { bytes ->
                    onImageSelected(bytes)
                },
                onDragStateChanged = { dragging ->
                    isDraggingOver = dragging
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(
                    width = 2.dp,
                    color = if (selectedImage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .clickable { imagePicker.launch() },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImage != null) {
                Image(
                    bitmap = selectedImage,
                    contentDescription = "Photo de profil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Portrait",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Hover / Action Overlay with Camera Icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (selectedImage != null) 0.25f else 0.15f), CircleShape),
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

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { imagePicker.launch() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Photo de profil",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (selectedImage != null) "(Photo ajoutée)" else "(Optionnel)",
                    color = if (selectedImage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = if (selectedImage != null) FontWeight.Medium else FontWeight.Normal
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isDraggingOver) "Déposez votre image ici..." else "Glissez-déposez ou cliquez pour importer votre portrait.",
                color = if (isDraggingOver) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Text(
                text = "Format JPG, PNG, WebP • Images uniquement",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
