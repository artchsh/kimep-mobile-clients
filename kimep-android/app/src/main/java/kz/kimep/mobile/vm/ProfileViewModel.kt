package kz.kimep.mobile.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.kimep.mobile.data.KimepRepository
import kz.kimep.mobile.data.friendlyMessage
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: KimepRepository,
    private val sessionId: String,
) : ViewModel() {

    var refreshing by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun refresh() {
        viewModelScope.launch {
            refreshing = true
            error = null
            repository.refreshProfile(sessionId)
                .onFailure { error = it.friendlyMessage() }
            refreshing = false
        }
    }

    companion object {
        fun factory(repository: KimepRepository, sessionId: String) = viewModelFactory {
            initializer { ProfileViewModel(repository, sessionId) }
        }
    }
}
