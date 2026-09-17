package kz.kimep.mobile.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The API returns ASP.NET JSON dates as "/Date(<epoch millis>)/".
 *
 * Two shapes are used:
 *  - absolute dates (Date_From, Date_To, ExpiredOn): epoch millis, UTC.
 *  - time-of-day (Time_From, Time_To): encoded on the OLE base date 1899-12-30.
 *
 * KIMEP local time is a fixed UTC+5 (Kazakhstan unified its offset in 2024). A fixed
 * offset is used deliberately so historical/OLE dates are not affected by timezone
 * database rules for 1899.
 */
object ApiDate {

    /** Offset for absolute date fields (Date_From, Date_To, ExpiredOn): UTC+5. */
    val KIMEP_OFFSET: ZoneOffset = ZoneOffset.ofHours(5)

    /**
     * Offset for time-of-day fields (Time_From, Time_To).
     *
     * The backend serialises class times using KIMEP's legacy pre-2024 offset of
     * UTC+6 (Kazakhstan unified on UTC+5 in March 2024). Reading them as UTC+5
     * made every lesson appear one hour early, so times use +6 while dates keep
     * +5. The day of an absolute date is identical under either offset.
     */
    val KIMEP_TIME_OFFSET: ZoneOffset = ZoneOffset.ofHours(6)

    private val numberRegex = Regex("-?\\d+")

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    fun instant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        val match = numberRegex.find(raw) ?: return null
        return runCatching { Instant.ofEpochMilli(match.value.toLong()) }.getOrNull()
    }

    /** Absolute date/time (for Date_* fields) in KIMEP local time (UTC+5). */
    fun localDateTime(raw: String?): LocalDateTime? =
        instant(raw)?.atOffset(KIMEP_OFFSET)?.toLocalDateTime()

    fun localDate(raw: String?): LocalDate? = localDateTime(raw)?.toLocalDate()

    /** Time-of-day (for Time_* fields) in KIMEP legacy local time (UTC+6). */
    fun localTime(raw: String?): LocalTime? =
        instant(raw)?.atOffset(KIMEP_TIME_OFFSET)?.toLocalTime()

    fun formatDate(raw: String?): String? = localDate(raw)?.format(dateFormatter)

    /** "HH:mm" in KIMEP local time, or null. */
    fun formatTime(raw: String?): String? =
        localTime(raw)?.let { "%02d:%02d".format(it.hour, it.minute) }
}
