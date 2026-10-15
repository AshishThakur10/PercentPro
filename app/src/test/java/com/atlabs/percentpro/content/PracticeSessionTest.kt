package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalizedText
import com.atlabs.percentpro.model.PracticeResult
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionTest {

    private val topic = LessonId.WHAT_IS_A_PERCENTAGE

    private fun question(
        id: String,
        correctIndex: Int,
        options: List<String> = listOf("A", "B", "C", "D")
    ) = Question(
        id = QuestionId(id),
        topic = topic,
        difficulty = QuestionDifficulty.EASY,
        prompt = LocalizedText.of("Question $id", "प्रश्न $id"),
        options = options.map { LocalizedText.of(it, "विकल्प $it") },
        correctIndex = correctIndex,
        explanation = LocalizedText.of("Why $id", "$id क्यों")
    )

    private fun sessionOf(vararg questions: Question) = PracticeSession(topic, questions.toList())

    private val threeQuestions = listOf(
        question("q1", correctIndex = 1),
        question("q2", correctIndex = 0),
        question("q3", correctIndex = 2)
    )

    // ---- answer evaluation -------------------------------------------------

    @Test
    fun `a correct submission is recorded as correct`() {
        val session = sessionOf(*threeQuestions.toTypedArray()).submit(1)
        assertTrue(session.isCorrect(QuestionId("q1")))
        assertEquals(1, session.correctCount)
        assertEquals(0, session.incorrectCount)
    }

    @Test
    fun `an incorrect submission is recorded as incorrect`() {
        val session = sessionOf(*threeQuestions.toTypedArray()).submit(0)
        assertFalse(session.isCorrect(QuestionId("q1")))
        assertEquals(0, session.correctCount)
        assertEquals(1, session.incorrectCount)
        assertEquals(1, session.answeredCount)
    }

    // ---- duplicate submissions ---------------------------------------------

    @Test
    fun `resubmitting the same question changes nothing`() {
        val first = sessionOf(*threeQuestions.toTypedArray()).submit(1)
        val second = first.submit(0)
        assertEquals(first.answers, second.answers)
        assertEquals(1, second.correctCount)
    }

    @Test
    fun `resubmitting cannot award duplicate credit`() {
        var session = sessionOf(*threeQuestions.toTypedArray())
        repeat(5) { session = session.submit(1) }
        assertEquals(1, session.answeredCount)
        assertEquals(1, session.correctCount)
        assertEquals(0, session.incorrectCount)
    }

    @Test
    fun `an out of range option is ignored`() {
        val session = sessionOf(*threeQuestions.toTypedArray()).submit(9)
        assertEquals(0, session.answeredCount)
    }

    @Test
    fun `a negative option index is ignored`() {
        val session = sessionOf(*threeQuestions.toTypedArray()).submit(-1)
        assertEquals(0, session.answeredCount)
    }

    // ---- navigation preserves answers --------------------------------------

    @Test
    fun `answers survive moving between questions`() {
        var session = sessionOf(*threeQuestions.toTypedArray())
        session = session.submit(1)          // q1 correct
        session = session.next()
        assertEquals(1, session.currentIndex)

        session = session.submit(2)          // q2 wrong
        session = session.next()
        session = session.submit(2)          // q3 correct
        session = session.previous()
        session = session.previous()

        assertEquals(0, session.currentIndex)
        assertEquals(QuestionId("q1"), session.currentQuestion?.id)
        assertEquals(3, session.answeredCount)
        assertEquals(2, session.correctCount)
        assertEquals(1, session.incorrectCount)
    }

    @Test
    fun `going back to an answered question keeps its submitted answer`() {
        var session = sessionOf(*threeQuestions.toTypedArray())
        session = session.submit(1)
        session = session.next()
        session = session.submit(0)
        session = session.previous()

        assertEquals(0, session.currentIndex)
        assertEquals(1, session.submittedIndexFor(QuestionId("q1")))
        assertTrue(session.isAnswered(QuestionId("q1")))
    }

    @Test
    fun `next clamps at the last question and previous at the first`() {
        var session = sessionOf(*threeQuestions.toTypedArray())
        session = session.previous()
        assertEquals(0, session.currentIndex)
        repeat(10) { session = session.next() }
        assertEquals(2, session.currentIndex)
        assertTrue(session.isLastQuestion)
    }

    @Test
    fun `goTo clamps to the available range`() {
        val session = sessionOf(*threeQuestions.toTypedArray())
        assertEquals(2, session.goTo(99).currentIndex)
        assertEquals(0, session.goTo(-5).currentIndex)
    }

    // ---- unanswered and scoring --------------------------------------------

    @Test
    fun `an untouched session has every question unanswered`() {
        val session = sessionOf(*threeQuestions.toTypedArray())
        assertEquals(3, session.questionCount)
        assertEquals(0, session.answeredCount)
        assertEquals(3, session.unansweredCount)
        assertEquals(0, session.accuracyPercent)
    }

    @Test
    fun `unanswered questions count against the score`() {
        var session = sessionOf(*threeQuestions.toTypedArray())
        session = session.submit(1)          // q1 correct
        session = session.next().submit(3)  // q2 wrong, q3 left alone
        assertEquals(1, session.correctCount)
        assertEquals(1, session.incorrectCount)
        assertEquals(1, session.unansweredCount)
        // 1 correct out of 3 asked, not out of 2 answered.
        assertEquals(33, session.accuracyPercent)
    }

    @Test
    fun `accuracy is integer and rounded down`() {
        val questions = List(3) { question("q$it", 0) }
        var session = sessionOf(*questions.toTypedArray())
        session = session.submit(0)
        session = session.next()
        session = session.submit(1) // wrong
        session = session.next()
        session = session.submit(1) // wrong
        assertEquals(33, session.accuracyPercent)
    }

    @Test
    fun `a perfect session scores one hundred`() {
        var session = sessionOf(*threeQuestions.toTypedArray())
        session = session.submit(1)
        session = session.next().submit(0)
        session = session.next().submit(2)
        assertEquals(100, session.accuracyPercent)
        assertEquals(0, session.unansweredCount)
    }

    @Test
    fun `an empty session reports zero rather than dividing by zero`() {
        val session = PracticeSession.empty(topic)
        assertTrue(session.isEmpty)
        assertEquals(0, session.questionCount)
        assertEquals(0, session.questionNumber)
        assertEquals(0, session.accuracyPercent)
        assertEquals(0f, session.progress, 0f)
        assertEquals(session, session.next())
        assertEquals(session, session.previous())
        assertEquals(session, session.submit(0))
    }

    @Test
    fun `progress runs from zero to one`() {
        val session = sessionOf(*threeQuestions.toTypedArray())
        assertEquals(0f, session.progress, 0.0001f)
        val afterOne = session.submit(1)
        assertEquals(1f / 3f, afterOne.progress, 0.0001f)
    }

    @Test
    fun `an unknown question id is never correct or answered`() {
        val session = sessionOf(*threeQuestions.toTypedArray()).submit(1)
        assertFalse(session.isAnswered(QuestionId("nope")))
        assertFalse(session.isCorrect(QuestionId("nope")))
        assertEquals(null, session.submittedIndexFor(QuestionId("nope")))
    }

    // ---- conversion to a stored result -------------------------------------

    @Test
    fun `a finished session converts to a result that adds up`() {
        var session = sessionOf(*threeQuestions.toTypedArray())
        session = session.submit(1)
        session = session.next().submit(1)
        session = session.next().submit(2)

        val result = session.toResult(1_700_000_000_000L)
        assertEquals(topic, result.topic)
        assertEquals(3, result.total)
        assertEquals(2, result.correct)
        assertEquals(1, result.incorrect)
        assertEquals(0, result.unanswered)
        assertEquals(66, result.accuracyPercent)
        assertTrue(result.isValid)
    }

    @Test
    fun `an empty session converts to an empty but valid result`() {
        val result = PracticeSession.empty(topic).toResult(1_700_000_000_000L)
        assertEquals(0, result.total)
        assertEquals(0, result.accuracyPercent)
        assertTrue(result.isValid)
    }

    // ---- language selection -------------------------------------------------

    @Test
    fun `question content switches between English and Hindi`() {
        val q = question("q1", 1, options = listOf("one", "two", "three", "four"))
        assertEquals("Question q1", q.prompt.forLanguage(AppLanguage.ENGLISH))
        assertEquals("प्रश्न q1", q.prompt.forLanguage(AppLanguage.HINDI))
        assertEquals("two", q.optionAt(1, AppLanguage.ENGLISH))
        assertEquals("विकल्प two", q.optionAt(1, AppLanguage.HINDI))
        assertEquals("two", q.correctOption(AppLanguage.ENGLISH))
        assertEquals("विकल्प two", q.correctOption(AppLanguage.HINDI))
        assertEquals("Why q1", q.explanation.forLanguage(AppLanguage.ENGLISH))
        assertEquals("q1 क्यों", q.explanation.forLanguage(AppLanguage.HINDI))
    }

    @Test
    fun `asking for a missing option returns null instead of crashing`() {
        val q = question("q1", 0)
        assertEquals(null, q.optionAt(7, AppLanguage.ENGLISH))
        assertEquals(null, q.optionAt(-1, AppLanguage.HINDI))
    }

    @Test
    fun `option count and answer key are validated`() {
        assertTrue(question("q", 0).isValid)
        assertFalse(question("q", 0, options = listOf("A", "B")).isValid)
        assertFalse(question("q", 9).isValid)
        assertFalse(question("q", -1).isValid)
        assertFalse(question("q", 0, options = listOf("A", "A", "C", "D")).isValid)
    }

    // ---- result model -------------------------------------------------------

    @Test
    fun `accuracy is computed with integer arithmetic`() {
        assertEquals(0, PracticeResult.accuracyOf(0, 0))
        assertEquals(0, PracticeResult.accuracyOf(5, 0))
        assertEquals(0, PracticeResult.accuracyOf(0, 5))
        assertEquals(100, PracticeResult.accuracyOf(5, 5))
        assertEquals(50, PracticeResult.accuracyOf(1, 2))
        assertEquals(33, PracticeResult.accuracyOf(1, 3))
        assertEquals(66, PracticeResult.accuracyOf(2, 3))
    }

    @Test
    fun `very large counts do not overflow`() {
        val large = Int.MAX_VALUE / 100
        val accuracy = PracticeResult.accuracyOf(large, Int.MAX_VALUE)
        assertTrue(accuracy in 0..100)
    }

    @Test
    fun `a result whose counts do not add up is invalid`() {
        val bad = PracticeResult(
            topic = topic,
            completedAtEpochMillis = 1L,
            total = 5,
            correct = 2,
            incorrect = 2,
            unanswered = 2,
            accuracyPercent = 40
        )
        assertFalse(bad.isValid)
    }

    @Test
    fun `a result with a mismatched accuracy is invalid`() {
        val bad = PracticeResult(
            topic = topic,
            completedAtEpochMillis = 1L,
            total = 5,
            correct = 2,
            incorrect = 3,
            unanswered = 0,
            accuracyPercent = 99
        )
        assertFalse(bad.isValid)
    }

    @Test
    fun `the result factory clamps nonsensical counts`() {
        val result = PracticeResult.of(
            topic = topic,
            completedAtEpochMillis = -5L,
            total = 4,
            correct = 99,
            incorrect = 99,
            unanswered = 99
        )
        assertEquals(0L, result.completedAtEpochMillis)
        assertTrue(result.correct in 0..4)
        assertTrue(result.incorrect in 0..4)
        assertTrue(result.unanswered in 0..4)
        assertTrue(result.accuracyPercent in 0..100)
    }
}