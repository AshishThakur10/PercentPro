package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.AchievementId
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.QuestionId

/**
 * Gamification state.
 *
 * Three things live here and must stay mutually consistent:
 *
 *  - [rewardedActivityIds] is the ledger. It is the authoritative record of what
 *    has been paid, and [totalXp] is always derived from it rather than stored
 *    independently, so XP and the ledger cannot drift apart.
 *  - [qualifyingDates] is the authoritative record of which days counted for a
 *    streak. Both streaks are computed from it, so a corrupted counter cannot
 *    survive.
 *  - [todayAnsweredQuestionIds] holds the current day's distinct submitted
 *    questions for the daily goal, tagged with [activityDate] so it is rolled
 *    over rather than carried into the next day.
 *
 * **Activity from before this feature existed is not counted.** The repository
 * seeds the ledger with the lessons and mock tests that were already done, so
 * they can never pay out again, but they contribute nothing to XP or to
 * achievement progress. Counts therefore start at zero for existing users. See
 * `GamificationRepository.seedPreExistingActivity` for the exact policy.
 */
data class GamificationState(
    val totalXp: Int = 0,
    val dailyQuestionGoal: Int = 10,
    /** Reward ledger. Ids are [com.atlabs.percentpro.model.ActivityId] values. */
    val rewardedActivityIds: Set<String> = emptySet(),
    /**
     * Activity that was already finished before this feature existed.
     *
     * Held apart from [rewardedActivityIds] deliberately. These entries must
     * *block* a future payout, but because total XP is derived from the paid
     * ledger, putting them in that set would manufacture XP for work already
     * done. They therefore contribute nothing to XP and nothing to achievements.
     */
    val preExistingActivityIds: Set<String> = emptySet(),
    /** Every question submitted since Phase 5, correct or not. */
    val distinctAnsweredQuestionIds: Set<QuestionId> = emptySet(),
    val completedLessonIds: Set<String> = emptySet(),
    val completedMockTestIds: Set<String> = emptySet(),
    val perfectPracticeTopics: Set<String> = emptySet(),
    val qualifyingDates: Set<LocalDateKey> = emptySet(),
    val goalReachedDates: Set<LocalDateKey> = emptySet(),
    val activityDate: LocalDateKey? = null,
    val todayAnsweredQuestionIds: Set<QuestionId> = emptySet(),
    val unlockedAchievements: Map<AchievementId, Long> = emptyMap()
) {
    /** Distinct questions answered since Phase 5; drives question-count achievements. */
    val uniqueQuestionCount: Int get() = distinctAnsweredQuestionIds.size

    val lessonCount: Int get() = completedLessonIds.size

    val mockTestCount: Int get() = completedMockTestIds.size

    /** Distinct questions submitted today. */
    val todayQuestionCount: Int
        get() = if (activityDate == null) 0 else todayAnsweredQuestionIds.size

    val isDailyGoalReached: Boolean
        get() = dailyQuestionGoal > 0 && todayQuestionCount >= dailyQuestionGoal

    /** Progress toward today's goal, always in 0f..1f. */
    val dailyGoalProgress: Float
        get() = if (dailyQuestionGoal <= 0) {
            0f
        } else {
            todayQuestionCount.toFloat() / dailyQuestionGoal.toFloat()
        }.coerceIn(0f, 1f)

    val unlockedAchievementCount: Int get() = unlockedAchievements.size
}

/**
 * Something the learner did that the gamification system may reward.
 *
 * Reported by ViewModels at the point a thing genuinely happened, never from a
 * composable, so re-rendering cannot create an event.
 */
sealed interface LearningActivity {
    /** The local calendar day this happened on. */
    val occurredOn: LocalDateKey

    /** A practice or mock-test answer the learner submitted. */
    data class AnsweredQuestion(
        val questionId: QuestionId,
        val wasCorrect: Boolean,
        override val occurredOn: LocalDateKey
    ) : LearningActivity

    /** A lesson reached its final block. */
    data class LessonCompleted(
        val lessonId: String,
        override val occurredOn: LocalDateKey
    ) : LearningActivity

    /** A mock test was finalised, by explicit submit or by timeout. */
    data class MockTestCompleted(
        val testId: String,
        override val occurredOn: LocalDateKey
    ) : LearningActivity

    /** A topic-practice session was finished with every question answered. */
    data class PracticeSessionCompleted(
        val topic: String,
        val allCorrect: Boolean,
        override val occurredOn: LocalDateKey
    ) : LearningActivity
}