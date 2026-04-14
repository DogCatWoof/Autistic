package org.meow.autistic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.keto.KetoLogEntry
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val GOAL_NET_CARBS = 20.0
private const val GOAL_FAT = 155.0
private const val GOAL_PROTEIN = 100.0
private const val GOAL_FIBER = 25.0
private const val GOAL_WATER = 64.0
private const val GOAL_SODIUM = 2000.0
private const val GOAL_SODIUM_MAX = 4000.0

private val Double.fmt: String
    get() = if (this == kotlin.math.floor(this)) this.toInt().toString() else "%.1f".format(this)

@Composable
fun KetoScreen(modifier: Modifier = Modifier, viewModel: KetoViewModel = koinViewModel()) {
    val date by viewModel.date.collectAsState()
    val entries by viewModel.entries.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val fat = entries.sumOf { it.fat }
    val protein = entries.sumOf { it.protein }
    val totalCarbs = entries.sumOf { it.totalCarbs }
    val fiber = entries.sumOf { it.fiber }
    val netCarbs = entries.sumOf { it.netCarbs }
    val sodium = entries.sumOf { it.sodium }
    val water = entries.sumOf { it.water }
    val calories = entries.sumOf { it.calories }

    Scaffold(
        modifier = modifier,
        topBar = { KetoDateBar(date = date, onPrev = viewModel::previousDay, onNext = viewModel::nextDay) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Log entry")
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item { KetoMacroRow(calories, netCarbs, fat, protein) }
            item { KetoGoalsSection(netCarbs, fat, protein, fiber, water, sodium) }
            item { SectionHeader("Log") }
            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("No entries yet. Tap + to log.", style = MaterialTheme.typography.bodyMedium) }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    KetoFoodItem(entry = entry, onDelete = { viewModel.delete(entry) })
                }
                item { KetoTotalsRow(fat, protein, totalCarbs, fiber, netCarbs, sodium, water) }
            }
        }
    }

    if (showAddDialog) {
        AddKetoEntryDialog(
            date = date,
            onDismiss = { showAddDialog = false },
            onConfirm = { entry -> viewModel.insert(entry); showAddDialog = false },
        )
    }
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
        navigationIcon = { IconButton(onClick = onPrev) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous day") } },
        actions = { IconButton(onClick = onNext) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next day") } },
    )
}

@Composable
private fun KetoMacroRow(calories: Double, netCarbs: Double, fat: Double, protein: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MacroCard(Modifier.weight(1f), "Calories", calories.toInt().toString(), "kcal", Color(0xFF1565C0))
        MacroCard(Modifier.weight(1f), "Net Carbs", netCarbs.fmt, "g", Color(0xFFC62828))
        MacroCard(Modifier.weight(1f), "Fat", fat.fmt, "g", Color(0xFF2E7D32))
        MacroCard(Modifier.weight(1f), "Protein", protein.fmt, "g", Color(0xFFF9A825))
    }
}

@Composable
private fun MacroCard(modifier: Modifier, label: String, value: String, unit: String, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(start = 2.dp))
            }
        }
    }
}

@Composable
private fun KetoGoalsSection(netCarbs: Double, fat: Double, protein: Double, fiber: Double, water: Double, sodium: Double) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("Progress Toward Goals", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
        GoalRow("Net Carbs", netCarbs, GOAL_NET_CARBS, "g", inverse = true)
        GoalRow("Fat", fat, GOAL_FAT, "g")
        GoalRow("Protein", protein, GOAL_PROTEIN, "g")
        GoalRow("Fiber", fiber, GOAL_FIBER, "g")
        GoalRow("Water", water, GOAL_WATER, "oz")
        GoalRow("Sodium", sodium, GOAL_SODIUM, "mg", sodiumMax = GOAL_SODIUM_MAX)
    }
}

@Composable
private fun GoalRow(label: String, current: Double, goal: Double, unit: String, inverse: Boolean = false, sodiumMax: Double? = null) {
    val progress = (current / goal).toFloat().coerceIn(0f, 1f)
    val color = when {
        sodiumMax != null -> if (current > sodiumMax) MaterialTheme.colorScheme.error else if (current >= goal) MaterialTheme.colorScheme.primary else Color(0xFFF9A825)
        inverse -> if (current <= goal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        else -> if (current >= goal) MaterialTheme.colorScheme.primary else Color(0xFFF9A825)
    }
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("${current.fmt} / ${goal.toInt()}$unit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 3.dp), color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KetoFoodItem(entry: KetoLogEntry, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }
    SwipeToDismissBox(state = dismissState, enableDismissFromStartToEnd = false, backgroundContent = { DeleteSwipeBackground() }) {
        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Fat ${entry.fat.fmt}g · Protein ${entry.protein.fmt}g",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text("${entry.netCarbs.fmt}g net", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFC62828))
            }
            Text(
                "Carbs ${entry.totalCarbs.fmt}g · Fiber ${entry.fiber.fmt}g",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
            )
            if (entry.sodium > 0 || entry.water > 0) {
                Text(
                    listOfNotNull(
                        "Sodium ${entry.sodium.fmt}mg".takeIf { entry.sodium > 0 },
                        "Water ${entry.water.fmt}oz".takeIf { entry.water > 0 },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun KetoTotalsRow(fat: Double, protein: Double, totalCarbs: Double, fiber: Double, netCarbs: Double, sodium: Double, water: Double) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text("Daily Totals", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("Fat ${fat.fmt}g · Protein ${protein.fmt}g · Carbs ${totalCarbs.fmt}g · Fiber ${fiber.fmt}g · Net Carbs ${netCarbs.fmt}g", style = MaterialTheme.typography.bodySmall)
        if (sodium > 0 || water > 0) {
            Text(
                listOfNotNull("Sodium ${sodium.fmt}mg".takeIf { sodium > 0 }, "Water ${water.fmt}oz".takeIf { water > 0 }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StepperRow(label: String, value: Double, step: Double, unit: String, onValueChange: (Double) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("$label ($unit)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onValueChange((value - step).coerceAtLeast(0.0)) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease $label")
            }
            Text(value.fmt, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 40.dp))
            IconButton(onClick = { onValueChange(value + step) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase $label")
            }
        }
    }
}

@Composable
private fun AddKetoEntryDialog(date: String, onDismiss: () -> Unit, onConfirm: (KetoLogEntry) -> Unit) {
    var fat by remember { mutableStateOf(0.0) }
    var protein by remember { mutableStateOf(0.0) }
    var carbs by remember { mutableStateOf(0.0) }
    var fiber by remember { mutableStateOf(0.0) }
    var sodium by remember { mutableStateOf(0.0) }
    var water by remember { mutableStateOf(0.0) }
    val netCarbs = (carbs - fiber).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Entry") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                StepperRow("Fat", fat, 1.0, "g") { fat = it }
                StepperRow("Protein", protein, 1.0, "g") { protein = it }
                StepperRow("Total Carbs", carbs, 1.0, "g") { carbs = it; if (fiber > it) { fiber = it } }
                StepperRow("Fiber", fiber, 1.0, "g") { fiber = it.coerceAtMost(carbs) }
                Text("Net Carbs: ${netCarbs.fmt}g", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828), modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                StepperRow("Sodium", sodium, 50.0, "mg") { sodium = it }
                StepperRow("Water", water, 1.0, "oz") { water = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                onConfirm(KetoLogEntry(date = date, foodName = time, fat = fat, protein = protein, totalCarbs = carbs, fiber = fiber, sodium = sodium, water = water))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
