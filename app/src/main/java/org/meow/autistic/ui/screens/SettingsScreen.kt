package org.meow.autistic.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import org.meow.autistic.data.conversation.TONE_NEUTRAL
import org.meow.autistic.data.conversation.TONES
import org.meow.autistic.data.conversation.TonePreferencesStore
import org.meow.autistic.data.debug.DebugSettings
import org.meow.autistic.data.mood.showMoodCheckInNotification

/**
 * Settings screen — Android-style list of settings categories.
 * Manages sub-screen navigation internally.
 */
@Composable
fun SettingsScreen(
    allNavItems: List<NavigationItem>,
    onSignedOut: () -> Unit = {},
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
            onSignedOut = onSignedOut,
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
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogcat by remember { mutableStateOf(false) }
    if (showLogcat) LogcatDialog(onDismiss = { showLogcat = false })
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager: GoogleAuthManager = koinInject()
    val tokenStore: TokenStore = koinInject()
    var accountEmail by remember { mutableStateOf(tokenStore.getAccountEmail()) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) { showMoodCheckInNotification(context) }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsSectionLabel("Account")
        ListItem(
            headlineContent = { Text("Google Account") },
            supportingContent = { Text(accountEmail ?: "Connected") },
            trailingContent = {
                TextButton(onClick = {
                    scope.launch {
                        authManager.signOut()
                        accountEmail = null
                        onSignedOut()
                    }
                }) { Text("Disconnect") }
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
                        showMoodCheckInNotification(context)
                    } else {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    showMoodCheckInNotification(context)
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

