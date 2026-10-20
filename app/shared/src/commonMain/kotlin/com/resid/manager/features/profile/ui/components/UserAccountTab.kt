package com.resid.manager.features.profile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resid.manager.dto.UserDto
import com.resid.manager.features.auth.ui.components.register.RegisterProfilePhotoSection
import com.resid.manager.ui.components.AppTextField
import com.resid.manager.ui.components.PhoneTextField

@Composable
fun UserAccountTab(
    user: UserDto?,
    firstName: String,
    lastName: String,
    phone: String,
    isEditingProfile: Boolean,
    editFirstName: String,
    editLastName: String,
    editPhone: String,
    editAvatarUrl: String?,
    editAvatarBytes: ByteArray?,
    isAvatarRemoved: Boolean,
    isSavingProfile: Boolean,
    profileError: String?,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onFirstNameChanged: (String) -> Unit,
    onLastNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onAvatarSelected: (ByteArray, String, String) -> Unit,
    onRemoveAvatar: () -> Unit,
    onSaveProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopStart
    ) {
        Card(
            modifier = Modifier.widthIn(max = 600.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fiche d'Information Utilisateur",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF006948)
                    )

                    if (!isEditingProfile) {
                        IconButton(onClick = onStartEditing) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color(0xFF006948))
                        }
                    }
                }

                HorizontalDivider()

                if (isEditingProfile) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Profile photo selector / editor / remover
                        RegisterProfilePhotoSection(
                            avatarBytes = editAvatarBytes,
                            avatarUrl = if (isAvatarRemoved) null else editAvatarUrl,
                            onAvatarSelected = onAvatarSelected,
                            onRemoveAvatar = onRemoveAvatar
                        )

                        AppTextField(
                            value = editFirstName,
                            onValueChange = onFirstNameChanged,
                            label = "Prénom *",
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AppTextField(
                            value = editLastName,
                            onValueChange = onLastNameChanged,
                            label = "Nom *",
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                            modifier = Modifier.fillMaxWidth()
                        )
                        PhoneTextField(
                            value = editPhone,
                            onPhoneChanged = onPhoneChanged,
                            label = "Téléphone",
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                            modifier = Modifier.fillMaxWidth()
                        )

                        profileError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onSaveProfile,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                                modifier = Modifier.weight(1f).height(44.dp),
                                enabled = !isSavingProfile,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSavingProfile) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("Enregistrer")
                                }
                            }

                            OutlinedButton(
                                onClick = onCancelEditing,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Annuler")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Avatar display (with initials fallback)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF006948).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val avatarUrl = user?.avatarUrl
                                if (!avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "Photo de profil",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    val initials = if (firstName.isNotBlank()) {
                                        "${firstName.take(1)}${lastName.take(1)}".uppercase()
                                    } else "AD"
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color(0xFF006948)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "$firstName $lastName",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = user?.email ?: "Non spécifié",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Prénom : $firstName", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Nom : $lastName", style = MaterialTheme.typography.titleMedium)
                            Text(text = "E-mail : ${user?.email ?: "Non spécifié"}", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Téléphone : ${phone.ifBlank { "Non spécifié" }}", style = MaterialTheme.typography.titleMedium)
                            Text(text = "ID Utilisateur : ${user?.id ?: "Non spécifié"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }

                        HorizontalDivider()

                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Déconnexion")
                        }
                    }
                }
            }
        }
    }
}
