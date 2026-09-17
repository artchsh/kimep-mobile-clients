package kz.kimep.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.kimep.mobile.data.KimepRepository
import kz.kimep.mobile.data.analytics.Analytics
import kz.kimep.mobile.data.analytics.AnalyticsEvents
import kz.kimep.mobile.data.model.AssessmentScore
import kz.kimep.mobile.data.model.FinalGrade
import kz.kimep.mobile.data.model.GpaCredits
import kz.kimep.mobile.ui.components.LoadingState
import kz.kimep.mobile.ui.components.MessageState
import kz.kimep.mobile.vm.GradesViewModel

@Composable
fun GradesScreen(
    repository: KimepRepository,
    analytics: Analytics,
    sessionId: String,
    modifier: Modifier = Modifier,
) {
    val viewModel: GradesViewModel =
        viewModel(factory = GradesViewModel.factory(repository, sessionId))
    val state = viewModel.uiState
    var tab by rememberSaveable { mutableIntStateOf(0) }

    when {
        state.loading -> LoadingState(modifier)

        state.error != null && state.finalGrades.isEmpty() -> MessageState(
            icon = Icons.Filled.CloudOff,
            title = "Couldn't load your grades",
            message = state.error,
            actionLabel = "Retry",
            onAction = { viewModel.load() },
            modifier = modifier,
        )

        else -> Column(modifier.fillMaxSize()) {
            GpaCard(state.gpa)
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = {
                        tab = 0
                        analytics.track(AnalyticsEvents.GRADES_TAB, mapOf("tab" to "current"))
                    },
                    text = { Text("Current") },
                )
                Tab(
                    selected = tab == 1,
                    onClick = {
                        tab = 1
                        analytics.track(AnalyticsEvents.GRADES_TAB, mapOf("tab" to "transcript"))
                    },
                    text = { Text("Transcript") },
                )
            }
            when (tab) {
                0 -> AssessmentList(state.assessment)
                else -> TranscriptList(state.finalGrades)
            }
        }
    }
}

@Composable
private fun GpaCard(gpa: GpaCredits) {
    val progress = if (gpa.creditsTaken > 0) {
        (gpa.creditsEarned.toFloat() / gpa.creditsTaken).coerceIn(0f, 1f)
    } else {
        0f
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Cumulative GPA",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "%.2f".format(gpa.gpa),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Credits",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${gpa.creditsEarned}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "of ${gpa.creditsTaken}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun AssessmentList(items: List<AssessmentScore>) {
    if (items.isEmpty()) {
        MessageState(
            icon = Icons.Filled.School,
            title = "No current courses",
            message = "You have no registered courses this term.",
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items) { AssessmentCard(it) }
    }
}

@Composable
private fun AssessmentCard(score: AssessmentScore) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = score.title ?: "Course",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )
            score.code?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScorePill("Assessment 1", score.score1, Modifier.weight(1f))
                ScorePill("Assessment 2", score.score2, Modifier.weight(1f))
                ScorePill("Assessment 3", score.score3, Modifier.weight(1f))
            }
            score.finalAssessment?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Final assessment: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScorePill(label: String, value: Double?, modifier: Modifier = Modifier) {
    val filled = value != null
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (filled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (filled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                text = value?.let { "%.0f".format(it) } ?: "–",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun TranscriptList(items: List<FinalGrade>) {
    if (items.isEmpty()) {
        MessageState(
            icon = Icons.Filled.School,
            title = "No grades yet",
            message = "Completed courses will appear here.",
        )
        return
    }
    val grouped = remember(items) { items.groupBy { it.semester } }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        grouped.forEach { (semester, grades) ->
            item(key = "semester_$semester") {
                Text(
                    text = semesterLabel(semester),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                )
            }
            items(grades) { TranscriptRow(it) }
        }
    }
}

@Composable
private fun TranscriptRow(grade: FinalGrade) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = grade.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                )
                Text(
                    text = "%.2f pts".format(grade.point),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            GradeBadge(grade.grade)
        }
    }
}

@Composable
private fun GradeBadge(grade: String) {
    val (background, foreground) = gradeColors(grade)
    Surface(
        color = background,
        contentColor = foreground,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = grade.ifBlank { "—" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}
