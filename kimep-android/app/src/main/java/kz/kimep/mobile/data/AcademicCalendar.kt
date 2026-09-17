package kz.kimep.mobile.data

import kz.kimep.mobile.data.model.CalendarData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** A week that counts as "midterms" for a semester, derived from the academic calendar. */
data class MidtermWindow(
    val label: String,
    val start: LocalDate,
    val end: LocalDate,
) {
    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}

/**
 * Maps a timetable semester code (e.g. "F2026", "S2026", "SU1/2026") onto the academic
 * calendar's academic-year + semester identifiers, and derives midterm weeks from the
 * calendar's progress-submission deadlines.
 */
object AcademicCalendar {

    /** "F2026" -> "2026-2027"; "S2026" / "SU1/2026" -> "2025-2026". */
    fun yearIdForSemester(semesterCode: String): String? {
        val year = Regex("\\d{4}").find(semesterCode)?.value?.toIntOrNull() ?: return null
        val head = semesterCode.substringBefore('/')
        return when {
            head.startsWith("F", ignoreCase = true) -> "$year-${year + 1}"
            head.startsWith("SU", ignoreCase = true) -> "${year - 1}-$year"
            head.startsWith("S", ignoreCase = true) -> "${year - 1}-$year"
            else -> null
        }
    }

    /** "F2026" -> "F"; "S2026" -> "S"; "SU1/2026" -> "SU1". */
    fun semesterId(semesterCode: String): String? {
        val head = semesterCode.substringBefore('/')
        return when {
            head.startsWith("SU1", ignoreCase = true) -> "SU1"
            head.startsWith("SU2", ignoreCase = true) -> "SU2"
            head.startsWith("F", ignoreCase = true) -> "F"
            head.startsWith("S", ignoreCase = true) -> "S"
            else -> null
        }
    }

    /**
     * Weeks (Mon–Sun) that contain a "Progress Submission" deadline for the given
     * semester. Falls back to an empty list when the calendar has no matching data.
     */
    fun midtermWindows(data: CalendarData, semesterCode: String): List<MidtermWindow> {
        val yearId = yearIdForSemester(semesterCode) ?: return emptyList()
        val semId = semesterId(semesterCode) ?: return emptyList()
        val year = data.years.firstOrNull { it.id == yearId } ?: return emptyList()
        val semester = year.semesters.firstOrNull { it.id == semId } ?: return emptyList()

        return semester.events
            .filter { it.title.contains("Progress Submission", ignoreCase = true) }
            .mapNotNull { event ->
                val date = event.start
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@mapNotNull null
                val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                MidtermWindow(
                    label = shortLabel(event.title),
                    start = monday,
                    end = monday.plusDays(6),
                )
            }
    }

    private fun shortLabel(title: String): String = when {
        title.contains("First", ignoreCase = true) -> "First progress submissions are due"
        title.contains("Second", ignoreCase = true) -> "Second progress submissions are due"
        else -> title
    }
}
