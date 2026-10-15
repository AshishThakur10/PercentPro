package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.Lesson
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LessonStatus

/**
 * Single place that decides what a learner may open. Keeping the rules here
 * rather than in the UI means the home screen, the path screen and the lesson
 * screen cannot disagree about what is locked.
 */
object LessonAvailability {

    /**
     * @param completed lessons the learner has finished, by stable id.
     * @param inProgress lesson the learner has opened but not finished, if any.
     */
    fun statusFor(
        lesson: Lesson,
        allLessons: List<Lesson>,
        completed: Set<LessonId>,
        inProgress: LessonId?
    ): LessonStatus {
        // Checked before the completed set: if saved data claims an unwritten
        // lesson is done, the app must still not present it as available content.
        if (!lesson.isPlayable) return LessonStatus.LOCKED_UPCOMING

        if (lesson.id in completed) return LessonStatus.COMPLETED

        if (lesson.id == inProgress) return LessonStatus.IN_PROGRESS

        val previous = allLessons.lastOrNull { it.order < lesson.order }
        return if (previous == null || previous.id in completed) {
            LessonStatus.AVAILABLE
        } else {
            LessonStatus.LOCKED_SEQUENCE
        }
    }

    /** The lesson the learner should work on next, if any. */
    fun currentLesson(
        allLessons: List<Lesson>,
        completed: Set<LessonId>,
        inProgress: LessonId?
    ): Lesson? {
        allLessons.firstOrNull {
            statusFor(it, allLessons, completed, inProgress) == LessonStatus.IN_PROGRESS
        }?.let { return it }

        return allLessons.firstOrNull {
            statusFor(it, allLessons, completed, inProgress) == LessonStatus.AVAILABLE
        }
    }

    /**
     * Whether the lesson screen is allowed to open for this lesson. Completed
     * lessons stay openable so a learner can review them.
     */
    fun canOpen(
        lesson: Lesson,
        allLessons: List<Lesson>,
        completed: Set<LessonId>,
        inProgress: LessonId?
    ): Boolean = statusFor(lesson, allLessons, completed, inProgress).isUnlocked

    /** Lessons that actually have content, i.e. the denominator for progress. */
    fun implementedLessons(allLessons: List<Lesson>): List<Lesson> =
        allLessons.filter { it.isPlayable }

    fun completionPercent(allLessons: List<Lesson>, completed: Set<LessonId>): Int {
        val implemented = implementedLessons(allLessons)
        if (implemented.isEmpty()) return 0
        val doneCount = implemented.count { it.id in completed }
        return (doneCount * 100f / implemented.size).toInt()
    }
}