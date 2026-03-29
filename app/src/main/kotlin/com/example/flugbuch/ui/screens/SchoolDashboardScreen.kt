package com.example.flugbuch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flugbuch.data.model.FlightModel
import com.example.flugbuch.data.model.StudentWithFlights
import com.example.flugbuch.data.model.UserProfile
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
    onNavigateBack: () -> Unit
) {
    val studentsWithFlights by schoolViewModel.studentsWithFlights.collectAsState()
    val isLoading by schoolViewModel.isLoading.collectAsState()
    val error by schoolViewModel.error.collectAsState()
    val schoolName by schoolViewModel.schoolName.collectAsState()

    LaunchedEffect(schoolId) {
        schoolViewModel.loadSchoolData(schoolId)
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

                studentsWithFlights.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
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

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Statistik-Header
                        item {
                            SchoolStatsHeader(studentsWithFlights)
                        }

                        // Schüler-Karten
                        items(studentsWithFlights, key = { it.profile.id }) { studentData ->
                            StudentCard(studentData)
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
            StatItem(
                icon = Icons.Default.People,
                value = "${data.size}",
                label = "Schüler"
            )
            StatItem(
                icon = Icons.Default.FlightTakeoff,
                value = "$totalFlights",
                label = "Flüge gesamt"
            )
            StatItem(
                icon = Icons.Default.Schedule,
                value = "${totalMinutes / 60}h ${totalMinutes % 60}m",
                label = "Flugzeit"
            )
        }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
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
private fun StudentCard(data: StudentWithFlights) {
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
                    verticalAlignment = Alignment.CenterVertically
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
            if (expanded && flights.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                flights.take(20).forEach { flight ->
                    FlightRow(flight)
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

@Composable
private fun FlightRow(flight: FlightModel) {
    val dateStr = remember(flight.date) {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(flight.date))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(dateStr, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${flight.startLocation.ifBlank { "?" }} → ${flight.landingLocation.ifBlank { "?" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${flight.durationMinutes / 60}h ${flight.durationMinutes % 60}m",
                style = MaterialTheme.typography.bodySmall)
            Text(flight.flightType, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
