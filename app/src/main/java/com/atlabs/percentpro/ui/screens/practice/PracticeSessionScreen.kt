package com.atlabs.percentpro.ui.screens.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlabs.percentpro.R
import com.atlabs.percentpro.model.RewardOutcome
import com.atlabs.percentpro.ui.components.ProvenanceChips
import com.atlabs.percentpro.ui.components.RewardEarnedPanel
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.Provenance
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionId

@Composable
fun PracticeSessionRoute(
    container: AppContainer,
    topic: LessonId,
    language: AppLanguage,
    onExit: () -> Unit,
    /**
     * Optional targeted selection. When null this is ordinary topic practice over
     * every question for the topic; the same screen and engine run it either way.
     */
    plan: com.atlabs.percentpro.model.PracticePlan? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: PracticeSessionViewModel = viewModel(
        key = plan?.key ?: topic.storageKey,
        factory = PracticeSessionViewModel.factory(container, topic, plan)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Title comes from the same catalog entry the topic list used, so the two can
    // never show different names for the same topic.
    // remembered: lessons() materialises the whole catalog, and this was being
    // rebuilt and rescanned on every recomposition of the session.
    val lesson = remember(topic, container) {
        container.lessonCatalog.lessons().firstOrNull { it.id == topic }
    }
    val topicTitle = lesson?.let { stringResource(it.titleRes) }.orEmpty()

    PracticeSessionScreen(
        uiState = uiState,
        language = language,
        topicTitle = topicTitle,
        isAnswered = viewModel::isAnswered,
        submittedIndexFor = viewModel::submittedIndexFor,
        shouldConfirmExit = viewModel::shouldConfirmExit,
        onSubmit = viewModel::submit,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onFinish = viewModel::finish,
        onStartReview = viewModel::startReview,
        onBackToResults = viewModel::backToResults,
        onRestart = viewModel::restart,
        onExit = onExit,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSessionScreen(
    uiState: PracticeSessionUiState,
    language: AppLanguage,
    topicTitle: String,
    isAnswered: (QuestionId) -> Boolean,
    submittedIndexFor: (QuestionId) -> Int?,
    shouldConfirmExit: () -> Boolean,
    onSubmit: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
    onStartReview: () -> Unit,
    onBackToResults: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topicTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (shouldConfirmExit()) showExitDialog = true else onExit()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_exit_lesson)
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
                PracticePhase.ANSWERING -> AnsweringBody(
                    uiState = uiState,
                    language = language,
                    isAnswered = isAnswered,
                    submittedIndexFor = submittedIndexFor,
                    onSubmit = onSubmit,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onFinish = onFinish
                )

                PracticePhase.RESULTS -> PracticeResultsBody(
                    topicTitle = topicTitle,
                    correct = uiState.session.correctCount,
                    incorrect = uiState.session.incorrectCount,
                    unanswered = uiState.session.unansweredCount,
                    total = uiState.session.questionCount,
                    accuracyPercent = uiState.session.accuracyPercent,
                    reward = RewardOutcome(
                        xpAwarded = uiState.xpEarnedThisSession,
                        unlockedAchievements = uiState.unlockedAchievements
                    ),
                    onReview = onStartReview,
                    onRestart = onRestart,
                    onExit = onExit
                )

                PracticePhase.REVIEW -> PracticeReviewBody(
                    language = language,
                    questions = uiState.session.questions,
                    isAnswered = isAnswered,
                    submittedIndexFor = submittedIndexFor,
                    onBack = onBackToResults
                )
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.practice_exit_title)) },
            text = {
                Text(
                    if (uiState.session.unansweredCount > 0) {
                        pluralStringResource(
                            R.plurals.practice_exit_message,
                            uiState.session.unansweredCount,
                            uiState.session.unansweredCount
                        )
                    } else {
                        stringResource(R.string.practice_exit_message_clean)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onExit()
                }) {
                    Text(stringResource(R.string.practice_exit_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.practice_exit_cancel))
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Answering
// ---------------------------------------------------------------------------

@Composable
private fun AnsweringBody(
    uiState: PracticeSessionUiState,
    language: AppLanguage,
    isAnswered: (QuestionId) -> Boolean,
    submittedIndexFor: (QuestionId) -> Int?,
    onSubmit: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val question = uiState.currentQuestion

    if (!uiState.hasQuestions || question == null) {
        Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.practice_message_empty),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // Keyed on the question id, so moving to a new question always starts
    // unselected and coming back restores what was already submitted.
    var pendingSelection by rememberSaveable(question.id.value) { mutableIntStateOf(-1) }
    val answered = isAnswered(question.id)
    val submitted = submittedIndexFor(question.id)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "counter") {
            Text(
                text = stringResource(
                    R.string.practice_question_counter,
                    uiState.questionNumber,
                    uiState.questionCount
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item(key = "progress") {
            LinearProgressIndicator(
                progress = { uiState.session.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                drawStopIndicator = {}
            )
        }

        item(key = "prompt") {
            Text(
                text = question.prompt.forLanguage(language),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
        }

        // Which exams this question is written for, shown above the options so a
        // learner can see why it looks the way it does before answering. Renders
        // nothing for an untagged question.
        item(key = "provenance") {
            ProvenanceChips(
                question = question,
                language = language,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(
            count = question.options.size,
            key = { index -> "${question.id.value}_option_$index" }
        ) { index ->
            PracticeOptionRow(
                label = question.optionAt(index, language).orEmpty(),
                optionLetter = ('A' + index).toString(),
                selected = !answered && pendingSelection == index,
                revealed = answered,
                isCorrect = answered && question.isCorrectAnswer(index),
                isWrongSelection = answered && submitted == index && !question.isCorrectAnswer(index),
                onSelect = { pendingSelection = index }
            )
        }

        if (answered) {
            item(key = "verdict") {
                AnswerVerdict(
                    isCorrect = submitted == question.correctIndex,
                    selectedText = question.optionAt(submitted ?: -1, language).orEmpty(),
                    correctText = question.correctOption(language),
                    explanation = question.explanation.forLanguage(language)
                )
            }
        } else if (uiState.session.unansweredCount > 0) {
            item(key = "unanswered_banner") {
                Text(
                    text = pluralStringResource(
                        R.plurals.practice_unanswered_banner,
                        uiState.session.unansweredCount,
                        uiState.session.unansweredCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "actions") {
            PracticeAnswerActions(
                answered = answered,
                selectionMade = pendingSelection >= 0,
                canFinish = uiState.canFinish,
                isFirst = uiState.session.isFirstQuestion,
                isLast = uiState.session.isLastQuestion,
                onSubmit = {
                    if (pendingSelection >= 0) onSubmit(pendingSelection)
                },
                onNext = onNext,
                onPrevious = onPrevious,
                onFinish = onFinish
            )
        }
    }
}

@Composable
private fun AnswerVerdict(
    isCorrect: Boolean,
    selectedText: String,
    correctText: String,
    explanation: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isCorrect) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (isCorrect) {
                            R.string.practice_correct_answer
                        } else {
                            R.string.practice_incorrect_answer
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCorrect) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.weight(1f)
                )
                if (isCorrect) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (!isCorrect) {
                Text(
                    text = stringResource(R.string.practice_your_answer, selectedText),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                text = stringResource(R.string.practice_correct_answer_label, correctText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = stringResource(R.string.practice_explanation_label).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 14.dp)
            )
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun PracticeAnswerActions(
    answered: Boolean,
    selectionMade: Boolean,
    canFinish: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!answered) {
            Button(
                onClick = onSubmit,
                enabled = selectionMade,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.practice_check_answer),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isFirst) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = stringResource(R.string.lesson_engine_previous),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Button(
                onClick = if (isLast) onFinish else onNext,
                // Finishing is only offered once every question has an answer.
                enabled = if (isLast) canFinish else true,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(
                        if (isLast) R.string.practice_finish else R.string.practice_next_question
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Results
// ---------------------------------------------------------------------------

@Composable
private fun PracticeResultsBody(
    topicTitle: String,
    correct: Int,
    incorrect: Int,
    unanswered: Int,
    total: Int,
    accuracyPercent: Int,
    onReview: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    reward: RewardOutcome = RewardOutcome.NONE,
    modifier: Modifier = Modifier
) {
    val scoreDescription = stringResource(R.string.cd_practice_score, accuracyPercent, correct, total)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "title") {
            Text(
                text = stringResource(R.string.practice_results_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }
            )
        }

        item(key = "topic") {
            Text(
                text = topicTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.practice_results_score),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = stringResource(R.string.practice_results_score_value, accuracyPercent),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .semantics { contentDescription = scoreDescription }
                    )
                    Text(
                        text = encouragement(accuracyPercent, total),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

        item(key = "stats") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ResultStatTile(
                    value = total.toString(),
                    label = stringResource(R.string.practice_results_total),
                    modifier = Modifier.weight(1f)
                )
                ResultStatTile(
                    value = correct.toString(),
                    label = stringResource(R.string.practice_results_correct),
                    modifier = Modifier.weight(1f)
                )
                ResultStatTile(
                    value = incorrect.toString(),
                    label = stringResource(R.string.practice_results_incorrect),
                    modifier = Modifier.weight(1f)
                )
                ResultStatTile(
                    value = unanswered.toString(),
                    label = stringResource(R.string.practice_results_unanswered),
                    modifier = Modifier.weight(1f)
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
                        text = stringResource(R.string.practice_results_review),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = stringResource(R.string.practice_results_again),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                TextButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.practice_results_return))
                }
            }
        }
    }
}

@Composable
private fun encouragement(accuracyPercent: Int, total: Int): String {
    if (total <= 0) return stringResource(R.string.practice_message_empty)
    val res = when {
        accuracyPercent >= 90 -> R.string.practice_message_perfect
        accuracyPercent >= 60 -> R.string.practice_message_good
        accuracyPercent >= 30 -> R.string.practice_message_fair
        else -> R.string.practice_message_weak
    }
    return stringResource(res)
}

// ---------------------------------------------------------------------------
// Review
// ---------------------------------------------------------------------------

@Composable
private fun PracticeReviewBody(
    language: AppLanguage,
    questions: List<Question>,
    isAnswered: (QuestionId) -> Boolean,
    submittedIndexFor: (QuestionId) -> Int?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "title") {
            Text(
                text = stringResource(R.string.practice_review_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
        }
        itemsIndexed(
            items = questions,
            key = { _, question -> question.id.value }
        ) { index, question ->
            // itemsIndexed, not items plus indexOf: the scan was inside the item
            // lambda, making the whole review list quadratic in its own length.
            ReviewQuestionCard(
                number = index + 1,
                prompt = question.prompt.forLanguage(language),
                options = question.options.map { it.forLanguage(language) },
                correctIndex = question.correctIndex,
                submittedIndex = submittedIndexFor(question.id),
                answered = isAnswered(question.id),
                explanation = question.explanation.forLanguage(language),
                provenance = question.provenance,
                language = language
            )
        }
        item(key = "back") {
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.practice_review_back),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun ReviewQuestionCard(
    number: Int,
    prompt: String,
    options: List<String>,
    correctIndex: Int,
    submittedIndex: Int?,
    answered: Boolean,
    explanation: String,
    provenance: List<Provenance> = emptyList(),
    language: AppLanguage = AppLanguage.ENGLISH,
    modifier: Modifier = Modifier
) {
    val correctLabel = stringResource(R.string.cd_option_correct)
    val chosenLabel = stringResource(R.string.cd_option_selected)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "$number. $prompt",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() }
            )
            ProvenanceChips(
                provenance = provenance,
                language = language,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            options.forEachIndexed { index, option ->
                val isCorrect = index == correctIndex
                val isChosen = answered && index == submittedIndex
                // The option letter is the thing being read aloud, so it is
                // merged into one node carrying the whole answer rather than
                // being announced as an unexplained single character.
                Text(
                    text = "${('A' + index)}  $option",
                    style = MaterialTheme.typography.bodyMedium,
                    // primary rather than secondary: MainPurple on the app
                    // surface measures 3.8:1, under the 4.5:1 body-text floor.
                    color = when {
                        isCorrect -> MaterialTheme.colorScheme.primary
                        isChosen -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = reviewOptionDescription(
                                option = option,
                                isCorrect = isCorrect,
                                isChosen = isChosen,
                                answered = answered,
                                correctLabel = correctLabel,
                                chosenLabel = chosenLabel
                            )
                        }
                )
            }

            // A written status per option, not colour alone. The tint above is a
            // second channel, not the only one: this is what a reader in greyscale,
            // with colour blindness, or on a screen where the tint does not read,
            // actually relies on.
            if (answered) {
                val chosenLabel = options.getOrNull(submittedIndex ?: -1)
                if (chosenLabel != null && submittedIndex != correctIndex) {
                    Text(
                        text = stringResource(R.string.practice_your_answer, chosenLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.practice_correct_answer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.practice_unanswered_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

/**
 * Spells out one review option for a screen reader.
 *
 * The status words are passed in already resolved from string resources by the
 * caller, so a Hindi review is announced in Hindi rather than in English. The
 * option text itself is already localized by the time it reaches here.
 */
private fun reviewOptionDescription(
    option: String,
    isCorrect: Boolean,
    isChosen: Boolean,
    answered: Boolean,
    correctLabel: String,
    chosenLabel: String
): String = buildString {
    append(option)
    if (answered && isChosen) {
        append(". ").append(chosenLabel)
    }
    if (isCorrect) {
        append(". ").append(correctLabel)
    }
}

// ---------------------------------------------------------------------------
// Shared option row
// ---------------------------------------------------------------------------

@Composable
internal fun PracticeOptionRow(
    label: String,
    optionLetter: String,
    selected: Boolean,
    revealed: Boolean,
    isCorrect: Boolean,
    isWrongSelection: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highlighted = selected || (revealed && (isCorrect || isWrongSelection))
    val borderColor = when {
        revealed && isCorrect -> MaterialTheme.colorScheme.secondary
        revealed && isWrongSelection -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    // The wrong answer is named as explicitly as the right one. Previously a
    // wrong pick fell through to no label at all and was signalled only by a red
    // border, which says nothing to a screen reader and little to a colourblind
    // reader in a way the border's shape does not correct.
    val statusDescription = when {
        revealed && isCorrect -> stringResource(R.string.cd_option_correct)
        revealed && isWrongSelection -> stringResource(R.string.cd_option_incorrect)
        selected -> stringResource(R.string.cd_option_selected)
        else -> null
    }

    // The letter badge needs its own background pair rather than reusing
    // borderColor: surface-coloured text on the outline grey measured 1.6:1,
    // which is unreadable. primary/onPrimary measures 10.3:1 in light and 11.5:1
    // in dark, and surfaceVariant/onSurface measures 15.4:1 and 12.3:1.
    val badgeContainer = when {
        highlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val badgeContent = when {
        highlighted -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = !revealed,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .background(color = containerColor, shape = MaterialTheme.shapes.medium)
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(optionLetter)
                    append(". ")
                    append(label)
                    statusDescription?.let {
                        append(". ")
                        append(it)
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 28.dp, minHeight = 28.dp)
                .background(color = badgeContainer, shape = MaterialTheme.shapes.extraSmall)
                // Decorative: the merged row above already announces the letter
                // as part of the option, so reading it again would duplicate it.
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = optionLetter,
                style = MaterialTheme.typography.labelLarge,
                color = badgeContent
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        )
        if (revealed && isCorrect) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}