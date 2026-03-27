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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flugbuch.R
import com.example.flugbuch.viewmodel.AltitudeStats
import com.example.flugbuch.viewmodel.DistanceStats
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
                title = { Text(stringResource(R.string.stats_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                StatCard(title = stringResource(R.string.stats_by_type)) {
                    FlightTypeChart(stats.typeStats)
                }
            }

            // Flüge nach Schirm
            if (stats.gliderStats.isNotEmpty()) {
                StatCard(title = stringResource(R.string.stats_by_glider)) {
                    GliderStatsTable(stats.gliderStats)
                }
            }

            // Monatsübersicht
            if (stats.flightsByMonth.isNotEmpty()) {
                StatCard(title = stringResource(R.string.stats_by_month)) {
                    MonthlyBarChart(stats.flightsByMonth)
                }
            }

            // Distanz-Statistiken
            stats.distanceStats?.let { dist ->
                DistanceStatsSection(dist)
            }

            // Höhen-Statistiken
            stats.altitudeStats?.let { alt ->
                AltitudeStatsSection(alt)
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
                stringResource(R.string.stats_empty),
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
            label = stringResource(R.string.stats_total_flights),
            icon = Icons.Default.FlightTakeoff,
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
        StatSummaryCard(
            modifier = Modifier.weight(1f),
            value = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
            label = stringResource(R.string.stats_total_hours),
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
                    stringResource(stat.flightType.labelRes),
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
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.stats_glider_col), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.stats_flights_col), modifier = Modifier.width(50.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            Text(stringResource(R.string.stats_hours_col), modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
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

    val lastMonthsLabel = stringResource(R.string.stats_last_months, entries.size)
    Text(
        lastMonthsLabel,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

// ─── Distanz-Statistiken ────────────────────────────────────────────────────

@Composable
private fun DistanceStatsSection(dist: DistanceStats) {
    StatCard(title = stringResource(R.string.stats_distance_section)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniStatCard(
                modifier = Modifier.weight(1f),
                value = "%.1f km".format(dist.totalKm),
                label = stringResource(R.string.stats_dist_total),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
            MiniStatCard(
                modifier = Modifier.weight(1f),
                value = "%.1f km".format(dist.avgKm),
                label = stringResource(R.string.stats_dist_avg),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
            MiniStatCard(
                modifier = Modifier.weight(1f),
                value = "%.1f km".format(dist.maxKm),
                label = stringResource(R.string.stats_dist_max),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        }

        if (dist.byGlider.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.stats_dist_by_glider), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            val maxDist = dist.byGlider.maxOf { it.second }.coerceAtLeast(0.001)
            dist.byGlider.take(5).forEach { (name, km) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(name, modifier = Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Box(modifier = Modifier.weight(1f).height(20.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((km / maxDist).toFloat().coerceIn(0f, 1f))
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    MaterialTheme.shapes.small
                                )
                        )
                    }
                    Text("%.1f".format(km), modifier = Modifier.width(55.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        if (dist.byMonth.size >= 2) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.stats_dist_by_month), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            SimpleLineChart(
                data = dist.byMonth.entries.map { it.key to it.value.toFloat() },
                lineColor = MaterialTheme.colorScheme.primary,
                unit = "km",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─── Höhen-Statistiken ───────────────────────────────────────────────────────

@Composable
private fun AltitudeStatsSection(alt: AltitudeStats) {
    StatCard(title = stringResource(R.string.stats_altitude_section)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniStatCard(
                modifier = Modifier.weight(1f),
                value = "${alt.globalMaxAlt} m",
                label = stringResource(R.string.stats_alt_highest),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
            MiniStatCard(
                modifier = Modifier.weight(1f),
                value = "%.0f m".format(alt.avgMaxAlt),
                label = stringResource(R.string.stats_alt_avg),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        }

        if (alt.maxByGlider.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.stats_alt_by_glider), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            val maxAlt = alt.maxByGlider.maxOf { it.second }.coerceAtLeast(1)
            alt.maxByGlider.take(5).forEach { (name, altM) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(name, modifier = Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Box(modifier = Modifier.weight(1f).height(20.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(altM.toFloat() / maxAlt)
                                .background(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                    MaterialTheme.shapes.small
                                )
                        )
                    }
                    Text("$altM m", modifier = Modifier.width(55.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        if (alt.byMonth.size >= 2) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.stats_alt_by_month), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            SimpleLineChart(
                data = alt.byMonth.entries.map { it.key to it.value.toFloat() },
                lineColor = MaterialTheme.colorScheme.secondary,
                unit = "m",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    containerColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SimpleLineChart(
    data: List<Pair<String, Float>>,
    lineColor: Color,
    unit: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOf { it.second }.coerceAtLeast(0.001f)
    val labelColor = MaterialTheme.colorScheme.onSurface
    val fillColor = lineColor.copy(alpha = 0.15f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            if (data.size < 2) {
                drawCircle(lineColor, 6.dp.toPx(), Offset(size.width / 2f, size.height / 2f))
                return@Canvas
            }

            val stepX = size.width / (data.size - 1).toFloat()
            val points = data.mapIndexed { i, (_, v) ->
                Offset(i * stepX, size.height * (1f - v / maxValue))
            }

            // Fläche unter der Linie
            val fillPath = Path().apply {
                moveTo(points.first().x, size.height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height)
                close()
            }
            drawPath(fillPath, fillColor)

            // Linie
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
            drawPath(linePath, lineColor, style = Stroke(width = 2.dp.toPx()))

            // Punkte
            points.forEach { drawCircle(lineColor, 3.dp.toPx(), it) }

            // Y-Achse Labels (Max und Min)
            val paint = android.graphics.Paint().apply {
                color = labelColor.copy(alpha = 0.6f).toArgb()
                textSize = 9.dp.toPx()
            }
            drawContext.canvas.nativeCanvas.drawText(
                "%.0f $unit".format(maxValue),
                4.dp.toPx(), 12.dp.toPx(), paint
            )
        }

        // X-Achse Labels
        if (data.size <= 6) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                data.forEach { (label, _) ->
                    Text(label.takeLast(5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(data.first().first.takeLast(5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.weight(1f))
                Text(data[data.size / 2].first.takeLast(5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.weight(1f))
                Text(data.last().first.takeLast(5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}
