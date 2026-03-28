package org.meow.autistic.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.meow.autistic.NavigationItem
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.auth.TokenStore
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
    var showSync by remember { mutableStateOf(false) }
    var showQueryLog by remember { mutableStateOf(false) }
    var showNavPrefs by remember { mutableStateOf(false) }

    BackHandler(enabled = showSync || showQueryLog || showNavPrefs) {
        when {
            showSync -> showSync = false
            showQueryLog -> showQueryLog = false
            showNavPrefs -> showNavPrefs = false
        }
    }

    when {
        showSync -> SyncSettingsScreen(modifier = modifier)
        showQueryLog -> QueryLogScreen(modifier = modifier)
        showNavPrefs -> NavPreferencesScreen(allItems = allNavItems, modifier = modifier)
        else -> SettingsMainList(
            onSyncClick = { showSync = true },
            onQueryLogClick = { showQueryLog = true },
            onNavPrefsClick = { showNavPrefs = true },
            modifier = modifier,
        )
    }
}

@Composable
private fun SettingsMainList(
    onSyncClick: () -> Unit,
    onQueryLogClick: () -> Unit,
    onNavPrefsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) { showNotification(context) }
    }

    Column(modifier = modifier.fillMaxSize()) {
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
            supportingContent = { Text("Open Food Facts") },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open sync settings",
                )
            },
            modifier = Modifier.clickable { onSyncClick() },
        )
        HorizontalDivider()
        SettingsSectionLabel("Diagnostics")
        ListItem(
            headlineContent = { Text("Query Log") },
            supportingContent = { Text("Recent database query timings") },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open query log",
                )
            },
            modifier = Modifier.clickable { onQueryLogClick() },
        )
    }
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
