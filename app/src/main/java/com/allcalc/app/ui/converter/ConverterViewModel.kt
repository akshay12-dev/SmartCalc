package com.allcalc.app.ui.converter

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allcalc.app.data.local.HistoryEntity
import com.allcalc.app.data.local.HistoryRepository
import com.allcalc.app.data.local.HistoryTypes
import com.allcalc.app.data.preferences.PreferencesRepository
import com.allcalc.app.domain.converter.ConversionCategory
import com.allcalc.app.domain.converter.ConversionEngine
import com.allcalc.app.utils.ShortcutBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

data class ConverterUiState(
    val category: ConversionCategory = ConversionCategory.LENGTH,
    val from: String = "ft",
    val to: String = "m",
    val input: String = "1",
    val result: String? = null,
    val errorMessage: String? = null,
    val recent: List<HistoryEntity> = emptyList(),
    val savedNotice: Boolean = false
)

class ConverterViewModel(
    private val history: HistoryRepository,
    private val prefs: PreferencesRepository,
    private val engine: ConversionEngine
) : ViewModel() {

    private val _state = MutableStateFlow(ConverterUiState())
    val state: StateFlow<ConverterUiState> = _state.asStateFlow()

    private var precision = 4
    private var lastSavedKey: String? = null

    private companion object {
        /** Decimal places used when the precision setting is "auto" (0). */
        const val AUTO_PRECISION = 6
    }

    init {
        viewModelScope.launch {
            prefs.decimalPrecision.collect {
                // 0 means "auto" everywhere else in the app, so keep a sensible default here too.
                precision = if (it <= 0) AUTO_PRECISION else it.coerceIn(1, 10)
                recompute()
            }
        }
        viewModelScope.launch {
            history.observeByType(HistoryTypes.CONVERSION, 6).collect { recent ->
                _state.update { it.copy(recent = recent) }
            }
        }
        viewModelScope.launch {
            ShortcutBus.converter.collect { preset ->
                if (preset != null) {
                    _state.update {
                        it.copy(
                            category = preset.category,
                            from = preset.from,
                            to = preset.to,
                            input = preset.value ?: it.input,
                            errorMessage = null
                        )
                    }
                    recompute()
                    ShortcutBus.converter.value = null
                }
            }
        }
        viewModelScope.launch {
            snapshotFlow {
                listOf(_state.value.category, _state.value.from, _state.value.to, _state.value.input, precision)
            }
                .debounce(1500)
                .collect { saveIfPossible() }
        }
    }

    fun setCategory(category: ConversionCategory) {
        val units = ConversionEngine.units(category)
        val from = units.first().symbol
        val to = units.getOrNull(1)?.symbol ?: from
        _state.update { it.copy(category = category, from = from, to = to, errorMessage = null) }
        recompute()
    }

    fun setInput(text: String) {
        _state.update { it.copy(input = text, errorMessage = null) }
        recompute()
    }

    fun setFrom(symbol: String) {
        _state.update { it.copy(from = symbol, errorMessage = null) }
        recompute()
    }

    fun setTo(symbol: String) {
        _state.update { it.copy(to = symbol, errorMessage = null) }
        recompute()
    }

    fun swap() {
        _state.update { it.copy(from = it.to, to = it.from) }
        recompute()
    }

    fun clear() {
        lastSavedKey = null
        _state.update { it.copy(input = "", result = null, errorMessage = null) }
    }

    fun saveNow() {
        val s = _state.value
        val value = s.input.trim().toDoubleOrNull()
        val result = s.result ?: return
        if (value == null) return
        val label = "${s.input.trim()} ${s.from} → $result ${s.to}"
        lastSavedKey = label
        _state.update { it.copy(savedNotice = true) }
        viewModelScope.launch {
            history.add(
                HistoryEntity(
                    type = HistoryTypes.CONVERSION,
                    category = s.category.name,
                    label = label,
                    expression = "${s.input.trim()} ${s.from}",
                    result = "$result ${s.to}"
                )
            )
            kotlinx.coroutines.delay(2000)
            _state.update { it.copy(savedNotice = false) }
        }
    }

    private fun recompute() {
        val s = _state.value
        val value = s.input.trim().toDoubleOrNull()
        val from = ConversionEngine.find(s.category, s.from)
        val to = ConversionEngine.find(s.category, s.to)
        if (value == null || from == null || to == null) {
            _state.update {
                it.copy(
                    result = null,
                    errorMessage = if (s.input.isBlank()) null else "Enter a valid number"
                )
            }
            return
        }
        val converted = try {
            engine.convert(value, from, to)
        } catch (e: Exception) {
            _state.update { it.copy(result = null, errorMessage = e.message ?: "Conversion error") }
            return
        }
        if (!converted.isFinite()) {
            _state.update { it.copy(result = null, errorMessage = "Result is undefined or too large") }
            return
        }
        val scaled = BigDecimal.valueOf(converted)
            .setScale(precision, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        _state.update { it.copy(result = scaled, errorMessage = null) }
    }

    private fun saveIfPossible() {
        val s = _state.value
        val value = s.input.trim().toDoubleOrNull()
        val result = s.result ?: return
        if (value == null) return
        val label = "${s.input.trim()} ${s.from} → $result ${s.to}"
        if (lastSavedKey == label) return
        lastSavedKey = label
        viewModelScope.launch {
            history.add(
                HistoryEntity(
                    type = HistoryTypes.CONVERSION,
                    category = s.category.name,
                    label = label,
                    expression = "${s.input.trim()} ${s.from}",
                    result = "$result ${s.to}"
                )
            )
        }
    }
}