package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LessonStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Walks the unlock chain the way a learner would: complete lessons in order and
 * check that exactly one new lesson becomes available at each step.
 */
class LearningFlowTest {

    private val lessons = LessonCatalog().lessons()

    private fun statusOf(id: LessonId, completed: Set<LessonId>) = LessonAvailability.statusFor(
        lesson = lessons.first { it.id == id },
        allLessons = lessons,
        completed = completed,
        inProgress = null
    )

    private val ids = lessons.map { it.id }

    @Test
    fun `a fresh learner sees lesson one open and everything else shut`() {
        val open = ids.filter { statusOf(it, emptySet()).isUnlocked }
        assertEquals(listOf(LessonId.WHAT_IS_A_PERCENTAGE), open)
    }

    @Test
    fun `each completion opens exactly one more lesson until the content runs out`() {
        val playable = ids.filter { it.isPlayableByCatalog() }
        var completed = emptySet<LessonId>()

        playable.forEachIndexed { index, expectedNext ->
            assertEquals(
                "lesson ${index + 1} should be available",
                LessonStatus.AVAILABLE,
                statusOf(expectedNext, completed)
            )
            completed = completed + expectedNext
        }

        assertEquals(12, completed.size)
        assertNull(LessonAvailability.currentLesson(lessons, completed, null))
    }

    @Test
    fun `the challenge lesson stays shut even when everything before it is done`() {
        val everythingDone = ids.toSet()
        assertEquals(
            LessonStatus.LOCKED_UPCOMING,
            statusOf(LessonId.COMPETITIVE_EXAM_CHALLENGE, everythingDone)
        )
    }

    @Test
    fun `saved data cannot skip the queue or unlock unwritten lessons`() {
        // A tampered save that skips lesson two and claims lesson three is done.
        val tampered = setOf(
            LessonId.WHAT_IS_A_PERCENTAGE,
            LessonId.FRACTIONS_TO_PERCENTAGES
        )
        // The unfinished predecessor is what stays open next.
        assertEquals(LessonStatus.AVAILABLE, statusOf(LessonId.PERCENTAGES_VISUALLY, tampered))
        // A written lesson downstream still follows the sequence rule, which
        // keys off the lesson immediately before it (lesson three, which the
        // tampered save claims is done).
        assertEquals(
            LessonStatus.AVAILABLE,
            statusOf(LessonId.DECIMALS_TO_PERCENTAGES, tampered)
        )
        // Nothing written later opens without its own predecessor being done.
        assertEquals(
            LessonStatus.LOCKED_SEQUENCE,
            statusOf(LessonId.EXAM_SHORTCUTS, tampered)
        )
        // Crucially, no unwritten lesson is unlocked by a fabricated save.
        assertEquals(
            LessonStatus.LOCKED_UPCOMING,
            statusOf(LessonId.POPULATION_CHANGE, tampered)
        )
        assertEquals(
            LessonStatus.LOCKED_UPCOMING,
            statusOf(LessonId.COMPETITIVE_EXAM_CHALLENGE, tampered)
        )
    }

    @Test
    fun `course progress never counts unwritten lessons`() {
        val allIdsAsDone = ids.toSet()
        assertEquals(100, LessonAvailability.completionPercent(lessons, allIdsAsDone))
        assertEquals(0, LessonAvailability.completionPercent(lessons, emptySet()))
        // Twelve written lessons, so one is 8% after truncation.
        assertEquals(
            8,
            LessonAvailability.completionPercent(lessons, setOf(LessonId.WHAT_IS_A_PERCENTAGE))
        )
    }

    @Test
    fun `progress survives a full save and restore cycle`() {
        var progress = LearningProgress()
        progress = progress.startLesson(LessonId.WHAT_IS_A_PERCENTAGE)
        progress = progress.setBlockIndex(5)

        val encoded = ProgressCodec.encode(progress)
        progress = ProgressCodec.decode(
            version = encoded.version,
            completedLessonIds = encoded.completedLessonIds,
            lessonInProgress = encoded.lessonInProgress,
            lastBlockIndex = encoded.lastBlockIndex,
            playableLessons = setOf(
                LessonId.WHAT_IS_A_PERCENTAGE,
                LessonId.PERCENTAGES_VISUALLY,
                LessonId.FRACTIONS_TO_PERCENTAGES
            ),
            blockCount = 8
        )

        assertEquals(LessonId.WHAT_IS_A_PERCENTAGE, progress.lessonInProgress)
        assertEquals(5, progress.lastBlockIndex)

        // Finishing the lesson unlocks the next one after the same round trip.
        progress = progress.markCompleted(LessonId.WHAT_IS_A_PERCENTAGE)
        val finished = ProgressCodec.encode(progress)
        val restored = ProgressCodec.decode(
            version = finished.version,
            completedLessonIds = finished.completedLessonIds,
            lessonInProgress = finished.lessonInProgress,
            lastBlockIndex = finished.lastBlockIndex,
            playableLessons = setOf(
                LessonId.WHAT_IS_A_PERCENTAGE,
                LessonId.PERCENTAGES_VISUALLY,
                LessonId.FRACTIONS_TO_PERCENTAGES
            ),
            blockCount = 5
        )

        assertTrue(restored.isCompleted(LessonId.WHAT_IS_A_PERCENTAGE))
        assertEquals(
            LessonStatus.AVAILABLE,
            statusOf(LessonId.PERCENTAGES_VISUALLY, restored.completedLessonIds)
        )
    }

    private fun LessonId.isPlayableByCatalog(): Boolean =
        lessons.first { it.id == this }.isPlayable
}