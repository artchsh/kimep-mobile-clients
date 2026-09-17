package kz.kimep.mobile.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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

    // Record a launch once tracking is actually permitted (first run: right after consent).
    LaunchedEffect(consent) {
        if (consent == ConsentState.Granted) {
            container.analytics.track(AnalyticsEvents.APP_OPEN)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (container.analyticsEnabled && consent == ConsentState.Undecided) {
            PrivacyConsentScreen(
                onAccept = { scope.launch { container.analyticsStore.setConsent(true) } },
                onDecline = { scope.launch { container.analyticsStore.setConsent(false) } },
            )
            return@Surface
        }

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
}
