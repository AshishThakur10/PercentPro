package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.AnswerEvent
import com.atlabs.percentpro.model.AnswerSource
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.QuestionDifficulty

/**
 * How a topic is performing, stated as a small set of named states.
 *
 * [NOT_ENOUGH_DATA] exists so a topic the learner has barely touched is never
 * shown as weak. Five answered questions is the point where an accuracy figure
 * starts to mean something; below it the honest answer is "not enough data".
 */
enum class TopicStatus {
    /** Fewer than [InsightThresholds.MIN_ATTEMPTS_FOR_CLASSIFICATION] attempts. */
    NOT_ENOUGH_DATA,

    /** Enough attempts, accuracy below the practice threshold. */
    NEEDS_PRACTICE,

    /** Enough attempts, accuracy in the middle band. */
    IMPROVING,

    /** Enough attempts, accuracy at or above the strong threshold. */
    STRONG
}

/** Accuracy and attempt counts for one topic, plus its difficulty split. */
data class TopicPerformance(
    val topic: LessonId,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val difficultyBreakdown: Map<QuestionDifficulty, DifficultyPerformance> = emptyMap()
) {
    val accuracyPercent: Int
        get() = if (attempted <= 0) 0 else correct * 100 / attempted

    val hasEnoughData: Boolean
        get() = attempted >= InsightThresholds.MIN_ATTEMPTS_FOR_CLASSIFICATION

    val status: TopicStatus
        get() = when {
            !hasEnoughData -> TopicStatus.NOT_ENOUGH_DATA
            accuracyPercent < InsightThresholds.NEEDS_PRACTICE_BELOW_PERCENT -> TopicStatus.NEEDS_PRACTICE
            accuracyPercent < InsightThresholds.IMPROVING_BELOW_PERCENT -> TopicStatus.IMPROVING
            else -> TopicStatus.STRONG
        }

    /** Accuracy for one difficulty, or null when that level was never attempted. */
    fun accuracyFor(difficulty: QuestionDifficulty): Int? =
        difficultyBreakdown[difficulty]?.accuracyPercent

    /** 0f..1f, for the progress bar. */
    val accuracyFraction: Float
        get() = if (attempted <= 0) 0f else correct.toFloat() / attempted

    /** The share of attempts made at the easiest level, used to spot mastery claims. */
    val easyAttemptShare: Float
        get() {
            val easy = difficultyBreakdown[QuestionDifficulty.EASY]?.attempted ?: 0
            return if (attempted <= 0) 0f else easy.toFloat() / attempted
        }

    /**
     * True when the accuracy looks high but is mostly built on easy questions.
     *
     * Shown as a caution rather than as a downgrade: the status stays whatever it
     * is, but the UI stops presenting it as mastery on that evidence alone.
     */
    val isHighAccuracyOnEasyQuestionsOnly: Boolean
        get() = hasEnoughData &&
            status == TopicStatus.STRONG &&
            easyAttemptShare >= 0.8f
}

/** Attempts and accuracy at a single difficulty within a topic. */
data class DifficultyPerformance(
    val difficulty: QuestionDifficulty,
    val attempted: Int,
    val correct: Int
) {
    val accuracyPercent: Int
        get() = if (attempted <= 0) 0 else correct * 100 / attempted
}

/**
 * The most recent window compared with the one before it.
 *
 * [hasComparison] is false unless both windows have enough answers, so the UI can
 * say "keep practising" instead of drawing a trend from two tiny samples.
 */
data class RecentPerformance(
    val attempted: Int = 0,
    val correct: Int = 0,
    val windowSize: Int = InsightThresholds.RECENT_WINDOW_SIZE,
    val previousAttempted: Int = 0,
    val previousCorrect: Int = 0
) {
    val accuracyPercent: Int
        get() = if (attempted <= 0) 0 else correct * 100 / attempted

    val previousAccuracyPercent: Int
        get() = if (previousAttempted <= 0) 0 else previousCorrect * 100 / previousAttempted

    /** Only compare like with like: both windows must be at least half full. */
    val hasComparison: Boolean
        get() = attempted >= windowSize / 2 && previousAttempted >= windowSize / 2

    /** Percentage-point change, positive when the learner improved. */
    val accuracyDeltaPercent: Int
        get() = if (hasComparison) accuracyPercent - previousAccuracyPercent else 0

    val isImproving: Boolean get() = hasComparison && accuracyDeltaPercent > 0
    val isDeclining: Boolean get() = hasComparison && accuracyDeltaPercent < 0
    val isSteady: Boolean get() = hasComparison && accuracyDeltaPercent == 0
}

/** Overall figures plus the per-topic breakdown and the recent window. */
data class LearningAnalytics(
    val overallAttempted: Int = 0,
    val overallCorrect: Int = 0,
    val completedLessonCount: Int = 0,
    val topics: List<TopicPerformance> = emptyList(),
    val recent: RecentPerformance = RecentPerformance(),
    val topicsWithData: Int = 0
) {
    val overallAccuracyPercent: Int
        get() = if (overallAttempted <= 0) 0 else overallCorrect * 100 / overallAttempted

    val hasAnyActivity: Boolean get() = overallAttempted > 0

    val strongestTopic: TopicPerformance?
        get() = topics
            .filter { it.hasEnoughData && it.attempted > 0 }
            .maxByOrNull { it.accuracyPercent }

    /**
     * The topic with the most room to improve.
     *
     * Only topics with enough attempts qualify, and the one with the most
     * attempts wins a tie, so a topic beaten twice by five tries does not outrank
     * a consistently weak topic with a real sample.
     */
    val weakestTopic: TopicPerformance?
        get() = topics
            .filter { it.hasEnoughData && it.status == TopicStatus.NEEDS_PRACTICE }
            .minWithOrNull(compareBy({ it.accuracyPercent }, { -it.attempted }))

    /** Topics that qualify as weak-practice targets. */
    val weakTopics: List<TopicPerformance>
        get() = topics
            .filter { it.hasEnoughData && it.status == TopicStatus.NEEDS_PRACTICE }
            .sortedBy { it.accuracyPercent }

    /** Topics with enough attempts in any state, studied least-recently first. */
    val studiedTopics: List<TopicPerformance>
        get() = topics.filter { it.attempted > 0 }

    fun topic(topic: LessonId): TopicPerformance? =
        topics.firstOrNull { it.topic == topic }
}

/**
 * Derives every insight number from the answer ledger.
 *
 * Pure and total: the same events always produce the same analytics, which is
 * what makes the recommendation text reproducible and the tests deterministic.
 */
object LearningAnalyticsEngine {

    /**
     * Builds the analytics for one moment in time.
     *
     * [events] is the whole stored ledger; unanswerable or duplicate entries are
     * ignored here as well as at write time, so a hand-edited file cannot inflate
     * a count.
     */
    fun analyse(
        events: List<AnswerEvent>,
        completedLessons: Set<LessonId> = emptySet(),
        source: AnswerSource? = null
    ): LearningAnalytics {
        val usable = deduplicate(events).let { list ->
            if (source == null) list else list.filter { it.source == source }
        }

        val byTopic = usable.groupBy { it.topicKey }
        val topics = byTopic.entries.mapNotNull { (topicKey, topicEvents) ->
            LessonId.fromStorageKey(topicKey)?.let { topic ->
                TopicPerformance(
                    topic = topic,
                    attempted = topicEvents.size,
                    correct = topicEvents.count { it.wasCorrect },
                    incorrect = topicEvents.count { !it.wasCorrect },
                    difficultyBreakdown = topicEvents
                        .groupBy { it.difficulty }
                        .mapValues { (difficulty, bucket) ->
                            DifficultyPerformance(
                                difficulty = difficulty,
                                attempted = bucket.size,
                                correct = bucket.count { it.wasCorrect }
                            )
                        }
                )
            }
        }.sortedBy { it.topic.ordinal }

        return LearningAnalytics(
            overallAttempted = usable.size,
            overallCorrect = usable.count { it.wasCorrect },
            completedLessonCount = completedLessons.size,
            topics = topics,
            recent = recentWindow(usable),
            topicsWithData = topics.count { it.hasEnoughData }
        )
    }

    /**
     * The most recent [InsightThresholds.RECENT_WINDOW_SIZE] answers, compared with
     * the window before it.
     */
    fun recentWindow(events: List<AnswerEvent>): RecentPerformance {
        val ordered = events.sortedBy { it.answeredAtEpochMillis }
        val window = InsightThresholds.RECENT_WINDOW_SIZE
        val recent = ordered.takeLast(window)
        val previous = ordered.dropLast(recent.size).takeLast(window)

        return RecentPerformance(
            attempted = recent.size,
            correct = recent.count { it.wasCorrect },
            windowSize = window,
            previousAttempted = previous.size,
            previousCorrect = previous.count { it.wasCorrect }
        )
    }

    /**
     * Removes repeated attempts.
     *
     * The ledger already refuses a duplicate on write; doing it again here means a
     * corrupted or hand-edited file still cannot double-count, and restores after
     * a crash cannot inflate a count.
     */
    fun deduplicate(events: List<AnswerEvent>): List<AnswerEvent> {
        val seen = mutableSetOf<String>()
        return events.filter { seen.add(it.attemptKey) }
    }

    /**
     * Recent performance split by topic, for the insights screen.
     *
     * Derived from the same recent window so the two can never disagree.
     */
    fun recentByTopic(events: List<AnswerEvent>): List<TopicPerformance> {
        val ordered = events.sortedBy { it.answeredAtEpochMillis }
        val recent = ordered.takeLast(InsightThresholds.RECENT_WINDOW_SIZE)
        return recent.groupBy { it.topicKey }.mapNotNull { (topicKey, bucket) ->
            LessonId.fromStorageKey(topicKey)?.let { topic ->
                TopicPerformance(
                    topic = topic,
                    attempted = bucket.size,
                    correct = bucket.count { it.wasCorrect },
                    incorrect = bucket.count { !it.wasCorrect },
                    difficultyBreakdown = bucket.groupBy { it.difficulty }
                        .mapValues { (difficulty, group) ->
                            DifficultyPerformance(
                                difficulty = difficulty,
                                attempted = group.size,
                                correct = group.count { it.wasCorrect }
                            )
                        }
                )
            }
        }.sortedBy { it.topic.ordinal }
    }
}