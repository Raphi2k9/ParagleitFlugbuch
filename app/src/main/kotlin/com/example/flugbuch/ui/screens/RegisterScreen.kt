package com.example.flugbuch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flugbuch.auth.AuthState
import com.example.flugbuch.data.model.UserRole
import com.example.flugbuch.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var passwordVisible by remember { mutableStateOf(false) }
    var roleExpanded by remember { mutableStateOf(false) }

    val passwordMismatch = password.isNotBlank() && passwordConfirm.isNotBlank() && password != passwordConfirm
    val errorMessage = if (authState is AuthState.Error) (authState as AuthState.Error).message else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrieren") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Name
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; viewModel.clearError() },
                label = { Text("Vollständiger Name") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // E-Mail
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.clearError() },
                label = { Text("E-Mail") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Lizenznummer
            OutlinedTextField(
                value = licenseNumber,
                onValueChange = { licenseNumber = it },
                label = { Text("Lizenznummer (optional)") },
                leadingIcon = { Icon(Icons.Default.Badge, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Rolle
            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = it }
            ) {
                OutlinedTextField(
                    value = when (selectedRole) {
                        UserRole.STUDENT -> "Schüler / Pilot"
                        UserRole.INSTRUCTOR -> "Fluglehrer"
                        UserRole.SCHOOL_ADMIN -> "Flugschule (Admin)"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rolle") },
                    leadingIcon = { Icon(Icons.Default.School, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = roleExpanded,
                    onDismissRequest = { roleExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Schüler / Pilot") },
                        onClick = { selectedRole = UserRole.STUDENT; roleExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Fluglehrer") },
                        onClick = { selectedRole = UserRole.INSTRUCTOR; roleExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Flugschule (Admin)") },
                        onClick = { selectedRole = UserRole.SCHOOL_ADMIN; roleExpanded = false }
                    )
                }
            }

            // Passwort
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; viewModel.clearError() },
                label = { Text("Passwort") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Passwort bestätigen
            OutlinedTextField(
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it },
                label = { Text("Passwort bestätigen") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = passwordMismatch,
                supportingText = if (passwordMismatch) {{ Text("Passwörter stimmen nicht überein") }} else null,
                modifier = Modifier.fillMaxWidth()
            )

            // Fehlermeldung von Server
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Registrieren-Button
            Button(
                onClick = {
                    viewModel.register(
                        email = email.trim(),
                        password = password,
                        fullName = fullName.trim(),
                        licenseNumber = licenseNumber.trim(),
                        role = selectedRole.name
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading && !passwordMismatch
                        && email.isNotBlank() && password.isNotBlank()
                        && fullName.isNotBlank() && passwordConfirm.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Konto erstellen")
                }
            }

            // Hinweis für Schul-Admins
            if (selectedRole == UserRole.SCHOOL_ADMIN) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "Als Flugschule kannst du nach der Registrierung " +
                            "deine Schule anlegen und einen Invite-Code für Schüler generieren.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
