package kz.kimep.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kz.kimep.mobile.data.ApiDate
import kz.kimep.mobile.data.KimepApi
import kz.kimep.mobile.data.KimepRepository
import kz.kimep.mobile.data.SessionState
import kz.kimep.mobile.data.analytics.Analytics
import kz.kimep.mobile.data.analytics.AnalyticsEvents
import kz.kimep.mobile.vm.ProfileViewModel
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(
    session: SessionState.LoggedIn,
    repository: KimepRepository,
    analytics: Analytics,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProfileViewModel =
        viewModel(factory = ProfileViewModel.factory(repository, session.id))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(name = session.fullName, avatarId = session.id)

        Spacer(Modifier.height(16.dp))
        Text(
            text = session.fullName.ifBlank { "Student" },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        if (session.studentId.isNotBlank()) {
            Text(
                text = "Student ID ${session.studentId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                InfoRow(Icons.Filled.Badge, "Student ID", session.studentId.ifBlank { "—" })
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                InfoRow(Icons.Filled.School, "Program", session.program ?: "—")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                InfoRow(Icons.Filled.Schedule, "Session expires", expiresText(session.expiresOnMs))
            }
        }

        if (viewModel.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = viewModel.error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                analytics.track(AnalyticsEvents.LOGOUT)
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sign out")
        }

        Spacer(Modifier.height(6.dp))

        TextButton(onClick = { viewModel.refresh() }, enabled = !viewModel.refreshing) {
            if (viewModel.refreshing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Refresh profile")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Avatar(name: String, avatarId: String) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { "K" }

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        AsyncImage(
            model = KimepApi.avatarUrl(avatarId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape),
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.End,
        )
    }
}

private val expiryFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy")

private fun expiresText(expiresOnMs: Long?): String =
    expiresOnMs
        ?.let { ApiDate.KIMEP_OFFSET.let { offset -> java.time.Instant.ofEpochMilli(it).atOffset(offset) } }
        ?.format(expiryFormatter)
        ?: "—"
