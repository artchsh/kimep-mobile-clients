package kz.kimep.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kz.kimep.mobile.data.analytics.ConsentState
import kz.kimep.mobile.ui.KimepRoot
import kz.kimep.mobile.ui.theme.KimepTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionWhenReady()
        setContent {
            KimepTheme {
                KimepRoot()
            }
        }
    }

    /**
     * Defer the system notification prompt until after the first-run analytics notice has
     * been answered, so the two dialogs don't stack on a fresh install.
     */
    private fun requestNotificationPermissionWhenReady() {
        lifecycleScope.launch {
            val container = (application as KimepApp).container
            if (container.analyticsEnabled) {
                container.analyticsStore.consent.first {
                    it != ConsentState.Loading && it != ConsentState.Undecided
                }
            }
            requestNotificationPermission()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }
}
