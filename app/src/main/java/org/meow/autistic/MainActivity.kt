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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.launch
import org.meow.autistic.ui.screens.AppDrawerSheet
import org.meow.autistic.ui.screens.DailyScreen
import org.meow.autistic.ui.screens.EventsScreen
import org.meow.autistic.ui.screens.MoodScreen
import org.meow.autistic.ui.screens.NavBottomSheet
import org.meow.autistic.ui.screens.NotesScreen
import org.meow.autistic.ui.screens.SettingsScreen
import org.meow.autistic.ui.screens.TodoListScreen
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
            NavigationItem("Todo", Icons.Filled.Done, Icons.Outlined.Done),
            NavigationItem(
                title = "Events",
                selectedIcon = Icons.Filled.DateRange,
                unselectedIcon = Icons.Outlined.DateRange,
                subItems = listOf("Events", "Daily"),
            ),
            NavigationItem("Mood", Icons.Filled.Face, Icons.Outlined.Face),
            NavigationItem("Notes", Icons.Filled.Create, Icons.Outlined.Create),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent {
            AutisticTheme {
                val bottomItems = BOTTOM_ITEMS
                // currentDestination can be any screen name, including "Daily" which has no tab
                var currentDestination by rememberSaveable { mutableStateOf("Todo") }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var bottomSheetIndex by rememberSaveable { mutableStateOf<Int?>(null) }
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                // "Daily" lives under "Events" in the nav bar
                val activeNavTitle = if (currentDestination == "Daily") "Events" else currentDestination

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawerSheet(
                            items = bottomItems,
                            selectedIndex = bottomItems.indexOfFirst { it.title == activeNavTitle },
                            showSettings = showSettings,
                            onSelect = { index ->
                                currentDestination = bottomItems[index].title
                                showSettings = false
                                scope.launch { drawerState.close() }
                            },
                        )
                    },
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
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
                                SettingsScreen()
                            } else {
                                when (currentDestination) {
                                    "Todo" -> TodoListScreen()
                                    "Daily" -> DailyScreen()
                                    "Events" -> EventsScreen()
                                    "Mood" -> MoodScreen()
                                    "Notes" -> NotesScreen()
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
            val channel = NotificationChannel(
                "test_channel",
                "Test Channel",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Test Channel Description" }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

fun showNotification(context: Context) {
    val intent = Intent(context, NotificationActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val builder = NotificationCompat.Builder(context, "test_channel")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Test Notification")
        .setContentText("This is a test notification.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
    notificationManager.notify(1, builder.build())
}
