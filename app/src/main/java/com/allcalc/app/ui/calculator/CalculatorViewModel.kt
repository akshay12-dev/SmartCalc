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
    val angleModeDeg: Boolean = true,
    /** Insertion point inside [expression], between 0 and expression.length. */
    val cursor: Int = 0,
    /** Selected range (start until end) of [expression], or null when nothing is selected. */
    val selection: Pair<Int, Int>? = null
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
                            angleModeDeg = it.angleModeDeg,
                            cursor = preset.result.length
                        )
                    }
                    clearOnNext = true
                    ShortcutBus.calculator.value = null
                }
            }
        }
    }

    /** Places the editing caret at [index] (used when the user taps the expression). */
    fun setCursor(index: Int) {
        // Touching the expression means the user wants to edit what is shown, not start over.
        clearOnNext = false
        val length = _state.value.expression.length
        _state.update {
            it.copy(cursor = index.coerceIn(0, length), selection = null)
        }
    }

    /** Selects the number/name under [index]; used by long-press to delete or replace it. */
    fun selectTokenAt(index: Int) {
        val expr = _state.value.expression
        if (expr.isEmpty()) return
        val probe = index.coerceIn(0, expr.length).coerceAtMost(expr.length - 1)
        val ch = expr[probe]
        var start = probe
        var end = probe + 1
        when {
            ch.isDigit() || ch == '.' -> {
                while (start > 0 && (expr[start - 1].isDigit() || expr[start - 1] == '.')) start--
                while (end < expr.length && (expr[end].isDigit() || expr[end] == '.')) end++
            }
            ch.isLetter() -> {
                while (start > 0 && expr[start - 1].isLetter()) start--
                while (end < expr.length && expr[end].isLetter()) end++
            }
        }
        clearOnNext = false
        _state.update { it.copy(selection = start to end, cursor = end) }
    }

    fun insertDigit(digit: String) {
        if (clearOnNext) startNew()
        val st = _state.value
        var expr = st.expression
        var cursor = st.cursor.coerceIn(0, expr.length)
        selectionRange(st)?.let { (start, end) ->
            expr = expr.substring(0, start) + expr.substring(end)
            cursor = start
        }

        if (expr == "0" && cursor == 1 && digit != ".") {
            _state.update {
                it.copy(expression = digit, cursor = 1, selection = null, errorMessage = null)
            }
            recompute()
            return
        }

        // Only the number token the caret sits in matters for the duplicate-decimal check.
        var start = cursor
        while (start > 0 && (expr[start - 1].isDigit() || expr[start - 1] == '.')) start--
        var end = cursor
        while (end < expr.length && (expr[end].isDigit() || expr[end] == '.')) end++
        if (digit == "." &&
            (expr.substring(start, cursor).contains('.') || expr.substring(cursor, end).contains('.'))
        ) return

        val newExpr = expr.substring(0, cursor) + digit + expr.substring(cursor)
        _state.update {
            it.copy(expression = newExpr, cursor = cursor + 1, selection = null, errorMessage = null)
        }
        recompute()
    }

    fun insertFunction(name: String) {
        if (clearOnNext) {
            _state.update {
                it.copy(expression = "", lastExpression = null, result = null, cursor = 0, selection = null)
            }
            clearOnNext = false
        }
        val st = _state.value
        var expr = st.expression
        var cursor = st.cursor.coerceIn(0, expr.length)
        selectionRange(st)?.let { (start, end) ->
            expr = expr.substring(0, start) + expr.substring(end)
            cursor = start
        }
        val newExpr = expr.substring(0, cursor) + name + expr.substring(cursor)
        _state.update {
            it.copy(
                expression = newExpr,
                cursor = cursor + name.length,
                selection = null,
                errorMessage = null
            )
        }
        recompute()
    }

    fun insertOperator(op: String) {
        if (op == "%") {
            insertPercent()
            return
        }
        val st = _state.value
        if (clearOnNext) {
            clearOnNext = false
            val isNumber = st.expression.toDoubleOrNull() != null
            if (!isNumber) {
                _state.update {
                    it.copy(
                        expression = op,
                        cursor = op.length,
                        lastExpression = null,
                        result = null,
                        selection = null,
                        errorMessage = null
                    )
                }
                recompute()
                return
            }
        }
        var expr = st.expression
        var cursor = st.cursor.coerceIn(0, expr.length)
        selectionRange(st)?.let { (start, end) ->
            expr = expr.substring(0, start) + expr.substring(end)
            cursor = start
        }
        val before = expr.substring(0, cursor)
        val after = expr.substring(cursor)
        val endsWithOp = before.isNotEmpty() && before.last() in "+-×÷*/^"
        val newBefore = if (endsWithOp && op in "+-×÷*/^") before.dropLast(1) + op else before + op
        _state.update {
            it.copy(
                expression = newBefore + after,
                cursor = newBefore.length,
                lastExpression = null,
                result = null,
                selection = null,
                errorMessage = null
            )
        }
        recompute()
    }

    /**
     * % is a postfix operator. It is only appended after a value, so key presses that would
     * produce an unparseable expression (e.g. "+%") are ignored.
     */
    private fun insertPercent() {
        val st = _state.value
        var expr = st.expression
        var cursor = st.cursor.coerceIn(0, expr.length)
        selectionRange(st)?.let { (start, end) ->
            expr = expr.substring(0, start) + expr.substring(end)
            cursor = start
        }
        val before = expr.substring(0, cursor)
        val last = before.lastOrNull() ?: return
        if (last in "+-×÷*/^(=%") return
        clearOnNext = false
        val newExpr = before + "%" + expr.substring(cursor)
        _state.update {
            it.copy(
                expression = newExpr,
                cursor = cursor + 1,
                lastExpression = null,
                result = null,
                selection = null,
                errorMessage = null
            )
        }
        recompute()
    }

    /** x² — squares the number at the caret (or the selected range). */
    fun square() {
        clearOnNext = false
        val st = _state.value
        val expr = st.expression
        val cursor = st.cursor.coerceIn(0, expr.length)
        val sel = selectionRange(st)
        val newExpr: String
        val newCursor: Int
        if (sel != null) {
            val replacement = "(${expr.substring(sel.first, sel.second)})^2"
            newExpr = expr.substring(0, sel.first) + replacement + expr.substring(sel.second)
            newCursor = sel.first + replacement.length
        } else {
            val (rawStart, rawEnd) = numberBounds(expr, cursor)
            if (rawStart == rawEnd) {
                newExpr = expr.substring(0, cursor) + "^2" + expr.substring(cursor)
                newCursor = cursor + 2
            } else {
                val (start, end) = numberBoundsWithSign(expr, rawStart, rawEnd)
                val replacement = "(${expr.substring(start, end)})^2"
                newExpr = expr.substring(0, start) + replacement + expr.substring(end)
                newCursor = start + replacement.length
            }
        }
        _state.update {
            it.copy(expression = newExpr, cursor = newCursor, selection = null, errorMessage = null)
        }
        recompute()
    }

    /** ± — flips the sign of the number at the caret (or the selected range). */
    fun toggleSign() {
        clearOnNext = false
        val st = _state.value
        var expr = st.expression
        if (expr.isEmpty()) {
            _state.update {
                it.copy(expression = "-", cursor = 1, selection = null, errorMessage = null)
            }
            return
        }
        var cursor = st.cursor.coerceIn(0, expr.length)
        val sel = selectionRange(st)
        val (start, end) = sel ?: numberBounds(expr, cursor)
        when {
            start == end -> {
                // No number under the caret: insert a unary minus there.
                expr = expr.substring(0, cursor) + "-" + expr.substring(cursor)
                cursor += 1
            }
            hasUnaryMinusBefore(expr, start) -> {
                expr = expr.substring(0, start - 1) + expr.substring(start)
                cursor = (cursor - 1).coerceAtLeast(0)
            }
            else -> {
                expr = expr.substring(0, start) + "-" + expr.substring(start)
                cursor = if (sel != null) start + 1 else cursor + 1
            }
        }
        _state.update {
            it.copy(
                expression = expr,
                cursor = cursor.coerceIn(0, expr.length),
                selection = null,
                errorMessage = null
            )
        }
        recompute()
    }

    /** 1/x — wraps the number at the caret (or the selected range) in a reciprocal. */
    fun invertValue() {
        clearOnNext = false
        val st = _state.value
        val expr = st.expression
        var cursor = st.cursor.coerceIn(0, expr.length)
        val sel = selectionRange(st)
        val newExpr: String
        if (sel != null) {
            val replacement = "1/(${expr.substring(sel.first, sel.second)})"
            newExpr = expr.substring(0, sel.first) + replacement + expr.substring(sel.second)
            cursor = sel.first + replacement.length
        } else {
            val (rawStart, rawEnd) = numberBounds(expr, cursor)
            if (rawStart == rawEnd) {
                val insertion = "1/("
                newExpr = expr.substring(0, cursor) + insertion + expr.substring(cursor)
                cursor += insertion.length
            } else {
                val (start, end) = numberBoundsWithSign(expr, rawStart, rawEnd)
                val replacement = "1/(${expr.substring(start, end)})"
                newExpr = expr.substring(0, start) + replacement + expr.substring(end)
                cursor = start + replacement.length
            }
        }
        _state.update {
            it.copy(
                expression = newExpr,
                cursor = cursor.coerceIn(0, newExpr.length),
                selection = null,
                errorMessage = null
            )
        }
        recompute()
    }

    /** Bounds of the number token touching [cursor]. */
    private fun numberBounds(expr: String, cursor: Int): Pair<Int, Int> {
        var start = cursor.coerceIn(0, expr.length)
        var end = start
        while (start > 0 && (expr[start - 1].isDigit() || expr[start - 1] == '.')) start--
        while (end < expr.length && (expr[end].isDigit() || expr[end] == '.')) end++
        return start to end
    }

    /** Extends a number range to include a directly preceding unary minus, e.g. -5. */
    private fun numberBoundsWithSign(expr: String, start: Int, end: Int): Pair<Int, Int> {
        val s = if (hasUnaryMinusBefore(expr, start)) start - 1 else start
        return s to end
    }

    private fun hasUnaryMinusBefore(expr: String, start: Int): Boolean =
        start > 0 && expr[start - 1] == '-' &&
            (start - 1 == 0 || expr[start - 2] in "+-×÷*/^(")

    private fun selectionRange(st: CalculatorUiState): Pair<Int, Int>? {
        val (start, end) = st.selection ?: return null
        val a = start.coerceIn(0, st.expression.length)
        val b = end.coerceIn(0, st.expression.length)
        return if (b > a) a to b else null
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
                    errorMessage = null,
                    cursor = text.length,
                    selection = null
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
                _state.update {
                    it.copy(
                        expression = last,
                        lastExpression = null,
                        result = null,
                        cursor = last.length,
                        selection = null
                    )
                }
                recompute()
                return
            }
        }
        val st = _state.value
        val expr = st.expression

        selectionRange(st)?.let { (start, end) ->
            val newExpr = expr.substring(0, start) + expr.substring(end)
            _state.update {
                it.copy(expression = newExpr, cursor = start, selection = null, errorMessage = null)
            }
            recompute()
            return
        }

        val cursor = st.cursor.coerceIn(0, expr.length)
        if (cursor == 0) return
        val newExpr = expr.substring(0, cursor - 1) + expr.substring(cursor)
        _state.update {
            it.copy(expression = newExpr, cursor = cursor - 1, selection = null, errorMessage = null)
        }
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
        _state.update {
            it.copy(expression = "", lastExpression = null, result = null, cursor = 0, selection = null)
        }
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