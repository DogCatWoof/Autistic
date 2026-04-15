package org.meow.autistic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.meow.autistic.data.mood.MoodCheckInWorker
import org.meow.autistic.data.navigation.NavPreferencesStore
import org.meow.autistic.data.navigation.NavStateStore
import org.meow.autistic.data.task.DailyResetWorker
import org.meow.autistic.data.task.REMINDER_CHANNEL_ID
import org.meow.autistic.ui.screens.filterNavItems
import org.meow.autistic.ui.screens.navTitlesFrom
import org.meow.autistic.ui.screens.AppDrawerSheet
import org.meow.autistic.ui.screens.NavBottomSheet
import org.meow.autistic.ui.screens.KetoScreen
import org.meow.autistic.ui.screens.MoodScreen
import org.meow.autistic.ui.screens.NotesScreen
import org.meow.autistic.ui.screens.ScanScreen
import org.meow.autistic.ui.screens.SettingsScreen
import org.meow.autistic.ui.screens.TaskListScreen
import org.meow.autistic.ui.theme.AutisticTheme

data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val subItems: List<String> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    companion object {
        private val BOTTOM_ITEMS = listOf(
            NavigationItem(
                title = "Task",
                selectedIcon = Icons.Filled.Done,
                unselectedIcon = Icons.Outlined.Done,
            ),
            NavigationItem("Scan", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
            NavigationItem("Notes", Icons.Filled.Create, Icons.Outlined.Create),
            NavigationItem("Mood", Icons.Filled.Mood, Icons.Outlined.Mood),
            NavigationItem("Keto", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        DailyResetWorker.enqueue(this)
        MoodCheckInWorker.enqueue(this)
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
                val context = LocalContext.current
                val allNavTitles = remember { navTitlesFrom(BOTTOM_ITEMS) }
                val savedEnabled by NavPreferencesStore.getEnabledFlow(context)
                    .collectAsState(initial = null)
                val bottomItems = filterNavItems(BOTTOM_ITEMS, savedEnabled ?: allNavTitles)
                var currentDestination by rememberSaveable { mutableStateOf(initialDestination) }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var bottomSheetIndex by rememberSaveable { mutableStateOf<Int?>(null) }
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    GlobalErrorHandler.errors.collect { snackbarHostState.showSnackbar(it) }
                }

                LaunchedEffect(currentDestination) {
                    NavStateStore.saveDestination(context, currentDestination)
                }

                var showExitDialog by remember { mutableStateOf(false) }
                BackHandler { showExitDialog = true }
                if (showExitDialog) {
                    AlertDialog(
                        onDismissRequest = { showExitDialog = false },
                        title = { Text("Exit app?") },
                        text = { Text("Are you sure you want to exit?") },
                        confirmButton = {
                            TextButton(onClick = { finish() }) { Text("Exit") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
                        },
                    )
                }

                val activeNavTitle = currentDestination

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawerSheet(
                            items = bottomItems,
                            currentDestination = currentDestination,
                            showSettings = showSettings,
                            onItemSelect = { item ->
                                currentDestination = item.subItems.firstOrNull() ?: item.title
                                showSettings = false
                                scope.launch { drawerState.close() }
                            },
                            onSubItemSelect = { subItem ->
                                currentDestination = subItem
                                showSettings = false
                                scope.launch { drawerState.close() }
                            },
                            onClose = { scope.launch { drawerState.close() } },
                        )
                    },
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                },
                                title = {
                                    Text(text = if (showSettings) "Settings" else currentDestination)
                                },
                                actions = {
                                    IconButton(onClick = { showSettings = !showSettings }) {
                                        Icon(
                                            imageVector = if (showSettings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                            contentDescription = "Settings",
                                        )
                                    }
                                },
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                bottomItems.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        modifier = Modifier.semantics {
                                            testTag = "nav_tab_${item.title.lowercase()}"
                                        },
                                        selected = !showSettings && item.title == activeNavTitle,
                                        onClick = {
                                            if (item.subItems.isEmpty()) {
                                                currentDestination = item.title
                                                showSettings = false
                                            } else {
                                                bottomSheetIndex = index
                                            }
                                        },
                                        label = { Text(text = item.title) },
                                        icon = {
                                            Icon(
                                                imageVector = if (!showSettings && item.title == activeNavTitle) {
                                                    item.selectedIcon
                                                } else {
                                                    item.unselectedIcon
                                                },
                                                contentDescription = item.title,
                                            )
                                        },
                                    )
                                }
                            }
                        },
                    ) { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding)) {
                            if (showSettings) {
                                SettingsScreen(allNavItems = BOTTOM_ITEMS)
                            } else {
                                when (currentDestination) {
                                    "Task" -> TaskListScreen()
                                    "Scan" -> ScanScreen()
                                    "Notes" -> NotesScreen()
                                    "Mood" -> MoodScreen()
                                    "Keto" -> KetoScreen()
                                }
                            }
                        }
                    }
                }

                bottomSheetIndex?.let { tabIndex ->
                    NavBottomSheet(
                        item = bottomItems[tabIndex],
                        onSubItemSelected = { subItem ->
                            currentDestination = subItem
                            showSettings = false
                            bottomSheetIndex = null
                        },
                        onDismiss = { bottomSheetIndex = null },
                    )
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(
                NotificationChannel("test_channel", "Test Channel", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Test Channel Description" }
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(REMINDER_CHANNEL_ID, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Reminders for upcoming tasks" }
            )
        }
    }
}

fun showNotification(context: Context) {
    val intent = Intent(context, NotificationActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val builder = NotificationCompat.Builder(context, "test_channel")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("😊 How are you feeling?")
        .setContentText("Tap to log your mood.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
    notificationManager.notify(1, builder.build())
}
