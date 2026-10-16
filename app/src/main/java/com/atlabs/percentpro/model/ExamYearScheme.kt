package com.atlabs.percentpro.model

/**
 * The published scheme of one examination over a span of years.
 *
 * A span rather than a single year because most of these schemes have not moved
 * in years: SSC CHSL Tier 1 has been 25 questions at two marks with half a mark
 * deducted since the exam began. Storing one row per year would mean restating
 * the same numbers a dozen times and giving a future correction a dozen places
 * to be applied inconsistently.
 *
 * This is the scheme the Commission published, not a claim that any question in
 * the app came out of a paper held in that year. A year paper is built from
 * questions tagged for the examination and scored under the scheme that was in
 * force; see [Provenance] for what a tag does and does not claim.
 */
data class ExamYearScheme(
    val examId: ExamId,

    /** First year this scheme applied, inclusive. */
    val fromYear: Int,

    /**
     * Last year this scheme applied, inclusive.
     *
     * A null upper bound means "still current", so a scheme that has never been
     * superseded does not need editing each time the calendar turns over.
     */
    val toYear: Int?,

    /** Questions the real section held in these years. */
    val sectionQuestionCount: Int,

    val marksPerCorrectInTwelfths: Int,
    val negativeMarkInTwelfths: Int,

    /** Whole-paper allowance, or 0 when the section is timed on its own. */
    val paperDurationMinutes: Int,
    val sectionDurationMinutes: Int,
    val hasSectionalTimer: Boolean,

    /**
     * Whether every figure here has been confirmed against the official notice.
     *
     * A false scheme is not shown and not offered. Better an acknowledged gap in
     * the archive than a paper that quietly teaches the wrong penalty.
     */
    val isVerified: Boolean = true
) {
    /** How many years this span covers, for reporting a gap's size. */
    fun yearsInSpan(): Int = (lastYear - fromYear + 1).coerceAtLeast(0)

    fun covers(year: Int): Boolean =
        year >= fromYear && (toYear == null || year <= toYear)

    val lastYear: Int get() = toYear ?: ExamYearScheme.OPEN_ENDED

    val marksPerCorrect: Double get() = marksPerCorrectInTwelfths / MockTestConfig.MARKS_UNIT
    val negativeMark: Double get() = negativeMarkInTwelfths / MockTestConfig.MARKS_UNIT

    val sectionMaxMarks: Double
        get() = (sectionQuestionCount * marksPerCorrectInTwelfths) / MockTestConfig.MARKS_UNIT

    /** "2017–2026" or "2022 onwards", for a label. */
    fun yearRangeLabel(): String = if (toYear == null) {
        "$fromYear onwards"
    } else if (fromYear == toYear) {
        "$fromYear"
    } else {
        "$fromYear–$toYear"
    }

    companion object {
        /** Stands in for a scheme that is still running. */
        const val OPEN_ENDED = 9999
    }
}

/** One entry a reader can start, as shown on the year list. */
data class ExamYearPaper(
    val examId: ExamId,

    /**
     * The year this paper represents, or null for a span.
     *
     * A span covers every year in its range, because those years share one
     * scheme and one pool of questions: listing 2020 through 2026 separately
     * would offer the same paper nine times over.
     */
    val year: Int?,

    val scheme: ExamYearScheme,
    val questionCount: Int
) {
    val isSpan: Boolean get() = year == null

    /** Stable id, also the exam-paper key used by the mock test engine. */
    val id: String
        get() = "exam_${examId.value}" + (year?.let { "_$it" } ?: "_${scheme.fromYear}")
}