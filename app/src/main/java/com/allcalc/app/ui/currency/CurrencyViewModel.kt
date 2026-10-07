package com.allcalc.app.ui.currency

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allcalc.app.data.currency.CurrencyRepository
import com.allcalc.app.data.local.HistoryEntity
import com.allcalc.app.data.local.HistoryRepository
import com.allcalc.app.data.local.HistoryTypes
import com.allcalc.app.data.preferences.CurrencyCache
import com.allcalc.app.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

data class CurrencyUiState(
    val cache: CurrencyCache? = null,
    val from: String = "USD",
    val to: String = "INR",
    val amount: String = "1",
    val result: String? = null,
    val errorMessage: String? = null,
    val refreshing: Boolean = false,
    val savedNotice: Boolean = false
)

class CurrencyViewModel(
    private val repository: CurrencyRepository,
    private val prefs: PreferencesRepository,
    private val history: HistoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CurrencyUiState())
    val state: StateFlow<CurrencyUiState> = _state.asStateFlow()

    private var lastSavedKey: String? = null

    init {
        viewModelScope.launch {
            val initial = repository.loadCache()
            _state.update { it.copy(cache = initial) }
            recompute()

            val auto = prefs.autoCurrency.first()
            val stale = initial.updatedAt == 0L ||
                System.currentTimeMillis() - initial.updatedAt > 24L * 60 * 60 * 1000
            if (auto && stale) refresh()
        }
        viewModelScope.launch {
            prefs.currencyCache.collect { cache ->
                if (cache != null) {
                    _state.update { it.copy(cache = cache) }
                    recompute()
                }
            }
        }
        viewModelScope.launch {
            snapshotFlow {
                listOf(
                    _state.value.from,
                    _state.value.to,
                    _state.value.amount,
                    _state.value.result
                )
            }
                .debounce(1500)
                .collect { saveIfPossible() }
        }
    }

    fun setAmount(text: String) {
        _state.update { it.copy(amount = text) }
        recompute()
    }

    fun setFrom(code: String) {
        _state.update { it.copy(from = code) }
        recompute()
    }

    fun setTo(code: String) {
        _state.update { it.copy(to = code) }
        recompute()
    }

    fun swap() {
        _state.update { it.copy(from = it.to, to = it.from) }
        recompute()
    }

    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.refresh()) {
                is CurrencyRepository.FetchResult.Success -> {
                    _state.update { it.copy(cache = result.cache, refreshing = false) }
                    recompute()
                }
                is CurrencyRepository.FetchResult.Failure -> {
                    _state.update { it.copy(refreshing = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun saveNow() {
        val s = _state.value
        val amountText = s.amount.trim()
        if (amountText.toDoubleOrNull() == null) return
        val result = s.result ?: return
        val label = "$amountText ${s.from} → $result ${s.to}"
        lastSavedKey = label
        _state.update { it.copy(savedNotice = true) }
        viewModelScope.launch {
            history.add(
                HistoryEntity(
                    type = HistoryTypes.CONVERSION,
                    category = "CURRENCY",
                    label = label,
                    expression = "$amountText ${s.from}",
                    result = "$result ${s.to}"
                )
            )
            kotlinx.coroutines.delay(2000)
            _state.update { it.copy(savedNotice = false) }
        }
    }

    private fun saveIfPossible() {
        val s = _state.value
        val amountText = s.amount.trim()
        if (amountText.toDoubleOrNull() == null) return
        val result = s.result ?: return
        val label = "$amountText ${s.from} → $result ${s.to}"
        if (lastSavedKey == label) return
        lastSavedKey = label
        viewModelScope.launch {
            history.add(
                HistoryEntity(
                    type = HistoryTypes.CONVERSION,
                    category = "CURRENCY",
                    label = label,
                    expression = "$amountText ${s.from}",
                    result = "$result ${s.to}"
                )
            )
        }
    }

    private fun recompute() {
        val s = _state.value
        val cache = s.cache ?: run {
            _state.update { it.copy(result = null) }
            return
        }
        val amount = s.amount.trim().toDoubleOrNull() ?: run {
            _state.update { it.copy(result = null) }
            return
        }
        val value = try {
            repository.convert(amount, s.from, s.to, cache.rates)
        } catch (e: Exception) {
            _state.update { it.copy(result = null, errorMessage = e.message) }
            return
        }
        if (!value.isFinite()) {
            _state.update { it.copy(result = null, errorMessage = "Rate calculation exceeds valid range") }
            return
        }
        _state.update { it.copy(result = formatMoney(value), errorMessage = null) }
    }

    private fun formatMoney(value: Double): String {
        if (!value.isFinite()) return "—"
        val bd = BigDecimal.valueOf(value)
        val scale = if (bd.abs() >= BigDecimal.ONE) 2 else 4
        return bd.setScale(scale, RoundingMode.HALF_UP).toPlainString()
    }
}