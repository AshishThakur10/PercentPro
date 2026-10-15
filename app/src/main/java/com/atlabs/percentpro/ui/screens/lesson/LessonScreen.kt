package com.atlabs.percentpro.ui.screens.lesson

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlabs.percentpro.R
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.RewardOutcome
import com.atlabs.percentpro.ui.components.RewardEarnedPanel

@Composable
fun LessonRoute(
    container: AppContainer,
    lessonId: LessonId,
    hasNextLesson: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LessonViewModel = viewModel(
        key = lessonId.storageKey,
        factory = LessonViewModel.factory(container, lessonId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = !uiState.isBlocked, onBack = onExit)

    LessonScreen(
        uiState = uiState,
        hasNextLesson = hasNextLesson,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onRestart = { viewModel.onJumpTo(0) },
        onComplete = viewModel::onCompleteLesson,
        onExit = onExit,
        modifier = modifier
    )
}

@Composable
fun LessonScreen(
    uiState: LessonUiState,
    hasNextLesson: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onRestart: () -> Unit,
    onComplete: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { LessonTopBar(uiState = uiState, onExit = onExit) },
        bottomBar = {
            val showBottomBar = !uiState.isBlocked &&
                uiState.blocks.isNotEmpty() &&
                !uiState.isCompleted
            if (showBottomBar) {
                LessonBottomBar(
                    uiState = uiState,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onComplete = onComplete
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isBlocked -> BlockedLesson(onExit = onExit)

                uiState.lesson == null -> Unit

                uiState.isCompleted -> LessonFinishedBody(
                    reward = uiState.reward,
                    title = stringResource(uiState.lesson.titleRes),
                    hasNextLesson = hasNextLesson,
                    onReturnToPath = onExit
                )

                else -> LessonBody(
                    uiState = uiState,
                    onRestart = onRestart
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonTopBar(
    uiState: LessonUiState,
    onExit: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = uiState.lesson?.let { stringResource(it.titleRes) }.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
        },
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_exit_lesson)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun LessonBody(
    uiState: LessonUiState,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("lesson_blocks"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "step") {
            Text(
                text = stringResource(
                    R.string.lesson_engine_step,
                    uiState.blockNumber,
                    uiState.blockCount
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item(key = "block") {
            uiState.currentBlock?.let { block -> LessonBlockContent(block = block) }
        }
        if (uiState.currentBlockIndex > 0) {
            item(key = "restart") {
                TextButton(onClick = onRestart) {
                    Text(stringResource(R.string.lesson_engine_start_over))
                }
            }
        }
    }
}

@Composable
private fun LessonBottomBar(
    uiState: LessonUiState,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        LinearProgressIndicator(
            progress = { uiState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
            drawStopIndicator = {}
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!uiState.isFirstBlock) {
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
                onClick = if (uiState.isLastBlock) onComplete else onNext,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(
                        if (uiState.isLastBlock) {
                            R.string.lesson_engine_finish
                        } else {
                            R.string.lesson_engine_next
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun LessonFinishedBody(
    title: String,
    hasNextLesson: Boolean,
    onReturnToPath: () -> Unit,
    reward: RewardOutcome = RewardOutcome.NONE,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.lesson_complete_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = if (hasNextLesson) {
                        stringResource(R.string.lesson_complete_body, title)
                    } else {
                        stringResource(R.string.lesson_complete_body_no_next, title)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 10.dp)
                )
                RewardEarnedPanel(
                    outcome = reward,
                    modifier = Modifier.padding(top = 20.dp)
                )

                Button(
                    onClick = onReturnToPath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.lesson_engine_return_to_path),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedLesson(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = stringResource(R.string.lesson_locked_upcoming_reason),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .semantics { heading() }
            )
            Button(
                onClick = onExit,
                modifier = Modifier.padding(top = 24.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.lesson_engine_return_to_path))
            }
        }
    }
}