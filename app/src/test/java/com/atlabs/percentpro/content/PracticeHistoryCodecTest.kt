package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.PracticeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeHistoryCodecTest {

    private val topicA = LessonId.WHAT_IS_A_PERCENTAGE
    private val topicB = LessonId.FRACTIONS_TO_PERCENTAGES

    private fun result(
        topic: LessonId = topicA,
        at: Long = 1_000L,
        total: Int = 6,
        correct: Int = 4,
        incorrect: Int = 2,
        unanswered: Int = 0
    ) = PracticeResult.of(topic, at, total, correct, incorrect, unanswered)

    @Test
    fun `round trips a saved history`() {
        val results = listOf(
            result(topicA, at = 1_000L, total = 6, correct = 6, incorrect = 0, unanswered = 0),
            result(topicB, at = 2_000L, total = 6, correct = 3, incorrect = 2, unanswered = 1)
        )
        val restored = PracticeHistoryCodec.decode(PracticeHistoryCodec.encode(results))

        assertEquals(2, restored.size)
        assertEquals(results, restored)
    }

    @Test
    fun `nothing stored decodes to an empty history`() {
        assertTrue(PracticeHistoryCodec.decode(null).isEmpty())
        assertTrue(PracticeHistoryCodec.decode("").isEmpty())
        assertTrue(PracticeHistoryCodec.decode("   ").isEmpty())
    }

    @Test
    fun `restored results are ordered oldest first`() {
        val raw = listOf(
            result(at = 3_000L),
            result(at = 1_000L),
            result(at = 2_000L)
        ).joinToString("\n") { "${it.topic.storageKey}|${it.completedAtEpochMillis}|${it.total}|${it.correct}|${it.incorrect}|${it.unanswered}|${it.accuracyPercent}" }

        val restored = PracticeHistoryCodec.decode(raw)
        assertEquals(listOf(1_000L, 2_000L, 3_000L), restored.map { it.completedAtEpochMillis })
    }

    @Test
    fun `malformed records are skipped without losing the good ones`() {
        val good = "what_is_a_percentage|1000|6|4|2|0|66"
        val raw = listOf(
            "not-a-record",
            "",
            "what_is_a_percentage|1000",                 // too few fields
            "what_is_a_percentage|1000|6|4|2|0|66|extra",  // too many fields
            "a_removed_topic|1000|6|4|2|0|66",           // unknown topic
            "what_is_a_percentage|notanumber|6|4|2|0|66", // bad timestamp
            "what_is_a_percentage|1000|6|4|2|0|notanum", // bad accuracy
            "what_is_a_percentage|1000|6|4|2|2|66",      // counts do not add up
            good
        ).joinToString("\n")

        val restored = PracticeHistoryCodec.decode(raw)
        assertEquals(1, restored.size)
        assertEquals(1_000L, restored.first().completedAtEpochMillis)
        assertEquals(4, restored.first().correct)
    }

    @Test
    fun `records with negative numbers are rejected`() {
        val raw = "what_is_a_percentage|-5|6|4|2|0|66"
        assertTrue(PracticeHistoryCodec.decode(raw).isEmpty())
    }

    @Test
    fun `invalid results are never written`() {
        val invalid = PracticeResult(
            topic = topicA,
            completedAtEpochMillis = 1L,
            total = 5,
            correct = 2,
            incorrect = 2,
            unanswered = 2,
            accuracyPercent = 40
        )
        assertTrue(PracticeHistoryCodec.encode(listOf(invalid)).isEmpty())
        assertTrue(PracticeHistoryCodec.decode(PracticeHistoryCodec.encode(listOf(invalid))).isEmpty())
    }

    @Test
    fun `history is capped so storage cannot grow without bound`() {
        val many = (1..(PracticeHistoryCodec.MAX_STORED_RESULTS + 25))
            .map { index -> result(at = index.toLong()) }

        val encoded = PracticeHistoryCodec.encode(many)
        val restored = PracticeHistoryCodec.decode(encoded)

        assertEquals(PracticeHistoryCodec.MAX_STORED_RESULTS, restored.size)
        // The newest entries are the ones kept.
        assertEquals(
            (PracticeHistoryCodec.MAX_STORED_RESULTS + 25).toLong(),
            restored.last().completedAtEpochMillis
        )
    }

    @Test
    fun `latest result per topic wins`() {
        val results = listOf(
            result(topicA, at = 1_000L, correct = 1, incorrect = 5, unanswered = 0),
            result(topicA, at = 5_000L, correct = 5, incorrect = 1, unanswered = 0),
            result(topicB, at = 3_000L, correct = 2, incorrect = 4, unanswered = 0)
        )

        val latest = PracticeHistoryCodec.latestByTopic(results)
        assertEquals(2, latest.size)
        assertEquals(5_000L, latest[topicA]?.completedAtEpochMillis)
        assertEquals(3_000L, latest[topicB]?.completedAtEpochMillis)
    }

    @Test
    fun `latest by topic on an empty history is empty`() {
        assertTrue(PracticeHistoryCodec.latestByTopic(emptyList()).isEmpty())
    }

    @Test
    fun `an unknown topic has no stored result`() {
        assertNull(PracticeHistoryCodec.latestByTopic(emptyList())[LessonId.ORIGINAL_WHOLE])
    }

    @Test
    fun `topics without questions can still have history once they are written`() {
        // Decoding must not depend on which topics currently have questions, so a
        // result written before a topic was removed still round trips.
        val stored = result(topic = LessonId.ORIGINAL_WHOLE, total = 3, correct = 1, incorrect = 2, unanswered = 0)
        val restored = PracticeHistoryCodec.decode(PracticeHistoryCodec.encode(listOf(stored)))
        assertEquals(LessonId.ORIGINAL_WHOLE, restored.single().topic)
    }
}