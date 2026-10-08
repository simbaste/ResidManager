package com.resid.manager.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Interface representing a platform-specific photo picker launcher.
 */
interface ImagePickerLauncher {
    fun launch()
}

/**
 * Creates and remembers a photo picker launcher restricted to images (JPEG, PNG, WebP).
 */
@Composable
expect fun rememberImagePickerLauncher(
    onImagePicked: (ByteArray) -> Unit
): ImagePickerLauncher

/**
 * Adds platform drag-and-drop file receiving support where supported (e.g. Web).
 */
@Composable
expect fun Modifier.imageDropTarget(
    onImageDropped: (ByteArray) -> Unit,
    onDragStateChanged: (Boolean) -> Unit = {}
): Modifier
