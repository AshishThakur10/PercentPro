package com.atlabs.percentpro.model

import androidx.annotation.StringRes
import com.atlabs.percentpro.R

enum class Difficulty(@StringRes val labelRes: Int) {
    EASY(R.string.difficulty_easy),
    MEDIUM(R.string.difficulty_medium),
    HARD(R.string.difficulty_hard)
}

/**
 * Where a lesson sits for the learner right now.
 *
 * The two locked states are deliberately distinct: one means "finish the lesson
 * before this one", the other means "this lesson is not written yet". Showing the
 * same lock icon for both would be dishonest about what is actually blocked.
 */
enum class LessonStatus {
    /** Unlocked, not started. */
    AVAILABLE,

    /** Unlocked and part-way through. */
    IN_PROGRESS,

    /** Unlocked and finished; can be reopened for review. */
    COMPLETED,

    /** The previous lesson has not been completed yet. */
    LOCKED_SEQUENCE,

    /** The lesson is on the path but its content is not implemented. */
    LOCKED_UPCOMING;

    val isUnlocked: Boolean
        get() = this == AVAILABLE || this == IN_PROGRESS || this == COMPLETED

    val isLocked: Boolean get() = !isUnlocked
}

/**
 * Metadata for one node on the learning path. Body content lives separately in
 * `content.LessonContentRepository`, so a lesson can be listed long before it is
 * playable.
 */
data class Lesson(
    val id: LessonId,
    val order: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val difficulty: Difficulty,
    val durationMinutes: Int,
    /** True only once every content block for this lesson exists. */
    val isImplemented: Boolean
) {
    val isPlayable: Boolean get() = isImplemented && id != LessonId.COMPETITIVE_EXAM_CHALLENGE
}