package org.meow.autistic.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.meow.autistic.NavigationItem
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.auth.TokenStore
import org.meow.autistic.data.backup.DriveBackupService
import org.meow.autistic.data.backup.RestoreResult
import org.meow.autistic.data.conversation.TONE_NEUTRAL
import org.meow.autistic.data.conversation.TONES
import org.meow.autistic.data.conversation.TonePreferencesStore
import org.meow.autistic.data.debug.DebugSettings
import org.meow.autistic.showNotification

/**
 * Settings screen — Android-style list of settings categories.
 * Manages sub-screen navigation internally.
 */
@Composable
fun SettingsScreen(
    allNavItems: List<NavigationItem>,
    modifier: Modifier = Modifier,
) {
    var showQueryLog by remember { mutableStateOf(false) }
    var showNavPrefs by remember { mutableStateOf(false) }
    var showDailyTasks by remember { mutableStateOf(false) }
    var showSequences by remember { mutableStateOf(false) }

    BackHandler(enabled = showQueryLog || showNavPrefs || showDailyTasks || showSequences) {
        when {
            showQueryLog -> showQueryLog = false
            showNavPrefs -> showNavPrefs = false
            showDailyTasks -> showDailyTasks = false
            showSequences -> showSequences = false
        }
    }

    when {
        showQueryLog -> QueryLogScreen(modifier = modifier)
        showNavPrefs -> NavPreferencesScreen(allItems = allNavItems, modifier = modifier)
        showDailyTasks -> DailyTasksSettingsScreen(modifier = modifier)
        showSequences -> SequenceListScreen(modifier = modifier)
        else -> SettingsMainList(
            onQueryLogClick = { showQueryLog = true },
            onNavPrefsClick = { showNavPrefs = true },
            onDailyTasksClick = { showDailyTasks = true },
            onSequencesClick = { showSequences = true },
            modifier = modifier,
        )
    }
}

@Composable
private fun SettingsMainList(
    onQueryLogClick: () -> Unit,
    onNavPrefsClick: () -> Unit,
    onDailyTasksClick: () -> Unit,
    onSequencesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogcat by remember { mutableStateOf(false) }
    if (showLogcat) LogcatDialog(onDismiss = { showLogcat = false })
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenStore = remember { TokenStore.create(context) }
    val authManager = remember { GoogleAuthManager(context, tokenStore) }
    val backupService: DriveBackupService = koinInject()
    var isAuthenticated by remember { mutableStateOf(authManager.isAuthenticated()) }
    var accountEmail by remember { mutableStateOf(tokenStore.getAccountEmail()) }
    var syncExpanded by remember { mutableStateOf(false) }
    var showRestorePrompt by remember { mutableStateOf(false) }
    var showRestartPrompt by remember { mutableStateOf(false) }
    var restoreError by remember { mutableStateOf("") }
    var showRestoreError by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (authManager.handleSignInResult(result.data)) {
            isAuthenticated = true
            accountEmail = tokenStore.getAccountEmail()
            // Check for a Drive backup and prompt restore on fresh install.
            scope.launch {
                if (backupService.hasRemoteBackup()) {
                    showRestorePrompt = true
                }
            }
        }
    }

    if (showRestorePrompt) {
        AlertDialog(
            onDismissRequest = { showRestorePrompt = false },
            title = { Text("Restore backup?") },
            text = { Text("A backup was found on Google Drive. Would you like to restore your data now?") },
            confirmButton = {
                TextButton(
                    enabled = !isRestoring,
                    onClick = {
                        showRestorePrompt = false
                        scope.launch {
                            isRestoring = true
                            when (val result = backupService.restoreDatabase()) {
                                RestoreResult.Success -> showRestartPrompt = true
                                RestoreResult.NotFound -> { /* dismissed race condition */ }
                                RestoreResult.DecryptionFailed -> {
                                    restoreError = "Backup was created on a different device and cannot be decrypted here."
                                    showRestoreError = true
                                }
                                is RestoreResult.Error -> {
                                    restoreError = result.message
                                    showRestoreError = true
                                }
                            }
                            isRestoring = false
                        }
                    },
                ) { Text(if (isRestoring) "Restoring…" else "Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestorePrompt = false }) { Text("Skip") }
            },
        )
    }

    if (showRestartPrompt) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Restore complete") },
            text = { Text("Your data has been restored. Tap Restart to reload the app.") },
            confirmButton = {
                TextButton(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) {
                    Text("Restart")
                }
            },
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

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) { showNotification(context) }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsSectionLabel("Account")
        ListItem(
            headlineContent = { Text("Google Account") },
            supportingContent = {
                Text(if (isAuthenticated) accountEmail ?: "Connected" else "Not connected")
            },
            trailingContent = {
                if (isAuthenticated) {
                    TextButton(onClick = {
                        scope.launch {
                            authManager.signOut()
                            isAuthenticated = false
                            accountEmail = null
                        }
                    }) { Text("Disconnect Google") }
                } else {
                    TextButton(
                        onClick = { signInLauncher.launch(authManager.getSignInIntent()) }
                    ) { Text("Connect Google Account") }
                }
            },
        )
        HorizontalDivider()
        SettingsSectionLabel("Notifications")
        ListItem(
            headlineContent = { Text("Test Notification") },
            supportingContent = { Text("Send a test notification to this device") },
            modifier = Modifier.clickable {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        showNotification(context)
                    } else {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    showNotification(context)
                }
            },
        )
        HorizontalDivider()
        SettingsSectionLabel("Tasks")
        ListItem(
            headlineContent = { Text("Daily Tasks") },
            supportingContent = { Text("Recurring tasks added to your list every day") },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open daily tasks",
                )
            },
            modifier = Modifier.clickable { onDailyTasksClick() },
        )
        ListItem(
            headlineContent = { Text("Sequences") },
            supportingContent = { Text("Ordered step checklists for repeating tasks") },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open sequences",
                )
            },
            modifier = Modifier.clickable { onSequencesClick() },
        )
        HorizontalDivider()
        ConversationToneItem()
        HorizontalDivider()
        SettingsSectionLabel("Navigation")
        ListItem(
            headlineContent = { Text("Bottom Navigation") },
            supportingContent = { Text("Choose which items appear in the navigation bar") },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open navigation preferences",
                )
            },
            modifier = Modifier.clickable { onNavPrefsClick() },
        )
        HorizontalDivider()
        SettingsSectionLabel("Data")
        ListItem(
            headlineContent = { Text("Sync") },
            supportingContent = { if (!syncExpanded) Text("Daily reset, task list, backup") },
            trailingContent = {
                Icon(
                    if (syncExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (syncExpanded) "Collapse sync" else "Expand sync",
                )
            },
            modifier = Modifier.clickable { syncExpanded = !syncExpanded },
        )
        AnimatedVisibility(visible = syncExpanded) {
            Column {
                DailySyncItem()
                TaskListSyncItem(isAuthenticated = isAuthenticated)
                DriveBackupSyncItem(isAuthenticated = isAuthenticated)
            }
        }
        HorizontalDivider()
        SettingsSectionLabel("Diagnostics")
        DebugModeItem()
        ListItem(
            headlineContent = { Text("Query Log") },
            supportingContent = { Text("Recent database query timings") },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Open query log")
            },
            modifier = Modifier.clickable { onQueryLogClick() },
        )
        ListItem(
            headlineContent = { Text("Logcat") },
            supportingContent = { Text("Recent system log output") },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Show logcat")
            },
            modifier = Modifier.clickable { showLogcat = true },
        )
    }
}

@Composable
private fun LogcatDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var lines by remember { mutableStateOf("Loading…") }

    LaunchedEffect(Unit) {
        lines = withContext(Dispatchers.IO) {
            runCatching {
                Runtime.getRuntime()
                    .exec(arrayOf("logcat", "-d", "-t", "100"))
                    .inputStream
                    .bufferedReader()
                    .readText()
            }.getOrElse { e -> "Failed to read logcat: ${e.message}" }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Logcat") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = lines,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun DebugModeItem() {
    val debugSettings: DebugSettings = koinInject()
    var checked by remember { mutableStateOf(debugSettings.isDebugEnabled) }
    ListItem(
        headlineContent = { Text("Debug Mode") },
        supportingContent = { Text("Show exception toasts when errors occur") },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = { enabled ->
                    checked = enabled
                    debugSettings.isDebugEnabled = enabled
                },
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConversationToneItem() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedTone by TonePreferencesStore.getToneFlow(context).collectAsState(initial = TONE_NEUTRAL)
    SettingsSectionLabel("Conversation")
    ListItem(
        headlineContent = { Text("Response Tone") },
        supportingContent = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TONES.forEach { tone ->
                    FilterChip(
                        selected = tone == selectedTone,
                        onClick = { scope.launch { TonePreferencesStore.setTone(context, tone) } },
                        label = { Text(tone.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
        },
    )
}

/**
 * Section header label styled to match Android settings convention.
 */
@Composable
internal fun SettingsSectionLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}
