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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sync settings sub-screen — lists all available sync sources.
 */
@Composable
fun SyncSettingsScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        SettingsSectionLabel("Products")
        OpenFoodFactsItem()
    }
}

@Composable
private fun OpenFoodFactsItem() {
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
        lastSync != null -> {
            val formatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date(lastSync!!))
            "Last synced: $formatted"
        }
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
