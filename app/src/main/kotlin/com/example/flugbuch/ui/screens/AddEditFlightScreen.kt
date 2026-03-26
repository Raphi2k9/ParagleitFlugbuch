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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flugbuch.data.entities.FlightEntity
import com.example.flugbuch.data.entities.FlightType
import com.example.flugbuch.data.entities.TrainingExercise
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

    // ── Formular-State ──────────────────────────────────────────────────────
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var gliderName by remember { mutableStateOf("") }
    var durationHours by remember { mutableStateOf("0") }
    var durationMinutes by remember { mutableStateOf("0") }
    var flightType by remember { mutableStateOf(FlightType.THERMAL) }
    var trainingExercises by remember { mutableStateOf(emptySet<TrainingExercise>()) }
    var pruefungBestanden by remember { mutableStateOf<Boolean?>(null) }
    var startLocation by remember { mutableStateOf("") }
    var landingLocation by remember { mutableStateOf("") }
    var maxAltitude by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var windConditions by remember { mutableStateOf("") }
    var cloudCover by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Beim Bearbeiten vorhandene Daten laden
    LaunchedEffect(flightId) {
        if (flightId != null) {
            viewModel.getFlightById(flightId)?.let { flight ->
                selectedDateMillis = flight.date
                gliderName = flight.gliderName
                durationHours = (flight.durationMinutes / 60).toString()
                durationMinutes = (flight.durationMinutes % 60).toString()
                flightType = FlightType.entries.find { it.name == flight.flightType } ?: FlightType.THERMAL
                trainingExercises = TrainingExercise.fromStorageString(flight.trainingExercises)
                pruefungBestanden = flight.pruefungBestanden
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

    // ── Validierungs-States ─────────────────────────────────────────────────

    // Heute 23:59:59 in lokaler Zeit – Grenze für gültige Daten
    val todayEndMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
    // Heute Mitternacht UTC – für den DatePicker (arbeitet intern mit UTC)
    val todayUtcMidnight = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val dateError by remember(selectedDateMillis) {
        derivedStateOf { selectedDateMillis > todayEndMillis }
    }
    // Strecke: ungültig wenn nicht leer, nicht nur Punkt und kein gültiger Double
    val distanceError by remember(distance) {
        derivedStateOf {
            distance.isNotBlank() && distance != "." && distance.toDoubleOrNull() == null
        }
    }
    // Temperatur: ungültig wenn nicht leer, nicht nur Minus und kein gültiger Int
    val temperatureError by remember(temperature) {
        derivedStateOf {
            temperature.isNotBlank() && temperature != "-" && temperature.toIntOrNull() == null
        }
    }
    // Schirm: Pflichtfeld – Fehler wird erst nach erstem Speicherversuch gezeigt
    var gliderTouched by remember { mutableStateOf(false) }
    val gliderError = gliderTouched && gliderName.isBlank()

    // Speichern-Button: aktiv nur wenn alle Validierungen bestehen
    val canSave by remember(gliderName, dateError, distanceError, temperatureError) {
        derivedStateOf {
            gliderName.isNotBlank() && !dateError && !distanceError && !temperatureError
        }
    }

    // ── UI-State ────────────────────────────────────────────────────────────
    var showDatePicker by remember { mutableStateOf(false) }
    var showGliderDropdown by remember { mutableStateOf(false) }
    var showFlightTypeDropdown by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY) }
    val formattedDate = remember(selectedDateMillis) { dateFormat.format(Date(selectedDateMillis)) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis <= todayUtcMidnight

            override fun isSelectableYear(year: Int): Boolean =
                year <= Calendar.getInstance().get(Calendar.YEAR)
        }
    )

    fun validateAndSave() {
        gliderTouched = true
        if (!canSave) return

        val totalMinutes = (durationHours.toIntOrNull() ?: 0) * 60 +
                (durationMinutes.toIntOrNull() ?: 0)

        val flight = FlightEntity(
            id = if (isEditMode) flightId!! else 0,
            date = selectedDateMillis,
            gliderName = gliderName.trim(),
            durationMinutes = totalMinutes,
            flightType = flightType.name,
            trainingExercises = if (flightType == FlightType.SCHULUNGSFLUG)
                TrainingExercise.toStorageString(trainingExercises).ifBlank { null }
            else null,
            pruefungBestanden = if (flightType == FlightType.PRUEFUNGSFLUG) pruefungBestanden else null,
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

    // ── Scaffold ─────────────────────────────────────────────────────────────
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
                    TextButton(
                        onClick = ::validateAndSave,
                        enabled = canSave
                    ) {
                        Text(
                            "Speichern",
                            fontWeight = FontWeight.Bold,
                            color = if (canSave)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                        )
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Pflichtfelder")

            // ── Datum ────────────────────────────────────────────────────────
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                border = if (dateError)
                    CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                    )
                else
                    CardDefaults.outlinedCardBorder()
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
                            color = if (dateError) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            formattedDate,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (dateError) MaterialTheme.colorScheme.error
                                    else Color.Unspecified
                        )
                        if (dateError) {
                            Text(
                                "Das Datum darf nicht in der Zukunft liegen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (dateError) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Schirm mit Dropdown ──────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = showGliderDropdown,
                onExpandedChange = { showGliderDropdown = it }
            ) {
                OutlinedTextField(
                    value = gliderName,
                    onValueChange = {
                        gliderName = it
                        gliderTouched = true
                    },
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
                                    gliderTouched = true
                                    showGliderDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Flugdauer ────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = durationHours,
                    onValueChange = { input ->
                        if (input.length <= 3 && input.all { it.isDigit() }) durationHours = input
                    },
                    label = { Text("Stunden") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = { Text("h", modifier = Modifier.padding(end = 8.dp)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { input ->
                        val v = input.toIntOrNull()
                        if (input.isEmpty() || (v != null && v in 0..59)) durationMinutes = input
                    },
                    label = { Text("Minuten") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = { Text("min", modifier = Modifier.padding(end = 8.dp)) },
                    singleLine = true
                )
            }

            // ── Flugart ──────────────────────────────────────────────────────
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

            // ── Schulungsübungen (nur bei Flugart Schulungsflug) ─────────────
            if (flightType == FlightType.SCHULUNGSFLUG) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader("Schulungsübungen")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        TrainingExercise.entries.forEach { exercise ->
                            val checked = exercise in trainingExercises
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        trainingExercises = if (isChecked)
                                            trainingExercises + exercise
                                        else
                                            trainingExercises - exercise
                                    }
                                )
                                Text(
                                    text = exercise.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // ── Prüfungsergebnis (nur bei Flugart Prüfungsflug) ──────────────
            if (flightType == FlightType.PRUEFUNGSFLUG) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader("Prüfungsergebnis")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = pruefungBestanden == true,
                        onClick = { pruefungBestanden = if (pruefungBestanden == true) null else true },
                        label = { Text("Bestanden") },
                        leadingIcon = if (pruefungBestanden == true) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = pruefungBestanden == false,
                        onClick = { pruefungBestanden = if (pruefungBestanden == false) null else false },
                        label = { Text("Nicht bestanden") },
                        leadingIcon = if (pruefungBestanden == false) {
                            { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader("Optionale Angaben")

            // ── Start- & Landeplatz ──────────────────────────────────────────
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

            // ── Höhe & Strecke ────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Höhe: nur positive Ganzzahlen – ungültige Zeichen werden still gefiltert
                OutlinedTextField(
                    value = maxAltitude,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        maxAltitude = filtered
                    },
                    label = { Text("Max. Höhe") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = { Text("m", modifier = Modifier.padding(end = 8.dp)) },
                    leadingIcon = { Icon(Icons.Default.Height, null) },
                    singleLine = true
                )
                // Strecke: Dezimalzahl – Komma wird zu Punkt normalisiert
                OutlinedTextField(
                    value = distance,
                    onValueChange = { input ->
                        val filtered = buildString {
                            var hasDot = false
                            for (c in input) {
                                when {
                                    c.isDigit() -> append(c)
                                    (c == '.' || c == ',') && !hasDot -> {
                                        append('.')
                                        hasDot = true
                                    }
                                }
                            }
                        }
                        distance = filtered
                    },
                    label = { Text("Strecke") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = distanceError,
                    supportingText = if (distanceError) ({
                        Text("Bitte nur Zahlen eingeben")
                    }) else null,
                    trailingIcon = { Text("km", modifier = Modifier.padding(end = 4.dp)) },
                    leadingIcon = { Icon(Icons.Default.Route, null) },
                    singleLine = true
                )
            }

            HorizontalDivider()
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = cloudCover,
                    onValueChange = { cloudCover = it },
                    label = { Text("Bewölkung") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Cloud, null) },
                    placeholder = { Text("z.B. 3/8") },
                    singleLine = true
                )
                // Temperatur: ganze Zahl, auch negativ
                OutlinedTextField(
                    value = temperature,
                    onValueChange = { input ->
                        val filtered = buildString {
                            for ((i, c) in input.withIndex()) {
                                when {
                                    c.isDigit() -> append(c)
                                    c == '-' && i == 0 -> append(c)
                                }
                            }
                        }
                        temperature = filtered
                    },
                    label = { Text("Temperatur") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = temperatureError,
                    supportingText = if (temperatureError) ({
                        Text("Bitte nur Zahlen eingeben")
                    }) else null,
                    trailingIcon = { Text("°C", modifier = Modifier.padding(end = 4.dp)) },
                    leadingIcon = { Icon(Icons.Default.Thermostat, null) },
                    singleLine = true
                )
            }

            HorizontalDivider()
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

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = ::validateAndSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditMode) "Änderungen speichern" else "Flug speichern")
            }
        }
    }

    // ── DatePicker-Dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDateMillis = millis
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
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
