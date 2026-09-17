package kz.kimep.mobile.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.kimep.mobile.data.AcademicCalendar
import kz.kimep.mobile.data.CalendarRepository
import kz.kimep.mobile.data.KimepRepository
import kz.kimep.mobile.data.ScheduleCache
import kz.kimep.mobile.data.friendlyMessage
import kz.kimep.mobile.data.model.ClassMeeting
import kz.kimep.mobile.data.model.FinalExam
import kz.kimep.mobile.data.notify.ReminderManager
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ScheduleUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val meetings: List<ClassMeeting> = emptyList(),
    val finals: List<FinalExam> = emptyList(),
    val fromCache: Boolean = false,
    val midterm: String? = null,
)

class ScheduleViewModel(
    private val repository: KimepRepository,
    private val cache: ScheduleCache,
    private val calendarRepository: CalendarRepository,
    private val reminderManager: ReminderManager,
    private val sessionId: String,
) : ViewModel() {

    var uiState by mutableStateOf(ScheduleUiState())
        private set

    init {
        viewModelScope.launch {
            // 1. Show the cached timetable instantly (stale-while-revalidate).
            val cached = runCatching { cache.read() }.getOrNull()
            if (cached != null && cached.meetings.isNotEmpty()) {
                uiState = ScheduleUiState(
                    loading = false,
                    meetings = cached.meetings,
                    finals = cached.finals,
                    fromCache = true,
                    midterm = midtermLabel(cached.meetings),
                )
            }
            // 2. Refresh in the background.
            load()
        }
    }

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            uiState = uiState.copy(
                loading = uiState.meetings.isEmpty(),
                refreshing = refresh,
                error = null,
            )

            val meetingsResult = repository.schedule(sessionId)
            val finalsResult = repository.finalExams(sessionId)
            val hadData = uiState.meetings.isNotEmpty()

            meetingsResult.onSuccess { cache.saveSchedule(it) }
            finalsResult.onSuccess { cache.saveFinals(it) }

            val meetings = meetingsResult.getOrElse { uiState.meetings }

            uiState = uiState.copy(
                loading = false,
                refreshing = false,
                error = if (meetingsResult.isFailure && !hadData) {
                    meetingsResult.exceptionOrNull()?.friendlyMessage()
                } else {
                    null
                },
                meetings = meetings,
                finals = finalsResult.getOrElse { uiState.finals },
                fromCache = meetingsResult.isFailure && hadData,
                midterm = midtermLabel(meetings),
            )

            if (meetingsResult.isSuccess) {
                reminderManager.refresh()
            }
        }
    }

    /** Returns a label when today falls inside a midterm week, else null. */
    private suspend fun midtermLabel(meetings: List<ClassMeeting>): String? {
        val code = meetings.firstOrNull()?.semester?.takeIf { it.isNotBlank() } ?: return null
        val data = runCatching { calendarRepository.load() }.getOrNull() ?: return null
        val today = LocalDate.now()
        return AcademicCalendar.midtermWindows(data, code)
            .firstOrNull { it.contains(today) }
            ?.label
    }

    companion object {
        fun factory(
            repository: KimepRepository,
            cache: ScheduleCache,
            calendarRepository: CalendarRepository,
            reminderManager: ReminderManager,
            sessionId: String,
        ) = viewModelFactory {
            initializer {
                ScheduleViewModel(repository, cache, calendarRepository, reminderManager, sessionId)
            }
        }
    }
}
