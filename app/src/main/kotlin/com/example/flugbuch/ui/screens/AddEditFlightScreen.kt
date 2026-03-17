package com.example.flugbuch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flugbuch.data.entities.FlightEntity
import com.example.flugbuch.data.entities.FlightType
import com.example.flugbuch.viewmodel.FlightViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFlightScreen(
    viewModel: FlightViewModel,
    flightId: Int?,
    onNavigateBack: () -> Unit
) {
    val isEditMode = flightId != null
    val gliders by viewModel.allGliders.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Formular-State
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var gliderName by remember { mutableStateOf("") }
    var durationHours by remember { mutableStateOf("0") }
    var durationMinutes by remember { mutableStateOf("0") }
    var flightType by remember { mutableStateOf(FlightType.THERMAL) }
    var startLocation by remember { mutableStateOf("") }
    var landingLocation by remember { mutableStateOf("") }
    var maxAltitude by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var windConditions by remember { mutableStateOf("") }
    var cloudCover by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Vorhandenen Flug laden
    LaunchedEffect(flightId) {
        if (flightId != null) {
            viewModel.getFlightById(flightId)?.let { flight ->
                selectedDateMillis = flight.date
                gliderName = flight.gliderName
                val totalMin = flight.durationMinutes
                durationHours = (totalMin / 60).toString()
                durationMinutes = (totalMin % 60).toString()
                flightType = FlightType.entries.find { it.name == flight.flightType } ?: FlightType.THERMAL
                startLocation = flight.startLocation
                landingLocation = flight.landingLocation
                maxAltitude = flight.maxAltitude?.toString() ?: ""
                distance = flight.distance?.toString() ?: ""
                windConditions = flight.windConditions
                cloudCover = flight.cloudCover
                temperature = flight.temperature?.toString() ?: ""
                notes = flight.notes
            }
        }
    }

    // Validierung
    var gliderError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showGliderDropdown by remember { mutableStateOf(false) }
    var showFlightTypeDropdown by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY) }
    val formattedDate = remember(selectedDateMillis) {
        dateFormat.format(Date(selectedDateMillis))
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    fun validateAndSave() {
        gliderError = gliderName.isBlank()
        if (gliderError) return

        val totalMinutes = (durationHours.toIntOrNull() ?: 0) * 60 +
                (durationMinutes.toIntOrNull() ?: 0)

        val flight = FlightEntity(
            id = if (isEditMode) flightId!! else 0,
            date = selectedDateMillis,
            gliderName = gliderName.trim(),
            durationMinutes = totalMinutes,
            flightType = flightType.name,
            startLocation = startLocation.trim(),
            landingLocation = landingLocation.trim(),
            maxAltitude = maxAltitude.toIntOrNull(),
            distance = distance.toDoubleOrNull(),
            windConditions = windConditions.trim(),
            cloudCover = cloudCover.trim(),
            temperature = temperature.toIntOrNull(),
            notes = notes.trim()
        )

        scope.launch {
            if (isEditMode) viewModel.updateFlight(flight)
            else viewModel.insertFlight(flight)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Flug bearbeiten" else "Neuer Flug",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    TextButton(onClick = ::validateAndSave) {
                        Text("Speichern", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- PFLICHTFELDER ---
            SectionHeader("Pflichtfelder")

            // Datum
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Datum",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            formattedDate,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Schirm mit Dropdown
            ExposedDropdownMenuBox(
                expanded = showGliderDropdown,
                onExpandedChange = { showGliderDropdown = it }
            ) {
                OutlinedTextField(
                    value = gliderName,
                    onValueChange = { gliderName = it; gliderError = false },
                    label = { Text("Gleitschirm / Flügel *") },
                    placeholder = { Text("z.B. Advance Alpha 7") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    isError = gliderError,
                    supportingText = if (gliderError) ({ Text("Pflichtfeld") }) else null,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGliderDropdown)
                    },
                    leadingIcon = { Icon(Icons.Default.Paragliding, null) },
                    singleLine = true
                )
                if (gliders.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = showGliderDropdown,
                        onDismissRequest = { showGliderDropdown = false }
                    ) {
                        gliders.forEach { glider ->
                            DropdownMenuItem(
                                text = { Text(glider.name) },
                                onClick = {
                                    gliderName = glider.name
                                    showGliderDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Flugdauer
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = durationHours,
                    onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) durationHours = it },
                    label = { Text("Stunden") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = { Text("h", modifier = Modifier.padding(end = 8.dp)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = {
                        val v = it.toIntOrNull()
                        if (it.isEmpty() || (v != null && v in 0..59)) durationMinutes = it
                    },
                    label = { Text("Minuten") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = { Text("min", modifier = Modifier.padding(end = 8.dp)) },
                    singleLine = true
                )
            }

            // Flugart
            ExposedDropdownMenuBox(
                expanded = showFlightTypeDropdown,
                onExpandedChange = { showFlightTypeDropdown = it }
            ) {
                OutlinedTextField(
                    value = flightType.displayName,
                    onValueChange = {},
                    label = { Text("Flugart *") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFlightTypeDropdown)
                    },
                    leadingIcon = { Icon(Icons.Default.FlightTakeoff, null) }
                )
                ExposedDropdownMenu(
                    expanded = showFlightTypeDropdown,
                    onDismissRequest = { showFlightTypeDropdown = false }
                ) {
                    FlightType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(type.displayName, fontWeight = FontWeight.Medium)
                                    Text(
                                        type.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            onClick = {
                                flightType = type
                                showFlightTypeDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            // --- OPTIONALE FELDER ---
            SectionHeader("Optionale Angaben")

            // Start- und Landeplatz
            OutlinedTextField(
                value = startLocation,
                onValueChange = { startLocation = it },
                label = { Text("Startplatz") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.FlightTakeoff, null) },
                singleLine = true
            )
            OutlinedTextField(
                value = landingLocation,
                onValueChange = { landingLocation = it },
                label = { Text("Landeplatz") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.FlightLand, null) },
                singleLine = true
            )

            // Höhe und Strecke
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = maxAltitude,
                    onValueChange = { if (it.all { c -> c.isDigit() }) maxAltitude = it },
                    label = { Text("Max. Höhe") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = { Text("m", modifier = Modifier.padding(end = 8.dp)) },
                    leadingIcon = { Icon(Icons.Default.Height, null) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = distance,
                    onValueChange = { distance = it },
                    label = { Text("Strecke") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = { Text("km", modifier = Modifier.padding(end = 4.dp)) },
                    leadingIcon = { Icon(Icons.Default.Route, null) },
                    singleLine = true
                )
            }

            // Wetterbedingungen
            SectionHeader("Wetterbedingungen")
            OutlinedTextField(
                value = windConditions,
                onValueChange = { windConditions = it },
                label = { Text("Wind") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Air, null) },
                placeholder = { Text("z.B. NW 15 km/h, böig") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cloudCover,
                    onValueChange = { cloudCover = it },
                    label = { Text("Bewölkung") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Cloud, null) },
                    placeholder = { Text("z.B. 3/8") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = { Text("Temperatur") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = { Text("°C", modifier = Modifier.padding(end = 4.dp)) },
                    leadingIcon = { Icon(Icons.Default.Thermostat, null) },
                    singleLine = true
                )
            }

            // Notizen
            SectionHeader("Notizen")
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Persönliche Bemerkungen") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 8,
                leadingIcon = { Icon(Icons.Default.Notes, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Speichern-Button
            Button(
                onClick = ::validateAndSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditMode) "Änderungen speichern" else "Flug speichern")
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}
