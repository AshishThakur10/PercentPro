package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LearningRecommendation
import com.atlabs.percentpro.model.Lesson
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LessonStatus
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.RecommendationDestination
import com.atlabs.percentpro.model.RecommendationKind
import com.atlabs.percentpro.model.RecommendationReason
import com.atlabs.percentpro.model.RecommendationTitleKey

/**
 * Chooses the single next thing to suggest.
 *
 * Pure and rule-based on purpose: no randomness, no network, no model to
 * download. The same analytics and progress always produce the same
 * recommendation, so the explanation can be checked against the numbers and the
 * behaviour is testable.
 *
 * The order is fixed and each step is a precondition for the next, which is what
 * makes "why this?" answerable:
 *
 *  1. an unfinished lesson is already open, so finish it;
 *  2. otherwise the next unlocked lesson, if the course is not finished;
 *  3. otherwise a topic with enough attempts and low accuracy;
 *  4. otherwise a step up in difficulty on a strong topic;
 *  5. otherwise a mixed run across studied topics;
 *  6. otherwise a gentle starting point when there is no history.
 *
 * Locked lessons, topics with no questions, and empty difficulty buckets can
 * never be recommended, because each step checks availability first.
 */
object RecommendationEngine {

    /**
     * @param analytics derived performance, already classified.
     * @param lessons the full ordered course.
     * @param progress saved lesson progress.
     * @param questionCounts questions available per topic, used to skip empty ones.
     */
    fun recommend(
        analytics: LearningAnalytics,
        lessons: List<Lesson>,
        progress: LearningProgress,
        questionCounts: Map<LessonId, Int>
    ): LearningRecommendation {
        val statuses = lessons.associate { lesson ->
            lesson.id to LessonAvailability.statusFor(
                lesson = lesson,
                allLessons = lessons,
                completed = progress.completedLessonIds,
                inProgress = progress.lessonInProgress
            )
        }

        // 1. Resume what is already open.
        progress.lessonInProgress?.let { inProgress ->
            val lesson = lessons.firstOrNull { it.id == inProgress } ?: return@let
            if (lesson.isPlayable && statuses[lesson.id] == LessonStatus.IN_PROGRESS) {
                return LearningRecommendation(
                    kind = RecommendationKind.RESUME_LESSON,
                    titleKey = RecommendationTitleKey.RESUME_LESSON,
                    reasonCode = RecommendationReason.LESSON_IN_PROGRESS,
                    lessonId = lesson.id,
                    destination = RecommendationDestination.OpenLesson(lesson.id)
                )
            }
        }

        val playable = lessons.filter { it.isPlayable }
        val availableLesson = playable.firstOrNull {
            statuses[it.id] == LessonStatus.AVAILABLE
        }

        // 2. Foundational learning is not finished yet: keep walking the path.
        if (availableLesson != null) {
            return LearningRecommendation(
                kind = RecommendationKind.CONTINUE_COURSE,
                titleKey = RecommendationTitleKey.CONTINUE_COURSE,
                reasonCode = RecommendationReason.NEXT_LESSON_UNLOCKED,
                lessonId = availableLesson.id,
                destination = RecommendationDestination.OpenLesson(availableLesson.id)
            )
        }

        // 3. A topic the data says is weak, with questions to practise against.
        analytics.weakTopics.firstOrNull { (questionCounts[it.topic] ?: 0) > 0 }?.let { weak ->
            return LearningRecommendation(
                kind = RecommendationKind.PRACTICE_WEAK_TOPIC,
                titleKey = RecommendationTitleKey.PRACTICE_WEAK_TOPIC,
                reasonCode = RecommendationReason.LOW_TOPIC_ACCURACY,
                reasonArgs = listOf(weak.accuracyPercent),
                topic = weak.topic,
                destination = RecommendationDestination.PracticeTopic(weak.topic)
            )
        }

        // 4. Secure on the easiest level: offer the next one up, if there are
        //    questions at that level.
        strongTopicWithHeadroom(analytics, questionCounts)?.let { (topic, current, next) ->
            return LearningRecommendation(
                kind = RecommendationKind.STEP_UP_DIFFICULTY,
                titleKey = RecommendationTitleKey.STEP_UP_DIFFICULTY,
                reasonCode = RecommendationReason.STRONG_ON_EASY,
                reasonArgs = listOf(current.accuracyPercent),
                topic = topic,
                difficulty = next,
                destination = RecommendationDestination.PracticeDifficulty(topic, next)
            )
        }

        // 5. Several comparable topics: a mixed run covers more ground.
        val studied = analytics.studiedTopics
            .filter { (questionCounts[it.topic] ?: 0) > 0 }
        if (studied.size >= InsightThresholds.MIN_TOPICS_FOR_MIXED_REVIEW) {
            val spread = studied.maxOf { it.accuracyPercent } - studied.minOf { it.accuracyPercent }
            if (spread <= MIXED_REVIEW_MAX_SPREAD) {
                return LearningRecommendation(
                    kind = RecommendationKind.MIXED_REVIEW,
                    titleKey = RecommendationTitleKey.MIXED_REVIEW,
                    reasonCode = RecommendationReason.BALANCED_ACROSS_TOPICS,
                    reasonArgs = listOf(studied.size),
                    destination = RecommendationDestination.MixedReview
                )
            }
        }

        // 6. Nothing specific stands out.
        //
        // Only a genuinely weak topic is offered here. Falling back to "some
        // topic you have touched" would label a strong one as weak, which is
        // exactly the misreading the dashboard exists to avoid.
        val fallback = analytics.weakTopics
            .firstOrNull { (questionCounts[it.topic] ?: 0) > 0 }
        if (fallback != null) {
            return LearningRecommendation(
                kind = RecommendationKind.PRACTICE_WEAK_TOPIC,
                titleKey = RecommendationTitleKey.PRACTICE_WEAK_TOPIC,
                reasonCode = RecommendationReason.LOW_TOPIC_ACCURACY,
                reasonArgs = listOf(fallback.accuracyPercent),
                topic = fallback.topic,
                destination = RecommendationDestination.PracticeTopic(fallback.topic)
            )
        }

        // Every written lesson is finished. If nothing has been answered either,
        // point at the dashboard to explain why rather than inventing an advice.
        return if (!analytics.hasAnyActivity && !playable.isEmpty()) {
            LearningRecommendation(
                kind = RecommendationKind.START_WITH_LESSON_ONE,
                titleKey = RecommendationTitleKey.START_WITH_LESSON_ONE,
                reasonCode = RecommendationReason.NO_HISTORY_YET,
                lessonId = playable.first().id,
                destination = RecommendationDestination.OpenInsights
            )
        } else {
            LearningRecommendation(
                kind = RecommendationKind.KEEP_PRACTISING,
                titleKey = RecommendationTitleKey.KEEP_PRACTISING,
                reasonCode = RecommendationReason.COURSE_COMPLETE,
                destination = RecommendationDestination.OpenInsights
            )
        }
    }

    /**
     * A strong topic where the next difficulty up has questions to offer.
     *
     * Two conditions, both deliberate. A real sample at the easy level, so a
     * high percentage is not a fluke of three questions; and at least one attempt
     * at a harder level, so the learner has already shown they can handle
     * something beyond the easiest questions. That second condition is what stops
     * "you are great at easy questions" from being read as "go and try hard ones".
     */
    private fun strongTopicWithHeadroom(
        analytics: LearningAnalytics,
        questionCounts: Map<LessonId, Int>
    ): Triple<LessonId, DifficultyPerformance, QuestionDifficulty>? =
        analytics.topics
            .asSequence()
            .filter { it.hasEnoughData }
            .filter { (questionCounts[it.topic] ?: 0) > 0 }
            .sortedByDescending { it.accuracyPercent }
            .mapNotNull { performance ->
                val current = performance.difficultyBreakdown[QuestionDifficulty.EASY]
                    ?.takeIf {
                        it.attempted >= InsightThresholds.MIN_ATTEMPTS_FOR_CLASSIFICATION &&
                            it.accuracyPercent >= InsightThresholds.ADVANCE_DIFFICULTY_AT_OR_ABOVE_PERCENT
                    }
                    ?: return@mapNotNull null
                val next = InsightThresholds.harderThan(QuestionDifficulty.EASY)
                    ?: return@mapNotNull null
                val attemptedHarder = InsightThresholds.ASCENDING_DIFFICULTY
                    .drop(1)
                    .any { (performance.difficultyBreakdown[it]?.attempted ?: 0) > 0 }
                if (!attemptedHarder) return@mapNotNull null
                Triple(performance.topic, current, next)
            }
            .firstOrNull()

    /** Above this spread the topics are too uneven for a mixed run to be useful. */
    private const val MIXED_REVIEW_MAX_SPREAD = 30
}