package kz.kimep.mobile.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.kimep.mobile.KimepApp
import kz.kimep.mobile.data.SessionState
import kz.kimep.mobile.ui.components.LoadingState
import kotlinx.coroutines.launch

@Composable
fun KimepRoot() {
    val context = LocalContext.current
    val container = (context.applicationContext as KimepApp).container
    val session by container.sessionStore.state
        .collectAsStateWithLifecycle(initialValue = SessionState.Loading)
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (val current = session) {
            SessionState.Loading -> LoadingState()

            SessionState.LoggedOut -> LoginScreen(container.repository)

            is SessionState.LoggedIn -> MainScreen(
                session = current,
                repository = container.repository,
                calendarRepository = container.calendarRepository,
                scheduleCache = container.scheduleCache,
                settingsStore = container.settingsStore,
                reminderManager = container.reminderManager,
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
