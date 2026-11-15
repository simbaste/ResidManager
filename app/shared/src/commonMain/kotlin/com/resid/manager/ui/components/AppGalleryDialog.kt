package com.resid.manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.resid.manager.util.ImageFormatDetector

/**
 * Modèle générique pour la gestion des photos de galerie.
 */
data class GalleryPhotoItem(
    val id: String,
    val url: String? = null,
    val bytes: ByteArray? = null,
    val description: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GalleryPhotoItem

        if (id != other.id) return false
        if (url != other.url) return false
        if (bytes != null) {
            if (other.bytes == null) return false
            if (!bytes.contentEquals(other.bytes)) return false
        } else if (other.bytes != null) return false
        if (description != other.description) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        result = 31 * result + description.hashCode()
        return result
    }
}

val DEFAULT_GALLERY_TAGS = listOf("Façade", "Salon", "Chambre", "Cuisine", "Salle de bain", "Balcon / Terrasse", "Entrée")

/**
 * Composant de modale générique de gestion et d'édition de galerie photo.
 * Utilisable pour les résidences, logements/unités, états des lieux, etc.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppGalleryContent(
    title: String = "Galerie photo",
    icon: ImageVector = Icons.Default.PhotoLibrary,
    initialPhotos: List<GalleryPhotoItem>,
    suggestedTags: List<String> = DEFAULT_GALLERY_TAGS,
    defaultDescription: String = "Photo",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (List<GalleryPhotoItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val photos = remember {
        mutableStateListOf<GalleryPhotoItem>().apply {
            addAll(initialPhotos)
        }
    }

    var editingPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var editDescriptionText by remember { mutableStateOf("") }

    var pendingImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingDescription by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isDraggingOverUpload by remember { mutableStateOf(false) }

    // État pour le Drag & Drop par appui long
    val lazyGridState = rememberLazyGridState()
    var draggedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var dragAccumulatedOffset by remember { mutableStateOf(Offset.Zero) }

    fun handleIncomingBytes(bytes: ByteArray) {
        val format = ImageFormatDetector.detect(bytes)
        if (format.isHeic) {
            validationError = "Le format HEIC n'est pas supporté. Veuillez choisir du JPG, PNG ou WebP."
            return
        }
        validationError = null
        pendingImageBytes = bytes
        if (pendingDescription.isBlank()) {
            pendingDescription = defaultDescription
        }
    }

    val imagePicker = rememberImagePickerLauncher { bytes ->
        handleIncomingBytes(bytes)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val isCompact = maxWidth < 600.dp
        val isMedium = maxWidth in 600.dp..900.dp
        val gridColumns = if (isCompact) 2 else if (isMedium) 3 else 4

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 16.dp else 24.dp)
        ) {
            // Header de la modale
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${photos.size} photo${if (photos.size > 1) "s" else ""} enregistrée${if (photos.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Message d'erreur
            val displayedError = validationError ?: errorMessage
            if (!displayedError.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = displayedError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Zone d'ajout immédiat lorsqu'une image vient d'être sélectionnée
                    if (pendingImageBytes != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Nouvelle photo prête à être ajoutée",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            pendingImageBytes = null
                                            pendingDescription = ""
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Annuler",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Prévisualisation miniature
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        AsyncImage(
                                            model = pendingImageBytes,
                                            contentDescription = "Prévisualisation",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Champ texte description
                                    Column(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = pendingDescription,
                                            onValueChange = { pendingDescription = it },
                                            label = { Text("Description / Pièce") },
                                            placeholder = { Text("Ex: Salon principal") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Suggestions de pièces
                                if (suggestedTags.isNotEmpty()) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        suggestedTags.forEach { tag ->
                                            FilterChip(
                                                selected = pendingDescription.equals(tag, ignoreCase = true),
                                                onClick = { pendingDescription = tag },
                                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            val newId = "new_${com.resid.manager.getCurrentEpochMillis()}"
                                            photos.add(
                                                GalleryPhotoItem(
                                                    id = newId,
                                                    bytes = pendingImageBytes,
                                                    description = pendingDescription.ifBlank { defaultDescription }
                                                )
                                            )
                                            pendingImageBytes = null
                                            pendingDescription = ""
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Valider la photo")
                                    }
                                }
                            }
                        }
                    }

                    // Grille responsive contenant la carte d'upload + photos existantes
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        state = lazyGridState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Carte d'upload permanente
                        item(key = "upload_card") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { imagePicker.launch() }
                                    .imageDropTarget(
                                        onImageDropped = { bytes -> handleIncomingBytes(bytes) },
                                        onDragStateChanged = { isDraggingOverUpload = it }
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDraggingOverUpload) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(
                                    width = if (isDraggingOverUpload) 2.dp else 1.5.dp,
                                    color = if (isDraggingOverUpload) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Ajouter",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "+ Ajouter une photo",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Glisser ou cliquer\nJPG, PNG, WebP",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        // 2. Photos existantes
                        itemsIndexed(photos, key = { _, item -> item.id }) { index, photoItem ->
                            val isDragged = draggedPhotoIndex == index
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .offset {
                                        if (isDragged) {
                                            IntOffset(dragAccumulatedOffset.x.toInt(), dragAccumulatedOffset.y.toInt())
                                        } else {
                                            IntOffset.Zero
                                        }
                                    }
                                    .graphicsLayer {
                                        if (isDragged) {
                                            scaleX = 1.08f
                                            scaleY = 1.08f
                                            shadowElevation = 16f
                                        }
                                    }
                                    .pointerInput(photos) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedPhotoIndex = index
                                                dragAccumulatedOffset = Offset.Zero
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragAccumulatedOffset += dragAmount

                                                val currentDragged = draggedPhotoIndex
                                                if (currentDragged != null) {
                                                    // Calcul du déplacement vers un élément voisin
                                                    // item 0 est l'upload card, donc les photos sont indexées avec offset 1 dans la grille
                                                    val visibleItems = lazyGridState.layoutInfo.visibleItemsInfo
                                                    val draggedLayoutItem = visibleItems.find { it.key == photoItem.id }
                                                    if (draggedLayoutItem != null) {
                                                        val targetCenterX = draggedLayoutItem.offset.x + draggedLayoutItem.size.width / 2 + dragAccumulatedOffset.x
                                                        val targetCenterY = draggedLayoutItem.offset.y + draggedLayoutItem.size.height / 2 + dragAccumulatedOffset.y

                                                        val hoveredItem = visibleItems.find { item ->
                                                            item.key != "upload_card" && item.key != photoItem.id &&
                                                            targetCenterX in item.offset.x.toFloat()..(item.offset.x + item.size.width).toFloat() &&
                                                            targetCenterY in item.offset.y.toFloat()..(item.offset.y + item.size.height).toFloat()
                                                        }

                                                        if (hoveredItem != null) {
                                                            val targetIndex = photos.indexOfFirst { it.id == hoveredItem.key }
                                                            if (targetIndex != -1 && targetIndex != currentDragged) {
                                                                val moved = photos.removeAt(currentDragged)
                                                                photos.add(targetIndex, moved)
                                                                draggedPhotoIndex = targetIndex
                                                                dragAccumulatedOffset = Offset.Zero
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                draggedPhotoIndex = null
                                                dragAccumulatedOffset = Offset.Zero
                                            },
                                            onDragCancel = {
                                                draggedPhotoIndex = null
                                                dragAccumulatedOffset = Offset.Zero
                                            }
                                        )
                                    },
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isDragged) 8.dp else 2.dp),
                                border = BorderStroke(
                                    width = if (isDragged) 2.dp else 1.dp,
                                    color = if (isDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Image
                                    if (photoItem.bytes != null) {
                                        AsyncImage(
                                            model = photoItem.bytes,
                                            contentDescription = photoItem.description,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (!photoItem.url.isNullOrBlank()) {
                                        AsyncImage(
                                            model = photoItem.url,
                                            contentDescription = photoItem.description,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }

                                    // Badge Étoile en haut à gauche pour la photo principale
                                    if (index == 0) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                                                .padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Photo principale",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Principale",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    } else {
                                        // Bouton pour définir directement comme photo principale
                                        IconButton(
                                            onClick = {
                                                val itemToMove = photos.removeAt(index)
                                                photos.add(0, itemToMove)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.55f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Définir comme photo principale",
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    // Boutons d'action rapides en haut à droite (Édition & Suppression - les flèches ont été supprimées au profit du Drag & Drop)
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                editingPhotoIndex = index
                                                editDescriptionText = photoItem.description
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.55f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Modifier la description",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { photos.removeAt(index) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.55f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Supprimer la photo",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    // Overlay sombre en bas avec description / pièce
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.85f)
                                                    )
                                                )
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = photoItem.description.ifBlank { "Sans titre" },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pied de modale : Boutons d'action principaux
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isLoading
                        ) {
                            Text("Annuler")
                        }

                        Button(
                            onClick = { onSave(photos.toList()) },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enregistrement...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enregistrer les modifications", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Dialogue d'édition de description d'une photo
                if (editingPhotoIndex != null) {
                    val targetIdx = editingPhotoIndex!!
                    if (targetIdx in photos.indices) {
                        AlertDialog(
                            onDismissRequest = { editingPhotoIndex = null },
                            title = { Text("Modifier la description") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = editDescriptionText,
                                        onValueChange = { editDescriptionText = it },
                                        label = { Text("Description / Pièce") },
                                        placeholder = { Text("Ex: Salon, Chambre...") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (suggestedTags.isNotEmpty()) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            suggestedTags.forEach { tag ->
                                                FilterChip(
                                                    selected = editDescriptionText.equals(tag, ignoreCase = true),
                                                    onClick = { editDescriptionText = tag },
                                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val currentItem = photos[targetIdx]
                                        photos[targetIdx] = currentItem.copy(description = editDescriptionText)
                                        editingPhotoIndex = null
                                    }
                                ) {
                                    Text("Appliquer")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { editingPhotoIndex = null }) {
                                    Text("Annuler")
                                }
                            }
                        )
                    }
                }
            }
        }


/**
 * Modale de galerie photo basée sur [AppGalleryContent].
 */
@Composable
fun AppGalleryDialog(
    title: String = "Galerie photo",
    icon: ImageVector = Icons.Default.PhotoLibrary,
    initialPhotos: List<GalleryPhotoItem>,
    suggestedTags: List<String> = DEFAULT_GALLERY_TAGS,
    defaultDescription: String = "Photo",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (List<GalleryPhotoItem>) -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            AppGalleryContent(
                title = title,
                icon = icon,
                initialPhotos = initialPhotos,
                suggestedTags = suggestedTags,
                defaultDescription = defaultDescription,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onDismiss = onDismiss,
                onSave = onSave
            )
        }
    }
}