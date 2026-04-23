package org.meow.autistic.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.foodlog.FoodLogEntry
import org.meow.autistic.data.foodlog.FoodLogItemEntry
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/** At most 4 characters: whole number up to 9999, one decimal up to 999.9, no decimals above that. */
private val Double.fmt: String
    get() = when {
        this >= 1000.0 || this == kotlin.math.floor(this) -> this.toInt().toString()
        else -> "%.1f".format(this)
    }

@Composable
fun FoodLogScreen(modifier: Modifier = Modifier, viewModel: FoodLogViewModel = koinViewModel()) {
    val context = LocalContext.current
    val date by viewModel.date.collectAsState()
    val totals by viewModel.totals.collectAsState()
    val entryList by viewModel.items.collectAsState()
    val labelState by viewModel.labelAnalysisState.collectAsState()
    val photoStatuses by viewModel.photoAnalysisStatuses.collectAsState()
    var fabExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    val fabRotation by animateFloatAsState(targetValue = if (fabExpanded) 45f else 0f, label = "fab_rotation")

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        if (captured) {
            previewUri = pendingCameraUri
            fabExpanded = false
        }
        pendingCameraUri = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // No-op here, user can tap again or we could call launchCamera()
        // but it's cleaner to let the user tap again to avoid unexpected behavior
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val tempDir = File(context.cacheDir, "camera_temp").also { it.mkdirs() }
        val tempFile = File(tempDir, "temp_food_photo.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    fun savePhoto(): String? {
        val destDir = File(context.filesDir, "food_log_images").also { it.mkdirs() }
        val destFile = File(destDir, "${System.currentTimeMillis()}.jpg")
        return runCatching {
            File(context.cacheDir, "camera_temp/temp_food_photo.jpg").copyTo(destFile, overwrite = true)
            destFile.absolutePath
        }.getOrNull()
    }

    if (showAddDialog) {
        AddFoodEntryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { item ->
                viewModel.addItem(item)
                showAddDialog = false
            },
        )
    }

    when (val state = labelState) {
        is LabelAnalysisState.Loading ->
            NutritionLabelLoadingDialog(onDismiss = viewModel::dismissLabelAnalysis)
        is LabelAnalysisState.Ready ->
            NutritionLabelServingsDialog(
                data = state.data,
                imagePath = state.imagePath,
                onDismiss = viewModel::dismissLabelAnalysis,
                onConfirm = { servings -> viewModel.saveLabelEntry(state.data, servings, state.imagePath) },
            )
        is LabelAnalysisState.Error ->
            NutritionLabelErrorDialog(
                onDismiss = viewModel::dismissLabelAnalysis,
                onManualEntry = { viewModel.dismissLabelAnalysis(); showAddDialog = true },
            )
        LabelAnalysisState.Idle -> {}
    }

    Scaffold(
        modifier = modifier,
        topBar = { FoodLogDateBar(date = date, onPrev = viewModel::previousDay, onNext = viewModel::nextDay) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = fabExpanded) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 16.dp),
                    ) {
                        FoodLogSpeedDialItem("Photo") { launchCamera() }
                        FoodLogSpeedDialItem("Manual") { fabExpanded = false; showAddDialog = true }
                    }
                }
                FloatingActionButton(onClick = { fabExpanded = !fabExpanded }) {
                    Icon(Icons.Default.Add, contentDescription = "New Entry", modifier = Modifier.rotate(fabRotation))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item { FoodLogNutritionSummary(totals) }
            if (entryList.isNotEmpty()) {
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                item {
                    Text(
                        "Entries",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                    )
                }
                items(entryList, key = { it.id }) { item ->
                    FoodLogEntryListItem(
                        item = item,
                        analysisStatus = photoStatuses[item.id],
                        onDelete = { viewModel.deleteItem(item) },
                    )
                }
            }
        }

        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable { fabExpanded = false },
            )
        }
    }

    val uri = previewUri
    if (uri != null) {
        PhotoPreviewOverlay(
            uri = uri,
            onRetake = { launchCamera() },
            onAcceptAsLabel = {
                val path = savePhoto()
                previewUri = null
                if (path != null) viewModel.startLabelAnalysis(path)
            },
            onAcceptAsFood = {
                val path = savePhoto()
                previewUri = null
                if (path != null) viewModel.queueFoodPhotoItem(path)
            },
            onDismiss = { previewUri = null },
        )
    }
}

@Composable
private fun FoodLogNutritionSummary(totals: FoodLogEntry) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FoodLogNutritionSection("Calories")
        CalorieBreakdown(totals)
        FoodLogNutritionSection("Carbohydrates")
        FoodLogNutritionRow("Total Carbs", totals.totalCarbs, "g", 0.dp)
        FoodLogNutritionRow("Dietary Fiber", totals.fiber, "g", 16.dp)
        FoodLogNutritionRow("Total Sugars", totals.totalSugars, "g", 16.dp)
        FoodLogNutritionRow("Added Sugars", totals.addedSugars, "g", 32.dp)
        FoodLogNutritionRow("Sugar Alcohols", totals.sugarAlcohols, "g", 16.dp)
        FoodLogNutritionRow("Net Carbs", totals.netCarbs, "g", 16.dp)
    }
}

@Composable
private fun CalorieBreakdown(totals: FoodLogEntry) {
    val proteinColor = MaterialTheme.colorScheme.primary
    val fatColor = MaterialTheme.colorScheme.tertiary
    val carbColor = MaterialTheme.colorScheme.error
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    val total = totals.calories.coerceAtLeast(1.0)
    val proteinFrac = (totals.proteinCalories / total).toFloat().coerceIn(0f, 1f)
    val fatFrac = (totals.fatCalories / total).toFloat().coerceIn(0f, 1f)
    val carbFrac = (totals.netCarbCalories / total).toFloat().coerceIn(0f, 1f)
    val remaining = (1f - proteinFrac - fatFrac - carbFrac).coerceAtLeast(0f)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Total", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "${totals.calories.fmt} kcal",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 60.dp),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp)),
    ) {
        if (proteinFrac > 0f) Box(Modifier.weight(proteinFrac).fillMaxHeight().background(proteinColor))
        if (fatFrac > 0f) Box(Modifier.weight(fatFrac).fillMaxHeight().background(fatColor))
        if (carbFrac > 0f) Box(Modifier.weight(carbFrac).fillMaxHeight().background(carbColor))
        if (remaining > 0f) Box(Modifier.weight(remaining).fillMaxHeight().background(emptyColor))
    }

    CalorieLegendRow("Protein", totals.proteinCalories, total, proteinColor)
    CalorieLegendRow("Fat", totals.fatCalories, total, fatColor)
    CalorieLegendRow("Net Carbs", totals.netCarbCalories, total, carbColor)
}

@Composable
private fun CalorieLegendRow(label: String, kcal: Double, total: Double, color: Color) {
    val pct = if (total > 0.0) (kcal / total * 100).toInt() else 0
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f).padding(start = 6.dp),
        )
        Text(
            "${kcal.fmt} kcal",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 60.dp),
            textAlign = TextAlign.End,
        )
        Text(
            "$pct%",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun FoodLogNutritionSection(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun FoodLogNutritionRow(label: String, value: Double, unit: String, indent: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = indent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "${value.fmt}$unit",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 60.dp),
        )
    }
}

@Composable
private fun FoodLogEntryListItem(
    item: FoodLogItemEntry,
    analysisStatus: PhotoAnalysisStatus?,
    onDelete: () -> Unit,
) {
    val timeLabel = item.loggedAt.atZone(ZoneId.systemDefault()).format(timeFormatter)
    val summaryLine = buildString {
        if (item.description != null) append("${item.description} · ")
        when {
            analysisStatus == PhotoAnalysisStatus.Analyzing -> append("Analyzing... · ")
            analysisStatus == PhotoAnalysisStatus.Queued || item.isAiPending -> append("Queued for WiFi · ")
            item.imagePath != null && item.aiAnalysisResult == null -> append("📷 · ")
            else -> {}
        }
        append("${item.calories.fmt} kcal · ${item.netCarbs.fmt}g net carbs")
    }

    ListItem(
        headlineContent = { Text(timeLabel) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(summaryLine)
                if (item.aiAnalysisResult != null) {
                    Text(
                        text = item.aiAnalysisResult,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete entry")
            }
        },
    )
}

@Composable
private fun FoodLogSpeedDialItem(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = MaterialTheme.shapes.small, tonalElevation = 6.dp, shadowElevation = 2.dp, onClick = onClick) {
            Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
        SmallFloatingActionButton(onClick = onClick) {
            Icon(Icons.Default.Add, contentDescription = label)
        }
    }
}

@Composable
private fun AddFoodEntryDialog(onDismiss: () -> Unit, onConfirm: (FoodLogItemEntry) -> Unit) {
    var description by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var totalFat by remember { mutableStateOf("") }
    var totalCarbs by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var totalSugars by remember { mutableStateOf("") }
    var addedSugars by remember { mutableStateOf("") }
    var sugarAlcohols by remember { mutableStateOf("") }

    val netCarbs by remember {
        derivedStateOf {
            val tc = totalCarbs.toDoubleOrNull() ?: 0.0
            val f = fiber.toDoubleOrNull() ?: 0.0
            val sa = sugarAlcohols.toDoubleOrNull() ?: 0.0
            (tc - f - sa).coerceAtLeast(0.0)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                FoodLogDialogLabel("Calories")
                FoodLogNutrientInputRow("Calories", calories, "kcal") { calories = it }
                FoodLogNutrientInputRow("Protein", protein, "g") { protein = it }
                FoodLogNutrientInputRow("Total Fat", totalFat, "g") { totalFat = it }
                FoodLogDialogLabel("Carbohydrates")
                FoodLogNutrientInputRow("Total Carbs", totalCarbs, "g") { totalCarbs = it }
                FoodLogNutrientInputRow("Dietary Fiber", fiber, "g") { fiber = it }
                FoodLogNutrientInputRow("Total Sugars", totalSugars, "g") { totalSugars = it }
                FoodLogNutrientInputRow("Added Sugars", addedSugars, "g") { addedSugars = it }
                FoodLogNutrientInputRow("Sugar Alcohols", sugarAlcohols, "g") { sugarAlcohols = it }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Net Carbs",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${netCarbs.fmt}g",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(FoodLogItemEntry(
                    date = "", loggedAt = Instant.now(),
                    description = description.trim().takeIf { it.isNotEmpty() },
                    calories = calories.toDoubleOrNull() ?: 0.0,
                    protein = protein.toDoubleOrNull() ?: 0.0,
                    totalFat = totalFat.toDoubleOrNull() ?: 0.0,
                    totalCarbs = totalCarbs.toDoubleOrNull() ?: 0.0,
                    fiber = fiber.toDoubleOrNull() ?: 0.0,
                    totalSugars = totalSugars.toDoubleOrNull() ?: 0.0,
                    addedSugars = addedSugars.toDoubleOrNull() ?: 0.0,
                    sugarAlcohols = sugarAlcohols.toDoubleOrNull() ?: 0.0,
                ))
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun FoodLogDialogLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun FoodLogNutrientInputRow(label: String, value: String, unit: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            suffix = { Text(unit) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
            modifier = Modifier.width(96.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodLogDateBar(date: String, onPrev: () -> Unit, onNext: () -> Unit) {
    val today = LocalDate.now()
    val parsed = LocalDate.parse(date)
    val label = when (parsed) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
    TopAppBar(
        title = { Text("Food Log · $label") },
        navigationIcon = {
            IconButton(onClick = onPrev) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous day")
            }
        },
        actions = {
            IconButton(onClick = onNext) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next day")
            }
        },
    )
}
