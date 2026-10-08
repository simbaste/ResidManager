package com.resid.manager.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberImagePickerLauncher(
    onImagePicked: (ByteArray) -> Unit
): ImagePickerLauncher {
    return remember {
        object : ImagePickerLauncher {
            private var currentDelegate: NSObject? = null

            override fun launch() {
                val picker = UIImagePickerController()
                picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary

                val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
                    override fun imagePickerController(
                        picker: UIImagePickerController,
                        didFinishPickingImage: UIImage,
                        editingInfo: Map<Any?, *>?
                    ) {
                        picker.dismissViewControllerAnimated(true, null)
                        currentDelegate = null
                        val nsData: NSData? = UIImageJPEGRepresentation(didFinishPickingImage, 0.85)
                        nsData?.let { data ->
                            val bytes = ByteArray(data.length.toInt())
                            if (bytes.isNotEmpty()) {
                                bytes.usePinned { pinned ->
                                    memcpy(pinned.addressOf(0), data.bytes, data.length)
                                }
                                onImagePicked(bytes)
                            }
                        }
                    }

                    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                        picker.dismissViewControllerAnimated(true, null)
                        currentDelegate = null
                    }
                }

                currentDelegate = delegate
                picker.delegate = delegate

                val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
                var topController = rootController
                while (topController?.presentedViewController != null) {
                    topController = topController.presentedViewController
                }
                topController?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

@Composable
actual fun Modifier.imageDropTarget(
    onImageDropped: (ByteArray) -> Unit,
    onDragStateChanged: (Boolean) -> Unit
): Modifier = this
