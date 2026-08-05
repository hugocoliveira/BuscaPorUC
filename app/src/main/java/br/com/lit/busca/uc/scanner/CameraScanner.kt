package br.com.lit.busca.uc.scanner

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CameraScanner"

/**
 * Configura e inicia a câmera traseira com análise de imagem em tempo real.
 * Retorna o [ExecutorService] — o chamador deve chamar shutdown() ao encerrar.
 */
fun iniciarScanner(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onCodigoLido: (String) -> Unit
): ExecutorService {
    val cameraExecutor = Executors.newSingleThreadExecutor()
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor, BarcodeAnalyzer { codigo ->
                    previewView.post { onCodigoLido(codigo) }
                })
            }

        runCatching {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
        }.onFailure { e -> Log.e(TAG, "Falha ao iniciar câmera: ${e.message}", e) }

    }, ContextCompat.getMainExecutor(context))

    return cameraExecutor
}

private class BarcodeAnalyzer(
    private val onCodigoDetectado: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val imagemMedia = imageProxy.image
        if (imagemMedia == null) { imageProxy.close(); return }

        val imagem = InputImage.fromMediaImage(imagemMedia, imageProxy.imageInfo.rotationDegrees)
        scanner.process(imagem)
            .addOnSuccessListener { codigos ->
                codigos.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.let { onCodigoDetectado(it) }
            }
            .addOnFailureListener { e -> Log.w(TAG, "Falha na análise do frame: ${e.message}") }
            .addOnCompleteListener { imageProxy.close() }
    }
}
