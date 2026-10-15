package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.DifficultyFilter
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.PracticeMode
import com.atlabs.percentpro.model.PracticePlan
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionId

/**
 * Assembles targeted practice from the existing question bank.
 *
 * It reuses the bank and the ordinary [PracticeSession] engine rather than
 * defining a second MCQ implementation: a plan is just a list of question ids
 * that the existing session screen already knows how to run.
 *
 * Every rule is about producing something runnable. A topic with no questions,
 * a difficulty nothing was authored at, or a mixed request naming unknown
 * topics all produce an empty plan rather than an exception, and the caller
 * shows an honest empty state.
 */
object PracticePlanner {

    /** Questions in a targeted run, so a session stays comparable in length. */
    const val DEFAULT_TARGETED_LENGTH = 10

    /** Questions in a mixed review run. */
    const val DEFAULT_MIXED_LENGTH = 10

    /** A topic needs this many questions before a filtered run is offered. */
    const val MIN_TOPIC_QUESTIONS = 1

    /**
     * A single-topic run, optionally filtered to one difficulty.
     *
     * Falls back to the unfiltered topic questions when the requested difficulty
     * has nothing, because an empty screen helps nobody; the returned plan then
     * carries [DifficultyFilter.ANY] so the caller can say so honestly.
     */
    fun planTopic(
        topic: LessonId,
        questionsFor: (LessonId) -> List<Question>,
        difficulty: DifficultyFilter = DifficultyFilter.ANY,
        limit: Int = DEFAULT_TARGETED_LENGTH
    ): PracticePlan {
        val available = questionsFor(topic).filter { it.isValid }
        if (available.isEmpty()) return emptyPlan(PracticeMode.TOPIC, difficulty, topic)

        val filtered = available.filter { difficulty.matches(it.difficulty) }
        if (filtered.isEmpty()) {
            val fallback = available.take(limit.coerceAtLeast(1))
            return PracticePlan(
                mode = PracticeMode.TOPIC,
                difficultyFilter = DifficultyFilter.ANY,
                questionIds = fallback.map { it.id },
                topicIds = listOf(topic),
                primaryTopic = topic
            )
        }

        return PracticePlan(
            mode = PracticeMode.TOPIC,
            difficultyFilter = difficulty,
            questionIds = filtered.take(limit.coerceAtLeast(1)).map { it.id },
            topicIds = listOf(topic),
            primaryTopic = topic
        )
    }

    /**
     * A mixed run across several topics, round-robin so no topic dominates.
     *
     * Topics are visited in the given order and each contributes one question per
     * pass, which keeps the spread even when one topic has far more questions
     * than another. Ids are de-duplicated across the whole plan.
     */
    fun planMixedReview(
        topics: List<LessonId>,
        questionsFor: (LessonId) -> List<Question>,
        difficulty: DifficultyFilter = DifficultyFilter.ANY,
        limit: Int = DEFAULT_MIXED_LENGTH
    ): PracticePlan {
        val target = limit.coerceAtLeast(1)
        val pools = topics
            .map { topic ->
                topic to questionsFor(topic)
                    .filter { it.isValid }
                    .filter { difficulty.matches(it.difficulty) }
            }
            .filter { (_, questions) -> questions.isNotEmpty() }

        if (pools.isEmpty()) return emptyPlan(PracticeMode.MIXED_REVIEW, difficulty, null)

        val selected = mutableListOf<Question>()
        val seen = mutableSetOf<QuestionId>()
        var poolIndex = 0
        while (selected.size < target && poolIndex < MAX_ROUNDS) {
            var addedThisPass = false
            pools.forEach { (_, pool) ->
                if (selected.size >= target) return@forEach
                val next = pool.firstOrNull { it.id !in seen } ?: return@forEach
                seen.add(next.id)
                selected.add(next)
                addedThisPass = true
            }
            if (!addedThisPass) break
            poolIndex++
        }

        if (selected.isEmpty()) return emptyPlan(PracticeMode.MIXED_REVIEW, difficulty, null)

        val usedTopics = pools.filter { (_, pool) ->
            pool.any { question -> selected.any { it.id == question.id } }
        }.map { it.first }

        return PracticePlan(
            mode = PracticeMode.MIXED_REVIEW,
            difficultyFilter = difficulty,
            questionIds = selected.map { it.id },
            topicIds = usedTopics,
            primaryTopic = usedTopics.firstOrNull()
        )
    }

    private fun emptyPlan(
        mode: PracticeMode,
        difficulty: DifficultyFilter,
        topic: LessonId?
    ) = PracticePlan(
        mode = mode,
        difficultyFilter = difficulty,
        questionIds = emptyList(),
        topicIds = listOfNotNull(topic),
        primaryTopic = topic
    )

    /** Guards against a pathological round-robin when a pool repeats ids. */
    private const val MAX_ROUNDS = 200
}