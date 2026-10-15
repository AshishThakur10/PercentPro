package com.atlabs.percentpro.ui.screens.mocktest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlabs.percentpro.content.MockTestCatalog
import com.atlabs.percentpro.content.MockTestSelector
import com.atlabs.percentpro.content.QuestionRepository
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.data.MockTestRepository
import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.MockTestResult
import com.atlabs.percentpro.model.Question
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MockTestLandingItem(
    val config: MockTestConfig,
    /** How many questions the bank can actually supply for this test. */
    val availableQuestions: Int,
    val isAvailable: Boolean
)

/**
 * The mock-test list plus recently saved results.
 *
 * There is deliberately no "the current test" accessor any more: Phase 8
 * removed it because reading a single default test off this state was how the
 * start button came to run the first paper in the catalogue rather than the one
 * that was tapped. Callers now name the test they mean.
 */
data class MockTestLandingUiState(
    val tests: List<MockTestLandingItem> = emptyList(),
    val recentResults: List<MockTestResult> = emptyList()
)

class MockTestLandingViewModel(
    private val mockTestCatalog: MockTestCatalog,
    private val questionRepository: QuestionRepository,
    mockTestRepository: MockTestRepository
) : ViewModel() {

    val uiState: StateFlow<MockTestLandingUiState> = mockTestRepository.results
        .map { results ->
            MockTestLandingUiState(
                tests = mockTestCatalog.tests().map { config ->
                    val available = selectQuestions(config).size
                    MockTestLandingItem(
                        config = config,
                        availableQuestions = available,
                        isAvailable = available >= config.questionCount
                    )
                },
                recentResults = results.take(MAX_RECENT_RESULTS)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MockTestLandingUiState()
        )

    /** Deterministic: the same config always yields the same paper. */
    fun selectQuestions(config: MockTestConfig): List<Question> = MockTestSelector.select(
        config = config,
        topics = questionRepository.availableTopics(),
        questionsByTopic = questionRepository::questionsFor
    )

    companion object {
        private const val MAX_RECENT_RESULTS = 5

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                MockTestLandingViewModel(
                    mockTestCatalog = container.mockTestCatalog,
                    questionRepository = container.questionRepository,
                    mockTestRepository = container.mockTestRepository
                )
            }
        }
    }
}