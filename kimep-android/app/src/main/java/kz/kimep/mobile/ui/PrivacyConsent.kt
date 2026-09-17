package kz.kimep.mobile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * First-run, dismissible notice. Analytics is on by default (opt-out); this explains
 * what is collected and offers a one-tap opt-out without blocking the app.
 */
@Composable
fun AnalyticsFirstRunDialog(
    onKeepEnabled: () -> Unit,
    onOptOut: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepEnabled,
        icon = { Icon(Icons.Filled.PrivacyTip, contentDescription = null) },
        title = { Text("Anonymous statistics") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "This app collects anonymous usage statistics to see what is " +
                        "used and what is not. It is on by default.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                PrivacyNoticeBody()
            }
        },
        confirmButton = {
            TextButton(onClick = onKeepEnabled) { Text("Keep on") }
        },
        dismissButton = {
            TextButton(onClick = onOptOut) { Text("Turn off") }
        },
    )
}

@Composable
fun PrivacyNoticeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.PrivacyTip, contentDescription = null) },
        title = { Text("Anonymous statistics") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PrivacyNoticeBody()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun PrivacyNoticeBody() {
    Column {
        NoticeSection(
            title = "What we collect",
            lines = listOf(
                "Which screens you open and which buttons you tap.",
                "A random ID generated on this device, used to count returning users.",
                "App version, language and screen size.",
            ),
        )
        NoticeSection(
            title = "What we never collect",
            lines = listOf(
                "Your name, student ID, password or session.",
                "Your grades, courses, schedule or any content you view.",
                "Your location, contacts, or the device advertising ID.",
            ),
        )
        NoticeSection(
            title = "Why",
            lines = listOf(
                "This is a proof of concept. The data is used purely for analytics: " +
                    "what is used, what is not, and whether people come back.",
            ),
        )
        NoticeSection(
            title = "Who sees it",
            lines = listOf(
                "Nobody today — it is not shared with anyone.",
                "In the future it may be shared with KIMEP administration to inform " +
                    "product decisions.",
            ),
        )
        NoticeSection(
            title = "Your control",
            lines = listOf(
                "This is on by default and you can turn it off at any time in " +
                    "Settings → Privacy. It takes effect immediately, and turning it " +
                    "off also forgets the random ID.",
            ),
        )
    }
}

@Composable
private fun NoticeSection(title: String, lines: List<String>) {
    Column(Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        lines.forEach { line ->
            Row(Modifier.padding(bottom = 2.dp)) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
