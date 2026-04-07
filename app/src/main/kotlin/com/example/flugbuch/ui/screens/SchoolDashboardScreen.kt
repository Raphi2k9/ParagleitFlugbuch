package com.example.flugbuch.ui.screens

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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flugbuch.data.model.FlightModel
import com.example.flugbuch.data.model.StudentWithFlights
import com.example.flugbuch.data.model.UserRole
import com.example.flugbuch.viewmodel.AuthViewModel
import com.example.flugbuch.viewmodel.SchoolViewModel
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

    val clipboard = LocalClipboardManager.current
    var showInviteCode by remember { mutableStateOf(false) }

    // Flight-Bearbeiten-Dialog State (Admin)
    var editingFlight by remember { mutableStateOf<FlightModel?>(null) }

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
                    if (isAdmin && inviteCode.isNotBlank()) {
                        IconButton(onClick = { showInviteCode = !showInviteCode }) {
                            Icon(Icons.Default.Share, "Invite-Code")
                        }
                    }
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
                        // Invite-Code Card (nur Admin, wenn sichtbar)
                        if (isAdmin && showInviteCode && inviteCode.isNotBlank()) {
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
                                            Text("Invite-Code",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                                            Text(inviteCode,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
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
                                onEditFlight = { flight -> editingFlight = flight },
                                onApproveFlight = { flightId, approved ->
                                    schoolViewModel.approveStudentFlight(flightId, approved, schoolId)
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
    onEditFlight: (FlightModel) -> Unit,
    onApproveFlight: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val profile = data.profile
    val flights = data.flights
    val totalMinutes = flights.sumOf { it.durationMinutes }

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
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Weniger" else "${flights.size} Flüge")
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, modifier = Modifier.size(16.dp)
                    )
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
                            onEdit = { onEditFlight(flight) },
                            onApprove = { approved -> onApproveFlight(flight.id, approved) }
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
    onEdit: () -> Unit,
    onApprove: (Boolean) -> Unit
) {
    val dateStr = remember(flight.date) {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(flight.date))
    }

    val approvedColor = when (flight.instructorApproved) {
        true -> MaterialTheme.colorScheme.tertiary
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
                // Instructor: Akzeptieren/Ablehnen
                if (isInstructor) {
                    val isApproved = flight.instructorApproved == true
                    IconButton(
                        onClick = { onApprove(!isApproved) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isApproved) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            if (isApproved) "Akzeptiert" else "Akzeptieren",
                            modifier = Modifier.size(20.dp),
                            tint = if (isApproved) MaterialTheme.colorScheme.tertiary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Auch Admin sieht Approve-Status (read-only Icon)
                if (isAdmin && flight.instructorApproved != null) {
                    val approved = flight.instructorApproved == true
                    Icon(
                        if (approved) Icons.Default.Verified else Icons.Default.RemoveCircleOutline,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = approvedColor
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
