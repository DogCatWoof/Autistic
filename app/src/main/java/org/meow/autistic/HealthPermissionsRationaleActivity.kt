package org.meow.autistic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.meow.autistic.ui.theme.AutisticTheme

/**
 * Required by Health Connect for Play Store distribution.
 * Handles the android.intent.action.VIEW_PERMISSION_USAGE intent
 * to show users why health permissions are needed.
 */
class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutisticTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Why Autistic uses Health Connect",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Autistic reads health data to give you a daily overview of your wellbeing alongside your tasks and calendar. The following data is used:",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                        listOf(
                            "Steps — daily activity level",
                            "Sleep — hours of sleep last night",
                            "Heart Rate — average bpm today",
                            "Weight — most recent recorded weight",
                            "Calories Burned — total active calories today",
                            "Blood Glucose — most recent reading today",
                        ).forEach { line ->
                            Text("• $line", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "All data stays on your device and is never uploaded to any server.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { finish() }) { Text("Close") }
                    }
                }
            }
        }
    }
}
