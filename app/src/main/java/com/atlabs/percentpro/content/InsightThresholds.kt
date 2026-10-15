package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.QuestionDifficulty

/**
 * Every threshold the insights system uses, in one place.
 *
 * They are gathered deliberately: a classification that quietly hard-codes its
 * own cut-off in several places is impossible to tune and easy to make
 * inconsistent between the dashboard and the recommendation text.
 */
object InsightThresholds {

    /** Below this many attempts a topic is not classified at all. */
    const val MIN_ATTEMPTS_FOR_CLASSIFICATION = 5

    /** At or above this many attempts, accuracy below this means "needs practice". */
    const val NEEDS_PRACTICE_BELOW_PERCENT = 60

    /** At or above this many attempts, accuracy below this (but at/above the above) is "improving". */
    const val IMPROVING_BELOW_PERCENT = 80

    /** Accuracy at or above this, with enough attempts, is "strong". */
    const val STRONG_AT_OR_ABOVE_PERCENT = 80

    /** Recent window, and the window it is compared against. */
    const val RECENT_WINDOW_SIZE = 20

    /**
     * Basic accuracy at or above which a strong learner is nudged upwards.
     *
     * Deliberately stricter than [STRONG_AT_OR_ABOVE_PERCENT]: suggesting
     * harder material needs more evidence than simply calling something strong.
     */
    const val ADVANCE_DIFFICULTY_AT_OR_ABOVE_PERCENT = 85

    /** Attempts needed before a topic may be called a weak-topic practice target. */
    const val MIN_ATTEMPTS_FOR_WEAK_TARGET = 5

    /** Mixed review needs at least this many studied topics to be worth offering. */
    const val MIN_TOPICS_FOR_MIXED_REVIEW = 2

    /** Difficulty levels are attempted in this order when stepping up. */
    val ASCENDING_DIFFICULTY = listOf(
        QuestionDifficulty.EASY,
        QuestionDifficulty.MEDIUM,
        QuestionDifficulty.HARD
    )

    /** The next difficulty up, or null when already at the top. */
    fun harderThan(difficulty: QuestionDifficulty): QuestionDifficulty? {
        val index = ASCENDING_DIFFICULTY.indexOf(difficulty)
        return ASCENDING_DIFFICULTY.getOrNull(index + 1)
    }
}