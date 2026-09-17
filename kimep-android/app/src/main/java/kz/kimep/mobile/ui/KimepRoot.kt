package kz.kimep.mobile.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.kimep.mobile.KimepApp
import kz.kimep.mobile.data.SessionState
import kz.kimep.mobile.data.analytics.AnalyticsEvents
import kz.kimep.mobile.data.analytics.ConsentState
import kz.kimep.mobile.ui.components.LoadingState
import kotlinx.coroutines.launch

@Composable
fun KimepRoot() {
    val context = LocalContext.current
    val container = (context.applicationContext as KimepApp).container
    val scope = rememberCoroutineScope()

    val consent by container.analyticsStore.consent
        .collectAsStateWithLifecycle(initialValue = ConsentState.Loading)
    val session by container.sessionStore.state
        .collectAsStateWithLifecycle(initialValue = SessionState.Loading)

    var showNotice by rememberSaveable { mutableStateOf(false) }

    // Opt-out model: Analytics.track() drops the event only if consent was denied.
    LaunchedEffect(Unit) {
        container.analytics.track(AnalyticsEvents.APP_OPEN)
    }

    // Show the dismissible notice once, until the user makes a choice.
    LaunchedEffect(consent) {
        if (container.analyticsEnabled && consent == ConsentState.Undecided) {
            showNotice = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (val current = session) {
            SessionState.Loading -> LoadingState()

            SessionState.LoggedOut -> LoginScreen(
                repository = container.repository,
                analytics = container.analytics,
            )

            is SessionState.LoggedIn -> MainScreen(
                session = current,
                repository = container.repository,
                calendarRepository = container.calendarRepository,
                scheduleCache = container.scheduleCache,
                settingsStore = container.settingsStore,
                reminderManager = container.reminderManager,
                analyticsStore = container.analyticsStore,
                analytics = container.analytics,
                analyticsEnabled = container.analyticsEnabled,
                onLogout = {
                    scope.launch {
                        container.reminderManager.cancelAll()
                        container.scheduleCache.clear()
                        container.repository.logout()
                    }
                },
            )
        }
    }

    if (showNotice) {
        AnalyticsFirstRunDialog(
            onKeepEnabled = {
                showNotice = false
                scope.launch {
                    container.analyticsStore.setConsent(true)
                    container.analytics.track(
                        AnalyticsEvents.CONSENT,
                        mapOf("decision" to "granted", "source" to "first_run_notice"),
                    )
                }
            },
            onOptOut = {
                showNotice = false
                scope.launch {
                    container.analyticsStore.setConsent(false)
                    container.analyticsStore.clearIdentity()
                }
            },
        )
    }
}
