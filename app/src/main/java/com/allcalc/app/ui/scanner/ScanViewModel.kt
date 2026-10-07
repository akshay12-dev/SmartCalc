package com.allcalc.app.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allcalc.app.data.local.HistoryEntity
import com.allcalc.app.data.local.HistoryRepository
import com.allcalc.app.data.local.HistoryTypes
import com.allcalc.app.data.preferences.PreferencesRepository
import com.allcalc.app.domain.calculator.EvalResult
import com.allcalc.app.domain.calculator.EvaluationException
import com.allcalc.app.domain.calculator.ExpressionEngine
import com.allcalc.app.domain.calculator.formatSmart
import com.allcalc.app.domain.scanner.MathNormalizer
import com.allcalc.app.utils.ShortcutBus
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class ScanPhase { CAMERA, CROP, RESULT }

data class ScanResult(val headline: String, val details: List<String>)

data class ScanUiState(
    val phase: ScanPhase = ScanPhase.CAMERA,
    val image: Bitmap? = null,
    val ocrBusy: Boolean = false,
    val detectedText: String = "",
    val editedText: String = "",
    val warning: String? = null,
    val solving: Boolean = false,
    val answer: ScanResult? = null,
    val errorMessage: String? = null,
    val angleModeDeg: Boolean = true,
    val savedNotice: Boolean = false
)

class ScanViewModel(
    private val appContext: Context,
    private val engine: ExpressionEngine,
    private val normalizer: MathNormalizer,
    private val history: HistoryRepository,
    private val prefs: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    init {
        viewModelScope.launch {
            prefs.angleMode.collect { mode ->
                _state.update { it.copy(angleModeDeg = mode == 0) }
            }
        }
        viewModelScope.launch {
            ShortcutBus.scanner.collect { text ->
                if (text != null) {
                    _state.update {
                        it.copy(
                            phase = ScanPhase.RESULT,
                            detectedText = text,
                            editedText = text,
                            answer = null,
                            warning = "Please verify the detected equation."
                        )
                    }
                    ShortcutBus.scanner.value = null
                }
            }
        }
    }

    fun onCaptured(image: Bitmap) {
        _state.update { it.copy(phase = ScanPhase.CROP, image = image, errorMessage = null) }
    }

    fun onCropped(cropped: Bitmap) {
        _state.update { it.copy(image = cropped, ocrBusy = true, answer = null, errorMessage = null) }
        viewModelScope.launch {
            val output = withContext(Dispatchers.Default) { recognize(cropped) }
            val text = output.first
            val confidence = output.second
            _state.update {
                it.copy(
                    ocrBusy = false,
                    phase = ScanPhase.RESULT,
                    detectedText = text,
                    editedText = normalizer.prepare(text),
                    warning = buildWarning(confidence, text),
                    answer = null
                )
            }
        }
    }

    fun onEditText(text: String) {
        _state.update { it.copy(editedText = text, answer = null) }
    }

    fun onSolve() {
        val text = _state.value.editedText.trim()
        if (text.isEmpty()) {
            _state.update { it.copy(errorMessage = "Enter the equation you want to solve") }
            return
        }
        _state.update { it.copy(solving = true, answer = null, errorMessage = null) }
        viewModelScope.launch {
            try {
                val prepared = normalizer.prepare(text)
                val result = withContext(Dispatchers.Default) {
                    engine.evaluate(prepared, _state.value.angleModeDeg, 12, allowVariables = true)
                }
                val answer = when (result) {
                    is EvalResult.Value -> {
                        val value = formatSmart(result.value, 12)
                        ScanResult(value, result.steps.ifEmpty { listOf("$prepared = $value") })
                    }
                    is EvalResult.Solved -> {
                        val value = "${result.variable} = ${formatSmart(result.value, 12)}"
                        ScanResult(value, result.steps.ifEmpty { listOf(value) })
                    }
                    is EvalResult.Equality -> {
                        val value = if (result.holds) "True" else "False"
                        ScanResult(value, result.steps)
                    }
                }
                _state.update { it.copy(solving = false, answer = answer) }
            } catch (e: EvaluationException) {
                _state.update { it.copy(solving = false, answer = null, errorMessage = e.message) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(solving = false, answer = null, errorMessage = "Could not solve it. ${e.message}")
                }
            }
        }
    }

    fun onSaveResult() {
        val state = _state.value
        val answer = state.answer ?: return
        _state.update { it.copy(savedNotice = true) }
        viewModelScope.launch {
            history.add(
                HistoryEntity(
                    type = HistoryTypes.SCAN,
                    category = "",
                    label = "Scan: ${state.editedText.trim()}",
                    expression = state.detectedText,
                    result = answer.headline
                )
            )
            kotlinx.coroutines.delay(2000)
            _state.update { it.copy(savedNotice = false) }
        }
    }

    fun onRetake() {
        _state.update {
            it.copy(phase = ScanPhase.CAMERA, image = null, answer = null, errorMessage = null)
        }
    }

    fun onCancelCrop() {
        _state.update { it.copy(phase = ScanPhase.CAMERA, image = null, errorMessage = null) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun buildWarning(confidence: Float?, text: String): String? {
        val lowConfidence = confidence != null && confidence < 0.6f
        val noNumbers = !text.any { it.isDigit() }
        return if (lowConfidence || noNumbers) {
            "Please verify the detected equation before solving."
        } else {
            null
        }
    }

    private suspend fun recognize(image: Bitmap): Pair<String, Float?> =
        suspendCancellableCoroutine { continuation ->
            val input = InputImage.fromBitmap(image, 0)
            recognizer.process(input).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val text = task.result?.text.orEmpty()
                    val confidences = task.result?.textBlocks.orEmpty()
                        .flatMap { it.lines }
                        .flatMap { it.elements }
                        .mapNotNull { it.confidence }
                    val confidence = if (confidences.isEmpty()) null else confidences.average().toFloat()
                    if (continuation.isActive) continuation.resume(text to confidence)
                } else {
                    val exception = task.exception ?: Exception("OCR failed")
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
            }
        }

    override fun onCleared() {
        super.onCleared()
        recognizer.close()
    }
}