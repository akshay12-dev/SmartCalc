package com.allcalc.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allcalc.app.data.local.HistoryEntity
import com.allcalc.app.data.local.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val history: HistoryRepository) : ViewModel() {

    val items: StateFlow<List<HistoryEntity>> = history.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(item: HistoryEntity) {
        viewModelScope.launch { history.add(item) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { history.delete(id) }
    }

    fun clear() {
        viewModelScope.launch { history.clear() }
    }
}