package org.meow.autistic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.meow.autistic.GlobalErrorHandler
import org.meow.autistic.NavigationItem
import org.meow.autistic.data.firestore.FirestoreSyncService
import org.meow.autistic.data.navigation.NavPreferencesStore
import org.meow.autistic.data.navigation.NavStateStore
import org.meow.autistic.data.sync.IMMEDIATE_WORK_NAME

internal val BOTTOM_ITEMS = listOf(
    NavigationItem("Task", Icons.Filled.Done, Icons.Outlined.Done),
    NavigationItem("Scan", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
    NavigationItem("Notes", Icons.Filled.Create, Icons.Outlined.Create),
    NavigationItem("Mood", Icons.Filled.Mood, Icons.Outlined.Mood),
    NavigationItem("Food Log", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    NavigationItem("Talk", Icons.Filled.Chat, Icons.Filled.Chat),
    NavigationItem("Sequences", Icons.Filled.PlaylistPlay, Icons.Filled.PlaylistPlay),
)

/** Full app shell: drawer, top/bottom bars, and screen routing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    initialDestination: String,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val allNavTitles = remember { navTitlesFrom(BOTTOM_ITEMS) }
    val savedEnabled by NavPreferencesStore.getEnabledFlow(context).collectAsState(initial = null)
    val bottomItems = filterNavItems(BOTTOM_ITEMS, savedEnabled ?: allNavTitles)

    var currentDestination by rememberSaveable { mutableStateOf(initialDestination) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var bottomSheetIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        GlobalErrorHandler.errors.collect { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Indefinite) }
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
            confirmButton = { TextButton(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) { Text("Exit") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Cancel") } },
        )
    }

    val workManager = remember { androidx.work.WorkManager.getInstance(context) }
    val syncWorkInfos by workManager.getWorkInfosForUniqueWorkFlow(IMMEDIATE_WORK_NAME).collectAsState(initial = emptyList())
    val isSyncing = syncWorkInfos.firstOrNull()?.state == androidx.work.WorkInfo.State.RUNNING
    val firestoreSyncService = koinInject<FirestoreSyncService>()
    var hasPending by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasPending = firestoreSyncService.hasPendingItems() }

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
            modifier = modifier.fillMaxSize(),
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
                    title = { Text(if (showSettings) "Settings" else currentDestination) },
                    actions = {
                        if (!showSettings) {
                            val syncScope = rememberCoroutineScope()
                            IconButton(
                                onClick = { syncScope.launch { org.meow.autistic.data.sync.SyncScheduler(workManager).triggerImmediate() } },
                                enabled = !isSyncing,
                            ) {
                                Icon(
                                    imageVector = if (hasPending) Icons.Filled.Sync else Icons.Outlined.Sync,
                                    contentDescription = "Sync",
                                )
                            }
                        }
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
                            modifier = Modifier.semantics { testTag = "nav_tab_${item.title.lowercase()}" },
                            selected = !showSettings && item.title == currentDestination,
                            onClick = {
                                if (item.subItems.isEmpty()) {
                                    currentDestination = item.title
                                    showSettings = false
                                } else {
                                    bottomSheetIndex = index
                                }
                            },
                            label = { Text(item.title) },
                            icon = {
                                Icon(
                                    imageVector = if (!showSettings && item.title == currentDestination) item.selectedIcon else item.unselectedIcon,
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
                    SettingsScreen(allNavItems = BOTTOM_ITEMS, onSignedOut = onSignedOut)
                } else {
                    when (currentDestination) {
                        "Task" -> TaskListScreen()
                        "Scan" -> ScanScreen()
                        "Notes" -> NotesScreen()
                        "Mood" -> MoodScreen()
                        "Food Log" -> FoodLogScreen()
                        "Talk" -> ConversationScreen()
                        "Sequences" -> SequenceListScreen()
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
