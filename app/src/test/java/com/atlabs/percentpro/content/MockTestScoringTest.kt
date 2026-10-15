package com.atlabs.percentpro.content

import com.atlabs.percentpro.R
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalizedText
import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.model.MockTestSubmission
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockTestScoringTest {

    private val config = MockTestConfig(
        id = MockTestId("test_scoring"),
        titleRes = R.string.mock_title_percentage_aptitude,
        descriptionRes = R.string.mock_description_percentage_aptitude,
        questionCount = 4,
        durationMillis = 15 * 60 * 1000L,
        marksPerCorrectInQuarters = 4,
        negativeMarkInQuarters = 1
    )

    private fun question(id: String, correctIndex: Int) = Question(
        id = QuestionId(id),
        topic = LessonId.WHAT_IS_A_PERCENTAGE,
        difficulty = QuestionDifficulty.EASY,
        prompt = LocalizedText.of("Question $id", "प्रश्न $id"),
        options = listOf("A", "B", "C", "D").map { LocalizedText.of("$it$id", "$it$id") },
        correctIndex = correctIndex,
        explanation = LocalizedText.of("Because $id", "क्योंकि $id")
    )

    /** Four questions whose correct answers are 0, 1, 2, 3 in order. */
    private val questions = listOf(
        question("q1", correctIndex = 0),
        question("q2", correctIndex = 1),
        question("q3", correctIndex = 2),
        question("q4", correctIndex = 3)
    )

    private fun session() = MockTestSession.start(
        sessionId = "session-1",
        config = config,
        questions = questions,
        startedAtEpochMillis = 1_000L
    )

    @Test
    fun `marks are held as whole quarter marks`() {
        assertEquals(1.0, config.marksPerCorrect, 0.0)
        assertEquals(0.25, config.negativeMark, 0.0)
        assertEquals(4.0, config.maxScore, 0.0)
    }

    @Test
    fun `all correct scores full marks`() {
        var live = session()
        questions.indices.forEach { index ->
            live = live.goTo(index).selectOption(index)
        }

        val score = live.score()

        assertEquals(4, score.correct)
        assertEquals(0, score.incorrect)
        assertEquals(0, score.unanswered)
        assertEquals(16, score.scoreInQuarters)
        assertEquals(16, score.maxMarksInQuarters)
        assertEquals(4.0, score.score, 0.0)
        assertEquals(100, score.percentagePercent)
    }

    @Test
    fun `wrong answers are deducted at a quarter mark each`() {
        // q1 correct, q2 wrong, q3 wrong, q4 unanswered: +1 -0.25 -0.25 = 0.50
        var live = session()
        live = live.goTo(0).selectOption(0)
        live = live.goTo(1).selectOption(3)
        live = live.goTo(2).selectOption(0)

        val score = live.score()

        assertEquals(1, score.correct)
        assertEquals(2, score.incorrect)
        assertEquals(1, score.unanswered)
        assertEquals(2, score.scoreInQuarters)
        assertEquals(0.5, score.score, 0.0)
    }

    @Test
    fun `unanswered questions score nothing and are not penalised`() {
        var live = session()
        live = live.goTo(0).selectOption(0)

        val score = live.score()

        assertEquals(3, score.unanswered)
        assertEquals(4, score.scoreInQuarters)
    }

    @Test
    fun `a fully wrong paper does not score below negative marks`() {
        var live = session()
        questions.indices.forEach { index ->
            live = live.goTo(index).selectOption((index + 1) % 4)
        }

        val score = live.score()

        assertEquals(0, score.correct)
        assertEquals(4, score.incorrect)
        assertEquals(-4, score.scoreInQuarters)
        assertEquals(-1.0, score.score, 0.0)
    }

    @Test
    fun `accuracy ignores skipped questions and differs from percentage score`() {
        var live = session()
        live = live.goTo(0).selectOption(0)
        live = live.goTo(1).selectOption(3)

        val score = live.score()

        // Attempted 2, correct 1 => 50% accuracy.
        assertEquals(50, score.accuracyPercent)
        // Score 0.75 of 4.00 => 18% after negative marking.
        assertEquals(18, score.percentagePercent)
        assertNotEquals(score.accuracyPercent, score.percentagePercent)
    }

    @Test
    fun `accuracy is zero when nothing was attempted`() {
        val score = session().score()

        assertEquals(0, score.accuracyPercent)
        assertEquals(0, score.percentagePercent)
        assertEquals(0, score.scoreInQuarters)
    }

    @Test
    fun `clearing a response returns that question to unanswered`() {
        var live = session()
        live = live.goTo(0).selectOption(2)
        assertEquals(1, live.answeredCount)

        live = live.clearCurrentResponse()

        assertEquals(0, live.answeredCount)
        assertEquals(4, live.unattemptedCount)
        assertEquals(0, live.score().incorrect)
    }

    @Test
    fun `selecting a different option replaces the previous answer`() {
        var live = session()
        live = live.goTo(0).selectOption(2)
        live = live.selectOption(0)

        assertEquals(1, live.answeredCount)
        assertEquals(0, live.selectedOptionFor(questions[0].id))
        assertEquals(1, live.score().correct)
    }

    @Test
    fun `an out of range option is ignored`() {
        val live = session().selectOption(99)

        assertEquals(0, live.answeredCount)
        assertNull(live.selectedOptionFor(questions[0].id))
    }

    @Test
    fun `marks format to two decimal places`() {
        assertEquals("1.00", MockTestScore.from(session().goTo(0).selectOption(0)).score.formatMarks())
        assertEquals(
            "-0.25",
            MockTestScore.from(session().goTo(0).selectOption(3)).score.formatMarks()
        )
        assertEquals("0.00", MockTestScore.from(session()).score.formatMarks())
        assertEquals("0.75", 0.75.formatMarks())
        assertEquals("3.75", 3.75.formatMarks())
        assertEquals("-1.25", (-1.25).formatMarks())
    }

    @Test
    fun `different marking schemes need no scoring change`() {
        val plusTwoMinusHalf = config.copy(
            id = MockTestId("plus_two"),
            marksPerCorrectInQuarters = 8,
            negativeMarkInQuarters = 2,
            questionCount = 2
        )
        val twoQuestions = questions.take(2)
        var live = MockTestSession.start("s2", plusTwoMinusHalf, twoQuestions, 0L)
        live = live.goTo(0).selectOption(0)
        live = live.goTo(1).selectOption(3)

        val score = live.score()

        assertEquals(2.0, plusTwoMinusHalf.marksPerCorrect, 0.0)
        assertEquals(0.5, plusTwoMinusHalf.negativeMark, 0.0)
        assertEquals(1.5, score.score, 0.0)
    }
}

class MockTestSessionTest {

    private val config = MockTestConfig(
        id = MockTestId("session_test"),
        titleRes = R.string.mock_title_percentage_aptitude,
        descriptionRes = R.string.mock_description_percentage_aptitude,
        questionCount = 3,
        durationMillis = 15 * 60 * 1000L,
        marksPerCorrectInQuarters = 4,
        negativeMarkInQuarters = 1
    )

    private fun question(id: String) = Question(
        id = QuestionId(id),
        topic = LessonId.WHAT_IS_A_PERCENTAGE,
        difficulty = QuestionDifficulty.EASY,
        prompt = LocalizedText.of("Q$id", "प्रश्न $id"),
        options = listOf("A", "B", "C", "D").map { LocalizedText.of("$it", "$it") },
        correctIndex = 0,
        explanation = LocalizedText.of("E$id", "E$id")
    )

    private val questions = listOf(question("q1"), question("q2"), question("q3"))

    private fun session(startedAt: Long = 0L) =
        MockTestSession.start("s", config, questions, startedAt)

    @Test
    fun `the deadline is absolute and remaining time is derived from it`() {
        val live = session(startedAt = 1_000L)

        assertEquals(1_000L + config.durationMillis, live.deadlineEpochMillis)
        assertEquals(600_000L, live.remainingMillis(1_000L + 300_000L))
        assertEquals(10, live.remainingMinutes(1_000L + 300_000L))
    }

    @Test
    fun `remaining time never goes negative`() {
        val live = session(startedAt = 0L)

        assertEquals(0L, live.remainingMillis(10_000_000L))
        assertTrue(live.hasExpired(10_000_000L))
    }

    @Test
    fun `a session is immutable - mutations return new values`() {
        val original = session()
        val moved = original.next()

        assertEquals(0, original.currentIndex)
        assertEquals(1, moved.currentIndex)
    }

    @Test
    fun `navigation is clamped to the paper`() {
        val live = session()

        assertTrue(live.previous().currentIndex == 0)
        assertEquals(2, live.goTo(99).currentIndex)
    }

    @Test
    fun `the first question counts as visited when the paper starts`() {
        assertEquals(setOf(QuestionId("q1")), session().visitedQuestionIds)
    }

    @Test
    fun `visiting records the question`() {
        val live = session().goTo(2)

        assertTrue(QuestionId("q3") in live.visitedQuestionIds)
        assertEquals(QuestionPaletteState.VISITED_UNANSWERED, live.paletteStateFor(QuestionId("q3")))
    }

    @Test
    fun `palette distinguishes answered marked and untouched questions`() {
        var live = session()
        live = live.goTo(0).selectOption(1)
        live = live.goTo(1).toggleReviewForCurrent()
        live = live.goTo(2).selectOption(0).toggleReviewForCurrent()

        val states = live.paletteStates()

        assertEquals(QuestionPaletteState.ANSWERED, states[0])
        assertEquals(QuestionPaletteState.MARKED_REVIEW, states[1])
        assertEquals(QuestionPaletteState.ANSWERED_AND_MARKED_REVIEW, states[2])
        assertTrue(states[0].isAnswered)
        assertFalse(states[0].isMarkedForReview)
        assertTrue(states[2].isAnswered)
        assertTrue(states[2].isMarkedForReview)
    }

    @Test
    fun `a question that was never visited is NOT_VISITED`() {
        val empty = MockTestSession.start("s", config, questions, 0L).copy(
            visitedQuestionIds = emptySet()
        )

        assertEquals(QuestionPaletteState.NOT_VISITED, empty.paletteStateFor(QuestionId("q1")))
    }

    @Test
    fun `review marking toggles both ways and works without an answer`() {
        var live = session().goTo(1)
        live = live.toggleReviewForCurrent()
        assertTrue(live.isMarked(QuestionId("q2")))
        assertEquals(1, live.markedForReviewCount)

        live = live.toggleReviewForCurrent()
        assertFalse(live.isMarked(QuestionId("q2")))
        assertEquals(0, live.markedForReviewCount)
    }

    @Test
    fun `clearing a response leaves the question visited`() {
        val live = session().goTo(0).selectOption(1).clearCurrentResponse()

        assertTrue(QuestionId("q1") in live.visitedQuestionIds)
        assertEquals(QuestionPaletteState.VISITED_UNANSWERED, live.paletteStateFor(QuestionId("q1")))
    }

    @Test
    fun `submission is idempotent - the first submission wins`() {
        val manual = session().submit(MockTestSubmission.MANUAL)
        val second = manual.submit(MockTestSubmission.TIMEOUT)

        assertEquals(MockTestSubmission.MANUAL, manual.submission)
        assertEquals(MockTestSubmission.MANUAL, second.submission)
        assertTrue(second.isSubmitted)
    }

    @Test
    fun `a submitted paper refuses every mutation`() {
        val frozen = session().goTo(0).selectOption(0).submit(MockTestSubmission.MANUAL)

        assertEquals(frozen, frozen.selectOption(2))
        assertEquals(frozen, frozen.clearCurrentResponse())
        assertEquals(frozen, frozen.toggleReviewForCurrent())
        assertEquals(frozen, frozen.goTo(2))
        assertEquals(frozen, frozen.next())
        assertEquals(frozen, frozen.previous())
    }

    @Test
    fun `answering never changes the paper length or the deadline`() {
        val before = session(startedAt = 5_000L)
        val after = before.goTo(0).selectOption(3)

        assertEquals(before.questions, after.questions)
        assertEquals(before.deadlineEpochMillis, after.deadlineEpochMillis)
    }

    @Test
    fun `first and last question flags are correct`() {
        val live = session()

        assertTrue(live.isFirstQuestion)
        assertFalse(live.isLastQuestion)
        assertTrue(live.goTo(2).isLastQuestion)
        assertFalse(live.isSubmitted)
    }
}