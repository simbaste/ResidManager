package com.resid.manager.features.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserDto
import com.resid.manager.dto.UserRole
import com.resid.manager.features.profile.ProfileViewModel
import com.resid.manager.features.profile.mvi.ProfileEffect
import com.resid.manager.features.profile.mvi.ProfileIntent
import com.resid.manager.features.profile.ui.components.EditCategoryDialog
import com.resid.manager.features.profile.ui.components.ResidenceConfigTab
import com.resid.manager.features.profile.ui.components.UserAccountTab
import com.resid.manager.ui.components.ResidTopAppBar
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    user: UserDto?,
    firstName: String,
    lastName: String,
    phone: String,
    activeResidence: ResidenceContext?,
    jwtToken: String?,
    onLogout: () -> Unit,
    onUserUpdated: (UserDto) -> Unit = {},
    viewModel: ProfileViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val token = jwtToken ?: ""
    val residenceId = activeResidence?.residenceId
    val isManagement = activeResidence != null && (
        activeResidence.userRoleInResidence == UserRole.OWNER ||
        activeResidence.userRoleInResidence == UserRole.ADMIN ||
        activeResidence.userRoleInResidence == UserRole.MANAGER
    )

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.UserProfileUpdated -> onUserUpdated(effect.user)
                else -> Unit
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ResidTopAppBar(
            title = "Mon Espace Personnel"
        )

        TabRow(
            selectedTabIndex = uiState.activeTab,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Tab(
                selected = uiState.activeTab == 0,
                onClick = { viewModel.onIntent(ProfileIntent.SetTab(0, token, residenceId)) },
                text = { Text("Mon Compte", style = MaterialTheme.typography.titleMedium) }
            )
            if (isManagement) {
                Tab(
                    selected = uiState.activeTab == 1,
                    onClick = { viewModel.onIntent(ProfileIntent.SetTab(1, token, residenceId)) },
                    text = { Text("Configuration Résidence", style = MaterialTheme.typography.titleMedium) }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        if (uiState.activeTab == 0) {
            UserAccountTab(
                user = user,
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                isEditingProfile = uiState.isEditingProfile,
                editFirstName = uiState.editFirstName,
                editLastName = uiState.editLastName,
                editPhone = uiState.editPhone,
                isSavingProfile = uiState.isSavingProfile,
                profileError = uiState.profileError,
                onStartEditing = { viewModel.onIntent(ProfileIntent.StartEditingProfile(firstName, lastName, phone)) },
                onCancelEditing = { viewModel.onIntent(ProfileIntent.CancelEditingProfile) },
                onFirstNameChanged = { viewModel.onIntent(ProfileIntent.SetEditFirstName(it)) },
                onLastNameChanged = { viewModel.onIntent(ProfileIntent.SetEditLastName(it)) },
                onPhoneChanged = { viewModel.onIntent(ProfileIntent.SetEditPhone(it)) },
                onSaveProfile = { viewModel.onIntent(ProfileIntent.SaveProfile(token)) },
                onLogout = onLogout,
                modifier = Modifier.weight(1f)
            )
        } else if (isManagement && residenceId != null) {
            ResidenceConfigTab(
                categories = uiState.categories,
                isLoadingCategories = uiState.isLoadingCategories,
                formCategoryKey = uiState.formCategoryKey,
                formCategoryLabel = uiState.formCategoryLabel,
                isSubmittingCategory = uiState.isSubmittingCategory,
                categoryError = uiState.categoryError,
                selectedCurrency = uiState.selectedCurrency,
                isSubmittingCurrency = uiState.isSubmittingCurrency,
                currencySuccess = uiState.currencySuccess,
                currencyErrorMsg = uiState.currencyErrorMsg,
                onCategoryKeyChanged = { viewModel.onIntent(ProfileIntent.SetCategoryKey(it)) },
                onCategoryLabelChanged = { viewModel.onIntent(ProfileIntent.SetCategoryLabel(it)) },
                onSubmitCategory = { viewModel.onIntent(ProfileIntent.SubmitCategory(token, residenceId)) },
                onSelectCurrency = { viewModel.onIntent(ProfileIntent.SelectCurrency(it)) },
                onSubmitCurrency = { viewModel.onIntent(ProfileIntent.SubmitCurrency(token, residenceId)) },
                onEditCategoryClick = { cat -> viewModel.onIntent(ProfileIntent.SetEditingCategory(cat)) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (uiState.editingCategory != null) {
        EditCategoryDialog(
            categoryLabel = uiState.editCategoryLabel,
            isSubmitting = uiState.isSubmittingEdit,
            errorMessage = uiState.editError,
            onLabelChanged = { viewModel.onIntent(ProfileIntent.SetEditCategoryLabel(it)) },
            onDismiss = { viewModel.onIntent(ProfileIntent.SetEditingCategory(null)) },
            onSubmit = { viewModel.onIntent(ProfileIntent.SaveCategoryEdit(token)) }
        )
    }
}
