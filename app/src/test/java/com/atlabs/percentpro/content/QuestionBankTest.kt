package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the authored question bank.
 *
 * These assertions are the reason the bank can be trusted: a mistyped answer
 * index, a duplicated option or a missing translation fails the build rather
 * than reaching a learner.
 */
class QuestionBankTest {

    private val bank = QuestionBank()
    private val repository = QuestionRepository(bank)

    /** Every topic the bank ships questions for, in course order. */
    private val expectedTopics = listOf(
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
        LessonId.EXAM_SHORTCUTS,
        LessonId.POPULATION_CHANGE,
        LessonId.SALARY_EXPENDITURE_SAVINGS,
        LessonId.MARKS_AND_RESULTS,
        LessonId.ELECTION_VOTING,
        LessonId.PRICE_DISCOUNT_TAX,
        LessonId.PROFIT_LOSS,
        LessonId.REVERSE_PERCENTAGE,
        LessonId.MIXED_WORD_PROBLEMS
    )

    /** Topics listed on the path but deliberately left without questions. */
    private val topicsWithoutQuestions = listOf(LessonId.COMPETITIVE_EXAM_CHALLENGE)

    @Test
    fun `every question passes validation`() {
        bank.all().forEach { question ->
            assertTrue("${question.id} should be valid", question.isValid)
        }
    }

    @Test
    fun `validation filters nothing out of the real bank`() {
        assertEquals(bank.all().size, bank.all().count { it.isValid })
    }

    @Test
    fun `available topics match the topics the bank ships`() {
        assertEquals(expectedTopics, repository.availableTopics())
        expectedTopics.forEach { topic ->
            assertTrue("$topic should have questions", repository.hasQuestions(topic))
        }
    }

    @Test
    fun `the challenge lesson has no questions and is never offered`() {
        topicsWithoutQuestions.forEach { topic ->
            assertFalse("$topic must not be playable", repository.hasQuestions(topic))
            assertTrue(repository.questionsFor(topic).isEmpty())
        }
    }

    @Test
    fun `each available topic has at least five questions`() {
        expectedTopics.forEach { topic ->
            assertTrue(
                "$topic needs at least 5 questions",
                repository.questionCountFor(topic) >= 5
            )
        }
    }

    @Test
    fun `the bank meets the Phase 6 size target`() {
        assertTrue(
            "the bank must hold at least 150 questions, found ${bank.all().size}",
            bank.all().size >= 150
        )
    }

    @Test
    fun `question ids are unique across the whole bank`() {
        assertEquals(bank.all().size, bank.all().map { it.id }.distinct().size)
    }

    @Test
    fun `question ids are unique within a topic`() {
        expectedTopics.forEach { topic ->
            val ids = repository.questionsFor(topic).map { it.id }
            assertEquals(ids.size, ids.distinct().size)
        }
    }

    @Test
    fun `every question belongs to a topic the repository serves`() {
        bank.all().forEach { question ->
            assertTrue(
                "${question.id} sits in a topic with no questions",
                repository.hasQuestions(question.topic)
            )
        }
    }

    @Test
    fun `no question has duplicate options`() {
        bank.all().forEach { question ->
            val english = question.options.map { it.english }
            val hindi = question.options.map { it.hindi }
            assertEquals(english.size, english.distinct().size)
            assertEquals(hindi.size, hindi.distinct().size)
        }
    }

    @Test
    fun `option order is identical in both languages`() {
        bank.all().forEach { question ->
            assertEquals(OPTION_COUNT_EXPECTED, question.options.size)
        }
    }

    @Test
    fun `the original twenty-four answer keys are unchanged`() {
        // Guards the questions that shipped before Phase 6. Their text and
        // correct option must never drift, because a learner may already have
        // answered them and earned XP for it.
        assertEquals(24, QuestionBankAnswerKey.ORIGINAL.size)
        QuestionBankAnswerKey.ORIGINAL.forEach { (id, correctText) ->
            val question = bank.all().firstOrNull { it.id.value == id }
            assertNotNull("missing question $id", question)
            requireNotNull(question)
            assertEquals("wrong answer key for $id", correctText, question.options[question.correctIndex].english)
            assertTrue("$id should be marked correct for its own key", question.isCorrectAnswer(question.correctIndex))
        }
    }

    @Test
    fun `every generated answer key is unchanged`() {
        QuestionBankAnswerKey.GENERATED.forEach { (id, expectedIndex) ->
            val question = bank.all().firstOrNull { it.id.value == id }
            assertNotNull("missing question $id", question)
            requireNotNull(question)
            assertEquals("wrong answer key for $id", expectedIndex, question.correctIndex)
            assertTrue("$id should be marked correct for its own key", question.isCorrectAnswer(expectedIndex))
        }
    }

    @Test
    fun `the bank contains exactly the ids the answer key expects`() {
        assertEquals(QuestionBankAnswerKey.ALL_IDS.size, bank.all().size)
        assertEquals(
            QuestionBankAnswerKey.ALL_IDS,
            bank.all().map { it.id.value }.toSet()
        )
    }

    @Test
    fun `only one option is ever the correct answer`() {
        bank.all().forEach { question ->
            val correctCount = question.options.indices.count(question::isCorrectAnswer)
            assertEquals("${question.id} must have exactly one correct option", 1, correctCount)
        }
    }

    @Test
    fun `maths in the questions agrees with the calculation helpers`() {
        // 50% of 100, 25% of 200 and the four fraction conversions.
        assertEquals(50.0, PercentageMath.percentOf(50.0, 100.0), 1e-9)
        assertEquals(50.0, PercentageMath.percentOf(25.0, 200.0), 1e-9)
        assertEquals(50.0, PercentageMath.fractionAsPercent(1.0, 2.0), 1e-9)
        assertEquals(25.0, PercentageMath.fractionAsPercent(1.0, 4.0), 1e-9)
        assertEquals(75.0, PercentageMath.fractionAsPercent(3.0, 4.0), 1e-9)
        assertEquals(20.0, PercentageMath.fractionAsPercent(1.0, 5.0), 1e-9)
        // The lesson 2 grid: 10 squares shaded at 10% each is 10 of 100.
        assertEquals(10, PercentageMath.highlightedSquareCount(10.0))
        assertEquals(100, PercentageMath.highlightedSquareCount(100.0))
    }

    @Test
    fun `topic filtering returns only that topic's questions`() {
        expectedTopics.forEach { topic ->
            val questions = repository.questionsFor(topic)
            assertTrue("questions leaked across topics for $topic", questions.all { it.topic == topic })
            assertEquals("no questions for $topic", questions.size, repository.questionCountFor(topic))
        }
        assertTrue(repository.questionsFor(LessonId.COMPETITIVE_EXAM_CHALLENGE).isEmpty())
    }

    @Test
    fun `finding a question by id works within a topic`() {
        val question = repository.questionsFor(LessonId.WHAT_IS_A_PERCENTAGE).first()
        assertEquals(question, repository.find(LessonId.WHAT_IS_A_PERCENTAGE, question.id))
        assertEquals(null, repository.find(LessonId.PERCENTAGES_VISUALLY, question.id))
    }

    @Test
    fun `question ordering is deterministic across reads`() {
        assertEquals(repository.questionsFor(LessonId.FRACTIONS_TO_PERCENTAGES).map { it.id },
            QuestionRepository().questionsFor(LessonId.FRACTIONS_TO_PERCENTAGES).map { it.id })
    }

    @Test
    fun `every question has a non trivial explanation`() {
        bank.all().forEach { question ->
            assertTrue(question.explanation.english.length > 10)
            assertTrue(question.explanation.hindi.length > 10)
        }
    }

    @Test
    fun `numeric answers stay identical across languages`() {
        // Numbers and symbols must not drift between the two versions of a question.
        bank.all().forEach { question ->
            val englishDigits = question.options.map { it.english }
                .mapNotNull { option -> Regex("\\d+(\\.\\d+)?").findAll(option).map { it.value }.toList() }
            val hindiDigits = question.options.map { it.hindi }
                .mapNotNull { option -> Regex("\\d+(\\.\\d+)?").findAll(option).map { it.value }.toList() }
            assertEquals(
                "numeric options differ between languages for ${question.id}",
                englishDigits,
                hindiDigits
            )
        }
    }

    private companion object {
        const val OPTION_COUNT_EXPECTED = 4
    }
}