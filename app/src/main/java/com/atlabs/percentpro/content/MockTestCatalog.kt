package com.atlabs.percentpro.content

import com.atlabs.percentpro.R
import com.atlabs.percentpro.model.ExamId
import com.atlabs.percentpro.model.ExamYearPaper
import com.atlabs.percentpro.model.ExamYearScheme
import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.MockTestId

/**
 * Questions in an exam paper.
 *
 * The real sections run 20-35 questions, but this app teaches percentage and
 * nothing else, so a paper is a drill through the percentage part of a section.
 * The exam card says so in as many words rather than letting a fifteen question
 * paper imply a fifteen question section.
 *
 * Shared with [ExamCatalog], which derives the year-by-year papers from the same
 * figure, so a change to one cannot leave the other offering a different length.
 */
internal const val EXAM_PAPER_QUESTIONS = 15

/**
 * Minutes allowed for an exam paper.
 *
 * Deliberately not the real duration. The real section allows fifteen to
 * thirty minutes for twenty to thirty five questions; this paper has fifteen, so
 * it gets the same generous per-question allowance the real section implies.
 * Holding a short paper to a ninety minute clock teaches a reader to abandon it.
 */
private const val EXAM_PAPER_MINUTES = 15

/**
 * The papers shipped with the app.
 *
 * The first entry is the original generic percentage paper. The next group is
 * built from [ExamCatalog]: each takes its length, marking scheme and clock from
 * the real examination it is named after, and is drawn only from questions
 * tagged for that exam.
 *
 * Year papers are the third group. They are **not held in this list**: there are
 * over seventy of them and the landing screen draws a card per entry, so they are
 * synthesised on demand from the scheme history by [testById] and offered from
 * the year list instead. [tests] therefore stays short enough to render.
 *
 * None of these is a reproduction of a real paper. The questions are written for
 * this app to match how the exam asks them, and the marking and timing are the
 * published scheme, which is public information a learner is entitled to.
 *
 * Order matters: [percentage_aptitude_15] stays first because tests and tooling
 * address the head of this list as "the paper we ship".
 */
class MockTestCatalog(private val exams: ExamCatalog = ExamCatalog()) {

    fun tests(): List<MockTestConfig> = TESTS

    /** Resolves a paper by id, including the year papers built on demand. */
    fun testById(id: MockTestId): MockTestConfig? =
        tests().firstOrNull { it.id == id } ?: yearPaperById(id.value)

    fun testForExam(examId: ExamId): MockTestConfig? =
        tests().firstOrNull { it.examId == examId }

    // ---- year papers -------------------------------------------------------

    /**
     * Builds the paper for one exam and year.
     *
     * Returns null when the exam did not run that year, when no scheme covers
     * it, or when the covering scheme has not been confirmed against the
     * official notice. In the last case [ExamCatalog.unverifiedRanges] reports
     * the gap, so the reader sees that the archive is incomplete instead of
     * finding a year quietly absent.
     */
    fun yearPaper(examId: ExamId, year: Int): MockTestConfig? {
        val scheme = exams.schemeFor(examId, year) ?: return null
        if (!scheme.isVerified) return null
        // A year that has not happened yet has no paper to sit.
        if (!exams.hasYear(examId, year)) return null
        val exam = exams.examById(examId)?.takeIf { it.isAvailable } ?: return null
        return MockTestConfig(
            id = MockTestId(ExamYearPaper.idFor(examId, year)),
            titleRes = exam.titleRes,
            descriptionRes = exam.descriptionRes,
            questionCount = EXAM_PAPER_QUESTIONS,
            durationMillis = EXAM_PAPER_MINUTES * 60 * 1000L,
            marksPerCorrectInTwelfths = scheme.marksPerCorrectInTwelfths,
            negativeMarkInTwelfths = scheme.negativeMarkInTwelfths,
            examId = examId,
            year = year,
            poolOffset = offsetFor(examId, year, scheme)
        )
    }

    /**
     * Rotates the draw so two papers on the same scheme are not the same paper.
     *
     * Keyed on the year rather than drawn from a counter, so opening the same
     * year twice gives the same paper and a saved result still lines up with the
     * questions it was scored against.
     */
    private fun offsetFor(examId: ExamId, year: Int, scheme: ExamYearScheme): Int {
        val span = (scheme.lastYear - scheme.fromYear).coerceAtLeast(1)
        val indexFromStart = (year - scheme.fromYear).coerceAtLeast(0)
        return (indexFromStart + examId.value.length) % span.coerceAtLeast(1)
    }

    /** Parses a year paper id back into its exam and year. */
    fun parseYearPaperId(id: String): Pair<ExamId, Int>? = ExamYearPaper.parseId(id)

    private fun yearPaperById(id: String): MockTestConfig? =
        parseYearPaperId(id)?.let { (examId, year) -> yearPaper(examId, year) }

    private companion object {

        const val FIFTEEN_MINUTES = 15 * 60 * 1000L

        val TESTS = listOf(
            MockTestConfig(
                id = MockTestId("percentage_aptitude_15"),
                titleRes = R.string.mock_title_percentage_aptitude,
                descriptionRes = R.string.mock_description_percentage_aptitude,
                questionCount = 15,
                durationMillis = FIFTEEN_MINUTES,
                // +1 mark correct, -0.25 mark incorrect.
                marksPerCorrectInTwelfths = 12,
                negativeMarkInTwelfths = 3
            )
        ) + examPapers()
    }
}

/**
 * One paper per confirmed examination.
 *
 * Built from [ExamCatalog] rather than written out by hand so the marking
 * scheme on a paper cannot drift away from the scheme shown on its exam card.
 */
private fun examPapers(): List<MockTestConfig> {
    val minutes = EXAM_PAPER_MINUTES
    return ExamCatalog().available().map { exam ->
        MockTestConfig(
            id = MockTestId("exam_${exam.id.value}"),
            titleRes = exam.titleRes,
            descriptionRes = exam.descriptionRes,
            questionCount = EXAM_PAPER_QUESTIONS,
            durationMillis = minutes * 60 * 1000L,
            marksPerCorrectInTwelfths = exam.marksPerCorrectInTwelfths,
            negativeMarkInTwelfths = exam.negativeMarkInTwelfths,
            examId = exam.id
        )
    }
}