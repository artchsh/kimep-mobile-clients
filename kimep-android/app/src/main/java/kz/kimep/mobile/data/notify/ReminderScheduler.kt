package kz.kimep.mobile.data.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import kz.kimep.mobile.data.ApiDate
import kz.kimep.mobile.data.model.ClassMeeting
import kz.kimep.mobile.data.model.FinalExam
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Schedules exact alarms for class and final-exam reminders.
 *
 * Each class occurrence produces up to two alarms: one an hour before and one ten
 * minutes before. Request codes are deterministic
 * (`meetingId * 1000 + weekIndex * 2 + leadKind`) so previously scheduled alarms
 * can be cancelled without persisting each one.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService<AlarmManager>()

    fun rescheduleLessons(
        meetings: List<ClassMeeting>,
        previousIds: Set<Int>,
        hourBefore: Boolean,
        tenMinutesBefore: Boolean,
    ) {
        cancel(previousIds)
        if (!hourBefore && !tenMinutesBefore) return
        meetings.forEach { scheduleMeeting(it, hourBefore, tenMinutesBefore) }
    }

    fun rescheduleFinals(exams: List<FinalExam>, previousIds: Set<Int>, enabled: Boolean) {
        cancelFinals(previousIds)
        if (!enabled) return
        exams.forEach(::scheduleFinal)
    }

    fun cancel(meetingIds: Set<Int>) {
        val am = alarmManager ?: return
        meetingIds.forEach { id ->
            for (week in 0 until MAX_WEEKS) {
                for (kind in 0..1) {
                    val pi = existingPendingIntent(id * 1000 + week * 2 + kind) ?: continue
                    am.cancel(pi)
                    pi.cancel()
                }
            }
        }
    }

    fun cancelFinals(examIds: Set<Int>) {
        val am = alarmManager ?: return
        examIds.forEach { id ->
            val pi = existingPendingIntent(FINAL_CODE_BASE + (id and 0xFFFF)) ?: return@forEach
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun scheduleMeeting(meeting: ClassMeeting, hourBefore: Boolean, tenMinutesBefore: Boolean) {
        val from = ApiDate.localDate(meeting.dateFrom) ?: return
        val to = ApiDate.localDate(meeting.dateTo) ?: return
        val time = ApiDate.localTime(meeting.timeFrom) ?: return
        val day = dayOfWeek(meeting.weekDay) ?: return

        val now = ZonedDateTime.now()
        var date: LocalDate = from.with(TemporalAdjusters.nextOrSame(day))
        if (date.isBefore(now.toLocalDate())) {
            date = now.toLocalDate().with(TemporalAdjusters.nextOrSame(day))
        }

        var week = 0
        while (!date.isAfter(to) && week < MAX_WEEKS) {
            val startAt = LocalDateTime.of(date, time)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            if (hourBefore) {
                scheduleClassAlarm(meeting, week, LEAD_HOUR, startAt - HOUR_MS, "Class in 1 hour")
            }
            if (tenMinutesBefore) {
                scheduleClassAlarm(meeting, week, LEAD_TEN, startAt - TEN_MIN_MS, "Class in 10 minutes")
            }

            date = date.plusWeeks(1)
            week++
        }
    }

    private fun scheduleClassAlarm(
        meeting: ClassMeeting,
        week: Int,
        leadKind: Int,
        triggerAt: Long,
        title: String,
    ) {
        if (triggerAt <= System.currentTimeMillis()) return
        val code = meeting.id * 1000 + week * 2 + leadKind
        val text = buildString {
            append(meeting.title)
            append("\nStarts at ").append(ApiDate.formatTime(meeting.timeFrom).orEmpty())
            meeting.hall?.takeIf { it.isNotBlank() }?.let { append(" • ").append(it) }
            meeting.instructor?.takeIf { it.isNotBlank() }?.let { append("\n").append(it) }
        }
        setExact(triggerAt, pendingIntent(code, title, text, Notifications.CHANNEL_LESSONS))
    }

    private fun scheduleFinal(exam: FinalExam) {
        val date = ApiDate.localDate(exam.date) ?: return
        val time = ApiDate.localTime(exam.timeFrom) ?: LocalTime.of(9, 0)
        val triggerAt = LocalDateTime.of(date.minusDays(1), time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (triggerAt <= System.currentTimeMillis()) return

        val code = FINAL_CODE_BASE + (exam.id and 0xFFFF)
        val text = buildString {
            append(exam.title ?: exam.code ?: "Final exam")
            append("\nTomorrow at ").append("%02d:%02d".format(time.hour, time.minute))
            exam.hall?.takeIf { it.isNotBlank() }?.let { append(" • ").append(it) }
        }
        setExact(triggerAt, pendingIntent(code, "Final exam tomorrow", text, Notifications.CHANNEL_FINALS))
    }

    private fun setExact(triggerAt: Long, pi: PendingIntent) {
        val am = alarmManager ?: return
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun pendingIntent(code: Int, title: String, text: String, channel: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_TEXT, text)
            putExtra(ReminderReceiver.EXTRA_CHANNEL, channel)
            putExtra(ReminderReceiver.EXTRA_ID, code)
        }
        return PendingIntent.getBroadcast(
            context,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun existingPendingIntent(code: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            code,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun dayOfWeek(name: String): DayOfWeek? =
        runCatching { DayOfWeek.valueOf(name.trim().uppercase()) }.getOrNull()

    companion object {
        private const val MAX_WEEKS = 30
        private const val HOUR_MS = 60L * 60L * 1000L
        private const val TEN_MIN_MS = 10L * 60L * 1000L
        private const val LEAD_HOUR = 0
        private const val LEAD_TEN = 1
        private const val FINAL_CODE_BASE = 9_000_000
    }
}
