package org.meow.autistic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.backup.DriveBackupService
import org.meow.autistic.data.backup.RestoreResult
import org.meow.autistic.data.mood.MoodCheckInWorker
import org.meow.autistic.data.navigation.NavStateStore
import org.meow.autistic.data.task.DailyResetWorker
import org.meow.autistic.core.notifications.registerNotificationChannels
import org.meow.autistic.data.sync.SyncScheduler
import org.meow.autistic.ui.screens.AuthScreen
import org.meow.autistic.ui.screens.MainScaffold
import org.meow.autistic.ui.theme.AutisticTheme

data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val subItems: List<String> = emptyList(),
)

class MainActivity : ComponentActivity() {

    private val authManager: GoogleAuthManager by inject()
    private val backupService: DriveBackupService by inject()

    private var isAuthenticated by mutableStateOf(false)
    private var showRestorePrompt by mutableStateOf(false)
    private var showRestartPrompt by mutableStateOf(false)
    private var restoreError by mutableStateOf("")
    private var showRestoreError by mutableStateOf(false)

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (authManager.handleSignInResult(result.data)) {
            isAuthenticated = true
            lifecycleScope.launch {
                if (backupService.hasRemoteBackup()) {
                    showRestorePrompt = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerNotificationChannels(this)
        DailyResetWorker.enqueue(this)
        MoodCheckInWorker.enqueue(this)

        val syncScheduler = SyncScheduler(androidx.work.WorkManager.getInstance(this))
        syncScheduler.schedulePeriodicSync()
        isAuthenticated = authManager.isAuthenticated()
        if (isAuthenticated) syncScheduler.triggerImmediate()

        val savedDestination = runBlocking { NavStateStore.getDestinationFlow(this@MainActivity).first() }
        val initialDestination = when (intent?.action) {
            "org.meow.autistic.OPEN_SCAN"  -> "Scan"
            "org.meow.autistic.OPEN_TASKS" -> "Task"
            "org.meow.autistic.OPEN_NOTES" -> "Notes"
            "org.meow.autistic.OPEN_MOOD"  -> "Mood"
            else -> savedDestination ?: "Task"
        }

        setContent {
            AutisticTheme {
                if (!isAuthenticated) {
                    AuthScreen(onSignInClick = { signInLauncher.launch(authManager.getSignInIntent()) })
                    return@AutisticTheme
                }

                if (showRestorePrompt) {
                    AlertDialog(
                        onDismissRequest = { showRestorePrompt = false },
                        title = { Text("Restore backup?") },
                        text = { Text("A backup was found on Google Drive. Would you like to restore your data now?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showRestorePrompt = false
                                lifecycleScope.launch {
                                    when (val result = backupService.restoreDatabase()) {
                                        RestoreResult.Success -> showRestartPrompt = true
                                        RestoreResult.NotFound -> { }
                                        RestoreResult.DecryptionFailed -> {
                                            restoreError = "Backup was created on a different device and cannot be decrypted here."
                                            showRestoreError = true
                                        }
                                        is RestoreResult.Error -> {
                                            restoreError = result.message
                                            showRestoreError = true
                                        }
                                    }
                                }
                            }) { Text("Restore") }
                        },
                        dismissButton = { TextButton(onClick = { showRestorePrompt = false }) { Text("Skip") } },
                    )
                }

                if (showRestartPrompt) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Restore complete") },
                        text = { Text("Your data has been restored. Tap Restart to reload the app.") },
                        confirmButton = { TextButton(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) { Text("Restart") } },
                        dismissButton = {},
                    )
                }

                if (showRestoreError) {
                    AlertDialog(
                        onDismissRequest = { showRestoreError = false },
                        title = { Text("Restore failed") },
                        text = { Text(restoreError) },
                        confirmButton = { TextButton(onClick = { showRestoreError = false }) { Text("OK") } },
                    )
                }

                MainScaffold(
                    initialDestination = initialDestination,
                    onSignedOut = { isAuthenticated = false },
                )
            }
        }
    }
}
