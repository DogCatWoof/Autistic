package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.meow.autistic.data.health.HealthSnapshotEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private val titleFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
private val updatedFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a").withZone(ZoneId.systemDefault())

/** Detail dialog for a single [HealthSnapshotEntity] row. */
@Composable
fun HealthSnapshotDetailDialog(
    snapshot: HealthSnapshotEntity,
    onDismiss: () -> Unit,
) {
    val dateLabel = runCatching {
        LocalDate.parse(snapshot.date).format(titleFormatter)
    }.getOrDefault(snapshot.date)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dateLabel) },
        text = {
            Column {
                DetailRow("Steps", snapshot.steps?.toString() ?: "—")
                DetailRow("Sleep", snapshot.sleepMinutes?.let { formatDetailSleep(it) } ?: "—")
                DetailRow("Heart Rate", snapshot.avgHeartRateBpm?.let { "${it.roundToInt()} bpm" } ?: "—")
                DetailRow("Weight", snapshot.weightKg?.let { "${"%.1f".format(it * 2.20462)} lbs" } ?: "—")
                DetailRow("Calories", snapshot.caloriesBurned?.let { "${it.roundToInt()} Cal" } ?: "—")
                DetailRow("Blood Glucose", snapshot.bloodGlucoseMmol?.let { "${"%.1f".format(it)} mmol/L" } ?: "—")
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text(
                    "Updated: ${updatedFormatter.format(snapshot.lastUpdatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatDetailSleep(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
