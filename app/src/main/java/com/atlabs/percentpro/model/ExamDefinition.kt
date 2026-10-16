package com.atlabs.percentpro.model

import androidx.annotation.StringRes

/**
 * Stable identifier for a government examination.
 *
 * Values are lowercase snake_case so they are safe to embed in the practice
 * session id and in the destination key, which are persisted.
 */
@JvmInline
value class ExamId(val value: String) {
    init {
        require(value.isNotBlank()) { "an exam id cannot be blank" }
        require(PATTERN.matches(value)) {
            "exam id '$value' must be lowercase letters, digits and underscores"
        }
    }

    private companion object {
        val PATTERN = Regex("^[a-z0-9_]+$")
    }
}

/**
 * The published shape of one paper of a real government examination.
 *
 * This is the public description of the exam, not a copy of it. Nothing here
 * reproduces an examination paper: it records how many questions the real
 * section holds, how it is marked and how long it runs, so a learner can see
 * what the real thing looks like before attempting a drill.
 *
 * Marks are in twelfths for the same reason [MockTestConfig] uses them: the
 * Railway papers deduct exactly one third, which is only a whole number of
 * marks in twelfths.
 */
data class ExamDefinition(
    val id: ExamId,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,

    /** The paper this definition describes, e.g. "Tier 1" or "CBT-1". */
    val paper: LocalizedText,

    /** The part of the paper the percentage drill is drawn from. */
    val section: LocalizedText,

    /** Questions the real examination asks in this section. */
    val realSectionQuestionCount: Int,

    val marksPerCorrectInTwelfths: Int,
    val negativeMarkInTwelfths: Int,

    /**
     * Minutes the whole paper runs for.
     *
     * [sectionDurationMinutes] is what the reader actually has to budget, and
     * it is not the same thing: SSC CHSL gives fifteen minutes per section
     * inside a sixty minute paper, while SSC CGL publishes one sixty minute
     * allowance for the whole of Tier 1.
     */
    val paperDurationMinutes: Int,

    /** Published per-section allowance, or 0 when the section shares the paper. */
    val sectionDurationMinutes: Int,

    /**
     * Whether the real examination times this section on its own.
     *
     * The banking prelims do, and it is the single biggest difference in how
     * they are attempted, so the drill says so rather than implying the timing
     * is the same as the SSC papers.
     */
    val hasSectionalTimer: Boolean,

    /**
     * Share of this exam's questions at each difficulty.
     *
     * What makes a paper feel like its exam rather than like a generic drill.
     * The Railway and SSC graduate papers lean harder than the banking
     * prelims, which are fast and forgiving.
     *
     * This is editorial: it describes the exam as this app teaches it, not a
     * published statistic, so it lives here beside the scheme rather than in the
     * question generator.
     */
    val difficultyMix: Map<QuestionDifficulty, Double>,

    /**
     * False while the published pattern could not be confirmed.
     *
     * An exam in this state is never offered as a paper and never shows a
     * count, because publishing a marking scheme nobody has checked is worse
     * than admitting the gap.
     */
    val isAvailable: Boolean = true
) {
    val marksPerCorrect: Double get() = marksPerCorrectInTwelfths / MockTestConfig.MARKS_UNIT
    val negativeMark: Double get() = negativeMarkInTwelfths / MockTestConfig.MARKS_UNIT
    val realSectionMaxMarks: Double
        get() = (realSectionQuestionCount * marksPerCorrectInTwelfths) / MockTestConfig.MARKS_UNIT
}