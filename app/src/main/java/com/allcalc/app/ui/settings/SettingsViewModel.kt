package com.allcalc.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allcalc.app.data.local.HistoryRepository
import com.allcalc.app.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val history: HistoryRepository,
    private val prefs: PreferencesRepository
) : ViewModel() {

    val theme: StateFlow<Int> = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val decimalPrecision: StateFlow<Int> = prefs.decimalPrecision.stateIn(viewModelScope, SharingStarted.Eagerly, 4)
    val angleMode: StateFlow<Int> = prefs.angleMode.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val haptics: StateFlow<Boolean> = prefs.haptics.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val sound: StateFlow<Boolean> = prefs.sound.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val autoCurrency: StateFlow<Boolean> = prefs.autoCurrency.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setTheme(value: Int) = set { prefs.setTheme(value) }

    fun setDecimalPrecision(value: Int) = set { prefs.setDecimalPrecision(value) }

    fun setAngleMode(value: Int) = set { prefs.setAngleMode(value) }

    fun setHaptics(value: Boolean) = set { prefs.setHaptics(value) }

    fun setSound(value: Boolean) = set { prefs.setSound(value) }

    fun setAutoCurrency(value: Boolean) = set { prefs.setAutoCurrency(value) }

    fun clearHistory() = set { history.clear() }

    private fun set(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}