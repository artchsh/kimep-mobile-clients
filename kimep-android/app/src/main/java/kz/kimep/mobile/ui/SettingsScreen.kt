package kz.kimep.mobile.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.kimep.mobile.data.ReminderSettings
import kz.kimep.mobile.data.SettingsStore
import kz.kimep.mobile.data.notify.ReminderManager
import kz.kimep.mobile.vm.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    reminderManager: ReminderManager,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel =
        viewModel(factory = SettingsViewModel.factory(settingsStore, reminderManager))
    val settings by viewModel.settings
        .collectAsStateWithLifecycle(initialValue = ReminderSettings())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            SettingSwitch(
                title = "1 hour before class",
                subtitle = "Reminder an hour before each class starts",
                checked = settings.lessonReminderHour,
                onCheckedChange = viewModel::setLessonReminderHour,
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingSwitch(
                title = "10 minutes before class",
                subtitle = "A final nudge ten minutes before each class",
                checked = settings.lessonReminderTenMinutes,
                onCheckedChange = viewModel::setLessonReminderTenMinutes,
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingSwitch(
                title = "Final exam reminders",
                subtitle = "Reminder the day before a scheduled final exam",
                checked = settings.finalReminders,
                onCheckedChange = viewModel::setFinalReminders,
            )
        }

        ExactAlarmNotice()

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Reminders are scheduled on this device from your current timetable. " +
                "They are rebuilt each time you open the app. If exact alarms are not " +
                "permitted, Android may deliver them a few minutes late.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true
}

@Composable
private fun ExactAlarmNotice() {
    val context = LocalContext.current
    var canExact by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canExact = canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (canExact) return

    Spacer(Modifier.height(16.dp))
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Allow exact alarms", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Android needs permission to deliver reminders at the exact time. " +
                    "Without it, reminders may arrive late.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM),
                        )
                    }
                },
            ) {
                Text("Open settings")
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(0.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
