package org.meow.autistic.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val NAV_DATE_FMT = DateTimeFormatter.ofPattern("EEE MMM d")
private const val CALORIE_GOAL = 2000

@Composable
fun IntegratedHealthScreen(modifier: Modifier = Modifier) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showWeekly by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    if (showWeekly) {
        WeeklyHealthDialog(endDate = selectedDate, onDismiss = { showWeekly = false })
    }

    Column(modifier = modifier.fillMaxSize()) {
        DayNavigator(
            date = selectedDate,
            isToday = selectedDate == today,
            onBack = { selectedDate = selectedDate.minusDays(1) },
            onForward = { selectedDate = selectedDate.plusDays(1) },
            onDateClick = { showWeekly = true },
        )
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SummarySection(selectedDate) }
            item {
                Text(
                    text = "Records",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(mockRecordsFor(selectedDate)) { record ->
                HealthRecordRow(record)
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DayNavigator(
    date: LocalDate,
    isToday: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onDateClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Previous day")
        }
        Text(
            text = if (isToday) "Today — ${date.format(NAV_DATE_FMT)}" else date.format(NAV_DATE_FMT),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onDateClick() },
        )
        IconButton(onClick = onForward, enabled = !isToday) {
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Next day")
        }
    }
}

@Composable
private fun SummarySection(date: LocalDate) {
    val mock = mockSnapshotFor(date)
    var weightDisplay by remember(date) { mutableStateOf(mock.weight) }
    var showWeightDialog by remember { mutableStateOf(false) }

    if (showWeightDialog) {
        WeightEntryDialog(
            current = weightDisplay,
            onConfirm = { weightDisplay = it; showWeightDialog = false },
            onDismiss = { showWeightDialog = false },
        )
    }

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            text = "Summary",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        CaloriesProgressCard(
            burned = mock.caloriesBurned,
            goal = CALORIE_GOAL,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Restaurant,
                iconTint = Color(0xFF6D4C41),
                label = "Carbs",
                primary = "${mock.carbs}g",
                secondary = null,
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Bloodtype,
                iconTint = Color(0xFFB71C1C),
                label = "Blood Sugar",
                primary = "${mock.bloodSugarLatest} mmol",
                secondary = "avg ${mock.bloodSugarAvg}",
            )
            WeightCard(
                modifier = Modifier.weight(1f),
                weight = weightDisplay,
                onEditClick = { showWeightDialog = true },
            )
        }
    }
}

@Composable
private fun CaloriesProgressCard(burned: Int, goal: Int, modifier: Modifier = Modifier) {
    val progress = (burned.toFloat() / goal).coerceIn(0f, 1f)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Calories Burned",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "%,d / %,d kcal".format(burned, goal),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE65100),
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(progress * 100).toInt()}% of daily goal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeightCard(modifier: Modifier = Modifier, weight: String, onEditClick: () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MonitorWeight,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(20.dp),
                )
                IconButton(onClick = onEditClick, modifier = Modifier.size(20.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit weight",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = weight,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Weight",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WeightEntryDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current.removeSuffix(" kg")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter weight") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.toDoubleOrNull() != null,
                onClick = { onConfirm("$text kg") },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    primary: String,
    secondary: String?,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HealthRecordRow(record: MockRecord) {
    ListItem(
        leadingContent = {
            Icon(record.icon, contentDescription = null, tint = record.tint, modifier = Modifier.size(24.dp))
        },
        headlineContent = { Text(record.label) },
        supportingContent = { Text(record.detail) },
        trailingContent = {
            Text(
                text = record.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun WeeklyHealthDialog(endDate: LocalDate, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Week ending ${endDate.format(DateTimeFormatter.ofPattern("MMM d"))}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ChartPlaceholder(label = "Calories burned — 7 day bar chart", color = Color(0xFFE65100))
                ChartPlaceholder(label = "Carbs — bar chart", color = Color(0xFF6D4C41))
                ChartPlaceholder(label = "Blood sugar — scatter", color = Color(0xFFB71C1C))
                ChartPlaceholder(label = "Weight — line chart", color = Color(0xFF2E7D32))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun ChartPlaceholder(label: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                ),
        ) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)) {
                val pts = listOf(0.6f, 0.4f, 0.7f, 0.3f, 0.8f, 0.5f, 0.65f)
                val step = size.width / (pts.size - 1)
                val path = Path()
                pts.forEachIndexed { i, v ->
                    val x = i * step
                    val y = size.height * (1f - v)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                pts.forEachIndexed { i, v ->
                    drawCircle(color, radius = 4.dp.toPx(), center = Offset(i * step, size.height * (1f - v)))
                }
            }
        }
    }
}

// ── Mock data ──────────────────────────────────────────────────────────────

private data class MockSnapshot(
    val caloriesBurned: Int,
    val bloodSugarLatest: String,
    val bloodSugarAvg: String,
    val carbs: Int,
    val weight: String,
)

private data class MockRecord(
    val icon: ImageVector,
    val tint: Color,
    val label: String,
    val detail: String,
    val time: String,
)

private fun mockSnapshotFor(date: LocalDate): MockSnapshot {
    val seed = date.dayOfYear
    return MockSnapshot(
        caloriesBurned = 1600 + (seed * 53 % 600),
        bloodSugarLatest = "5.${seed % 9}",
        bloodSugarAvg = "5.${(seed + 2) % 9}",
        carbs = 120 + (seed * 17 % 80),
        weight = "%.1f kg".format(74.0 + (seed % 10) * 0.3),
    )
}

private fun mockRecordsFor(date: LocalDate): List<MockRecord> {
    val seed = date.dayOfYear
    return listOf(
        MockRecord(Icons.Default.LocalFireDepartment, Color(0xFFE65100), "Calories burned", "${1600 + seed * 53 % 600} kcal", "11:59 PM"),
        MockRecord(Icons.Default.Restaurant, Color(0xFF6D4C41), "Carbs", "${60 + seed * 7 % 40}g — lunch", "12:30 PM"),
        MockRecord(Icons.Default.Restaurant, Color(0xFF6D4C41), "Carbs", "${30 + seed * 5 % 30}g — breakfast", "8:00 AM"),
        MockRecord(Icons.Default.Bloodtype, Color(0xFFB71C1C), "Blood glucose", "5.${seed % 9} mmol/L", "1:00 PM"),
        MockRecord(Icons.Default.Bloodtype, Color(0xFFB71C1C), "Blood glucose", "6.${(seed + 3) % 9} mmol/L", "8:00 AM"),
        MockRecord(Icons.Default.MonitorWeight, Color(0xFF2E7D32), "Weight", "%.1f kg".format(74.0 + (seed % 10) * 0.3), "7:15 AM"),
    )
}
