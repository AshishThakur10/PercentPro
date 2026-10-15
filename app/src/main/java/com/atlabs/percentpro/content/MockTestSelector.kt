package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionId

/**
 * Chooses which questions make up a mock test.
 *
 * Selection is round-robin across the available topics, taking questions in bank
 * order. That gives an even spread across topics and, because nothing is
 * randomised, the same test every time — so a stored result can always be shown
 * next to the same questions.
 */
object MockTestSelector {

    fun select(
        config: MockTestConfig,
        topics: List<LessonId>,
        questionsByTopic: (LessonId) -> List<Question>
    ): List<Question> {
        if (config.questionCount <= 0) return emptyList()

        val pools = topics.map { questionsByTopic(it).toMutableList() }
        val selected = mutableListOf<Question>()
        val seen = mutableSetOf<QuestionId>()
        var allPoolsExhausted = false

        // Keep taking one question from each topic in turn until the paper is
        // full or every pool has run out.
        while (selected.size < config.questionCount && !allPoolsExhausted) {
            allPoolsExhausted = true
            pools.forEach { pool ->
                if (selected.size >= config.questionCount) return@forEach
                val next = pool.firstOrNull { question -> question.id !in seen }
                if (next != null) {
                    seen.add(next.id)
                    pool.remove(next)
                    selected.add(next)
                    allPoolsExhausted = false
                }
            }
        }
        return selected
    }
}