package com.atlabs.percentpro.ui.screens.mocktest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R
import com.atlabs.percentpro.content.formatMarks
import com.atlabs.percentpro.model.MockTestResult
import com.atlabs.percentpro.model.RewardOutcome
import com.atlabs.percentpro.ui.components.RewardEarnedPanel

/**
 * Result summary.
 *
 * Accuracy and percentage score are shown as separate, separately labelled
 * numbers because they answer different questions: accuracy ignores what the
 * learner skipped, percentage score is measured against the whole paper after
 * negative marking.
 */
@Composable
fun MockTestResultScreen(
    result: MockTestResult?,
    onReview: () -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit,
    reward: RewardOutcome = RewardOutcome.NONE,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (result == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.mock_no_questions))
            }
            return@Scaffold
        }

        val scoreText = result.score.formatMarks()
        val maxText = result.maxMarks.formatMarks()
        val summaryDescription = stringResource(
            R.string.cd_mock_score_summary,
            scoreText,
            maxText,
            result.accuracyPercent,
            result.percentagePercent
        )
        val totalSeconds = (result.timeTakenMillis / 1000L).coerceAtLeast(0L)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "title") {
                Column {
                    Text(
                        text = stringResource(result.testTitleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = stringResource(R.string.mock_results_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = stringResource(
                            if (result.didTimeOut) {
                                R.string.mock_results_timed_out_note
                            } else {
                                R.string.mock_results_manually_submitted_note
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.didTimeOut) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item(key = "score") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = summaryDescription
                            }
                    ) {
                        Text(
                            text = stringResource(R.string.mock_result_score),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = stringResource(R.string.mock_score_value, scoreText, maxText),
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            // "Out of 8.00", not the score format. Reusing
                            // mock_score_value here used to render the maximum
                            // as "8.00 / 8.00".
                            text = stringResource(R.string.mock_result_max_marks) + ": " +
                                stringResource(R.string.mock_result_out_of, maxText),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(modifier = Modifier.padding(top = 12.dp)) {
                            MetricColumn(
                                label = stringResource(R.string.mock_result_accuracy),
                                value = stringResource(
                                    R.string.mock_percent_value,
                                    result.accuracyPercent
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            MetricColumn(
                                label = stringResource(R.string.mock_result_percentage),
                                value = stringResource(
                                    R.string.mock_percent_value,
                                    result.percentagePercent
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item(key = "counts") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CountRow(stringResource(R.string.mock_result_total), result.totalQuestions)
                    CountRow(stringResource(R.string.mock_result_attempted), result.correct + result.incorrect)
                    CountRow(stringResource(R.string.mock_result_correct), result.correct)
                    CountRow(stringResource(R.string.mock_result_incorrect), result.incorrect)
                    CountRow(stringResource(R.string.mock_result_unanswered), result.unanswered)
                    CountRow(stringResource(R.string.mock_result_review), result.markedForReview)
                    CountRow(
                        label = stringResource(R.string.mock_result_time_taken),
                        valueString = if (totalSeconds >= 60) {
                            stringResource(
                                R.string.mock_result_time_value,
                                (totalSeconds / 60L).toInt(),
                                (totalSeconds % 60L).toInt()
                            )
                        } else {
                            stringResource(R.string.mock_result_time_value_seconds, totalSeconds.toInt())
                        }
                    )
                }
            }

            if (!reward.isEmpty) {
                item(key = "reward") {
                    RewardEarnedPanel(outcome = reward)
                }
            }

            item(key = "actions") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onReview,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.mock_result_review_all),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = stringResource(R.string.mock_result_retry),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.mock_result_return))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun CountRow(label: String, value: Int, modifier: Modifier = Modifier) =
    CountRow(label, value.toString(), modifier)

@Composable
private fun CountRow(label: String, valueString: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = valueString, style = MaterialTheme.typography.labelLarge)
    }
}
