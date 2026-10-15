package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.MockTestSubmission
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionId

/** Palette state of a single question in an exam paper. */
enum class QuestionPaletteState {
    NOT_VISITED,
    VISITED_UNANSWERED,
    ANSWERED,
    MARKED_REVIEW,
    ANSWERED_AND_MARKED_REVIEW;

    val isAnswered: Boolean
        get() = this == ANSWERED || this == ANSWERED_AND_MARKED_REVIEW

    val isMarkedForReview: Boolean
        get() = this == MARKED_REVIEW || this == ANSWERED_AND_MARKED_REVIEW
}

/**
 * One attempt at a mock test, held as an immutable value.
 *
 * The clock is not stored here at all. Only the absolute deadline is, and the
 * remaining time is always recomputed from it against system time. That is what
 * makes backgrounding, rotation and a process restart harmless: there is no
 * counter that can drift, be paused or be reset by recomposition.
 */
data class MockTestSession(
    val sessionId: String,
    val config: MockTestConfig,
    val questions: List<Question>,
    val startedAtEpochMillis: Long,
    val deadlineEpochMillis: Long,
    val currentIndex: Int = 0,
    /** Question id to the option index the learner picked. */
    val answers: Map<QuestionId, Int> = emptyMap(),
    val markedForReview: Set<QuestionId> = emptySet(),
    val visitedQuestionIds: Set<QuestionId> = emptySet(),
    val submission: MockTestSubmission? = null
) {

    val questionCount: Int get() = questions.size

    val currentQuestion: Question? get() = questions.getOrNull(currentIndex)

    val questionNumber: Int get() = if (questions.isEmpty()) 0 else currentIndex + 1

    val isSubmitted: Boolean get() = submission != null

    val isFirstQuestion: Boolean get() = currentIndex <= 0

    val isLastQuestion: Boolean get() = questions.isNotEmpty() && currentIndex == questions.lastIndex

    val answeredCount: Int get() = questions.count { it.id in answers }

    val unattemptedCount: Int
        get() = (questions.size - answeredCount).coerceAtLeast(0)

    val correctCount: Int
        get() = questions.count { it.id in answers && it.isCorrectAnswer(answers.getValue(it.id)) }

    val incorrectCount: Int
        get() = questions.count { it.id in answers && !it.isCorrectAnswer(answers.getValue(it.id)) }

    val markedForReviewCount: Int get() = questions.count { it.id in markedForReview }

    val attemptedCount: Int get() = answeredCount

    /**
     * Remaining time on the clock. Derived from the absolute deadline, so this is
     * correct at any moment and can never drift below zero.
     */
    fun remainingMillis(nowEpochMillis: Long): Long =
        (deadlineEpochMillis - nowEpochMillis).coerceAtLeast(0L)

    fun remainingMinutes(nowEpochMillis: Long): Int =
        (remainingMillis(nowEpochMillis) / 60_000L).toInt()

    fun remainingSeconds(nowEpochMillis: Long): Int =
        ((remainingMillis(nowEpochMillis) % 60_000L) / 1_000L).toInt()

    fun hasExpired(nowEpochMillis: Long): Boolean = remainingMillis(nowEpochMillis) <= 0L

    fun selectedOptionFor(questionId: QuestionId): Int? = answers[questionId]

    fun isAnswered(questionId: QuestionId): Boolean = answers.containsKey(questionId)

    fun isMarked(questionId: QuestionId): Boolean = questionId in markedForReview

    /**
     * Palette state for a question.
     *
     * Order matters: an answered question that is also flagged for review gets
     * its own state so the learner can tell the two conditions apart.
     */
    fun paletteStateFor(questionId: QuestionId): QuestionPaletteState {
        val answered = questionId in answers
        val marked = questionId in markedForReview
        return when {
            answered && marked -> QuestionPaletteState.ANSWERED_AND_MARKED_REVIEW
            marked -> QuestionPaletteState.MARKED_REVIEW
            answered -> QuestionPaletteState.ANSWERED
            questionId in visitedQuestionIds -> QuestionPaletteState.VISITED_UNANSWERED
            else -> QuestionPaletteState.NOT_VISITED
        }
    }

    fun paletteStates(): List<QuestionPaletteState> = questions.map { paletteStateFor(it.id) }

    // ---- mutations ---------------------------------------------------------
    // Every one returns a new session and refuses to act once the test has been
    // submitted, so a late tap cannot change a frozen paper.

    /** Moves to a question and records it as visited. */
    fun goTo(index: Int): MockTestSession {
        if (isSubmitted || questions.isEmpty()) return this
        val target = index.coerceIn(0, questions.lastIndex)
        val question = questions[target]
        return copy(
            currentIndex = target,
            visitedQuestionIds = visitedQuestionIds + question.id
        )
    }

    fun next(): MockTestSession = goTo(currentIndex + 1)

    fun previous(): MockTestSession = goTo(currentIndex - 1)

    /** Selects an option for the current question, replacing any previous pick. */
    fun selectOption(optionIndex: Int): MockTestSession {
        if (isSubmitted) return this
        val question = currentQuestion ?: return this
        if (optionIndex !in question.options.indices) return this
        return copy(
            answers = answers + (question.id to optionIndex),
            visitedQuestionIds = visitedQuestionIds + question.id
        )
    }

    /** Clears only the current question's answer. */
    fun clearCurrentResponse(): MockTestSession {
        if (isSubmitted) return this
        val question = currentQuestion ?: return this
        if (question.id !in answers) return this
        return copy(answers = answers - question.id)
    }

    /** Works whether or not the question has an answer. */
    fun toggleReviewForCurrent(): MockTestSession {
        if (isSubmitted) return this
        val question = currentQuestion ?: return this
        val updated = if (question.id in markedForReview) {
            markedForReview - question.id
        } else {
            markedForReview + question.id
        }
        return copy(
            markedForReview = updated,
            visitedQuestionIds = visitedQuestionIds + question.id
        )
    }

    /** Freezes the paper. Idempotent: the first submission wins. */
    fun submit(submission: MockTestSubmission): MockTestSession =
        if (isSubmitted) this else copy(submission = submission)

    fun score(): MockTestScore = MockTestScore.from(this)

    companion object {
        fun start(
            sessionId: String,
            config: MockTestConfig,
            questions: List<Question>,
            startedAtEpochMillis: Long
        ): MockTestSession = MockTestSession(
            sessionId = sessionId,
            config = config,
            questions = questions,
            startedAtEpochMillis = startedAtEpochMillis,
            deadlineEpochMillis = startedAtEpochMillis + config.durationMillis,
            visitedQuestionIds = questions.firstOrNull()?.id?.let { setOf(it) } ?: emptySet()
        )
    }
}

/** Questions grouped by topic, for building a paper. */
fun List<Question>.topicsInOrder(): List<LessonId> = map { it.topic }.distinct()