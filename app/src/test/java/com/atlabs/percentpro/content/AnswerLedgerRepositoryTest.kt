package com.atlabs.percentpro.content

import com.atlabs.percentpro.data.AnswerLedgerRepository
import com.atlabs.percentpro.data.GamificationRepository
import com.atlabs.percentpro.model.AnswerEvent
import com.atlabs.percentpro.model.AnswerSource
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.model.MockTestResult
import com.atlabs.percentpro.model.MockTestSubmission
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/**
 * The ledger's write path: idempotency, persistence across recreation,
 * corruption tolerance and the one-time recovery of mock-test history.
 */
class AnswerLedgerRepositoryTest {

    private val clockMillis = 1_772_323_200_000L
    private val zone = TimeZone.getTimeZone("Asia/Kolkata")
    private val questionRepository = QuestionRepository()
    private val topic = LessonId.WHAT_IS_A_PERCENTAGE

    private val bankQuestion = questionRepository.questionsFor(topic).first()

    private fun repository(
        store: InMemoryGamificationStore = InMemoryGamificationStore(),
        marker: InMemoryBaselineMarker = InMemoryBaselineMarker()
    ) = AnswerLedgerRepository(
        store = store,
        marker = marker,
        questionRepository = questionRepository,
        now = { clockMillis },
        timeZone = { zone }
    )

    private fun event(
        id: String,
        correct: Boolean,
        session: String = "run-1",
        source: AnswerSource = AnswerSource.TOPIC_PRACTICE
    ) = AnswerEvent(
        questionId = QuestionId(id),
        topicKey = topic.storageKey,
        difficulty = QuestionDifficulty.EASY,
        wasCorrect = correct,
        source = source,
        sessionId = session,
        answeredAtEpochMillis = clockMillis,
        answeredOn = LocalDateKey.of(2026, 3, 1)
    )

    // ---- recording ----------------------------------------------------------

    @Test
    fun `an answer is recorded once`() = runBlocking {
        val ledger = repository()

        assertTrue(ledger.record(event("q1", true)))
        assertEquals(1, ledger.currentEvents().size)
    }

    @Test
    fun `recording the same answer twice stores it once`() = runBlocking {
        val ledger = repository()

        ledger.record(event("q1", true))
        val second = ledger.record(event("q1", true))

        assertFalse("the duplicate should report that it was ignored", second)
        assertEquals(1, ledger.currentEvents().size)
    }

    @Test
    fun `repeated callbacks for one answer cannot inflate a count`() = runBlocking {
        val ledger = repository()

        repeat(25) { ledger.record(event("q1", true)) }

        val analytics = LearningAnalyticsEngine.analyse(ledger.currentEvents())
        assertEquals(1, analytics.overallAttempted)
        assertEquals(100, analytics.overallAccuracyPercent)
    }

    @Test
    fun `the same question in a different session is a separate attempt`() = runBlocking {
        val ledger = repository()

        ledger.record(event("q1", true, session = "run-1"))
        ledger.record(event("q1", true, session = "run-2"))

        assertEquals(2, ledger.currentEvents().size)
    }

    @Test
    fun `practice and mock test attempts on one question are both kept`() = runBlocking {
        val ledger = repository()

        ledger.record(event("q1", true, session = "run-1"))
        ledger.record(
            event("q1", false, session = "paper-1", source = AnswerSource.MOCK_TEST)
        )

        val events = ledger.currentEvents()
        assertEquals(2, events.size)
        assertEquals(1, events.count { it.source == AnswerSource.TOPIC_PRACTICE })
        assertEquals(1, events.count { it.source == AnswerSource.MOCK_TEST })
    }

    @Test
    fun `an unknown question is not recorded`() = runBlocking {
        val ledger = repository()

        val stored = ledger.recordAnswer(
            questionId = QuestionId("a_question_that_does_not_exist"),
            topic = topic,
            source = AnswerSource.TOPIC_PRACTICE,
            sessionId = "run-1",
            wasCorrect = true
        )

        assertFalse(stored)
        assertTrue(ledger.currentEvents().isEmpty())
    }

    // ---- persistence across recreation -------------------------------------

    @Test
    fun `analytics survive a new repository over the same storage`() = runBlocking {
        val store = InMemoryGamificationStore()
        val marker = InMemoryBaselineMarker()

        val first = repository(store, marker)
        repeat(5) { index -> first.record(event("q$index", index % 2 == 0)) }
        val before = LearningAnalyticsEngine.analyse(first.currentEvents())

        // Simulates the process being recreated.
        val recreated = repository(store, marker)
        val after = LearningAnalyticsEngine.analyse(recreated.currentEvents())

        assertEquals(before.overallAttempted, after.overallAttempted)
        assertEquals(before.overallCorrect, after.overallCorrect)
        assertEquals(before.overallAccuracyPercent, after.overallAccuracyPercent)
    }

    @Test
    fun `a duplicate is still refused after recreation`() = runBlocking {
        val store = InMemoryGamificationStore()
        val marker = InMemoryBaselineMarker()
        repository(store, marker).record(event("q1", true))

        val secondWrite = repository(store, marker).record(event("q1", true))

        assertFalse(secondWrite)
        assertEquals(1, repository(store, marker).currentEvents().size)
    }

    @Test
    fun `clearing removes the ledger and the backfill marker`() = runBlocking {
        val store = InMemoryGamificationStore()
        val marker = InMemoryBaselineMarker()
        val ledger = repository(store, marker)
        ledger.record(event("q1", true))
        ledger.backfillFromMockTests(emptyList())

        ledger.clearAll()

        assertTrue(ledger.currentEvents().isEmpty())
        assertFalse(runBlocking { marker.isSeeded() })
    }

    // ---- corruption ---------------------------------------------------------

    @Test
    fun `corrupted storage yields an empty ledger rather than an exception`() = runBlocking {
        val store = InMemoryGamificationStore()
        store.corruptWith("not a ledger at all\n||||||||\n|||||||")

        val events = repository(store).currentEvents()

        assertTrue(events.isEmpty())
    }

    @Test
    fun `one damaged line does not discard the readable ones`() = runBlocking {
        val store = InMemoryGamificationStore()
        val good = AnswerEventCodec.encode(listOf(event("q1", true)))
        store.corruptWith("broken\n$good")

        val events = repository(store).currentEvents()

        assertEquals(1, events.size)
        assertEquals(QuestionId("q1"), events.first().questionId)
    }

    @Test
    fun `a duplicated line in storage is dropped on read`() = runBlocking {
        val store = InMemoryGamificationStore()
        val line = AnswerEventCodec.encode(listOf(event("q1", true)))
        store.corruptWith("$line\n$line")

        assertEquals(1, repository(store).currentEvents().size)
    }

    @Test
    fun `an unknown difficulty in storage is skipped`() = runBlocking {
        val store = InMemoryGamificationStore()
        val line = AnswerEventCodec.encode(listOf(event("q1", true)))
            .replace("EASY", "IMPOSSIBLE")
        store.corruptWith(line)

        assertTrue(repository(store).currentEvents().isEmpty())
    }

    @Test
    fun `an unknown source in storage is skipped`() = runBlocking {
        val store = InMemoryGamificationStore()
        val line = AnswerEventCodec.encode(listOf(event("q1", true)))
            .replace("|practice|", "|telepathy|")
        store.corruptWith(line)

        assertTrue(repository(store).currentEvents().isEmpty())
    }

    // ---- codec -------------------------------------------------------------

    @Test
    fun `the codec round trips an event`() {
        val original = event("q1", true)
        assertEquals(original, AnswerEventCodec.decode(AnswerEventCodec.encode(listOf(original))).single())
    }

    @Test
    fun `null blank and empty input decode to no events`() {
        listOf(null, "", "   ", "\n\n").forEach {
            assertTrue(AnswerEventCodec.decode(it).isEmpty())
        }
    }

    @Test
    fun `the codec keeps only the most recent events when over the cap`() {
        val events = (0 until AnswerEventCodec.MAX_STORED_EVENTS + 50).map {
            event("q$it", true, session = "s$it")
        }

        val decoded = AnswerEventCodec.decode(AnswerEventCodec.encode(events))

        assertEquals(AnswerEventCodec.MAX_STORED_EVENTS, decoded.size)
        assertEquals(QuestionId("q${events.size - 1}"), decoded.last().questionId)
    }

    @Test
    fun `the ledger never grows past the cap`() = runBlocking {
        val ledger = repository()
        repeat(AnswerEventCodec.MAX_STORED_EVENTS + 20) {
            ledger.record(event("q$it", true, session = "s$it"))
        }

        assertTrue(ledger.currentEvents().size <= AnswerEventCodec.MAX_STORED_EVENTS)
    }

    // ---- backfill ----------------------------------------------------------

    @Test
    fun `mock test history is recovered exactly`() = runBlocking {
        val question = questionRepository.questionsFor(topic).first()
        val wrongOption = (question.correctIndex + 1) % question.options.size
        val result = mockResult(
            sessionId = "paper-1",
            correctId = question.id,
            correctIndex = question.correctIndex,
            wrongId = questionRepository.questionsFor(topic)[1].id,
            wrongIndex = (questionRepository.questionsFor(topic)[1].correctIndex + 1) % 4,
            extra = question to wrongOption
        )

        val ledger = repository()
        ledger.backfillFromMockTests(listOf(result))

        val events = ledger.currentEvents()
        assertEquals(2, events.size)
        assertEquals(1, events.count { it.wasCorrect })
        assertEquals(1, events.count { !it.wasCorrect })
        assertTrue(events.all { it.source == AnswerSource.MOCK_TEST })
    }

    @Test
    fun `unanswered questions in a mock test are not recovered`() = runBlocking {
        val question = questionRepository.questionsFor(topic).first()
        val result = MockTestResult(
            sessionId = "paper-2",
            testId = MockTestId("percentage_aptitude_15"),
            testTitleRes = 1,
            startedAtEpochMillis = clockMillis,
            completedAtEpochMillis = clockMillis,
            records = listOf(
                com.atlabs.percentpro.model.MockTestQuestionRecord(
                    questionId = question.id,
                    order = 0,
                    selectedOptionIndex = question.correctIndex,
                    markedForReview = false
                ),
                com.atlabs.percentpro.model.MockTestQuestionRecord(
                    questionId = questionRepository.questionsFor(topic)[1].id,
                    order = 1,
                    // Never answered.
                    selectedOptionIndex = null,
                    markedForReview = false
                )
            ),
            totalQuestions = 2,
            correct = 1,
            incorrect = 0,
            unanswered = 1,
            markedForReview = 0,
            scoreInTwelfths = 12,
            maxMarksInTwelfths = 24,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 3,
            timeTakenMillis = 600_000L,
            submission = MockTestSubmission.MANUAL
        )

        val ledger = repository()
        ledger.backfillFromMockTests(listOf(result))

        assertEquals(1, ledger.currentEvents().size)
    }

    @Test
    fun `a practice answer and a mock answer of one question stay separate`() = runBlocking {
        val question = questionRepository.questionsFor(topic).first()
        val store = InMemoryGamificationStore()
        val marker = InMemoryBaselineMarker()
        val ledger = repository(store, marker)

        ledger.record(event(question.id.value, correct = false, session = "practice-run"))

        val result = MockTestResult(
            sessionId = "mock-run",
            testId = MockTestId("percentage_aptitude_15"),
            testTitleRes = 1,
            startedAtEpochMillis = clockMillis,
            completedAtEpochMillis = clockMillis,
            records = listOf(
                com.atlabs.percentpro.model.MockTestQuestionRecord(
                    questionId = question.id,
                    order = 0,
                    selectedOptionIndex = question.correctIndex,
                    markedForReview = false
                )
            ),
            totalQuestions = 1,
            correct = 1,
            incorrect = 0,
            unanswered = 0,
            markedForReview = 0,
            scoreInTwelfths = 12,
            maxMarksInTwelfths = 12,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 3,
            timeTakenMillis = 60_000L,
            submission = MockTestSubmission.MANUAL
        )
        ledger.backfillFromMockTests(listOf(result))

        val events = ledger.currentEvents()
        assertEquals("two genuine attempts of one question", 2, events.size)
        assertEquals(1, events.count { it.source == AnswerSource.TOPIC_PRACTICE })
        assertEquals(1, events.count { it.source == AnswerSource.MOCK_TEST })
    }

    @Test
    fun `backfill runs only once`() = runBlocking {
        val question = questionRepository.questionsFor(topic).first()
        val result = mockResult(
            sessionId = "paper-3",
            correctId = question.id,
            correctIndex = question.correctIndex,
            wrongId = questionRepository.questionsFor(topic)[1].id,
            wrongIndex = 0,
            extra = null
        )

        val store = InMemoryGamificationStore()
        val marker = InMemoryBaselineMarker()
        val ledger = repository(store, marker)
        ledger.backfillFromMockTests(listOf(result))
        ledger.backfillFromMockTests(listOf(result))

        assertEquals(2, ledger.currentEvents().size)
    }

    @Test
    fun `backfill never overwrites an answer captured live`() = runBlocking {
        val question = questionRepository.questionsFor(topic).first()
        val store = InMemoryGamificationStore()
        val marker = InMemoryBaselineMarker()
        val ledger = repository(store, marker)

        // A live mock-test answer says wrong; the backfill claims the same
        // session and question was right. Same attempt key, so one must win.
        ledger.record(
            event(
                question.id.value,
                correct = false,
                session = "paper-live",
                source = AnswerSource.MOCK_TEST
            )
        )

        val result = MockTestResult(
            sessionId = "paper-live",
            testId = MockTestId("percentage_aptitude_15"),
            testTitleRes = 1,
            startedAtEpochMillis = clockMillis,
            completedAtEpochMillis = clockMillis,
            records = listOf(
                com.atlabs.percentpro.model.MockTestQuestionRecord(
                    questionId = question.id,
                    order = 0,
                    selectedOptionIndex = question.correctIndex,
                    markedForReview = false
                )
            ),
            totalQuestions = 1,
            correct = 1,
            incorrect = 0,
            unanswered = 0,
            markedForReview = 0,
            scoreInTwelfths = 12,
            maxMarksInTwelfths = 12,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 3,
            timeTakenMillis = 60_000L,
            submission = MockTestSubmission.MANUAL
        )
        ledger.backfillFromMockTests(listOf(result))

        val events = ledger.currentEvents()
        assertEquals(1, events.size)
        assertFalse("the live answer must win over the backfilled copy", events.first().wasCorrect)
    }

    @Test
    fun `a mock test question that has left the bank is skipped`() = runBlocking {
        val result = MockTestResult(
            sessionId = "paper-4",
            testId = MockTestId("percentage_aptitude_15"),
            testTitleRes = 1,
            startedAtEpochMillis = clockMillis,
            completedAtEpochMillis = clockMillis,
            records = listOf(
                com.atlabs.percentpro.model.MockTestQuestionRecord(
                    questionId = QuestionId("removed_question"),
                    order = 0,
                    selectedOptionIndex = 0,
                    markedForReview = false
                )
            ),
            totalQuestions = 1,
            correct = 1,
            incorrect = 0,
            unanswered = 0,
            markedForReview = 0,
            scoreInTwelfths = 12,
            maxMarksInTwelfths = 12,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 3,
            timeTakenMillis = 60_000L,
            submission = MockTestSubmission.MANUAL
        )

        val ledger = repository()
        ledger.backfillFromMockTests(listOf(result))

        assertTrue(ledger.currentEvents().isEmpty())
    }

    @Test
    fun `backfill never awards XP or touches gamification state`() = runBlocking {
        val question = questionRepository.questionsFor(topic).first()
        val result = mockResult(
            sessionId = "paper-5",
            correctId = question.id,
            correctIndex = question.correctIndex,
            wrongId = questionRepository.questionsFor(topic)[1].id,
            wrongIndex = 0,
            extra = null
        )

        val ledger = repository()
        val before = GamificationRepository(
            InMemoryGamificationStore(),
            InMemoryBaselineMarker(),
            { clockMillis },
            { zone }
        ).current()

        ledger.backfillFromMockTests(listOf(result))

        val after = GamificationRepository(
            InMemoryGamificationStore(),
            InMemoryBaselineMarker(),
            { clockMillis },
            { zone }
        ).current()

        assertEquals(before.totalXp, after.totalXp)
        assertEquals(before.rewardedActivityIds, after.rewardedActivityIds)
        assertEquals(before.unlockedAchievements, after.unlockedAchievements)
    }

    private fun mockResult(
        sessionId: String,
        correctId: QuestionId,
        correctIndex: Int,
        wrongId: QuestionId,
        wrongIndex: Int,
        extra: Pair<com.atlabs.percentpro.model.Question, Int>?
    ) = MockTestResult(
        sessionId = sessionId,
        testId = MockTestId("percentage_aptitude_15"),
        testTitleRes = 1,
        startedAtEpochMillis = clockMillis,
        completedAtEpochMillis = clockMillis,
        records = buildList {
            add(
                com.atlabs.percentpro.model.MockTestQuestionRecord(correctId, 0, correctIndex, false)
            )
            add(
                com.atlabs.percentpro.model.MockTestQuestionRecord(wrongId, 1, wrongIndex, false)
            )
            extra?.let { (question, index) ->
                add(
                    com.atlabs.percentpro.model.MockTestQuestionRecord(
                        question.id,
                        2,
                        index,
                        false
                    )
                )
            }
        },
        totalQuestions = 2 + if (extra == null) 0 else 1,
        correct = 1,
        incorrect = 1,
        unanswered = 0,
        markedForReview = 0,
        scoreInTwelfths = 9,
        maxMarksInTwelfths = (2 + if (extra == null) 0 else 1) * 12,
        marksPerCorrectInTwelfths = 12,
        negativeMarkInTwelfths = 3,
        timeTakenMillis = 600_000L,
        submission = MockTestSubmission.MANUAL
    )
}