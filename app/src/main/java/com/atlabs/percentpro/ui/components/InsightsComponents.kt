package com.atlabs.percentpro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R
import com.atlabs.percentpro.content.TopicPerformance
import com.atlabs.percentpro.content.TopicStatus

/**
 * The name a topic gets on the insights screen.
 *
 * Resolved through the lesson catalogue, so a topic reads exactly as it does on
 * the learning path and in practice. Falls back to the storage key only if a
 * topic is not in the catalogue, which keeps the row renderable rather than
 * showing an empty name.
 */
@Composable
fun topicDisplayName(topicKey: String, titleResolver: (String) -> Int?): String {
    val titleRes = titleResolver(topicKey) ?: return topicKey
    return stringResource(titleRes)
}

/**
 * Localised label for a topic's status.
 *
 * Returned rather than rendered so the same string feeds both the visible row
 * and its accessibility description.
 */
@Composable
fun topicStatusLabel(status: TopicStatus): String = when (status) {
    TopicStatus.NOT_ENOUGH_DATA -> stringResource(R.string.insights_status_not_enough_data)
    TopicStatus.NEEDS_PRACTICE -> stringResource(R.string.insights_status_needs_practice)
    TopicStatus.IMPROVING -> stringResource(R.string.insights_status_improving)
    TopicStatus.STRONG -> stringResource(R.string.insights_status_strong)
}

/** Colour for the accuracy bar. Paired with [topicStatusLabel], never used alone. */
@Composable
fun topicStatusColor(status: TopicStatus) = when (status) {
    TopicStatus.NOT_ENOUGH_DATA -> MaterialTheme.colorScheme.outline
    TopicStatus.NEEDS_PRACTICE -> MaterialTheme.colorScheme.error
    TopicStatus.IMPROVING -> MaterialTheme.colorScheme.primary
    TopicStatus.STRONG -> MaterialTheme.colorScheme.secondary
}

/**
 * One topic's row on the dashboard.
 *
 * Status is carried by a text label and a bar length as well as colour, so the
 * information survives greyscale and colour-blind viewing.
 */
@Composable
fun TopicPerformanceRow(
    title: String,
    performance: TopicPerformance,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val statusLabel = topicStatusLabel(performance.status)
    val barDescription = stringResource(
        R.string.cd_insights_accuracy,
        performance.accuracyPercent,
        performance.attempted
    )
    val rowDescription = stringResource(
        R.string.cd_topic_performance,
        title,
        performance.attempted,
        performance.correct,
        performance.accuracyPercent,
        statusLabel
    )

    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = rowDescription },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.insights_topic_counts,
                        performance.attempted,
                        performance.correct
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.insights_accuracy_value, performance.accuracyPercent),
                    style = MaterialTheme.typography.titleSmall,
                    color = topicStatusColor(performance.status)
                )
            }

            LinearProgressIndicator(
                progress = { performance.accuracyFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(8.dp)
                    .clearAndSetSemantics { contentDescription = barDescription },
                color = topicStatusColor(performance.status),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {}
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = topicStatusColor(performance.status)
                )
                if (performance.status == TopicStatus.NOT_ENOUGH_DATA) {
                    Text(
                        text = "  " + stringResource(
                            R.string.insights_status_not_enough_data_body,
                            com.atlabs.percentpro.content.InsightThresholds
                                .MIN_ATTEMPTS_FOR_CLASSIFICATION
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (performance.isHighAccuracyOnEasyQuestionsOnly) {
                Text(
                    text = stringResource(R.string.insights_easy_only_caution),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/** Headline block: attempted questions, accuracy and completed lessons. */
@Composable
fun InsightsSummaryCard(
    attempted: Int,
    accuracyPercent: Int,
    lessonsCompleted: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.insights_overall_attempted),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = attempted.toString(),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(
                    R.string.insights_overall_accuracy,
                    stringResource(R.string.insights_accuracy_value, accuracyPercent)
                ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = stringResource(R.string.insights_lessons_completed_value, lessonsCompleted),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * The recent window against the window before it.
 *
 * When there is not enough to compare, it says so in words rather than showing a
 * number that two small samples cannot support.
 */
@Composable
fun RecentPerformanceCard(
    recentAccuracyPercent: Int,
    previousAccuracyPercent: Int,
    deltaPercent: Int,
    isImproving: Boolean,
    isDeclining: Boolean,
    hasComparison: Boolean,
    windowSize: Int,
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
                text = stringResource(R.string.insights_recent),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.insights_recent_window, windowSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.insights_recent_accuracy_value,
                        recentAccuracyPercent
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (hasComparison) {
                    // Trend is carried by the words, not by an arrow glyph, so
                    // it reads the same at any text size and in greyscale.
                    Text(
                        text = when {
                            isImproving -> stringResource(R.string.insights_trend_up, deltaPercent)
                            isDeclining -> stringResource(
                                R.string.insights_trend_down,
                                -deltaPercent
                            )
                            else -> stringResource(R.string.insights_trend_steady)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            isImproving -> MaterialTheme.colorScheme.secondary
                            isDeclining -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Text(
                text = if (hasComparison) {
                    stringResource(R.string.insights_vs_previous, windowSize) + ": " +
                        stringResource(
                            R.string.insights_accuracy_value,
                            previousAccuracyPercent
                        )
                } else {
                    stringResource(R.string.insights_trend_locked)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}