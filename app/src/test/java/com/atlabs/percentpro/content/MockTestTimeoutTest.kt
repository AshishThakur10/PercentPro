package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LocalizedText
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.MockTestResult
import com.atlabs.percentpro.model.MockTestSubmission
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What happens when the clock runs out.
 *
 * A timed paper can be finalised two ways -- the reader presses Submit, or the
 * deadline passes while they are looking at it -- and both can land within
 * milliseconds of each other. If that produced two results, or mutated the paper
 * after it was frozen, the learner's history would show an attempt they never
 * made.
 *
 * The submission guard itself lives in MockTestExamViewModel, which needs a
 * Context and cannot run on the JVM. Everything it relies on is pure and is
 * checked here: the session's own idempotency, the score derived from a frozen
 * paper, and the recovery of a paper that expired while the app was closed.
 */
class MockTestTimeoutTest {

    private val questions = listOf(
        question("q1", correctIndex = 0),
        question("q2", correctIndex = 1),
        question("q3", correctIndex = 2),
        question("q4", correctIndex = 3)
    )

    private val config = MockTestCatalog().tests().first()

    private fun question(id: String, correctIndex: Int) = Question(
        id = QuestionId(id),
        topic = LessonId.WHAT_IS_A_PERCENTAGE,
        difficulty = QuestionDifficulty.EASY,
        prompt = LocalizedText.of("Q$id", "Q$id"),
        options = listOf("A", "B", "C", "D").map { LocalizedText.of(it, it) },
        correctIndex = correctIndex,
        explanation = LocalizedText.of("E$id", "E$id")
    )

    /** Started at t=0; the config allows 15 minutes. */
    private fun live() = MockTestSession.start(
        sessionId = "timeout_session",
        config = config,
        questions = questions,
        startedAtEpochMillis = 0L
    )

    private val expiredAt = 16 * 60 * 1000L   // past the 15 minute deadline

    // ---- expiry is detected from the deadline, not a counter ---------------

    @Test
    fun `time remaining reaches zero once the deadline passes`() {
        val session = live()
        assertTrue(session.remainingMillis(0L) > 0)
        assertEquals(0L, session.remainingMillis(expiredAt))
    }

    @Test
    fun `time remaining never drifts below zero`() {
        assertEquals(0L, live().remainingMillis(expiredAt * 10))
    }

    // ---- submitting is idempotent ------------------------------------------

    @Test
    fun `submitting twice yields one submission`() {
        val session = live()

        val timedOut = session.submit(MockTestSubmission.TIMEOUT)
        val again = timedOut.submit(MockTestSubmission.MANUAL)

        assertTrue(timedOut.isSubmitted)
        assertTrue(again.isSubmitted)
        // The first submission wins. A manual submit racing the timeout must not
        // relabel an already-timeout paper as a deliberate one.
        assertEquals(MockTestSubmission.TIMEOUT, timedOut.submission)
        assertEquals(MockTestSubmission.TIMEOUT, again.submission)
    }

    @Test
    fun `submitting twice scores identically`() {
        val session = live().selectOption(0)
        val once = session.submit(MockTestSubmission.TIMEOUT).score()
        val twice = session.submit(MockTestSubmission.TIMEOUT)
            .submit(MockTestSubmission.MANUAL)
            .score()

        assertEquals(once.score, twice.score, 1e-9)
        assertEquals(once.maxMarks, twice.maxMarks, 1e-9)
    }

    @Test
    fun `a submitted paper cannot be mutated afterwards`() {
        val submitted = live().submit(MockTestSubmission.TIMEOUT)

        // Every mutator on the session must be inert once frozen. The ViewModel
        // also refuses these, but the session is the last line of defence.
        assertEquals(submitted.answers, submitted.selectOption(1).answers)
        assertEquals(submitted.answers, submitted.clearCurrentResponse().answers)
        assertEquals(submitted.markedForReview, submitted.toggleReviewForCurrent().markedForReview)
        assertEquals(submitted.currentIndex, submitted.next().currentIndex)
        assertEquals(submitted.currentIndex, submitted.goTo(2).currentIndex)
    }

    @Test
    fun `a submitted paper keeps the answers it had`() {
        val answered = live().selectOption(0).goTo(1).selectOption(1)
        val submitted = answered.submit(MockTestSubmission.TIMEOUT)

        assertEquals(answered.answers, submitted.answers)
        assertEquals(2, submitted.answeredCount)
    }

    // ---- scoring a paper that ran out of time ------------------------------

    @Test
    fun `unanswered questions at timeout score nothing and are not penalised`() {
        val submitted = live().submit(MockTestSubmission.TIMEOUT)
        val score = submitted.score()

        // Nothing answered: zero marks, and crucially no negative marks for the
        // four questions the reader never got to.
        assertEquals(0.0, score.score, 1e-9)
        assertEquals(4, score.unanswered)
    }

    @Test
    fun `a partly answered paper scores what was actually answered`() {
        // selectOption answers the *current* question and stays on it, so each
        // answer needs an explicit goTo.
        val partial = live()
            .selectOption(0)   // q1 correct
            .goTo(1)
            .selectOption(0)   // q2 wrong, its correct answer is 1
            .goTo(2)
            .selectOption(2)   // q3 correct, q4 never reached

        val submitted = partial.submit(MockTestSubmission.TIMEOUT)
        val score = submitted.score()

        // 2 correct (+2), 1 incorrect (-0.25), 1 unanswered (0).
        assertEquals(1.75, score.score, 1e-9)
        assertEquals(1, score.unanswered)
    }

    @Test
    fun `a paper answered in full at timeout scores the full marks`() {
        val full = live()
            .selectOption(0).goTo(1).selectOption(1)
            .goTo(2).selectOption(2).goTo(3).selectOption(3)
            .submit(MockTestSubmission.TIMEOUT)

        assertEquals(4.0, full.score().score, 1e-9)
    }

    // ---- recovery of a paper that expired while the app was closed ----------

    @Test
    fun `an expired stored session decodes and can be scored`() {
        // What happens when the app is closed past the deadline: the stored
        // session is recovered with allowExpired and submitted rather than lost.
        val started = live().selectOption(0)
        val stored = MockTestCodec.encodeSession(started)
        val lookup: (QuestionId) -> Question? = { id -> questions.firstOrNull { it.id == id } }

        val restore = MockTestCodec.decodeSession(
            raw = stored,
            catalog = MockTestCatalog(),
            questionsById = lookup,
            nowEpochMillis = expiredAt,
            allowExpired = true
        )

        assertTrue("an expired session must still be recoverable", restore is MockTestCodec.SessionRestore.Ready)
        val recovered = (restore as MockTestCodec.SessionRestore.Ready).session
        assertEquals(1, recovered.answeredCount)

        val result = MockTestResult.from(recovered.submit(MockTestSubmission.TIMEOUT), expiredAt)
        assertEquals(1.0, result.score, 1e-9)
    }

    @Test
    fun `an expired stored session is refused without recovery`() {
        // The mirror image: if recovery is not explicitly requested, an expired
        // session is unusable rather than silently revived.
        val stored = MockTestCodec.encodeSession(live())
        val lookup: (QuestionId) -> Question? = { id -> questions.firstOrNull { it.id == id } }

        val restore = MockTestCodec.decodeSession(
            raw = stored,
            catalog = MockTestCatalog(),
            questionsById = lookup,
            nowEpochMillis = expiredAt,
            allowExpired = false
        )

        assertTrue(restore is MockTestCodec.SessionRestore.Unusable)
        val unusable = restore as MockTestCodec.SessionRestore.Unusable
        assertEquals(MockTestCodec.RestoreFailure.EXPIRED, unusable.reason)
    }

    @Test
    fun `a fresh session at the deadline boundary is still live`() {
        // One millisecond before expiry the paper must not be treated as expired,
        // or a learner loses the last second of their attempt.
        val deadline = config.durationMillis
        val justBefore = live()

        assertFalse(justBefore.isSubmitted)
        assertTrue(justBefore.remainingMillis(deadline - 1) > 0)
        assertEquals(0L, justBefore.remainingMillis(deadline))
    }
}