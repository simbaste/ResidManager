package com.resid.manager.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
actual fun rememberImagePickerLauncher(
    onImagePicked: (ByteArray) -> Unit
): ImagePickerLauncher {
    return remember {
        object : ImagePickerLauncher {
            override fun launch() {
                // WasmJs fallback
            }
        }
    }
}

@Composable
actual fun Modifier.imageDropTarget(
    onImageDropped: (ByteArray) -> Unit,
    onDragStateChanged: (Boolean) -> Unit
): Modifier = this
