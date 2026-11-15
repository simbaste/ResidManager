package com.resid.manager.features.units.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resid.manager.ui.components.AmountTextField
import com.resid.manager.ui.components.AppGalleryContent
import com.resid.manager.ui.components.AppTextField
import com.resid.manager.ui.components.GalleryPhotoItem
import com.resid.manager.ui.theme.ResidTheme

private enum class CreateUnitStep {
    FORM,
    GALLERY
}

private val DEFAULT_UNIT_TAGS = listOf(
    "Pièce principale",
    "Chambre",
    "Cuisine",
    "Salle de bain",
    "Balcon / Terrasse",
    "Entrée",
    "Vue extérieure"
)

@Composable
fun CreateUnitContent(
    modifier: Modifier = Modifier,
    currencySymbol: String = "XOF",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double, Double, Double, List<String>, List<GalleryPhotoItem>) -> Unit,
) {
    var currentStep by remember { mutableStateOf(CreateUnitStep.FORM) }
    var name by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var nominalRent by remember { mutableStateOf("") }
    var serviceCharges by remember { mutableStateOf("") }
    var initialElectricityIndex by remember { mutableStateOf("0.0") }
    var selectedEquipments by remember { mutableStateOf(emptySet<String>()) }
    var draftPhotos by remember { mutableStateOf<List<GalleryPhotoItem>>(emptyList()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    if (currentStep == CreateUnitStep.GALLERY) {
        AppGalleryContent(
            title = "Galerie photo du logement",
            initialPhotos = draftPhotos,
            suggestedTags = DEFAULT_UNIT_TAGS,
            defaultDescription = "Logement",
            isLoading = false,
            errorMessage = null,
            onDismiss = {
                currentStep = CreateUnitStep.FORM
            },
            onSave = { updatedPhotos ->
                draftPhotos = updatedPhotos
                currentStep = CreateUnitStep.FORM
            },
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ajouter un nouveau logement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Fermer")
            }
        }

        AppTextField(
            value = name,
            onValueChange = {
                name = it
                validationError = null
            },
            label = "Nom / Numéro d'unité *",
            isError = (name.isBlank() && validationError != null),
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier.fillMaxWidth()
        )

        AppTextField(
            value = floor,
            onValueChange = {
                floor = it
                validationError = null
            },
            label = "Étage / Bloc *",
            isError = (floor.isBlank() && validationError != null),
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier.fillMaxWidth()
        )

        AppTextField(
            value = type,
            onValueChange = {
                type = it
                validationError = null
            },
            label = "Typologie de logement *",
            placeholder = "Studio...",
            isError = (type.isBlank() && validationError != null),
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier.fillMaxWidth()
        )

        AmountTextField(
            value = nominalRent,
            onValueChange = {
                nominalRent = it
                validationError = null
            },
            label = "Loyer Mensuel Principal *",
            currencySymbol = currencySymbol,
            isError = (nominalRent.toDoubleOrNull() == null && validationError != null),
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier.fillMaxWidth()
        )

        AmountTextField(
            value = serviceCharges,
            onValueChange = { serviceCharges = it },
            label = "Charges locatives mensuelles",
            currencySymbol = currencySymbol,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier.fillMaxWidth()
        )

        AppTextField(
            value = initialElectricityIndex,
            onValueChange = { initialElectricityIndex = it },
            label = "Index Compteur Initial (kWh)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Photos du logement (optionnelle) :", style = MaterialTheme.typography.bodyMedium)

        if (draftPhotos.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
            ) {
                itemsIndexed(draftPhotos, key = { _, item -> item.id }) { index, photoItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable { currentStep = CreateUnitStep.GALLERY },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
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
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            // Star badge for primary photo
                            if (index == 0) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Photo principale",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "Principale",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            if (photoItem.description.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = photoItem.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (draftPhotos.isEmpty()) 80.dp else 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .clickable { currentStep = CreateUnitStep.GALLERY },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (draftPhotos.isEmpty()) "+ Ajouter des photos via la galerie" else "Gérer / Ajouter des photos (${draftPhotos.size})",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        val err = validationError ?: errorMessage
        if (err != null) {
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Annuler")
            }
            Button(
                onClick = {
                    val rentVal = nominalRent.toDoubleOrNull()
                    val chargesVal = serviceCharges.toDoubleOrNull() ?: 0.0
                    val indexVal = initialElectricityIndex.toDoubleOrNull() ?: 0.0

                    if (name.isBlank() || floor.isBlank() || rentVal == null || rentVal <= 0.0) {
                        validationError = "Veuillez renseigner un nom, un étage et un loyer valide."
                        return@Button
                    }

                    onSubmit(name, floor, type.ifBlank { "Standard" }, rentVal, chargesVal, indexVal, selectedEquipments.toList(), draftPhotos)
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(if (isLoading) "Création..." else "Créer le logement")
            }
        }
    }
}
}

@PreviewLightDark
@Composable
fun CreateUnitContentPreview() {
    ResidTheme {
        CreateUnitContent(
            onDismiss = {},
            onSubmit = { _, _, _, _, _, _, _, _ -> }
        )
    }
}