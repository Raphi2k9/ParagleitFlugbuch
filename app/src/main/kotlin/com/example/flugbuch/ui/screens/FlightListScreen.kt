package com.example.flugbuch.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flugbuch.data.entities.FlightType
import com.example.flugbuch.ui.components.FlightCard
import com.example.flugbuch.viewmodel.FilterState
import com.example.flugbuch.viewmodel.FilterType
import com.example.flugbuch.viewmodel.FlightViewModel
import com.example.flugbuch.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightListScreen(
    viewModel: FlightViewModel,
    onAddFlight: () -> Unit,
    onEditFlight: (Int) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    val flights by viewModel.filteredFlights.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val gliders by viewModel.allGliders.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Flugbuch", fontWeight = FontWeight.Bold)
                        Text(
                            "${flights.size} Flüge",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter & Sortierung")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menü")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Statistiken") },
                            leadingIcon = { Icon(Icons.Default.BarChart, null) },
                            onClick = { showMenu = false; onNavigateToStats() }
                        )
                        DropdownMenuItem(
                            text = { Text("Export / Import") },
                            leadingIcon = { Icon(Icons.Default.ImportExport, null) },
                            onClick = { showMenu = false; onNavigateToExport() }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddFlight,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Neuer Flug") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        if (flights.isEmpty()) {
            EmptyFlightList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 88.dp  // FAB-Abstand
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Aktive Filter anzeigen
                if (filterState.filterType != FilterType.ALL) {
                    item {
                        ActiveFilterChip(
                            filterState = filterState,
                            onClear = {
                                viewModel.updateFilterState(
                                    filterState.copy(
                                        filterType = FilterType.ALL,
                                        selectedFlightType = null,
                                        selectedGlider = null
                                    )
                                )
                            }
                        )
                    }
                }

                items(
                    items = flights,
                    key = { it.id }
                ) { flight ->
                    FlightCard(
                        flight = flight,
                        onClick = { onEditFlight(flight.id) },
                        onLongClick = { viewModel.deleteFlight(flight) }
                    )
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterSortSheet(
            filterState = filterState,
            gliders = gliders.map { it.name },
            onDismiss = { showFilterSheet = false },
            onApply = { newState ->
                viewModel.updateFilterState(newState)
                showFilterSheet = false
            }
        )
    }
}

@Composable
private fun EmptyFlightList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Paragliding,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Noch keine Flüge erfasst",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tippe auf + um deinen ersten Flug\neinzutragen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActiveFilterChip(filterState: FilterState, onClear: () -> Unit) {
    val label = when (filterState.filterType) {
        FilterType.BY_TYPE -> "Flugart: ${filterState.selectedFlightType?.displayName}"
        FilterType.BY_GLIDER -> "Schirm: ${filterState.selectedGlider}"
        FilterType.ALL -> ""
    }
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Filter entfernen", modifier = Modifier.size(16.dp)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSortSheet(
    filterState: FilterState,
    gliders: List<String>,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit
) {
    var localState by remember { mutableStateOf(filterState) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Filter & Sortierung",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Sortierung
            Text("Sortierung", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortOrder.entries.forEach { order ->
                    val label = when (order) {
                        SortOrder.DATE_DESC -> "Datum ↓"
                        SortOrder.DATE_ASC -> "Datum ↑"
                        SortOrder.TYPE -> "Flugart"
                        SortOrder.GLIDER -> "Schirm"
                    }
                    FilterChip(
                        selected = localState.sortOrder == order,
                        onClick = { localState = localState.copy(sortOrder = order) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Filter nach Flugart
            Text("Filter nach Flugart", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = localState.filterType == FilterType.ALL,
                    onClick = {
                        localState = localState.copy(
                            filterType = FilterType.ALL,
                            selectedFlightType = null
                        )
                    },
                    label = { Text("Alle") }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            FlightType.entries.forEach { type ->
                FilterChip(
                    selected = localState.filterType == FilterType.BY_TYPE && localState.selectedFlightType == type,
                    onClick = {
                        localState = localState.copy(
                            filterType = FilterType.BY_TYPE,
                            selectedFlightType = type
                        )
                    },
                    label = { Text(type.displayName) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (gliders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Filter nach Schirm", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                gliders.forEach { glider ->
                    FilterChip(
                        selected = localState.filterType == FilterType.BY_GLIDER && localState.selectedGlider == glider,
                        onClick = {
                            localState = localState.copy(
                                filterType = FilterType.BY_GLIDER,
                                selectedGlider = glider
                            )
                        },
                        label = { Text(glider) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onApply(localState) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Anwenden")
            }
        }
    }
}
