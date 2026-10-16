package com.atlabs.percentpro.model

import com.atlabs.percentpro.content.MockTestSession

/** One question as it was answered in a finished mock test. */
data class MockTestQuestionRecord(
    val questionId: QuestionId,
    val order: Int,
    val selectedOptionIndex: Int?,
    val markedForReview: Boolean
)

/**
 * A finished mock test, stored permanently.
 *
 * Holds everything needed to re-open a past paper read-only: the exact question
 * order, the responses, the marking scheme that applied at the time, and whether
 * the clock or the learner ended it. Nothing here can touch lesson progress or
 * practice history.
 */
data class MockTestResult(
    val sessionId: String,
    val testId: MockTestId,
    val testTitleRes: Int,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val records: List<MockTestQuestionRecord>,
    val totalQuestions: Int,
    val correct: Int,
    val incorrect: Int,
    val unanswered: Int,
    val markedForReview: Int,
    val scoreInTwelfths: Int,
    val maxMarksInTwelfths: Int,
    val marksPerCorrectInTwelfths: Int,
    val negativeMarkInTwelfths: Int,
    val timeTakenMillis: Long,
    val submission: MockTestSubmission
) {
    val score: Double get() = scoreInTwelfths / MockTestConfig.MARKS_UNIT

    val maxMarks: Double get() = maxMarksInTwelfths / MockTestConfig.MARKS_UNIT

    val accuracyPercent: Int
        get() = if (correct + incorrect <= 0) 0 else correct * 100 / (correct + incorrect)

    val percentagePercent: Int
        get() = if (maxMarksInTwelfths <= 0) 0 else scoreInTwelfths * 100 / maxMarksInTwelfths

    val didTimeOut: Boolean get() = submission == MockTestSubmission.TIMEOUT

    companion object {

        fun from(
            session: MockTestSession,
            completedAtEpochMillis: Long
        ): MockTestResult {
            val score = session.score()
            val config = session.config
            return MockTestResult(
                sessionId = session.sessionId,
                testId = config.id,
                testTitleRes = config.titleRes,
                startedAtEpochMillis = session.startedAtEpochMillis,
                completedAtEpochMillis = completedAtEpochMillis,
                records = session.questions.mapIndexed { index, question ->
                    MockTestQuestionRecord(
                        questionId = question.id,
                        order = index,
                        selectedOptionIndex = session.selectedOptionFor(question.id),
                        markedForReview = session.isMarked(question.id)
                    )
                },
                totalQuestions = session.questionCount,
                correct = score.correct,
                incorrect = score.incorrect,
                unanswered = score.unanswered,
                markedForReview = score.markedForReview,
                scoreInTwelfths = score.scoreInTwelfths,
                maxMarksInTwelfths = score.maxMarksInTwelfths,
                marksPerCorrectInTwelfths = config.marksPerCorrectInTwelfths,
                negativeMarkInTwelfths = config.negativeMarkInTwelfths,
                // Never negative, and never longer than the test itself.
                timeTakenMillis = (completedAtEpochMillis - session.startedAtEpochMillis)
                    .coerceAtLeast(0L),
                submission = session.submission ?: MockTestSubmission.MANUAL
            )
        }
    }
}