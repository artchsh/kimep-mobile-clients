package kz.kimep.mobile.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.kimep.mobile.data.ReminderSettings
import kz.kimep.mobile.data.SettingsStore
import kz.kimep.mobile.data.notify.ReminderManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val reminderManager: ReminderManager,
) : ViewModel() {

    val settings: Flow<ReminderSettings> = settingsStore.settings

    fun setLessonReminderHour(enabled: Boolean) = update {
        settingsStore.setLessonReminderHour(enabled)
    }

    fun setLessonReminderTenMinutes(enabled: Boolean) = update {
        settingsStore.setLessonReminderTenMinutes(enabled)
    }

    fun setFinalReminders(enabled: Boolean) = update {
        settingsStore.setFinalReminders(enabled)
    }

    /** Re-derive all alarms from the current preferences. */
    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            reminderManager.refresh()
        }
    }

    companion object {
        fun factory(settingsStore: SettingsStore, reminderManager: ReminderManager) = viewModelFactory {
            initializer { SettingsViewModel(settingsStore, reminderManager) }
        }
    }
}
