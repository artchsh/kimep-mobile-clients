package kz.kimep.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "kimep_settings")

data class ReminderSettings(
    val lessonReminderHour: Boolean = true,
    val lessonReminderTenMinutes: Boolean = true,
    val finalReminders: Boolean = true,
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val LESSON_HOUR = booleanPreferencesKey("lesson_reminder_hour")
        val LESSON_TEN = booleanPreferencesKey("lesson_reminder_ten_min")
        val FINALS = booleanPreferencesKey("final_reminders")
        val SCHEDULED = stringSetPreferencesKey("scheduled_meeting_ids")
        val SCHEDULED_FINALS = stringSetPreferencesKey("scheduled_final_ids")
    }

    val settings: Flow<ReminderSettings> = context.settingsDataStore.data.map { prefs ->
        ReminderSettings(
            lessonReminderHour = prefs[Keys.LESSON_HOUR] ?: true,
            lessonReminderTenMinutes = prefs[Keys.LESSON_TEN] ?: true,
            finalReminders = prefs[Keys.FINALS] ?: true,
        )
    }

    suspend fun current(): ReminderSettings = settings.first()

    suspend fun setLessonReminderHour(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.LESSON_HOUR] = enabled }
    }

    suspend fun setLessonReminderTenMinutes(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.LESSON_TEN] = enabled }
    }

    suspend fun setFinalReminders(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.FINALS] = enabled }
    }

    suspend fun scheduledMeetingIds(): Set<Int> =
        context.settingsDataStore.data.first()[Keys.SCHEDULED]
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()

    suspend fun setScheduledMeetingIds(ids: Set<Int>) {
        context.settingsDataStore.edit { it[Keys.SCHEDULED] = ids.map(Int::toString).toSet() }
    }

    suspend fun scheduledFinalIds(): Set<Int> =
        context.settingsDataStore.data.first()[Keys.SCHEDULED_FINALS]
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()

    suspend fun setScheduledFinalIds(ids: Set<Int>) {
        context.settingsDataStore.edit { it[Keys.SCHEDULED_FINALS] = ids.map(Int::toString).toSet() }
    }
}
