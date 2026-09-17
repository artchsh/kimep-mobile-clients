package kz.kimep.mobile.di

import android.content.Context
import kz.kimep.mobile.data.CalendarRepository
import kz.kimep.mobile.data.KimepApi
import kz.kimep.mobile.data.KimepRepository
import kz.kimep.mobile.data.ScheduleCache
import kz.kimep.mobile.data.SessionStore
import kz.kimep.mobile.data.SettingsStore
import kz.kimep.mobile.data.notify.ReminderManager
import kz.kimep.mobile.data.notify.ReminderScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val sessionStore: SessionStore = SessionStore(appContext)
    val settingsStore: SettingsStore = SettingsStore(appContext)
    val calendarRepository: CalendarRepository = CalendarRepository(appContext)
    val scheduleCache: ScheduleCache = ScheduleCache(appContext)

    private val api: KimepApi = KimepApi()
    val repository: KimepRepository = KimepRepository(api, sessionStore)

    private val reminderScheduler: ReminderScheduler = ReminderScheduler(appContext)
    val reminderManager: ReminderManager =
        ReminderManager(repository, sessionStore, settingsStore, reminderScheduler)
}
