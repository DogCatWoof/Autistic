package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sync settings sub-screen — lists all available sync sources.
 */
@Composable
fun SyncSettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }
    val scheduler = remember { SyncScheduler(workManager) }

    val googleWorkInfos by workManager
        .getWorkInfosForUniqueWorkFlow(IMMEDIATE_WORK_NAME)
        .collectAsState(initial = emptyList())
    val googleLastSync by SyncWorker.getLastSyncFlow(context)
        .collectAsState(initial = null)

    val isGoogleSyncing = googleWorkInfos.firstOrNull()?.state == WorkInfo.State.RUNNING

    Column(modifier = modifier.fillMaxSize()) {
        SettingsSectionLabel("Google")
        GoogleSyncItem(
            label = "Tasks",
            isSyncing = isGoogleSyncing,
            lastSync = googleLastSync,
            onSync = { scheduler.triggerImmediate() },
        )
        GoogleSyncItem(
            label = "Calendar",
            isSyncing = isGoogleSyncing,
            lastSync = googleLastSync,
            onSync = { scheduler.triggerImmediate() },
        )
        SettingsSectionLabel("Products")
        OpenFoodFactsItem(workManager)
    }
}

@Composable
private fun GoogleSyncItem(
    label: String,
    isSyncing: Boolean,
    lastSync: Long?,
    onSync: () -> Unit,
) {
    val status = when {
        isSyncing -> "Syncing…"
        lastSync != null -> "Last synced: ${formatTimestamp(lastSync)}"
        else -> "Never synced"
    }
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(status) },
        trailingContent = {
            Button(enabled = !isSyncing, onClick = onSync) { Text("Sync") }
        },
    )
}

@Composable
private fun OpenFoodFactsItem(workManager: WorkManager) {
    val context = LocalContext.current

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
        lastSync != null -> "Last synced: ${formatTimestamp(lastSync!!)}"
        else -> "Never synced"
    }

    ListItem(
        headlineContent = { Text("Open Food Facts") },
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

private fun formatTimestamp(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
