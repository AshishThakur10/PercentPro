package com.atlabs.percentpro.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlabs.percentpro.content.LearningProgress
import com.atlabs.percentpro.content.LessonCatalog
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.content.QuestionRepository
import com.atlabs.percentpro.data.AnswerLedgerRepository
import com.atlabs.percentpro.data.GamificationRepository
import com.atlabs.percentpro.data.ProgressRepository
import com.atlabs.percentpro.model.LearningRecommendation
import com.atlabs.percentpro.model.Lesson
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.content.LearningAnalyticsEngine
import com.atlabs.percentpro.content.RecommendationEngine
import com.atlabs.percentpro.ui.gamification.GamificationSnapshot
import com.atlabs.percentpro.ui.gamification.gamificationSnapshots
import com.atlabs.percentpro.ui.screens.learningpath.LearningPathMapper
import com.atlabs.percentpro.ui.screens.learningpath.LearningPathUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Home derives everything from the same path resolution the learning path screen
 * uses, so progress and unlock state can never drift between the two.
 */
data class HomeUiState(
    val path: LearningPathUiState = LearningPathUiState(),
    val hasStarted: Boolean = false,
    /** Persisted XP, streak and daily-goal state. Never a placeholder. */
    val gamification: GamificationSnapshot = GamificationSnapshot(),
    /** The single next thing to suggest, or null before records have loaded. */
    val recommendation: LearningRecommendation? = null
) {
    /** The lesson the primary action opens, or null when nothing is left to start. */
    val nextLesson: Lesson? get() = path.nextLesson

    val completionPercent: Int get() = path.completionPercent

    val completedCount: Int get() = path.completedCount

    val implementedCount: Int get() = path.implementedCount
}

class HomeViewModel(
    lessonCatalog: LessonCatalog,
    progressRepository: ProgressRepository,
    gamificationRepository: GamificationRepository,
    answerLedgerRepository: AnswerLedgerRepository,
    private val questionRepository: QuestionRepository,
    now: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val lessons = lessonCatalog.lessons()

    private val questionCounts: Map<LessonId, Int> = questionRepository
        .availableTopics()
        .associateWith { questionRepository.questionCountFor(it) }

    val uiState: StateFlow<HomeUiState> = combine(
        progressRepository.progress,
        gamificationSnapshots(gamificationRepository, now),
        // Only the raw events are needed here; the lessons-completed count comes
        // from progress, which is already part of this combine.
        answerLedgerRepository.events.map { events ->
            LearningAnalyticsEngine.analyse(events = events)
        }
    ) { progress, gamification, analytics ->
        HomeUiState(
            path = LearningPathMapper.map(lessons, progress),
            hasStarted = progress.hasStarted,
            gamification = gamification,
            recommendation = RecommendationEngine.recommend(
                analytics = analytics.copy(
                    completedLessonCount = progress.completedLessonIds.size
                ),
                lessons = lessons,
                progress = progress,
                questionCounts = questionCounts
            )
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(
                path = LearningPathMapper.map(lessons, LearningProgress()),
                hasStarted = false
            )
        )

    /** Re-checked at the navigation boundary, not trusted from the tapped card. */
    fun canOpen(lessonId: LessonId): Boolean =
        LearningPathMapper.canOpen(lessons, uiState.value.path, lessonId)

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                HomeViewModel(
                    lessonCatalog = container.lessonCatalog,
                    progressRepository = container.progressRepository,
                    gamificationRepository = container.gamificationRepository,
                    answerLedgerRepository = container.answerLedgerRepository,
                    questionRepository = container.questionRepository
                )
            }
        }
    }
}