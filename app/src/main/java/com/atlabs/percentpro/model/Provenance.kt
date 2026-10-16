package com.atlabs.percentpro.model

/**
 * Where a question sits relative to a real government examination.
 *
 * The distinction between the two cases is the whole point of this type. A tag
 * that says a question *came from* a real paper is a claim a student could
 * check and, if it were wrong, a reason to lose trust in every other tag on the
 * screen. So the app never makes that claim without a source it can show, and
 * the weaker claim lives in its own case where it cannot be mistaken for the
 * stronger one.
 *
 * Nothing in either case reproduces an examination paper. See
 * [ExamCatalog] for the published scheme information these tags point at.
 */
sealed interface Provenance {

    val examId: ExamId

    /** The paper, e.g. "Tier 1" or "CBT 1". */
    val paper: LocalizedText

    /** The part of the paper, e.g. "Quantitative Aptitude". */
    val section: LocalizedText

    /**
     * Whether every free-text field is present in both languages.
     *
     * A half-translated tag is dropped with the question rather than shown, so
     * an English-only reader never sees a blank where Hindi should be.
     */
    val isComplete: Boolean

    /** Whether this tag names a specific calendar year. */
    val hasExactYear: Boolean

    /**
     * A question written for this app to match this exam's section.
     *
     * This claims the question is built the way that exam asks them: the
     * coverage, the difficulty mix and the speed the section demands. It claims
     * nothing about having appeared on a particular day, because nothing was
     * reproduced to make it.
     */
    data class ExamPattern(
        override val examId: ExamId,
        override val paper: LocalizedText,
        override val section: LocalizedText
    ) : Provenance {
        override val isComplete: Boolean
            get() = paper.isComplete && section.isComplete

        override val hasExactYear: Boolean get() = false
    }

    /**
     * A question verified against a real paper, with the source kept.
     *
     * Every field here is a claim about the world, so all of them are required.
     * [sourceUrl] is the reason this case exists: without somewhere to go and
     * look, a year and a question number are just assertions, and the honest
     * alternative is [ExamPattern].
     */
    data class VerifiedPyq(
        override val examId: ExamId,
        override val paper: LocalizedText,
        override val section: LocalizedText,
        val year: Int,
        /** Question set within the paper, where the exam publishes more than one. */
        val setLabel: LocalizedText?,
        /** Position in the paper, where the source records one. */
        val questionNumber: Int?,
        val sourceUrl: String
    ) : Provenance {
        override val isComplete: Boolean
            get() = paper.isComplete &&
                section.isComplete &&
                (setLabel?.isComplete ?: true) &&
                year in MIN_YEAR..MAX_YEAR &&
                sourceUrl.isNotBlank()

        override val hasExactYear: Boolean get() = true

        companion object {
            /**
             * The earliest year any of these examinations ran.
             *
             * A year outside this range is a typo in a data file rather than a
             * real paper, and is rejected at build time.
             */
            const val MIN_YEAR = 1994

            /** Far enough ahead to be obviously wrong. */
            const val MAX_YEAR = 2100
        }
    }
}

/** The exams this question is tagged for, in tag order, with no repeats. */
fun List<Provenance>.examIds(): List<ExamId> = distinctBy { it.examId }.map { it.examId }