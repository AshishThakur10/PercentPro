package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressCodecTest {

    private val playable = setOf(
        LessonId.WHAT_IS_A_PERCENTAGE,
        LessonId.PERCENTAGES_VISUALLY,
        LessonId.FRACTIONS_TO_PERCENTAGES
    )

    private fun decode(
        version: Int? = ProgressCodec.CURRENT_VERSION,
        completed: Set<String>? = null,
        active: String? = null,
        blockIndex: Int? = null,
        blockCount: Int = 8
    ) = ProgressCodec.decode(
        version = version,
        completedLessonIds = completed,
        lessonInProgress = active,
        lastBlockIndex = blockIndex,
        playableLessons = playable,
        blockCount = blockCount
    )

    @Test
    fun `round trips a saved progress snapshot`() {
        val original = LearningProgress(
            completedLessonIds = setOf(
                LessonId.WHAT_IS_A_PERCENTAGE,
                LessonId.PERCENTAGES_VISUALLY
            ),
            lessonInProgress = LessonId.FRACTIONS_TO_PERCENTAGES,
            lastBlockIndex = 4
        )
        val encoded = ProgressCodec.encode(original)
        assertEquals(ProgressCodec.CURRENT_VERSION, encoded.version)
        assertEquals(
            setOf("what_is_a_percentage", "percentages_visually"),
            encoded.completedLessonIds
        )
        assertEquals("fractions_to_percentages", encoded.lessonInProgress)
        assertEquals(4, encoded.lastBlockIndex)

        val restored = decode(
            version = encoded.version,
            completed = encoded.completedLessonIds,
            active = encoded.lessonInProgress,
            blockIndex = encoded.lastBlockIndex
        )
        assertEquals(original, restored)
    }

    @Test
    fun `nothing saved restores as a clean slate`() {
        val restored = decode(version = null, completed = null, active = null, blockIndex = null)
        assertEquals(LearningProgress(), restored)
        assertFalse(restored.hasStarted)
    }

    @Test
    fun `missing version discards saved data`() {
        val restored = decode(
            version = null,
            completed = setOf("what_is_a_percentage"),
            active = "percentages_visually",
            blockIndex = 3
        )
        assertTrue(restored.completedLessonIds.isEmpty())
        assertNull(restored.lessonInProgress)
        assertEquals(0, restored.lastBlockIndex)
    }

    @Test
    fun `data from a future version is discarded`() {
        val restored = decode(
            version = ProgressCodec.CURRENT_VERSION + 1,
            completed = setOf("what_is_a_percentage")
        )
        assertTrue(restored.completedLessonIds.isEmpty())
    }

    @Test
    fun `unknown lesson ids are dropped`() {
        val restored = decode(
            completed = setOf("what_is_a_percentage", "a_removed_lesson", "")
        )
        assertEquals(setOf(LessonId.WHAT_IS_A_PERCENTAGE), restored.completedLessonIds)
    }

    @Test
    fun `lessons without content cannot be restored as completed`() {
        val restored = decode(
            completed = setOf(
                "what_is_a_percentage",
                "decimals_to_percentages",
                "competitive_exam_challenge"
            )
        )
        assertEquals(setOf(LessonId.WHAT_IS_A_PERCENTAGE), restored.completedLessonIds)
    }

    @Test
    fun `an unwritten lesson is never restored as in progress`() {
        val restored = decode(active = "decimals_to_percentages", blockIndex = 2)
        assertNull(restored.lessonInProgress)
        assertEquals(0, restored.lastBlockIndex)
    }

    @Test
    fun `a lesson that is already completed is not left open`() {
        val restored = decode(
            completed = setOf("what_is_a_percentage"),
            active = "what_is_a_percentage",
            blockIndex = 5
        )
        assertNull(restored.lessonInProgress)
        assertEquals(0, restored.lastBlockIndex)
    }

    @Test
    fun `an out of range block index is clamped to the lesson`() {
        val tooHigh = decode(active = "what_is_a_percentage", blockIndex = 999, blockCount = 8)
        assertEquals(7, tooHigh.lastBlockIndex)

        val negative = decode(active = "what_is_a_percentage", blockIndex = -4, blockCount = 8)
        assertEquals(0, negative.lastBlockIndex)
    }

    @Test
    fun `block index is ignored when no lesson is open`() {
        val restored = decode(blockIndex = 6)
        assertNull(restored.lessonInProgress)
        assertEquals(0, restored.lastBlockIndex)
    }

    @Test
    fun `a lesson with a single block clamps to zero`() {
        val restored = decode(active = "what_is_a_percentage", blockIndex = 3, blockCount = 1)
        assertEquals(0, restored.lastBlockIndex)
    }

    @Test
    fun `marking a lesson completed clears it as the open lesson`() {
        val progress = LearningProgress(
            lessonInProgress = LessonId.WHAT_IS_A_PERCENTAGE,
            lastBlockIndex = 7
        )
        val done = progress.markCompleted(LessonId.WHAT_IS_A_PERCENTAGE)

        assertTrue(done.isCompleted(LessonId.WHAT_IS_A_PERCENTAGE))
        assertNull(done.lessonInProgress)
        assertEquals(0, done.lastBlockIndex)
        assertTrue(done.hasStarted)
    }

    @Test
    fun `opening a lesson resets the block index`() {
        val progress = LearningProgress()
            .markCompleted(LessonId.WHAT_IS_A_PERCENTAGE)
            .startLesson(LessonId.PERCENTAGES_VISUALLY)

        assertEquals(LessonId.PERCENTAGES_VISUALLY, progress.lessonInProgress)
        assertEquals(0, progress.lastBlockIndex)
    }

    @Test
    fun `saving a block index never goes negative`() {
        val progress = LearningProgress().setBlockIndex(-2)
        assertEquals(0, progress.lastBlockIndex)
    }

    @Test
    fun `a fresh learner has not started`() {
        assertFalse(LearningProgress().hasStarted)
        assertTrue(LearningProgress(completedLessonIds = setOf(LessonId.WHAT_IS_A_PERCENTAGE)).hasStarted)
        assertTrue(LearningProgress(lessonInProgress = LessonId.WHAT_IS_A_PERCENTAGE).hasStarted)
    }
}