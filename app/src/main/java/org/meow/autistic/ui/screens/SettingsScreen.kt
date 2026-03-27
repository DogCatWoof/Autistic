package org.meow.autistic.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.auth.TokenStore
import org.meow.autistic.data.product.OFF_SYNC_WORK_NAME
import org.meow.autistic.data.product.OpenFoodFactsWorker
import org.meow.autistic.showNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings screen — account connection and notification controls.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showNotification(context)
        }
    }

    val tokenStore = remember { TokenStore.create(context) }
    val authManager = remember { GoogleAuthManager(context, tokenStore) }
    var isAuthenticated by remember { mutableStateOf(authManager.isAuthenticated()) }
    var accountEmail by remember { mutableStateOf(tokenStore.getAccountEmail()) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (authManager.handleSignInResult(result.data)) {
            isAuthenticated = true
            accountEmail = tokenStore.getAccountEmail()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isAuthenticated) {
            Text(
                text = "Connected to Google:",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = accountEmail ?: "unknown",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(text = "Not connected to Google")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { signInLauncher.launch(authManager.getSignInIntent()) }) {
                Text(text = "Connect Google Account")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    showNotification(context)
                } else {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                showNotification(context)
            }
        }) {
            Text(text = "Show Test Notification")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Sync", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        SyncProductsSection()
    }
}

@Composable
private fun SyncProductsSection() {
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
    val progress = if (isSyncing) info?.progress?.getString("status") else null

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Open Food Facts", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))

        if (isSyncing) {
            Text(
                text = progress ?: "Syncing…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            lastSync?.let { ts ->
                val formatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date(ts))
                Text(
                    text = "Last synced: $formatted",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            syncError?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Error: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            enabled = !isSyncing,
            onClick = {
                workManager.enqueueUniqueWork(
                    OFF_SYNC_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<OpenFoodFactsWorker>().build()
                )
            }
        ) {
            Text(text = if (syncError != null) "Retry Sync" else "Sync Products")
        }
    }
}
