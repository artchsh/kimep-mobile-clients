package kz.kimep.mobile.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.kimep.mobile.data.ReminderSettings
import kz.kimep.mobile.data.SettingsStore
import kz.kimep.mobile.data.analytics.Analytics
import kz.kimep.mobile.data.analytics.AnalyticsEvents
import kz.kimep.mobile.data.analytics.AnalyticsStore
import kz.kimep.mobile.data.analytics.ConsentState
import kz.kimep.mobile.data.notify.ReminderManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val reminderManager: ReminderManager,
    private val analyticsStore: AnalyticsStore,
    private val analytics: Analytics,
) : ViewModel() {

    val settings: Flow<ReminderSettings> = settingsStore.settings
    val consent: Flow<ConsentState> = analyticsStore.consent

    fun setLessonReminderHour(enabled: Boolean) = update(
        event = AnalyticsEvents.REMINDER_TOGGLE,
        data = mapOf("kind" to "hour", "enabled" to enabled.toString()),
    ) {
        settingsStore.setLessonReminderHour(enabled)
    }

    fun setLessonReminderTenMinutes(enabled: Boolean) = update(
        event = AnalyticsEvents.REMINDER_TOGGLE,
        data = mapOf("kind" to "ten_minutes", "enabled" to enabled.toString()),
    ) {
        settingsStore.setLessonReminderTenMinutes(enabled)
    }

    fun setFinalReminders(enabled: Boolean) = update(
        event = AnalyticsEvents.REMINDER_TOGGLE,
        data = mapOf("kind" to "final_exam", "enabled" to enabled.toString()),
    ) {
        settingsStore.setFinalReminders(enabled)
    }

    fun setAnalyticsConsent(granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                analyticsStore.setConsent(true)
                analytics.track(AnalyticsEvents.CONSENT, mapOf("decision" to "granted"))
            } else {
                // Record the opt-out while tracking is still permitted, then stop and forget.
                analytics.track(AnalyticsEvents.CONSENT, mapOf("decision" to "denied"))
                analyticsStore.setConsent(false)
                analyticsStore.clearIdentity()
            }
        }
    }

    /** Apply a preference change, then rebuild alarms from the new state. */
    private fun update(event: String, data: Map<String, String>, block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            analytics.track(event, data)
            reminderManager.refresh()
        }
    }

    companion object {
        fun factory(
            settingsStore: SettingsStore,
            reminderManager: ReminderManager,
            analyticsStore: AnalyticsStore,
            analytics: Analytics,
        ) = viewModelFactory {
            initializer {
                SettingsViewModel(settingsStore, reminderManager, analyticsStore, analytics)
            }
        }
    }
}
