package com.atlabs.percentpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R
import com.atlabs.percentpro.model.LessonStatus
import com.atlabs.percentpro.ui.screens.home.HomeUiState

/**
 * The single primary action on home.
 *
 * It always describes the lesson that is genuinely open right now: it resumes the
 * lesson in progress, otherwise it starts the next available one, and it never
 * claims a lesson exists when the available set is exhausted.
 */
@Composable
fun PrimaryLessonAction(
    uiState: HomeUiState,
    onStart: () -> Unit,
    onOpenLearningPath: () -> Unit,
    modifier: Modifier = Modifier
) {
    val next = uiState.nextLesson

    Column(modifier = modifier.fillMaxWidth()) {
        if (next == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.cta_all_done_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.cta_all_done_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            TextButton(
                onClick = onOpenLearningPath,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(stringResource(R.string.learning_path_full_title))
            }
            return@Column
        }

        val nextTitle = stringResource(next.titleRes)
        val resuming = uiState.path.nodes
            .firstOrNull { it.lesson.id == next.id }
            ?.status == LessonStatus.IN_PROGRESS

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = if (resuming) {
                    stringResource(R.string.cta_continue_named_lesson, nextTitle)
                } else {
                    stringResource(R.string.cta_start_named_lesson, nextTitle)
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

