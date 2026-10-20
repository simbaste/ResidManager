package com.resid.manager.features.auth.ui.components.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.resid.manager.ui.components.imageDropTarget
import com.resid.manager.ui.components.rememberImagePickerLauncher
import com.resid.manager.ui.theme.ResidTheme
import com.resid.manager.ui.theme.residColors
import com.resid.manager.util.ImageFormatDetector

/**
 * Section de sélection et d'aperçu de photo de profil pour l'inscription.
 * Supporte le clic, le glisser-déposer et affiche les erreurs de format (HEIC/invalide) directement.
 */
@Composable
fun RegisterProfilePhotoSection(
    avatarBytes: ByteArray?,
    onAvatarSelected: (ByteArray, String, String) -> Unit,
    onRemoveAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    errorMessage: String? = null,
    onInvalidFormat: (String) -> Unit = {}
) {
    var isDraggingOver by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    fun processBytes(bytes: ByteArray) {
        val format = ImageFormatDetector.detect(bytes)
        if (format.isHeic) {
            val errorMsg = "Le format HEIC n'est pas supporté sur cette plateforme. Veuillez sélectionner une image JPEG, PNG ou WebP."
            localError = errorMsg
            onInvalidFormat(errorMsg)
            return
        }
        localError = null
        onAvatarSelected(bytes, format.mimeType, format.extension)
    }

    val imagePicker = rememberImagePickerLauncher { bytes ->
        processBytes(bytes)
    }

    val displayError = localError ?: errorMessage
    val hasError = displayError != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        hasError -> MaterialTheme.colorScheme.error.copy(alpha = 0.06f)
                        isDraggingOver -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else -> MaterialTheme.residColors.inputBackground
                    },
                    RoundedCornerShape(16.dp)
                )
                .border(
                    width = if (isDraggingOver || hasError) 2.dp else 1.dp,
                    color = when {
                        hasError -> MaterialTheme.colorScheme.error
                        isDraggingOver -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.residColors.inputBorder
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .imageDropTarget(
                    onImageDropped = { bytes ->
                        processBytes(bytes)
                    },
                    onDragStateChanged = { dragging ->
                        isDraggingOver = dragging
                    }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar Circular Container with optional Delete Badge
            val hasPhoto = avatarBytes != null || !avatarUrl.isNullOrBlank()

            Box(
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(
                            width = 2.dp,
                            color = when {
                                hasError -> MaterialTheme.colorScheme.error
                                hasPhoto -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            },
                            shape = CircleShape
                        )
                        .clickable { imagePicker.launch() },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBytes != null) {
                        AsyncImage(
                            model = avatarBytes,
                            contentDescription = "Photo de profil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Photo de profil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Portrait",
                            tint = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Hover / Action Overlay with Camera Icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = if (hasPhoto) 0.25f else 0.15f), CircleShape),
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

                if (hasPhoto) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable {
                                localError = null
                                onRemoveAvatar()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Supprimer la photo",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(12.dp)
                        )
                    }
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
                        color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (hasPhoto) "(Photo ajoutée)" else "(Optionnel)",
                        color = if (hasPhoto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = if (hasPhoto) FontWeight.Medium else FontWeight.Normal
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

        // Inline Error Message Display
        AnimatedVisibility(
            visible = hasError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = displayError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun RegisterProfilePhotoSectionPreview() {
    ResidTheme {
        RegisterProfilePhotoSection(
            avatarBytes = null,
            onAvatarSelected = { _, _, _ -> },
            onRemoveAvatar = {}
        )
    }
}
