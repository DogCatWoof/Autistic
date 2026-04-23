package org.meow.autistic.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import org.koin.compose.koinInject
import org.meow.autistic.data.health.HealthSnapshotEntity
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Settings sub-screen for managing Health Connect permissions and viewing today's data. */
@Composable
fun HealthConnectSettingsScreen(modifier: Modifier = Modifier) {
    val viewModel: HealthConnectViewModel = koinInject()
    val sdkStatus by viewModel.sdkStatus.collectAsState()
    val grantedPermissions by viewModel.grantedPermissions.collectAsState()
    val todaySnapshot by viewModel.todaySnapshot.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ -> viewModel.checkPermissions() }

    LaunchedEffect(Unit) { viewModel.checkPermissions() }

    val allGranted = viewModel.requiredPermissions.all { it in grantedPermissions }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Health Connect", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        when (sdkStatus) {
            HealthConnectClient.SDK_UNAVAILABLE -> UnavailableCard()
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> UpdateRequiredCard()
            else -> {
                PermissionsCard(
                    allGranted = allGranted,
                    onGrant = { permissionLauncher.launch(viewModel.requiredPermissions) },
                )
                Spacer(Modifier.height(16.dp))
                if (allGranted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Today's Data", style = MaterialTheme.typography.titleMedium)
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        } else {
                            IconButton(onClick = { viewModel.refreshSnapshot() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SnapshotCard(snapshot = todaySnapshot)
                }
            }
        }
    }
}

@Composable
private fun UnavailableCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                "Health Connect is not available on this device.",
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun UpdateRequiredCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Text(
                "Health Connect needs to be updated or installed from the Play Store.",
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun PermissionsCard(allGranted: Boolean, onGrant: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (allGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (allGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    if (allGranted) "Permissions granted" else "Permissions required",
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (!allGranted) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Steps, sleep, heart rate, weight, calories, and blood glucose.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onGrant) { Text("Grant Permissions") }
            }
        }
    }
}

@Composable
private fun SnapshotCard(snapshot: HealthSnapshotEntity?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (snapshot == null) {
                Text("No data yet — tap Refresh to read from Health Connect.")
            } else {
                val updated = snapshot.lastUpdatedAt
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("h:mm a"))
                Text(
                    "Last updated $updated",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                SnapshotRow("Steps", snapshot.steps?.toString())
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                SnapshotRow("Sleep", snapshot.sleepMinutes?.let { formatSleep(it) })
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                SnapshotRow("Avg Heart Rate", snapshot.avgHeartRateBpm?.let { "${it.roundToInt()} bpm" })
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                SnapshotRow("Weight", snapshot.weightKg?.let { "${"%.1f".format(it)} kg" })
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                SnapshotRow("Calories Burned", snapshot.caloriesBurned?.let { "${it.roundToInt()} kcal" })
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                SnapshotRow("Blood Glucose", snapshot.bloodGlucoseMmol?.let { "${"%.1f".format(it)} mmol/L" })
            }
        }
    }
}

@Composable
private fun SnapshotRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatSleep(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
