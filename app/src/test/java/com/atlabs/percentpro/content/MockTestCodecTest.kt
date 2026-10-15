package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalizedText
import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.model.MockTestSubmission
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockTestCodecTest {

    private val questions = listOf(
        question("q1", correctIndex = 0),
        question("q2", correctIndex = 1),
        question("q3", correctIndex = 2),
        question("q4", correctIndex = 3)
    )

    private fun question(id: String, correctIndex: Int) = Question(
        id = QuestionId(id),
        topic = LessonId.WHAT_IS_A_PERCENTAGE,
        difficulty = QuestionDifficulty.EASY,
        prompt = LocalizedText.of("Q$id", "Q$id"),
        options = listOf("A", "B", "C", "D").map { LocalizedText.of("$it", "$it") },
        correctIndex = correctIndex,
        explanation = LocalizedText.of("E$id", "E$id")
    )

    private val catalog = MockTestCatalog()
    private val lookup: (QuestionId) -> Question? = { id -> questions.firstOrNull { it.id == id } }

    private fun liveSession(startedAt: Long = 1_000L) = MockTestSession.start(
        sessionId = "session-abc",
        config = catalog.tests().first(),
        questions = questions,
        startedAtEpochMillis = startedAt
    )

    private fun decode(raw: String?, now: Long = 1_000L, allowExpired: Boolean = false) =
        MockTestCodec.decodeSession(raw, catalog, lookup, now, allowExpired)

    // ---- active session ----------------------------------------------------

    @Test
    fun `an unfinished paper round trips exactly`() {
        var live = liveSession()
        live = live.goTo(1).selectOption(3)
        live = live.goTo(2).toggleReviewForCurrent()

        val restored = decode(MockTestCodec.encodeSession(live), now = 2_000L)

        val session = (restored as MockTestCodec.SessionRestore.Ready).session
        assertEquals(live.sessionId, session.sessionId)
        assertEquals(live.questions.map { it.id }, session.questions.map { it.id })
        assertEquals(live.currentIndex, session.currentIndex)
        assertEquals(live.deadlineEpochMillis, session.deadlineEpochMillis)
        assertEquals(live.answers, session.answers)
        assertEquals(live.markedForReview, session.markedForReview)
        assertEquals(live.visitedQuestionIds, session.visitedQuestionIds)
        assertNull(session.submission)
    }

    @Test
    fun `the deadline survives a restart so the clock cannot be reset`() {
        val encoded = MockTestCodec.encodeSession(liveSession(startedAt = 5_000L))

        val session = (decode(encoded, now = 400_000L) as MockTestCodec.SessionRestore.Ready).session

        assertEquals(5_000L + 15 * 60 * 1000L, session.deadlineEpochMillis)
        assertEquals(505_000L, session.remainingMillis(400_000L))
    }

    @Test
    fun `an expired paper is refused by default`() {
        val encoded = MockTestCodec.encodeSession(liveSession(startedAt = 0L))

        val restore = decode(encoded, now = 2_000_000L)

        assertEquals(
            MockTestCodec.RestoreFailure.EXPIRED,
            (restore as MockTestCodec.SessionRestore.Unusable).reason
        )
    }

    @Test
    fun `an expired paper can still be read back for scoring`() {
        var live = liveSession(startedAt = 0L)
        live = live.goTo(0).selectOption(0)
        val encoded = MockTestCodec.encodeSession(live)

        val restore = decode(encoded, now = 2_000_000L, allowExpired = true)

        val session = (restore as MockTestCodec.SessionRestore.Ready).session
        assertEquals(1, session.answeredCount)
        assertEquals(1, session.score().correct)
    }

    @Test
    fun `no stored paper is not an error`() {
        assertTrue(decode(null) is MockTestCodec.SessionRestore.None)
        assertTrue(decode("") is MockTestCodec.SessionRestore.None)
        assertTrue(decode("   ") is MockTestCodec.SessionRestore.None)
    }

    @Test
    fun `a truncated record is rejected as corrupt`() {
        val encoded = MockTestCodec.encodeSession(liveSession())
        val truncated = encoded.substringBefore('|')

        assertEquals(
            MockTestCodec.RestoreFailure.CORRUPT,
            (decode(truncated) as MockTestCodec.SessionRestore.Unusable).reason
        )
    }

    @Test
    fun `an unknown version is rejected`() {
        val encoded = MockTestCodec.encodeSession(liveSession())
            .replaceFirst("v1", "v9")

        assertEquals(
            MockTestCodec.RestoreFailure.CORRUPT,
            (decode(encoded) as MockTestCodec.SessionRestore.Unusable).reason
        )
    }

    @Test
    fun `an unknown test id is reported separately from corruption`() {
        val encoded = MockTestCodec.encodeSession(liveSession())
            .replace("percentage_aptitude_15", "some_retired_test")

        assertEquals(
            MockTestCodec.RestoreFailure.UNKNOWN_TEST,
            (decode(encoded) as MockTestCodec.SessionRestore.Unusable).reason
        )
    }

    @Test
    fun `a removed question is reported rather than scored against a gap`() {
        val encoded = MockTestCodec.encodeSession(liveSession())

        val restore = MockTestCodec.decodeSession(
            raw = encoded,
            catalog = catalog,
            questionsById = { id -> if (id.value == "q2") null else lookup(id) },
            nowEpochMillis = 2_000L
        )

        assertEquals(
            MockTestCodec.RestoreFailure.MISSING_QUESTIONS,
            (restore as MockTestCodec.SessionRestore.Unusable).reason
        )
    }

    @Test
    fun `a stored answer pointing outside the options is rejected`() {
        val live = liveSession().goTo(0).selectOption(1)
        val tampered = MockTestCodec.encodeSession(live).replace("q1=1", "q1=99")

        assertEquals(
            MockTestCodec.RestoreFailure.CORRUPT,
            (decode(tampered) as MockTestCodec.SessionRestore.Unusable).reason
        )
    }

    @Test
    fun `a duplicated question id is rejected`() {
        val encoded = MockTestCodec.encodeSession(liveSession()).replace("q2,q3,q4", "q2,q2,q4")

        assertEquals(
            MockTestCodec.RestoreFailure.CORRUPT,
            (decode(encoded) as MockTestCodec.SessionRestore.Unusable).reason
        )
    }

    @Test
    fun `a stored index outside the paper is clamped on restore`() {
        val encoded = MockTestCodec.encodeSession(liveSession())
            .split("|").toMutableList()
            .also { it[5] = "99" }
            .joinToString("|")

        val session = (decode(encoded) as MockTestCodec.SessionRestore.Ready).session

        assertEquals(questions.lastIndex, session.currentIndex)
    }

    // ---- finished results --------------------------------------------------

    @Test
    fun `a result round trips exactly`() {
        var live = liveSession()
        live = live.goTo(0).selectOption(0)
        live = live.goTo(1).selectOption(3)
        live = live.goTo(2).toggleReviewForCurrent()
        val frozen = live.submit(MockTestSubmission.MANUAL)

        val original = com.atlabs.percentpro.model.MockTestResult.from(frozen, 600_000L)
        val restored = MockTestCodec.decodeResults(MockTestCodec.encodeResults(listOf(original)))

        assertEquals(1, restored.size)
        assertEquals(original, restored.first())
    }

    @Test
    fun `the submission reason survives storage`() {
        var live = liveSession().goTo(0).selectOption(0)

        val manual = com.atlabs.percentpro.model.MockTestResult.from(
            live.submit(MockTestSubmission.MANUAL),
            600_000L
        )
        val timedOut = com.atlabs.percentpro.model.MockTestResult.from(
            live.submit(MockTestSubmission.TIMEOUT),
            600_000L
        )

        assertEquals(
            MockTestSubmission.MANUAL,
            MockTestCodec.decodeResults(MockTestCodec.encodeResults(listOf(manual)))
                .single().submission
        )
        assertEquals(
            MockTestSubmission.TIMEOUT,
            MockTestCodec.decodeResults(MockTestCodec.encodeResults(listOf(timedOut)))
                .single().submission
        )
    }

    @Test
    fun `the marking scheme in force is kept with the result`() {
        val live = liveSession().goTo(0).selectOption(0)
        val result = com.atlabs.percentpro.model.MockTestResult.from(
            live.submit(MockTestSubmission.MANUAL),
            600_000L
        )

        val restored = MockTestCodec.decodeResults(MockTestCodec.encodeResults(listOf(result)))
            .single()

        assertEquals(result.marksPerCorrectInQuarters, restored.marksPerCorrectInQuarters)
        assertEquals(result.negativeMarkInQuarters, restored.negativeMarkInQuarters)
        assertEquals(result.scoreInQuarters, restored.scoreInQuarters)
    }

    @Test
    fun `a blank result list decodes to nothing`() {
        assertTrue(MockTestCodec.decodeResults(null).isEmpty())
        assertTrue(MockTestCodec.decodeResults("").isEmpty())
        assertTrue(MockTestCodec.encodeResults(emptyList()).isEmpty())
    }

    @Test
    fun `a damaged result line is skipped instead of crashing the list`() {
        val good = MockTestCodec.encodeResults(
            listOf(
                com.atlabs.percentpro.model.MockTestResult.from(
                    liveSession().submit(MockTestSubmission.MANUAL),
                    600_000L
                )
            )
        )
        val mixed = "garbage\n$good\nalso-garbage"

        val restored = MockTestCodec.decodeResults(mixed)

        assertEquals(1, restored.size)
    }

    @Test
    fun `results come back newest first`() {
        val older = com.atlabs.percentpro.model.MockTestResult.from(
            liveSession().submit(MockTestSubmission.MANUAL),
            100_000L
        )
        val newer = older.copy(sessionId = "second", completedAtEpochMillis = 900_000L)

        val restored = MockTestCodec.decodeResults(
            MockTestCodec.encodeResults(listOf(older, newer))
        )

        assertEquals(listOf("second", older.sessionId), restored.map { it.sessionId })
    }
}