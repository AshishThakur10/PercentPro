package com.atlabs.percentpro.content

import com.atlabs.percentpro.R
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalizedText
import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockTestSelectorTest {

    private val topics = listOf(
        LessonId.WHAT_IS_A_PERCENTAGE,
        LessonId.PERCENTAGE_OF_A_NUMBER,
        LessonId.PERCENTAGES_VISUALLY
    )

    private fun config(questionCount: Int) = MockTestConfig(
        id = MockTestId("selector"),
        titleRes = R.string.mock_title_percentage_aptitude,
        descriptionRes = R.string.mock_description_percentage_aptitude,
        questionCount = questionCount,
        durationMillis = 15 * 60 * 1000L,
        marksPerCorrectInQuarters = 4,
        negativeMarkInQuarters = 1
    )

    private fun question(id: String, topic: LessonId) = Question(
        id = QuestionId(id),
        topic = topic,
        difficulty = QuestionDifficulty.EASY,
        prompt = LocalizedText.of("Q$id", "Q$id"),
        options = listOf("A", "B", "C", "D").map { LocalizedText.of("$it", "$it") },
        correctIndex = 0,
        explanation = LocalizedText.of("E$id", "E$id")
    )

    /** Two questions per topic. */
    private val bank: Map<LessonId, List<Question>> = topics.associateWith { topic ->
        (1..2).map { index -> question("${topic.storageKey}_q$index", topic) }
    }

    private fun select(count: Int) = MockTestSelector.select(
        config = config(count),
        topics = topics,
        questionsByTopic = { topic -> bank.getValue(topic) }
    )

    @Test
    fun `selection takes the requested number of questions`() {
        assertEquals(6, select(6).size)
    }

    @Test
    fun `selection never repeats a question`() {
        val selected = select(6)

        assertEquals(selected.size, selected.map { it.id }.distinct().size)
    }

    @Test
    fun `selection spreads evenly across topics`() {
        val perTopic = select(6).groupingBy { it.topic }.eachCount()

        assertEquals(2, perTopic[LessonId.WHAT_IS_A_PERCENTAGE])
        assertEquals(2, perTopic[LessonId.PERCENTAGE_OF_A_NUMBER])
        assertEquals(2, perTopic[LessonId.PERCENTAGES_VISUALLY])
    }

    @Test
    fun `a part-filled final round does not starve the later topics`() {
        val perTopic = select(4).groupingBy { it.topic }.eachCount()

        assertEquals(3, perTopic.size)
        assertEquals(2, perTopic[LessonId.WHAT_IS_A_PERCENTAGE])
        assertEquals(1, perTopic[LessonId.PERCENTAGE_OF_A_NUMBER])
        assertEquals(1, perTopic[LessonId.PERCENTAGES_VISUALLY])
    }

    @Test
    fun `selection is deterministic`() {
        assertEquals(select(6), select(6))
    }

    @Test
    fun `selection returns everything available when the bank is smaller than the paper`() {
        val smallBank = mapOf(
            LessonId.WHAT_IS_A_PERCENTAGE to listOf(question("only_1", LessonId.WHAT_IS_A_PERCENTAGE))
        )

        val selected = MockTestSelector.select(
            config = config(15),
            topics = listOf(LessonId.WHAT_IS_A_PERCENTAGE),
            questionsByTopic = { smallBank.getValue(it) }
        )

        assertEquals(1, selected.size)
    }

    @Test
    fun `an empty bank produces an empty paper`() {
        val selected = MockTestSelector.select(
            config = config(15),
            topics = topics,
            questionsByTopic = { emptyList() }
        )

        assertTrue(selected.isEmpty())
    }

    @Test
    fun `a zero length paper selects nothing`() {
        assertTrue(select(0).isEmpty())
    }

    @Test
    fun `duplicate ids in a pool are only taken once`() {
        val duplicated = mapOf(
            LessonId.WHAT_IS_A_PERCENTAGE to
                listOf(question("dup", LessonId.WHAT_IS_A_PERCENTAGE), question("dup", LessonId.WHAT_IS_A_PERCENTAGE))
        )

        val selected = MockTestSelector.select(
            config = config(5),
            topics = listOf(LessonId.WHAT_IS_A_PERCENTAGE),
            questionsByTopic = { duplicated.getValue(it) }
        )

        assertEquals(1, selected.size)
    }

    @Test
    fun `the shipped test can be built from the real bank`() {
        val catalog = MockTestCatalog()
        val shipped = catalog.testById(MockTestId("percentage_aptitude_15"))

        assertTrue(shipped != null)
        val selected = MockTestSelector.select(
            config = shipped!!,
            topics = topics,
            questionsByTopic = { topic -> bank.getValue(topic) }
        )

        // The real bank has more per topic than this fake, so the paper fills.
        assertEquals(6, selected.size)
    }
}