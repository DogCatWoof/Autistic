package org.meow.autistic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ScanQueue(
    queue: List<ScanQueueItem>,
    selectedBarcode: String?,
    onSelect: (String) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (queue.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Scan a barcode to look it up", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    LazyColumn(modifier = modifier) {
        items(queue, key = { it.barcode }) { item ->
            SwipeableQueueItem(
                item = item,
                isSelected = item.barcode == selectedBarcode,
                onSelect = { onSelect(item.barcode) },
                onDismiss = { onDismiss(item.barcode) },
            )
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableQueueItem(
    item: ScanQueueItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            onDismiss()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.requireHorizontalSwipe(),
        backgroundContent = { DeleteSwipeBackground(contentDescription = "Remove") },
    ) {
        val containerColor = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
        ListItem(
            headlineContent = { Text(item.barcode) },
            supportingContent = { Text(item.status.label()) },
            colors = ListItemDefaults.colors(containerColor = containerColor),
            modifier = Modifier.clickable(onClick = onSelect),
        )
    }
}

internal fun ScanStatus.label(): String = when (this) {
    is ScanStatus.Pending -> "Queued"
    is ScanStatus.Loading -> "Looking up\u2026"
    is ScanStatus.Found -> "Found: ${product.name}"
    is ScanStatus.NotFound -> "Not found"
    is ScanStatus.NetworkError -> "Waiting for network\u2026"
}
