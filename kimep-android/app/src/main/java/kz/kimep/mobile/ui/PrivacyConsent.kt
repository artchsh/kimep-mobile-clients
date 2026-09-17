package kz.kimep.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyConsentScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PrivacyTip,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Your privacy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "This app collects anonymous usage statistics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                PrivacyNoticeBody()
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Agree and continue", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
                Text("Continue without sharing")
            }
        }
    }
}

@Composable
fun PrivacyNoticeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.PrivacyTip, contentDescription = null) },
        title = { Text("Anonymous statistics") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
            ) {
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
                "You can turn this off at any time in Settings and it takes effect " +
                    "immediately.",
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
