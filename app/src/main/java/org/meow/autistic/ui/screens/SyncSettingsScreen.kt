package org.meow.autistic.ui.screens

import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.meow.autistic.data.backup.DriveBackupService
import androidx.work.WorkInfo
import androidx.work.WorkManager
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
    val scope = rememberCoroutineScope()
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
        headlineContent = { Text("Daily Reset") },
        supportingContent = { Text(status) },
        trailingContent = {
            Button(
                enabled = !isSyncing,
                onClick = { scope.launch { DailyResetWorker.forceReset(context) } },
            ) { Text("Run Now") }
        },
    )
}

/** Sync list item for the Google Tasks + Calendar sync worker. */
@Composable
internal fun TaskListSyncItem(isAuthenticated: Boolean) {
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
        !isAuthenticated -> "Sign in to sync"
        isSyncing -> "Syncing…"
        lastSync != null -> "Last synced: ${formatSyncTimestamp(lastSync!!)}"
        else -> "Never synced"
    }

    ListItem(
        headlineContent = { Text("Task List") },
        supportingContent = { Text(status) },
        trailingContent = {
            Button(enabled = isAuthenticated && !isSyncing, onClick = { scheduler.triggerImmediate() }) {
                Text("Sync")
            }
        },
    )
}

/** Sync list item for triggering a manual Google Drive database backup. */
@Composable
internal fun DriveBackupSyncItem(isAuthenticated: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupService: DriveBackupService = koinInject()

    var isRunning by remember { mutableStateOf(false) }
    val lastBackup by DriveBackupService.getLastBackupFlow(context)
        .collectAsState(initial = null)

    val status = when {
        !isAuthenticated -> "Sign in to back up"
        isRunning -> "Backing up…"
        lastBackup != null -> "Last backup: ${formatSyncTimestamp(lastBackup!!)}"
        else -> "Never backed up"
    }

    ListItem(
        headlineContent = { Text("Google Drive Backup") },
        supportingContent = { Text(status) },
        trailingContent = {
            Button(
                enabled = isAuthenticated && !isRunning,
                onClick = {
                    scope.launch {
                        isRunning = true
                        backupService.backupDatabase()
                        isRunning = false
                    }
                },
            ) { Text("Backup") }
        },
    )
}

internal fun formatSyncTimestamp(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
