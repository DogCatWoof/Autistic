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
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.auth.TokenStore
import org.meow.autistic.showNotification

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
    }
}
