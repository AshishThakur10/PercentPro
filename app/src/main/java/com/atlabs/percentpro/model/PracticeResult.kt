package com.atlabs.percentpro.model

/**
 * The finished outcome of one practice session.
 *
 * Deliberately separate from [com.atlabs.percentpro.content.LearningProgress]:
 * practising a topic never marks a lesson complete, and finishing a lesson never
 * records a practice score.
 */
data class PracticeResult(
    val topic: LessonId,
    val completedAtEpochMillis: Long,
    val total: Int,
    val correct: Int,
    val incorrect: Int,
    val unanswered: Int,
    val accuracyPercent: Int
) {
    /** Counts must add up to the number of questions asked. */
    val isValid: Boolean
        get() = total >= 0 &&
            correct >= 0 &&
            incorrect >= 0 &&
            unanswered >= 0 &&
            (correct + incorrect + unanswered) == total &&
            accuracyPercent == accuracyOf(correct, total) &&
            completedAtEpochMillis >= 0L

    companion object {
        /** Integer-safe: computed in Long, never with floating point. */
        fun accuracyOf(correct: Int, total: Int): Int =
            if (total <= 0) 0 else ((correct.toLong() * 100L) / total.toLong()).toInt()

        /**
         * Counts are derived from the session rather than trusted from the UI,
         * so a session that was exited early still reports honestly.
         */
        fun of(
            topic: LessonId,
            completedAtEpochMillis: Long,
            total: Int,
            correct: Int,
            incorrect: Int,
            unanswered: Int
        ): PracticeResult = PracticeResult(
            topic = topic,
            completedAtEpochMillis = completedAtEpochMillis.coerceAtLeast(0L),
            total = total.coerceAtLeast(0),
            correct = correct.coerceIn(0, total.coerceAtLeast(0)),
            incorrect = incorrect.coerceIn(0, total.coerceAtLeast(0)),
            unanswered = unanswered.coerceIn(0, total.coerceAtLeast(0)),
            accuracyPercent = accuracyOf(correct.coerceIn(0, total.coerceAtLeast(0)), total)
        )
    }
}
