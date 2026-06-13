package com.example.flugbuch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flugbuch.auth.AuthState
import com.example.flugbuch.viewmodel.AuthViewModel
import com.example.flugbuch.viewmodel.SchoolViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSchoolScreen(
    schoolViewModel: SchoolViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val isLoading by schoolViewModel.isLoading.collectAsState()
    val clipboard = LocalClipboardManager.current

    var schoolName by remember { mutableStateOf("") }
    var schoolLocation by remember { mutableStateOf("") }
    var createdInviteCode by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentUserId = (authState as? AuthState.LoggedIn)?.profile?.id ?: ""

    // Beim Verlassen nach erfolgreicher Erstellung das Profil neu laden,
    // damit school_id/Rolle aktuell sind. Nicht früher – sonst navigiert
    // MainActivity sofort weg und der Invite-Code wäre nie sichtbar.
    val finishAndRefresh = {
        if (createdInviteCode != null) authViewModel.refreshProfile()
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flugschule erstellen") },
                navigationIcon = {
                    IconButton(onClick = finishAndRefresh) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (createdInviteCode == null) {
                // Formular
                Icon(Icons.Default.School, null,
                    modifier = Modifier.size(56.dp).align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.primary)

                Text("Neue Flugschule",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally))

                OutlinedTextField(
                    value = schoolName,
                    onValueChange = { schoolName = it },
                    label = { Text("Name der Flugschule") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = schoolLocation,
                    onValueChange = { schoolLocation = it },
                    label = { Text("Standort (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        errorMessage = null
                        schoolViewModel.createSchool(
                            name = schoolName.trim(),
                            location = schoolLocation.trim(),
                            adminUserId = currentUserId
                        ) { code ->
                            if (code != null) {
                                createdInviteCode = code
                            } else {
                                errorMessage = "Fehler beim Erstellen der Schule."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isLoading && schoolName.isNotBlank() && currentUserId.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Schule erstellen")
                    }
                }
            } else {
                // Erfolg: Invite-Code anzeigen
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Schule erstellt!", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)

                        Text("Dein Invite-Code für Schüler:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)

                        Text(createdInviteCode!!,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)

                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(createdInviteCode!!))
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Code kopieren")
                        }
                    }
                }

                Text("Teile diesen Code mit deinen Schülern. Sie können ihn in " +
                     "Einstellungen → Flugschule beitreten eingeben.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(
                    onClick = finishAndRefresh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fertig")
                }
            }
        }
    }
}
