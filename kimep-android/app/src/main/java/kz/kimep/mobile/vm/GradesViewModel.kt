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
import kz.kimep.mobile.data.model.AssessmentScore
import kz.kimep.mobile.data.model.FinalGrade
import kz.kimep.mobile.data.model.GpaCredits
import kotlinx.coroutines.launch

data class GradesUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val gpa: GpaCredits = GpaCredits(),
    val assessment: List<AssessmentScore> = emptyList(),
    val finalGrades: List<FinalGrade> = emptyList(),
)

class GradesViewModel(
    private val repository: KimepRepository,
    private val sessionId: String,
) : ViewModel() {

    var uiState by mutableStateOf(GradesUiState())
        private set

    init {
        load()
    }

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            uiState = uiState.copy(
                loading = !refresh && uiState.finalGrades.isEmpty(),
                refreshing = refresh,
                error = null,
            )

            val gpaResult = repository.gpa(sessionId)
            val assessmentResult = repository.assessmentScores(sessionId)
            val gradesResult = repository.finalGrades(sessionId)

            val critical = gpaResult.exceptionOrNull() ?: gradesResult.exceptionOrNull()

            uiState = GradesUiState(
                loading = false,
                refreshing = false,
                error = critical?.friendlyMessage(),
                gpa = gpaResult.getOrDefault(GpaCredits()),
                assessment = assessmentResult.getOrDefault(emptyList()),
                finalGrades = gradesResult.getOrDefault(emptyList()),
            )
        }
    }

    companion object {
        fun factory(repository: KimepRepository, sessionId: String) = viewModelFactory {
            initializer { GradesViewModel(repository, sessionId) }
        }
    }
}
