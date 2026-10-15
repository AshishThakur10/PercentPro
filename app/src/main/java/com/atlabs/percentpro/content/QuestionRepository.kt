package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionId

/**
 * Read-only access to the question bank, with a validation pass.
 *
 * Malformed questions are dropped here rather than allowed to reach a session.
 * [QuestionBankTest] asserts nothing is dropped, so this filter is a safety net
 * rather than a way to hide content mistakes.
 */
class QuestionRepository(private val bank: QuestionBank = QuestionBank()) {

    private val validByTopic: Map<LessonId, List<Question>> by lazy {
        bank.all()
            .filter { it.isValid }
            .groupBy { it.topic }
    }

    /** Topics that actually have questions, in path order. */
    fun availableTopics(): List<LessonId> =
        LessonId.entries.filter { validByTopic[it].orEmpty().isNotEmpty() }

    fun questionsFor(topic: LessonId): List<Question> = validByTopic[topic].orEmpty()

    fun questionCountFor(topic: LessonId): Int = questionsFor(topic).size

    fun hasQuestions(topic: LessonId): Boolean = questionCountFor(topic) > 0

    fun find(topic: LessonId, id: QuestionId): Question? =
        questionsFor(topic).firstOrNull { it.id == id }

    /**
     * Looks a question up without knowing its topic, needed when restoring a
     * stored paper that records ids and order but not lesson ids.
     */
    fun findAcrossTopics(id: QuestionId): Question? =
        availableTopics().firstNotNullOfOrNull { topic -> find(topic, id) }
}