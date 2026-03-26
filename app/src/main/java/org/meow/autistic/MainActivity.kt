package org.meow.autistic

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.auth.TokenStore
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.meow.autistic.ui.screens.DailyScreen
import org.meow.autistic.ui.screens.EventsScreen
import org.meow.autistic.ui.screens.MoodScreen
import org.meow.autistic.ui.screens.NotesScreen
import org.meow.autistic.ui.screens.TodoListScreen
import org.meow.autistic.ui.theme.AutisticTheme

data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    companion object {
        private val BOTTOM_ITEMS = listOf(
            NavigationItem(
                title = "Todo",
                selectedIcon = Icons.Filled.Done,
                unselectedIcon = Icons.Outlined.Done,
            ),
            NavigationItem(
                title = "Daily",
                selectedIcon = Icons.AutoMirrored.Filled.List,
                unselectedIcon = Icons.AutoMirrored.Outlined.List,
            ),
            NavigationItem(
                title = "Events",
                selectedIcon = Icons.Filled.DateRange,
                unselectedIcon = Icons.Outlined.DateRange,
            ),
            NavigationItem(
                title = "Mood",
                selectedIcon = Icons.Filled.Face,
                unselectedIcon = Icons.Outlined.Face,
            ),
            NavigationItem(
                title = "Notes",
                selectedIcon = Icons.Filled.Create,
                unselectedIcon = Icons.Outlined.Create,
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent {
            AutisticTheme {
                val bottomItems = BOTTOM_ITEMS

                var selectedBottomItemIndex by rememberSaveable {
                    mutableIntStateOf(0)
                }
                var showSettings by rememberSaveable {
                    mutableStateOf(false)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            title = {
                                val title = if (showSettings) {
                                    "Settings"
                                } else {
                                    bottomItems[selectedBottomItemIndex].title
                                }
                                Text(text = title)
                            },
                            actions = {
                                IconButton(onClick = {
                                    showSettings = !showSettings
                                }) {
                                    Icon(
                                        imageVector = if (showSettings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings"
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            bottomItems.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = !showSettings && index == selectedBottomItemIndex,
                                    onClick = {
                                        selectedBottomItemIndex = index
                                        showSettings = false
                                    },
                                    label = { Text(text = item.title) },
                                    icon = {
                                        Icon(
                                            imageVector = if (!showSettings && index == selectedBottomItemIndex) {
                                                item.selectedIcon
                                            } else {
                                                item.unselectedIcon
                                            },
                                            contentDescription = item.title
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        if (showSettings) {
                            Greeting(name = "Settings")
                        } else {
                            when (selectedBottomItemIndex) {
                                0 -> TodoListScreen()
                                1 -> DailyScreen()
                                2 -> EventsScreen()
                                3 -> MoodScreen()
                                4 -> NotesScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Test Channel"
            val descriptionText = "Test Channel Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("test_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showNotification(context)
        }
    }

    val tokenStore = remember { TokenStore.create(context) }
    val authManager = remember { GoogleAuthManager(context, tokenStore) }
    var isAuthenticated by remember { mutableStateOf(authManager.isAuthenticated()) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (authManager.handleSignInResult(result.data)) {
            isAuthenticated = true
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Hello $name!")
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
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
            Text(text = "Show Notification")
        }

        // TEMP: auth buttons — will be moved to ViewModel+UI in Phase 7 (TODO #74)
        if (isAuthenticated) {
            Text(text = "Connected: ${tokenStore.getAccountEmail() ?: "unknown"}")
            Button(onClick = {
                scope.launch {
                    authManager.signOut()
                    isAuthenticated = false
                }
            }) {
                Text(text = "Disconnect Google")
            }
        } else {
            Text(text = "Not connected to Google")
            Button(onClick = {
                signInLauncher.launch(authManager.getSignInIntent())
            }) {
                Text(text = "Connect Google Account")
            }
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AutisticTheme {
        Greeting("Android")
    }
}
