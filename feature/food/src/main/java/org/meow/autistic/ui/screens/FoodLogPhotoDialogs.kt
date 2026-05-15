package org.meow.autistic.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.meow.autistic.data.foodlog.FoodLogItemEntry
import org.meow.autistic.data.photo.ParsedNutritionData
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Double.fmt: String
    get() = when {
        this >= 1000.0 || this == kotlin.math.floor(this) -> this.toInt().toString()
        else -> "%.1f".format(this)
    }

internal enum class PhotoPreviewMode { Default, NutritionOnly }

@Composable
internal fun PhotoPreviewOverlay(
    uri: Uri,
    mode: PhotoPreviewMode = PhotoPreviewMode.Default,
    onRetake: () -> Unit,
    onAccept: () -> Unit = {},
    onAcceptAsNutrition: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        val degrees = context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = 2 })
        } ?: return@remember null
        if (degrees == 0f) {
            raw.asImageBitmap()
        } else {
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(degrees) }, true)
                .also { raw.recycle() }
                .asImageBitmap()
        }
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
            if (mode == PhotoPreviewMode.NutritionOnly) {
                Button(onClick = onAcceptAsNutrition, modifier = Modifier.weight(2f)) { Text("Use as Nutrition Label") }
            } else {
                Button(onClick = onAccept, modifier = Modifier.weight(2f)) { Text("Analyze") }
            }
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) { Text("Cancel", color = Color.White) }
    }
}

@Composable
internal fun PhotoAnalysisLoadingDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Please Wait") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(message)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun LabelNeedNutritionPhotoDialog(
    foodName: String,
    onTakePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (foodName.isNotBlank()) foodName else "Scan Nutrition Facts") },
        text = {
            Text(
                if (foodName.isNotBlank()) {
                    "\"$foodName\" isn't in the cache yet. Photograph the nutrition facts label to record its values."
                } else {
                    "Photograph the nutrition facts label to record this food's values."
                }
            )
        },
        confirmButton = { TextButton(onClick = onTakePhoto) { Text("Take Photo") } },
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
    onDismiss: () -> Unit,
    onConfirm: (data: ParsedNutritionData, servings: Double) -> Unit,
) {
    var nameText by remember { mutableStateOf(data.description ?: "") }
    var servingsField by remember { mutableStateOf(TextFieldValue("1", selection = TextRange(1))) }
    val servings by remember { derivedStateOf { servingsField.text.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0 } }
    var caloriesText by remember { mutableStateOf(data.calories.fmt) }
    var proteinText by remember { mutableStateOf(data.protein.fmt) }
    var totalFatText by remember { mutableStateOf(data.totalFat.fmt) }
    var totalCarbsText by remember { mutableStateOf(data.totalCarbs.fmt) }
    var fiberText by remember { mutableStateOf(data.fiber.fmt) }
    var totalSugarsText by remember { mutableStateOf(data.totalSugars.fmt) }
    var addedSugarsText by remember { mutableStateOf(data.addedSugars.fmt) }
    var sugarAlcoholsText by remember { mutableStateOf(data.sugarAlcohols.fmt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photo Entry") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
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
                        value = servingsField,
                        onValueChange = { servingsField = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .width(80.dp)
                            .onFocusChanged { fs ->
                                if (fs.isFocused) {
                                    servingsField = servingsField.copy(selection = TextRange(0, servingsField.text.length))
                                }
                            },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                NutritionEditRow("Calories", caloriesText, "Cal") { caloriesText = it }
                NutritionEditRow("Protein", proteinText, "g") { proteinText = it }
                NutritionEditRow("Total Fat", totalFatText, "g") { totalFatText = it }
                NutritionEditRow("Total Carbs", totalCarbsText, "g") { totalCarbsText = it }
                NutritionEditRow("Fiber", fiberText, "g") { fiberText = it }
                NutritionEditRow("Total Sugars", totalSugarsText, "g") { totalSugarsText = it }
                NutritionEditRow("Added Sugars", addedSugarsText, "g") { addedSugarsText = it }
                NutritionEditRow("Sugar Alcohols", sugarAlcoholsText, "g") { sugarAlcoholsText = it }
                val netCarbs = run {
                    val tc = totalCarbsText.toDoubleOrNull() ?: 0.0
                    val f = fiberText.toDoubleOrNull() ?: 0.0
                    val sa = sugarAlcoholsText.toDoubleOrNull() ?: 0.0
                    ((tc - f - sa) * servings).coerceAtLeast(0.0)
                }
                NutritionPreviewRow("Net Carbs", netCarbs, "g", highlight = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    data.copy(
                        description = nameText.trim().takeIf { it.isNotBlank() },
                        calories = caloriesText.toDoubleOrNull() ?: data.calories,
                        protein = proteinText.toDoubleOrNull() ?: data.protein,
                        totalFat = totalFatText.toDoubleOrNull() ?: data.totalFat,
                        totalCarbs = totalCarbsText.toDoubleOrNull() ?: data.totalCarbs,
                        fiber = fiberText.toDoubleOrNull() ?: data.fiber,
                        totalSugars = totalSugarsText.toDoubleOrNull() ?: data.totalSugars,
                        addedSugars = addedSugarsText.toDoubleOrNull() ?: data.addedSugars,
                        sugarAlcohols = sugarAlcoholsText.toDoubleOrNull() ?: data.sugarAlcohols,
                    ),
                    servings,
                )
            }) { Text("Accept") }
        },
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

@Composable
private fun NutritionEditRow(label: String, value: String, unit: String, onValueChange: (String) -> Unit) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length))) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { fieldValue = it; onValueChange(it.text) },
            suffix = { Text(unit, style = MaterialTheme.typography.bodySmall) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .width(96.dp)
                .onFocusChanged { fs ->
                    if (fs.isFocused) {
                        fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                    }
                },
            textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
        )
    }
}

private val detailTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")

@Composable
internal fun FoodLogItemDetailDialog(item: FoodLogItemEntry, onDismiss: () -> Unit) {
    val bitmap = remember(item.imagePath) {
        val path = item.imagePath ?: return@remember null
        val file = File(path)
        if (!file.exists()) return@remember null
        val degrees = ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL).let {
            when (it) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
        val raw = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 2 }) ?: return@remember null
        if (degrees == 0f) raw.asImageBitmap()
        else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(degrees) }, true)
            .also { raw.recycle() }.asImageBitmap()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(item.loggedAt.atZone(ZoneId.systemDefault()).format(detailTimeFormatter)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Food photo",
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
                item.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                item.aiAnalysisResult?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                HorizontalDivider()
                NutrientDetailRow("Calories", item.calories, " Cal", highlight = true)
                NutrientDetailRow("Protein", item.protein, "g")
                NutrientDetailRow("Total Fat", item.totalFat, "g")
                NutrientDetailRow("Total Carbs", item.totalCarbs, "g")
                NutrientDetailRow("  Fiber", item.fiber, "g")
                NutrientDetailRow("  Total Sugars", item.totalSugars, "g")
                NutrientDetailRow("  Added Sugars", item.addedSugars, "g")
                NutrientDetailRow("  Sugar Alcohols", item.sugarAlcohols, "g")
                NutrientDetailRow("Net Carbs", item.netCarbs, "g", highlight = true)
            }
        },
    )
}

@Composable
private fun NutrientDetailRow(label: String, value: Double, unit: String, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal)
        Text("${value.fmt}$unit", style = MaterialTheme.typography.bodySmall, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium)
    }
}
