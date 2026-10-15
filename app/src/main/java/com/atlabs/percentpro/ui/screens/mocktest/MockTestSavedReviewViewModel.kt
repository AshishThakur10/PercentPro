package com.atlabs.percentpro.ui.screens.mocktest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlabs.percentpro.R
import com.atlabs.percentpro.content.MockTestSession
import com.atlabs.percentpro.content.QuestionRepository
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.data.MockTestRepository
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.MockTestResult
import com.atlabs.percentpro.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MockTestSavedReviewUiState(
    val isLoading: Boolean = true,
    val result: MockTestResult? = null,
    /** Rebuilt paper, held read-only: no mutation path is exposed. */
    val session: MockTestSession? = null
) {
    val isMissing: Boolean get() = !isLoading && result == null

    fun selectedOptionFor(questionId: com.atlabs.percentpro.model.QuestionId): Int? =
        session?.selectedOptionFor(questionId)

    fun isMarkedForReview(questionId: com.atlabs.percentpro.model.QuestionId): Boolean =
        session?.isMarked(questionId) ?: false
}

/**
 * Re-opens a stored result.
 *
 * The paper is rebuilt from the saved records and immediately marked submitted,
 * so every mutation on [MockTestSession] is a no-op. Nothing here writes to the
 * repository.
 */
class MockTestSavedReviewViewModel(
    private val questionRepository: QuestionRepository,
    private val mockTestRepository: MockTestRepository,
    private val sessionId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MockTestSavedReviewUiState())
    val uiState: StateFlow<MockTestSavedReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val result = mockTestRepository.resultFor(sessionId)
            if (result == null) {
                _uiState.value = MockTestSavedReviewUiState(isLoading = false, result = null)
                return@launch
            }

            val questions = result.records
                .sortedBy { it.order }
                .mapNotNull { record -> questionRepository.findAcrossTopics(record.questionId) }

            // A paper that can no longer be rebuilt in full is shown as missing
            // rather than scored against a partial question set.
            val session = if (questions.size == result.records.size) {
                MockTestSession(
                    sessionId = result.sessionId,
                    config = MockTestConfig(
                        id = result.testId,
                        titleRes = result.testTitleRes,
                        descriptionRes = R.string.mock_landing_description,
                        questionCount = result.totalQuestions,
                        durationMillis = 0L,
                        marksPerCorrectInQuarters = result.marksPerCorrectInQuarters,
                        negativeMarkInQuarters = result.negativeMarkInQuarters
                    ),
                    questions = questions,
                    startedAtEpochMillis = result.startedAtEpochMillis,
                    deadlineEpochMillis = result.startedAtEpochMillis,
                    currentIndex = 0,
                    answers = result.records.mapNotNull { record ->
                        record.selectedOptionIndex?.let { record.questionId to it }
                    }.toMap(),
                    markedForReview = result.records
                        .filter { it.markedForReview }
                        .map { it.questionId }
                        .toSet(),
                    visitedQuestionIds = result.records.map { it.questionId }.toSet(),
                    submission = result.submission
                )
            } else {
                null
            }

            _uiState.value = MockTestSavedReviewUiState(
                isLoading = false,
                result = result,
                session = session
            )
        }
    }

    /** Questions in the order the learner actually saw them. */
    val questions: List<Question>
        get() = uiState.value.session?.questions.orEmpty()

    companion object {
        fun factory(container: AppContainer, sessionId: String) = viewModelFactory {
            initializer {
                MockTestSavedReviewViewModel(
                    questionRepository = container.questionRepository,
                    mockTestRepository = container.mockTestRepository,
                    sessionId = sessionId
                )
            }
        }
    }
}

@Composable
fun MockTestSavedReviewRoute(
    container: AppContainer,
    sessionId: String,
    language: AppLanguage,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MockTestSavedReviewViewModel = viewModel(
        key = sessionId,
        factory = MockTestSavedReviewViewModel.factory(container, sessionId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MockTestReviewScreen(
        uiState = MockTestExamUiState(
            phase = MockTestPhase.REVIEW,
            session = uiState.session,
            result = uiState.result
        ),
        language = language,
        onBack = onExit,
        modifier = modifier
    )
}