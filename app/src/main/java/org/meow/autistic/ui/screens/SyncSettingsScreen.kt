package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.meow.autistic.data.backup.DriveBackupService
import org.meow.autistic.data.backup.RestoreResult
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

private enum class BackupUiState { Idle, BackingUp, Restoring }
private enum class RestoreDialog { None, Confirm, Restarting, Failed }

/** Sync list item for Google Drive backup and restore. */
@Composable
internal fun DriveBackupSyncItem(isAuthenticated: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupService: DriveBackupService = koinInject()

    var uiState by remember { mutableStateOf(BackupUiState.Idle) }
    var dialog by remember { mutableStateOf(RestoreDialog.None) }
    var errorMessage by remember { mutableStateOf("") }

    val lastBackup by DriveBackupService.getLastBackupFlow(context)
        .collectAsState(initial = null)

    val statusText = when {
        !isAuthenticated -> "Sign in to back up"
        uiState == BackupUiState.BackingUp -> "Backing up…"
        uiState == BackupUiState.Restoring -> "Restoring…"
        lastBackup != null -> "Last backup: ${formatSyncTimestamp(lastBackup!!)}"
        else -> "Never backed up"
    }

    ListItem(
        headlineContent = { Text("Google Drive Backup") },
        supportingContent = { Text(statusText) },
        trailingContent = {
            val busy = uiState != BackupUiState.Idle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = isAuthenticated && !busy,
                    onClick = {
                        scope.launch {
                            uiState = BackupUiState.BackingUp
                            backupService.backupDatabase()
                            uiState = BackupUiState.Idle
                        }
                    },
                ) { Text("Backup") }
                Button(
                    enabled = isAuthenticated && !busy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    onClick = { dialog = RestoreDialog.Confirm },
                ) { Text("Restore") }
            }
        },
    )

    when (dialog) {
        RestoreDialog.Confirm -> AlertDialog(
            onDismissRequest = { dialog = RestoreDialog.None },
            title = { Text("Restore from backup?") },
            text = { Text("This will replace all local data with your last Google Drive backup. The app will restart automatically.") },
            confirmButton = {
                TextButton(onClick = {
                    dialog = RestoreDialog.None
                    scope.launch {
                        uiState = BackupUiState.Restoring
                        when (val result = backupService.restoreDatabase()) {
                            RestoreResult.Success -> dialog = RestoreDialog.Restarting
                            RestoreResult.NotFound -> {
                                errorMessage = "No backup found on Google Drive."
                                dialog = RestoreDialog.Failed
                            }
                            RestoreResult.DecryptionFailed -> {
                                errorMessage = "Backup was created on a different device and cannot be decrypted here."
                                dialog = RestoreDialog.Failed
                            }
                            is RestoreResult.Error -> {
                                errorMessage = result.message
                                dialog = RestoreDialog.Failed
                            }
                        }
                        uiState = BackupUiState.Idle
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { dialog = RestoreDialog.None }) { Text("Cancel") }
            },
        )

        RestoreDialog.Restarting -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Restore complete") },
            text = { Text("Your data has been restored. Tap Restart to reload the app.") },
            confirmButton = {
                TextButton(onClick = {
                    android.os.Process.killProcess(android.os.Process.myPid())
                }) { Text("Restart") }
            },
            dismissButton = {},
        )

        RestoreDialog.Failed -> AlertDialog(
            onDismissRequest = { dialog = RestoreDialog.None },
            title = { Text("Restore failed") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { dialog = RestoreDialog.None }) { Text("OK") }
            },
        )

        RestoreDialog.None -> Unit
    }
}

internal fun formatSyncTimestamp(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
