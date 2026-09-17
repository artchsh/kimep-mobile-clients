package kz.kimep.mobile.data.notify

import kz.kimep.mobile.data.KimepRepository
import kz.kimep.mobile.data.SessionState
import kz.kimep.mobile.data.SessionStore
import kz.kimep.mobile.data.SettingsStore
import kotlinx.coroutines.flow.first

/**
 * Single entry point to (re)build all pending reminders from the current session,
 * schedule and user preferences. Safe to call on app start, after login, after a
 * schedule refresh, after a settings change, and after reboot.
 */
class ReminderManager(
    private val repository: KimepRepository,
    private val sessionStore: SessionStore,
    private val settingsStore: SettingsStore,
    private val scheduler: ReminderScheduler,
) {

    suspend fun refresh() {
        val settings = settingsStore.current()
        // Skip the Loading placeholder emitted by onStart; we need the real value.
        val session = sessionStore.state.first { it !is SessionState.Loading }
        val previousMeetings = settingsStore.scheduledMeetingIds()
        val previousFinals = settingsStore.scheduledFinalIds()

        if (session !is SessionState.LoggedIn) {
            scheduler.cancel(previousMeetings)
            scheduler.cancelFinals(previousFinals)
            settingsStore.setScheduledMeetingIds(emptySet())
            settingsStore.setScheduledFinalIds(emptySet())
            return
        }

        val lessonsEnabled = settings.lessonReminderHour || settings.lessonReminderTenMinutes

        val meetings = repository.schedule(session.id).getOrDefault(emptyList())
        scheduler.rescheduleLessons(
            meetings = meetings,
            previousIds = previousMeetings,
            hourBefore = settings.lessonReminderHour,
            tenMinutesBefore = settings.lessonReminderTenMinutes,
        )
        settingsStore.setScheduledMeetingIds(
            if (lessonsEnabled) meetings.map { it.id }.toSet() else emptySet(),
        )

        val finals = repository.finalExams(session.id).getOrDefault(emptyList())
        scheduler.rescheduleFinals(finals, previousFinals, settings.finalReminders)
        settingsStore.setScheduledFinalIds(
            if (settings.finalReminders) finals.map { it.id }.toSet() else emptySet(),
        )
    }

    suspend fun cancelAll() {
        scheduler.cancel(settingsStore.scheduledMeetingIds())
        scheduler.cancelFinals(settingsStore.scheduledFinalIds())
        settingsStore.setScheduledMeetingIds(emptySet())
        settingsStore.setScheduledFinalIds(emptySet())
    }
}
