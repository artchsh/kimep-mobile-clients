package kz.kimep.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kz.kimep.mobile.data.CalendarRepository
import kz.kimep.mobile.data.KimepRepository
import kz.kimep.mobile.data.ScheduleCache
import kz.kimep.mobile.data.SessionState
import kz.kimep.mobile.data.SettingsStore
import kz.kimep.mobile.data.notify.ReminderManager

private data class Destination(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    session: SessionState.LoggedIn,
    repository: KimepRepository,
    calendarRepository: CalendarRepository,
    scheduleCache: ScheduleCache,
    settingsStore: SettingsStore,
    reminderManager: ReminderManager,
    onLogout: () -> Unit,
) {
    val destinations = listOf(
        Destination("Schedule", Icons.Filled.CalendarMonth),
        Destination("Grades", Icons.Filled.Assessment),
        Destination("Calendar", Icons.AutoMirrored.Filled.EventNote),
        Destination("Profile", Icons.Filled.Person),
    )

    var index by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (showSettings) "Settings" else destinations[index].label,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (showSettings) {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!showSettings) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            if (!showSettings) {
                NavigationBar {
                    destinations.forEachIndexed { i, destination ->
                        NavigationBarItem(
                            selected = index == i,
                            onClick = { index = i },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (showSettings) {
                SettingsScreen(settingsStore, reminderManager)
            } else {
                when (index) {
                    0 -> ScheduleScreen(repository, scheduleCache, calendarRepository, reminderManager, session.id)
                    1 -> GradesScreen(repository, session.id)
                    2 -> CalendarScreen(calendarRepository)
                    else -> ProfileScreen(session, repository, onLogout)
                }
            }
        }
    }
}
