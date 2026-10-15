package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.PracticeResult
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionId

/**
 * One practice run, held as an immutable value.
 *
 * Every operation returns a new session rather than mutating in place, which is
 * what lets submitted answers survive moving between questions: the answers live
 * in a map keyed by [QuestionId], not on whichever question happens to be showing.
 */
data class PracticeSession(
    val topic: LessonId,
    val questions: List<Question>,
    val currentIndex: Int = 0,
    /** Question id to the option index the learner submitted. */
    val answers: Map<QuestionId, Int> = emptyMap()
) {

    val questionCount: Int get() = questions.size

    val isEmpty: Boolean get() = questions.isEmpty()

    val currentQuestion: Question? get() = questions.getOrNull(currentIndex)

    val questionNumber: Int get() = if (questions.isEmpty()) 0 else currentIndex + 1

    val isFirstQuestion: Boolean get() = currentIndex <= 0

    val isLastQuestion: Boolean get() = questions.isNotEmpty() && currentIndex == questions.lastIndex

    val answeredCount: Int get() = answers.size

    val unansweredCount: Int get() = (questions.size - answers.size).coerceAtLeast(0)

    val correctCount: Int get() = questions.count { it.id in answers && isCorrect(it.id) }

    val incorrectCount: Int get() = questions.count { it.id in answers && !isCorrect(it.id) }

    /**
     * Correct answers as a share of all questions asked, so unanswered questions
     * count against the score. Integer arithmetic only.
     */
    val accuracyPercent: Int get() = PracticeResult.accuracyOf(correctCount, questions.size)

    /** 0f on the first question, 1f once the last one has been submitted. */
    val progress: Float
        get() = if (questions.isEmpty()) 0f else answeredCount.toFloat() / questions.size

    fun isAnswered(questionId: QuestionId): Boolean = answers.containsKey(questionId)

    fun submittedIndexFor(questionId: QuestionId): Int? = answers[questionId]

    fun isCorrect(questionId: QuestionId): Boolean {
        val selected = answers[questionId] ?: return false
        val question = questions.firstOrNull { it.id == questionId } ?: return false
        return question.isCorrectAnswer(selected)
    }

    fun goTo(index: Int): PracticeSession =
        if (questions.isEmpty()) this
        else copy(currentIndex = index.coerceIn(0, questions.lastIndex))

    fun next(): PracticeSession = goTo(currentIndex + 1)

    fun previous(): PracticeSession = goTo(currentIndex - 1)

    /**
     * Records the answer for the current question.
     *
     * Deliberately a no-op when the question is already answered, so a learner
     * cannot resubmit and be scored twice, and when the option index is out of
     * range.
     */
    fun submit(optionIndex: Int): PracticeSession {
        val question = currentQuestion ?: return this
        if (isAnswered(question.id)) return this
        if (optionIndex !in question.options.indices) return this
        return copy(answers = answers + (question.id to optionIndex))
    }

    /**
     * Freezes the session into a stored result. Unanswered questions are counted
     * honestly rather than treated as skipped.
     */
    fun toResult(completedAtEpochMillis: Long): PracticeResult = PracticeResult.of(
        topic = topic,
        completedAtEpochMillis = completedAtEpochMillis,
        total = questions.size,
        correct = correctCount,
        incorrect = incorrectCount,
        unanswered = unansweredCount
    )

    companion object {
        fun empty(topic: LessonId): PracticeSession = PracticeSession(topic, emptyList())
    }
}