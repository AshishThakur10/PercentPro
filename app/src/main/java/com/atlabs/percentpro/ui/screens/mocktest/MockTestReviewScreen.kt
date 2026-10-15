package com.atlabs.percentpro.ui.screens.mocktest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.Question

/**
 * Read-only walkthrough of a finished paper.
 *
 * Works both for the session just submitted and for a result opened later from
 * the landing screen, so nothing here can modify a saved result.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestReviewScreen(
    uiState: MockTestExamUiState,
    language: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val questions = uiState.session?.questions.orEmpty()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.mock_review_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.mock_review_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (questions.isEmpty()) {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // itemsIndexed, not items plus indexOf: the scan ran per row, making
            // the review list quadratic in its own length.
            itemsIndexed(items = questions, key = { _, question -> question.id.value }) { index, question ->
                MockTestReviewCard(
                    number = index + 1,
                    question = question,
                    selectedIndex = uiState.selectedOptionFor(question.id),
                    markedForReview = uiState.isMarkedForReview(question.id),
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
                        text = stringResource(R.string.mock_review_back),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun MockTestReviewCard(
    number: Int,
    question: Question,
    selectedIndex: Int?,
    markedForReview: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isCorrect = selectedIndex != null && question.isCorrectAnswer(selectedIndex)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "$number. ${question.prompt.forLanguage(language)}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() }
            )
            if (markedForReview) {
                Text(
                    text = stringResource(R.string.mock_review_marked_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            question.options.forEachIndexed { index, option ->
                val isCorrectOption = index == question.correctIndex
                val isChosen = index == selectedIndex
                val textColor = when {
                    isCorrectOption -> MaterialTheme.colorScheme.secondary
                    isChosen -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${('A' + index)}  ${option.forLanguage(language)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (isCorrectOption || isChosen) {
                        Text(
                            text = if (isCorrectOption) {
                                stringResource(R.string.mock_status_correct)
                            } else {
                                stringResource(R.string.mock_status_your_answer)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor
                        )
                    }
                }
            }

            Text(
                text = if (selectedIndex == null) {
                    stringResource(R.string.mock_review_not_answered)
                } else {
                    stringResource(
                        R.string.mock_review_your_answer,
                        question.optionAt(selectedIndex, language).orEmpty()
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = stringResource(R.string.mock_review_explanation).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = question.explanation.forLanguage(language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (selectedIndex != null) {
                Text(
                    text = stringResource(
                        if (isCorrect) {
                            R.string.mock_result_correct
                        } else {
                            R.string.mock_result_incorrect
                        }
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isCorrect) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}