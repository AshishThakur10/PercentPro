package com.atlabs.percentpro.ui.screens.mocktest

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlabs.percentpro.R
import com.atlabs.percentpro.content.MockTestCodec
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.ui.screens.practice.PracticeOptionRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestExamRoute(
    container: AppContainer,
    testId: MockTestId,
    language: AppLanguage,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MockTestExamViewModel = viewModel(
        key = testId.value,
        factory = MockTestExamViewModel.factory(container, testId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPalette by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Recompute the clock whenever the screen returns to the foreground. The
    // remaining time always comes from the stored deadline, so this cannot drift.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.tick()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Read as a composable resource so the snackbar follows the selected
    // language instead of resolving against a stale configuration.
    val recoveredMessage = stringResource(R.string.mock_restore_recovered)
    LaunchedEffect(uiState.recoveredResult) {
        if (uiState.recoveredResult != null) {
            snackbarHostState.showSnackbar(message = recoveredMessage)
            viewModel.dismissRecoveryNotice()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Owned here rather than in the shell so the system back gesture reaches
    // the same confirmation as the close button. A live paper stops and asks;
    // the instructions page, a finished result and the review page leave
    // immediately, because there is nothing there to lose.
    BackHandler { viewModel.requestExit(onExit) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val title = uiState.session?.config?.titleRes?.let { stringResource(it) }.orEmpty()
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.requestExit(onExit) },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_exit_lesson)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPalette = true },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(R.string.mock_palette_toggle)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.phase) {
                MockTestPhase.INSTRUCTIONS -> InstructionsPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    onStart = viewModel::startTest,
                    restoreProblem = uiState.restoreProblem,
                    onDismissProblem = viewModel::dismissRestoreProblem
                )

                MockTestPhase.IN_PROGRESS -> ExamPaper(
                    uiState = uiState,
                    language = language,
                    onSelectOption = viewModel::selectOption,
                    onNext = viewModel::nextQuestion,
                    onPrevious = viewModel::previousQuestion,
                    onToggleReview = viewModel::toggleReviewMark,
                    onClearResponse = {
                        viewModel.clearResponse()
                    },
                    onSubmit = viewModel::requestSubmit,
                    modifier = Modifier.fillMaxSize()
                )

                MockTestPhase.RESULT -> MockTestResultScreen(
                    result = uiState.result,
                    reward = uiState.reward,
                    onReview = viewModel::startReview,
                    onRetry = viewModel::retry,
                    onExit = onExit,
                    modifier = Modifier.fillMaxSize()
                )

                MockTestPhase.REVIEW -> MockTestReviewScreen(
                    uiState = uiState,
                    language = language,
                    onBack = viewModel::backToResult,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showPalette && uiState.phase == MockTestPhase.IN_PROGRESS) {
        ModalBottomSheet(
            onDismissRequest = { showPalette = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                QuestionPalette(
                    states = uiState.paletteStates,
                    currentIndex = uiState.session?.currentIndex ?: 0,
                    onSelect = { index ->
                        viewModel.goToQuestion(index)
                        showPalette = false
                    }
                )
                TextButton(
                    onClick = { showPalette = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp)
                ) {
                    Text(stringResource(R.string.mock_palette_close))
                }
            }
        }
    }

    if (uiState.showSubmitDialog) {
        val session = uiState.session
        AlertDialog(
            onDismissRequest = viewModel::dismissSubmitDialog,
            title = { Text(stringResource(R.string.mock_submit_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.mock_submit_dialog_body,
                        session?.answeredCount ?: 0,
                        session?.questionCount ?: 0,
                        session?.markedForReviewCount ?: 0
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmSubmit) {
                    Text(stringResource(R.string.mock_submit_dialog_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSubmitDialog) {
                    Text(stringResource(R.string.mock_submit_dialog_back))
                }
            }
        )
    }

    if (uiState.showExitDialog) {
        val session = uiState.session
        AlertDialog(
            onDismissRequest = viewModel::dismissExitDialog,
            title = { Text(stringResource(R.string.mock_exit_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.mock_exit_dialog_body,
                        session?.answeredCount ?: 0,
                        session?.questionCount ?: 0
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmExit(onExit) }) {
                    Text(stringResource(R.string.mock_exit_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExitDialog) {
                    Text(stringResource(R.string.mock_exit_dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun InstructionsPlaceholder(
    onStart: () -> Unit,
    restoreProblem: com.atlabs.percentpro.content.MockTestCodec.RestoreFailure?,
    onDismissProblem: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (restoreProblem != null) {
            Text(
                text = stringResource(
                    when (restoreProblem) {
                        MockTestCodec.RestoreFailure.UNKNOWN_TEST -> R.string.mock_restore_unknown_test
                        MockTestCodec.RestoreFailure.MISSING_QUESTIONS ->
                            R.string.mock_restore_missing_questions

                        else -> R.string.mock_restore_corrupt
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onDismissProblem) {
                Text(stringResource(R.string.mock_restore_ok))
            }
        }
        Button(onClick = onStart, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.mock_start_test))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExamPaper(
    uiState: MockTestExamUiState,
    language: AppLanguage,
    onSelectOption: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleReview: () -> Unit,
    onClearResponse: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = uiState.session ?: return
    val question = uiState.currentQuestion ?: return
    val selected = uiState.selectedOptionFor(question.id)
    val marked = uiState.isMarkedForReview(question.id)

    Column(modifier = modifier.fillMaxSize()) {
        TimerBar(
            remainingMillis = uiState.remainingMillis,
            questionNumber = uiState.questionNumber,
            questionCount = uiState.questionCount,
            answeredCount = session.answeredCount,
            markedCount = session.markedForReviewCount
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "prompt") {
                Text(
                    text = question.prompt.forLanguage(language),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
            }

            items(
                count = question.options.size,
                key = { index -> "${question.id.value}_opt_$index" }
            ) { index ->
                PracticeOptionRow(
                    label = question.optionAt(index, language).orEmpty(),
                    optionLetter = ('A' + index).toString(),
                    selected = selected == index,
                    revealed = false,
                    isCorrect = false,
                    isWrongSelection = false,
                    onSelect = { onSelectOption(index) }
                )
            }

            item(key = "question_actions") {
                // FlowRow, not Row: the mark-for-review chip and the clear
                // button together exceed the width of a narrow phone once the
                // labels are translated, and a Row cannot wrap.
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = onToggleReview,
                        label = {
                            Text(
                                stringResource(
                                    if (marked) {
                                        R.string.mock_unmark_review
                                    } else {
                                        R.string.mock_mark_review
                                    }
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.padding(0.dp)
                            )
                        },
                        // AssistChip is 32dp tall by default, under the 48dp
                        // minimum tap target.
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (marked) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    )
                    OutlinedButton(
                        onClick = onClearResponse,
                        enabled = selected != null,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.mock_clear_response))
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = !session.isFirstQuestion,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.lesson_engine_previous))
            }
            Button(
                onClick = if (session.isLastQuestion) onSubmit else onNext,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    stringResource(
                        if (session.isLastQuestion) {
                            R.string.mock_submit_test
                        } else {
                            R.string.practice_next_question
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun TimerBar(
    remainingMillis: Long,
    questionNumber: Int,
    questionCount: Int,
    answeredCount: Int,
    markedCount: Int,
    modifier: Modifier = Modifier
) {
    val totalSeconds = (remainingMillis / 1000L).coerceAtLeast(0L)
    val minutes = (totalSeconds / 60L).toInt()
    val seconds = (totalSeconds % 60L).toInt()
    val urgent = remainingMillis in 1..59_999L

    val timerDescription = stringResource(R.string.cd_mock_timer, minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.mock_exam_counter, questionNumber, questionCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.mock_timer_value, minutes, seconds),
                style = MaterialTheme.typography.titleMedium,
                color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { contentDescription = timerDescription },
                textAlign = TextAlign.End
            )
        }
        Text(
            text = stringResource(R.string.mock_result_attempted) + ": $answeredCount" +
                "  ·  " + stringResource(R.string.mock_result_review) + ": $markedCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}