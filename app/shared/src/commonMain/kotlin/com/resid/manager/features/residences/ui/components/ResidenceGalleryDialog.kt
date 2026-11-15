package com.resid.manager.features.residences.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.resid.manager.ui.components.AppGalleryContent
import com.resid.manager.ui.components.DEFAULT_GALLERY_TAGS
import com.resid.manager.ui.components.GalleryPhotoItem

/**
 * Contenu adaptatif de la galerie photo pour les résidences (utilisable dans NavEntry / adaptiveModal).
 */
@Composable
fun ResidenceGalleryContent(
    initialPhotos: List<GalleryPhotoItem>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (List<GalleryPhotoItem>) -> Unit
) {
    AppGalleryContent(
        title = "Galerie photo de la résidence",
        initialPhotos = initialPhotos,
        suggestedTags = DEFAULT_GALLERY_TAGS,
        defaultDescription = "Pièce",
        isLoading = isLoading,
        errorMessage = errorMessage,
        onDismiss = onDismiss,
        onSave = onSave,
        modifier = modifier
    )
}
