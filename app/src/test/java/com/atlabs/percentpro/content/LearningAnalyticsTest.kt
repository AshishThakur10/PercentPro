package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.AnswerEvent
import com.atlabs.percentpro.model.AnswerSource
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Accuracy, difficulty splits, classification and the recent window. */
class LearningAnalyticsTest {

    private val today = LocalDateKey.of(2026, 4, 1)

    private fun event(
        questionId: String,
        correct: Boolean,
        topic: LessonId = LessonId.WHAT_IS_A_PERCENTAGE,
        difficulty: QuestionDifficulty = QuestionDifficulty.EASY,
        session: String = "s1",
        at: Long = 1_000L
    ) = AnswerEvent(
        questionId = QuestionId(questionId),
        topicKey = topic.storageKey,
        difficulty = difficulty,
        wasCorrect = correct,
        source = AnswerSource.TOPIC_PRACTICE,
        sessionId = session,
        answeredAtEpochMillis = at,
        answeredOn = today
    )

    private fun series(
        prefix: String,
        correct: Int,
        incorrect: Int,
        topic: LessonId = LessonId.WHAT_IS_A_PERCENTAGE,
        difficulty: QuestionDifficulty = QuestionDifficulty.EASY,
        session: String = "s1"
    ): List<AnswerEvent> =
        (0 until correct).map { event("${prefix}_c$it", true, topic, difficulty, session) } +
            (0 until incorrect).map { event("${prefix}_w$it", false, topic, difficulty, session) }

    // ---- accuracy -----------------------------------------------------------

    @Test
    fun `accuracy is correct divided by attempted`() {
        val analytics = LearningAnalyticsEngine.analyse(series("a", correct = 3, incorrect = 1))

        assertEquals(4, analytics.overallAttempted)
        assertEquals(3, analytics.overallCorrect)
        assertEquals(75, analytics.overallAccuracyPercent)
    }

    @Test
    fun `an empty history reports zeroes rather than throwing`() {
        val analytics = LearningAnalyticsEngine.analyse(emptyList())

        assertEquals(0, analytics.overallAttempted)
        assertEquals(0, analytics.overallAccuracyPercent)
        assertFalse(analytics.hasAnyActivity)
        assertNull(analytics.strongestTopic)
        assertNull(analytics.weakestTopic)
    }

    @Test
    fun `overall accuracy matches the sum of the topics`() {
        val events = series("a", 3, 1) +
            series("b", 2, 2, topic = LessonId.PERCENTAGES_VISUALLY)
        val analytics = LearningAnalyticsEngine.analyse(events)

        assertEquals(5, analytics.overallCorrect)
        assertEquals(8, analytics.overallAttempted)
        assertEquals(62, analytics.overallAccuracyPercent)
        assertEquals(
            analytics.topics.sumOf { it.attempted },
            analytics.overallAttempted
        )
    }

    // ---- excluding non-attempts -------------------------------------------

    @Test
    fun `unanswered questions never reach the ledger so they cannot count`() {
        // Only a submitted answer produces an event at all. A viewed or skipped
        // question writes nothing, which is what excludes it from accuracy.
        val events = series("a", correct = 2, incorrect = 0)
        val analytics = LearningAnalyticsEngine.analyse(events)

        assertEquals(2, analytics.overallAttempted)
        assertEquals(100, analytics.overallAccuracyPercent)
    }

    @Test
    fun `a topic with no attempts is absent rather than counted as zero percent`() {
        val events = series("a", 2, 0)
        val analytics = LearningAnalyticsEngine.analyse(events)

        assertEquals(1, analytics.topics.size)
        assertNull(analytics.topic(LessonId.PERCENTAGES_VISUALLY))
    }

    @Test
    fun `filtering by source keeps practice and mock test apart`() {
        val practice = series("p", 3, 1)
        val mocks = series("m", 1, 3, session = "mock1").map {
            it.copy(source = AnswerSource.MOCK_TEST)
        }

        assertEquals(8, LearningAnalyticsEngine.analyse(practice + mocks).overallAttempted)
        assertEquals(
            4,
            LearningAnalyticsEngine.analyse(practice + mocks, source = AnswerSource.TOPIC_PRACTICE)
                .overallAttempted
        )
        assertEquals(
            4,
            LearningAnalyticsEngine.analyse(practice + mocks, source = AnswerSource.MOCK_TEST)
                .overallAttempted
        )
    }

    // ---- duplicate prevention ----------------------------------------------

    @Test
    fun `duplicate attempt records are counted once`() {
        val events = series("a", 2, 0)
        val doubled = events + events

        val analytics = LearningAnalyticsEngine.analyse(doubled)

        assertEquals(2, analytics.overallAttempted)
        assertEquals(2, analytics.overallCorrect)
    }

    @Test
    fun `a duplicate inside a topic does not inflate its accuracy`() {
        val events = series("a", correct = 1, incorrect = 1)
        val analytics = LearningAnalyticsEngine.analyse(events + events)

        val topic = analytics.topic(LessonId.WHAT_IS_A_PERCENTAGE)!!
        assertEquals(2, topic.attempted)
        assertEquals(50, topic.accuracyPercent)
    }

    @Test
    fun `the same question in two different sessions counts twice`() {
        // Two genuine attempts at one question are real history; only the same
        // attempt repeated is a duplicate.
        val first = series("a", 1, 0, session = "run1")
        val second = series("a", 1, 0, session = "run2")

        assertEquals(2, LearningAnalyticsEngine.analyse(first + second).overallAttempted)
    }

    // ---- classification thresholds -----------------------------------------

    @Test
    fun `fewer than five attempts is never classified as weak`() {
        val analytics = LearningAnalyticsEngine.analyse(series("a", correct = 0, incorrect = 4))

        val topic = analytics.topic(LessonId.WHAT_IS_A_PERCENTAGE)!!
        assertEquals(TopicStatus.NOT_ENOUGH_DATA, topic.status)
        assertFalse(topic.hasEnoughData)
        assertTrue(analytics.weakTopics.isEmpty())
        assertNull(analytics.weakestTopic)
    }

    @Test
    fun `at five attempts and low accuracy it needs practice`() {
        val analytics = LearningAnalyticsEngine.analyse(series("a", correct = 2, incorrect = 3))

        val topic = analytics.topic(LessonId.WHAT_IS_A_PERCENTAGE)!!
        assertTrue(topic.hasEnoughData)
        assertEquals(40, topic.accuracyPercent)
        assertEquals(TopicStatus.NEEDS_PRACTICE, topic.status)
        assertEquals(1, analytics.weakTopics.size)
    }

    @Test
    fun `the needs-practice boundary is exclusive`() {
        // 59% is below 60 and must be needs practice.
        val below = LearningAnalyticsEngine.analyse(series("a", correct = 19, incorrect = 33))
        assertEquals(36, below.topic(LessonId.WHAT_IS_A_PERCENTAGE)!!.accuracyPercent)
        assertEquals(TopicStatus.NEEDS_PRACTICE, below.topic(LessonId.WHAT_IS_A_PERCENTAGE)!!.status)
    }

    @Test
    fun `accuracy from sixty up to below eighty is improving`() {
        val analytics = LearningAnalyticsEngine.analyse(series("a", correct = 3, incorrect = 2))

        val topic = analytics.topic(LessonId.WHAT_IS_A_PERCENTAGE)!!
        assertEquals(60, topic.accuracyPercent)
        assertEquals(TopicStatus.IMPROVING, topic.status)
    }

    @Test
    fun `the improving band boundary is exclusive`() {
        val analytics = LearningAnalyticsEngine.analyse(series("a", correct = 79, incorrect = 21))
        assertEquals(79, analytics.topic(LessonId.WHAT_IS_A_PERCENTAGE)!!.accuracyPercent)
        assertEquals(TopicStatus.IMPROVING, analytics.topic(LessonId.WHAT_IS_A_PERCENTAGE)!!.status)
    }

    @Test
    fun `eighty percent or above is strong`() {
        val analytics = LearningAnalyticsEngine.analyse(series("a", correct = 4, incorrect = 1))

        val topic = analytics.topic(LessonId.WHAT_IS_A_PERCENTAGE)!!
        assertEquals(80, topic.accuracyPercent)
        assertEquals(TopicStatus.STRONG, topic.status)
    }

    @Test
    fun `thresholds all come from one place`() {
        val five = LearningAnalyticsEngine.analyse(series("a", correct = 4, incorrect = 1))
        val four = LearningAnalyticsEngine.analyse(series("a", correct = 3, incorrect = 1))

        assertEquals(
            TopicStatus.STRONG,
            five.topic(LessonId.WHAT_IS_A_PERCENTAGE)!!.status
        )
        assertEquals(
            TopicStatus.NOT_ENOUGH_DATA,
            four.topic(LessonId.WHAT_IS_A_PERCENTAGE)!!.status
        )
        assertEquals(5, InsightThresholds.MIN_ATTEMPTS_FOR_CLASSIFICATION)
    }

    // ---- strongest and weakest ---------------------------------------------

    @Test
    fun `strongest topic is the highest scoring one with enough data`() {
        val events = series("a", correct = 4, incorrect = 1) +
            series("b", correct = 1, incorrect = 4, topic = LessonId.PERCENTAGES_VISUALLY) +
            series("c", correct = 5, incorrect = 0, topic = LessonId.ORIGINAL_WHOLE)

        val analytics = LearningAnalyticsEngine.analyse(events)

        assertEquals(LessonId.ORIGINAL_WHOLE, analytics.strongestTopic?.topic)
        assertEquals(LessonId.PERCENTAGES_VISUALLY, analytics.weakestTopic?.topic)
    }

    @Test
    fun `a tiny high-scoring topic is never named strongest`() {
        val events = series("a", correct = 1, incorrect = 0) +
            series("b", correct = 4, incorrect = 1, topic = LessonId.ORIGINAL_WHOLE)

        val analytics = LearningAnalyticsEngine.analyse(events)

        assertEquals(LessonId.ORIGINAL_WHOLE, analytics.strongestTopic?.topic)
    }

    @Test
    fun `weakest topic prefers the larger sample on a tie`() {
        val events = series("small", 2, 3) +
            series("big", 4, 6, session = "s2", topic = LessonId.PERCENTAGES_VISUALLY)

        val analytics = LearningAnalyticsEngine.analyse(events)

        assertEquals(LessonId.PERCENTAGES_VISUALLY, analytics.weakestTopic?.topic)
        assertEquals(10, analytics.weakestTopic?.attempted)
    }

    // ---- difficulty ---------------------------------------------------------

    @Test
    fun `performance is split by difficulty`() {
        val events = series("e", correct = 4, incorrect = 0, difficulty = QuestionDifficulty.EASY) +
            series("m", correct = 1, incorrect = 3, difficulty = QuestionDifficulty.MEDIUM)

        val topic = LearningAnalyticsEngine.analyse(events).topic(LessonId.WHAT_IS_A_PERCENTAGE)!!

        assertEquals(100, topic.accuracyFor(QuestionDifficulty.EASY))
        assertEquals(25, topic.accuracyFor(QuestionDifficulty.MEDIUM))
        assertNull(topic.accuracyFor(QuestionDifficulty.HARD))
    }

    @Test
    fun `a high score built on easy questions is flagged as not proof of mastery`() {
        val events = series("e", correct = 5, incorrect = 0, difficulty = QuestionDifficulty.EASY)
        val topic = LearningAnalyticsEngine.analyse(events)
            .topic(LessonId.WHAT_IS_A_PERCENTAGE)!!

        assertEquals(TopicStatus.STRONG, topic.status)
        assertTrue(topic.isHighAccuracyOnEasyQuestionsOnly)
    }

    @Test
    fun `strength spread across difficulties is not flagged`() {
        val events = series("e", correct = 5, incorrect = 0, difficulty = QuestionDifficulty.EASY) +
            series("m", correct = 3, incorrect = 2, difficulty = QuestionDifficulty.MEDIUM)
        val topic = LearningAnalyticsEngine.analyse(events)
            .topic(LessonId.WHAT_IS_A_PERCENTAGE)!!

        assertFalse(topic.isHighAccuracyOnEasyQuestionsOnly)
    }

    // ---- recent window ------------------------------------------------------

    @Test
    fun `the recent window holds only the most recent answers`() {
        val events = (0 until 25).map { event("q$it", correct = it >= 5, at = it.toLong()) }

        val recent = LearningAnalyticsEngine.recentWindow(events)

        assertEquals(InsightThresholds.RECENT_WINDOW_SIZE, recent.attempted)
        // Only five answers exist before the window, so there is no comparison.
        assertEquals(5, recent.previousAttempted)
        assertFalse(recent.hasComparison)
    }

    @Test
    fun `improvement is reported against the previous window`() {
        // Old window all wrong, recent window all right.
        val old = (0 until 20).map { event("old$it", false, session = "old", at = it.toLong()) }
        val fresh = (20 until 40).map { event("new$it", true, session = "new", at = it.toLong()) }

        val recent = LearningAnalyticsEngine.recentWindow(old + fresh)

        assertEquals(100, recent.accuracyPercent)
        assertEquals(0, recent.previousAccuracyPercent)
        assertTrue(recent.hasComparison)
        assertTrue(recent.isImproving)
        assertEquals(100, recent.accuracyDeltaPercent)
    }

    @Test
    fun `decline is reported against the previous window`() {
        val old = (0 until 20).map { event("old$it", true, session = "old", at = it.toLong()) }
        val fresh = (20 until 40).map { event("new$it", false, session = "new", at = it.toLong()) }

        val recent = LearningAnalyticsEngine.recentWindow(old + fresh)

        assertTrue(recent.hasComparison)
        assertTrue(recent.isDeclining)
        assertEquals(-100, recent.accuracyDeltaPercent)
    }

    @Test
    fun `steady is neither improving nor declining`() {
        val events = (0 until 40).map { event("q$it", correct = it % 2 == 0, session = "s$it", at = it.toLong()) }

        val recent = LearningAnalyticsEngine.recentWindow(events)

        assertEquals(50, recent.accuracyPercent)
        assertEquals(50, recent.previousAccuracyPercent)
        assertTrue(recent.isSteady)
        assertEquals(0, recent.accuracyDeltaPercent)
    }

    @Test
    fun `no comparison is claimed from a partial history`() {
        val events = (0 until 25).map { event("q$it", it % 2 == 0, session = "s$it", at = it.toLong()) }

        val recent = LearningAnalyticsEngine.recentWindow(events)

        // The previous window only has five answers, under half of twenty.
        assertEquals(5, recent.previousAttempted)
        assertFalse(recent.hasComparison)
        assertEquals(0, recent.accuracyDeltaPercent)
    }

    @Test
    fun `a history shorter than one window has no comparison`() {
        val recent = LearningAnalyticsEngine.recentWindow(series("a", 3, 1))

        assertEquals(4, recent.attempted)
        assertEquals(0, recent.previousAttempted)
        assertFalse(recent.hasComparison)
    }

    @Test
    fun `recent performance splits by topic from the same window`() {
        val events = series("a", correct = 2, incorrect = 1, topic = LessonId.WHAT_IS_A_PERCENTAGE) +
            series("b", correct = 1, incorrect = 2, topic = LessonId.ORIGINAL_WHOLE)

        val byTopic = LearningAnalyticsEngine.recentByTopic(events)

        assertEquals(2, byTopic.size)
        assertEquals(3, byTopic.first { it.topic == LessonId.WHAT_IS_A_PERCENTAGE }.attempted)
    }

    // ---- mixed sources in the window ---------------------------------------

    @Test
    fun `the recent window spans both practice and mock tests`() {
        val practice = (0 until 10).map { event("p$it", true, session = "p$it", at = it.toLong()) }
        val mocks = (10 until 20).map {
            event("m$it", false, session = "mock$it", at = it.toLong())
                .copy(source = AnswerSource.MOCK_TEST)
        }

        val recent = LearningAnalyticsEngine.recentWindow(practice + mocks)

        assertEquals(20, recent.attempted)
        assertEquals(50, recent.accuracyPercent)
    }
}