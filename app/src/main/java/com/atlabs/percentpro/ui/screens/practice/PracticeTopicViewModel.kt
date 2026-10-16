package com.atlabs.percentpro.ui.screens.practice

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlabs.percentpro.content.LessonCatalog
import com.atlabs.percentpro.content.ExamCatalog
import com.atlabs.percentpro.content.QuestionRepository
import com.atlabs.percentpro.content.PracticePlanner
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.data.PracticeHistoryRepository
import com.atlabs.percentpro.model.ExamId
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
    val lastResult: PracticeResult?,

    /**
     * How many of this topic's questions are tagged for each exam.
     *
     * Only exams with at least one question appear: an exam that cannot fill a
     * drill would only offer a dead end.
     */
    val examCounts: Map<ExamId, Int> = emptyMap()
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
    private val examCatalog: ExamCatalog,
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
                        lastResult = latestByTopic[lessonId],
                        examCounts = examCountsFor(lessonId)
                    )
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PracticeTopicsUiState()
        )

    /**
     * Exam tags present on this topic's questions.
     *
     * An exam with fewer questions than a drill needs is left out rather than
     * shown as a filter that cannot produce a run.
     */
    private fun examCountsFor(topic: LessonId): Map<ExamId, Int> {
        val counts = LinkedHashMap<ExamId, Int>()
        questionRepository.questionsFor(topic).forEach { question ->
            question.examIds.forEach { examId ->
                counts[examId] = (counts[examId] ?: 0) + 1
            }
        }
        val order = examCatalog.exams().map { it.id }
        return counts
            .filterValues { it >= MIN_QUESTIONS_PER_EXAM_DRILL }
            .entries
            .sortedBy { order.indexOf(it.key) }
            .associate { it.key to it.value }
    }

    /** Re-checked at the navigation boundary, not trusted from the tapped card. */
    fun canOpen(topic: LessonId): Boolean = questionRepository.hasQuestions(topic)

    /**
     * Whether an exam can still fill a drill for a topic.
     *
     * Re-checked at the navigation boundary for the same reason as [canOpen]:
     * the card is not trusted to know what the bank holds.
     */
    fun canOpenExam(topic: LessonId, examId: ExamId): Boolean =
        questionRepository.questionsFor(topic).count { it.isFrom(examId) } >=
            MIN_QUESTIONS_PER_EXAM_DRILL

    companion object {

        /** Shortest run an exam filter will offer, matching the paper length. */
        const val MIN_QUESTIONS_PER_EXAM_DRILL = PracticePlanner.DEFAULT_TARGETED_LENGTH

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                PracticeTopicViewModel(
                    questionRepository = container.questionRepository,
                    lessonCatalog = container.lessonCatalog,
                    examCatalog = container.examCatalog,
                    historyRepository = container.practiceHistoryRepository
                )
            }
        }
    }
}