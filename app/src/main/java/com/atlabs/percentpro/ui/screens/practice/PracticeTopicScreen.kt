package com.atlabs.percentpro.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.ui.components.SectionHeader

@Composable
fun PracticeTopicRoute(
    container: AppContainer,
    onTopicSelected: (LessonId) -> Unit,
    onOpenMockTests: () -> Unit,
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
        onOpenMockTests = onOpenMockTests,
        modifier = modifier
    )
}

@Composable
fun PracticeTopicScreen(
    uiState: PracticeTopicsUiState,
    onTopicSelected: (LessonId) -> Unit,
    onOpenMockTests: () -> Unit,
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
                        PracticeTopicCard(topic = topic, onClick = { onTopicSelected(topic.lessonId) })
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