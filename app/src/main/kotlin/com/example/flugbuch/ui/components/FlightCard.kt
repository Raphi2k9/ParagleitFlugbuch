package com.example.flugbuch.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flugbuch.R
import com.example.flugbuch.data.entities.FlightEntity
import com.example.flugbuch.data.entities.FlightType
import com.example.flugbuch.data.entities.TrainingExercise
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlightCard(
    flight: FlightEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY) }
    val formattedDate = remember(flight.date) {
        dateFormat.format(Date(flight.date))
    }
    val flightType = remember(flight.flightType) {
        FlightType.entries.find { it.name == flight.flightType }
    }
    val durationText = remember(flight.durationMinutes) {
        val h = flight.durationMinutes / 60
        val m = flight.durationMinutes % 60
        if (h > 0) "${h}h ${m}min" else "${m}min"
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.flight_card_delete_title)) },
            text = { Text(stringResource(R.string.flight_card_delete_text, formattedDate)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onLongClick()
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteDialog = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                FlightTypeBadge(flightType = flightType)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Paragliding,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = flight.gliderName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (flight.startLocation.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = flight.startLocation,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                flight.maxAltitude?.let { alt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Height,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${alt}m",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (flightType == FlightType.PRUEFUNGSFLUG && flight.pruefungBestanden != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (flight.pruefungBestanden) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (flight.pruefungBestanden)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (flight.pruefungBestanden)
                            stringResource(R.string.label_passed)
                        else
                            stringResource(R.string.label_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (flight.pruefungBestanden)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            if (flightType == FlightType.SCHULUNGSFLUG && !flight.trainingExercises.isNullOrBlank()) {
                val exercises = TrainingExercise.fromStorageString(flight.trainingExercises)
                if (exercises.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val names = exercises.take(3).joinToString(", ") { context.getString(it.labelRes) }
                        val suffix = if (exercises.size > 3) " …" else ""
                        Text(
                            text = stringResource(R.string.flight_card_exercises, exercises.size, names + suffix),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }

            if (flight.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = flight.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun FlightTypeBadge(flightType: FlightType?) {
    val color = when (flightType) {
        FlightType.THERMAL -> MaterialTheme.colorScheme.primary
        FlightType.MANEUVER_TRAINING -> MaterialTheme.colorScheme.tertiary
        FlightType.SIV -> MaterialTheme.colorScheme.error
        FlightType.CROSS_COUNTRY -> MaterialTheme.colorScheme.secondary
        FlightType.HIKE_AND_FLY -> MaterialTheme.colorScheme.secondary
        FlightType.SCHULUNGSFLUG -> MaterialTheme.colorScheme.tertiary
        FlightType.PRUEFUNGSFLUG -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.outline
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = if (flightType != null) stringResource(flightType.labelRes) else stringResource(R.string.label_unknown),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
