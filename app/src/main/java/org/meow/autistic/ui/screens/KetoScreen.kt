package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.keto.KetoItemEntry
import org.meow.autistic.data.keto.KetoLogEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val GOAL_NET_CARBS = 20.0

/** At most 4 characters: whole number up to 9999, one decimal up to 999.9, no decimals above that. */
private val Double.fmt: String
    get() = when {
        this >= 1000.0 || this == kotlin.math.floor(this) -> this.toInt().toString()
        else -> "%.1f".format(this)
    }

@Composable
fun KetoScreen(modifier: Modifier = Modifier, viewModel: KetoViewModel = koinViewModel()) {
    val date by viewModel.date.collectAsState()
    val totals by viewModel.totals.collectAsState()
    val entryList by viewModel.items.collectAsState()
    val showNewEntryOptions by viewModel.showNewEntryOptions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddEntryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { item ->
                viewModel.addItem(item)
                showAddDialog = false
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = { KetoDateBar(date = date, onPrev = viewModel::previousDay, onNext = viewModel::nextDay) },
        bottomBar = {
            NewEntryBar(
                expanded = showNewEntryOptions,
                onToggle = viewModel::toggleNewEntryOptions,
                onManual = {
                    viewModel.toggleNewEntryOptions()
                    showAddDialog = true
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            item { NutritionSummary(totals) }
            if (entryList.isNotEmpty()) {
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                item {
                    Text(
                        "Entries",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                    )
                }
                items(entryList, key = { it.id }) { item ->
                    EntryListItem(item = item, onDelete = { viewModel.deleteItem(item) })
                }
            }
        }
    }
}

@Composable
private fun NutritionSummary(totals: KetoLogEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Carbohydrates",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        NutritionRow("Total Carbs", totals.totalCarbs, "g", indent = 0.dp)
        NutritionRow("Dietary Fiber", totals.fiber, "g", indent = 16.dp)
        NutritionRow("Total Sugars", totals.totalSugars, "g", indent = 16.dp)
        NutritionRow("Added Sugars", totals.addedSugars, "g", indent = 32.dp)
        NutritionRow("Sugar Alcohols", totals.sugarAlcohols, "g", indent = 16.dp)
        NetCarbsRow(netCarbs = totals.netCarbs)
    }
}

@Composable
private fun NutritionRow(label: String, value: Double, unit: String, indent: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = indent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "${value.fmt}$unit",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 52.dp),
        )
    }
}

@Composable
private fun NetCarbsRow(netCarbs: Double) {
    val progress = (netCarbs / GOAL_NET_CARBS).toFloat().coerceIn(0f, 1f)
    val overGoal = netCarbs > GOAL_NET_CARBS
    val barColor = if (overGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 48.dp, end = 48.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Net Carbs", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "${netCarbs.fmt} / ${GOAL_NET_CARBS.toInt()}g",
                style = MaterialTheme.typography.bodySmall,
                color = if (overGoal) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
            color = barColor,
        )
    }
}

@Composable
private fun EntryListItem(item: KetoItemEntry, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = { Text("${item.netCarbs.fmt}g net carbs · ${item.totalCarbs.fmt}g total") },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${item.name}")
            }
        },
    )
}

@Composable
private fun NewEntryBar(expanded: Boolean, onToggle: () -> Unit, onManual: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onManual, modifier = Modifier.weight(1f)) { Text("Manual") }
                    OutlinedButton(onClick = { /* TODO: camera flow */ }, modifier = Modifier.weight(1f)) { Text("Photo") }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) "Cancel" else "New Entry")
            }
        }
    }
}

@Composable
private fun AddEntryDialog(onDismiss: () -> Unit, onConfirm: (KetoItemEntry) -> Unit) {
    var name by remember { mutableStateOf("") }
    var totalCarbs by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var totalSugars by remember { mutableStateOf("") }
    var addedSugars by remember { mutableStateOf("") }
    var sugarAlcohols by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                CarbField("Total Carbs (g)", totalCarbs) { totalCarbs = it }
                CarbField("Fiber (g)", fiber) { fiber = it }
                CarbField("Total Sugars (g)", totalSugars) { totalSugars = it }
                CarbField("Added Sugars (g)", addedSugars) { addedSugars = it }
                CarbField("Sugar Alcohols (g)", sugarAlcohols) { sugarAlcohols = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        KetoItemEntry(
                            date = "",
                            name = name.trim(),
                            totalCarbs = totalCarbs.toDoubleOrNull() ?: 0.0,
                            fiber = fiber.toDoubleOrNull() ?: 0.0,
                            totalSugars = totalSugars.toDoubleOrNull() ?: 0.0,
                            addedSugars = addedSugars.toDoubleOrNull() ?: 0.0,
                            sugarAlcohols = sugarAlcohols.toDoubleOrNull() ?: 0.0,
                        )
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CarbField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KetoDateBar(date: String, onPrev: () -> Unit, onNext: () -> Unit) {
    val today = LocalDate.now()
    val parsed = LocalDate.parse(date)
    val label = when (parsed) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
    TopAppBar(
        title = { Text("Keto · $label") },
        navigationIcon = {
            IconButton(onClick = onPrev) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous day")
            }
        },
        actions = {
            IconButton(onClick = onNext) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next day")
            }
        },
    )
}
