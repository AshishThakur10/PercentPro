package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId

/**
 * Chooses which questions make up a mock test.
 *
 * Selection is round-robin across the available topics, taking questions in bank
 * order. That gives an even spread across topics and, because nothing is
 * randomised, the same test every time — so a stored result can always be shown
 * next to the same questions.
 *
 * When [MockTestConfig.examId] is set the pool is narrowed first to the questions
 * tagged for that examination, which is what makes an exam paper feel like its
 * exam rather than like the generic paper. With no exam set the behaviour is
 * exactly what it was before exam papers existed.
 */
object MockTestSelector {

    /**
     * Builds a paper.
     *
     * [difficultyMix] is the share of the paper wanted at each difficulty. It is
     * only honoured when the config names an exam; the generic paper keeps the
     * plain round-robin so existing papers do not change under a reader who has
     * already sat one.
     *
     * Without this the mix collapses: the bank lists a topic's easy questions
     * first, so taking the head of every topic pool in turn yields a paper that
     * is almost entirely easy, and an exam that expects hard questions gets an
     * easy paper under its own name.
     *
     * [MockTestConfig.poolOffset] rotates each band before it is drawn. An exam
     * holds far fewer questions than the sum of its papers, so without this every
     * year paper on one scheme would be the same fifteen questions.
     */
    fun select(
        config: MockTestConfig,
        topics: List<LessonId>,
        questionsByTopic: (LessonId) -> List<Question>,
        difficultyMix: Map<QuestionDifficulty, Double>? = null
    ): List<Question> {
        if (config.questionCount <= 0) return emptyList()

        val examId = config.examId
        val scoped = topics.map { topic ->
            questionsByTopic(topic)
                .filter { examId == null || it.isFrom(examId) }
        }

        val selected = mutableListOf<Question>()
        val seen = mutableSetOf<QuestionId>()
        fun take(question: Question) {
            seen.add(question.id)
            selected.add(question)
        }

        if (examId != null && !difficultyMix.isNullOrEmpty()) {
            for (band in QuestionDifficulty.entries) {
                val wanted = quotaFor(band, difficultyMix, config.questionCount)
                if (wanted <= 0) continue
                // Spread within the band rather than emptying one topic first.
                val pools = scoped.map { pool ->
                    pool.filter { it.difficulty == band }
                        .rotatedBy(config.poolOffset)
                        .filter { it.id !in seen }
                        .toMutableList()
                }
                var allExhausted = false
                var drawn = 0
                while (drawn < wanted && !allExhausted) {
                    allExhausted = true
                    pools.forEach { pool ->
                        if (drawn >= wanted) return@forEach
                        val next = pool.firstOrNull { it.id !in seen } ?: return@forEach
                        take(next)
                        pool.remove(next)
                        drawn++
                        allExhausted = false
                    }
                }
            }
            // A band that ran short leaves places over. Fill them from anything
            // still tagged for this exam, so a paper is never short because the
            // bank has fewer hard questions than the profile asked for.
            if (selected.size < config.questionCount) {
                fillRemaining(config, scoped, selected, seen)
            }
            return selected
        }

        val pools = scoped.map { it.toMutableList() }
        var allPoolsExhausted = false
        while (selected.size < config.questionCount && !allPoolsExhausted) {
            allPoolsExhausted = true
            pools.forEach { pool ->
                if (selected.size >= config.questionCount) return@forEach
                val next = pool.firstOrNull { question -> question.id !in seen }
                if (next != null) {
                    take(next)
                    pool.remove(next)
                    allPoolsExhausted = false
                }
            }
        }
        return selected
    }

    /**
     * How many of a paper's places belong to one difficulty.
     *
     * Largest-remainder apportionment: rounding each band independently would
     * make the quotas sum to something other than the paper length, and the
     * leftover goes to the bands with the biggest fractional parts. Ties break
     * on band order so the result never depends on map iteration.
     */
    private fun quotaFor(
        band: QuestionDifficulty,
        mix: Map<QuestionDifficulty, Double>,
        total: Int
    ): Int {
        val exact = QuestionDifficulty.entries.associateWith {
            (mix[it] ?: 0.0) * total
        }
        val floor = exact.mapValues { (_, value) -> value.toInt() }
        val leftover = total - floor.values.sum()
        val byRemainder = QuestionDifficulty.entries.sortedWith(
            compareByDescending<QuestionDifficulty> { exact.getValue(it) - floor.getValue(it) }
                .thenBy { it.ordinal }
        )
        return floor.getValue(band) + if (band in byRemainder.take(leftover)) 1 else 0
    }

    /** Tops a paper up from anything still tagged for the exam, in topic order. */
    private fun fillRemaining(
        config: MockTestConfig,
        scoped: List<List<Question>>,
        selected: MutableList<Question>,
        seen: MutableSet<QuestionId>
    ) {
        val pools = scoped.map { pool ->
            pool.rotatedBy(config.poolOffset).filter { it.id !in seen }.toMutableList()
        }
        var allExhausted = false
        while (selected.size < config.questionCount && !allExhausted) {
            allExhausted = true
            pools.forEach { pool ->
                if (selected.size >= config.questionCount) return@forEach
                val next = pool.firstOrNull() ?: return@forEach
                pool.remove(next)
                seen.add(next.id)
                selected.add(next)
                allExhausted = false
            }
        }
    }

    /**
     * Starts a list [places] further along and wraps round.
     *
     * An offset past the end of a short pool yields an empty list, which would
     * silently empty that topic's contribution, so the offset is reduced first.
     */
    private fun List<Question>.rotatedBy(places: Int): List<Question> {
        if (isEmpty() || places == 0) return this
        val shift = Math.floorMod(places, size)
        return drop(shift) + take(shift)
    }

    /**
     * How many questions are available for a paper, ignoring its length.
     *
     * The landing screen needs this to tell a reader *why* a paper cannot be
     * started, and the exam screen needs it to avoid starting one that will
     * immediately run out of questions.
     */
    fun availableCount(
        config: MockTestConfig,
        topics: List<LessonId>,
        questionsByTopic: (LessonId) -> List<Question>
    ): Int {
        val examId = config.examId
        return topics
            .flatMap { questionsByTopic(it) }
            .filter { it.isValid }
            .count { examId == null || it.isFrom(examId) }
    }
}