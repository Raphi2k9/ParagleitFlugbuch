package com.example.flugbuch.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flugbuch.viewmodel.FlightViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportScreen(
    viewModel: FlightViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }
    var exportedFilePath by remember { mutableStateOf<String?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Import JSON Launcher
    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isLoading = true
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: ""
            viewModel.importFromJson(content) { count ->
                isLoading = false
            }
        }
    }

    // Import CSV Launcher
    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isLoading = true
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: ""
            viewModel.importFromCsv(content) { count ->
                isLoading = false
            }
        }
    }

    // Import IGC Launcher
    val importIgcLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isLoading = true
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: ""
            viewModel.importFromIgc(content) { _ ->
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export / Import", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Export Sektion
            InfoCard(
                title = "Daten exportieren",
                description = "Exportiere alle Flüge als JSON oder CSV-Datei für Backups oder zur Weiterverarbeitung.",
                icon = Icons.Default.Upload
            )

            // JSON Export
            ExportImportActionCard(
                title = "Als JSON exportieren",
                description = "Vollständiger Export aller Daten im JSON-Format. Ideal für Backups und Re-Import.",
                icon = Icons.Default.DataObject,
                buttonText = "JSON exportieren",
                buttonEnabled = !isLoading
            ) {
                isLoading = true
                viewModel.exportFlightsToJson(context) { file ->
                    isLoading = false
                    file?.let {
                        exportedFilePath = it.absolutePath
                        // Share-Intent
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                it
                            )
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Flugbuch exportieren"))
                    }
                }
            }

            // CSV Export
            ExportImportActionCard(
                title = "Als CSV exportieren",
                description = "Export im CSV-Format für Tabellenkalkulationen (Excel, LibreOffice).",
                icon = Icons.Default.TableChart,
                buttonText = "CSV exportieren",
                buttonEnabled = !isLoading
            ) {
                isLoading = true
                viewModel.exportFlightsToCsv(context) { file ->
                    isLoading = false
                    file?.let {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                it
                            )
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Flugbuch CSV exportieren"))
                    }
                }
            }

            HorizontalDivider()

            // Import Sektion
            InfoCard(
                title = "Daten importieren",
                description = "Importiere Flüge aus einer JSON- oder CSV-Datei. Bestehende Flüge bleiben erhalten.",
                icon = Icons.Default.Download
            )

            ExportImportActionCard(
                title = "Aus JSON importieren",
                description = "Wähle eine JSON-Datei aus, die zuvor mit dieser App exportiert wurde.",
                icon = Icons.Default.FileOpen,
                buttonText = "JSON-Datei auswählen",
                buttonEnabled = !isLoading
            ) {
                importJsonLauncher.launch("application/json")
            }

            ExportImportActionCard(
                title = "Aus CSV importieren",
                description = "Importiere Flüge aus einer CSV-Datei. Unterstützt eigene Spaltennamen (Deutsch & Englisch). Pflichtfelder: Datum, Schirm, Dauer.",
                icon = Icons.Default.TableChart,
                buttonText = "CSV-Datei auswählen",
                buttonEnabled = !isLoading
            ) {
                importCsvLauncher.launch("text/*")
            }

            ExportImportActionCard(
                title = "Aus IGC importieren",
                description = "Importiere einen Flug aus einer IGC-Tracklog-Datei. Datum, Schirm, Dauer, Höhe und Distanz werden automatisch ausgelesen.",
                icon = Icons.Default.FlightTakeoff,
                buttonText = "IGC-Datei auswählen",
                buttonEnabled = !isLoading
            ) {
                importIgcLauncher.launch("*/*")
            }

            HorizontalDivider()

            // Gefahrenzone
            InfoCard(
                title = "Gefahrenzone",
                description = "Aktionen hier können nicht rückgängig gemacht werden.",
                icon = Icons.Default.Warning
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DeleteForever,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Flugbuch leeren",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Löscht alle Flüge unwiderruflich. Schirme bleiben erhalten.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showDeleteAllDialog = true },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Alles löschen")
                    }
                }
            }

            if (showDeleteAllDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteAllDialog = false },
                    icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Flugbuch leeren?") },
                    text = { Text("Alle Flüge werden unwiderruflich gelöscht. Diese Aktion kann nicht rückgängig gemacht werden.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteAllDialog = false
                                viewModel.deleteAllFlights()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Ja, alles löschen")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDeleteAllDialog = false }) {
                            Text("Abbrechen")
                        }
                    }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            exportedFilePath?.let { path ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Exportiert: $path",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ExportImportActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonText: String,
    buttonEnabled: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAction,
                enabled = buttonEnabled,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(buttonText)
            }
        }
    }
}
