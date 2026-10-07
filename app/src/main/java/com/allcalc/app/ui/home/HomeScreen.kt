package com.allcalc.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.allcalc.app.data.local.HistoryTypes
import com.allcalc.app.domain.converter.ConversionCategory
import com.allcalc.app.ui.common.ToolCard
import com.allcalc.app.ui.history.HistoryViewModel
import com.allcalc.app.ui.navigation.Routes
import com.allcalc.app.utils.AppViewModelProvider
import com.allcalc.app.utils.ShortcutBus
import com.allcalc.app.utils.ShortcutBus.CalcPreset
import com.allcalc.app.utils.ShortcutBus.ConverterPreset

private data class QuickLink(
    val label: String,
    val subtitle: String,
    val category: ConversionCategory,
    val from: String,
    val to: String
)

private val quickLinks = listOf(
    QuickLink("kg → g", "Kilogram to gram", ConversionCategory.WEIGHT, "kg", "g"),
    QuickLink("km → mi", "Kilometre to mile", ConversionCategory.LENGTH, "km", "mi"),
    QuickLink("ft → cm", "Foot to centimetre", ConversionCategory.LENGTH, "ft", "cm"),
    QuickLink("°C → °F", "Celsius to Fahrenheit", ConversionCategory.TEMPERATURE, "°C", "°F"),
    QuickLink("l → gal", "Litre to gallon", ConversionCategory.VOLUME, "l", "gal"),
    QuickLink("GB → MB", "Gigabyte to megabyte", ConversionCategory.DATA, "GB", "MB")
)

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val historyViewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider)
    val recent by historyViewModel.items.collectAsStateWithLifecycle(emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        if (onBack != null) {
            com.allcalc.app.ui.common.ScreenHeader(title = "AllCalc Hub", onHome = onBack)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            Text(
                text = "AllCalc",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Calculator · Scan & Solve · Unit Converter",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Text("Quick conversions", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickLinks) { link ->
                    Surface(
                        onClick = {
                            ShortcutBus.converter.value = ConverterPreset(
                                category = link.category,
                                from = link.from,
                                to = link.to,
                                value = "1"
                            )
                            onNavigate(Routes.CONVERTER)
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(link.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                link.subtitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Quick calculators", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val shortcuts = listOf(
                    "Fraction" to "fraction",
                    "Percentage" to "percentage",
                    "Loan / EMI" to "loan",
                    "BMI" to "bmi",
                    "Tip & Split" to "tip",
                    "Triangle" to "triangle",
                    "Volume" to "volume",
                    "Std Dev" to "std_dev",
                    "Investment" to "investment",
                    "Age" to "age",
                    "GPA" to "gpa",
                    "Random (RNG)" to "rng",
                    "More (42+)..." to "hub"
                )
                items(shortcuts) { (label, id) ->
                    SuggestionChip(
                        onClick = {
                            if (id == "hub") onNavigate(Routes.SPECIALIZED_HUB)
                            else onNavigate(Routes.specializedDetail(id))
                        },
                        label = { Text(label) }
                    )
                }
            }
        }

        item {
            Text("Tools", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolCard(
                    title = "Specialized Calculators (42+)",
                    subtitle = "Math · Stats · Geometry · Financial · Fitness & Health",
                    icon = Icons.Filled.Functions,
                    onClick = { onNavigate(Routes.SPECIALIZED_HUB) }
                )
                ToolCard(
                    title = "Scientific Calculator",
                    subtitle = "Trig, logs, factorials, % and solving x",
                    icon = Icons.Filled.Calculate,
                    onClick = { onNavigate(Routes.CALC) }
                )
                ToolCard(
                    title = "Scan & Solve",
                    subtitle = "Point the camera at any equation",
                    icon = Icons.Filled.CameraAlt,
                    onClick = { onNavigate(Routes.SCAN) }
                )
                ToolCard(
                    title = "Unit Converter",
                    subtitle = "12 categories of units",
                    icon = Icons.Filled.SwapHoriz,
                    onClick = { onNavigate(Routes.CONVERTER) }
                )
                ToolCard(
                    title = "Currency Converter",
                    subtitle = "Live exchange rates (auto-update)",
                    icon = Icons.Filled.AttachMoney,
                    onClick = { onNavigate(Routes.currency("USD", "INR", "1")) }
                )
                ToolCard(
                    title = "History",
                    subtitle = "Calculator, scans and conversions",
                    icon = Icons.Filled.History,
                    onClick = { onNavigate(Routes.HISTORY) }
                )
                ToolCard(
                    title = "Settings",
                    subtitle = "Theme, precision, angle mode and more",
                    icon = Icons.Filled.Settings,
                    onClick = { onNavigate(Routes.SETTINGS) }
                )
            }
        }

        item {
            Text("Recent history", style = MaterialTheme.typography.titleMedium)
        }
        if (recent.isEmpty()) {
            item {
                Text(
                    text = "No history yet — your saved calculations will appear here.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(recent.take(5)) { entry ->
                Surface(
                    onClick = {
                        when (entry.type) {
                            HistoryTypes.CALC -> {
                                ShortcutBus.calculator.value = CalcPreset(entry.expression, entry.result)
                                onNavigate(Routes.CALC)
                            }
                            HistoryTypes.SCAN -> {
                                ShortcutBus.scanner.value = entry.expression
                                onNavigate(Routes.SCAN)
                            }
                            HistoryTypes.CONVERSION -> {
                                val preset = ShortcutBus.fromHistory(entry)
                                if (preset != null) {
                                    ShortcutBus.converter.value = preset
                                    onNavigate(Routes.CONVERTER)
                                } else if (entry.category == "CURRENCY") {
                                    val from = entry.expression.trim().substringAfter(" ").trim()
                                    val to = entry.result.trim().substringAfter(" ").trim()
                                    val value = entry.expression.trim().substringBefore(" ").trim()
                                    onNavigate(Routes.currency(from, to, value))
                                }
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(entry.label, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        Text(
                            entry.result,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
}