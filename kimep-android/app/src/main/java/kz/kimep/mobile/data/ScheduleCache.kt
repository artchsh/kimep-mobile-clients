package kz.kimep.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kz.kimep.mobile.data.model.ClassMeeting
import kz.kimep.mobile.data.model.FinalExam
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.cacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "kimep_cache")

/**
 * Persists the last successfully fetched timetable so the UI can render instantly on
 * launch and then update as soon as the network response arrives
 * (stale-while-revalidate).
 */
class ScheduleCache(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val SCHEDULE = stringPreferencesKey("schedule_json")
        val FINALS = stringPreferencesKey("finals_json")
        val UPDATED_AT = longPreferencesKey("schedule_updated_at")
    }

    data class Snapshot(
        val meetings: List<ClassMeeting>,
        val finals: List<FinalExam>,
        val updatedAt: Long,
    )

    suspend fun read(): Snapshot? {
        val prefs = context.cacheDataStore.data.first()
        val scheduleJson = prefs[Keys.SCHEDULE] ?: return null
        val meetings = runCatching {
            json.decodeFromString(ListSerializer(ClassMeeting.serializer()), scheduleJson)
        }.getOrNull() ?: return null
        val finals = prefs[Keys.FINALS]?.let { raw ->
            runCatching {
                json.decodeFromString(ListSerializer(FinalExam.serializer()), raw)
            }.getOrNull()
        }.orEmpty()
        return Snapshot(meetings, finals, prefs[Keys.UPDATED_AT] ?: 0L)
    }

    suspend fun saveSchedule(meetings: List<ClassMeeting>) {
        val encoded = json.encodeToString(ListSerializer(ClassMeeting.serializer()), meetings)
        context.cacheDataStore.edit {
            it[Keys.SCHEDULE] = encoded
            it[Keys.UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun saveFinals(finals: List<FinalExam>) {
        val encoded = json.encodeToString(ListSerializer(FinalExam.serializer()), finals)
        context.cacheDataStore.edit { it[Keys.FINALS] = encoded }
    }

    suspend fun clear() {
        context.cacheDataStore.edit { it.clear() }
    }
}
