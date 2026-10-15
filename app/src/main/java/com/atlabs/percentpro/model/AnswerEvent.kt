package com.atlabs.percentpro.model

/**
 * Where an answer came from.
 *
 * Kept on the event so a metric can be attributed to practice or to a mock test
 * without guessing. The two are never pooled where that would mislead: practice
 * is untimed and usually easier, while a mock test is timed and mixed.
 */
enum class AnswerSource(val storageKey: String) {
    TOPIC_PRACTICE("practice"),
    MOCK_TEST("mock");

    companion object {
        private val byKey = entries.associateBy { it.storageKey }

        fun fromKey(key: String): AnswerSource? = byKey[key]
    }
}

/**
 * One submitted answer.
 *
 * This is the canonical record the whole insights system reads. Practice and
 * mock-test sessions both write here, so there is a single definition of "an
 * attempt" instead of one per feature.
 *
 * [attemptKey] is what makes counting safe: it is derived from the session and
 * the question, never from a timestamp, so re-submitting, restoring a session or
 * re-opening a saved result produces the same key and is ignored rather than
 * counted twice.
 */
data class AnswerEvent(
    val questionId: QuestionId,
    val topicKey: String,
    val difficulty: QuestionDifficulty,
    val wasCorrect: Boolean,
    val source: AnswerSource,
    val sessionId: String,
    val answeredAtEpochMillis: Long,
    val answeredOn: LocalDateKey
) {
    /** Stable identity for de-duplication across restarts. */
    val attemptKey: String get() = "${source.storageKey}|$sessionId|${questionId.value}"
}

/**
 * Answer events that arrived before this phase existed.
 *
 * Mock-test results record which option was chosen for every question, so their
 * answers can be recovered exactly. Topic practice saves only counts, with no
 * question ids, so its history cannot be reconstructed and is not invented.
 * Those practice sessions remain visible in the practice history screen; they
 * simply do not contribute to question-level analytics.
 */
enum class LegacySource {
    /** Recoverable from saved mock-test results. */
    RECOVERABLE,

    /** Counts only, with no question ids, so nothing can be recovered. */
    COUNTS_ONLY
}