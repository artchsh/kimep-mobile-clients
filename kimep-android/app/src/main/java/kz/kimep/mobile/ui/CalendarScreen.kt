package kz.kimep.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.kimep.mobile.data.CalendarRepository
import kz.kimep.mobile.data.model.CalendarClosure
import kz.kimep.mobile.data.model.CalendarEvent
import kz.kimep.mobile.ui.components.LoadingState
import kz.kimep.mobile.ui.components.MessageState
import kz.kimep.mobile.vm.CalendarViewModel
import java.time.LocalDate

@Composable
fun CalendarScreen(
    repository: CalendarRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.factory(repository))
    val state = viewModel.uiState
    var yearIndex by rememberSaveable { mutableIntStateOf(0) }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }

    when {
        state.loading -> LoadingState(modifier)

        state.error != null -> MessageState(
            icon = Icons.Filled.CloudOff,
            title = "Couldn't load the calendar",
            message = state.error,
            actionLabel = "Retry",
            onAction = { viewModel.load() },
            modifier = modifier,
        )

        state.data.years.isEmpty() -> MessageState(
            icon = Icons.AutoMirrored.Filled.EventNote,
            title = "No calendar data",
            modifier = modifier,
        )

        else -> {
            val year = state.data.years.getOrElse(yearIndex) { state.data.years.first() }
            val tabTitles = remember(year) { year.semesters.map { it.name } + "Closed" }

            Column(modifier.fillMaxSize()) {
                if (state.data.years.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.data.years.forEachIndexed { index, y ->
                            FilterChip(
                                selected = index == yearIndex,
                                onClick = {
                                    yearIndex = index
                                    tabIndex = 0
                                },
                                label = { Text(y.id) },
                            )
                        }
                    }
                }

                PrimaryScrollableTabRow(
                    selectedTabIndex = tabIndex.coerceIn(0, tabTitles.lastIndex),
                    edgePadding = 16.dp,
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                            text = { Text(title) },
                        )
                    }
                }

                if (tabIndex < year.semesters.size) {
                    EventList(year.semesters[tabIndex].events)
                } else {
                    ClosureList(year.closed)
                }
            }
        }
    }
}

private enum class EventStatus { Past, Current, Future }

private fun statusOf(event: CalendarEvent, today: String): EventStatus {
    val start = event.start
    val end = event.end ?: event.start
    return when {
        end == null -> EventStatus.Future
        end < today -> EventStatus.Past
        start != null && start <= today && end >= today -> EventStatus.Current
        else -> EventStatus.Future
    }
}

@Composable
private fun EventList(events: List<CalendarEvent>) {
    if (events.isEmpty()) {
        MessageState(
            icon = Icons.AutoMirrored.Filled.EventNote,
            title = "No dates for this semester",
        )
        return
    }
    val today = remember { LocalDate.now().toString() }
    val statuses = remember(events, today) { events.map { statusOf(it, today) } }
    val firstCurrentOrUpcoming = remember(statuses) { statuses.indexOfFirst { it != EventStatus.Past } }
    val firstUpcoming = remember(statuses) { statuses.indexOfFirst { it == EventStatus.Future } }
    val listState = rememberLazyListState()

    // Open with the current (or next) event at the top; scroll up for past events.
    LaunchedEffect(events) {
        val target = if (firstCurrentOrUpcoming > 0) firstCurrentOrUpcoming else 0
        listState.scrollToItem(target)
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(events) { index, event ->
            EventRow(
                event = event,
                status = statuses[index],
                isNext = index == firstUpcoming,
            )
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, status: EventStatus, isNext: Boolean) {
    val container = when {
        status == EventStatus.Current -> MaterialTheme.colorScheme.primaryContainer
        isNext -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val content = when {
        status == EventStatus.Current -> MaterialTheme.colorScheme.onPrimaryContainer
        isNext -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (status == EventStatus.Past) 0.55f else 1f)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (status == EventStatus.Current || isNext) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    color = content,
                )
                Spacer(Modifier.height(2.dp))
                Text(text = event.display, style = MaterialTheme.typography.bodyMedium, color = content)
            }
            when {
                status == EventStatus.Current -> StatusBadge(
                    text = "Now",
                    background = MaterialTheme.colorScheme.primary,
                    foreground = MaterialTheme.colorScheme.onPrimary,
                )
                isNext -> StatusBadge(
                    text = "Next",
                    background = MaterialTheme.colorScheme.secondary,
                    foreground = MaterialTheme.colorScheme.onSecondary,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, background: Color, foreground: Color) {
    Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun ClosureList(closed: List<CalendarClosure>) {
    if (closed.isEmpty()) {
        MessageState(Icons.AutoMirrored.Filled.EventNote, "No closures listed")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "closed_header") {
            Text(
                text = "KIMEP is closed",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(closed) { closure ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(closure.title, style = MaterialTheme.typography.bodyLarge)
                    closure.dates.forEach { date ->
                        Text(
                            text = date.display,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    closure.note?.takeIf { it.isNotBlank() }?.let { note ->
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
