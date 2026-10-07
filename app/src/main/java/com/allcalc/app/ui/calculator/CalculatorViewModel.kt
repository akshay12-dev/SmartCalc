package com.allcalc.app.ui.calculator

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
import com.allcalc.app.utils.ShortcutBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val expression: String = "",
    val lastExpression: String? = null,
    val result: String? = null,
    val errorMessage: String? = null,
    val angleModeDeg: Boolean = true
)

class CalculatorViewModel(
    private val engine: ExpressionEngine,
    private val history: HistoryRepository,
    private val prefs: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorUiState())
    val state: StateFlow<CalculatorUiState> = _state.asStateFlow()

    private var clearOnNext = false
    private var precision: Int = 12

    init {
        viewModelScope.launch {
            prefs.angleMode.collect { mode ->
                _state.update { it.copy(angleModeDeg = mode == 0) }
                recompute()
            }
        }
        viewModelScope.launch {
            prefs.decimalPrecision.collect { p ->
                precision = if (p <= 0) 12 else p.coerceIn(1, 12)
                recompute()
            }
        }
        viewModelScope.launch {
            ShortcutBus.calculator.collect { preset ->
                if (preset != null) {
                    _state.update {
                        CalculatorUiState(
                            expression = preset.result,
                            lastExpression = preset.expression,
                            result = null,
                            angleModeDeg = it.angleModeDeg
                        )
                    }
                    clearOnNext = true
                    ShortcutBus.calculator.value = null
                }
            }
        }
    }

    fun insertDigit(digit: String) {
        if (clearOnNext) startNew()
        val expr = _state.value.expression
        if (expr == "0" && digit != ".") {
            _state.update { it.copy(expression = digit, errorMessage = null) }
            recompute()
            return
        }
        val lastOp = expr.indexOfLast { it in "+-*/^%()!×÷" }
        val currentNumber = if (lastOp >= 0) expr.substring(lastOp + 1) else expr
        if (digit == "." && currentNumber.contains(".")) return
        _state.update { it.copy(expression = it.expression + digit, errorMessage = null) }
        recompute()
    }

    fun insertFunction(name: String) {
        if (clearOnNext) {
            _state.update { it.copy(expression = "", lastExpression = null, result = null) }
            clearOnNext = false
        }
        _state.update { it.copy(expression = it.expression + name, errorMessage = null) }
        recompute()
    }

    fun insertOperator(op: String) {
        val st = _state.value
        if (clearOnNext) {
            clearOnNext = false
            val isNumber = st.expression.toDoubleOrNull() != null
            if (isNumber) {
                _state.update { it.copy(expression = it.expression + op, lastExpression = null, result = null, errorMessage = null) }
            } else {
                _state.update { it.copy(expression = op, lastExpression = null, result = null, errorMessage = null) }
            }
        } else {
            val expr = st.expression
            val endsWithOp = expr.isNotEmpty() && expr.last() in "+-×÷*/^"
            val newExpr = if (endsWithOp && op in "+-×÷*/^") {
                expr.dropLast(1) + op
            } else {
                expr + op
            }
            _state.update { it.copy(expression = newExpr, errorMessage = null) }
        }
        recompute()
    }

    fun onEquals() {
        if (clearOnNext) return
        val raw = _state.value.expression.trim()
        if (raw.isEmpty()) return
        val expression = engine.autocomplete(raw)
        try {
            val evaluated = engine.evaluate(expression, _state.value.angleModeDeg, precision)
            val text = toText(evaluated)
            _state.update {
                it.copy(
                    expression = text,
                    lastExpression = expression,
                    result = null,
                    errorMessage = null
                )
            }
            clearOnNext = true
            viewModelScope.launch {
                history.add(
                    HistoryEntity(
                        type = HistoryTypes.CALC,
                        category = "",
                        label = expression,
                        expression = expression,
                        result = text
                    )
                )
            }
        } catch (e: EvaluationException) {
            _state.update { it.copy(errorMessage = e.message) }
        }
    }

    fun backspace() {
        if (clearOnNext) {
            clearOnNext = false
            val last = _state.value.lastExpression
            if (!last.isNullOrEmpty()) {
                _state.update { it.copy(expression = last, lastExpression = null, result = null) }
                recompute()
                return
            }
        }
        _state.update { it.copy(expression = it.expression.dropLast(1), errorMessage = null) }
        recompute()
    }

    fun clear() {
        clearOnNext = false
        _state.update { CalculatorUiState(angleModeDeg = it.angleModeDeg) }
    }

    fun toggleAngleMode() {
        val next = if (_state.value.angleModeDeg) 1 else 0
        viewModelScope.launch { prefs.setAngleMode(next) }
    }

    private fun startNew() {
        clearOnNext = false
        _state.update { it.copy(expression = "", lastExpression = null, result = null) }
    }

    private fun recompute() {
        val raw = _state.value.expression.trim()
        if (raw.isEmpty()) {
            _state.update { it.copy(result = null, errorMessage = null) }
            return
        }
        try {
            val full = engine.autocomplete(raw)
            val evaluated = engine.evaluate(full, _state.value.angleModeDeg, precision)
            val preview = toText(evaluated)
            // If the expression is already a solved value, don't show identical live preview
            _state.update { it.copy(result = if (preview == raw) null else preview, errorMessage = null) }
        } catch (e: EvaluationException) {
            _state.update { it.copy(result = null) }
        }
    }

    private fun toText(result: EvalResult): String = when (result) {
        is EvalResult.Value -> formatSmart(result.value, precision)
        is EvalResult.Solved -> "${result.variable} = ${formatSmart(result.value, precision)}"
        is EvalResult.Equality -> if (result.holds) "True" else "False"
    }
}