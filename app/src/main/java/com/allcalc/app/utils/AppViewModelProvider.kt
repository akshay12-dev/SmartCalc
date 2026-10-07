package com.allcalc.app.utils

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.allcalc.app.AllCalcApplication
import com.allcalc.app.data.local.HistoryEntity
import com.allcalc.app.domain.converter.ConversionCategory
import com.allcalc.app.domain.converter.ConversionEngine
import com.allcalc.app.ui.calculator.CalculatorViewModel
import com.allcalc.app.ui.converter.ConverterViewModel
import com.allcalc.app.ui.currency.CurrencyViewModel
import com.allcalc.app.ui.history.HistoryViewModel
import com.allcalc.app.ui.scanner.ScanViewModel
import com.allcalc.app.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow

object ShortcutBus {
    data class CalcPreset(val expression: String, val result: String)
    data class ConverterPreset(val category: ConversionCategory, val from: String, val to: String, val value: String? = null)

    val calculator: MutableStateFlow<CalcPreset?> = MutableStateFlow(null)
    val converter: MutableStateFlow<ConverterPreset?> = MutableStateFlow(null)
    val scanner: MutableStateFlow<String?> = MutableStateFlow(null)

    fun fromHistory(entry: HistoryEntity): ConverterPreset? {
        val categoryName = entry.category
        if (categoryName.isEmpty() || categoryName == "CURRENCY") return null
        val category = ConversionCategory.entries.firstOrNull { it.name == categoryName } ?: return null
        val value = entry.expression.trim().substringBefore(" ").takeIf { it.toDoubleOrNull() != null } ?: return null
        val from = entry.expression.trim().substringAfter(" ").trim()
        val to = entry.result.trim().substringAfter(" ").trim()
        if (ConversionEngine.find(category, from) == null) return null
        if (ConversionEngine.find(category, to) == null) return null
        return ConverterPreset(category, from, to, value)
    }
}

val AppViewModelProvider: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = app().container
        CalculatorViewModel(app.expressionEngine, app.historyRepository, app.preferences)
    }
    initializer {
        val app = app().container
        ConverterViewModel(app.historyRepository, app.preferences, app.conversionEngine)
    }
    initializer {
        val app = app().container
        CurrencyViewModel(app.currencyRepository, app.preferences, app.historyRepository)
    }
    initializer {
        val app = app().container
        HistoryViewModel(app.historyRepository)
    }
    initializer {
        val app = app().container
        ScanViewModel(app.appContext, app.expressionEngine, app.mathNormalizer, app.historyRepository, app.preferences)
    }
    initializer {
        val app = app().container
        SettingsViewModel(app.historyRepository, app.preferences)
    }
}

fun CreationExtras.app(): AllCalcApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AllCalcApplication