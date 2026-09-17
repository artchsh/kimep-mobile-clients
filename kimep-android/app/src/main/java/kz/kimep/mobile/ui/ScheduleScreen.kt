package kz.kimep.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.kimep.mobile.data.ApiDate
import kz.kimep.mobile.data.CalendarRepository
import kz.kimep.mobile.data.KimepRepository
import kz.kimep.mobile.data.ScheduleCache
import kz.kimep.mobile.data.analytics.Analytics
import kz.kimep.mobile.data.analytics.AnalyticsEvents
import kz.kimep.mobile.data.model.ClassMeeting
import kz.kimep.mobile.data.model.FinalExam
import kz.kimep.mobile.data.notify.ReminderManager
import kz.kimep.mobile.ui.components.LoadingState
import kz.kimep.mobile.ui.components.MessageState
import kz.kimep.mobile.vm.ScheduleViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val weekOrder = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
)

private fun weekdayIndex(day: String): Int =
    weekOrder.indexOfFirst { it.equals(day, ignoreCase = true) }.takeIf { it >= 0 } ?: weekOrder.size

private val accentPalette = listOf(
    Color(0xFF1D4E89),
    Color(0xFF00696E),
    Color(0xFF8B5000),
    Color(0xFF6A359C),
    Color(0xFF9C2B4E),
    Color(0xFF2E6B2E),
)

private fun accentFor(key: String): Color =
    accentPalette[(key.hashCode() and 0x7FFFFFFF) % accentPalette.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    repository: KimepRepository,
    scheduleCache: ScheduleCache,
    calendarRepository: CalendarRepository,
    reminderManager: ReminderManager,
    analytics: Analytics,
    sessionId: String,
    modifier: Modifier = Modifier,
) {
    val viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModel.factory(
            repository,
            scheduleCache,
            calendarRepository,
            reminderManager,
            sessionId,
        ),
    )
    val state = viewModel.uiState
    var showFinals by rememberSaveable { mutableStateOf(false) }

    when {
        state.loading -> LoadingState(modifier)

        state.error != null && state.meetings.isEmpty() -> MessageState(
            icon = Icons.Filled.CloudOff,
            title = "Couldn't load your schedule",
            message = state.error,
            actionLabel = "Retry",
            onAction = { viewModel.load() },
            modifier = modifier,
        )

        else -> Column(modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !showFinals,
                    onClick = {
                        showFinals = false
                        analytics.track(AnalyticsEvents.SCHEDULE_TAB, mapOf("tab" to "classes"))
                    },
                    label = { Text("Classes") },
                )
                FilterChip(
                    selected = showFinals,
                    onClick = {
                        showFinals = true
                        analytics.track(AnalyticsEvents.SCHEDULE_TAB, mapOf("tab" to "finals"))
                    },
                    label = { Text("Finals") },
                )
            }

            if (showFinals) {
                FinalsList(state.finals)
            } else {
                state.midterm?.let { MidtermBanner(it) }
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = {
                        analytics.track(AnalyticsEvents.SCHEDULE_REFRESH)
                        viewModel.load(refresh = true)
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.meetings.isEmpty()) {
                        MessageState(
                            icon = Icons.Filled.EventBusy,
                            title = "No scheduled classes",
                            message = "There are no classes for the current term.",
                        )
                    } else {
                        ScheduleList(state.meetings)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleList(meetings: List<ClassMeeting>) {
    val grouped = remember(meetings) {
        meetings
            .groupBy { it.weekDay }
            .toList()
            .sortedBy { (day, _) -> weekdayIndex(day) }
            .map { (day, classes) ->
                day to classes.sortedBy { ApiDate.instant(it.timeFrom)?.toEpochMilli() ?: Long.MAX_VALUE }
            }
    }
    val todayName = remember { LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) }
    val semester = meetings.firstOrNull()?.semester?.takeIf { it.isNotBlank() }
    val dateFrom = ApiDate.formatDate(meetings.firstOrNull()?.dateFrom)
    val dateTo = ApiDate.formatDate(meetings.firstOrNull()?.dateTo)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (semester != null) {
            item(key = "semester") {
                Column(Modifier.padding(bottom = 4.dp)) {
                    Text(semesterLabel(semester), style = MaterialTheme.typography.titleMedium)
                    if (dateFrom != null && dateTo != null) {
                        Text(
                            text = "$dateFrom – $dateTo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        grouped.forEach { (day, classes) ->
            val isToday = day.equals(todayName, ignoreCase = true)
            item(key = "day_$day") { DayHeader(day, isToday) }
            items(classes, key = { it.id }) { ClassCard(it, isToday) }
        }
    }
}

@Composable
private fun DayHeader(day: String, isToday: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = day, style = MaterialTheme.typography.titleSmall)
        if (isToday) {
            Spacer(Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun MidtermBanner(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "Midterm week · $label", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ClassCard(meeting: ClassMeeting, isToday: Boolean) {
    val accent = accentFor(meeting.courseId ?: meeting.title)

    Card(
        shape = RoundedCornerShape(20.dp),
        border = if (isToday) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        val contentColor = LocalContentColor.current
        val secondaryColor =
            if (isToday) contentColor.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = ApiDate.formatTime(meeting.timeFrom) ?: "--:--",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = ApiDate.formatTime(meeting.timeTo) ?: "--:--",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor,
                )
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
            ) {
                Text(
                    text = meeting.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                meeting.hall?.takeIf { it.isNotBlank() }
                    ?.let { MetaRow(Icons.Filled.LocationOn, it, secondaryColor) }
                meeting.instructor?.takeIf { it.isNotBlank() }
                    ?.let { MetaRow(Icons.Filled.Person, it, secondaryColor) }
                meeting.section?.takeIf { it.isNotBlank() }?.let { section ->
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = contentColor.copy(alpha = 0.12f),
                        contentColor = contentColor,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "Section $section",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalsList(finals: List<FinalExam>) {
    if (finals.isEmpty()) {
        MessageState(
            icon = Icons.Filled.EventBusy,
            title = "No final exams scheduled yet",
            message = "The exam timetable is usually published closer to the end of the term.",
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(finals, key = { it.id }) { FinalCard(it) }
    }
}

@Composable
private fun FinalCard(exam: FinalExam) {
    val title = exam.title ?: exam.code ?: exam.courseId ?: "Final exam"
    val accent = accentFor(exam.courseId ?: title)
    val dateText = ApiDate.formatDate(exam.date) ?: exam.weekDay
    val from = ApiDate.formatTime(exam.timeFrom)
    val to = ApiDate.formatTime(exam.timeTo)
    val timeText = when {
        from != null && to != null -> "$from – $to"
        from != null -> from
        else -> null
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                if (dateText != null || timeText != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = listOfNotNull(dateText, timeText).joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                exam.hall?.takeIf { it.isNotBlank() }
                    ?.let { MetaRow(Icons.Filled.LocationOn, it, MaterialTheme.colorScheme.onSurfaceVariant) }
                exam.section?.takeIf { it.isNotBlank() }?.let { section ->
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "Section $section",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = color,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
