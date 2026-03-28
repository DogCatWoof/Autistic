package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.core.context.GlobalContext
import org.meow.autistic.data.diagnostics.QueryEntry
import org.meow.autistic.data.diagnostics.QueryLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SLOW_THRESHOLD_MS = 50L

/**
 * Displays the in-memory [QueryLogger] entries. Entries with duration > [SLOW_THRESHOLD_MS]
 * are highlighted in error colour. Auto-updates as new entries arrive.
 */
@Composable
fun QueryLogScreen(modifier: Modifier = Modifier) {
    val logger = remember { GlobalContext.get().get<QueryLogger>() }
    val entries: List<QueryEntry> by logger.entries.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Query Log", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { logger.clear() }) { Text("Clear") }
        }
        HorizontalDivider()
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No queries recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn {
                items(entries) { entry -> QueryEntryItem(entry) }
            }
        }
    }
}

@Composable
private fun QueryEntryItem(entry: QueryEntry) {
    val isSlow = entry.durationMs > SLOW_THRESHOLD_MS
    val textColor = if (isSlow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = { Text(text = entry.label, color = textColor) },
        supportingContent = {
            Text(
                text = formatTimestamp(entry.timestamp),
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = {
            Text(
                text = "${entry.durationMs} ms",
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
            )
        },
    )
}

private fun formatTimestamp(ms: Long): String =
    SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(ms))
