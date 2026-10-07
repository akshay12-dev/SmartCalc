package com.allcalc.app.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.allcalc.app.ui.common.NoticeBanner
import com.allcalc.app.ui.common.ScreenHeader
import com.allcalc.app.utils.AppViewModelProvider
import com.allcalc.app.utils.BitmapUtils
import java.util.concurrent.Executors
import kotlin.math.floor

import androidx.compose.material3.CircularProgressIndicator

@Composable
fun ScanScreen(
    onHome: () -> Unit,
    onOpenCalculator: (() -> Unit)? = null
) {
    val viewModel: ScanViewModel = viewModel(factory = AppViewModelProvider)
    val state by viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = try {
                BitmapUtils.decodeUri(context, uri)
            } catch (e: Exception) {
                null
            }
            if (bitmap != null) viewModel.onCaptured(bitmap)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Scan & Solve", onHome = onHome)

        when (state.phase) {
            ScanPhase.CAMERA -> {
                Box(modifier = Modifier.weight(1f)) {
                    if (!hasPermission) {
                        PermissionNotice {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    } else {
                        CameraSection(
                            onCaptured = viewModel::onCaptured,
                            onPickImage = {
                                pickImage.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }
                }
            }
            ScanPhase.CROP -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    state.image?.let { bitmap ->
                        CropOverlay(
                            bitmap = bitmap,
                            onConfirm = viewModel::onCropped,
                            onCancel = viewModel::onCancelCrop
                        )
                    }
                    if (state.ocrBusy) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Black.copy(alpha = 0.75f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Recognizing equation…",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
            ScanPhase.RESULT -> {
                ResultSection(
                    state = state,
                    onEditText = viewModel::onEditText,
                    onSolve = viewModel::onSolve,
                    onSave = viewModel::onSaveResult,
                    onRetake = viewModel::onRetake,
                    onClearError = viewModel::clearError,
                    onOpenCalculator = onOpenCalculator
                )
            }
        }
    }
}

@Composable
private fun PermissionNotice(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera permission is needed to scan equations.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onRequest) { Text("Grant camera permission") }
    }
}

@Composable
private fun CameraSection(
    onCaptured: (Bitmap) -> Unit,
    onPickImage: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraUnavailable by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    val onCapturedRef = rememberUpdatedState(onCaptured)

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val runnable = Runnable {
            try {
                val provider = providerFuture.get()
                provider.unbindAll()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                cameraUnavailable = false
            } catch (e: Exception) {
                cameraUnavailable = true
            }
        }
        providerFuture.addListener(runnable, ContextCompat.getMainExecutor(context))
        onDispose {
            executor.shutdown()
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            if (cameraUnavailable) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Camera is not available on this device. You can still pick an image.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onPickImage) { Text("Pick from gallery") }
                }
            } else {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPickImage,
                modifier = Modifier.weight(1f)
            ) { Text("Gallery") }
            Button(
                onClick = {
                    capturing = true
                    imageCapture.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = runCatching { BitmapUtils.imageProxyToBitmap(image) }.getOrNull()
                                image.close()
                                capturing = false
                                if (bitmap != null) onCapturedRef.value(bitmap)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                capturing = false
                            }
                        }
                    )
                },
                enabled = !capturing,
                modifier = Modifier.weight(1f)
            ) { Text(if (capturing) "Capturing…" else "Capture") }
        }
    }
}

private enum class CropEdge { NONE, LEFT, TOP, RIGHT, BOTTOM }

@Composable
private fun CropOverlay(
    bitmap: Bitmap,
    onConfirm: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var crop by remember { mutableStateOf(RectF(0.05f, 0.1f, 0.95f, 0.9f)) }
    val density = LocalDensity.current
    val touchZone = with(density) { 42.dp.toPx() }
    val minFraction = 0.15f

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color.Black)
        ) {
            val fit = rememberFitRect(bitmap, maxWidth, maxHeight)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInputCrop(crop, fit, touchZone, minFraction) { newCrop -> crop = newCrop }
            ) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Canvas(modifier = Modifier.matchParentSize()) {
                    val left = fit.left + crop.left * fit.width()
                    val top = fit.top + crop.top * fit.height()
                    val right = fit.left + crop.right * fit.width()
                    val bottom = fit.top + crop.bottom * fit.height()
                    val dim = Color.Black.copy(alpha = 0.55f)

                    drawRect(dim, topLeft = Offset(0f, 0f), size = Size(size.width, top))
                    drawRect(dim, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
                    drawRect(dim, topLeft = Offset(0f, top), size = Size(left, bottom - top))
                    drawRect(dim, topLeft = Offset(right, top), size = Size(size.width - right, bottom - top))

                    val stroke = 3.dp.toPx()
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        style = Stroke(stroke)
                    )
                    val handle = 9.dp.toPx()
                    drawCircle(Color.White, handle, Offset(left, top))
                    drawCircle(Color.White, handle, Offset(right, top))
                    drawCircle(Color.White, handle, Offset(left, bottom))
                    drawCircle(Color.White, handle, Offset(right, bottom))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = { onConfirm(cropBitmap(bitmap, crop)) }, modifier = Modifier.weight(1f)) {
                Text("Crop & scan")
            }
        }
    }
}

private fun cropBitmap(bitmap: Bitmap, crop: RectF): Bitmap {
    val left = floor(crop.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
    val top = floor(crop.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
    val right = floor(crop.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
    val bottom = floor(crop.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
    return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
}

@Composable
private fun rememberFitRect(
    bitmap: Bitmap,
    boxWidth: androidx.compose.ui.unit.Dp,
    boxHeight: androidx.compose.ui.unit.Dp
): RectF {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val widthPx = with(density) { boxWidth.toPx() }
    val heightPx = with(density) { boxHeight.toPx() }
    val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    val boxAspect = widthPx / heightPx
    return if (imageAspect > boxAspect) {
        val drawH = widthPx / imageAspect
        RectF(0f, (heightPx - drawH) / 2f, widthPx, (heightPx + drawH) / 2f)
    } else {
        val drawW = heightPx * imageAspect
        RectF((widthPx - drawW) / 2f, 0f, (widthPx + drawW) / 2f, heightPx)
    }
}

private fun Modifier.pointerInputCrop(
    crop: RectF,
    fit: RectF,
    touchZone: Float,
    minFraction: Float,
    onChange: (RectF) -> Unit
): Modifier = pointerInput(crop, fit) {
    var edge = CropEdge.NONE
    fun edgeAt(pos: Offset): CropEdge {
        val left = fit.left + crop.left * fit.width()
        val top = fit.top + crop.top * fit.height()
        val right = fit.left + crop.right * fit.width()
        val bottom = fit.top + crop.bottom * fit.height()
        val dL = kotlin.math.abs(pos.x - left)
        val dR = kotlin.math.abs(pos.x - right)
        val dT = kotlin.math.abs(pos.y - top)
        val dB = kotlin.math.abs(pos.y - bottom)
        if (dL < touchZone && dT < touchZone) return CropEdge.LEFT
        if (dR < touchZone && dT < touchZone) return CropEdge.TOP
        if (dL < touchZone && dB < touchZone) return CropEdge.BOTTOM
        if (dR < touchZone && dB < touchZone) return CropEdge.RIGHT
        val min = minOf(dL, dR, dT, dB)
        return when (min) {
            dL -> CropEdge.LEFT
            dR -> CropEdge.RIGHT
            dT -> CropEdge.TOP
            else -> CropEdge.BOTTOM
        }
    }
    detectDragGestures(
        onDragStart = { pos -> edge = edgeAt(pos) },
        onDrag = { _: PointerInputChange, amount ->
            if (edge == CropEdge.NONE) return@detectDragGestures
            val dx = amount.x / fit.width()
            val dy = amount.y / fit.height()
            val next = RectF(crop)
            when (edge) {
                CropEdge.LEFT -> next.left = (next.left + dx).coerceIn(0f, next.right - minFraction)
                CropEdge.RIGHT -> next.right = (next.right + dx).coerceIn(next.left + minFraction, 1f)
                CropEdge.TOP -> next.top = (next.top + dy).coerceIn(0f, next.bottom - minFraction)
                CropEdge.BOTTOM -> next.bottom = (next.bottom + dy).coerceIn(next.top + minFraction, 1f)
                CropEdge.NONE -> {}
            }
            onChange(next)
        }
    )
}

@Composable
private fun ResultSection(
    state: ScanUiState,
    onEditText: (String) -> Unit,
    onSolve: () -> Unit,
    onSave: () -> Unit,
    onRetake: () -> Unit,
    onClearError: () -> Unit,
    onOpenCalculator: (() -> Unit)? = null
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Detected text", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.detectedText,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text("Expression", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.editedText,
                onValueChange = onEditText,
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onSolve() })
            )
        }
        state.warning?.let {
            item { NoticeBanner(it) }
        }
        state.errorMessage?.let {
            item { NoticeBanner(it, isError = true) }
            item { TextButton(onClick = onClearError) { Text("Dismiss") } }
        }
        if (state.savedNotice) {
            item { NoticeBanner("Saved to history ✓", isError = false) }
        }
        item {
            Button(
                onClick = onSolve,
                enabled = !state.solving && state.editedText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.solving) "Solving…" else "Solve")
            }
        }
        state.answer?.let { answer ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            "Answer",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            answer.headline,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        answer.details.forEach { line ->
                            Text(
                                line,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (onOpenCalculator != null) {
                                TextButton(onClick = {
                                    com.allcalc.app.utils.ShortcutBus.calculator.value =
                                        com.allcalc.app.utils.ShortcutBus.CalcPreset(state.editedText, answer.headline)
                                    onOpenCalculator()
                                }) {
                                    Text("Open in Calculator")
                                }
                            }
                            TextButton(onClick = {
                                clipboard.setText(AnnotatedString(answer.headline))
                                copied = true
                            }) {
                                Text(if (copied) "Copied ✓" else "Copy answer")
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                    Text("Retake / Repick")
                }
                Button(
                    onClick = onSave,
                    enabled = state.answer != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save to history")
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}