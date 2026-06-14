package com.example.flugbuch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flugbuch.R
import com.example.flugbuch.auth.AuthState
import com.example.flugbuch.data.model.UserRole
import com.example.flugbuch.ui.theme.ThemePreference
import com.example.flugbuch.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    pilotName: String,
    onPilotNameChange: (String) -> Unit,
    licenseNumber: String,
    onLicenseNumberChange: (String) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onNavigateToSchoolDashboard: (String) -> Unit,
    onNavigateToJoinSchool: () -> Unit,
    onNavigateToCreateSchool: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val currentProfile = (authState as? AuthState.LoggedIn)?.profile

    var nameInput by remember(pilotName) { mutableStateOf(pilotName) }
    var licenseInput by remember(licenseNumber) { mutableStateOf(licenseNumber) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Abmelden") },
            text = { Text("Wirklich abmelden? Lokale Flugdaten werden gelöscht und beim nächsten Login neu synchronisiert.") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text("Abmelden", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Konto-Info ---
            if (currentProfile != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountCircle, null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentProfile.fullName.ifBlank { "Pilot" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                when (currentProfile.userRole) {
                                    UserRole.STUDENT -> "Schüler / Pilot"
                                    UserRole.INSTRUCTOR -> "Fluglehrer"
                                    UserRole.SCHOOL_ADMIN -> "Flugschule (Admin)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, "Abmelden",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // --- Flugschule ---
            if (currentProfile != null) {
                Text("Flugschule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)

                when {
                    currentProfile.isSchoolStaff && currentProfile.schoolId != null -> {
                        // Instruktor/Admin mit Schule → Dashboard anzeigen
                        FilledTonalButton(
                            onClick = { onNavigateToSchoolDashboard(currentProfile.schoolId!!) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Dashboard, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Schul-Dashboard öffnen")
                        }
                    }
                    currentProfile.userRole == UserRole.SCHOOL_ADMIN && currentProfile.schoolId == null -> {
                        // Admin ohne Schule → Schule erstellen
                        FilledTonalButton(
                            onClick = onNavigateToCreateSchool,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Flugschule erstellen")
                        }
                    }
                    currentProfile.schoolId == null -> {
                        // Schüler ODER Fluglehrer ohne Schule → beitreten
                        OutlinedButton(
                            onClick = onNavigateToJoinSchool,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.School, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Flugschule beitreten")
                        }
                    }
                    else -> {
                        // Schüler mit zugeordneter Schule
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.School, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Text("Einer Flugschule zugeordnet",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                HorizontalDivider()
            }

            // --- Pilotendaten ---
            Text(
                stringResource(R.string.settings_pilot_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text(stringResource(R.string.settings_pilot_name)) },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(stringResource(R.string.settings_pilot_name_hint)) }
            )

            OutlinedTextField(
                value = licenseInput,
                onValueChange = { licenseInput = it },
                label = { Text(stringResource(R.string.settings_license)) },
                leadingIcon = { Icon(Icons.Default.Badge, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(stringResource(R.string.settings_license_hint)) }
            )

            Button(
                onClick = {
                    onPilotNameChange(nameInput)
                    onLicenseNumberChange(licenseInput)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_save))
            }

            HorizontalDivider()

            // --- Design ---
            Text(
                stringResource(R.string.settings_design_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ThemeOption(
                icon = Icons.Default.BrightnessAuto,
                label = stringResource(R.string.settings_theme_system),
                description = stringResource(R.string.settings_theme_system_desc),
                selected = themePreference == ThemePreference.SYSTEM,
                onClick = { onThemeChange(ThemePreference.SYSTEM) }
            )

            ThemeOption(
                icon = Icons.Default.LightMode,
                label = stringResource(R.string.settings_theme_light),
                description = stringResource(R.string.settings_theme_light_desc),
                selected = themePreference == ThemePreference.LIGHT,
                onClick = { onThemeChange(ThemePreference.LIGHT) }
            )

            ThemeOption(
                icon = Icons.Default.DarkMode,
                label = stringResource(R.string.settings_theme_dark),
                description = stringResource(R.string.settings_theme_dark_desc),
                selected = themePreference == ThemePreference.DARK,
                onClick = { onThemeChange(ThemePreference.DARK) }
            )

            HorizontalDivider()

            // --- Sprache ---
            Text(
                stringResource(R.string.settings_language_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ThemeOption(
                icon = Icons.Default.Language,
                label = stringResource(R.string.settings_language_german),
                description = stringResource(R.string.settings_language_german_desc),
                selected = language == "de",
                onClick = { onLanguageChange("de") }
            )

            ThemeOption(
                icon = Icons.Default.Language,
                label = stringResource(R.string.settings_language_english),
                description = stringResource(R.string.settings_language_english_desc),
                selected = language == "en",
                onClick = { onLanguageChange("en") }
            )
        }
    }
}

@Composable
private fun ThemeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, tint = contentColor)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = contentColor, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.7f))
            }
            if (selected) Icon(Icons.Default.Check, null, tint = contentColor)
        }
    }
}
