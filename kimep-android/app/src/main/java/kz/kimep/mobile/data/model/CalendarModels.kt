package kz.kimep.mobile.data.model

import kotlinx.serialization.Serializable

/**
 * Academic calendar parsed from the official KIMEP PDFs by tools/parse_calendar.py
 * and bundled as assets/calendar.json.
 */
@Serializable
data class CalendarData(
    val years: List<CalendarYear> = emptyList(),
)

@Serializable
data class CalendarYear(
    val id: String = "",
    val title: String = "",
    val semesters: List<CalendarSemester> = emptyList(),
    val closed: List<CalendarClosure> = emptyList(),
)

@Serializable
data class CalendarSemester(
    val id: String = "",
    val name: String = "",
    val events: List<CalendarEvent> = emptyList(),
)

@Serializable
data class CalendarEvent(
    val title: String = "",
    val start: String? = null,
    val end: String? = null,
    val display: String = "",
)

@Serializable
data class CalendarClosure(
    val title: String = "",
    val note: String? = null,
    val dates: List<CalendarEvent> = emptyList(),
)
