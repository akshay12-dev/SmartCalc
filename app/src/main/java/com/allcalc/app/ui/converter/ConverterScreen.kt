package com.allcalc.app.ui.converter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.allcalc.app.domain.converter.ConversionCategory
import com.allcalc.app.domain.converter.ConversionEngine
import com.allcalc.app.ui.common.InfoNote
import com.allcalc.app.ui.common.NoticeBanner
import com.allcalc.app.ui.common.ScreenHeader
import com.allcalc.app.ui.common.SectionTitle
import com.allcalc.app.ui.common.UnitDropdown
import com.allcalc.app.utils.AppViewModelProvider

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.AssistChip

@Composable
fun ConverterScreen(
    onHome: () -> Unit,
    onOpenCurrency: (() -> Unit)? = null
) {
    val viewModel: ConverterViewModel = viewModel(factory = AppViewModelProvider)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var swapRotation by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(targetValue = swapRotation, animationSpec = tween(300))
    var copied by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Unit Converter",
            onHome = onHome,
            trailing = if (onOpenCurrency != null) {
                {
                    TextButton(onClick = onOpenCurrency) {
                        Icon(Icons.Filled.AttachMoney, contentDescription = null)
                        Text("Currency")
                    }
                }
            } else null
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ConversionCategory.entries.toList()) { category ->
                        FilterChip(
                            selected = state.category == category,
                            onClick = { viewModel.setCategory(category) },
                            label = { Text(category.title) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::setInput,
                    label = { Text("Value") },
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
                    listOf("1", "5", "10", "50", "100").forEach { quickVal ->
                        AssistChip(
                            onClick = { viewModel.setInput(quickVal) },
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
                        options = ConversionEngine.units(state.category).map { it.symbol },
                        selected = state.from,
                        onSelect = viewModel::setFrom,
                        displayOption = { sym ->
                            val unit = ConversionEngine.find(state.category, sym)
                            if (unit != null) "$sym · ${unit.name}" else sym
                        },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        swapRotation += 180f
                        viewModel.swap()
                    }) {
                        Icon(
                            Icons.Filled.SwapHoriz,
                            contentDescription = "Swap units",
                            modifier = Modifier.rotate(animatedRotation)
                        )
                    }
                    UnitDropdown(
                        label = "To",
                        options = ConversionEngine.units(state.category).map { it.symbol },
                        selected = state.to,
                        onSelect = viewModel::setTo,
                        displayOption = { sym ->
                            val unit = ConversionEngine.find(state.category, sym)
                            if (unit != null) "$sym · ${unit.name}" else sym
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            state.errorMessage?.let {
                item { NoticeBanner(it, isError = true) }
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
                        val fromUnit = ConversionEngine.find(state.category, state.from)
                        val toUnit = ConversionEngine.find(state.category, state.to)
                        if (fromUnit != null && toUnit != null) {
                            Text(
                                text = "${state.input.trim()} ${fromUnit.name} = ${state.result ?: "—"} ${toUnit.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (state.result != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(state.result.orEmpty()))
                                    copied = true
                                }) {
                                    Text(if (copied) "Copied ✓" else "Copy result")
                                }
                            }
                        }
                    }
                }
            }

            state.category.note?.let { note ->
                item { InfoNote(note) }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::clear,
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear") }
                    Button(
                        onClick = { viewModel.saveNow() },
                        enabled = state.result != null,
                        modifier = Modifier.weight(1f)
                    ) { Text("Save to history") }
                }
            }

            item { SectionTitle("Recent conversions") }
            if (state.recent.isEmpty()) {
                item { Text("No conversions saved yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(state.recent) { entry ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(entry.label, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                            Text(
                                entry.result,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}