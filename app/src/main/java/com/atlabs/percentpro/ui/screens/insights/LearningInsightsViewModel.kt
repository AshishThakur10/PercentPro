package com.atlabs.percentpro.ui.screens.insights

import com.atlabs.percentpro.content.LearningAnalytics
import com.atlabs.percentpro.content.InsightThresholds
import com.atlabs.percentpro.content.LearningAnalyticsEngine
import com.atlabs.percentpro.content.PracticePlanner
import com.atlabs.percentpro.content.RecommendationEngine
import com.atlabs.percentpro.content.TopicPerformance
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.data.AnswerLedgerRepository
import com.atlabs.percentpro.model.DifficultyFilter
import com.atlabs.percentpro.model.LearningRecommendation
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.PracticePlan
import com.atlabs.percentpro.model.QuestionDifficulty
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class InsightsUiState(
    val analytics: LearningAnalytics = LearningAnalytics(),
    val recentByTopic: List<TopicPerformance> = emptyList(),
    val recommendation: LearningRecommendation? = null,
    val questionCounts: Map<LessonId, Int> = emptyMap(),
    val isLoading: Boolean = true
) {
    val hasAnyActivity: Boolean get() = analytics.hasAnyActivity
}

/**
 * Derives the insights screen from saved records.
 *
 * The screen holds no analytics of its own: every figure comes from
 * [LearningAnalyticsEngine] over the answer ledger, and the suggestion comes from
 * [RecommendationEngine]. That keeps the dashboard and the Home card in agreement
 * because they read the same computation.
 */
class LearningInsightsViewModel(
    container: AppContainer,
    private val answerLedgerRepository: AnswerLedgerRepository,
    private val now: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val questionRepository = container.questionRepository
    private val lessons = container.lessonCatalog.lessons()
    private val questionCounts: Map<LessonId, Int> =
        questionRepository.availableTopics().associateWith { questionRepository.questionCountFor(it) }

    val uiState: StateFlow<InsightsUiState> = combine(
        answerLedgerRepository.events,
        container.progressRepository.progress
    ) { events, progress ->
        val analytics = LearningAnalyticsEngine.analyse(
            events = events,
            completedLessons = progress.completedLessonIds
        )
        InsightsUiState(
            analytics = analytics,
            recentByTopic = LearningAnalyticsEngine.recentByTopic(events),
            recommendation = RecommendationEngine.recommend(
                analytics = analytics,
                lessons = lessons,
                progress = progress,
                questionCounts = questionCounts
            ),
            questionCounts = questionCounts,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState()
    )

    /** Builds a weak-topic drill. */
    fun planForTopic(topic: LessonId, difficulty: DifficultyFilter = DifficultyFilter.ANY): PracticePlan =
        PracticePlanner.planTopic(topic, questionRepository::questionsFor, difficulty)

    /** Builds a mixed run over the topics the learner has already studied. */
    fun planMixedReview(difficulty: DifficultyFilter = DifficultyFilter.ANY): PracticePlan =
        PracticePlanner.planMixedReview(
            topics = uiState.value.analytics.studiedTopics.map { it.topic },
            questionsFor = questionRepository::questionsFor,
            difficulty = difficulty
        )

    /** Difficulty levels that actually have questions for a topic. */
    fun availableDifficulties(topic: LessonId): List<QuestionDifficulty> =
        questionRepository.questionsFor(topic)
            .filter { it.isValid }
            .map { it.difficulty }
            .distinct()
            .sortedBy { InsightDifficultyOrder.indexOf(it) }

    companion object {
        /** Beginner first, so the difficulty chips read in course order. */
        private val InsightDifficultyOrder = InsightThresholds.ASCENDING_DIFFICULTY

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { LearningInsightsViewModel(container, container.answerLedgerRepository) }
        }
    }
}