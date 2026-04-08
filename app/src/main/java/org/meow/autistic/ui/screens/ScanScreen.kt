package org.meow.autistic.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.koin.androidx.compose.koinViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Barcode scan screen. Requests camera permission, displays a live camera preview,
 * detects barcodes using ML Kit, and looks up the product in the local database.
 */
@Composable
fun ScanScreen(modifier: Modifier = Modifier, viewModel: ScanViewModel = koinViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

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

    LaunchedEffect(uiState) {
        if (uiState is ScanUiState.Scanning && !cameraPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            uiState is ScanUiState.Loading -> CircularProgressIndicator()

            uiState is ScanUiState.NeedsSync -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Button(onClick = { viewModel.triggerProductSync() }) {
                    Text("Download Product Database")
                }
            }

            uiState is ScanUiState.Syncing -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    (uiState as ScanUiState.Syncing).status,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            cameraPermissionDenied -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text("Camera permission required", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Grant camera permission in app settings to scan barcodes.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }) { Text("Open Settings") }
            }

            uiState is ScanUiState.Scanning && cameraPermissionGranted ->
                CameraPreview(onBarcodeDetected = viewModel::onBarcodeDetected)

            uiState is ScanUiState.Found -> ProductDetail(
                productJson = (uiState as ScanUiState.Found).productJson,
                onScanAgain = viewModel::resetToScanning
            )

            uiState is ScanUiState.NotFound -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text("Product not found", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Barcode: ${(uiState as ScanUiState.NotFound).barcode}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = viewModel::resetToScanning) { Text("Scan again") }
            }
        }
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
                    imageAnalysis
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        update = { _ -> analyzer.reset() }
    )
}

@Composable
private fun ProductDetail(productJson: String, onScanAgain: () -> Unit, modifier: Modifier = Modifier) {
    val map: Map<String, String> = remember(productJson) {
        Gson().fromJson(productJson, object : TypeToken<Map<String, String>>() {}.type)
            ?: emptyMap()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = map["product_name"]?.takeIf { it.isNotBlank() } ?: "Unknown product",
            style = MaterialTheme.typography.titleLarge
        )
        map["brands"]?.takeIf { it.isNotBlank() }?.let {
            Text(text = "Brand: $it", style = MaterialTheme.typography.bodyMedium)
        }
        map["quantity"]?.takeIf { it.isNotBlank() }?.let {
            Text(text = "Quantity: $it", style = MaterialTheme.typography.bodyMedium)
        }
        map["ingredients_text"]?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(text = "Ingredients", style = MaterialTheme.typography.labelLarge)
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
        val nutritionKeys = listOf("energy_100g", "proteins_100g", "carbohydrates_100g", "fat_100g")
        val nutrition = nutritionKeys.mapNotNull { key ->
            val v = map[key]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            key.removeSuffix("_100g").replaceFirstChar { it.uppercase() } to v
        }
        if (nutrition.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(text = "Nutrition (per 100g)", style = MaterialTheme.typography.labelLarge)
            nutrition.forEach { (label, value) ->
                Text(text = "$label: $value", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onScanAgain, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Scan again")
        }
    }
}

private class BarcodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) :
    ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private val active = AtomicBoolean(true)

    fun reset() { active.set(true) }

    override fun analyze(imageProxy: ImageProxy) {
        if (!active.get()) { imageProxy.close(); return }
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let { code ->
                    if (active.compareAndSet(true, false)) {
                        onBarcodeDetected(code)
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
