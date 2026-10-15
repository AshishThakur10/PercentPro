package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId

/**
 * What gets written to disk about a learner's progress.
 *
 * Deliberately free of Android types so the encode/decode rules can be unit
 * tested without DataStore.
 */
data class LearningProgress(
    val completedLessonIds: Set<LessonId> = emptySet(),
    val lessonInProgress: LessonId? = null,
    val lastBlockIndex: Int = 0
) {
    val hasStarted: Boolean
        get() = completedLessonIds.isNotEmpty() || lessonInProgress != null

    fun isCompleted(id: LessonId): Boolean = id in completedLessonIds

    fun markCompleted(id: LessonId) = copy(
        completedLessonIds = completedLessonIds + id,
        // A finished lesson is no longer the one being worked on.
        lessonInProgress = if (lessonInProgress == id) null else lessonInProgress,
        lastBlockIndex = if (lessonInProgress == id) 0 else lastBlockIndex
    )

    fun startLesson(id: LessonId) = copy(
        lessonInProgress = id,
        lastBlockIndex = 0
    )

    fun setBlockIndex(index: Int) = copy(lastBlockIndex = index.coerceAtLeast(0))
}

/**
 * Converts [LearningProgress] to and from flat values for DataStore, and is the
 * only place that validates what comes back off disk.
 *
 * Saved data outlives app versions, so anything unrecognised is dropped rather
 * than trusted: an unknown lesson id, a version from the future, or a block index
 * that no longer exists all fall back to a safe state instead of crashing or
 * unlocking content the learner never reached.
 */
object ProgressCodec {

    const val CURRENT_VERSION = 1

    data class Encoded(
        val version: Int,
        val completedLessonIds: Set<String>,
        val lessonInProgress: String?,
        val lastBlockIndex: Int
    )

    fun encode(progress: LearningProgress): Encoded = Encoded(
        version = CURRENT_VERSION,
        completedLessonIds = progress.completedLessonIds.map { it.storageKey }.toSet(),
        lessonInProgress = progress.lessonInProgress?.storageKey,
        lastBlockIndex = progress.lastBlockIndex
    )

    /**
     * @param playableLessons lessons whose content exists; a lesson in progress
     * that is not playable is discarded.
     * @param blockCount number of blocks in the lesson being resumed.
     */
    fun decode(
        version: Int?,
        completedLessonIds: Set<String>?,
        lessonInProgress: String?,
        lastBlockIndex: Int?,
        playableLessons: Set<LessonId>,
        blockCount: Int
    ): LearningProgress {
        if (version == null || version != CURRENT_VERSION) return LearningProgress()

        val completed = completedLessonIds
            .orEmpty()
            .mapNotNull(LessonId::fromStorageKey)
            .filter { it in playableLessons }
            .toSet()

        val active = LessonId.fromStorageKey(lessonInProgress)
            ?.takeIf { it in playableLessons && it !in completed }

        // Clamped rather than rejected: an older save with a higher index should
        // still land the learner on the last block, not on an empty lesson.
        val safeIndex = lastBlockIndex
            ?.takeIf { active != null }
            ?.coerceIn(0, (blockCount - 1).coerceAtLeast(0))
            ?: 0

        return LearningProgress(
            completedLessonIds = completed,
            lessonInProgress = active,
            lastBlockIndex = safeIndex
        )
    }
}