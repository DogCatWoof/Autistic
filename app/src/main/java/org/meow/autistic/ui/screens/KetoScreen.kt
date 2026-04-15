package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
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
    val entry by viewModel.entry.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = { KetoDateBar(date = date, onPrev = viewModel::previousDay, onNext = viewModel::nextDay) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SectionLabel("Carbohydrates")
            CarbRow("Total Carbs", entry.totalCarbs, "g", indent = 0.dp) {
                viewModel.adjust(KetoField.TOTAL_CARBS, it)
            }
            CarbRow("Dietary Fiber", entry.fiber, "g", indent = 16.dp) {
                viewModel.adjust(KetoField.FIBER, it)
            }
            CarbRow("Total Sugars", entry.totalSugars, "g", indent = 16.dp) {
                viewModel.adjust(KetoField.TOTAL_SUGARS, it)
            }
            CarbRow("Added Sugars", entry.addedSugars, "g", indent = 32.dp) {
                viewModel.adjust(KetoField.ADDED_SUGARS, it)
            }
            CarbRow("Sugar Alcohols", entry.sugarAlcohols, "g", indent = 16.dp) {
                viewModel.adjust(KetoField.SUGAR_ALCOHOLS, it)
            }

            NetCarbsRow(netCarbs = entry.netCarbs)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

/** Value row with [−][+] steppers and optional left indent; used for carb sub-fields. */
@Composable
private fun CarbRow(
    label: String,
    value: Double,
    unit: String,
    indent: Dp,
    onAdjust: (Double) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = indent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onAdjust(-1.0) }) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease $label")
        }
        Text(
            "${value.fmt}$unit",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 52.dp),
        )
        IconButton(onClick = { onAdjust(1.0) }) {
            Icon(Icons.Default.Add, contentDescription = "Increase $label")
        }
    }
}

/** Read-only Net Carbs progress bar (derived; no steppers). Red when over goal. */
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
