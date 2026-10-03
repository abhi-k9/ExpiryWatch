package io.github.abhik9.expirywatch.core.scanner

import android.content.Context
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TorchState(
    val isAvailable: Boolean = false,
    val isOn: Boolean = false,
)

/**
 * Owns the camera use cases for the scanner: a preview for the viewfinder and an analysis stream
 * fed to the flavor's [BarcodeAnalyzer].
 */
@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    analyzerFactory: BarcodeAnalyzerFactory,
    private val log: EventLog,
) : ViewModel() {
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    private val _torch = MutableStateFlow(TorchState())
    val torch: StateFlow<TorchState> = _torch.asStateFlow()

    private val _isUnavailable = MutableStateFlow(false)

    /** The scanner couldn't be started, e.g. because there is no camera or the barcode reader failed. */
    val isUnavailable: StateFlow<Boolean> = _isUnavailable.asStateFlow()

    private val _detectedBarcode = MutableStateFlow<String?>(null)

    /** The first barcode found since the last [consumeDetectedBarcode]. */
    val detectedBarcode: StateFlow<String?> = _detectedBarcode.asStateFlow()

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val analyzer: BarcodeAnalyzer? = try {
        analyzerFactory.create { barcode ->
            if (_detectedBarcode.compareAndSet(expect = null, update = barcode)) {
                log.record { "scanner: barcode detected" }
            }
        }
    } catch (e: RuntimeException) {
        // E.g. ML Kit missing a component: the user can still type the barcode.
        log.record { "scanner: barcode reader unavailable: ${e.stackTraceToString()}" }
        _isUnavailable.value = true
        null
    }

    private val preview = Preview.Builder().build().apply {
        setSurfaceProvider { request -> _surfaceRequest.value = request }
    }

    private val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        ANALYSIS_RESOLUTION,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    ),
                )
                .build(),
        )
        .build()
        .apply { analyzer?.let { setAnalyzer(analysisExecutor, it) } }

    private var camera: Camera? = null

    /** Binds the camera to [lifecycleOwner] until the calling coroutine is cancelled. */
    suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
        if (analyzer == null) return
        val (cameraProvider, boundCamera) = try {
            val provider = ProcessCameraProvider.awaitInstance(appContext)
            provider to provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.record { "scanner: camera unavailable: $e" }
            _isUnavailable.value = true
            return
        }
        camera = boundCamera
        _torch.value = TorchState(isAvailable = boundCamera.cameraInfo.hasFlashUnit())
        log.record { "scanner: camera started, flashlight available: ${_torch.value.isAvailable}" }
        try {
            awaitCancellation()
        } finally {
            cameraProvider.unbind(preview, imageAnalysis)
            camera = null
            _torch.update { it.copy(isOn = false) }
        }
    }

    fun setTorch(on: Boolean) {
        val cameraControl = camera?.cameraControl ?: return
        cameraControl.enableTorch(on)
        _torch.update { it.copy(isOn = on) }
    }

    fun consumeDetectedBarcode() {
        _detectedBarcode.value = null
    }

    override fun onCleared() {
        imageAnalysis.clearAnalyzer()
        analyzer?.close()
        analysisExecutor.shutdown()
    }

    private companion object {
        // Plenty for barcodes, and small enough to analyze several frames per second.
        val ANALYSIS_RESOLUTION = Size(1280, 720)
    }
}
