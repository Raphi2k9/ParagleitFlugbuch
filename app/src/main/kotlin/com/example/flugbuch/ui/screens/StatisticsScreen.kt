package com.example.flugbuch.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flugbuch.viewmodel.FlightTypeStats
import com.example.flugbuch.viewmodel.GliderStats
import com.example.flugbuch.viewmodel.StatisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onNavigateBack: () -> Unit
) {
    val stats by viewModel.statistics.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiken", fontWeight = FontWeight.Bold) },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (stats.totalFlights == 0) {
                EmptyStats()
                return@Column
            }

            // Gesamtübersicht
            OverviewCards(stats.totalFlights, stats.totalMinutes)

            // Flüge nach Flugart
            if (stats.typeStats.isNotEmpty()) {
                StatCard(title = "Flüge nach Flugart") {
                    FlightTypeChart(stats.typeStats)
                }
            }

            // Flüge nach Schirm
            if (stats.gliderStats.isNotEmpty()) {
                StatCard(title = "Flüge nach Schirm") {
                    GliderStatsTable(stats.gliderStats)
                }
            }

            // Monatsübersicht
            if (stats.flightsByMonth.isNotEmpty()) {
                StatCard(title = "Flüge pro Monat") {
                    MonthlyBarChart(stats.flightsByMonth)
                }
            }
        }
    }
}

@Composable
private fun EmptyStats() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.BarChart,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Noch keine Daten für Statistiken",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun OverviewCards(totalFlights: Int, totalMinutes: Int) {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatSummaryCard(
            modifier = Modifier.weight(1f),
            value = totalFlights.toString(),
            label = "Flüge gesamt",
            icon = Icons.Default.FlightTakeoff,
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
        StatSummaryCard(
            modifier = Modifier.weight(1f),
            value = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
            label = "Flugstunden",
            icon = Icons.Default.Schedule,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    }
}

@Composable
private fun StatSummaryCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FlightTypeChart(typeStats: List<FlightTypeStats>) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline
    )
    val maxCount = typeStats.maxOf { it.count }.toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        typeStats.forEachIndexed { index, stat ->
            val color = colors[index % colors.size]
            val fraction = if (maxCount > 0) stat.count / maxCount else 0f
            val hours = stat.totalMinutes / 60
            val minutes = stat.totalMinutes % 60
            val durationText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stat.flightType.displayName,
                    modifier = Modifier.width(120.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(
                                color = color.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
                Text(
                    "${stat.count} (${durationText})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.width(90.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun GliderStatsTable(gliderStats: List<GliderStats>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Schirm", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Flüge", modifier = Modifier.width(50.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            Text("Stunden", modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
        HorizontalDivider()

        gliderStats.take(10).forEach { stat ->
            val h = stat.totalMinutes / 60
            val m = stat.totalMinutes % 60
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stat.gliderName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text("${stat.count}", modifier = Modifier.width(50.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                Text(
                    if (h > 0) "${h}h ${m}m" else "${m}m",
                    modifier = Modifier.width(70.dp),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun MonthlyBarChart(flightsByMonth: Map<String, Int>) {
    if (flightsByMonth.isEmpty()) return

    val entries = flightsByMonth.entries.toList()
    val maxValue = entries.maxOf { it.value }.toFloat().coerceAtLeast(1f)
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = (size.width / entries.size) * 0.6f
            val spacing = (size.width / entries.size) * 0.4f
            val chartHeight = size.height - 32.dp.toPx()

            entries.forEachIndexed { index, (month, count) ->
                val x = index * (barWidth + spacing) + spacing / 2
                val barHeight = (count / maxValue) * chartHeight
                val y = chartHeight - barHeight

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )

                // Monatslabel (nur MM)
                drawContext.canvas.nativeCanvas.drawText(
                    month.takeLast(2),
                    x + barWidth / 2,
                    size.height,
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 9.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }

    Text(
        "Letzte ${entries.size} Monate",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}
