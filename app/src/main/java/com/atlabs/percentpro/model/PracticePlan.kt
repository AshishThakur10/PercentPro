package com.atlabs.percentpro.model

import com.atlabs.percentpro.content.QuestionRepository

/** What kind of practice run is being assembled. */
enum class PracticeMode {
    /** Every question comes from one topic. */
    TOPIC,

    /** Questions are drawn across several previously studied topics. */
    MIXED_REVIEW
}

/** Difficulty filter for a targeted run. */
enum class DifficultyFilter {
    ANY,
    EASY,
    MEDIUM,
    HARD;

    fun matches(difficulty: QuestionDifficulty): Boolean =
        this == ANY || difficulty == associatedDifficulty

    val associatedDifficulty: QuestionDifficulty?
        get() = when (this) {
            ANY -> null
            EASY -> QuestionDifficulty.EASY
            MEDIUM -> QuestionDifficulty.MEDIUM
            HARD -> QuestionDifficulty.HARD
        }
}

/**
 * A ready-to-run practice selection.
 *
 * Holding ids rather than [com.atlabs.percentpro.model.Question] keeps the plan
 * serialisable for navigation and lets the questions be resolved at the moment
 * the screen opens, so a bank update is picked up without rewriting saved state.
 */
data class PracticePlan(
    val mode: PracticeMode,
    val difficultyFilter: DifficultyFilter,
    val questionIds: List<QuestionId>,
    val topicIds: List<LessonId>,
    /** Topic the session header shows; for a mixed run, the first topic used. */
    val primaryTopic: LessonId?
) {
    val isEmpty: Boolean get() = questionIds.isEmpty()

    val size: Int get() = questionIds.size

    /** Stable key for navigation and for distinguishing history. */
    val key: String
        get() = buildString {
            append(mode.storageKey)
            append(':')
            append(difficultyFilter.name)
            append(':')
            append(topicIds.joinToString(",") { it.storageKey })
        }

    /** Prefix used on the practice session id, so history can tell runs apart. */
    val sessionPrefix: String
        get() = "targeted_${mode.storageKey}_${difficultyFilter.name.lowercase()}"
}

private val PracticeMode.storageKey: String
    get() = when (this) {
        PracticeMode.TOPIC -> "topic"
        PracticeMode.MIXED_REVIEW -> "mixed"
    }

/** Builds the question list for a plan. */
fun PracticePlan.resolveQuestions(repository: QuestionRepository): List<com.atlabs.percentpro.model.Question> =
    questionIds.mapNotNull { repository.findAcrossTopics(it) }