package org.meow.autistic.ui.screens

import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import org.meow.autistic.data.product.OFF_SYNC_WORK_NAME
import org.meow.autistic.data.product.OpenFoodFactsWorker
import org.meow.autistic.data.sync.IMMEDIATE_WORK_NAME
import org.meow.autistic.data.sync.SyncScheduler
import org.meow.autistic.data.sync.SyncWorker
import org.meow.autistic.data.task.DAILY_RESET_WORK_NAME
import org.meow.autistic.data.task.DailyResetWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Sync list item for the daily task reset worker. */
@Composable
internal fun DailySyncItem() {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }

    val workInfos by workManager
        .getWorkInfosForUniqueWorkFlow(DAILY_RESET_WORK_NAME)
        .collectAsState(initial = emptyList())
    val lastReset by DailyResetWorker.getLastResetFlow(context)
        .collectAsState(initial = null)

    val isSyncing = workInfos.firstOrNull()?.state == WorkInfo.State.RUNNING
    val status = when {
        isSyncing -> "Running…"
        lastReset != null -> "Last run: $lastReset"
        else -> "Never run"
    }

    ListItem(
        headlineContent = { Text("Daily Tasks") },
        supportingContent = { Text(status) },
        trailingContent = {
            Button(
                enabled = !isSyncing,
                onClick = { DailyResetWorker.enqueue(context) },
            ) { Text("Run") }
        },
    )
}

/** Sync list item for the Google Tasks + Calendar sync worker. */
@Composable
internal fun TaskListSyncItem() {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }
    val scheduler = remember { SyncScheduler(workManager) }

    val workInfos by workManager
        .getWorkInfosForUniqueWorkFlow(IMMEDIATE_WORK_NAME)
        .collectAsState(initial = emptyList())
    val lastSync by SyncWorker.getLastSyncFlow(context)
        .collectAsState(initial = null)

    val isSyncing = workInfos.firstOrNull()?.state == WorkInfo.State.RUNNING
    val status = when {
        isSyncing -> "Syncing…"
        lastSync != null -> "Last synced: ${formatSyncTimestamp(lastSync!!)}"
        else -> "Never synced"
    }

    ListItem(
        headlineContent = { Text("Task List") },
        supportingContent = { Text(status) },
        trailingContent = {
            Button(enabled = !isSyncing, onClick = { scheduler.triggerImmediate() }) {
                Text("Sync")
            }
        },
    )
}

/** Sync list item for the Open Food Facts product database worker. */
@Composable
internal fun ProductSyncItem() {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }

    val workInfos by workManager
        .getWorkInfosForUniqueWorkFlow(OFF_SYNC_WORK_NAME)
        .collectAsState(initial = emptyList())
    val lastSync by OpenFoodFactsWorker.getLastSyncFlow(context)
        .collectAsState(initial = null)

    val info = workInfos.firstOrNull()
    val isSyncing = info?.state == WorkInfo.State.RUNNING
    val syncError = if (info?.state == WorkInfo.State.FAILED) {
        info.outputData.getString("error") ?: "Unknown error"
    } else null

    val status = when {
        isSyncing -> info?.progress?.getString("status") ?: "Syncing…"
        syncError != null -> "Error: $syncError"
        lastSync != null -> "Last synced: ${formatSyncTimestamp(lastSync!!)}"
        else -> "Never synced"
    }

    ListItem(
        headlineContent = { Text("Product Data") },
        supportingContent = { Text(status) },
        trailingContent = {
            Button(
                enabled = !isSyncing,
                onClick = {
                    workManager.enqueueUniqueWork(
                        OFF_SYNC_WORK_NAME,
                        ExistingWorkPolicy.KEEP,
                        OneTimeWorkRequestBuilder<OpenFoodFactsWorker>().build(),
                    )
                },
            ) {
                Text(if (syncError != null) "Retry" else "Sync")
            }
        },
    )
}

internal fun formatSyncTimestamp(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
