package com.atlabs.percentpro.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlabs.percentpro.R
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.content.ExamCatalog
import com.atlabs.percentpro.model.ExamId
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.ui.components.SectionHeader

@Composable
fun PracticeTopicRoute(
    container: AppContainer,
    onTopicSelected: (LessonId) -> Unit,
    onExamSelected: (LessonId, ExamId) -> Unit,
    onOpenMockTests: () -> Unit,
    onOpenExamPapers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PracticeTopicViewModel =
        viewModel(factory = PracticeTopicViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PracticeTopicScreen(
        uiState = uiState,
        onTopicSelected = { topic ->
            if (viewModel.canOpen(topic)) onTopicSelected(topic)
        },
        onExamSelected = { topic, examId ->
            // Re-checked here rather than trusted from the card, for the same
            // reason as canOpen: the card does not know what the bank holds.
            if (viewModel.canOpenExam(topic, examId)) onExamSelected(topic, examId)
        },
        onOpenMockTests = onOpenMockTests,
        onOpenExamPapers = onOpenExamPapers,
        modifier = modifier
    )
}

@Composable
fun PracticeTopicScreen(
    uiState: PracticeTopicsUiState,
    onTopicSelected: (LessonId) -> Unit,
    onExamSelected: (LessonId, ExamId) -> Unit,
    onOpenMockTests: () -> Unit,
    onOpenExamPapers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val contentWidth = if (maxWidth > 640.dp) 640.dp else maxWidth

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .width(contentWidth)
                        .testTag("practice_topic_list"),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 20.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "header") {
                        SectionHeader(
                            title = stringResource(R.string.practice_title),
                            subtitle = stringResource(R.string.practice_subtitle)
                        )
                    }

                    // Mock tests live here rather than in the bottom bar, so the
                    // bar keeps its four familiar destinations.
                    item(key = "mock_tests") {
                        MockTestsCard(onClick = onOpenMockTests)
                    }
                    items(
                        items = uiState.topics,
                        key = { it.lessonId.storageKey }
                    ) { topic ->
                        PracticeTopicCard(
                            topic = topic,
                            onClick = { onTopicSelected(topic.lessonId) },
                            onExamSelected = onExamSelected
                        )
                    }

                    // Last, because it is a way into the archive rather than a
                    // topic: someone browsing wants their way through the list
                    // before they want to be taken somewhere else.
                    item(key = "exam_papers") {
                        ExamPapersEntryCard(onClick = onOpenExamPapers)
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeTopicCard(
    topic: PracticeTopic,
    onClick: () -> Unit,
    onExamSelected: (LessonId, ExamId) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(topic.titleRes)
    val questionCountText = pluralStringResource(
        R.plurals.practice_topic_count,
        topic.questionCount,
        topic.questionCount
    )
    val lastResultText = topic.lastResult?.let { result ->
        stringResource(
            R.string.practice_topic_last_result,
            result.accuracyPercent,
            result.correct,
            result.total
        )
    } ?: stringResource(R.string.practice_topic_never_attempted)

    val topicDescription = stringResource(
        R.string.cd_practice_topic,
        title,
        questionCountText,
        lastResultText
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = topicDescription
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = questionCountText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = lastResultText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            ExamFilterRow(
                topic = topic,
                onExamSelected = onExamSelected
            )

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                shape = MaterialTheme.shapes.large,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.practice_start_topic),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExamFilterRow(
    topic: PracticeTopic,
    onExamSelected: (LessonId, ExamId) -> Unit,
    modifier: Modifier = Modifier
) {
    if (topic.examCounts.isEmpty()) return
    val catalog = remember { ExamCatalog() }
    // Built here rather than inside the semantics block: that lambda is not a
    // composable scope, so it cannot resolve a string resource.
    val rowDescription = examFilterDescription(topic, catalog)

    Column(modifier = modifier.padding(top = 14.dp)) {
        Text(
            text = stringResource(R.string.practice_exam_filter_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // FlowRow, not Row: seven exam chips will not fit a narrow phone and a
        // Row cannot wrap, so the labels would be clipped mid-word once
        // translated.
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .semantics { contentDescription = rowDescription },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            topic.examCounts.forEach { (examId, count) ->
                val exam = catalog.examById(examId) ?: return@forEach
                val label = stringResource(exam.titleRes)
                FilterChip(
                    selected = false,
                    onClick = { onExamSelected(topic.lessonId, examId) },
                    label = {
                        Text(
                            text = "$label · $count",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}

/** One spoken summary of the whole row, rather than seven disconnected counts. */
@Composable
private fun examFilterDescription(
    topic: PracticeTopic,
    catalog: ExamCatalog
): String {
    // A plain loop rather than joinToString: the compiler does not treat
    // joinToString's nullable transform parameter as a composable scope, so the
    // string resources could not be resolved inside it.
    val parts = ArrayList<String>(topic.examCounts.size)
    for ((examId, count) in topic.examCounts) {
        val exam = catalog.examById(examId) ?: continue
        parts += pluralStringResource(
            R.plurals.cd_practice_exam_filter,
            count,
            stringResource(exam.titleRes),
            count
        )
    }
    return parts.joinToString(", ")
}

/**
 * Entry to the year-by-year archive.
 *
 * Outlined rather than filled, so it does not compete with the Mock Tests card
 * above it. Both are ways out of this screen rather than subjects to practise.
 */
@Composable
private fun ExamPapersEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(R.string.practice_papers_by_year_title)
    val subtitle = stringResource(R.string.practice_papers_by_year_subtitle)
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $subtitle."
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun MockTestsCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.mock_entry_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.mock_entry_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** Circular badge reused by the results and review screens. */
@Composable
internal fun ResultStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}