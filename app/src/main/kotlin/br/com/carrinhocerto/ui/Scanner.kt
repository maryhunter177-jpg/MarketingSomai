package br.com.carrinhocerto.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import br.com.carrinhocerto.domain.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable fun Scanner(ocr: Boolean, active: Boolean, onCode: (String) -> Unit, onPrices: (List<PriceCandidate>) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var permitted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) permitted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permitted = it }
    if (!permitted) {
        Column(modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Precisamos da câmera para ler códigos e etiquetas. Você também pode digitar o código e o preço.")
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Permitir câmera") }
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Abrir configurações") }
        }
        return
    }
    val preview = remember { PreviewView(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torch by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentCode by rememberUpdatedState(onCode)
    val currentPrices by rememberUpdatedState(onPrices)
    val currentActive by rememberUpdatedState(active)
    val capture = remember { AtomicBoolean(false) }
    DisposableEffect(owner, ocr, permitted) {
        val executor = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        val future = ProcessCameraProvider.getInstance(context)
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_CODE_128).build())
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        var last: Long? = null
        var stable = 0
        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        analysis.setAnalyzer(executor, object : ImageAnalysis.Analyzer {
            @androidx.annotation.OptIn(ExperimentalGetImage::class)
            override fun analyze(frame: ImageProxy) {
                val media = frame.image
                if (!currentActive || media == null) { frame.close(); return }
                val input = InputImage.fromMediaImage(media, frame.imageInfo.rotationDegrees)
                if (ocr) recognizer.process(input).addOnSuccessListener(executor) { text ->
                    val candidates = PriceTagParser().parse(text.textBlocks.flatMap { it.lines }.map { line ->
                        val box = line.boundingBox
                        val center = if (frame.imageInfo.rotationDegrees % 180 == 0) frame.height / 2 else frame.width / 2
                        TagLine(line.text, box?.height() ?: 1, if (box == null) 0 else (100 - kotlin.math.abs(box.centerY() - center) * 100 / center.coerceAtLeast(1)).coerceAtLeast(0))
                    })
                    val value = candidates.firstOrNull()?.cents
                    stable = if (value != null && value == last) stable + 1 else 1
                    last = value
                    if (stable == 3 || capture.getAndSet(false)) main.execute { currentPrices(candidates) }
                }.addOnFailureListener { main.execute { error = "Não foi possível ler a etiqueta. Tente novamente ou digite o preço." } }.addOnCompleteListener { frame.close() }
                else scanner.process(input).addOnSuccessListener { codes -> codes.firstOrNull()?.rawValue?.let(currentCode) }.addOnFailureListener { error = "Leitura indisponível. Digite o código." }.addOnCompleteListener { frame.close() }
            }
        })
        future.addListener({
            if (!disposed) try {
                provider = future.get()
                val view = Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider }
                provider?.unbindAll()
                camera = provider?.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, view, analysis)
            } catch (_: Exception) { error = "Câmera indisponível. Você pode digitar o código e o preço." }
        }, main)
        onDispose { disposed = true; analysis.clearAnalyzer(); provider?.unbindAll(); scanner.close(); recognizer.close(); executor.shutdown() }
    }
    Box(modifier) {
        AndroidView(factory = { preview }, modifier = Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.Center).fillMaxWidth(0.85f).height(72.dp).border(2.dp, MaterialTheme.colorScheme.primary))
        Surface(Modifier.align(Alignment.TopCenter), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) { Text(if (ocr) "Enquadre o preço da prateleira" else "Enquadre o código de barras", Modifier.padding(8.dp)) }
        Row(Modifier.align(Alignment.BottomCenter)) {
            FilledTonalButton(onClick = { if (camera?.cameraInfo?.hasFlashUnit() == true) { torch = !torch; camera?.cameraControl?.enableTorch(torch) } else error = "Este aparelho não tem lanterna disponível." }) { Text(if (torch) "Desligar lanterna" else "Lanterna") }
            if (ocr) Button(onClick = { capture.set(true) }) { Text("Capturar preço") }
        }
        error?.let { Surface(Modifier.align(Alignment.Center)) { Text(it, Modifier.padding(12.dp)) } }
    }
}
