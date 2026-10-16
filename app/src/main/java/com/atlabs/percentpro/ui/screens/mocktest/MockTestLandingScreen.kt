package com.atlabs.percentpro.ui.screens.mocktest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlabs.percentpro.R
import com.atlabs.percentpro.content.ExamCatalog
import com.atlabs.percentpro.content.formatMarks
import com.atlabs.percentpro.content.formatPenalty
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.ExamDefinition
import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.model.MockTestResult
import com.atlabs.percentpro.ui.components.SectionHeader

@Composable
fun MockTestLandingRoute(
    container: AppContainer,
    language: AppLanguage,
    onStartTest: (MockTestId) -> Unit,
    onOpenResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MockTestLandingViewModel =
        viewModel(factory = MockTestLandingViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MockTestLandingScreen(
        uiState = uiState,
        language = language,
        onStartTest = onStartTest,
        onOpenResult = onOpenResult,
        modifier = modifier
    )
}

@Composable
fun MockTestLandingScreen(
    uiState: MockTestLandingUiState,
    language: AppLanguage,
    onStartTest: (MockTestId) -> Unit,
    onOpenResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showInstructions by rememberSaveable { mutableStateOf(false) }

    // Which paper the instructions dialog belongs to. Held as an id rather than
    // as the item itself so the dialog and the card that opened it cannot drift
    // apart; `uiState.test` (the first paper) was previously used, which meant
    // starting any card would open and then run the *first* test in the list.
    var pendingTestId by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingTest = uiState.tests.firstOrNull { it.config.id.value == pendingTestId }

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
                        .testTag("mock_landing_list"),
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
                            title = stringResource(R.string.mock_entry_title),
                            subtitle = stringResource(R.string.mock_entry_subtitle)
                        )
                    }

                    uiState.tests.forEach { landingItem ->
                        item(key = "test_${landingItem.config.id.value}") {
                            MockTestCard(
                                item = landingItem,
                                language = language,
                                onStart = {
                                    pendingTestId = landingItem.config.id.value
                                    showInstructions = true
                                }
                            )
                        }
                    }

                    item(key = "history_header") {
                        SectionHeader(
                            title = stringResource(R.string.mock_recent_results),
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    if (uiState.recentResults.isEmpty()) {
                        item(key = "history_empty") {
                            Text(
                                text = stringResource(R.string.mock_no_results_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(
                            items = uiState.recentResults,
                            key = { it.sessionId }
                        ) { result ->
                            MockTestResultRow(result = result, onOpen = { onOpenResult(result.sessionId) })
                        }
                    }
                }
            }
        }
    }

    if (showInstructions && pendingTest != null) {
        MockTestInstructionsDialog(
            item = pendingTest,
            onConfirm = {
                showInstructions = false
                // Run the paper whose card was tapped, not the first one.
                onStartTest(pendingTest.config.id)
                pendingTestId = null
            },
            onDismiss = {
                showInstructions = false
                pendingTestId = null
            }
        )
    }
}

@Composable
private fun MockTestCard(
    item: MockTestLandingItem,
    language: AppLanguage,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = item.config
    val examCatalog = remember { ExamCatalog() }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(config.titleRes),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(config.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            // An exam card carries its own scope note rather than repeating the
            // generic disclaimer, which said "not a full examination" and is
            // both too weak and now wrong for these: they use a real scheme.
            val exam = config.examId?.let { examCatalog.examById(it) }
            if (exam != null && exam.isAvailable) {
                Text(
                    text = stringResource(
                        R.string.exam_paper_practice_note,
                        exam.paper.forLanguage(language),
                        exam.section.forLanguage(language)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                ExamPatternBlock(exam = exam)
            } else {
                Text(
                    text = stringResource(R.string.mock_landing_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Column(modifier = Modifier.padding(top = 14.dp)) {
                RuleRow(
                    label = stringResource(R.string.mock_rules_questions),
                    value = stringResource(R.string.mock_rules_questions_value, config.questionCount)
                )
                RuleRow(
                    label = stringResource(R.string.mock_rules_duration),
                    value = stringResource(R.string.mock_rules_duration_value, config.durationMinutes)
                )
                RuleRow(
                    label = stringResource(R.string.mock_rules_marks_correct),
                    value = stringResource(
                        R.string.mock_rules_marks_value_correct,
                        config.marksPerCorrect.formatMarks()
                    )
                )
                // formatPenalty, not formatMarks: a third of a mark is four
                // twelfths and has no terminating decimal, so it has to print as
                // the fraction or the learner cannot check their own arithmetic.
                RuleRow(
                    label = stringResource(R.string.mock_rules_marks_incorrect),
                    value = stringResource(
                        R.string.mock_rules_marks_value_incorrect,
                        config.negativeMark.formatPenalty()
                    )
                )
                RuleRow(
                    label = stringResource(R.string.mock_rules_marks_unanswered),
                    value = stringResource(R.string.mock_rules_marks_value_unanswered)
                )
            }

            Button(
                onClick = onStart,
                enabled = item.isAvailable,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.mock_start_test),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (!item.isAvailable) {
                Text(
                    text = pluralStringResource(
                        R.plurals.mock_insufficient_questions,
                        item.availableQuestions,
                        item.availableQuestions
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * What the real paper looks like, under an exam card.
 *
 * Deliberately shows the real section size rather than the drill's fifteen
 * questions: a learner comparing this app to a previous paper should be able to
 * see exactly how much of that paper percentage covers.
 */
@Composable
private fun ExamPatternBlock(
    exam: ExamDefinition,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = 12.dp)) {
        Text(
            text = stringResource(R.string.exam_card_pattern),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = pluralStringResource(
                R.plurals.exam_card_section_questions,
                exam.realSectionQuestionCount,
                exam.realSectionQuestionCount
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = pluralStringResource(
                R.plurals.exam_card_paper_duration,
                exam.paperDurationMinutes,
                exam.paperDurationMinutes
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            // SSC CHSL times each section at fifteen minutes while SSC CGL gives
            // one allowance for the whole of Tier 1, so the two are described
            // differently rather than sharing one line of copy.
            text = if (exam.hasSectionalTimer) {
                pluralStringResource(
                    R.plurals.exam_card_section_timer,
                    exam.sectionDurationMinutes,
                    exam.sectionDurationMinutes
                )
            } else {
                stringResource(R.string.exam_card_shares_timer)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.exam_card_drill_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun RuleRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MockTestResultRow(
    result: MockTestResult,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scoreText = result.score.formatMarks()
    val line = stringResource(
        R.string.mock_result_line,
        stringResource(result.testTitleRes),
        result.correct,
        result.totalQuestions,
        result.percentagePercent
    )
    val rowDescription = stringResource(
        R.string.cd_mock_result_row,
        stringResource(result.testTitleRes),
        scoreText,
        result.correct,
        result.totalQuestions
    )

    Card(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = rowDescription },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = line, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(
                        if (result.didTimeOut) {
                            R.string.mock_timed_out_badge
                        } else {
                            R.string.mock_manual_badge
                        }
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            TextButton(onClick = onOpen) {
                Text(stringResource(R.string.mock_view_result))
            }
        }
    }
}

@Composable
private fun MockTestInstructionsDialog(
    item: MockTestLandingItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = item.config
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.mock_instructions_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(config.titleRes),
                    style = MaterialTheme.typography.titleMedium
                )
                InstructionBullet(stringResource(R.string.mock_instructions_body))
                InstructionBullet(
                    stringResource(R.string.mock_rules_questions) + ": " +
                        stringResource(R.string.mock_rules_questions_value, config.questionCount) +
                        "   ·   " +
                        stringResource(R.string.mock_rules_duration) + ": " +
                        stringResource(R.string.mock_rules_duration_value, config.durationMinutes)
                )
                InstructionBullet(
                    stringResource(R.string.mock_rules_marks_correct) + " " +
                        stringResource(
                            R.string.mock_rules_marks_value_correct,
                            config.marksPerCorrect.formatMarks()
                        ) + ",  " +
                        stringResource(R.string.mock_rules_marks_incorrect) + " " +
                        stringResource(
                            R.string.mock_rules_marks_value_incorrect,
                            config.negativeMark.formatMarks()
                        ) + ",  " +
                        stringResource(R.string.mock_rules_marks_unanswered) + " " +
                        stringResource(R.string.mock_rules_marks_value_unanswered)
                )
                InstructionBullet(stringResource(R.string.mock_instructions_negative))
                InstructionBullet(stringResource(R.string.mock_instructions_palette))
                InstructionBullet(stringResource(R.string.mock_instructions_timeout))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.mock_instructions_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.mock_instructions_cancel))
            }
        }
    )
}

@Composable
private fun InstructionBullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "•", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.sizeIn(maxWidth = 420.dp)
        )
    }
}