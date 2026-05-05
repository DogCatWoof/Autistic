package org.meow.autistic.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.product.NutrientValue
import org.meow.autistic.data.product.Nutriments
import org.meow.autistic.data.product.ProductEntity
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

/**
 * Barcode scan screen. Camera fills the top half; scanned barcodes accumulate in a
 * list on the bottom half with live status updates and swipe-to-dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(modifier: Modifier = Modifier, viewModel: ScanViewModel = koinViewModel()) {
    val context = LocalContext.current
    val queue by viewModel.queue.collectAsState()
    val selectedBarcode by viewModel.selectedBarcode.collectAsState()

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraPermissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        cameraPermissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Resolve selected item for the detail sheet
    val selectedItem = queue.find { it.barcode == selectedBarcode }
    val selectedProduct = (selectedItem?.status as? ScanStatus.Found)?.product

    Column(modifier = modifier.fillMaxSize().testTag("scan_screen")) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                cameraPermissionDenied -> PermissionDeniedContent(
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    },
                    modifier = Modifier.align(Alignment.Center),
                )
                cameraPermissionGranted ->
                    CameraPreview(onBarcodeDetected = viewModel::onBarcodeDetected)
                else -> Box(modifier = Modifier.fillMaxSize())
            }
        }
        ScanQueue(
            queue = queue,
            selectedBarcode = selectedBarcode,
            onSelect = viewModel::select,
            onDismiss = viewModel::dismiss,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }

    if (selectedProduct != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.select(selectedBarcode!!) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ProductDetail(product = selectedProduct, modifier = Modifier.padding(bottom = 32.dp))
        }
    }
}

@Composable
private fun PermissionDeniedContent(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(24.dp),
    ) {
        Text("Camera permission required", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Grant camera permission in app settings to scan barcodes.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenSettings) { Text("Open Settings") }
    }
}

@Composable
private fun CameraPreview(onBarcodeDetected: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember { BarcodeAnalyzer(onBarcodeDetected) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}

@Composable
private fun ProductDetail(product: ProductEntity, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(product.name, style = MaterialTheme.typography.titleLarge)
        product.brands?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        NutritionFactsLabel(product)
        product.ingredients?.let {
            Text("Ingredients", style = MaterialTheme.typography.labelLarge)
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun NutritionFactsLabel(product: ProductEntity, modifier: Modifier = Modifier) {
    val borderColor = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RectangleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text("Nutrition Facts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        product.servingsPerContainer?.let {
            Text("$it servings per container", style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Serving size", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(product.quantity ?: "—", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(thickness = 8.dp, color = borderColor, modifier = Modifier.padding(vertical = 2.dp))

        Text("Amount per serving", style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Calories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(product.nutriments?.energyKcal?.value ?: "—", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        }

        HorizontalDivider(thickness = 4.dp, color = borderColor)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("% Daily Value*", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }

        val n = product.nutriments
        NutrientRow("Total Fat",            n?.fat,               bold = true, dv = 78.0,   dvUnit = "g")
        NutrientRow("Saturated Fat",        n?.saturatedFat,      bold = true, dv = 20.0,   dvUnit = "g",  indent = true)
        NutrientRow("Trans Fat",            n?.transFat,          indent = true)
        NutrientRow("Polyunsaturated Fat",  n?.polyunsaturatedFat, indent = true)
        NutrientRow("Monounsaturated Fat",  n?.monounsaturatedFat, indent = true)
        NutrientRow("Cholesterol",          n?.cholesterol,       bold = true, dv = 300.0,  dvUnit = "mg")
        NutrientRow("Sodium",               n?.sodium,            bold = true, dv = 2300.0, dvUnit = "mg")
        NutrientRow("Total Carbohydrate",   n?.carbohydrates,     bold = true, dv = 275.0,  dvUnit = "g")
        NutrientRow("Dietary Fiber",        n?.fiber,             bold = true, dv = 28.0,   dvUnit = "g",  indent = true)
        NutrientRow("Total Sugars",         n?.sugars,            indent = true)
        NutrientRow("Protein",              n?.proteins,          bold = true)

        if (n != null) VitaminsAndMineralsTable(n, borderColor)

        Spacer(Modifier.height(4.dp))
        Text(
            "* The % Daily Value tells you how much a nutrient in a serving contributes to a daily diet. 2,000 calories a day is used for general nutrition advice.",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun VitaminsAndMineralsTable(n: Nutriments, borderColor: Color) {
    val items: List<Pair<String, NutrientValue>> = listOfNotNull(
        n.vitaminA?.let  { "Vitamin A"    to it },
        n.vitaminD?.let  { "Vitamin D"    to it },
        n.calcium?.let   { "Calcium"      to it },
        n.iron?.let      { "Iron"         to it },
        n.potassium?.let { "Potassium"    to it },
        n.magnesium?.let { "Magnesium"    to it },
        n.phosphorus?.let{ "Phosphorus"   to it },
        n.riboflavin?.let{ "Riboflavin"   to it },
        n.folate?.let    { "Folate"       to it },
        n.vitaminB12?.let{ "Vitamin B-12" to it },
    )
    if (items.isEmpty()) return
    HorizontalDivider(thickness = 4.dp, color = borderColor)
    items.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth()) {
            MineralCell(pair[0].first, pair[0].second, Modifier.weight(1f))
            if (pair.size > 1) {
                VerticalDivider(color = borderColor)
                MineralCell(pair[1].first, pair[1].second, Modifier.weight(1f))
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = borderColor)
    }
}

@Composable
private fun NutrientRow(
    label: String,
    nv: NutrientValue?,
    bold: Boolean = false,
    dv: Double? = null,
    dvUnit: String? = null,
    indent: Boolean = false,
) {
    HorizontalDivider(thickness = 0.5.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 16.dp else 0.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val weight = if (bold) FontWeight.Bold else FontWeight.Normal
        Row(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = weight)
            nv?.let { Text(" ${it.value}${it.unit}", style = MaterialTheme.typography.bodySmall) }
        }
        val pct = dvPercent(nv, dv, dvUnit)
        if (pct != null) {
            Text(
                "$pct%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 32.dp),
            )
        }
    }
}

@Composable
private fun MineralCell(label: String, nv: NutrientValue, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("$label ${nv.value}${nv.unit}", style = MaterialTheme.typography.labelSmall)
        nv.dailyValuePercent?.let {
            Text("${it}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

private fun dvPercent(nv: NutrientValue?, dv: Double? = null, dvUnit: String? = null): Int? {
    if (nv == null) return null
    nv.dailyValuePercent?.let { return it }
    if (dv == null || dvUnit == null) return null
    val amount = nv.value.toDoubleOrNull() ?: return null
    val normalized = when {
        nv.unit.lowercase() == "mg" && dvUnit.lowercase() == "g" -> amount / 1000.0
        nv.unit.lowercase() == "g" && dvUnit.lowercase() == "mg" -> amount * 1000.0
        else -> amount
    }
    return (normalized / dv * 100).roundToInt().coerceIn(0, 999)
}

/**
 * Debounces barcode detections: fires [onBarcodeDetected] only when the detected barcode
 * changes, or after [DEBOUNCE_MS] has elapsed since the last detection of the same barcode.
 */
private class BarcodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) :
    ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private val lastBarcode = AtomicReference<String?>(null)
    private val lastDetectedAt = AtomicLong(0L)

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let { code ->
                    val now = SystemClock.elapsedRealtime()
                    val prev = lastBarcode.get()
                    if (code != prev || now - lastDetectedAt.get() > DEBOUNCE_MS) {
                        lastBarcode.set(code)
                        lastDetectedAt.set(now)
                        onBarcodeDetected(code)
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    companion object {
        private const val DEBOUNCE_MS = 1_500L
    }
}
