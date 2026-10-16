package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.ExamId
import com.atlabs.percentpro.model.QuestionDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The exam papers, checked against the real bank.
 *
 * These are integration checks rather than unit tests because the property that
 * matters cannot be verified in isolation: a paper is only convincing if the
 * questions actually tagged for that exam can fill it, in the right proportions,
 * without repeating one.
 */
class ExamPaperTest {

    private val catalog = MockTestCatalog()
    private val exams = ExamCatalog()
    private val repository = QuestionRepository()

    private fun build(config: com.atlabs.percentpro.model.MockTestConfig) =
        MockTestSelector.select(
            config = config,
            topics = repository.availableTopics(),
            questionsByTopic = repository::questionsFor,
            difficultyMix = config.examId?.let { exams.examById(it)?.difficultyMix }
        )

    @Test
    fun `every shipped paper can be filled from the real bank`() {
        catalog.tests().forEach { config ->
            val built = build(config)
            assertEquals(
                "${config.id.value} could not be filled",
                config.questionCount,
                built.size
            )
        }
    }

    @Test
    fun `an exam paper draws only questions tagged for that exam`() {
        catalog.tests().filter { it.examId != null }.forEach { config ->
            val examId = config.examId!!
            val stray = build(config).filterNot { it.isFrom(examId) }
            assertTrue(
                "${config.id.value} drew ${stray.size} untagged questions",
                stray.isEmpty()
            )
        }
    }

    @Test
    fun `no paper repeats a question`() {
        catalog.tests().forEach { config ->
            val ids = build(config).map { it.id }
            assertEquals(
                "${config.id.value} repeated a question",
                ids.size,
                ids.distinct().size
            )
        }
    }

    @Test
    fun `an exam paper keeps the difficulty mix its exam is known for`() {
        catalog.tests().filter { it.examId != null }.forEach { config ->
            val mix = exams.examById(config.examId!!)!!.difficultyMix
            val built = build(config)
            QuestionDifficulty.entries.forEach { band ->
                val expected = mix.getValue(band) * config.questionCount
                val actual = built.count { it.difficulty == band }
                // Largest-remainder apportionment lands within one place of the
                // exact share, so anything further off means the bank cannot
                // supply this band for this exam.
                assertTrue(
                    "${config.id.value} wanted about ${expected.toInt()} $band " +
                        "questions but drew $actual",
                    kotlin.math.abs(actual - expected) <= 1.0
                )
            }
        }
    }

    @Test
    fun `an exam paper spans several topics rather than one`() {
        catalog.tests().filter { it.examId != null }.forEach { config ->
            val spread = build(config).map { it.topic }.distinct().size
            assertTrue(
                "${config.id.value} covered only $spread topics",
                spread >= 4
            )
        }
    }

    @Test
    fun `a paper's marking scheme is the one its exam publishes`() {
        catalog.tests().filter { it.examId != null }.forEach { config ->
            val exam = exams.examById(config.examId!!)!!
            assertEquals(
                "${config.id.value} correct-answer mark",
                exam.marksPerCorrectInTwelfths,
                config.marksPerCorrectInTwelfths
            )
            assertEquals(
                "${config.id.value} penalty",
                exam.negativeMarkInTwelfths,
                config.negativeMarkInTwelfths
            )
        }
    }

    @Test
    fun `the generic paper is unchanged and still draws every topic`() {
        val generic = catalog.tests().first()
        assertEquals("percentage_aptitude_15", generic.id.value)
        assertEquals(null, generic.examId)
        assertEquals(15, build(generic).map { it.topic }.distinct().size)
    }

    @Test
    fun `the Railway papers deduct exactly one third and stay exact`() {
        listOf("rrb_ntpc", "rrb_group_d").forEach { value ->
            val exam = exams.examById(ExamId(value))!!
            assertEquals(
                "$value should deduct one third",
                4,
                exam.negativeMarkInTwelfths
            )
            val config = catalog.testForExam(exam.id)!!
            val built = build(config)
            val wrongEveryQuestion = com.atlabs.percentpro.content.MockTestScore.of(
                config = config,
                totalQuestions = built.size,
                correct = 0,
                incorrect = built.size
            )
            // Three wrong answers must cost exactly one mark, which is the whole
            // reason for scoring in twelfths.
            val three = com.atlabs.percentpro.content.MockTestScore.of(
                config = config,
                totalQuestions = 3,
                correct = 0,
                incorrect = 3
            )
            assertEquals(-1.0, three.score, 1e-9)
            assertTrue(wrongEveryQuestion.score < 0)
        }
    }

    // ---- catalog integrity -------------------------------------------------

    @Test
    fun `the exam with an unconfirmed pattern offers no paper`() {
        val unconfirmed = exams.exams().filterNot { it.isAvailable }
        unconfirmed.forEach { exam ->
            assertEquals(
                "${exam.id} is unavailable but has a paper",
                null,
                catalog.testForExam(exam.id)
            )
            assertEquals(
                "${exam.id} is unavailable but still shows a question count",
                0,
                exam.realSectionQuestionCount
            )
        }
        assertEquals(
            "the list of withheld exams changed; check the reason string still fits",
            1,
            unconfirmed.size
        )
    }

    @Test
    fun `every confirmed exam has exactly one paper and a difficulty mix`() {
        exams.available().forEach { exam ->
            val config = catalog.testForExam(exam.id)
            assertNotNull("${exam.id.value} has no paper", config)
            assertEquals(exam.id, config!!.examId)
            assertEquals(
                "${exam.id.value} has an incomplete difficulty mix",
                QuestionDifficulty.entries.size,
                exam.difficultyMix.size
            )
            val total = exam.difficultyMix.values.sum()
            assertTrue(
                "${exam.id.value} mix sums to $total",
                kotlin.math.abs(total - 1.0) < 1e-9
            )
        }
    }

    @Test
    fun `every paper and every tag refers to an exam the catalog defines`() {
        val known = exams.exams().map { it.id }.toSet()
        catalog.tests().forEach { config ->
            config.examId?.let {
                assertTrue("${config.id.value} names unknown exam $it", it in known)
            }
        }
        repository.availableTopics().forEach { topic ->
            repository.questionsFor(topic).forEach { question ->
                question.examIds.forEach {
                    assertTrue("${question.id} is tagged for unknown exam $it", it in known)
                }
            }
        }
    }

    @Test
    fun `the generic paper is still the first entry`() {
        // Several tools and tests treat the head of the list as "the shipped
        // paper", so exam papers must be appended rather than prepended.
        assertEquals("percentage_aptitude_15", catalog.tests().first().id.value)
        assertFalse(catalog.tests().first().examId != null)
    }
}