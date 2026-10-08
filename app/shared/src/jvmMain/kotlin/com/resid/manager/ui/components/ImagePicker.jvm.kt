package com.resid.manager.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberImagePickerLauncher(
    onImagePicked: (ByteArray) -> Unit
): ImagePickerLauncher {
    return remember {
        object : ImagePickerLauncher {
            override fun launch() {
                val chooser = JFileChooser()
                val filter = FileNameExtensionFilter("Images (*.jpg, *.png, *.webp)", "jpg", "jpeg", "png", "webp")
                chooser.fileFilter = filter
                chooser.isAcceptAllFileFilterUsed = false
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    val file = chooser.selectedFile
                    if (file != null && file.exists()) {
                        onImagePicked(file.readBytes())
                    }
                }
            }
        }
    }
}

@Composable
actual fun Modifier.imageDropTarget(
    onImageDropped: (ByteArray) -> Unit,
    onDragStateChanged: (Boolean) -> Unit
): Modifier = this
