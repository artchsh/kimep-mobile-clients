package kz.kimep.mobile.data.analytics

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.analyticsDataStore: DataStore<Preferences> by preferencesDataStore(name = "kimep_analytics")

sealed interface ConsentState {
    /** Not read from disk yet. */
    data object Loading : ConsentState

    /** The user has not decided yet — show the consent screen. */
    data object Undecided : ConsentState

    data object Granted : ConsentState
    data object Denied : ConsentState
}

data class AnonymousIdentity(val id: String, val installAgeDays: Long)

/**
 * Local state backing anonymous analytics: the consent decision, a random device-local
 * identifier, and first-seen time (used for install age / retention).
 *
 * The identifier is only ever created and sent while tracking is explicitly enabled.
 */
class AnalyticsStore(private val context: Context) {

    private object Keys {
        val CONSENT = stringPreferencesKey("consent")
        val ANON_ID = stringPreferencesKey("anonymous_id")
        val FIRST_SEEN = longPreferencesKey("first_seen_at")
    }

    val consent: Flow<ConsentState> = context.analyticsDataStore.data.map { prefs ->
        when (prefs[Keys.CONSENT]) {
            "granted" -> ConsentState.Granted
            "denied" -> ConsentState.Denied
            else -> ConsentState.Undecided
        }
    }

    suspend fun setConsent(granted: Boolean) {
        context.analyticsDataStore.edit { prefs ->
            prefs[Keys.CONSENT] = if (granted) "granted" else "denied"
            if (granted && prefs[Keys.FIRST_SEEN] == null) {
                prefs[Keys.FIRST_SEEN] = System.currentTimeMillis()
            }
        }
    }

    /**
     * Opt-out model: tracking is on unless the user has explicitly denied it
     * (including before they have responded to the first-run notice).
     */
    suspend fun isTrackingEnabled(): Boolean = consent.first() != ConsentState.Denied

    /** Random, device-local identifier. Created lazily; regenerated if identity was cleared. */
    suspend fun identity(): AnonymousIdentity {
        val prefs = context.analyticsDataStore.data.first()
        val existing = prefs[Keys.ANON_ID]
        val firstSeen = prefs[Keys.FIRST_SEEN] ?: System.currentTimeMillis()
        val id = existing ?: UUID.randomUUID().toString()

        if (existing == null || prefs[Keys.FIRST_SEEN] == null) {
            context.analyticsDataStore.edit {
                if (it[Keys.ANON_ID] == null) it[Keys.ANON_ID] = id
                if (it[Keys.FIRST_SEEN] == null) it[Keys.FIRST_SEEN] = firstSeen
            }
        }

        val days = ((System.currentTimeMillis() - firstSeen) / 86_400_000L).coerceAtLeast(0L)
        return AnonymousIdentity(id, days)
    }

    /** Forgets the anonymous identifier and install age, keeping the consent decision. */
    suspend fun clearIdentity() {
        context.analyticsDataStore.edit {
            it.remove(Keys.ANON_ID)
            it.remove(Keys.FIRST_SEEN)
        }
    }
}
