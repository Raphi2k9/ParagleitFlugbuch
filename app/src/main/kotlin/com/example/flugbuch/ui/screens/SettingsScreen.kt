package com.example.flugbuch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flugbuch.R
import com.example.flugbuch.ui.theme.ThemePreference

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
    onNavigateBack: () -> Unit
) {
    var nameInput by remember(pilotName) { mutableStateOf(pilotName) }
    var licenseInput by remember(licenseNumber) { mutableStateOf(licenseNumber) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
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
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(stringResource(R.string.settings_pilot_name_hint)) }
            )

            OutlinedTextField(
                value = licenseInput,
                onValueChange = { licenseInput = it },
                label = { Text(stringResource(R.string.settings_license)) },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
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
                Icon(Icons.Default.Save, contentDescription = null)
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

            // --- Sprache / Language ---
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
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (selected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = contentColor, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.7f))
            }
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = contentColor)
            }
        }
    }
}
