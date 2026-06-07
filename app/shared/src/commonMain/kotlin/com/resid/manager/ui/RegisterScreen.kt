package com.resid.manager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.resid.manager.viewmodel.LoginViewModel

@Composable
fun RegisterScreen(
    viewModel: LoginViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Créer un compte",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // First Name field
                OutlinedTextField(
                    value = uiState.firstName,
                    onValueChange = { viewModel.onFirstNameChanged(it) },
                    label = { Text("Prénom *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Last Name field
                OutlinedTextField(
                    value = uiState.lastName,
                    onValueChange = { viewModel.onLastNameChanged(it) },
                    label = { Text("Nom de famille *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date of Birth field
                OutlinedTextField(
                    value = uiState.birthDate,
                    onValueChange = { viewModel.onBirthDateChanged(it) },
                    label = { Text("Date de naissance (AAAA-MM-JJ)") },
                    placeholder = { Text("Ex: 1990-05-15") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Phone Number field
                OutlinedTextField(
                    value = uiState.phone,
                    onValueChange = { viewModel.onPhoneChanged(it) },
                    label = { Text("Téléphone") },
                    placeholder = { Text("Ex: +22501020304") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                // Email field
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    label = { Text("Adresse email *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                // Password field
                OutlinedTextField(
                    value = uiState.passwordPlain,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    label = { Text("Mot de passe *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { viewModel.togglePasswordVisibility() }) {
                            Text(if (uiState.passwordVisible) "Masquer" else "Afficher")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                val error = uiState.errorMessage
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Create Account Button
                Button(
                    onClick = { viewModel.register() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Créer mon compte")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Call to action link to go to login
                TextButton(onClick = { viewModel.navigateToLogin() }) {
                    Text("Déjà un compte ? Se connecter")
                }
            }
        }
    }
}
