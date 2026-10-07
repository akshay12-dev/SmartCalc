package com.allcalc.app.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.allcalc.app.ui.common.KeyButton
import com.allcalc.app.ui.common.NoticeBanner
import com.allcalc.app.ui.common.SciKey
import com.allcalc.app.ui.common.ScreenHeader
import com.allcalc.app.utils.AppViewModelProvider
import kotlinx.coroutines.delay

private val sciFunctions = listOf(
    "sin(", "cos(", "tan(", "asin(", "acos(", "atan(",
    "ln(", "log(", "sqrt(", "abs(", "^", "!", "π", "e", "|"
)

private val keypad: List<List<Pair<String, KeyMode>>> = listOf(
    listOf("AC" to KeyMode.AC, "(" to KeyMode.OP, ")" to KeyMode.OP, "÷" to KeyMode.OP),
    listOf("7" to KeyMode.DIGIT, "8" to KeyMode.DIGIT, "9" to KeyMode.DIGIT, "×" to KeyMode.OP),
    listOf("4" to KeyMode.DIGIT, "5" to KeyMode.DIGIT, "6" to KeyMode.DIGIT, "-" to KeyMode.OP),
    listOf("1" to KeyMode.DIGIT, "2" to KeyMode.DIGIT, "3" to KeyMode.DIGIT, "+" to KeyMode.OP),
    listOf("0" to KeyMode.DIGIT, "." to KeyMode.DIGIT, "⌫" to KeyMode.BACK, "=" to KeyMode.EQUALS)
)

private enum class KeyMode { DIGIT, OP, AC, BACK, EQUALS }

@Composable
fun CalculatorScreen(onHome: () -> Unit) {
    val viewModel: CalculatorViewModel = viewModel(factory = AppViewModelProvider)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var copiedResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedResult) {
        copiedResult?.let { marker ->
            delay(1500)
            if (copiedResult == marker) copiedResult = null
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val rowHeight = ((maxHeight - 208.dp).coerceAtLeast(0.dp) / 5f).coerceIn(44.dp, 76.dp)

        Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (state.angleModeDeg) "Calculator · DEG" else "Calculator · RAD",
            onHome = onHome,
            trailing = {
                TextButton(onClick = viewModel::toggleAngleMode) {
                    Text(
                        text = if (state.angleModeDeg) "\u2192 RAD" else "\u2192 DEG",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.Bottom
        ) {
            state.lastExpression?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.expression.ifEmpty { "0" },
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (state.expression.isNotEmpty()) {
                    IconButton(onClick = {
                        val textToCopy = state.expression
                        clipboard.setText(AnnotatedString(textToCopy))
                        copiedResult = textToCopy
                    }) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy expression",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            state.result?.let { result ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "= $result",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (copiedResult != null) {
                Text(
                    text = "Copied to clipboard",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            state.errorMessage?.let {
                NoticeBanner(it, isError = true)
                Spacer(Modifier.height(4.dp))
            }
        }

        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(sciFunctions) { fn ->
                SciKey(label = fn.trimEnd('(')) {
                    if (fn.endsWith("(")) viewModel.insertFunction(fn) else viewModel.insertOperator(fn)
                }
            }
        }

        keypad.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, mode) ->
                    val displayLabel = if (label == "AC") {
                        if (state.expression.isNotEmpty()) "C" else "AC"
                    } else label

                    val style = when (mode) {
                        KeyMode.DIGIT -> com.allcalc.app.ui.common.KeyStyle.DIGIT
                        KeyMode.OP -> com.allcalc.app.ui.common.KeyStyle.OPERATOR
                        KeyMode.AC, KeyMode.BACK -> com.allcalc.app.ui.common.KeyStyle.ACTION
                        KeyMode.EQUALS -> com.allcalc.app.ui.common.KeyStyle.EQUALS
                    }

                    KeyButton(
                        label = displayLabel,
                        modifier = Modifier.weight(1f),
                        keyStyle = style,
                        onLongClick = if (mode == KeyMode.BACK) { { viewModel.clear() } } else null,
                        onClick = {
                            when (mode) {
                                KeyMode.DIGIT -> viewModel.insertDigit(label)
                                KeyMode.OP -> viewModel.insertOperator(label)
                                KeyMode.AC -> viewModel.clear()
                                KeyMode.BACK -> viewModel.backspace()
                                KeyMode.EQUALS -> viewModel.onEquals()
                            }
                        }
                    )
                }
            }
        }
        }
}
}