package org.meow.autistic

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

    private var isAuthenticated by mutableStateOf(false)

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
                    AuthScreen(onSignInClick = {
                        lifecycleScope.launch {
                            try {
                                if (authManager.signIn(this@MainActivity)) {
                                    isAuthenticated = true
                                    syncScheduler.triggerImmediate()
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Sign-in failed", e)
                            }
                        }
                    })
                    return@AutisticTheme
                }

                MainScaffold(
                    initialDestination = initialDestination,
                    onSignedOut = { isAuthenticated = false },
                )
            }
        }
    }
}
