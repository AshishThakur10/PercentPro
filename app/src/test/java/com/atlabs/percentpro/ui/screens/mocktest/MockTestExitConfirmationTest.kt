package com.atlabs.percentpro.ui.screens.mocktest

import com.atlabs.percentpro.content.MockTestCatalog
import com.atlabs.percentpro.content.MockTestSession
import com.atlabs.percentpro.model.LocalizedText
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.MockTestSubmission
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The "are you sure you want to lose this paper?" decision.
 *
 * Phase 8 added a confirmation before leaving a running mock test. The rule that
 * decides when to ask lives on [MockTestExamUiState.exitWouldDiscardWork], so it
 * is exercised here directly rather than through the ViewModel, which needs a
 * DataStore and therefore a device.
 *
 * What matters is that the confirmation appears for exactly the one case where
 * something is lost, and never for the cases where it is not.
 */
class MockTestExitConfirmationTest {

    private val questions = listOf(
        question("q1", correctIndex = 0),
        question("q2", correctIndex = 1),
        question("q3", correctIndex = 2),
        question("q4", correctIndex = 3)
    )

    private val catalog = MockTestCatalog()

    private fun question(id: String, correctIndex: Int) = Question(
        id = QuestionId(id),
        topic = LessonId.WHAT_IS_A_PERCENTAGE,
        difficulty = QuestionDifficulty.EASY,
        prompt = LocalizedText.of("Q$id", "Q$id"),
        options = listOf("A", "B", "C", "D").map { LocalizedText.of(it, it) },
        correctIndex = correctIndex,
        explanation = LocalizedText.of("E$id", "E$id")
    )

    private fun liveSession() = MockTestSession.start(
        sessionId = "session-exit",
        config = catalog.tests().first(),
        questions = questions,
        startedAtEpochMillis = 1_000L
    )

    private fun instructionsState() = MockTestExamUiState(
        phase = MockTestPhase.INSTRUCTIONS
    )

    private fun inProgressState(session: MockTestSession = liveSession()) = MockTestExamUiState(
        phase = MockTestPhase.IN_PROGRESS,
        session = session,
        remainingMillis = session.remainingMillis(1_000L)
    )

    // ---- the warning is shown ----------------------------------------------

    @Test
    fun `leaving a live paper warns because the attempt would be lost`() {
        assertTrue(inProgressState().exitWouldDiscardWork)
    }

    @Test
    fun `leaving a live paper warns even before anything is answered`() {
        // No answers yet, but the clock has started and the attempt exists; the
        // reader is still choosing to give up something real.
        assertTrue(inProgressState().exitWouldDiscardWork)
    }

    @Test
    fun `leaving a live paper warns once questions have been answered`() {
        val answered = inProgressState().session!!.selectOption(1)
        val state = inProgressState(answered)
        assertEquals(1, state.session?.answeredCount)
        assertTrue(state.exitWouldDiscardWork)
    }

    @Test
    fun `leaving a live paper warns after a review mark is set`() {
        val marked = inProgressState().session!!.toggleReviewForCurrent()
        assertTrue(inProgressState(marked).exitWouldDiscardWork)
    }

    // ---- the warning is not shown ------------------------------------------

    @Test
    fun `leaving the instructions page does not warn`() {
        assertFalse(instructionsState().exitWouldDiscardWork)
    }

    @Test
    fun `leaving a result does not warn`() {
        val result = MockTestExamUiState(
            phase = MockTestPhase.RESULT,
            session = liveSession().submit(MockTestSubmission.MANUAL)
        )
        assertFalse(result.exitWouldDiscardWork)
    }

    @Test
    fun `leaving the review does not warn`() {
        val review = MockTestExamUiState(
            phase = MockTestPhase.REVIEW,
            session = liveSession().submit(MockTestSubmission.MANUAL)
        )
        assertFalse(review.exitWouldDiscardWork)
    }

    @Test
    fun `a submitted session in the live phase does not warn`() {
        // Defensive: the phase and the submitted flag are separate, and this
        // checks the submitted flag is honoured on its own.
        val state = MockTestExamUiState(
            phase = MockTestPhase.IN_PROGRESS,
            session = liveSession().submit(MockTestSubmission.MANUAL)
        )
        assertFalse(state.exitWouldDiscardWork)
    }

    @Test
    fun `an empty state does not warn`() {
        assertFalse(MockTestExamUiState().exitWouldDiscardWork)
    }

    // ---- the dialog's own state --------------------------------------------

    @Test
    fun `the exit dialog starts closed`() {
        assertFalse(MockTestExamUiState().showExitDialog)
        assertFalse(inProgressState().showExitDialog)
    }

    @Test
    fun `the exit dialog is independent of the submit dialog`() {
        // Opening one must not imply the other, or dismissing an exit would
        // silently cancel a submission prompt instead.
        val state = inProgressState().copy(showSubmitDialog = true)
        assertTrue(state.showSubmitDialog)
        assertFalse(state.showExitDialog)
    }

    @Test
    fun `asking for an exit on a live paper leaves the paper itself untouched`() {
        val before = inProgressState()
        val after = before.copy(showExitDialog = true)

        // Dismissing the warning must return the reader to exactly the paper
        // they had: same session, same question, same clock.
        assertEquals(before.session, after.session)
        assertEquals(before.remainingMillis, after.remainingMillis)
        assertEquals(before.questionNumber, after.questionNumber)
        assertTrue(after.session?.isSubmitted == false)
    }

    @Test
    fun `the live phase reports the counts the warning quotes`() {
        val state = inProgressState()
        assertEquals(0, state.session?.answeredCount)
        assertEquals(questions.size, state.session?.questionCount)
    }
}