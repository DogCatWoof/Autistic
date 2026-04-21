package org.meow.autistic.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.meow.autistic.data.photo.ParsedNutritionData

private val Double.fmt: String
    get() = when {
        this >= 1000.0 || this == kotlin.math.floor(this) -> this.toInt().toString()
        else -> "%.1f".format(this)
    }

@Composable
internal fun PhotoPreviewOverlay(
    uri: Uri,
    onRetake: () -> Unit,
    onAcceptAsLabel: () -> Unit,
    onAcceptAsFood: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeStream(stream, null, opts)
        }?.asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) {},
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Captured photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) { Text("Retake") }
            Button(onClick = onAcceptAsLabel, modifier = Modifier.weight(1f)) { Text("Label") }
            Button(onClick = onAcceptAsFood, modifier = Modifier.weight(1f)) { Text("Food") }
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) { Text("Cancel", color = Color.White) }
    }
}

@Composable
internal fun NutritionLabelLoadingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Analyzing Label") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Reading nutrition information...")
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun NutritionLabelErrorDialog(onDismiss: () -> Unit, onManualEntry: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Could Not Parse Label") },
        text = { Text("The label couldn't be read automatically. Enter nutrition info manually?") },
        confirmButton = { TextButton(onClick = onManualEntry) { Text("Manual Entry") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Dismiss") } },
    )
}

@Composable
internal fun NutritionLabelServingsDialog(
    data: ParsedNutritionData,
    imagePath: String,
    onDismiss: () -> Unit,
    onConfirm: (servings: Double) -> Unit,
) {
    var servingsText by remember { mutableStateOf("1") }
    val servings by remember { derivedStateOf { servingsText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0 } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nutrition Label") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (data.servingSize != null) {
                    Text(
                        "Per serving: ${data.servingSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Servings", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = servingsText,
                        onValueChange = { servingsText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.width(80.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                NutritionPreviewRow("Calories", data.calories * servings, "kcal")
                NutritionPreviewRow("Protein", data.protein * servings, "g")
                NutritionPreviewRow("Total Fat", data.totalFat * servings, "g")
                NutritionPreviewRow("Total Carbs", data.totalCarbs * servings, "g")
                NutritionPreviewRow("Fiber", data.fiber * servings, "g")
                NutritionPreviewRow("Total Sugars", data.totalSugars * servings, "g")
                NutritionPreviewRow("Added Sugars", data.addedSugars * servings, "g")
                NutritionPreviewRow("Sugar Alcohols", data.sugarAlcohols * servings, "g")
                val netCarbs = ((data.totalCarbs - data.fiber - data.sugarAlcohols) * servings).coerceAtLeast(0.0)
                NutritionPreviewRow("Net Carbs", netCarbs, "g", highlight = true)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(servings) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NutritionPreviewRow(label: String, value: Double, unit: String, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${value.fmt}$unit",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
