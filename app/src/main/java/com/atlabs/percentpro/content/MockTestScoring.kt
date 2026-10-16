package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.QuestionId
import kotlin.math.abs

/**
 * The score of a mock test.
 *
 * [accuracyPercent] and [percentagePercent] are deliberately separate numbers and
 * must be labelled differently on screen: accuracy only looks at questions the
 * learner actually attempted, while percentage score is measured against the
 * whole paper and includes negative marks.
 *
 * All arithmetic is integer based, with marks held in twelfths of a mark, so a
 * score is exact and reproducible.
 */
data class MockTestScore(
    val totalQuestions: Int,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val unanswered: Int,
    val markedForReview: Int,
    val correctMarksInTwelfths: Int,
    val penaltyInTwelfths: Int,
    val maxMarksInTwelfths: Int
) {
    val scoreInTwelfths: Int get() = correctMarksInTwelfths - penaltyInTwelfths

    val score: Double get() = scoreInTwelfths / MockTestConfig.MARKS_UNIT

    val maxMarks: Double get() = maxMarksInTwelfths / MockTestConfig.MARKS_UNIT

    /** Correct answers as a share of questions attempted. */
    val accuracyPercent: Int
        get() = if (attempted <= 0) 0 else (correct.toLong() * 100L / attempted.toLong()).toInt()

    /** Score as a share of the maximum obtainable, after negative marking. */
    val percentagePercent: Int
        get() = if (maxMarksInTwelfths <= 0) {
            0
        } else {
            ((scoreInTwelfths.toLong() * 100L) / maxMarksInTwelfths.toLong()).toInt()
        }

    val isPassed: Boolean get() = percentagePercent >= 40

    companion object {

        fun from(session: MockTestSession): MockTestScore {
            val config = session.config
            val correct = session.correctCount
            val incorrect = session.incorrectCount
            return MockTestScore(
                totalQuestions = session.questionCount,
                attempted = session.attemptedCount,
                correct = correct,
                incorrect = incorrect,
                unanswered = session.unattemptedCount,
                markedForReview = session.markedForReviewCount,
                correctMarksInTwelfths = correct * config.marksPerCorrectInTwelfths,
                penaltyInTwelfths = incorrect * config.negativeMarkInTwelfths,
                maxMarksInTwelfths = session.questionCount * config.marksPerCorrectInTwelfths
            )
        }

        /** Exposed for direct unit testing of the marking rules. */
        fun of(
            config: MockTestConfig,
            totalQuestions: Int,
            correct: Int,
            incorrect: Int,
            markedForReview: Int = 0
        ): MockTestScore {
            val unanswered = (totalQuestions - correct - incorrect).coerceAtLeast(0)
            return MockTestScore(
                totalQuestions = totalQuestions,
                attempted = correct + incorrect,
                correct = correct,
                incorrect = incorrect,
                unanswered = unanswered,
                markedForReview = markedForReview,
                correctMarksInTwelfths = correct * config.marksPerCorrectInTwelfths,
                penaltyInTwelfths = incorrect * config.negativeMarkInTwelfths,
                maxMarksInTwelfths = totalQuestions * config.marksPerCorrectInTwelfths
            )
        }
    }
}

/**
 * Formats a mark score as a plain decimal, e.g. 3.75 or -0.25.
 *
 * The input is always an exact multiple of a twelfth of a mark, so the
 * remainder is rendered from a fixed table rather than by dividing. Dividing
 * would print -0.25 as -0.10 once floating point is involved.
 */
fun Double.formatMarks(): String {
    val twelfths = Math.round(this * MockTestConfig.MARKS_UNIT).toInt()
    val negative = twelfths < 0
    val magnitude = abs(twelfths)
    val whole = magnitude / TWELFTHS_PER_MARK
    val remainder = magnitude % TWELFTHS_PER_MARK
    val sign = if (negative) "-" else ""
    return "$sign$whole.${TWELFTH_DECIMALS[remainder]}"
}

/**
 * Formats a penalty, preferring the exact fraction for a third of a mark.
 *
 * The Railway papers deduct one third, which is four twelfths but not a
 * terminating decimal. Printing it as "0.33" would be a rounding error a learner
 * can catch by hand, because three wrong answers at 0.33 is 0.99 and not 1.00.
 * So a third is rendered as the fraction instead.
 *
 * The sign follows the input exactly as [formatMarks] does. A config holds the
 * penalty as a positive magnitude and lets the surrounding label say whether it
 * is being deducted, so both `1/3` and `-1/3` are reachable.
 */
fun Double.formatPenalty(): String {
    val twelfths = Math.round(this * MockTestConfig.MARKS_UNIT).toInt()
    if (abs(twelfths) != ONE_THIRD_IN_TWELFTHS) return formatMarks()
    return if (twelfths < 0) "-1/3" else "1/3"
}

private const val TWELFTHS_PER_MARK = 12

/** A penalty of exactly one third of a mark, held as twelfths. */
private const val ONE_THIRD_IN_TWELFTHS = 4

/** Rounded to two places for display; the arithmetic behind it stays exact. */
private val TWELFTH_DECIMALS = arrayOf(
    "00", "08", "17", "25", "33", "42", "50", "58", "67", "75", "83", "92"
)

/** Question ids in paper order, used when a result is written to disk. */
fun MockTestSession.questionIdOrder(): List<QuestionId> = questions.map { it.id }