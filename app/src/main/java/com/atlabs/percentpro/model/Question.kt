package com.atlabs.percentpro.model

/** Every question in this phase is a four-option multiple choice question. */
const val OPTION_COUNT = 4

/**
 * Stable identifier for a question. Positions in a list are not identifiers:
 * reordering the bank must not change what has already been answered.
 */
@JvmInline
value class QuestionId(val value: String)

enum class QuestionDifficulty {
    EASY,
    MEDIUM,

    /**
     * Multi-step or multi-percentage reasoning.
     *
     * Added in Phase 6 alongside [Difficulty.HARD]. Nothing persists a
     * difficulty, so adding the level cannot invalidate saved data.
     */
    HARD
}

/**
 * One practice question with both of its translations.
 *
 * [correctIndex] is the index into [options] in both languages, since the option
 * order is fixed and identical across them.
 *
 * [provenance] says which examinations this question is written for. It is empty
 * for a question that is not tied to any exam, which is a normal state and not a
 * gap: a topic drill needs no exam tag to be useful.
 */
data class Question(
    val id: QuestionId,
    val topic: LessonId,
    val difficulty: QuestionDifficulty,
    val prompt: LocalizedText,
    val options: List<LocalizedText>,
    val correctIndex: Int,
    val explanation: LocalizedText,
    val provenance: List<Provenance> = emptyList()
) {

    /**
     * Content checks applied before a question is ever shown. Badly authored
     * questions are filtered out rather than crashing a practice session.
     */
    val isValid: Boolean
        get() = id.value.isNotBlank() &&
            options.size == OPTION_COUNT &&
            correctIndex in options.indices &&
            options.all { it.isComplete } &&
            // Duplicate options make a question ambiguous rather than hard.
            options.distinctBy { it.english }.size == OPTION_COUNT &&
            prompt.isComplete &&
            explanation.isComplete &&
            // An incomplete tag is a content fault like any other, so the
            // question goes with it rather than showing a blank label.
            provenance.all { it.isComplete }

    fun optionAt(index: Int, language: AppLanguage): String? =
        options.getOrNull(index)?.forLanguage(language)

    fun correctOption(language: AppLanguage): String =
        options[correctIndex].forLanguage(language)

    fun isCorrectAnswer(index: Int): Boolean = index == correctIndex

    /** Whether this question is written for the given examination. */
    fun isFrom(examId: ExamId): Boolean = provenance.any { it.examId == examId }

    /** The tag shown on screen; the first is the primary one. */
    val primaryProvenance: Provenance? get() = provenance.firstOrNull()

    /** Examinations this question is tagged for, without repeats. */
    val examIds: List<ExamId> get() = provenance.examIds()
}