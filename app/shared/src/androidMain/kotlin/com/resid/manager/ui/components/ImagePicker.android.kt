package com.resid.manager.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImagePickerLauncher(
    onImagePicked: (ByteArray) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.use { stream ->
                stream.readBytes()
            }
            if (bytes != null) {
                onImagePicked(bytes)
            }
        }
    }

    return remember(launcher) {
        object : ImagePickerLauncher {
            override fun launch() {
                launcher.launch("image/*")
            }
        }
    }
}

@Composable
actual fun Modifier.imageDropTarget(
    onImageDropped: (ByteArray) -> Unit,
    onDragStateChanged: (Boolean) -> Unit
): Modifier = this
