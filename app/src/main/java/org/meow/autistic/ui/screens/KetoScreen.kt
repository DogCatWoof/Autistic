package org.meow.autistic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.keto.KetoItemEntry
import org.meow.autistic.data.keto.KetoLogEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

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
    var fabExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val fabRotation by animateFloatAsState(targetValue = if (fabExpanded) 45f else 0f, label = "fab_rotation")

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
        topBar = { FoodLogDateBar(date = date, onPrev = viewModel::previousDay, onNext = viewModel::nextDay) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = fabExpanded) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 16.dp),
                    ) {
                        SpeedDialItem("Photo") { fabExpanded = false }
                        SpeedDialItem("Manual") { fabExpanded = false; showAddDialog = true }
                    }
                }
                FloatingActionButton(onClick = { fabExpanded = !fabExpanded }) {
                    Icon(Icons.Default.Add, contentDescription = "New Entry", modifier = Modifier.rotate(fabRotation))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
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

        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable { fabExpanded = false },
            )
        }
    }
}

@Composable
private fun NutritionSummary(totals: KetoLogEntry) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NutritionSection("Calories")
        NutritionRow("Calories", totals.calories, "kcal", 0.dp)
        NutritionSection("Carbohydrates")
        NutritionRow("Total Carbs", totals.totalCarbs, "g", 0.dp)
        NutritionRow("Dietary Fiber", totals.fiber, "g", 16.dp)
        NutritionRow("Total Sugars", totals.totalSugars, "g", 16.dp)
        NutritionRow("Added Sugars", totals.addedSugars, "g", 32.dp)
        NutritionRow("Sugar Alcohols", totals.sugarAlcohols, "g", 16.dp)
        NutritionRow("Net Carbs", totals.netCarbs, "g", 16.dp)
    }
}

@Composable
private fun NutritionSection(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
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
            modifier = Modifier.widthIn(min = 60.dp),
        )
    }
}

@Composable
private fun EntryListItem(item: KetoItemEntry, onDelete: () -> Unit) {
    val timeLabel = item.loggedAt.atZone(ZoneId.systemDefault()).format(timeFormatter)
    val supporting = buildString {
        if (item.description != null) append("${item.description} · ")
        append("${item.calories.fmt} kcal · ${item.netCarbs.fmt}g net carbs")
    }
    ListItem(
        headlineContent = { Text(timeLabel) },
        supportingContent = { Text(supporting) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete entry")
            }
        },
    )
}

@Composable
private fun SpeedDialItem(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = MaterialTheme.shapes.small, tonalElevation = 6.dp, shadowElevation = 2.dp, onClick = onClick) {
            Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
        SmallFloatingActionButton(onClick = onClick) {
            Icon(Icons.Default.Add, contentDescription = label)
        }
    }
}

@Composable
private fun AddEntryDialog(onDismiss: () -> Unit, onConfirm: (KetoItemEntry) -> Unit) {
    var description by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var totalCarbs by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var totalSugars by remember { mutableStateOf("") }
    var addedSugars by remember { mutableStateOf("") }
    var sugarAlcohols by remember { mutableStateOf("") }

    val netCarbs by remember {
        derivedStateOf {
            val tc = totalCarbs.toDoubleOrNull() ?: 0.0
            val f = fiber.toDoubleOrNull() ?: 0.0
            val sa = sugarAlcohols.toDoubleOrNull() ?: 0.0
            (tc - f - sa).coerceAtLeast(0.0)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                DialogLabel("Calories")
                NutrientInputRow("Calories", calories, "kcal") { calories = it }
                DialogLabel("Carbohydrates")
                NutrientInputRow("Total Carbs", totalCarbs, "g") { totalCarbs = it }
                NutrientInputRow("Dietary Fiber", fiber, "g") { fiber = it }
                NutrientInputRow("Total Sugars", totalSugars, "g") { totalSugars = it }
                NutrientInputRow("Added Sugars", addedSugars, "g") { addedSugars = it }
                NutrientInputRow("Sugar Alcohols", sugarAlcohols, "g") { sugarAlcohols = it }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Net Carbs",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${netCarbs.fmt}g",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(KetoItemEntry(
                    date = "", loggedAt = Instant.now(),
                    description = description.trim().takeIf { it.isNotEmpty() },
                    calories = calories.toDoubleOrNull() ?: 0.0,
                    totalCarbs = totalCarbs.toDoubleOrNull() ?: 0.0,
                    fiber = fiber.toDoubleOrNull() ?: 0.0,
                    totalSugars = totalSugars.toDoubleOrNull() ?: 0.0,
                    addedSugars = addedSugars.toDoubleOrNull() ?: 0.0,
                    sugarAlcohols = sugarAlcohols.toDoubleOrNull() ?: 0.0,
                ))
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DialogLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun NutrientInputRow(label: String, value: String, unit: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            suffix = { Text(unit) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
            modifier = Modifier.width(96.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodLogDateBar(date: String, onPrev: () -> Unit, onNext: () -> Unit) {
    val today = LocalDate.now()
    val parsed = LocalDate.parse(date)
    val label = when (parsed) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
    TopAppBar(
        title = { Text("Food Log · $label") },
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
