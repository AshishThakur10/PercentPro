package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.ExamId
import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The year-by-year archive.
 *
 * These check the archive against the real bank and against the figures taken
 * from each Commission's own notice, because a paper labelled with a year is a
 * statement about what that year looked like.
 */
class ExamYearPapersTest {

    /** Pinned so a paper list that runs to the year 9999 cannot pass unnoticed. */
    private val exams = ExamCatalog(currentYear = 2026)
    private val catalog = MockTestCatalog(exams)
    private val repository = QuestionRepository()

    private fun build(config: MockTestConfig): List<Question> = MockTestSelector.select(
        config = config,
        topics = repository.availableTopics(),
        questionsByTopic = repository::questionsFor,
        difficultyMix = config.examId?.let { exams.examById(it)?.difficultyMix }
    )

    // ---- the recorded scheme table -----------------------------------------

    @Test
    fun `every confirmed scheme matches the published notice`() {
        // Pinned deliberately. These came from the SSC, RRB and IBPS notices, and
        // a future edit that silently changes one of them would otherwise pass
        // every other test in this file.
        val expected = listOf(
            // examId, from, to, sectionQ, correctTwelfths, wrongTwelfths, minutes
            Row("ssc_cgl", 2016, 2016, 25, 24, 6, 75),
            Row("ssc_cgl", 2017, null, 25, 24, 6, 60),
            Row("ssc_chsl", 2018, null, 25, 24, 6, 60),
            Row("ssc_gd", 2018, 2021, 25, 12, 0, 90),
            Row("ssc_gd", 2022, null, 20, 24, 3, 60),
            Row("rrb_ntpc", 2015, null, 30, 12, 4, 90),
            Row("rrb_group_d", 2018, null, 25, 12, 4, 90),
            Row("ibps_po", 2015, 2024, 35, 12, 3, 60),
            Row("ibps_po", 2025, null, 35, 12, 3, 60),
            Row("ibps_clerk", 2015, null, 35, 12, 3, 60)
        )
        val actual = exams.available().flatMap { exam ->
            exams.schemesFor(exam.id).map { scheme ->
                Row(
                    exam.id.value, scheme.fromYear, scheme.toYear,
                    scheme.sectionQuestionCount,
                    scheme.marksPerCorrectInTwelfths,
                    scheme.negativeMarkInTwelfths,
                    scheme.paperDurationMinutes
                )
            }
        }
        assertEquals(
            "the recorded scheme table changed; update this test only from a notice",
            expected.sortedBy { it.id },
            actual.sortedBy { it.id }
        )
    }

    @Test
    fun `a scheme span never overlaps another for the same exam`() {
        exams.available().forEach { exam ->
            val spans = exams.schemesFor(exam.id)
            spans.forEachIndexed { index, scheme ->
                spans.drop(index + 1).forEach { later ->
                    assertFalse(
                        "${exam.id.value}: ${scheme.yearRangeLabel()} overlaps " +
                            "${later.yearRangeLabel()}",
                        scheme.covers(later.fromYear)
                    )
                }
            }
        }
    }

    @Test
    fun `no scheme runs past the current year`() {
        exams.available().forEach { exam ->
            assertTrue(
                "${exam.id.value} claims years after 2026",
                exams.lastYear(exam.id)!! <= 2026
            )
        }
    }

    // ---- papers ------------------------------------------------------------

    @Test
    fun `every year paper can be filled from the real bank`() {
        exams.available().forEach { exam ->
            exams.papersFor(exam.id, includeEveryYear = true).forEach { paper ->
                val config = catalog.yearPaper(exam.id, paper.year!!)
                assertNotNull("${paper.id} could not be resolved", config)
                assertEquals(
                    "${paper.id} is short of questions",
                    config!!.questionCount,
                    build(config).size
                )
            }
        }
    }

    @Test
    fun `a year paper draws only questions tagged for its exam`() {
        exams.available().forEach { exam ->
            // The first *offerable* year, not the first recorded one: an exam
            // whose earliest span is unconfirmed has no paper for it.
            val firstYear = exams.papersFor(exam.id, includeEveryYear = true)
                .first().year!!
            val config = catalog.yearPaper(exam.id, firstYear)!!
            build(config).forEach { question ->
                assertTrue(
                    "${question.id} was not tagged for ${exam.id.value}",
                    question.isFrom(exam.id)
                )
            }
        }
    }

    @Test
    fun `no year paper repeats a question`() {
        exams.available().forEach { exam ->
            exams.papersFor(exam.id, includeEveryYear = true).forEach { paper ->
                val ids = build(catalog.yearPaper(exam.id, paper.year!!)!!).map { it.id }
                assertEquals("${paper.id} repeated a question", ids.size, ids.distinct().size)
            }
        }
    }

    @Test
    fun `a year paper keeps the difficulty mix of its examination`() {
        exams.available().forEach { exam ->
            val mix = exam.difficultyMix
            val config = catalog.yearPaper(exam.id, exams.lastYear(exam.id)!!)!!
            val built = build(config)
            com.atlabs.percentpro.model.QuestionDifficulty.entries.forEach { band ->
                val expected = mix.getValue(band) * config.questionCount
                val actual = built.count { it.difficulty == band }
                assertTrue(
                    "${exam.id.value} wanted about ${expected.toInt()} $band, drew $actual",
                    kotlin.math.abs(actual - expected) <= 1.0
                )
            }
        }
    }

    @Test
    fun `a year paper is scored under that year's marking scheme`() {
        exams.available().forEach { exam ->
            exams.papersFor(exam.id, includeEveryYear = true).forEach { paper ->
                val config = catalog.yearPaper(exam.id, paper.year!!)!!
                assertEquals(
                    "${paper.id} correct-answer mark",
                    paper.scheme.marksPerCorrectInTwelfths,
                    config.marksPerCorrectInTwelfths
                )
                assertEquals(
                    "${paper.id} penalty",
                    paper.scheme.negativeMarkInTwelfths,
                    config.negativeMarkInTwelfths
                )
                assertEquals("${paper.id} year", paper.year, config.year)
            }
        }
    }

    @Test
    fun `the 2016 and 2017 SSC CGL eras are recorded differently`() {
        // The whole reason spans exist: SSC CGL allowed seventy five minutes for
        // the whole of Tier 1 in 2016 and sixty from 2017. The card shows that
        // allowance, so it is the scheme that has to differ, not the drill clock.
        val early = exams.schemeFor(ExamId("ssc_cgl"), 2016)!!
        val later = exams.schemeFor(ExamId("ssc_cgl"), 2017)!!
        assertEquals(75, early.paperDurationMinutes)
        assertEquals(60, later.paperDurationMinutes)
        // Same marking either side, which is why only the timing is asserted here.
        assertEquals(early.marksPerCorrectInTwelfths, later.marksPerCorrectInTwelfths)
        assertEquals(early.negativeMarkInTwelfths, later.negativeMarkInTwelfths)
    }

    @Test
    fun `every year paper gets the same drill clock regardless of era`() {
        // A fifteen question paper held to a ninety minute clock teaches a
        // reader to abandon it, so the era's real allowance is shown on the card
        // instead of being used as the drill's timer.
        exams.available().forEach { exam ->
            exams.papersFor(exam.id, includeEveryYear = true).forEach { paper ->
                val config = catalog.yearPaper(exam.id, paper.year!!)!!
                assertEquals(
                    "${paper.id} clock",
                    15 * 60 * 1000L,
                    config.durationMillis
                )
            }
        }
    }

    // ---- what is deliberately not offered ----------------------------------

    @Test
    fun `an unconfirmed era yields no paper`() {
        // SSC GD was 100 questions worth 100 marks in 90 minutes before 2022 and
        // no notice giving that era's penalty could be confirmed. Rather than
        // guess at a figure a reader would be scored against, those years are
        // offered nothing.
        val gd = ExamId("ssc_gd")
        assertNull(catalog.yearPaper(gd, 2019))
        assertNull(catalog.yearPaper(gd, 2021))
        assertNotNull(catalog.yearPaper(gd, 2022))
        assertEquals(
            listOf(2018 to 2021),
            exams.unverifiedRanges(gd).map { it.fromYear to it.toYear!! }
        )
    }

    @Test
    fun `an unconfirmed era is still reported as a gap`() {
        // Hiding it would make the archive look complete when it is not.
        val gaps = exams.unverifiedRanges(ExamId("ssc_gd"))
        assertEquals(1, gaps.size)
        assertFalse(gaps.first().isVerified)
        assertEquals("the gap should cover the 2018-2021 era", 4, gaps.first().yearsInSpan())
        assertEquals(
            "the gap should be reported as unavailable",
            4,
            exams.available().first { it.id == ExamId("ssc_gd") }.let {
                exams.unverifiedRanges(it.id).sumOf { scheme -> scheme.yearsInSpan() }
            }
        )
    }

    @Test
    fun `a year the exam did not run yields no paper`() {
        assertNull(catalog.yearPaper(ExamId("ssc_chsl"), 2015))
        assertNull(catalog.yearPaper(ExamId("ssc_gd"), 2017))
    }

    @Test
    fun `a future year yields no paper`() {
        // An open-ended scheme must not claim every year to 9999.
        assertTrue(!exams.hasYear(ExamId("ssc_cgl"), 2030))
        assertNull(catalog.yearPaper(ExamId("ssc_cgl"), 2030))
    }

    @Test
    fun `the exam with no confirmed scheme is absent from the archive`() {
        // SSC MTS is held back, so it must not appear as a browsable archive.
        // Computed from the catalog rather than the UI helper, so this stays a
        // content test.
        val browsable = exams.available().filter { exam ->
            exams.papersFor(exam.id, includeEveryYear = true).isNotEmpty()
        }.map { it.id }
        assertFalse(browsable.contains(ExamId("ssc_mts")))
        assertTrue(browsable.contains(ExamId("ssc_cgl")))
        assertEquals(7, browsable.size)
    }

    // ---- eras versus years -------------------------------------------------

    @Test
    fun `eras collapse a stable run of years into one entry`() {
        val chsl = exams.papersFor(ExamId("ssc_chsl"))
        assertEquals("CHSL has one scheme, so one thing to practise", 1, chsl.size)
        assertTrue(chsl.first().isSpan)
        assertEquals(
            "every year behind the toggle",
            9,
            exams.papersFor(ExamId("ssc_chsl"), includeEveryYear = true).size
        )
    }

    @Test
    fun `an exam that changed scheme gets one era per scheme`() {
        val cgl = exams.papersFor(ExamId("ssc_cgl"))
        assertEquals(2, cgl.size)
        assertEquals(listOf(2016, 2017), cgl.map { it.scheme.fromYear })
        assertTrue(cgl.first().isSpan)
    }

    @Test
    fun `every listed year paper has a distinct id`() {
        val ids = exams.available().flatMap { exam ->
            exams.papersFor(exam.id, includeEveryYear = true).map { it.id }
        }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `a span and a single year of the same exam do not share an id`() {
        val span = exams.papersFor(ExamId("ssc_cgl")).first { it.isSpan }
        val year = exams.papersFor(ExamId("ssc_cgl"), includeEveryYear = true)
            .first { it.year == 2017 }
        assertFalse(span.id == year.id)
    }

    // ---- resolution and repetition -----------------------------------------

    @Test
    fun `the id the year list hands over must resolve to a paper`() {
        // The path a reader actually takes: the card in the year list supplies
        // the id, and MockTestExamViewModel.startTest resolves it. These two
        // used to build ids with different prefixes, so every tap produced an id
        // nothing could resolve and Start did nothing at all.
        exams.available().forEach { exam ->
            exams.papersFor(exam.id).forEach { paper ->
                val config = catalog.testById(MockTestId(paper.id))
                assertNotNull("${exam.id.value}: '${paper.id}' does not resolve", config)
                assertEquals("${paper.id} wrong exam", exam.id, config!!.examId)
            }
            exams.papersFor(exam.id, includeEveryYear = true).forEach { paper ->
                assertNotNull(
                    "${exam.id.value}: year id '${paper.id}' does not resolve",
                    catalog.testById(MockTestId(paper.id))
                )
            }
        }
    }

    @Test
    fun `every year list id is prefixed so it cannot be mistaken for a plain exam paper`() {
        exams.available().forEach { exam ->
            exams.papersFor(exam.id, includeEveryYear = true).forEach { paper ->
                assertTrue(
                    "'${paper.id}' lacks the year-paper prefix",
                    paper.id.startsWith(com.atlabs.percentpro.model.ExamYearPaper.YEAR_PAPER_ID_PREFIX)
                )
            }
        }
        // And the plain exam papers must not carry it.
        catalog.tests().filter { it.examId != null }.forEach {
            assertFalse(
                "'${it.id.value}' should not be prefixed as a year paper",
                it.id.value.startsWith(
                    com.atlabs.percentpro.model.ExamYearPaper.YEAR_PAPER_ID_PREFIX
                )
            )
        }
    }

    @Test
    fun `a year paper id resolves back to the same config`() {
        // This is the path the mock test engine and its saved-session restore
        // both take, so it has to round-trip or a stored paper is unrecoverable.
        val paper = exams.papersFor(ExamId("ibps_po"), includeEveryYear = true)
            .first { it.year == 2024 }
        val restored = catalog.testById(MockTestId(paper.id))
        assertNotNull("a year paper id did not resolve", restored)
        assertEquals(
            catalog.yearPaper(ExamId("ibps_po"), 2024),
            restored
        )
    }

    @Test
    fun `two year papers of one exam differ from each other`() {
        // Without the pool offset every year paper on a scheme would be the same
        // fifteen questions, and the archive would be one paper listed nine times.
        fun ids(year: Int) = build(catalog.yearPaper(ExamId("ssc_chsl"), year)!!)
            .map { it.id }.toSet()

        val first = ids(2018)
        val second = ids(2019)
        assertTrue("the two papers are identical", first != second)
        assertTrue(
            "the two papers overlap far too much: ${(first intersect second).size} of ${first.size}",
            (first intersect second).size <= 5
        )
    }

    @Test
    fun `a paper is reproducible across reads`() {
        // A saved result has to be showable next to the questions it was scored
        // against, so selection cannot depend on anything but the config.
        val config = catalog.yearPaper(ExamId("ssc_cgl"), 2019)!!
        assertEquals(build(config).map { it.id }, build(config).map { it.id })
    }

    @Test
    fun `an offset past the end of a pool still fills the paper`() {
        // A short pool reduced to nothing would silently drop a topic's whole
        // contribution rather than wrapping round.
        val config = catalog.yearPaper(ExamId("ssc_chsl"), 2018)!!.copy(poolOffset = 9_000)
        assertEquals(config.questionCount, build(config).size)
    }

    @Test
    fun `year papers do not appear in the mock test landing list`() {
        // The landing screen draws a card per entry; seventy more would make it
        // unusable, which is why year configs are synthesised instead of listed.
        val ids = MockTestCatalog().tests().map { it.id.value }
        assertEquals(8, ids.size)
        assertTrue(ids.none { it.startsWith("examyr_") })
    }

    @Test
    fun `the generic paper is still first`() {
        assertEquals("percentage_aptitude_15", catalog.tests().first().id.value)
    }

    @Test
    fun `a malformed year paper id resolves to nothing`() {
        listOf("examyr_", "examyr_ssc_cgl", "examyr_ssc_cgl_", "examyr_SSC_CGL_2019",
            "examyr_ssc-cgl_2019", "examyr_ssc_cgl_19x", "not_a_year_paper")
            .forEach { assertNull("$it should not resolve", catalog.testById(MockTestId(it))) }
    }

    @Test
    fun `a year paper id parses back to its exam and year`() {
        assertEquals(
            ExamId("ssc_chsl") to 2021,
            catalog.parseYearPaperId("examyr_ssc_chsl_2021")
        )
    }

    private data class Row(
        val id: String,
        val from: Int,
        val to: Int?,
        val sectionQ: Int,
        val correctTwelfths: Int,
        val wrongTwelfths: Int,
        val minutes: Int
    )
}