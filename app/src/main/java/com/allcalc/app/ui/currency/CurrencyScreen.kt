package com.allcalc.app.ui.currency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.allcalc.app.data.currency.Currencies
import com.allcalc.app.ui.common.NoticeBanner
import com.allcalc.app.ui.common.ScreenHeader
import com.allcalc.app.ui.common.UnitDropdown
import com.allcalc.app.utils.AppViewModelProvider

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate

@Composable
fun CurrencyScreen(
    initialFrom: String,
    initialTo: String,
    initialValue: String,
    onBack: () -> Unit
) {
    val viewModel: CurrencyViewModel = viewModel(factory = AppViewModelProvider)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val codes = Currencies.list.map { it.code }
    var swapRotation by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(targetValue = swapRotation, animationSpec = tween(300))
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (initialFrom.isNotBlank()) viewModel.setFrom(initialFrom)
        if (initialTo.isNotBlank()) viewModel.setTo(initialTo)
        if (initialValue.isNotBlank()) viewModel.setAmount(initialValue)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Currency Converter",
            onHome = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::setAmount,
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { viewModel.saveNow() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("1", "10", "100", "500", "1000").forEach { quickVal ->
                        AssistChip(
                            onClick = { viewModel.setAmount(quickVal) },
                            label = { Text(quickVal) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UnitDropdown(
                        label = "From",
                        options = codes,
                        selected = state.from,
                        onSelect = viewModel::setFrom,
                        displayOption = { code ->
                            val c = Currencies.byCode(code)
                            "${c.flag} $code · ${c.name}"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        swapRotation += 180f
                        viewModel.swap()
                    }) {
                        Icon(
                            Icons.Filled.SwapHoriz,
                            contentDescription = "Swap currencies",
                            modifier = Modifier.rotate(animatedRotation)
                        )
                    }
                    UnitDropdown(
                        label = "To",
                        options = codes,
                        selected = state.to,
                        onSelect = viewModel::setTo,
                        displayOption = { code ->
                            val c = Currencies.byCode(code)
                            "${c.flag} $code · ${c.name}"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            state.errorMessage?.let { message ->
                item { NoticeBanner(message, isError = true) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = viewModel::refresh) { Text("Retry") }
                        TextButton(onClick = viewModel::clearError) { Text("Dismiss") }
                    }
                }
            }

            if (state.ratesUpdatedNotice) {
                item { NoticeBanner("Live rates updated ✓", isError = false) }
            }

            if (state.savedNotice) {
                item { NoticeBanner("Saved to history ✓", isError = false) }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            "Result",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = state.result ?: "—",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        val fromCurrency = Currencies.byCode(state.from)
                        val toCurrency = Currencies.byCode(state.to)
                        Text(
                            text = "${fromCurrency.flag} ${state.amount.trim()} ${state.from} = " +
                                "${state.result ?: "—"} ${state.to} ${toCurrency.flag}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (state.result != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString("${state.result.orEmpty()} ${state.to}"))
                                    copied = true
                                }) {
                                    Text(if (copied) "Copied ✓" else "Copy result")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::saveNow,
                        enabled = state.result != null,
                        modifier = Modifier.weight(1f)
                    ) { Text("Save to history") }
                    Button(
                        onClick = viewModel::refresh,
                        enabled = !state.refreshing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.refreshing) "Updating…" else "Update rates")
                    }
                }
            }

            state.cache?.let { cache ->
                if (cache.isSample) {
                    item {
                        NoticeBanner(
                            "Showing sample rates. Sample rates are clearly marked and are replaced " +
                                "by live data when you press Update."
                        )
                    }
                }
                item {
                    val ageHours = (System.currentTimeMillis() - cache.updatedAt) / 3_600_000L
                    val ageText = when {
                        cache.updatedAt == 0L -> "No live data yet"
                        ageHours < 1 -> "Updated recently"
                        ageHours < 24 -> "Updated $ageHours hours ago"
                        else -> "Updated ${ageHours / 24} days ago"
                    }
                    val stale = cache.isSample && cache.updatedAt == 0L ||
                        (cache.updatedAt > 0 && ageHours >= 24)
                    Text(
                        text = ageText + if (stale) " — rates may be outdated." else "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}