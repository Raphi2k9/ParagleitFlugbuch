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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flugbuch.R
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
                        if (isEditMode) stringResource(R.string.edit_flight_title) else stringResource(R.string.add_flight_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = ::validateAndSave,
                        enabled = canSave
                    ) {
                        Text(
                            stringResource(R.string.action_save),
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
            SectionHeader(stringResource(R.string.flight_required_fields))

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
                            stringResource(R.string.flight_date),
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
                                stringResource(R.string.flight_date_future_error),
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
                    label = { Text(stringResource(R.string.flight_glider)) },
                    placeholder = { Text(stringResource(R.string.flight_glider_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    isError = gliderError,
                    supportingText = if (gliderError) ({ Text(stringResource(R.string.required_field)) }) else null,
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
                    label = { Text(stringResource(R.string.flight_hours)) },
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
                    label = { Text(stringResource(R.string.flight_minutes)) },
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
                    value = stringResource(flightType.labelRes),
                    onValueChange = {},
                    label = { Text(stringResource(R.string.flight_type_field)) },
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
                                    Text(stringResource(type.labelRes), fontWeight = FontWeight.Medium)
                                    Text(
                                        stringResource(type.descRes),
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
                SectionHeader(stringResource(R.string.flight_training_exercises))
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
                                    text = stringResource(exercise.labelRes),
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
                SectionHeader(stringResource(R.string.flight_exam_result))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = pruefungBestanden == true,
                        onClick = { pruefungBestanden = if (pruefungBestanden == true) null else true },
                        label = { Text(stringResource(R.string.label_passed)) },
                        leadingIcon = if (pruefungBestanden == true) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = pruefungBestanden == false,
                        onClick = { pruefungBestanden = if (pruefungBestanden == false) null else false },
                        label = { Text(stringResource(R.string.label_failed)) },
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
            SectionHeader(stringResource(R.string.flight_optional_fields))

            // ── Start- & Landeplatz ──────────────────────────────────────────
            OutlinedTextField(
                value = startLocation,
                onValueChange = { startLocation = it },
                label = { Text(stringResource(R.string.flight_start_location)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.FlightTakeoff, null) },
                singleLine = true
            )
            OutlinedTextField(
                value = landingLocation,
                onValueChange = { landingLocation = it },
                label = { Text(stringResource(R.string.flight_landing_location)) },
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
                    label = { Text(stringResource(R.string.flight_max_altitude)) },
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
                    label = { Text(stringResource(R.string.flight_distance)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = distanceError,
                    supportingText = if (distanceError) ({
                        Text(stringResource(R.string.numbers_only))
                    }) else null,
                    trailingIcon = { Text("km", modifier = Modifier.padding(end = 4.dp)) },
                    leadingIcon = { Icon(Icons.Default.Route, null) },
                    singleLine = true
                )
            }

            HorizontalDivider()
            SectionHeader(stringResource(R.string.flight_weather))

            OutlinedTextField(
                value = windConditions,
                onValueChange = { windConditions = it },
                label = { Text(stringResource(R.string.flight_wind)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Air, null) },
                placeholder = { Text(stringResource(R.string.flight_wind_hint)) },
                singleLine = true
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = cloudCover,
                    onValueChange = { cloudCover = it },
                    label = { Text(stringResource(R.string.flight_cloud_cover)) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Cloud, null) },
                    placeholder = { Text(stringResource(R.string.flight_cloud_cover_hint)) },
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
                    label = { Text(stringResource(R.string.flight_temperature)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = temperatureError,
                    supportingText = if (temperatureError) ({
                        Text(stringResource(R.string.numbers_only))
                    }) else null,
                    trailingIcon = { Text("°C", modifier = Modifier.padding(end = 4.dp)) },
                    leadingIcon = { Icon(Icons.Default.Thermostat, null) },
                    singleLine = true
                )
            }

            HorizontalDivider()
            SectionHeader(stringResource(R.string.flight_notes_section))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.flight_notes)) },
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
                Text(if (isEditMode) stringResource(R.string.flight_save_edit) else stringResource(R.string.flight_save_new))
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
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
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
