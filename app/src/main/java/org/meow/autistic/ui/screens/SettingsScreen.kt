package org.meow.autistic.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.auth.TokenStore
import org.meow.autistic.showNotification

/**
 * Settings screen — account connection and notification controls.
 * Auth buttons are temporary; will move to ViewModel in Phase 7 (TODO #74).
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
            Button(onClick = { signInLauncher.launch(authManager.getSignInIntent()) }) {
                Text(text = "Connect Google Account")
            }
        }
    }
}
