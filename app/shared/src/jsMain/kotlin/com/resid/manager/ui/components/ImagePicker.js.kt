package com.resid.manager.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.files.get

@Composable
actual fun rememberImagePickerLauncher(
    onImagePicked: (ByteArray) -> Unit
): ImagePickerLauncher {
    return remember {
        object : ImagePickerLauncher {
            override fun launch() {
                val input = document.createElement("input") as HTMLInputElement
                input.type = "file"
                input.accept = "image/*"
                input.style.display = "none"

                input.onchange = {
                    val file = input.files?.get(0)
                    if (file != null && file.type.startsWith("image/")) {
                        readFileAsByteArray(file, onImagePicked)
                    }
                    input.remove()
                }

                document.body?.appendChild(input)
                input.click()
            }
        }
    }
}

@Composable
actual fun Modifier.imageDropTarget(
    onImageDropped: (ByteArray) -> Unit,
    onDragStateChanged: (Boolean) -> Unit
): Modifier {
    DisposableEffect(Unit) {
        val onDragOver: (org.w3c.dom.events.Event) -> Unit = { e ->
            e.preventDefault()
            onDragStateChanged(true)
        }

        val onDragLeave: (org.w3c.dom.events.Event) -> Unit = { e ->
            e.preventDefault()
            onDragStateChanged(false)
        }

        val onDrop: (org.w3c.dom.events.Event) -> Unit = { e ->
            e.preventDefault()
            onDragStateChanged(false)
            val dragEvent = e as? DragEvent
            val file = dragEvent?.dataTransfer?.files?.get(0)
            if (file != null && file.type.startsWith("image/")) {
                readFileAsByteArray(file, onImageDropped)
            }
        }

        window.addEventListener("dragover", onDragOver)
        window.addEventListener("dragleave", onDragLeave)
        window.addEventListener("drop", onDrop)

        onDispose {
            window.removeEventListener("dragover", onDragOver)
            window.removeEventListener("dragleave", onDragLeave)
            window.removeEventListener("drop", onDrop)
        }
    }
    return this
}

private fun readFileAsByteArray(file: File, callback: (ByteArray) -> Unit) {
    val reader = FileReader()
    reader.onload = {
        val buffer = reader.result as? ArrayBuffer
        if (buffer != null) {
            val int8Array = Int8Array(buffer)
            val bytes = ByteArray(int8Array.length) { i ->
                js("int8Array[i]").unsafeCast<Byte>()
            }
            callback(bytes)
        }
    }
    reader.readAsArrayBuffer(file)
}
