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

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel(private val repository: KimepRepository) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun login(studentId: String, password: String) {
        if (studentId.isBlank() || password.isBlank()) {
            uiState = LoginUiState(error = "Enter your Student ID and password")
            return
        }
        viewModelScope.launch {
            uiState = LoginUiState(loading = true)
            repository.login(studentId, password)
                .onFailure { uiState = LoginUiState(error = it.friendlyMessage()) }
        }
    }

    companion object {
        fun factory(repository: KimepRepository) = viewModelFactory {
            initializer { LoginViewModel(repository) }
        }
    }
}
