package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.meow.autistic.data.energy.EnergyRepository

@Composable
internal fun StartOfDayDialog(
    onDismiss: () -> Unit,
    onConfirm: (sleepQuality: Int, stressLevel: Int, physicalState: Int) -> Unit,
) {
    var sleep by remember { mutableFloatStateOf(3f) }
    var stress by remember { mutableFloatStateOf(3f) }
    var physical by remember { mutableFloatStateOf(3f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How are you starting today?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WellnessSlider("Sleep quality", sleep, "Poor", "Great") { sleep = it }
                WellnessSlider("Stress level", stress, "Low", "High") { stress = it }
                WellnessSlider("Physical state", physical, "Unwell", "Great") { physical = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sleep.roundToSlider(), stress.roundToSlider(), physical.roundToSlider()) }) {
                Text("Set Baseline")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Skip") } },
    )
}

@Composable
private fun WellnessSlider(label: String, value: Float, lowLabel: String, highLabel: String, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${value.roundToSlider()}/5", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = 1f..5f, steps = 3)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(lowLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(highLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Float.roundToSlider(): Int = this.toInt().coerceIn(1, 5)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LogActivitySheet(
    onDismiss: () -> Unit,
    onLog: (activityType: String, description: String?, difficulty: Int?) -> Unit,
    onPreview: (activityType: String) -> Unit,
    projected: Double?,
    balance: Double,
) {
    val activityTypes = remember { EnergyRepository.DEFAULT_COSTS.keys.toList() }
    var selectedType by remember { mutableStateOf(activityTypes.first()) }
    var description by remember { mutableStateOf("") }
    var difficulty by remember { mutableIntStateOf(2) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Log Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        HorizontalDivider()

        Text("Activity type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            activityTypes.forEach { type ->
                FilterChip(
                    selected = type == selectedType,
                    onClick = { selectedType = type; onPreview(type) },
                    label = { Text(type) },
                )
            }
        }

        val cost = EnergyRepository.DEFAULT_COSTS[selectedType] ?: 2.0
        Text(
            "Cost: ${if (cost <= 0) "+${(-cost).let { if (it == kotlin.math.floor(it)) it.toInt().toString() else "%.1f".format(it) }}" else cost.let { if (it == kotlin.math.floor(it)) it.toInt().toString() else "%.1f".format(it) }} unit${if (kotlin.math.abs(cost) != 1.0) "s" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = if (cost <= 0) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (projected != null) {
            val projColor = if (projected >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Text(
                "After: ${projected.let { if (it == kotlin.math.floor(it)) it.toInt().toString() else "%.1f".format(it) }} units remaining",
                style = MaterialTheme.typography.bodySmall,
                color = projColor,
                fontWeight = FontWeight.Medium,
            )
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Text("How was it?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "Easier", 2 to "Normal", 3 to "Harder").forEach { (level, label) ->
                FilterChip(
                    selected = difficulty == level,
                    onClick = { difficulty = level },
                    label = { Text(label) },
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { onLog(selectedType, description.trim().takeIf { it.isNotEmpty() }, difficulty) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Log") }
    }
}
