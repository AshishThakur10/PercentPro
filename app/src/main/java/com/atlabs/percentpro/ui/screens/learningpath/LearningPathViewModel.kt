package com.atlabs.percentpro.ui.screens.learningpath

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlabs.percentpro.content.CourseSection
import com.atlabs.percentpro.content.LearningProgress
import com.atlabs.percentpro.content.LessonCatalog
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.data.ProgressRepository
import com.atlabs.percentpro.model.Lesson
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LessonStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One row of the learning path, with the status already resolved. */
data class LessonNode(
    val lesson: Lesson,
    val status: LessonStatus,
    /** For a sequence lock, the title of the lesson that must be finished first. */
    val blockedByTitleRes: Int? = null,
    val isBlockedByUpcomingContent: Boolean = false
) {
    val isOpenable: Boolean get() = status.isUnlocked
}

/** One named band of the course, with its resolved lesson rows. */
data class PathSection(
    val section: CourseSection,
    val nodes: List<LessonNode>
) {
    val completedCount: Int get() = nodes.count { it.status == LessonStatus.COMPLETED }
    val isFullyComplete: Boolean get() = nodes.isNotEmpty() && completedCount == nodes.size
}

data class LearningPathUiState(
    val nodes: List<LessonNode> = emptyList(),
    val completedCount: Int = 0,
    val implementedCount: Int = 0,
    /** The same nodes grouped into the course's named sections, in order. */
    val sections: List<PathSection> = emptyList()
) {
    val completionPercent: Int
        get() = if (implementedCount == 0) 0
        else (completedCount * 100f / implementedCount).toInt()

    val nextLesson: Lesson?
        get() = nodes.firstOrNull { it.status == LessonStatus.IN_PROGRESS }?.lesson
            ?: nodes.firstOrNull { it.status == LessonStatus.AVAILABLE }?.lesson

    fun nodeFor(lessonId: LessonId): LessonNode? = nodes.firstOrNull { it.lesson.id == lessonId }
}

class LearningPathViewModel(
    lessonCatalog: LessonCatalog,
    progressRepository: ProgressRepository
) : ViewModel() {

    private val lessons = lessonCatalog.lessons()

    val uiState: StateFlow<LearningPathUiState> = progressRepository.progress
        .map { progress -> LearningPathMapper.map(lessons, progress) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LearningPathMapper.map(lessons, LearningProgress())
        )

    fun canOpen(lessonId: LessonId): Boolean =
        LearningPathMapper.canOpen(lessons, uiState.value, lessonId)

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                LearningPathViewModel(container.lessonCatalog, container.progressRepository)
            }
        }
    }
}