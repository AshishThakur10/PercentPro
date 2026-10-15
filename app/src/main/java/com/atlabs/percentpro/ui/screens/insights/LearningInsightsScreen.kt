package com.atlabs.percentpro.ui.screens.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlabs.percentpro.R
import com.atlabs.percentpro.content.InsightThresholds
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.model.DifficultyFilter
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.PracticeMode
import com.atlabs.percentpro.model.PracticePlan
import com.atlabs.percentpro.model.LearningRecommendation
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.RecommendationReason
import com.atlabs.percentpro.model.RecommendationDestination
import com.atlabs.percentpro.model.RecommendationTitleKey
import com.atlabs.percentpro.ui.components.InsightsSummaryCard
import com.atlabs.percentpro.ui.components.RecentPerformanceCard
import com.atlabs.percentpro.ui.components.RecommendationCard
import com.atlabs.percentpro.ui.components.SectionHeader
import com.atlabs.percentpro.ui.components.TopicPerformanceRow

/**
 * The personalised dashboard.
 *
 * Every figure is derived from the saved answer ledger by
 * [com.atlabs.percentpro.content.LearningAnalyticsEngine]; nothing here invents a
 * number. Where there is not enough history the screen says so rather than
 * filling the gap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningInsightsRoute(
    container: AppContainer,
    onExit: () -> Unit,
    onStartPractice: (PracticePlan) -> Unit,
    onOpenLesson: (LessonId) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LearningInsightsViewModel =
        viewModel(factory = LearningInsightsViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Built once instead of per topic row: this lambda used to re-materialise
    // the entire lesson catalog and rescan it for every topic on screen.
    val titleOfTopic: (String) -> Int? = remember(container) {
        val titlesByKey = container.lessonCatalog.lessons()
            .associate { lesson -> lesson.id.storageKey to lesson.titleRes }
        val lookup: (String) -> Int? = { key -> titlesByKey[key] }
        lookup
    }

    LearningInsightsScreen(
        uiState = uiState,
        titleOf = titleOfTopic,
        onPracticeTopic = { topic, difficulty ->
            viewModel.planForTopic(topic, difficulty)
                .takeIf { !it.isEmpty }
                ?.let(onStartPractice)
        },
        onStartMixed = {
            viewModel.planMixedReview().takeIf { !it.isEmpty }?.let(onStartPractice)
        },
        onTopicClicked = { topic ->
            viewModel.planForTopic(topic).takeIf { !it.isEmpty }?.let(onStartPractice)
        },
        onRecommendationAction = { recommendation ->
            // An empty plan means there is nothing runnable behind this action, so
            // the tap is ignored rather than opening an empty session.
            val plan = when (val destination = recommendation.destination) {
                is RecommendationDestination.OpenLesson -> {
                    onOpenLesson(destination.lessonId)
                    null
                }

                is RecommendationDestination.PracticeTopic ->
                    viewModel.planForTopic(destination.topic)

                is RecommendationDestination.PracticeDifficulty ->
                    viewModel.planForTopic(destination.topic, destination.difficulty.toFilter())

                RecommendationDestination.MixedReview -> viewModel.planMixedReview()

                RecommendationDestination.OpenInsights -> null
            }
            if (plan != null && !plan.isEmpty) onStartPractice(plan)
        },
        onExit = onExit,
        modifier = modifier
    )
}

private fun QuestionDifficulty.toFilter(): DifficultyFilter = when (this) {
    QuestionDifficulty.EASY -> DifficultyFilter.EASY
    QuestionDifficulty.MEDIUM -> DifficultyFilter.MEDIUM
    QuestionDifficulty.HARD -> DifficultyFilter.HARD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningInsightsScreen(
    uiState: InsightsUiState,
    titleOf: (String) -> Int?,
    onPracticeTopic: (LessonId, DifficultyFilter) -> Unit,
    onStartMixed: () -> Unit,
    onTopicClicked: (LessonId) -> Unit,
    onRecommendationAction: (LearningRecommendation) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.insights_title),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.insights_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val contentWidth = if (maxWidth > 640.dp) 640.dp else maxWidth

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier
                        .width(contentWidth)
                        .testTag("insights_list"),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 12.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "header") {
                        SectionHeader(
                            title = stringResource(R.string.insights_title),
                            subtitle = stringResource(R.string.insights_subtitle)
                        )
                    }

                    uiState.recommendation?.let { recommendation ->
                        item(key = "recommendation") {
                            RecommendationCard(
                                title = recommendationTitle(recommendation.titleKey),
                                reason = recommendationReason(recommendation.reasonCode, recommendation.reasonArgs),
                                actionLabel = recommendationActionLabel(recommendation.destination),
                                onAction = { onRecommendationAction(recommendation) }
                            )
                        }
                    }

                    if (!uiState.hasAnyActivity) {
                        item(key = "empty") {
                            EmptyHistoryCard()
                        }
                        item(key = "legacy_note") {
                            LegacyDataNote()
                        }
                    }

                    item(key = "summary") {
                        InsightsSummaryCard(
                            attempted = uiState.analytics.overallAttempted,
                            accuracyPercent = uiState.analytics.overallAccuracyPercent,
                            lessonsCompleted = uiState.analytics.completedLessonCount
                        )
                    }

                    item(key = "recent") {
                        RecentPerformanceCard(
                            recentAccuracyPercent = uiState.analytics.recent.accuracyPercent,
                            previousAccuracyPercent = uiState.analytics.recent.previousAccuracyPercent,
                            deltaPercent = uiState.analytics.recent.accuracyDeltaPercent,
                            isImproving = uiState.analytics.recent.isImproving,
                            isDeclining = uiState.analytics.recent.isDeclining,
                            hasComparison = uiState.analytics.recent.hasComparison,
                            windowSize = InsightThresholds.RECENT_WINDOW_SIZE
                        )
                    }

                    if (uiState.hasAnyActivity) {
                        uiState.analytics.strongestTopic?.let { strongest ->
                            item(key = "strongest") {
                                TopicCallout(
                                    heading = stringResource(R.string.insights_strongest_topic),
                                    title = topicName(strongest.topic, titleOf)
                                )
                            }
                        }
                        uiState.analytics.weakestTopic?.let { weakest ->
                            item(key = "weakest") {
                                TopicCallout(
                                    heading = stringResource(R.string.insights_weakest_topic),
                                    title = topicName(weakest.topic, titleOf)
                                )
                            }
                        }

                        item(key = "mixed") {
                            MixedReviewCard(
                                topicCount = uiState.analytics.studiedTopics.size,
                                enabled = uiState.analytics.studiedTopics.size >=
                                    InsightThresholds.MIN_TOPICS_FOR_MIXED_REVIEW &&
                                    uiState.analytics.studiedTopics.any {
                                        (uiState.questionCounts[it.topic] ?: 0) > 0
                                    },
                                onStart = onStartMixed
                            )
                        }

                        item(key = "by_topic_header") {
                            SectionHeader(
                                title = stringResource(R.string.insights_by_topic),
                                subtitle = stringResource(
                                    R.string.insights_topics_covered,
                                    uiState.analytics.studiedTopics.size
                                ),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        items(
                            items = uiState.analytics.studiedTopics,
                            key = { it.topic.storageKey }
                        ) { performance ->
                            TopicPerformanceRow(
                                title = topicName(performance.topic, titleOf),
                                performance = performance,
                                onClick = { onTopicClicked(performance.topic) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun topicName(topic: LessonId, titleOf: (String) -> Int?): String {
    val titleRes = titleOf(topic.storageKey) ?: return topic.storageKey
    return stringResource(titleRes)
}

@Composable
private fun EmptyHistoryCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.insights_no_data_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.insights_no_data_body),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun LegacyDataNote(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.insights_legacy_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun TopicCallout(
    heading: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = heading,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun MixedReviewCard(
    topicCount: Int,
    enabled: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.targeted_title_mixed),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.targeted_mixed_subtitle, topicCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            TextButton(
                onClick = onStart,
                enabled = enabled,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(stringResource(R.string.targeted_start))
            }
        }
    }
}

@Composable
fun recommendationTitle(key: RecommendationTitleKey): String = when (key) {
    RecommendationTitleKey.RESUME_LESSON -> stringResource(R.string.rec_title_resume_lesson)
    RecommendationTitleKey.CONTINUE_COURSE -> stringResource(R.string.rec_title_continue_course)
    RecommendationTitleKey.PRACTICE_WEAK_TOPIC -> stringResource(R.string.rec_title_practice_weak)
    RecommendationTitleKey.STEP_UP_DIFFICULTY -> stringResource(R.string.rec_title_step_up)
    RecommendationTitleKey.MIXED_REVIEW -> stringResource(R.string.rec_title_mixed_review)
    RecommendationTitleKey.START_WITH_LESSON_ONE ->
        stringResource(R.string.rec_title_start_lesson_one)

    RecommendationTitleKey.KEEP_PRACTISING -> stringResource(R.string.rec_title_keep_practising)
}

@Composable
fun recommendationReason(
    code: RecommendationReason,
    args: List<Int>
): String {
    val value = args.firstOrNull() ?: 0
    return when (code) {
        RecommendationReason.LESSON_IN_PROGRESS ->
            stringResource(R.string.rec_reason_lesson_in_progress)

        RecommendationReason.NEXT_LESSON_UNLOCKED ->
            stringResource(R.string.rec_reason_next_lesson)

        RecommendationReason.LOW_TOPIC_ACCURACY ->
            stringResource(R.string.rec_reason_low_accuracy, value)

        RecommendationReason.STRONG_ON_EASY ->
            stringResource(R.string.rec_reason_strong_on_easy, value)

        RecommendationReason.BALANCED_ACROSS_TOPICS ->
            stringResource(R.string.rec_reason_balanced, value)

        RecommendationReason.NO_HISTORY_YET ->
            stringResource(R.string.rec_reason_no_history)

        RecommendationReason.COURSE_COMPLETE ->
            stringResource(R.string.rec_reason_course_complete)

        RecommendationReason.NO_RECOMMENDATION ->
            stringResource(R.string.rec_reason_no_history)
    }
}

@Composable
fun recommendationActionLabel(destination: RecommendationDestination): String =
    when (destination) {
        is RecommendationDestination.OpenLesson -> stringResource(R.string.rec_continue_learning)
        is RecommendationDestination.PracticeTopic,
        is RecommendationDestination.PracticeDifficulty,
        RecommendationDestination.MixedReview -> stringResource(R.string.rec_start_practice)

        RecommendationDestination.OpenInsights -> stringResource(R.string.rec_open_insights)
    }
