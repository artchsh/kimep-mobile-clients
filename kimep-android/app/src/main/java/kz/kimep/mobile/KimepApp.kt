package kz.kimep.mobile

import android.app.Application
import kz.kimep.mobile.data.notify.Notifications
import kz.kimep.mobile.di.AppContainer

class KimepApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
    }
}
