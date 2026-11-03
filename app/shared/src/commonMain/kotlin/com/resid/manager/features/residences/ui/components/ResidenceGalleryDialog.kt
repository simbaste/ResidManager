package com.resid.manager.features.residences.ui.components

import androidx.compose.runtime.Composable
import com.resid.manager.ui.components.AppGalleryDialog
import com.resid.manager.ui.components.DEFAULT_GALLERY_TAGS
import com.resid.manager.ui.components.GalleryPhotoItem

/**
 * Typealias conservé pour compatibilité ascendante.
 */
typealias GalleryPhotoItem = GalleryPhotoItem

/**
 * Modale de galerie pour les résidences, utilisant le composant générique [AppGalleryDialog].
 */
@Composable
fun ResidenceGalleryDialog(
    initialPhotos: List<GalleryPhotoItem>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (List<GalleryPhotoItem>) -> Unit
) {
    AppGalleryDialog(
        title = "Galerie photo de la résidence",
        initialPhotos = initialPhotos,
        suggestedTags = DEFAULT_GALLERY_TAGS,
        defaultDescription = "Pièce",
        isLoading = isLoading,
        errorMessage = errorMessage,
        onDismiss = onDismiss,
        onSave = onSave
    )
}
