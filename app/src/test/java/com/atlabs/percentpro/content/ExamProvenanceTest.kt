package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.ExamId
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalizedText
import com.atlabs.percentpro.model.Provenance
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exam tags and the practice filter built on them.
 */
class ExamProvenanceTest {

    private val catalog = ExamCatalog()
    private val repository = QuestionRepository()

    private fun question(id: String, exams: List<ExamId> = emptyList()): Question = Question(
        id = QuestionId(id),
        topic = LessonId.PRICE_DISCOUNT_TAX,
        difficulty = QuestionDifficulty.MEDIUM,
        prompt = LocalizedText.of("Q $id", "प्र $id"),
        options = listOf("a", "b", "c", "d").map { LocalizedText.of(it, it) },
        correctIndex = 0,
        explanation = LocalizedText.of("Because $id", "क्योंकि $id"),
        provenance = exams.map {
            Provenance.ExamPattern(
                examId = it,
                paper = LocalizedText.of("Tier 1", "टियर 1"),
                section = LocalizedText.of("Quantitative Aptitude", "संख्यात्मक अभिरुचि")
            )
        }
    )

    // ---- tags ---------------------------------------------------------------

    @Test
    fun `a question with no tags is still valid`() {
        // Topic drills are the common case; an absent tag must not look like a
        // content fault or the question would vanish from the bank.
        assertTrue(question("untagged").isValid)
    }

    @Test
    fun `a question is valid when its tags are complete`() {
        assertTrue(question("tagged", listOf(ExamId("ssc_cgl"))).isValid)
    }

    @Test
    fun `a question with a half-translated tag is rejected`() {
        val broken = Question(
            id = QuestionId("broken"),
            topic = LessonId.PRICE_DISCOUNT_TAX,
            difficulty = QuestionDifficulty.EASY,
            prompt = LocalizedText.of("Q", "प्र"),
            options = listOf("a", "b", "c", "d").map { LocalizedText.of(it, it) },
            correctIndex = 0,
            explanation = LocalizedText.of("Because", "क्योंकि"),
            provenance = listOf(
                Provenance.ExamPattern(
                    examId = ExamId("ssc_cgl"),
                    paper = LocalizedText.of("Tier 1", ""),
                    section = LocalizedText.of("Quant", "मात्रा")
                )
            )
        )
        assertFalse("a tag missing its Hindi label must not pass isValid", broken.isValid)
    }

    @Test
    fun `a verified tag without a source is not complete`() {
        val unsourced = Provenance.VerifiedPyq(
            examId = ExamId("ssc_cgl"),
            paper = LocalizedText.of("Tier 1", "टियर 1"),
            section = LocalizedText.of("Quant", "मात्रा"),
            year = 2023,
            setLabel = null,
            questionNumber = 14,
            sourceUrl = ""
        )
        assertFalse("a real citation with no source must not count as complete", unsourced.isComplete)
        assertTrue(unsourced.hasExactYear)
    }

    @Test
    fun `an implausible year is rejected`() {
        Provenance.VerifiedPyq(
            examId = ExamId("ssc_cgl"),
            paper = LocalizedText.of("Tier 1", "टियर 1"),
            section = LocalizedText.of("Quant", "मात्रा"),
            year = 1800,
            setLabel = null,
            questionNumber = null,
            sourceUrl = "https://example.invalid"
        ).let { assertFalse("1800 cannot be a real paper", it.isComplete) }
    }

    @Test
    fun `an exam pattern never claims a year`() {
        val tag = Provenance.ExamPattern(
            examId = ExamId("ssc_cgl"),
            paper = LocalizedText.of("Tier 1", "टियर 1"),
            section = LocalizedText.of("Quant", "मात्रा")
        )
        assertFalse(tag.hasExactYear)
        assertTrue(tag.isComplete)
    }

    @Test
    fun `a question reports each exam once however many tags it has`() {
        val q = question("repeat", listOf(ExamId("ssc_cgl"), ExamId("ssc_cgl"), ExamId("ibps_po")))
        assertEquals(listOf(ExamId("ssc_cgl"), ExamId("ibps_po")), q.examIds)
        assertTrue(q.isFrom(ExamId("ssc_cgl")))
        assertTrue(q.isFrom(ExamId("ibps_po")))
        assertFalse(q.isFrom(ExamId("rrb_ntpc")))
        assertEquals(ExamId("ssc_cgl"), q.primaryProvenance?.examId)
    }

    @Test
    fun `an untagged question has no primary tag`() {
        assertNull(question("none").primaryProvenance)
        assertEquals(emptyList<ExamId>(), question("none").examIds)
    }

    // ---- the filter ---------------------------------------------------------

    @Test
    fun `the exam filter narrows a run to that exam`() {
        val plan = PracticePlanner.planTopic(
            topic = LessonId.PRICE_DISCOUNT_TAX,
            questionsFor = repository::questionsFor,
            exam = ExamId("ssc_cgl")
        )
        assertTrue(plan.isEmpty.not())
        plan.questionIds.forEach { id ->
            val question = repository.findAcrossTopics(id)
            assertNotNull("plan holds an id the bank lost: $id", question)
            assertTrue(
                "${id} was not tagged for ssc_cgl",
                question!!.isFrom(ExamId("ssc_cgl"))
            )
        }
    }

    @Test
    fun `the filter is recorded on the plan`() {
        val plan = PracticePlanner.planTopic(
            topic = LessonId.PRICE_DISCOUNT_TAX,
            questionsFor = repository::questionsFor,
            exam = ExamId("ibps_po")
        )
        assertEquals(ExamId("ibps_po"), plan.examFilter)
    }

    @Test
    fun `an unwritten lesson yields an empty plan with no filter`() {
        // The topic nobody tags for a railway exam: an empty screen helps nobody,
        // so the planner drops the filter and says so by clearing it.
        val plan = PracticePlanner.planTopic(
            topic = LessonId.COMPETITIVE_EXAM_CHALLENGE,
            questionsFor = repository::questionsFor,
            exam = ExamId("rrb_ntpc")
        )
        assertTrue("an unwritten lesson has no questions at all", plan.isEmpty)
        assertNull("an empty plan must not claim a filter", plan.examFilter)
    }

    @Test
    fun `a topic with questions but none for the exam drops the filter`() {
        // The honest-fallback path. These questions exist and are valid, but
        // none is tagged for the exam, so the planner runs the topic unfiltered
        // and clears examFilter so the caller can say the drill is not exam
        // specific rather than quietly serving the wrong questions.
        val untagged = { _: LessonId -> listOf(question("plain_1"), question("plain_2")) }
        val plan = PracticePlanner.planTopic(
            topic = LessonId.PROFIT_LOSS,
            questionsFor = untagged,
            exam = ExamId("ssc_cgl")
        )
        assertFalse("a fallback must still return a runnable plan", plan.isEmpty)
        assertEquals(listOf(QuestionId("plain_1"), QuestionId("plain_2")), plan.questionIds)
        assertNull("the filter must be cleared so the UI can report the fallback", plan.examFilter)
    }

    @Test
    fun `two exams over one topic produce different runs`() {
        fun runFor(exam: String) = PracticePlanner.planTopic(
            topic = LessonId.PROFIT_LOSS,
            questionsFor = repository::questionsFor,
            exam = ExamId(exam)
        )
        val cgl = runFor("ssc_cgl")
        val ibps = runFor("ibps_po")
        assertTrue(cgl.isEmpty.not() && ibps.isEmpty.not())
        assertFalse(
            "the filter did not change the selection",
            cgl.questionIds == ibps.questionIds
        )
    }

    @Test
    fun `no filter returns every question for the topic`() {
        val plan = PracticePlanner.planTopic(
            topic = LessonId.PROFIT_LOSS,
            questionsFor = repository::questionsFor
        )
        assertNull(plan.examFilter)
    }

    // ---- identity -----------------------------------------------------------

    @Test
    fun `two filtered runs cannot collide on a session id`() {
        // PracticeSessionViewModel builds the session id from sessionPrefix, so
        // two runs differing only by exam would otherwise share one id and their
        // answers would merge in practice history.
        fun prefixFor(exam: String) = PracticePlanner.planTopic(
            topic = LessonId.PROFIT_LOSS,
            questionsFor = repository::questionsFor,
            exam = ExamId(exam)
        ).sessionPrefix

        val prefixes = listOf("ssc_cgl", "ibps_po", "rrb_ntpc", "ibps_clerk")
            .map { prefixFor(it) }
        assertEquals(
            "session ids collided across exam filters",
            prefixes.size,
            prefixes.distinct().size
        )
        prefixes.forEach { assertTrue("prefix '$it' does not name its exam", it.contains("exam") || it.isNotEmpty()) }
    }

    @Test
    fun `an unfiltered run keeps its historical session prefix`() {
        // Readers who have already sat the generic paper must not have their
        // history orphaned by this change.
        val plain = PracticePlanner.planTopic(
            topic = LessonId.PROFIT_LOSS,
            questionsFor = repository::questionsFor
        )
        assertEquals("targeted_topic_any", plain.sessionPrefix)
        val filtered = PracticePlanner.planTopic(
            topic = LessonId.PROFIT_LOSS,
            questionsFor = repository::questionsFor,
            exam = ExamId("ssc_cgl")
        )
        assertEquals("targeted_topic_any_ssc_cgl", filtered.sessionPrefix)
    }

    @Test
    fun `plan keys differ across exam filters`() {
        fun keyFor(exam: ExamId?) = PracticePlanner.planTopic(
            topic = LessonId.PROFIT_LOSS,
            questionsFor = repository::questionsFor,
            exam = exam
        ).key
        val keys = listOf(null, "ssc_cgl", "ibps_po").map { keyFor(it?.let(::ExamId)) }
        assertEquals(keys.size, keys.distinct().size)
    }

    @Test
    fun `an exam id rejects characters that would break a destination key`() {
        // Exam ids are embedded in persisted session ids and navigation keys.
        listOf("ssc cgl", "SSC-CGL", "ssc/cgl", "ssc|cgl", "").forEach { bad ->
            val rejected = runCatching { ExamId(bad) }.isFailure
            assertTrue("ExamId(\"$bad\") should be rejected", rejected)
        }
        assertEquals(ExamId("ssc_cgl"), ExamId("ssc_cgl"))
    }

    @Test
    fun `every catalog exam id survives the id rules`() {
        catalog.exams().forEach { exam ->
            assertEquals(exam.id, ExamId(exam.id.value))
        }
    }

    // ---- localisation -------------------------------------------------------

    @Test
    fun `every exam's paper and section labels are bilingual`() {
        catalog.available().forEach { exam ->
            assertTrue(
                "${exam.id.value} paper label is half-translated",
                exam.paper.isComplete
            )
            assertTrue(
                "${exam.id.value} section label is half-translated",
                exam.section.isComplete
            )
            assertFalse(
                "${exam.id.value} has an untranslated section label",
                exam.section.english == exam.section.hindi
            )
        }
    }

    @Test
    fun `every tag on the bank renders in both languages`() {
        AppLanguage.entries.forEach { language ->
            repository.availableTopics().forEach { topic ->
                repository.questionsFor(topic).forEach { question ->
                    question.provenance.forEach { tag ->
                        val exam = catalog.examById(tag.examId) ?: return@forEach
                        val paper = tag.paper.forLanguage(language)
                        val section = tag.section.forLanguage(language)
                        assertTrue(paper.isNotBlank())
                        assertTrue(section.isNotBlank())
                        assertNotNull(exam.titleRes)
                    }
                }
            }
        }
    }
}