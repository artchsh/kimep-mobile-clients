package kz.kimep.mobile.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.kimep.mobile.data.CalendarRepository
import kz.kimep.mobile.data.model.CalendarData
import kotlinx.coroutines.launch

data class CalendarUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: CalendarData = CalendarData(),
)

class CalendarViewModel(private val repository: CalendarRepository) : ViewModel() {

    var uiState by mutableStateOf(CalendarUiState())
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            uiState = uiState.copy(loading = true, error = null)
            runCatching { repository.load() }
                .onSuccess { uiState = CalendarUiState(loading = false, data = it) }
                .onFailure { uiState = CalendarUiState(loading = false, error = it.message) }
        }
    }

    companion object {
        fun factory(repository: CalendarRepository) = viewModelFactory {
            initializer { CalendarViewModel(repository) }
        }
    }
}
