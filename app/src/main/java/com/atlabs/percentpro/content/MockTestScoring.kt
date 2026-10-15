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
 * All arithmetic is integer based, with marks held in quarter marks, so a score
 * is exact and reproducible.
 */
data class MockTestScore(
    val totalQuestions: Int,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val unanswered: Int,
    val markedForReview: Int,
    val correctMarksInQuarters: Int,
    val penaltyInQuarters: Int,
    val maxMarksInQuarters: Int
) {
    val scoreInQuarters: Int get() = correctMarksInQuarters - penaltyInQuarters

    val score: Double get() = scoreInQuarters / MockTestConfig.QUARTER

    val maxMarks: Double get() = maxMarksInQuarters / MockTestConfig.QUARTER

    /** Correct answers as a share of questions attempted. */
    val accuracyPercent: Int
        get() = if (attempted <= 0) 0 else (correct.toLong() * 100L / attempted.toLong()).toInt()

    /** Score as a share of the maximum obtainable, after negative marking. */
    val percentagePercent: Int
        get() = if (maxMarksInQuarters <= 0) {
            0
        } else {
            ((scoreInQuarters.toLong() * 100L) / maxMarksInQuarters.toLong()).toInt()
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
                correctMarksInQuarters = correct * config.marksPerCorrectInQuarters,
                penaltyInQuarters = incorrect * config.negativeMarkInQuarters,
                maxMarksInQuarters = session.questionCount * config.marksPerCorrectInQuarters
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
                correctMarksInQuarters = correct * config.marksPerCorrectInQuarters,
                penaltyInQuarters = incorrect * config.negativeMarkInQuarters,
                maxMarksInQuarters = totalQuestions * config.marksPerCorrectInQuarters
            )
        }
    }
}

/**
 * Formats a quarter-mark score as a plain decimal, e.g. 3.75 or -0.25.
 *
 * The input is always an exact multiple of a quarter mark, so the remainder is
 * rendered from a fixed table rather than by dividing, which keeps -0.25 from
 * being printed as -0.10.
 */
fun Double.formatMarks(): String {
    val quarters = Math.round(this * MockTestConfig.QUARTER).toInt()
    val negative = quarters < 0
    val magnitude = abs(quarters)
    val whole = magnitude / 4
    val remainder = magnitude % 4
    val sign = if (negative) "-" else ""
    return "$sign$whole.${QUARTER_DECIMALS[remainder]}"
}

private val QUARTER_DECIMALS = arrayOf("00", "25", "50", "75")

/** Question ids in paper order, used when a result is written to disk. */
fun MockTestSession.questionIdOrder(): List<QuestionId> = questions.map { it.id }