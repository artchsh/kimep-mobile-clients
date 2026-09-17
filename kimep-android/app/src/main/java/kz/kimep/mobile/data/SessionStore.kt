package kz.kimep.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kz.kimep.mobile.data.model.PersonalInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kimep_session")

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(
        val id: String,
        val studentId: String,
        val firstName: String?,
        val lastName: String?,
        val program: String?,
        val expiresOnMs: Long?,
    ) : SessionState {
        val fullName: String
            get() = listOfNotNull(firstName, lastName).joinToString(" ").trim()
    }
}

class SessionStore(private val context: Context) {

    private object Keys {
        val ID = stringPreferencesKey("id")
        val STUDENT_ID = stringPreferencesKey("student_id")
        val FIRST = stringPreferencesKey("first_name")
        val LAST = stringPreferencesKey("last_name")
        val PROGRAM = stringPreferencesKey("program")
        val EXPIRES = longPreferencesKey("expires_on")
    }

    val state: Flow<SessionState> = context.dataStore.data
        .map { prefs ->
            val id = prefs[Keys.ID]
            if (id.isNullOrBlank()) {
                SessionState.LoggedOut
            } else {
                SessionState.LoggedIn(
                    id = id,
                    studentId = prefs[Keys.STUDENT_ID].orEmpty(),
                    firstName = prefs[Keys.FIRST],
                    lastName = prefs[Keys.LAST],
                    program = prefs[Keys.PROGRAM],
                    expiresOnMs = prefs[Keys.EXPIRES],
                )
            }
        }
        .onStart { emit(SessionState.Loading) }

    suspend fun save(
        studentId: String,
        sessionId: String,
        expiresOnMs: Long?,
        info: PersonalInfo?,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ID] = sessionId
            prefs[Keys.STUDENT_ID] = studentId
            info?.firstName?.let { prefs[Keys.FIRST] = it }
            info?.lastName?.let { prefs[Keys.LAST] = it }
            info?.programId?.let { prefs[Keys.PROGRAM] = it }
            if (expiresOnMs != null) prefs[Keys.EXPIRES] = expiresOnMs else prefs.remove(Keys.EXPIRES)
        }
    }

    suspend fun updateProfile(info: PersonalInfo) {
        context.dataStore.edit { prefs ->
            info.firstName?.let { prefs[Keys.FIRST] = it }
            info.lastName?.let { prefs[Keys.LAST] = it }
            info.programId?.let { prefs[Keys.PROGRAM] = it }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
