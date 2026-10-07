package com.allcalc.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.allcalc.app.data.local.HistoryEntity
import com.allcalc.app.data.local.HistoryTypes
import com.allcalc.app.ui.common.ScreenHeader
import com.allcalc.app.utils.AppViewModelProvider
import com.allcalc.app.utils.ShortcutBus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable
fun HistoryScreen(
    onHome: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenCurrency: (String, String, String) -> Unit
) {
    val viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider)
    val items by viewModel.items.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }
    val clipboard = LocalClipboardManager.current
    var copiedId by remember { mutableStateOf<Long?>(null) }

    val specializedCategories = listOf("MATH", "STATISTICS", "GEOMETRY", "FINANCIAL", "FITNESS", "OTHER")

    val filteredItems = remember(items, selectedFilter) {
        when (selectedFilter) {
            "Calculator" -> items.filter { it.type == HistoryTypes.CALC && it.category !in specializedCategories }
            "Specialized" -> items.filter { it.category in specializedCategories }
            "Financial" -> items.filter { it.category == "FINANCIAL" }
            "Fitness" -> items.filter { it.category == "FITNESS" }
            "Scan" -> items.filter { it.type == HistoryTypes.SCAN }
            "Converter" -> items.filter { it.type == HistoryTypes.CONVERSION && it.category != "CURRENCY" }
            "Currency" -> items.filter { it.type == HistoryTypes.CONVERSION && it.category == "CURRENCY" }
            else -> items
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "History",
            onHome = onHome,
            trailing = {
                if (items.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text("Clear all")
                    }
                }
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Calculator", "Specialized", "Financial", "Fitness", "Scan", "Converter", "Currency")
            items(filters.size) { idx ->
                val f = filters[idx]
                FilterChip(
                    selected = selectedFilter == f,
                    onClick = { selectedFilter = f },
                    label = { Text(f) }
                )
            }
        }

        if (confirmClear) {
            AlertDialog(
                onDismissRequest = { confirmClear = false },
                title = { Text("Clear history?") },
                text = { Text("This permanently deletes every saved calculation, scan and conversion.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmClear = false
                        viewModel.clear()
                    }) { Text("Clear") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
                }
            )
        }

        if (filteredItems.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (items.isEmpty()) "No history yet.\nCalculations, scans and conversions you save will show up here."
                    else "No $selectedFilter history found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (items.isEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { onNavigate(com.allcalc.app.ui.navigation.Routes.CALC) }) {
                        Text("Start calculating")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems, key = { it.id }) { entry ->
                    HistoryCard(
                        entry = entry,
                        isCopied = copiedId == entry.id,
                        onTap = {
                            when (entry.type) {
                                HistoryTypes.CALC -> {
                                    ShortcutBus.calculator.value = ShortcutBus.CalcPreset(entry.expression, entry.result)
                                    onNavigate(com.allcalc.app.ui.navigation.Routes.CALC)
                                }
                                HistoryTypes.SCAN -> {
                                    ShortcutBus.scanner.value = entry.expression
                                    onNavigate(com.allcalc.app.ui.navigation.Routes.SCAN)
                                }
                                HistoryTypes.CONVERSION -> {
                                    val preset = ShortcutBus.fromHistory(entry)
                                    if (preset != null) {
                                        ShortcutBus.converter.value = preset
                                        onNavigate(com.allcalc.app.ui.navigation.Routes.CONVERTER)
                                    } else if (entry.category == "CURRENCY") {
                                        val from = entry.expression.trim().substringAfter(" ")
                                        val to = entry.result.trim().substringAfter(" ")
                                        val value = entry.expression.trim().substringBefore(" ")
                                        onOpenCurrency(from, to, value)
                                    }
                                }
                            }
                        },
                        onCopy = {
                            clipboard.setText(AnnotatedString("${entry.label} = ${entry.result}"))
                            copiedId = entry.id
                        },
                        onDelete = { id -> viewModel.delete(id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    entry: HistoryEntity,
    isCopied: Boolean,
    onTap: () -> Unit,
    onCopy: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val icon: ImageVector = when (entry.type) {
        HistoryTypes.CALC -> Icons.Filled.Calculate
        HistoryTypes.CONVERSION -> Icons.Filled.SwapHoriz
        else -> Icons.Filled.CameraAlt
    }
    Card(
        onClick = onTap,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            ) {
                Text(entry.label, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                Text(
                    entry.result,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2
                )
                Text(
                    timestampText(entry.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCopy) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy result",
                    tint = if (isCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onDelete(entry.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
            }
        }
    }
}

private fun timestampText(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours h ago"
        days < 7 -> "$days d ago"
        else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}