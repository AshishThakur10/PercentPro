package com.atlabs.percentpro.ui.screens.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlabs.percentpro.content.LessonAvailability
import com.atlabs.percentpro.content.LessonBlock
import com.atlabs.percentpro.content.LessonCatalog
import com.atlabs.percentpro.content.LessonContentRepository
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.content.LearningActivity
import com.atlabs.percentpro.data.GamificationRepository
import com.atlabs.percentpro.data.ProgressRepository
import com.atlabs.percentpro.model.LocalDates
import com.atlabs.percentpro.model.RewardOutcome
import com.atlabs.percentpro.model.Lesson
import com.atlabs.percentpro.model.LessonId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LessonUiState(
    val lesson: Lesson? = null,
    val blocks: List<LessonBlock> = emptyList(),
    val currentBlockIndex: Int = 0,
    val isCompleted: Boolean = false,
    /** Set when the screen was opened for a lesson the learner may not enter. */
    val isBlocked: Boolean = false,
    /**
     * XP and achievements earned by finishing this lesson.
     *
     * [RewardOutcome.NONE] on a repeat, because the ledger already holds the
     * lesson, so the finished screen shows a reward only the first time.
     */
    val reward: RewardOutcome = RewardOutcome.NONE
) {
    val currentBlock: LessonBlock?
        get() = blocks.getOrNull(currentBlockIndex)

    val isFirstBlock: Boolean get() = currentBlockIndex <= 0

    val isLastBlock: Boolean get() = blocks.isNotEmpty() && currentBlockIndex == blocks.lastIndex

    val blockNumber: Int get() = currentBlockIndex + 1

    val blockCount: Int get() = blocks.size

    /** 0f at the start of the lesson, 1f once the final block is showing. */
    val progress: Float
        get() = if (blocks.isEmpty()) 0f else blockNumber.toFloat() / blocks.size
}

sealed interface LessonEffect {
    data object LessonFinished : LessonEffect
}

/**
 * Drives one lesson run.
 *
 * The screen renders whatever block list this hands it, so the engine works the
 * same for every lesson. Completion is only written when the learner confirms on
 * the final block, never by reaching it.
 */
class LessonViewModel(
    private val lessonCatalog: LessonCatalog,
    private val lessonContent: LessonContentRepository,
    private val progressRepository: ProgressRepository,
    private val gamificationRepository: GamificationRepository,
    private val lessonId: LessonId,
    private val now: () -> Long = System::currentTimeMillis,
    private val timeZone: () -> java.util.TimeZone = java.util.TimeZone::getDefault
) : ViewModel() {

    private val lessons = lessonCatalog.lessons()

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    private val _effects = MutableStateFlow<LessonEffect?>(null)
    val effects: StateFlow<LessonEffect?> = _effects.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val progress = progressRepository.current()
        val lesson = lessons.firstOrNull { it.id == lessonId }
        val blocks = lessonContent.blocksFor(lessonId)

        if (lesson == null || blocks.isEmpty()) {
            _uiState.value = LessonUiState(isBlocked = true)
            return
        }

        // Defensive: even if something navigated here directly, a locked lesson
        // never opens.
        val canOpen = LessonAvailability.canOpen(
            lesson = lesson,
            allLessons = lessons,
            completed = progress.completedLessonIds,
            inProgress = progress.lessonInProgress
        )
        if (!canOpen) {
            _uiState.value = LessonUiState(isBlocked = true)
            return
        }

        // Resume only when this lesson is the one left open; otherwise start over.
        val resumeIndex = if (progress.lessonInProgress == lessonId) {
            progress.lastBlockIndex
        } else {
            0
        }

        _uiState.value = LessonUiState(
            lesson = lesson,
            blocks = blocks,
            currentBlockIndex = resumeIndex.coerceIn(0, blocks.lastIndex),
            isCompleted = progress.isCompleted(lessonId)
        )

        if (progress.lessonInProgress != lessonId) {
            progressRepository.saveLessonPosition(lessonId, resumeIndex)
        }
    }

    fun onNext() {
        val state = _uiState.value
        if (state.isLastBlock) return
        val nextIndex = state.currentBlockIndex + 1
        _uiState.update { it.copy(currentBlockIndex = nextIndex) }
        persistPosition(nextIndex)
    }

    fun onPrevious() {
        val state = _uiState.value
        if (state.isFirstBlock) return
        val previousIndex = state.currentBlockIndex - 1
        _uiState.update { it.copy(currentBlockIndex = previousIndex) }
        persistPosition(previousIndex)
    }

    fun onJumpTo(index: Int) {
        val state = _uiState.value
        if (index !in state.blocks.indices) return
        _uiState.update { it.copy(currentBlockIndex = index) }
        persistPosition(index)
    }

    fun onCompleteLesson() {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        // Guard: a lesson is only completable from its final block.
        if (!state.isLastBlock) return
        if (state.isCompleted) return

        _uiState.update { it.copy(isCompleted = true) }
        viewModelScope.launch {
            progressRepository.markLessonCompleted(lesson.id)
            // Reported from here rather than from the screen, so the reward is
            // tied to the one code path that completes a lesson. The ledger makes
            // it a no-op if a repeat ever reaches this point.
            val outcome = gamificationRepository.record(
                LearningActivity.LessonCompleted(
                    lessonId = lesson.id.storageKey,
                    occurredOn = LocalDates.fromEpochMillis(now(), timeZone())
                )
            )
            if (!outcome.isEmpty) _uiState.update { it.copy(reward = outcome) }
            _effects.value = LessonEffect.LessonFinished
        }
    }

    fun consumeEffect() {
        _effects.value = null
    }

    private fun persistPosition(index: Int) {
        viewModelScope.launch {
            progressRepository.saveLessonPosition(lessonId, index)
        }
    }

    companion object {
        fun factory(container: AppContainer, lessonId: LessonId) = viewModelFactory {
            initializer {
                LessonViewModel(
                    lessonCatalog = container.lessonCatalog,
                    lessonContent = container.lessonContent,
                    progressRepository = container.progressRepository,
                    gamificationRepository = container.gamificationRepository,
                    lessonId = lessonId
                )
            }
        }
    }
}