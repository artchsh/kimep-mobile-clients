package kz.kimep.mobile.data.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kz.kimep.mobile.KimepApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms are cleared on reboot / app update, so rebuild them. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val app = context.applicationContext as? KimepApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.reminderManager.refresh()
            } catch (_: Throwable) {
                // best effort
            } finally {
                pending.finish()
            }
        }
    }
}
