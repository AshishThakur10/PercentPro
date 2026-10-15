package com.atlabs.percentpro.ui.screens.practice

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlabs.percentpro.content.LessonCatalog
import com.atlabs.percentpro.content.QuestionRepository
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.data.PracticeHistoryRepository
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.PracticeResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PracticeTopic(
    val lessonId: LessonId,
    @StringRes val titleRes: Int,
    val questionCount: Int,
    /** Most recent finished session for this topic, if there is one. */
    val lastResult: PracticeResult?
)

data class PracticeTopicsUiState(
    val topics: List<PracticeTopic> = emptyList()
) {
    val hasAnyTopics: Boolean get() = topics.isNotEmpty()
}

/**
 * Lists the topics that actually have questions.
 *
 * Topics with no questions are left out entirely rather than shown disabled, so
 * the screen can never offer something with no content behind it.
 */
class PracticeTopicViewModel(
    private val questionRepository: QuestionRepository,
    lessonCatalog: LessonCatalog,
    historyRepository: PracticeHistoryRepository
) : ViewModel() {

    private val titles: Map<LessonId, Int> =
        lessonCatalog.lessons().associate { it.id to it.titleRes }

    val uiState: StateFlow<PracticeTopicsUiState> = historyRepository.latestByTopic
        .map { latestByTopic ->
            PracticeTopicsUiState(
                topics = questionRepository.availableTopics().mapNotNull { lessonId ->
                    val titleRes = titles[lessonId] ?: return@mapNotNull null
                    PracticeTopic(
                        lessonId = lessonId,
                        titleRes = titleRes,
                        questionCount = questionRepository.questionCountFor(lessonId),
                        lastResult = latestByTopic[lessonId]
                    )
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PracticeTopicsUiState()
        )

    /** Re-checked at the navigation boundary, not trusted from the tapped card. */
    fun canOpen(topic: LessonId): Boolean = questionRepository.hasQuestions(topic)

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                PracticeTopicViewModel(
                    questionRepository = container.questionRepository,
                    lessonCatalog = container.lessonCatalog,
                    historyRepository = container.practiceHistoryRepository
                )
            }
        }
    }
}