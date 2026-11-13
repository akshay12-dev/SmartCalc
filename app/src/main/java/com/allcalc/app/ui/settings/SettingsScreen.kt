package com.allcalc.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.allcalc.app.ui.common.ScreenHeader
import com.allcalc.app.ui.common.SectionTitle
import com.allcalc.app.utils.AppViewModelProvider

@Composable
fun SettingsScreen(onHome: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider)
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val precision by viewModel.decimalPrecision.collectAsStateWithLifecycle()
    val angleMode by viewModel.angleMode.collectAsStateWithLifecycle()
    val haptics by viewModel.haptics.collectAsStateWithLifecycle()
    val sound by viewModel.sound.collectAsStateWithLifecycle()
    val autoCurrency by viewModel.autoCurrency.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Settings", onHome = onHome)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionTitle("Appearance") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = theme == 0, onClick = { viewModel.setTheme(0) }, label = { Text("System") })
                FilterChip(selected = theme == 1, onClick = { viewModel.setTheme(1) }, label = { Text("Light") })
                FilterChip(selected = theme == 2, onClick = { viewModel.setTheme(2) }, label = { Text("Dark") })
            }
        }

        item { SectionTitle("Numbers") }
        item {
            Column {
                Text("Decimal precision: $precision")
                Slider(
                    value = precision.toFloat(),
                    onValueChange = { viewModel.setDecimalPrecision(it.toInt()) },
                    valueRange = 0f..10f,
                    steps = 9
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = angleMode == 0, onClick = { viewModel.setAngleMode(0) }, label = { Text("Degrees") })
                FilterChip(selected = angleMode == 1, onClick = { viewModel.setAngleMode(1) }, label = { Text("Radians") })
            }
        }

        item { SectionTitle("Feedback") }
        item {
            SettingRow(title = "Haptic feedback", subtitle = "Vibrate on button presses") {
                Switch(checked = haptics, onCheckedChange = viewModel::setHaptics)
            }
        }
        item {
            SettingRow(title = "Key sounds", subtitle = "Play a beep when keys are pressed") {
                Switch(checked = sound, onCheckedChange = viewModel::setSound)
            }
        }

        item { SectionTitle("Currency") }
        item {
            SettingRow(
                title = "Auto-update rates",
                subtitle = "Refresh exchange rates when they are older than 12 hours"
            ) {
                Switch(checked = autoCurrency, onCheckedChange = viewModel::setAutoCurrency)
            }
        }

        item { SectionTitle("Data") }
        item {
            TextButton(
                onClick = { confirmClear = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear all history") }
        }

        item { SectionTitle("About") }
        item {
            Text(
                "AllCalc 1.0.0 — offline-first calculator, scan-and-solve and unit converter. " +
                    "Live currency rates come from free exchange-rate feeds (open.er-api.com and " +
                    "fallbacks); sample rates are used offline and are clearly marked until you update.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    viewModel.clearHistory()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing()
    }
}