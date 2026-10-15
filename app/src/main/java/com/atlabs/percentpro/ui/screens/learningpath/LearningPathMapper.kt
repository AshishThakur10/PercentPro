package com.atlabs.percentpro.ui.screens.learningpath

import com.atlabs.percentpro.content.CourseSection
import com.atlabs.percentpro.content.LearningProgress
import com.atlabs.percentpro.content.LessonAvailability
import com.atlabs.percentpro.model.Lesson
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LessonStatus

/**
 * Turns raw saved progress into the resolved path.
 *
 * Shared by the home screen and the learning path screen so the two can never
 * disagree about what is unlocked or completed.
 */
object LearningPathMapper {

    fun map(lessons: List<Lesson>, progress: LearningProgress): LearningPathUiState {
        val implemented = LessonAvailability.implementedLessons(lessons)
        val nodes = lessons.map { lesson -> mapNode(lessons, lesson, progress) }

        // Grouped by the lesson's own order rather than by an index, so adding a
        // lesson later cannot shift an existing one into the wrong section.
        val sections = CourseSection.all.map { section ->
            PathSection(
                section = section,
                nodes = nodes.filter { it.lesson.order in section.firstOrder..section.lastOrder }
            )
        }

        return LearningPathUiState(
            nodes = nodes,
            completedCount = implemented.count { progress.isCompleted(it.id) },
            implementedCount = implemented.size,
            sections = sections
        )
    }

    private fun mapNode(
        lessons: List<Lesson>,
        lesson: Lesson,
        progress: LearningProgress
    ): LessonNode {
        val status = LessonAvailability.statusFor(
            lesson = lesson,
            allLessons = lessons,
            completed = progress.completedLessonIds,
            inProgress = progress.lessonInProgress
        )
        return LessonNode(
            lesson = lesson,
            status = status,
            blockedByTitleRes = if (status == LessonStatus.LOCKED_SEQUENCE) {
                lessons
                    .lastOrNull { it.order < lesson.order }
                    ?.takeIf { !progress.isCompleted(it.id) }
                    ?.titleRes
            } else {
                null
            },
            isBlockedByUpcomingContent = status == LessonStatus.LOCKED_UPCOMING
        )
    }

    fun canOpen(
        lessons: List<Lesson>,
        pathState: LearningPathUiState,
        lessonId: LessonId
    ): Boolean {
        val lesson = lessons.firstOrNull { it.id == lessonId } ?: return false
        return LessonAvailability.canOpen(
            lesson = lesson,
            allLessons = lessons,
            completed = pathState.nodes
                .filter { it.status == LessonStatus.COMPLETED }
                .map { it.lesson.id }
                .toSet(),
            inProgress = pathState.nodes
                .firstOrNull { it.status == LessonStatus.IN_PROGRESS }
                ?.lesson
                ?.id
        )
    }
}