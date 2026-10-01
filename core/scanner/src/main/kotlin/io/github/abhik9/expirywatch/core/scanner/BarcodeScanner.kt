package io.github.abhik9.expirywatch.core.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * A full-screen camera scanner. Calls [onBarcodeScanned] once with the first barcode it reads.
 *
 * @param onEnterManually offered as a fallback when scanning doesn't work, e.g. without a camera.
 */
@Composable
fun BarcodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onEnterManually: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BarcodeScanner(
            onBarcodeScanned = onBarcodeScanned,
            onEnterManually = onEnterManually,
            onClose = onDismiss,
        )
    }
}

@Composable
fun BarcodeScanner(
    onBarcodeScanned: (String) -> Unit,
    onEnterManually: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BarcodeScannerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        permissionDenied = !granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val haptics = LocalHapticFeedback.current
    val detectedBarcode by viewModel.detectedBarcode.collectAsStateWithLifecycle()
    LaunchedEffect(detectedBarcode) {
        val barcode = detectedBarcode ?: return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.consumeDetectedBarcode()
        onBarcodeScanned(barcode)
    }

    val isCameraUnavailable by viewModel.isCameraUnavailable.collectAsStateWithLifecycle()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (hasCameraPermission && isCameraUnavailable) {
            Text(
                text = stringResource(R.string.core_scanner_camera_unavailable),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        } else if (hasCameraPermission) {
            CameraViewfinder(viewModel)
            ScanWindowOverlay(Modifier.fillMaxSize())
            Text(
                text = stringResource(R.string.core_scanner_hint),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 220.dp, start = 32.dp, end = 32.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        } else if (permissionDenied) {
            CameraPermissionRationale(
                onRequestPermission = { context.openAppSettings() },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        val torch by viewModel.torch.collectAsStateWithLifecycle()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val iconColors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            IconButton(onClick = onClose, colors = iconColors) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.core_scanner_close))
            }
            if (torch.isAvailable) {
                IconButton(onClick = { viewModel.setTorch(!torch.isOn) }, colors = iconColors) {
                    Icon(
                        imageVector = if (torch.isOn) Icons.Filled.FlashlightOff else Icons.Filled.FlashlightOn,
                        contentDescription = stringResource(
                            if (torch.isOn) R.string.core_scanner_torch_off else R.string.core_scanner_torch_on,
                        ),
                    )
                }
            }
        }

        FilledTonalButton(
            onClick = onEnterManually,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = 24.dp),
        ) {
            Icon(Icons.Outlined.Keyboard, contentDescription = null)
            Text(text = stringResource(R.string.core_scanner_enter_manually), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun CameraViewfinder(viewModel: BarcodeScannerViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
    }
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    surfaceRequest?.let { request ->
        CameraXViewfinder(surfaceRequest = request, modifier = Modifier.fillMaxSize())
    }
}

/** Dims everything but a rounded window in the middle, where the barcode should go. */
@Composable
private fun ScanWindowOverlay(modifier: Modifier = Modifier) {
    val frameColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val windowWidth = size.width * WINDOW_WIDTH_FRACTION
        val windowHeight = windowWidth * WINDOW_ASPECT_RATIO
        val topLeft = Offset((size.width - windowWidth) / 2, (size.height - windowHeight) / 2)
        val windowSize = Size(windowWidth, windowHeight)
        val cornerRadius = CornerRadius(24.dp.toPx())

        drawRect(Color.Black.copy(alpha = SCRIM_ALPHA))
        drawRoundRect(Color.Transparent, topLeft, windowSize, cornerRadius, blendMode = BlendMode.Clear)
        drawRoundRect(frameColor, topLeft, windowSize, cornerRadius, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
private fun CameraPermissionRationale(onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.core_scanner_permission_rationale),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRequestPermission) {
            Text(stringResource(R.string.core_scanner_open_settings))
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private const val WINDOW_WIDTH_FRACTION = 0.8f
private const val WINDOW_ASPECT_RATIO = 0.6f
private const val SCRIM_ALPHA = 0.55f
