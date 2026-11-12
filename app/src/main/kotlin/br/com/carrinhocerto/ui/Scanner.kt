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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
        Column(modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { AppIcon(Symbol.Camera, tint = MaterialTheme.colorScheme.primary) }
                Column(Modifier.weight(1f)) {
                    Text("Escaneie e confira", style = MaterialTheme.typography.titleMedium)
                    Text("Ative a câmera para ler códigos e etiquetas. Você também pode digitar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 14.dp)) { Text("Ativar câmera") }
                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Configurações", fontSize = 12.sp) }
            }
        }
        return
    }
    val preview = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torch by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentCode by rememberUpdatedState(onCode)
    val currentPrices by rememberUpdatedState(onPrices)
    val currentActive by rememberUpdatedState(active)
    val capture = remember { AtomicBoolean(false) }
    DisposableEffect(owner, ocr, permitted, active) {
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
                try {
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
                } catch (_: Exception) { frame.close(); main.execute { error = "Leitura indisponível. Tente novamente ou digite manualmente." } }
            }
        })
        future.addListener({
            if (!disposed && active) try {
                provider = future.get()
                val view = Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider }
                provider?.unbindAll()
                camera = provider?.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, view, analysis)
                torch = false
                error = null
                camera?.cameraInfo?.cameraState?.observe(owner) { state -> if (state.error != null) error = "Câmera indisponível. Feche outros aplicativos ou digite manualmente." }
            } catch (_: Exception) { error = "Câmera indisponível. Você pode digitar o código e o preço." }
        }, main)
        onDispose { disposed = true; camera?.cameraInfo?.cameraState?.removeObservers(owner); analysis.clearAnalyzer(); provider?.unbindAll(); scanner.close(); recognizer.close(); executor.shutdown() }
    }
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xFF11231C))) {
        AndroidView(factory = { preview }, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
        Box(Modifier.align(Alignment.Center).fillMaxWidth(0.8f).height(if (ocr) 72.dp else 48.dp).border(2.dp, Mint, RoundedCornerShape(12.dp)))
        Row(Modifier.align(Alignment.TopStart).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppIcon(if (ocr) Symbol.Camera else Symbol.Scan, modifier = Modifier.size(16.dp), tint = Mint)
            Text(if (ocr) "Enquadre o preço da prateleira" else "Enquadre o código de barras", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { if (camera?.cameraInfo?.hasFlashUnit() == true) { torch = !torch; camera?.cameraControl?.enableTorch(torch) } else error = "Este aparelho não tem lanterna disponível." }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = if (torch) Mint else Color.White), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                AppIcon(Symbol.Flash, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(if (torch) "Lanterna ligada" else "Lanterna", fontSize = 11.sp)
            }
            if (ocr) Button(onClick = { capture.set(true) }) { Text("Capturar preço") }
        }
        error?.let { Surface(Modifier.align(Alignment.Center)) { Text(it, Modifier.padding(12.dp)) } }
    }
}
