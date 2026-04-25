package org.meow.autistic.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import org.koin.compose.koinInject
import org.meow.autistic.data.health.HealthSnapshotEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val DATE_COL = 76.dp
private val DATA_COL = 68.dp

private val headerFormatter = DateTimeFormatter.ofPattern("EEE M/d")

/** Full-page Health Connect screen: permission management and 7-day snapshot table. */
@Composable
fun HealthConnectScreen(modifier: Modifier = Modifier) {
    val viewModel: HealthConnectViewModel = koinInject()
    val sdkStatus by viewModel.sdkStatus.collectAsState()
    val grantedPermissions by viewModel.grantedPermissions.collectAsState()
    val recentSnapshots by viewModel.recentSnapshots.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedSnapshot by viewModel.selectedSnapshot.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ -> viewModel.checkPermissions() }

    LaunchedEffect(Unit) { viewModel.checkPermissions() }

    val allGranted = viewModel.requiredPermissions.all { it in grantedPermissions }

    selectedSnapshot?.let { snap ->
        HealthSnapshotDetailDialog(
            snapshot = snap,
            onDismiss = { viewModel.selectSnapshot(null) },
        )
    }

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
                        Text("Last 7 Days", style = MaterialTheme.typography.titleMedium)
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        } else {
                            IconButton(onClick = { viewModel.refreshSnapshot() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh today")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SnapshotTable(
                        snapshots = recentSnapshots,
                        onRowClick = { viewModel.selectSnapshot(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SnapshotTable(
    snapshots: List<HealthSnapshotEntity>,
    onRowClick: (HealthSnapshotEntity) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(12.dp)
        ) {
            TableHeaderRow()
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            if (snapshots.isEmpty()) {
                Text(
                    "No data yet — tap Refresh to read from Health Connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                snapshots.forEachIndexed { index, snapshot ->
                    TableDataRow(snapshot, onClick = { onRowClick(snapshot) })
                    if (index < snapshots.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TableCell("Date", DATE_COL, isHeader = true)
        TableCell("Steps", DATA_COL, isHeader = true)
        TableCell("Sleep", DATA_COL, isHeader = true)
        TableCell("HR", DATA_COL, isHeader = true)
        TableCell("Weight", DATA_COL, isHeader = true)
        TableCell("Cal", DATA_COL, isHeader = true)
        TableCell("Glucose", DATA_COL, isHeader = true)
    }
}

@Composable
private fun TableDataRow(snapshot: HealthSnapshotEntity, onClick: () -> Unit) {
    val date = runCatching { LocalDate.parse(snapshot.date).format(headerFormatter) }
        .getOrDefault(snapshot.date)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() },
    ) {
        TableCell(date, DATE_COL)
        TableCell(snapshot.steps?.toString() ?: "—", DATA_COL)
        TableCell(snapshot.sleepMinutes?.let { formatSleep(it) } ?: "—", DATA_COL)
        TableCell(snapshot.avgHeartRateBpm?.let { "${it.roundToInt()}" } ?: "—", DATA_COL)
        TableCell(snapshot.weightKg?.let { "${"%.1f".format(it)}" } ?: "—", DATA_COL)
        TableCell(snapshot.caloriesBurned?.let { "${it.roundToInt()}" } ?: "—", DATA_COL)
        TableCell(snapshot.bloodGlucoseMmol?.let { "${"%.1f".format(it)}" } ?: "—", DATA_COL)
    }
}

@Composable
private fun TableCell(text: String, width: Dp, isHeader: Boolean = false) {
    Box(modifier = Modifier.width(width), contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            style = if (isHeader) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
            maxLines = 1,
        )
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

private fun formatSleep(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
