package com.example.flugbuch.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flugbuch.data.entities.FlightType
import com.example.flugbuch.ui.components.FlightCard
import com.example.flugbuch.ui.theme.ThemePreference
import com.example.flugbuch.viewmodel.FilterState
import com.example.flugbuch.viewmodel.FlightViewModel
import com.example.flugbuch.viewmodel.SortOrder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightListScreen(
    viewModel: FlightViewModel,
    onAddFlight: () -> Unit,
    onEditFlight: (Int) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit
) {
    val flights by viewModel.filteredFlights.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val gliders by viewModel.allGliders.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Filter-Badge wenn aktiv
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (filterState.isFiltered) {
                                    Badge()
                                }
                            }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter & Sortierung")
                        }
                    }
                    IconButton(onClick = { onNavigateToStats() }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Statistiken")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menü")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export / Import") },
                            leadingIcon = { Icon(Icons.Default.ImportExport, null) },
                            onClick = { showMenu = false; onNavigateToExport() }
                        )
                        DropdownMenuItem(
                            text = { Text("Einstellungen") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = { showMenu = false; onNavigateToSettings() }
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
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filterState.isFiltered) {
                    item {
                        ActiveFilterChips(
                            filterState = filterState,
                            onClear = {
                                viewModel.updateFilterState(
                                    filterState.copy(
                                        selectedFlightTypes = emptySet(),
                                        selectedGliders = emptySet()
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
                    val swipeState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            value == SwipeToDismissBoxValue.EndToStart
                        }
                    )

                    LaunchedEffect(swipeState.currentValue) {
                        if (swipeState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            var undone = false
                            val snackbarJob = launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Flug löschen...",
                                    actionLabel = "Rückgängig",
                                    duration = SnackbarDuration.Indefinite
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    undone = true
                                    swipeState.reset()
                                }
                            }
                            delay(2000)
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarJob.join()
                            if (!undone) {
                                viewModel.deleteFlight(flight)
                            }
                        }
                    }

                    SwipeToDismissBox(
                        state = swipeState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val bgColor by animateColorAsState(
                                targetValue = when (swipeState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color.Transparent
                                },
                                label = "swipe_bg"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(bgColor, shape = MaterialTheme.shapes.medium)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (swipeState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Löschen",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    ) {
                        FlightCard(
                            flight = flight,
                            onClick = { onEditFlight(flight.id) },
                            onLongClick = {
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Flug löschen?",
                                        actionLabel = "Löschen",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.deleteFlight(flight)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

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
private fun ActiveFilterChips(filterState: FilterState, onClear: () -> Unit) {
    val parts = buildList {
        if (filterState.selectedFlightTypes.isNotEmpty()) {
            add(
                if (filterState.selectedFlightTypes.size == 1)
                    filterState.selectedFlightTypes.first().displayName
                else
                    "${filterState.selectedFlightTypes.size} Flugarten"
            )
        }
        if (filterState.selectedGliders.isNotEmpty()) {
            add(
                if (filterState.selectedGliders.size == 1)
                    filterState.selectedGliders.first()
                else
                    "${filterState.selectedGliders.size} Schirme"
            )
        }
    }
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text("Filter: ${parts.joinToString(", ")}") },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = "Filter entfernen",
                modifier = Modifier.size(16.dp)
            )
        }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Filter & Sortierung",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // --- Sortierung ---
            Text("Sortierung", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
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

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // --- Multi-Select: Flugart ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter nach Flugart", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = {
                    localState = if (localState.selectedFlightTypes.size == FlightType.entries.size) {
                        localState.copy(selectedFlightTypes = emptySet())
                    } else {
                        localState.copy(selectedFlightTypes = FlightType.entries.toSet())
                    }
                }) {
                    Text(
                        if (localState.selectedFlightTypes.size == FlightType.entries.size)
                            "Alle abwählen"
                        else
                            "Alle auswählen"
                    )
                }
            }
            FlightType.entries.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            localState = localState.copy(
                                selectedFlightTypes = if (type in localState.selectedFlightTypes)
                                    localState.selectedFlightTypes - type
                                else
                                    localState.selectedFlightTypes + type
                            )
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = type in localState.selectedFlightTypes,
                        onCheckedChange = { checked ->
                            localState = localState.copy(
                                selectedFlightTypes = if (checked)
                                    localState.selectedFlightTypes + type
                                else
                                    localState.selectedFlightTypes - type
                            )
                        }
                    )
                    Column {
                        Text(type.displayName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            type.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (gliders.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // --- Multi-Select: Schirm ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filter nach Schirm", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = {
                        localState = if (localState.selectedGliders.size == gliders.size) {
                            localState.copy(selectedGliders = emptySet())
                        } else {
                            localState.copy(selectedGliders = gliders.toSet())
                        }
                    }) {
                        Text(
                            if (localState.selectedGliders.size == gliders.size)
                                "Alle abwählen"
                            else
                                "Alle auswählen"
                        )
                    }
                }
                gliders.forEach { glider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                localState = localState.copy(
                                    selectedGliders = if (glider in localState.selectedGliders)
                                        localState.selectedGliders - glider
                                    else
                                        localState.selectedGliders + glider
                                )
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = glider in localState.selectedGliders,
                            onCheckedChange = { checked ->
                                localState = localState.copy(
                                    selectedGliders = if (checked)
                                        localState.selectedGliders + glider
                                    else
                                        localState.selectedGliders - glider
                                )
                            }
                        )
                        Text(glider, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { localState = FilterState() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Zurücksetzen")
                }
                Button(
                    onClick = { onApply(localState) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Anwenden")
                }
            }
        }
    }
}
