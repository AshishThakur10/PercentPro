package com.atlabs.percentpro.content

import com.atlabs.percentpro.R
import com.atlabs.percentpro.model.ActivityId
import com.atlabs.percentpro.model.ActivityKind
import com.atlabs.percentpro.model.AchievementDefinition
import com.atlabs.percentpro.model.AchievementId
import com.atlabs.percentpro.model.AchievementMetric
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.RewardRules

/**
 * Pure gamification rules: no Android, no DataStore, no user-facing strings.
 *
 * Every function is a total function of its inputs and returns a new state.
 * Applying the same activity twice is always a no-op, which is what makes a
 * double tap, a recomposition or a process restart unable to pay out twice.
 */
object GamificationEngine {

    /**
     * Applies one activity.
     *
     * The order is fixed and deliberate:
     *  1. roll the day over, so yesterday's questions are never counted today;
     *  2. count the question for the daily goal, at most once per day;
     *  3. pay XP only if the ledger does not already hold the activity's id;
     *  4. record the activity for streak and achievement purposes;
     *  5. re-derive total XP from the ledger.
     *
     * An incorrect answer still counts toward the daily goal and can still make
     * the day qualify for a streak, because both are about showing up rather
     * than getting it right. It simply earns no question XP.
     */
    fun apply(
        state: GamificationState,
        activity: LearningActivity,
        atMillis: Long
    ): GamificationState {
        val today = activity.occurredOn
        val updated = when (activity) {
            is LearningActivity.AnsweredQuestion -> applyAnswered(rollOverTo(state, today), activity, today)

            is LearningActivity.LessonCompleted ->
                rollOverTo(state, today)
                    .withReward(ActivityId.lessonCompletedOf(activity.lessonId))
                    .markLessonCompleted(activity.lessonId)
                    .qualifying(today)

            is LearningActivity.MockTestCompleted ->
                rollOverTo(state, today)
                    .withReward(ActivityId.mockTestCompletedOf(activity.testId))
                    .markMockTestCompleted(activity.testId)
                    .qualifying(today)

            is LearningActivity.PracticeSessionCompleted -> {
                // Finishing a practice session is learning activity, so it can
                // qualify the day, but only a flawless one is rewarded.
                val base = rollOverTo(state, today).qualifying(today)
                if (activity.allCorrect) {
                    base.withReward(ActivityId.perfectPracticeOf(activity.topic))
                        .markPerfectPractice(activity.topic)
                } else {
                    base
                }
            }
        }

        return updated.copy(totalXp = xpOf(updated.rewardedActivityIds))
    }

    private fun applyAnswered(
        state: GamificationState,
        activity: LearningActivity.AnsweredQuestion,
        today: LocalDateKey
    ): GamificationState {
        var updated = state.copy(
            distinctAnsweredQuestionIds = state.distinctAnsweredQuestionIds + activity.questionId,
            todayAnsweredQuestionIds = state.todayAnsweredQuestionIds + activity.questionId,
            activityDate = today
        )

        // Reaching the target is recorded centrally so "Goal Getter" cannot be
        // missed by a screen that forgot to call the award.
        if (updated.dailyQuestionGoal > 0 &&
            updated.todayAnsweredQuestionIds.size >= updated.dailyQuestionGoal &&
            today !in updated.goalReachedDates
        ) {
            updated = updated.copy(goalReachedDates = updated.goalReachedDates + today)
        }

        // Five or more distinct submitted questions makes the day qualifying.
        if (updated.todayAnsweredQuestionIds.size >= RewardRules.QUESTIONS_PER_QUALIFYING_DAY) {
            updated = updated.qualifying(today)
        }

        if (activity.wasCorrect) {
            updated = updated.withReward(ActivityId.questionFirstCorrectOf(activity.questionId.value))
        }
        return updated
    }

    /**
     * Changes the daily target without touching today's progress.
     *
     * The count is kept, so moving from 10 to 30 part-way through a day leaves
     * the questions already answered counted.
     */
    fun setDailyGoal(state: GamificationState, goal: Int): GamificationState =
        state.copy(dailyQuestionGoal = goal.coerceAtLeast(0))

    /**
     * Moves the daily counters onto [today].
     *
     * Unfinished question counts are dropped rather than carried over, which is
     * what makes each day's progress independent.
     */
    fun rollOverTo(state: GamificationState, today: LocalDateKey): GamificationState = when {
        state.activityDate == null -> state.copy(activityDate = today)
        state.activityDate == today -> state
        else -> state.copy(activityDate = today, todayAnsweredQuestionIds = emptySet())
    }

    // ---- achievements ------------------------------------------------------

    /**
     * Unlocks every achievement whose threshold is now met.
     *
     * Called centrally after every state change, so no screen is responsible for
     * noticing an achievement. An achievement already in the map keeps its
     * original timestamp and can never be re-stamped.
     */
    fun unlockEarnedAchievements(
        state: GamificationState,
        atMillis: Long
    ): GamificationState {
        val earned = AchievementCatalogue.newlyEarned(state, atMillis)
        if (earned.isEmpty()) return state
        return state.copy(unlockedAchievements = state.unlockedAchievements + earned)
    }

    /** Applies an activity and then re-evaluates achievements, in one step. */
    fun applyAndEvaluate(
        state: GamificationState,
        activity: LearningActivity,
        atMillis: Long
    ): GamificationState =
        unlockEarnedAchievements(apply(state, activity, atMillis), atMillis)

    // ---- streaks -----------------------------------------------------------

    /**
     * Length of the run of consecutive qualifying days ending on [today].
     *
     * Zero when today has no qualifying activity and neither does yesterday.
     * The "nor yesterday" rule is what stops a streak being lost the instant a
     * new day begins, before that day has had a chance to be used.
     */
    fun currentStreak(state: GamificationState, today: LocalDateKey): Int {
        val dates = state.qualifyingDates
        if (today in dates) return runEndingAt(dates, today)
        val yesterday = today.minusOneDay
        return if (yesterday in dates) runEndingAt(dates, yesterday) else 0
    }

    /**
     * True when yesterday counted but today has not yet.
     *
     * The streak is still alive but will be lost at midnight, so the UI can say
     * so rather than showing a number that is about to change silently.
     */
    fun isStreakAtRisk(state: GamificationState, today: LocalDateKey): Boolean =
        today !in state.qualifyingDates && today.minusOneDay in state.qualifyingDates

    /** Longest run of consecutive qualifying days ever recorded. */
    fun longestStreak(state: GamificationState): Int {
        val sorted = state.qualifyingDates.sortedBy { it.value }
        if (sorted.isEmpty()) return 0
        var best = 1
        var run = 1
        for (index in 1 until sorted.size) {
            run = if (sorted[index - 1].daysUntil(sorted[index]) == 1) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    private fun runEndingAt(dates: Set<LocalDateKey>, end: LocalDateKey): Int {
        var count = 1
        var cursor = end
        while (cursor.minusOneDay in dates) {
            count++
            cursor = cursor.minusOneDay
        }
        return count
    }

    // ---- totals ------------------------------------------------------------

    /**
     * Total XP implied by a ledger.
     *
     * Used both to write [GamificationState.totalXp] and to validate it while
     * decoding, so a hand-edited or half-written file cannot leave XP
     * disagreeing with the events that produced it.
     */
    fun xpOf(activityIds: Collection<String>): Int =
        activityIds.sumOf { raw ->
            ActivityId.parse(raw)?.kind?.let { RewardRules.xpFor(it) } ?: 0
        }

    /** The XP value of one activity kind. */
    fun xpFor(kind: ActivityKind): Int = RewardRules.xpFor(kind)

    // ---- internals ---------------------------------------------------------

    /**
     * Adds a reward to the ledger, unless the activity is already paid or was
     * already done before this feature existed.
     *
     * Checking both sets here rather than only the paid ledger is what stops a
     * returning learner being paid again for a lesson they finished in an
     * earlier version.
     */
    private fun GamificationState.withReward(activityId: ActivityId): GamificationState = when {
        activityId.value in rewardedActivityIds -> this
        activityId.value in preExistingActivityIds -> this
        else -> copy(rewardedActivityIds = rewardedActivityIds + activityId.value)
    }

    private fun GamificationState.markLessonCompleted(lessonId: String): GamificationState =
        copy(completedLessonIds = completedLessonIds + lessonId)

    private fun GamificationState.markMockTestCompleted(testId: String): GamificationState =
        copy(completedMockTestIds = completedMockTestIds + testId)

    private fun GamificationState.markPerfectPractice(topic: String): GamificationState =
        copy(perfectPracticeTopics = perfectPracticeTopics + topic)

    /** Records a day as qualifying; repeated activity on the same day is a no-op. */
    private fun GamificationState.qualifying(day: LocalDateKey): GamificationState =
        copy(qualifyingDates = qualifyingDates + day)
}

/**
 * The achievement catalogue.
 *
 * Pure data plus one evaluator. Adding an achievement means adding a row to
 * [all]; no screen needs to know it exists, and every unlock is evaluated in one
 * place.
 */
object AchievementCatalogue {

    val all: List<AchievementDefinition> = listOf(
        definition(
            "first_steps",
            R.string.achievement_first_steps_title,
            R.string.achievement_first_steps_description,
            R.string.achievement_first_steps_badge,
            AchievementMetric.LESSONS_COMPLETED,
            1
        ),
        definition(
            "curious_learner",
            R.string.achievement_curious_learner_title,
            R.string.achievement_curious_learner_description,
            R.string.achievement_curious_learner_badge,
            AchievementMetric.DISTINCT_QUESTIONS_ANSWERED,
            10
        ),
        definition(
            "practice_pro",
            R.string.achievement_practice_pro_title,
            R.string.achievement_practice_pro_description,
            R.string.achievement_practice_pro_badge,
            AchievementMetric.DISTINCT_QUESTIONS_ANSWERED,
            50
        ),
        definition(
            "century_club",
            R.string.achievement_century_club_title,
            R.string.achievement_century_club_description,
            R.string.achievement_century_club_badge,
            AchievementMetric.DISTINCT_QUESTIONS_ANSWERED,
            100
        ),
        definition(
            "three_day_streak",
            R.string.achievement_three_day_streak_title,
            R.string.achievement_three_day_streak_description,
            R.string.achievement_three_day_streak_badge,
            AchievementMetric.STREAK_DAYS,
            3
        ),
        definition(
            "seven_day_streak",
            R.string.achievement_seven_day_streak_title,
            R.string.achievement_seven_day_streak_description,
            R.string.achievement_seven_day_streak_badge,
            AchievementMetric.STREAK_DAYS,
            7
        ),
        definition(
            "goal_getter",
            R.string.achievement_goal_getter_title,
            R.string.achievement_goal_getter_description,
            R.string.achievement_goal_getter_badge,
            AchievementMetric.DAILY_GOAL_REACHED,
            1
        ),
        definition(
            "mock_test_starter",
            R.string.achievement_mock_test_starter_title,
            R.string.achievement_mock_test_starter_description,
            R.string.achievement_mock_test_starter_badge,
            AchievementMetric.MOCK_TESTS_COMPLETED,
            1
        ),
        definition(
            "perfect_practice",
            R.string.achievement_perfect_practice_title,
            R.string.achievement_perfect_practice_description,
            R.string.achievement_perfect_practice_badge,
            AchievementMetric.PERFECT_PRACTICE_SESSION,
            1
        ),
        definition(
            "dedicated_learner",
            R.string.achievement_dedicated_learner_title,
            R.string.achievement_dedicated_learner_description,
            R.string.achievement_dedicated_learner_badge,
            AchievementMetric.LESSONS_COMPLETED,
            10
        )
    )

    fun byId(id: AchievementId): AchievementDefinition? =
        all.firstOrNull { it.id == id }

    fun isKnown(id: AchievementId): Boolean = byId(id) != null

    /** Current value of [definition]'s metric, for progress text. */
    fun progress(
        state: GamificationState,
        definition: AchievementDefinition
    ): Int = when (definition.metric) {
        AchievementMetric.LESSONS_COMPLETED -> state.lessonCount
        AchievementMetric.DISTINCT_QUESTIONS_ANSWERED -> state.uniqueQuestionCount
        // Longest rather than current, so a broken streak never makes an
        // achievement look unearned after it has been awarded.
        AchievementMetric.STREAK_DAYS -> GamificationEngine.longestStreak(state)
        AchievementMetric.DAILY_GOAL_REACHED -> if (state.goalReachedDates.isNotEmpty()) 1 else 0
        AchievementMetric.MOCK_TESTS_COMPLETED -> state.mockTestCount
        AchievementMetric.PERFECT_PRACTICE_SESSION ->
            if (state.perfectPracticeTopics.isNotEmpty()) 1 else 0
    }

    /** Achievements met by [state] but not yet recorded, stamped with [atMillis]. */
    fun newlyEarned(state: GamificationState, atMillis: Long): Map<AchievementId, Long> =
        all.asSequence()
            .filter { it.id !in state.unlockedAchievements }
            .filter { progress(state, it) >= it.target }
            .associate { it.id to atMillis }

    private fun definition(
        id: String,
        titleRes: Int,
        descriptionRes: Int,
        badgeLabelRes: Int,
        metric: AchievementMetric,
        target: Int
    ) = AchievementDefinition(
        id = AchievementId(id),
        titleRes = titleRes,
        descriptionRes = descriptionRes,
        badgeLabelRes = badgeLabelRes,
        metric = metric,
        target = target
    )
}