package com.atlabs.percentpro.model

import androidx.annotation.StringRes

/** Stable identifier for a mock test. */
@JvmInline
value class MockTestId(val value: String)

/**
 * Everything that defines a mock test: how many questions, how long, and how it
 * is marked.
 *
 * Marks are held in quarter marks as integers so scoring never depends on binary
 * floating point. A config with `marksPerCorrectInQuarters = 4` and
 * `negativeMarkInQuarters = 1` means +1.00 for a correct answer and −0.25 for a
 * wrong one. Other exams can be added by supplying a different config, with no
 * change to the scoring code.
 */
data class MockTestConfig(
    val id: MockTestId,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val questionCount: Int,
    val durationMillis: Long,
    val marksPerCorrectInQuarters: Int,
    val negativeMarkInQuarters: Int
) {
    val marksPerCorrect: Double get() = marksPerCorrectInQuarters / QUARTER
    val negativeMark: Double get() = negativeMarkInQuarters / QUARTER

    val maxScore: Double
        get() = (questionCount * marksPerCorrectInQuarters) / QUARTER

    val durationMinutes: Int get() = (durationMillis / 60_000L).toInt()

    companion object {
        const val QUARTER = 4.0
    }
}

enum class MockTestSubmission {
    /** The learner tapped Submit. */
    MANUAL,

    /** The clock reached the deadline first. */
    TIMEOUT
}