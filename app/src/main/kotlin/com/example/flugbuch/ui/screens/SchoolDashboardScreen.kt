package com.example.flugbuch.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.flugbuch.data.entities.FlightType
import com.example.flugbuch.data.model.FlightModel
import com.example.flugbuch.data.model.StudentWithFlights
import com.example.flugbuch.data.model.UserRole
import com.example.flugbuch.export.FlightExcelExporter
import com.example.flugbuch.viewmodel.AuthViewModel
import com.example.flugbuch.viewmodel.SchoolViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolDashboardScreen(
    schoolViewModel: SchoolViewModel,
    authViewModel: AuthViewModel,
    schoolId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val studentsWithFlights by schoolViewModel.studentsWithFlights.collectAsState()
    val isLoading by schoolViewModel.isLoading.collectAsState()
    val error by schoolViewModel.error.collectAsState()
    val schoolName by schoolViewModel.schoolName.collectAsState()
    val inviteCode by schoolViewModel.inviteCode.collectAsState()

    // Rolle des eingeloggten Nutzers
    val authState by authViewModel.authState.collectAsState()
    val currentProfile = (authState as? com.example.flugbuch.auth.AuthState.LoggedIn)?.profile
    val currentRole = currentProfile?.userRole ?: UserRole.STUDENT
    val isAdmin = currentRole == UserRole.SCHOOL_ADMIN
    val isInstructor = currentRole == UserRole.INSTRUCTOR
    // Schulpersonal (Admin + Fluglehrer) darf den Invite-Code sehen/weitergeben
    val isStaff = isAdmin || isInstructor

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Flight-Bearbeiten-Dialog State (Admin)
    var editingFlight by remember { mutableStateOf<FlightModel?>(null) }
    // Flug-Detailansicht (alle Felder, schreibgeschützt)
    var detailFlight by remember { mutableStateOf<FlightModel?>(null) }

    // Alle Flüge eines Schülers als Excel-Datei exportieren und teilen
    fun exportStudent(student: StudentWithFlights) {
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    FlightExcelExporter.export(context, student.profile.fullName, student.flights)
                }.getOrNull()
            }
            if (file == null) return@launch
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Flugbuch exportieren"))
        }
    }

    LaunchedEffect(schoolId) {
        schoolViewModel.loadSchoolData(schoolId)
    }

    // Edit-Dialog für Admin
    editingFlight?.let { flight ->
        FlightEditDialog(
            flight = flight,
            onDismiss = { editingFlight = null },
            onSave = { updatedFlight ->
                schoolViewModel.updateStudentFlight(updatedFlight, schoolId)
                editingFlight = null
            }
        )
    }

    // Detail-Dialog (alle Felder eines Flugs ansehen)
    detailFlight?.let { flight ->
        FlightDetailDialog(flight = flight, onDismiss = { detailFlight = null })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Flugschul-Dashboard")
                        if (schoolName.isNotBlank()) {
                            Text(schoolName, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { schoolViewModel.loadSchoolData(schoolId) }) {
                        Icon(Icons.Default.Refresh, "Aktualisieren")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Einstellungen")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(error ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { schoolViewModel.loadSchoolData(schoolId) }) {
                            Text("Wiederholen")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Invite-Code Card – für Admin UND Fluglehrer, immer sichtbar
                        if (isStaff && inviteCode.isNotBlank()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Invite-Code zum Weitergeben",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                                            Text(inviteCode,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                                            Text("Schüler & Fluglehrer treten damit der Schule bei",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                        IconButton(onClick = {
                                            clipboard.setText(AnnotatedString(inviteCode))
                                        }) {
                                            Icon(Icons.Default.ContentCopy, "Kopieren",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }
                                }
                            }
                        }

                        // Statistik-Header
                        if (studentsWithFlights.isNotEmpty()) {
                            item {
                                SchoolStatsHeader(studentsWithFlights)
                            }
                        }

                        // Leer-Zustand
                        if (studentsWithFlights.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillParentMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.People, null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(16.dp))
                                    Text("Noch keine Schüler registriert",
                                        style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Teile deinen Invite-Code damit Schüler beitreten können.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Schüler-Karten
                        items(studentsWithFlights, key = { it.profile.id }) { studentData ->
                            StudentCard(
                                data = studentData,
                                isAdmin = isAdmin,
                                isInstructor = isInstructor,
                                onRemoveStudent = { userId ->
                                    schoolViewModel.removeMember(userId, schoolId)
                                },
                                onExportStudent = { exportStudent(studentData) },
                                onOpenDetail = { flight -> detailFlight = flight },
                                onEditFlight = { flight -> editingFlight = flight },
                                onSignFlight = { flightId, approved ->
                                    schoolViewModel.signStudentFlight(
                                        flightId = flightId,
                                        approved = approved,
                                        instructorId = currentProfile?.id ?: "",
                                        instructorName = currentProfile?.fullName ?: "",
                                        schoolId = schoolId
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchoolStatsHeader(data: List<StudentWithFlights>) {
    val totalFlights = data.sumOf { it.flights.size }
    val totalMinutes = data.sumOf { s -> s.flights.sumOf { it.durationMinutes } }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(icon = Icons.Default.People, value = "${data.size}", label = "Schüler")
            StatItem(icon = Icons.Default.FlightTakeoff, value = "$totalFlights", label = "Flüge gesamt")
            StatItem(icon = Icons.Default.Schedule,
                value = "${totalMinutes / 60}h ${totalMinutes % 60}m", label = "Flugzeit")
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun StudentCard(
    data: StudentWithFlights,
    isAdmin: Boolean,
    isInstructor: Boolean,
    onRemoveStudent: (String) -> Unit,
    onExportStudent: () -> Unit,
    onOpenDetail: (FlightModel) -> Unit,
    onEditFlight: (FlightModel) -> Unit,
    onSignFlight: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    val profile = data.profile
    val flights = data.flights
    val totalMinutes = flights.sumOf { it.durationMinutes }

    if (showRemoveDialog) {
        val name = profile.fullName.ifBlank { "dieses Mitglied" }
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            icon = { Icon(Icons.Default.PersonRemove, null) },
            title = { Text("Mitglied entfernen?") },
            text = {
                Text("$name wird aus der Flugschule entfernt und erscheint nicht mehr " +
                    "im Dashboard. Die Flüge des Mitglieds bleiben in dessen Flugbuch erhalten.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    onRemoveStudent(profile.id)
                }) {
                    Text("Entfernen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Schüler-Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Person, null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(profile.fullName.ifBlank { "Unbekannter Schüler" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        if (profile.licenseNumber.isNotBlank()) {
                            Text("Lizenz: ${profile.licenseNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Fluglehrer/Admin: Flüge dieses Schülers als Excel exportieren
                    if (flights.isNotEmpty()) {
                        IconButton(onClick = onExportStudent) {
                            Icon(Icons.Default.Download, "Als Excel exportieren",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // Admin: Mitglied aus der Schule entfernen
                    if (isAdmin) {
                        IconButton(onClick = { showRemoveDialog = true }) {
                            Icon(Icons.Default.PersonRemove, "Mitglied entfernen",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Weniger" else "${flights.size} Flüge")
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Kurzstatistik
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text("${flights.size} Flüge") },
                    leadingIcon = { Icon(Icons.Default.FlightTakeoff, null, Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${totalMinutes / 60}h ${totalMinutes % 60}m") },
                    leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(16.dp)) }
                )
            }

            // Flüge expandiert
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                if (flights.isEmpty()) {
                    Text("Keine Flüge erfasst",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    flights.take(20).forEach { flight ->
                        FlightRow(
                            flight = flight,
                            isAdmin = isAdmin,
                            isInstructor = isInstructor,
                            onOpenDetail = { onOpenDetail(flight) },
                            onEdit = { onEditFlight(flight) },
                            onSign = { approved -> onSignFlight(flight.id, approved) }
                        )
                    }
                    if (flights.size > 20) {
                        Text("... und ${flights.size - 20} weitere Flüge",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightRow(
    flight: FlightModel,
    isAdmin: Boolean,
    isInstructor: Boolean,
    onOpenDetail: () -> Unit,
    onEdit: () -> Unit,
    onSign: (Boolean) -> Unit
) {
    val dateStr = remember(flight.date) {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(flight.date))
    }
    val isSigned = flight.instructorApproved == true
    // Schulpersonal (Fluglehrer und Admin) darf unterschreiben
    val canSign = isInstructor || isAdmin

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
                    .clickable(onClick = onOpenDetail)
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(dateStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    // Prüfungsergebnis
                    if (flight.pruefungBestanden != null) {
                        val bestanden = flight.pruefungBestanden == true
                        Icon(
                            if (bestanden) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = if (bestanden) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(Icons.Default.ChevronRight, "Details ansehen",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "${flight.startLocation.ifBlank { "?" }} → ${flight.landingLocation.ifBlank { "?" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(flight.flightType, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${flight.durationMinutes / 60}h ${flight.durationMinutes % 60}m",
                    style = MaterialTheme.typography.bodySmall)

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Admin: Bearbeiten-Button
                    if (isAdmin) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, "Bearbeiten",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // Fluglehrer/Admin: Unterschreiben bzw. Unterschrift zurückziehen
                    if (canSign) {
                        IconButton(
                            onClick = { onSign(!isSigned) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isSigned) Icons.Default.Verified else Icons.Default.RadioButtonUnchecked,
                                if (isSigned) "Unterschrift zurückziehen" else "Unterschreiben",
                                modifier = Modifier.size(20.dp),
                                tint = if (isSigned) MaterialTheme.colorScheme.tertiary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Unterschrift-Zeile: wer hat wann unterschrieben
        if (isSigned) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(Icons.Default.Verified, null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.tertiary)
                val signedDate = formatSignatureDate(flight.approvedAt)
                val label = when {
                    !flight.approvedByName.isNullOrBlank() && signedDate != null ->
                        "Unterschrieben von ${flight.approvedByName} · $signedDate"
                    !flight.approvedByName.isNullOrBlank() ->
                        "Unterschrieben von ${flight.approvedByName}"
                    else -> "Vom Fluglehrer bestätigt"
                }
                Text(label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// Wandelt einen ISO-8601-Zeitstempel (z. B. "2026-06-13T10:15:30Z" oder
// "2026-06-13 10:15:30+00") in "dd.MM.yyyy" um. Der Datumsteil steht immer vorne.
private fun formatSignatureDate(iso: String?): String? {
    if (iso.isNullOrBlank() || iso.length < 10) return null
    val datePart = iso.substring(0, 10) // yyyy-MM-dd
    val parts = datePart.split("-")
    if (parts.size != 3) return null
    return "${parts[2]}.${parts[1]}.${parts[0]}"
}

// ---- Detail-Dialog: alle Felder eines Flugs ansehen ----------------------

@Composable
private fun FlightDetailDialog(flight: FlightModel, onDismiss: () -> Unit) {
    val dateStr = remember(flight.date) {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(flight.date))
    }
    val typeLabel = FlightType.entries.find { it.name == flight.flightType }?.displayName
        ?: flight.flightType
    val durationStr = "${flight.durationMinutes / 60}h ${flight.durationMinutes % 60}m"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.FlightTakeoff, null) },
        title = { Text(dateStr) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DetailRow("Flugtyp", typeLabel)
                DetailRow("Gleitschirm", flight.gliderName)
                DetailRow("Flugdauer", durationStr)
                DetailRow("Startplatz", flight.startLocation)
                DetailRow("Landeplatz", flight.landingLocation)
                DetailRow("Max. Höhe", flight.maxAltitude?.let { "$it m" })
                DetailRow("Distanz", flight.distance?.let { "$it km" })
                DetailRow("Wind", flight.windConditions)
                DetailRow("Bewölkung", flight.cloudCover)
                DetailRow("Temperatur", flight.temperature?.let { "$it °C" })
                DetailRow("Prüfung bestanden", when (flight.pruefungBestanden) {
                    true -> "Ja"; false -> "Nein"; null -> null
                })
                DetailRow("Notizen", flight.notes)

                if (flight.instructorApproved == true) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    val signedDate = formatSignatureDate(flight.approvedAt)
                    val sig = when {
                        !flight.approvedByName.isNullOrBlank() && signedDate != null ->
                            "${flight.approvedByName} · $signedDate"
                        !flight.approvedByName.isNullOrBlank() -> flight.approvedByName
                        else -> "bestätigt"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Verified, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary)
                        Text("Unterschrieben von $sig",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.4f))
        Text(value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f))
    }
}

// ---- Edit-Dialog für School-Admin ----------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlightEditDialog(
    flight: FlightModel,
    onDismiss: () -> Unit,
    onSave: (FlightModel) -> Unit
) {
    var glider by remember { mutableStateOf(flight.gliderName) }
    var startLoc by remember { mutableStateOf(flight.startLocation) }
    var landingLoc by remember { mutableStateOf(flight.landingLocation) }
    var notes by remember { mutableStateOf(flight.notes) }
    var durationH by remember { mutableStateOf((flight.durationMinutes / 60).toString()) }
    var durationM by remember { mutableStateOf((flight.durationMinutes % 60).toString()) }

    // Prüfungsergebnis nur bei PRUEFUNGSFLUG
    var pruefungBestanden by remember { mutableStateOf(flight.pruefungBestanden) }
    val isExamFlight = flight.flightType == "PRUEFUNGSFLUG"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Flug bearbeiten") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = glider,
                    onValueChange = { glider = it },
                    label = { Text("Gleitschirm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = startLoc,
                    onValueChange = { startLoc = it },
                    label = { Text("Startplatz") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = landingLoc,
                    onValueChange = { landingLoc = it },
                    label = { Text("Landeplatz") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationH,
                        onValueChange = { durationH = it.filter(Char::isDigit) },
                        label = { Text("Stunden") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationM,
                        onValueChange = { durationM = it.filter(Char::isDigit) },
                        label = { Text("Minuten") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notizen") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isExamFlight) {
                    Text("Prüfungsergebnis", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = pruefungBestanden == true,
                            onClick = { pruefungBestanden = if (pruefungBestanden == true) null else true },
                            label = { Text("Bestanden") },
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                            }
                        )
                        FilterChip(
                            selected = pruefungBestanden == false,
                            onClick = { pruefungBestanden = if (pruefungBestanden == false) null else false },
                            label = { Text("Nicht bestanden") },
                            leadingIcon = {
                                Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = durationH.toIntOrNull() ?: 0
                    val m = durationM.toIntOrNull() ?: 0
                    onSave(
                        flight.copy(
                            gliderName = glider.trim(),
                            startLocation = startLoc.trim(),
                            landingLocation = landingLoc.trim(),
                            notes = notes.trim(),
                            durationMinutes = h * 60 + m,
                            pruefungBestanden = if (isExamFlight) pruefungBestanden else flight.pruefungBestanden
                        )
                    )
                },
                enabled = glider.isNotBlank()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
