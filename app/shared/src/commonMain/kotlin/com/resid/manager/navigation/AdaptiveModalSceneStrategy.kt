@file:OptIn(ExperimentalMaterial3Api::class)

package com.resid.manager.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

data class AdaptiveModalProperties(
    val dialogProperties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    val bottomSheetProperties: ModalBottomSheetProperties = ModalBottomSheetProperties()
)

object AdaptiveModalKey : NavMetadataKey<AdaptiveModalProperties>

fun adaptiveModal(
    dialogProperties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    bottomSheetProperties: ModalBottomSheetProperties = ModalBottomSheetProperties()
) = metadata {
    put(AdaptiveModalKey, AdaptiveModalProperties(dialogProperties, bottomSheetProperties))
}

@OptIn(ExperimentalMaterial3Api::class)
internal data class AdaptiveBottomSheetScene<T : Any>(
    override val key: T,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val properties: ModalBottomSheetProperties,
    private val onBack: () -> Unit
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)
    private var sheetState: SheetState? = null

    override val content: @Composable (() -> Unit) = {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        sheetState = state

        ModalBottomSheet(
            onDismissRequest = onBack,
            properties = properties,
            sheetState = state,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                entry.Content()
            }
        }
    }

    override suspend fun onRemove() {
        sheetState?.hide()
    }
}

internal data class AdaptiveDialogScene<T : Any>(
    override val key: T,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val properties: DialogProperties,
    private val onBack: () -> Unit
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable (() -> Unit) = {
        Dialog(
            onDismissRequest = onBack,
            properties = properties
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 560.dp)
                        .heightIn(max = 640.dp)
                ) {
                    entry.Content()
                }
            }
        }
    }
}

class AdaptiveModalSceneStrategy<T : Any>(
    private val isCompact: Boolean
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null
        val modalProps = lastEntry.metadata[AdaptiveModalKey] ?: return null

        return if (isCompact) {
            @Suppress("UNCHECKED_CAST")
            AdaptiveBottomSheetScene(
                key = lastEntry.contentKey as T,
                previousEntries = entries.dropLast(1),
                overlaidEntries = entries.dropLast(1),
                entry = lastEntry,
                properties = modalProps.bottomSheetProperties,
                onBack = onBack
            )
        } else {
            @Suppress("UNCHECKED_CAST")
            AdaptiveDialogScene(
                key = lastEntry.contentKey as T,
                previousEntries = entries.dropLast(1),
                overlaidEntries = entries.dropLast(1),
                entry = lastEntry,
                properties = modalProps.dialogProperties,
                onBack = onBack
            )
        }
    }
}
