package com.atlabs.percentpro.model

import androidx.annotation.StringRes

/** Stable identifier for a mock test. */
@JvmInline
value class MockTestId(val value: String)

/**
 * Everything that defines a mock test: how many questions, how long, and how it
 * is marked.
 *
 * Marks are held in twelfths of a mark as integers, so scoring never depends on
 * binary floating point. A config with `marksPerCorrectInTwelfths = 4` and
 * `negativeMarkInTwelfths = 1` means +1.00 for a correct answer and −0.25 for a
 * wrong one.
 *
 * Twelfths rather than quarters because the Indian government exams this app
 * covers do not all use a quarter penalty: SSC CGL/CHSL deduct 0.50, SSC GD and
 * the banking papers deduct 0.25, and the Railway papers deduct exactly one
 * third. Twelve is the least common multiple of 2, 3 and 4, so every real
 * scheme is a whole number of twelfths (+2 = 24, −0.50 = 6, −0.25 = 3, −1/3 = 4)
 * and no scheme has to be rounded.
 *
 * Other exams can be added by supplying a different config, with no change to
 * the scoring code.
 */
data class MockTestConfig(
    val id: MockTestId,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val questionCount: Int,
    val durationMillis: Long,
    val marksPerCorrectInTwelfths: Int,
    val negativeMarkInTwelfths: Int,

    /**
     * The examination this paper is written for, or null for the generic paper.
     *
     * When set, the paper is built only from questions tagged for that exam, so
     * it stays inside the section it claims to drill. This is not persisted: the
     * codec stores the test id and resolves the config from the catalog on
     * restore, so an exam paper needs no change to the saved format.
     */
    val examId: ExamId? = null,

    /**
     * The year this paper represents, or null for a paper that is not tied to one.
     *
     * A scheme span can be opened by any year it covers, so this is what
     * distinguishes "2017 pattern" from "2024 pattern" when both draw on the same
     * scheme. Not persisted: the codec stores the test id and rebuilds the config
     * from the catalog on restore.
     */
    val year: Int? = null,

    /**
     * Rotates which questions a paper draws from the exam's tagged pool.
     *
     * An exam holds far fewer questions than the sum of its year papers, so two
     * papers drawn the same way would be the same paper. Offsetting the start
     * point spreads them out. It is deliberately not a seed: a paper has to be
     * reproducible, or a saved result could never be shown against the same
     * questions again.
     */
    val poolOffset: Int = 0
) {
    val marksPerCorrect: Double get() = marksPerCorrectInTwelfths / MARKS_UNIT
    val negativeMark: Double get() = negativeMarkInTwelfths / MARKS_UNIT

    val maxScore: Double
        get() = (questionCount * marksPerCorrectInTwelfths) / MARKS_UNIT

    val durationMinutes: Int get() = (durationMillis / 60_000L).toInt()

    companion object {

        /** Marks are counted in twelfths of a mark, so scoring stays in integers. */
        const val MARKS_UNIT = 12.0
    }
}

enum class MockTestSubmission {
    /** The learner tapped Submit. */
    MANUAL,

    /** The clock reached the deadline first. */
    TIMEOUT
}