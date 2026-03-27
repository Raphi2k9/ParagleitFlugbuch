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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onNavigateBack: () -> Unit
) {
    var nameInput by remember(pilotName) { mutableStateOf(pilotName) }
    var licenseInput by remember(licenseNumber) { mutableStateOf(licenseNumber) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
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
                "Pilotendaten",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Wird in Exporten und Berichten verwendet") }
            )

            OutlinedTextField(
                value = licenseInput,
                onValueChange = { licenseInput = it },
                label = { Text("Lizenznummer") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Offizielle Pilotenlizenz-Nummer") }
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
                Text("Speichern")
            }

            HorizontalDivider()

            // --- Design ---
            Text(
                "Design",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ThemeOption(
                icon = Icons.Default.BrightnessAuto,
                label = "System-Theme",
                description = "Folgt der Geräteeinstellung",
                selected = themePreference == ThemePreference.SYSTEM,
                onClick = { onThemeChange(ThemePreference.SYSTEM) }
            )

            ThemeOption(
                icon = Icons.Default.LightMode,
                label = "Hell",
                description = "Heller Hintergrund",
                selected = themePreference == ThemePreference.LIGHT,
                onClick = { onThemeChange(ThemePreference.LIGHT) }
            )

            ThemeOption(
                icon = Icons.Default.DarkMode,
                label = "Dunkel",
                description = "Dunkler Hintergrund",
                selected = themePreference == ThemePreference.DARK,
                onClick = { onThemeChange(ThemePreference.DARK) }
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
