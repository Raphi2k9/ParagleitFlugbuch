package com.example.flugbuch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flugbuch.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinSchoolScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    var inviteCode by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flugschule beitreten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.School, null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(24.dp))

            Text("Invite-Code eingeben",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center)

            Text("Den Code erhältst du von deiner Flugschule.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp))

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.uppercase() },
                label = { Text("Invite-Code (z.B. ABC12345)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            resultMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(msg,
                    color = if (msg.startsWith("Fehler")) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    isLoading = true
                    authViewModel.joinSchool(inviteCode.trim()) { success ->
                        isLoading = false
                        resultMessage = if (success) "Erfolgreich beigetreten!"
                                        else "Fehler: Ungültiger Code oder Verbindungsproblem."
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading && inviteCode.length >= 6
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Beitreten")
                }
            }
        }
    }
}
