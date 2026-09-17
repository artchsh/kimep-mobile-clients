package kz.kimep.mobile.data.analytics

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.Locale

/** Stable event names — these are the dimensions you filter on in Umami. */
object AnalyticsEvents {
    const val APP_OPEN = "app_open"
    const val SCREEN_VIEW = "screen_view"
    const val LOGIN = "login"
    const val LOGOUT = "logout"
    const val SCHEDULE_TAB = "schedule_tab"
    const val SCHEDULE_REFRESH = "schedule_refresh"
    const val GRADES_TAB = "grades_tab"
    const val CALENDAR_FILTER = "calendar_filter"
    const val REMINDER_TOGGLE = "reminder_toggle"
    const val CONSENT = "consent_decision"
}

/**
 * Anonymous, privacy-conscious event tracking.
 *
 * Implementations must never send personal data (no student id, name, session, grades,
 * course titles) and must be safe to call before consent has been granted.
 */
interface Analytics {
    fun track(event: String, data: Map<String, String> = emptyMap())

    fun screen(name: String) = track(AnalyticsEvents.SCREEN_VIEW, mapOf("screen" to name))
}

/** Used when analytics is not configured or compiled out — sends nothing anywhere. */
object NoOpAnalytics : Analytics {
    override fun track(event: String, data: Map<String, String>) = Unit
}

/**
 * Umami (v2) client. Posts custom events to `<host>/api/send`.
 *
 * Privacy properties:
 *  - does nothing at all unless a host + website id are configured *and* consent is granted;
 *  - sends a synthetic URL (`/app/<event>`) rather than any real URL, so the session GUID
 *    embedded in avatar URLs can never leak;
 *  - identifies the device only by a random local UUID plus install age.
 *
 * Umami requirements handled here:
 *  - a valid `User-Agent` header is mandatory or the request is rejected;
 *  - the `cache` token returned by the server is echoed back as `x-umami-cache` so the
 *    events of one run are grouped into a single session (otherwise every event would
 *    look like a new visit and retention/session metrics would be meaningless).
 */
class UmamiAnalytics(
    private val host: String,
    private val websiteId: String,
    private val store: AnalyticsStore,
    private val context: Context,
    private val appVersion: String,
) : Analytics {

    private val json = Json { encodeDefaults = true }

    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val userAgent = "KIMEPMobile/$appVersion (Android)"

    @Volatile
    private var sessionCache: String? = null

    override fun track(event: String, data: Map<String, String>) {
        if (host.isBlank() || websiteId.isBlank()) return
        scope.launch {
            runCatching {
                if (!store.isTrackingEnabled()) return@runCatching
                val identity = store.identity()

                val payload = buildJsonObject {
                    put("website", websiteId)
                    put("hostname", HOSTNAME)
                    put("url", "/app/$event")
                    put("title", event)
                    put("screen", screenSize())
                    put("language", Locale.getDefault().toLanguageTag())
                    put("name", event)
                    putJsonObject("data") {
                        put("anon", identity.id)
                        put("installAgeDays", identity.installAgeDays)
                        put("appVersion", appVersion)
                        data.forEach { (key, value) -> put(key, value) }
                    }
                }

                val body = buildJsonObject {
                    put("type", "event")
                    put("payload", payload)
                }

                val response = client.post(host.trimEnd('/') + "/api/send") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.UserAgent, userAgent)
                    sessionCache?.let { header("x-umami-cache", it) }
                    setBody(body)
                }

                // Carry the session token into the next event.
                runCatching {
                    response.bodyAsText()
                        .let { Json.parseToJsonElement(it).jsonObject }
                        .get("cache")
                        ?.jsonPrimitive
                        ?.content
                        ?.let { sessionCache = it }
                }
            }
        }
    }

    private fun screenSize(): String {
        val metrics = context.resources.displayMetrics
        return "${metrics.widthPixels}x${metrics.heightPixels}"
    }

    private companion object {
        const val HOSTNAME = "kimep-mobile-app"
    }
}
