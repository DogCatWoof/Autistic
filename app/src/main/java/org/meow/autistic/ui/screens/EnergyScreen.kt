package org.meow.autistic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.energy.EnergyLogEntry
import org.meow.autistic.data.energy.EnergyRepository
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyScreen(modifier: Modifier = Modifier, viewModel: EnergyViewModel = koinViewModel()) {
    val logs by viewModel.todayLogs.collectAsState()
    val balance by viewModel.todayBalance.collectAsState()
    val capacity by viewModel.todayCapacity.collectAsState()
    val startOfDay by viewModel.startOfDay.collectAsState()
    val projected by viewModel.projectedBalance.collectAsState()

    var showLogSheet by remember { mutableStateOf(false) }
    var showStartOfDayDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<EnergyLogEntry?>(null) }

    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove entry?") },
            text = { Text("Remove \"${entry.activityType}\" from today's log?") },
            confirmButton = { TextButton(onClick = { viewModel.deleteLog(entry); deleteTarget = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }

    if (showStartOfDayDialog) {
        StartOfDayDialog(
            onDismiss = { showStartOfDayDialog = false },
            onConfirm = { sleep, stress, physical ->
                viewModel.submitStartOfDay(sleep, stress, physical)
                showStartOfDayDialog = false
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showLogSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Log activity")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item { EnergyBudgetBar(balance = balance, capacity = capacity, modifier = Modifier.padding(16.dp)) }

            if (startOfDay == null) {
                item {
                    StartOfDayBanner(
                        onSetBaseline = { showStartOfDayDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            if (projected != null) {
                item {
                    ProjectionCard(projected = projected!!, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }

            item {
                Text(
                    "Today's Log",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                )
            }

            if (logs.isEmpty()) {
                item {
                    Text(
                        "No activities logged yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                    )
                }
            } else {
                items(logs, key = { it.id }) { entry ->
                    EnergyLogRow(entry = entry, onDelete = { deleteTarget = entry })
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }

    if (showLogSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showLogSheet = false; viewModel.clearProjection() }, sheetState = sheetState) {
            LogActivitySheet(
                onDismiss = { showLogSheet = false; viewModel.clearProjection() },
                onLog = { type, desc, diff -> viewModel.logActivity(type, desc, diff); showLogSheet = false; viewModel.clearProjection() },
                onPreview = { type -> viewModel.previewProjection(type) },
                projected = projected,
                balance = balance,
            )
        }
    }
}

@Composable
private fun EnergyBudgetBar(balance: Double, capacity: Double, modifier: Modifier = Modifier) {
    val fraction = if (capacity > 0) (balance / capacity).coerceIn(0.0, 1.0).toFloat() else 0f
    val barColor = when {
        fraction > 0.6f -> Color(0xFF4CAF50)
        fraction > 0.3f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Energy Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${balance.fmt} / ${capacity.fmt} units",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(barColor),
            )
        }
        Text(
            "${(fraction * 100).roundToInt()}% remaining",
            style = MaterialTheme.typography.bodySmall,
            color = barColor,
        )
    }
}

@Composable
private fun StartOfDayBanner(onSetBaseline: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                "Set today's baseline",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            TextButton(onClick = onSetBaseline) { Text("Start") }
        }
    }
}

@Composable
private fun ProjectionCard(projected: Double, modifier: Modifier = Modifier) {
    val color = if (projected >= 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val textColor = if (projected >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color)) {
        Text(
            "After this activity: ${projected.fmt} units remaining",
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

private val logTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

@Composable
private fun EnergyLogRow(entry: EnergyLogEntry, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.activityType, style = MaterialTheme.typography.bodyMedium)
            if (entry.description != null) {
                Text(entry.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                entry.loggedAt.atZone(ZoneId.systemDefault()).format(logTimeFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val costColor = if (entry.energyCost <= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
        Text(
            "${if (entry.energyCost > 0) "−" else "+"}${entry.energyCost.let { if (it < 0) (-it).fmt else it.fmt }}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = costColor,
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val Double.fmt: String get() = if (this == kotlin.math.floor(this)) this.toInt().toString() else "%.1f".format(this)
