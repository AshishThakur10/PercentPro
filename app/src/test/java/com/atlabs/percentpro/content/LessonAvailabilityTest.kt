package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.Lesson
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LessonStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonAvailabilityTest {

    private val lessons = LessonCatalog().lessons()

    private fun statusOf(
        id: LessonId,
        completed: Set<LessonId> = emptySet(),
        inProgress: LessonId? = null
    ) = LessonAvailability.statusFor(
        lesson = lessons.first { it.id == id },
        allLessons = lessons,
        completed = completed,
        inProgress = inProgress
    )

    private fun canOpen(
        id: LessonId,
        completed: Set<LessonId> = emptySet(),
        inProgress: LessonId? = null
    ) = LessonAvailability.canOpen(
        lesson = lessons.first { it.id == id },
        allLessons = lessons,
        completed = completed,
        inProgress = inProgress
    )

    /** Lessons whose content ships in this phase, in order. */
    private val implementedIds = listOf(
        LessonId.WHAT_IS_A_PERCENTAGE,
        LessonId.PERCENTAGES_VISUALLY,
        LessonId.FRACTIONS_TO_PERCENTAGES,
        LessonId.DECIMALS_TO_PERCENTAGES,
        LessonId.PERCENTAGE_OF_A_NUMBER,
        LessonId.ORIGINAL_WHOLE,
        LessonId.PERCENTAGE_INCREASE_DECREASE,
        LessonId.SUCCESSIVE_PERCENTAGE_CHANGES,
        LessonId.PERCENTAGE_COMPARISON,
        LessonId.PERCENTAGE_POINTS_VS_CHANGE,
        LessonId.COMPARING_REPRESENTATIONS,
        LessonId.EXAM_SHORTCUTS
    )

    /** Catalogued in Phase 6 but deliberately left unwritten. */
    private val upcomingIds = listOf(
        LessonId.POPULATION_CHANGE,
        LessonId.SALARY_EXPENDITURE_SAVINGS,
        LessonId.MARKS_AND_RESULTS,
        LessonId.ELECTION_VOTING,
        LessonId.PRICE_DISCOUNT_TAX,
        LessonId.PROFIT_LOSS,
        LessonId.REVERSE_PERCENTAGE,
        LessonId.MIXED_WORD_PROBLEMS,
        LessonId.COMPETITIVE_EXAM_CHALLENGE
    )

    @Test
    fun `path holds twenty-one lessons in order`() {
        assertEquals(21, lessons.size)
        assertEquals((1..21).toList(), lessons.map { it.order })
        assertEquals(lessons.map { it.id }.distinct(), lessons.map { it.id })
    }

    @Test
    fun `the course is split into three ordered sections`() {
        val sections = LessonCatalog().sections()
        assertEquals(3, sections.size)
        assertEquals(listOf(1, 7, 13), sections.map { it.section.firstOrder })
        assertEquals(listOf(6, 12, 21), sections.map { it.section.lastOrder })
        // Every lesson appears in exactly one section, and the sections tile the path.
        val fromSections = sections.flatMap { it.lessons.map(Lesson::id) }
        assertEquals(lessons.map { it.id }, fromSections)
        sections.forEach { section ->
            assertEquals(section.section.lessonCount, section.lessons.size)
        }
    }

    @Test
    fun `the three sections hold the documented lesson counts`() {
        val sections = LessonCatalog().sections()
        assertEquals(6, sections[0].lessons.size)
        assertEquals(6, sections[1].lessons.size)
        assertEquals(9, sections[2].lessons.size)
    }

    @Test
    fun `only the first lesson is available for a new learner`() {
        assertEquals(LessonStatus.AVAILABLE, statusOf(LessonId.WHAT_IS_A_PERCENTAGE))
        assertEquals(LessonStatus.LOCKED_SEQUENCE, statusOf(LessonId.PERCENTAGES_VISUALLY))
        assertEquals(LessonStatus.LOCKED_SEQUENCE, statusOf(LessonId.FRACTIONS_TO_PERCENTAGES))
    }

    @Test
    fun `completing lesson one unlocks lesson two but not lesson three`() {
        val done = setOf(LessonId.WHAT_IS_A_PERCENTAGE)
        assertEquals(LessonStatus.COMPLETED, statusOf(LessonId.WHAT_IS_A_PERCENTAGE, done))
        assertEquals(LessonStatus.AVAILABLE, statusOf(LessonId.PERCENTAGES_VISUALLY, done))
        assertEquals(LessonStatus.LOCKED_SEQUENCE, statusOf(LessonId.FRACTIONS_TO_PERCENTAGES, done))
    }

    @Test
    fun `completing lesson two unlocks lesson three`() {
        val done = setOf(
            LessonId.WHAT_IS_A_PERCENTAGE,
            LessonId.PERCENTAGES_VISUALLY
        )
        assertEquals(LessonStatus.AVAILABLE, statusOf(LessonId.FRACTIONS_TO_PERCENTAGES, done))
    }

    @Test
    fun `completing every implemented lesson does not unlock the unwritten ones`() {
        val done = implementedIds.toSet()
        upcomingIds.forEach { id ->
            assertEquals("$id must stay shut", LessonStatus.LOCKED_UPCOMING, statusOf(id, done))
            assertFalse("$id must not open", canOpen(id, done))
        }
    }

    @Test
    fun `finishing the implemented lessons opens lesson twelve, not thirteen`() {
        // Everything up to but excluding EXAM_SHORTCUTS is done.
        val done = implementedIds.dropLast(1).toSet()
        assertEquals(LessonStatus.AVAILABLE, statusOf(LessonId.EXAM_SHORTCUTS, done))
        assertEquals(
            LessonStatus.LOCKED_UPCOMING,
            statusOf(LessonId.POPULATION_CHANGE, done)
        )
    }

    @Test
    fun `the competitive exam challenge is never available`() {
        val done = lessons.map { it.id }.toSet()
        assertEquals(
            LessonStatus.LOCKED_UPCOMING,
            statusOf(LessonId.COMPETITIVE_EXAM_CHALLENGE, done)
        )
        assertFalse(canOpen(LessonId.COMPETITIVE_EXAM_CHALLENGE, done))
    }

    @Test
    fun `an open lesson reports in progress and stays openable`() {
        assertEquals(
            LessonStatus.IN_PROGRESS,
            statusOf(LessonId.PERCENTAGES_VISUALLY, inProgress = LessonId.PERCENTAGES_VISUALLY)
        )
        assertTrue(
            canOpen(
                LessonId.PERCENTAGES_VISUALLY,
                inProgress = LessonId.PERCENTAGES_VISUALLY
            )
        )
    }

    @Test
    fun `locked lessons cannot be opened`() {
        assertFalse(canOpen(LessonId.PERCENTAGES_VISUALLY))
        assertFalse(canOpen(LessonId.FRACTIONS_TO_PERCENTAGES))
    }

    @Test
    fun `completed lessons stay openable so they can be reviewed`() {
        val done = setOf(LessonId.WHAT_IS_A_PERCENTAGE)
        assertTrue(canOpen(LessonId.WHAT_IS_A_PERCENTAGE, done))
    }

    @Test
    fun `currentLesson prefers the lesson already in progress`() {
        val inProgress = LessonId.PERCENTAGES_VISUALLY
        val current = LessonAvailability.currentLesson(
            allLessons = lessons,
            completed = setOf(LessonId.WHAT_IS_A_PERCENTAGE),
            inProgress = inProgress
        )
        assertEquals(inProgress, current?.id)
    }

    @Test
    fun `currentLesson falls back to the next available lesson`() {
        val current = LessonAvailability.currentLesson(lessons, emptySet(), null)
        assertEquals(LessonId.WHAT_IS_A_PERCENTAGE, current?.id)
    }

    @Test
    fun `currentLesson is null once nothing playable is left`() {
        assertNull(LessonAvailability.currentLesson(lessons, implementedIds.toSet(), null))
    }

    @Test
    fun `completion is measured against implemented lessons only`() {
        assertEquals(12, LessonAvailability.implementedLessons(lessons).size)
        assertEquals(0, LessonAvailability.completionPercent(lessons, emptySet()))
        // One of twelve is 8% after truncation.
        assertEquals(
            8,
            LessonAvailability.completionPercent(lessons, setOf(LessonId.WHAT_IS_A_PERCENTAGE))
        )
        assertEquals(
            100,
            LessonAvailability.completionPercent(lessons, implementedIds.toSet())
        )
        // Completing an unwritten lesson must not move the denominator.
        assertEquals(
            LessonAvailability.completionPercent(lessons, implementedIds.toSet()),
            LessonAvailability.completionPercent(
                lessons,
                implementedIds.toSet() + setOf(LessonId.POPULATION_CHANGE)
            )
        )
    }

    @Test
    fun `lessons that are listed but unwritten are never marked implemented`() {
        val unwritten = lessons.filter { !it.isImplemented }.map { it.id }
        assertEquals(upcomingIds, unwritten)
        unwritten.forEach { lesson ->
            assertFalse(lessons.first { it.id == lesson }.isPlayable)
            assertEquals(LessonStatus.LOCKED_UPCOMING, statusOf(lesson, lessons.map { it.id }.toSet()))
        }
    }

    @Test
    fun `every lesson carries a stable id, order, difficulty and duration`() {
        lessons.forEach { lesson ->
            assertTrue("duration for ${lesson.id}", lesson.durationMinutes > 0)
            assertTrue("order for ${lesson.id}", lesson.order > 0)
        }
    }

    @Test
    fun `lesson ids round trip through their storage key`() {
        lessons.map { it.id }.forEach { id ->
            assertEquals(id, LessonId.fromStorageKey(id.storageKey))
        }
        assertNull(LessonId.fromStorageKey("a_lesson_that_no_longer_exists"))
        assertNull(LessonId.fromStorageKey(null))
    }

    @Test
    fun `every implemented lesson has real content`() {
        val content = LessonContentRepository()
        val implemented = lessons.filter { it.isImplemented }.map { it.id }
        assertEquals(implementedIds, implemented)
        implemented.forEach { id ->
            val blocks = content.blocksFor(id)
            assertTrue("$id should have blocks", blocks.isNotEmpty())
        }
    }

    @Test
    fun `no unwritten lesson exposes content`() {
        val content = LessonContentRepository()
        upcomingIds.forEach { id ->
            assertTrue("$id must have no blocks", content.blocksFor(id).isEmpty())
            assertFalse("$id must not report itself implemented", content.isImplemented(id))
        }
    }

    @Test
    fun `lesson three covers the four required fraction examples`() {
        val lesson = lessons.first { it.id == LessonId.FRACTIONS_TO_PERCENTAGES }
        assertTrue(lesson.isImplemented)
        assertEquals(50.0, PercentageMath.fractionAsPercent(1.0, 2.0), 1e-9)
        assertEquals(25.0, PercentageMath.fractionAsPercent(1.0, 4.0), 1e-9)
        assertEquals(75.0, PercentageMath.fractionAsPercent(3.0, 4.0), 1e-9)
        assertEquals(20.0, PercentageMath.fractionAsPercent(1.0, 5.0), 1e-9)
    }

    @Test
    fun `lesson two exposes the grid values the lesson asks for`() {
        val blocks = LessonContentRepository().blocksFor(LessonId.PERCENTAGES_VISUALLY)
        val grid = blocks.filterIsInstance<PercentageGridBlock>().single()
        assertEquals(listOf(25, 50, 75), grid.selectablePercents)
    }

    @Test
    fun `lesson one contains introduction formula examples and a summary`() {
        val blocks = LessonContentRepository().blocksFor(LessonId.WHAT_IS_A_PERCENTAGE)
        assertTrue(blocks.any { it is IntroductionBlock })
        assertTrue(blocks.any { it is FormulaBlock })
        assertEquals(3, blocks.filterIsInstance<WorkedExampleBlock>().size)
        assertTrue(blocks.last() is SummaryBlock)
    }
}