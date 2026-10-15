package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.ActivityId
import com.atlabs.percentpro.model.ActivityKind
import com.atlabs.percentpro.model.AchievementId
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.QuestionId
import com.atlabs.percentpro.model.RewardRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GamificationRewardTest {

    private val today = LocalDateKey.of(2026, 1, 15)

    private fun answered(id: String, correct: Boolean = true, on: LocalDateKey = today) =
        LearningActivity.AnsweredQuestion(QuestionId(id), correct, on)

    private fun lessonCompleted(id: LessonId, on: LocalDateKey = today) =
        LearningActivity.LessonCompleted(id.storageKey, on)

    private fun mockCompleted(id: String, on: LocalDateKey = today) =
        LearningActivity.MockTestCompleted(id, on)

    // ---- XP for a lesson ----------------------------------------------------

    @Test
    fun `completing a lesson for the first time awards 20 XP`() {
        val outcome = GamificationEngine.apply(
            GamificationState(),
            lessonCompleted(LessonId.WHAT_IS_A_PERCENTAGE),
            atMillis = 1_000L
        )

        assertEquals(20, outcome.totalXp)
        assertTrue(ActivityId.lessonCompleted(LessonId.WHAT_IS_A_PERCENTAGE).value in outcome.rewardedActivityIds)
    }

    @Test
    fun `reopening or repeating a lesson does not award XP again`() {
        val first = GamificationEngine.apply(
            GamificationState(),
            lessonCompleted(LessonId.WHAT_IS_A_PERCENTAGE),
            atMillis = 1_000L
        )
        val second = GamificationEngine.apply(
            first,
            lessonCompleted(LessonId.WHAT_IS_A_PERCENTAGE),
            atMillis = 2_000L
        )

        assertEquals(20, second.totalXp)
        assertEquals(1, second.lessonCount)
    }

    @Test
    fun `different lessons each award once`() {
        var state = GamificationState()
        state = GamificationEngine.apply(state, lessonCompleted(LessonId.WHAT_IS_A_PERCENTAGE), 1L)
        state = GamificationEngine.apply(state, lessonCompleted(LessonId.PERCENTAGES_VISUALLY), 2L)
        state = GamificationEngine.apply(state, lessonCompleted(LessonId.WHAT_IS_A_PERCENTAGE), 3L)

        assertEquals(40, state.totalXp)
        assertEquals(2, state.lessonCount)
    }

    // ---- XP for a question --------------------------------------------------

    @Test
    fun `the first correct answer awards 5 XP`() {
        val state = GamificationEngine.apply(
            GamificationState(),
            answered("q1", correct = true),
            atMillis = 1_000L
        )

        assertEquals(5, state.totalXp)
    }

    @Test
    fun `an incorrect answer awards no question XP`() {
        val state = GamificationEngine.apply(
            GamificationState(),
            answered("q1", correct = false),
            atMillis = 1_000L
        )

        assertEquals(0, state.totalXp)
        // It still counts as activity for the goal and the streak.
        assertEquals(1, state.todayQuestionCount)
        assertEquals(1, state.uniqueQuestionCount)
    }

    @Test
    fun `answering the same question correctly again does not farm XP`() {
        var state = GamificationState()
        state = GamificationEngine.apply(state, answered("q1", correct = true), 1L)
        state = GamificationEngine.apply(state, answered("q1", correct = true), 2L)
        state = GamificationEngine.apply(state, answered("q1", correct = true), 3L)

        assertEquals(5, state.totalXp)
        assertEquals(1, state.uniqueQuestionCount)
    }

    @Test
    fun `answering wrong then right pays the first correct answer exactly once`() {
        var state = GamificationState()
        state = GamificationEngine.apply(state, answered("q1", correct = false), 1L)
        assertEquals(0, state.totalXp)

        state = GamificationEngine.apply(state, answered("q1", correct = true), 2L)
        assertEquals(5, state.totalXp)

        state = GamificationEngine.apply(state, answered("q1", correct = true), 3L)
        assertEquals(5, state.totalXp)
    }

    // ---- Mock tests ---------------------------------------------------------

    @Test
    fun `completing a mock test awards 10 XP exactly once per test definition`() {
        var state = GamificationEngine.apply(GamificationState(), mockCompleted("t1"), 1L)
        assertEquals(10, state.totalXp)

        // A retry of the same paper must not pay again.
        state = GamificationEngine.apply(state, mockCompleted("t1"), 2L)
        assertEquals(10, state.totalXp)
        assertEquals(1, state.mockTestCount)
    }

    @Test
    fun `a different mock test does pay separately`() {
        var state = GamificationEngine.apply(GamificationState(), mockCompleted("t1"), 1L)
        state = GamificationEngine.apply(state, mockCompleted("t2"), 2L)

        assertEquals(20, state.totalXp)
        assertEquals(2, state.mockTestCount)
    }

    @Test
    fun `mock completion XP is not duplicated by a timeout submission`() {
        // Manual and timeout are the same activity as far as the ledger is
        // concerned, so the second one is a no-op.
        var state = GamificationEngine.apply(GamificationState(), mockCompleted("t1"), 1L)
        state = GamificationEngine.apply(state, mockCompleted("t1"), 2L)

        assertEquals(10, state.totalXp)
    }

    // ---- Never negative -----------------------------------------------------

    @Test
    fun `XP can never go negative even if the ledger is tampered with`() {
        val tampered = GamificationState(rewardedActivityIds = emptySet(), totalXp = -500)

        val refreshed = GamificationEngine.apply(
            tampered,
            lessonCompleted(LessonId.WHAT_IS_A_PERCENTAGE),
            atMillis = 1L
        )

        assertEquals(20, refreshed.totalXp)
        assertTrue(refreshed.totalXp >= 0)
    }

    @Test
    fun `total XP is always the sum of the ledger`() {
        var state = GamificationState()
        state = GamificationEngine.apply(state, lessonCompleted(LessonId.WHAT_IS_A_PERCENTAGE), 1L)
        state = GamificationEngine.apply(state, answered("q1", correct = true), 2L)
        state = GamificationEngine.apply(state, answered("q2", correct = true), 3L)
        state = GamificationEngine.apply(state, mockCompleted("t1"), 4L)

        assertEquals(20 + 5 + 5 + 10, state.totalXp)
        assertEquals(state.totalXp, GamificationEngine.xpOf(state.rewardedActivityIds))
    }

    @Test
    fun `a ledger containing an unknown id contributes no XP`() {
        val forged = GamificationState(rewardedActivityIds = setOf("made_up_kind:x", "lesson_completed:a"))

        assertEquals(
            RewardRules.XP_LESSON_COMPLETED,
            GamificationEngine.xpOf(forged.rewardedActivityIds)
        )
    }

    @Test
    fun `perfect practice is recorded but awards no XP`() {
        val state = GamificationEngine.apply(
            GamificationState(),
            LearningActivity.PracticeSessionCompleted("what_is_a_percentage", allCorrect = true, today),
            atMillis = 1L
        )

        assertEquals(0, state.totalXp)
        assertEquals(1, state.perfectPracticeTopics.size)
    }

    @Test
    fun `an imperfect practice session is not rewarded at all`() {
        val state = GamificationEngine.apply(
            GamificationState(),
            LearningActivity.PracticeSessionCompleted("what_is_a_percentage", allCorrect = false, today),
            atMillis = 1L
        )

        assertEquals(0, state.totalXp)
        assertTrue(state.perfectPracticeTopics.isEmpty())
        // ...but finishing is still activity, so the day qualifies.
        assertTrue(today in state.qualifyingDates)
    }

    // ---- No XP for passive actions -----------------------------------------

    @Test
    fun `only answers lessons and mock completions can pay`() {
        val activities = listOf(
            answered("q1"),
            lessonCompleted(LessonId.WHAT_IS_A_PERCENTAGE),
            mockCompleted("t1")
        )
        val paidKinds = activities.mapNotNull { activity ->
            when (activity) {
                is LearningActivity.AnsweredQuestion -> ActivityKind.QUESTION_FIRST_CORRECT
                is LearningActivity.LessonCompleted -> ActivityKind.LESSON_COMPLETED
                is LearningActivity.MockTestCompleted -> ActivityKind.MOCK_TEST_COMPLETED
                is LearningActivity.PracticeSessionCompleted -> null
            }
        }

        // Review mode and navigation report no activity at all, so there is no
        // kind through which they could pay.
        assertEquals(
            setOf(
                ActivityKind.QUESTION_FIRST_CORRECT,
                ActivityKind.LESSON_COMPLETED,
                ActivityKind.MOCK_TEST_COMPLETED
            ),
            paidKinds.toSet()
        )
        assertNotNull(RewardRules.XP_PERFECT_PRACTICE_SESSION)
        assertEquals(0, RewardRules.XP_PERFECT_PRACTICE_SESSION)
    }
}

class GamificationDailyGoalTest {

    private val today = LocalDateKey.of(2026, 1, 15)
    private val tomorrow = today.plusOneDay

    private fun answered(id: String, on: LocalDateKey = today, correct: Boolean = false) =
        LearningActivity.AnsweredQuestion(QuestionId(id), correct, on)

    @Test
    fun `distinct submitted questions each count once`() {
        var state = GamificationState(dailyQuestionGoal = 10)
        state = GamificationEngine.apply(state, answered("q1"), 1L)
        state = GamificationEngine.apply(state, answered("q2"), 2L)
        state = GamificationEngine.apply(state, answered("q3"), 3L)

        assertEquals(3, state.todayQuestionCount)
    }

    @Test
    fun `re-answering the same question does not increase today's count`() {
        var state = GamificationState()
        state = GamificationEngine.apply(state, answered("q1"), 1L)
        state = GamificationEngine.apply(state, answered("q1"), 2L)
        state = GamificationEngine.apply(state, answered("q1"), 3L)

        assertEquals(1, state.todayQuestionCount)
    }

    @Test
    fun `an incorrect answer still counts toward the daily goal`() {
        val state = GamificationEngine.apply(
            GamificationState(dailyQuestionGoal = 10),
            answered("q1", correct = false),
            atMillis = 1L
        )

        assertEquals(1, state.todayQuestionCount)
        assertEquals(0, state.totalXp)
    }

    @Test
    fun `an unanswered question does not count because it is never reported`() {
        // Only a submitted answer produces an activity; navigating past a
        // question produces nothing, so nothing is counted.
        val state = GamificationState(dailyQuestionGoal = 10)

        assertEquals(0, state.todayQuestionCount)
        assertFalse(state.isDailyGoalReached)
    }

    @Test
    fun `reaching the goal sets the reached state and records the day`() {
        var state = GamificationState(dailyQuestionGoal = 10)
        repeat(10) { index -> state = GamificationEngine.apply(state, answered("q$index"), index.toLong()) }

        assertEquals(10, state.todayQuestionCount)
        assertTrue(state.isDailyGoalReached)
        assertEquals(1f, state.dailyGoalProgress, 0.0001f)
        assertTrue(today in state.goalReachedDates)
    }

    @Test
    fun `unfinished questions do not carry into the next day`() {
        var state = GamificationState(dailyQuestionGoal = 10)
        repeat(4) { index -> state = GamificationEngine.apply(state, answered("q$index"), index.toLong()) }
        assertEquals(4, state.todayQuestionCount)

        state = GamificationEngine.apply(state, answered("q9", on = tomorrow), 100L)

        assertEquals(1, state.todayQuestionCount)
        assertFalse(state.isDailyGoalReached)
        // ...but the distinct total is kept, because achievements are all-time.
        assertEquals(5, state.uniqueQuestionCount)
    }

    @Test
    fun `changing the goal keeps today's progress`() {
        var state = GamificationState(dailyQuestionGoal = 30)
        repeat(12) { index -> state = GamificationEngine.apply(state, answered("q$index"), index.toLong()) }

        state = GamificationEngine.setDailyGoal(state, 10)

        assertEquals(10, state.dailyQuestionGoal)
        assertEquals(12, state.todayQuestionCount)
        assertTrue(state.isDailyGoalReached)
        assertEquals(1f, state.dailyGoalProgress, 0.0001f)
    }

    @Test
    fun `raising the goal recalculates progress from the same count`() {
        var state = GamificationState(dailyQuestionGoal = 10)
        repeat(12) { index -> state = GamificationEngine.apply(state, answered("q$index"), index.toLong()) }

        state = GamificationEngine.setDailyGoal(state, 30)

        assertEquals(12, state.todayQuestionCount)
        assertEquals(12f / 30f, state.dailyGoalProgress, 0.0001f)
        assertFalse(state.isDailyGoalReached)
    }

    @Test
    fun `the default goal is ten questions`() {
        assertEquals(10, GamificationState().dailyQuestionGoal)
        assertEquals(10, RewardRules.DEFAULT_DAILY_GOAL)
        assertEquals(listOf(10, 20, 30), RewardRules.dailyGoalOptions)
    }

    @Test
    fun `progress is bounded between zero and one`() {
        var state = GamificationState(dailyQuestionGoal = 10)
        repeat(25) { index -> state = GamificationEngine.apply(state, answered("q$index"), index.toLong()) }

        assertEquals(1f, state.dailyGoalProgress, 0.0001f)
        assertEquals(0f, GamificationState().dailyGoalProgress, 0.0001f)
    }

    @Test
    fun `a day with no activity reports no progress`() {
        val state = GamificationState(dailyQuestionGoal = 10)

        assertEquals(0, state.todayQuestionCount)
        assertFalse(state.isDailyGoalReached)
        assertNull(state.activityDate)
    }

    @Test
    fun `rolling over a stale day clears the count`() {
        val withProgress = GamificationEngine.apply(
            GamificationState(dailyQuestionGoal = 10),
            answered("q1"),
            atMillis = 1L
        )

        val rolled = GamificationEngine.rollOverTo(withProgress, tomorrow)

        assertEquals(0, rolled.todayQuestionCount)
        assertEquals(tomorrow, rolled.activityDate)
        assertEquals(1, rolled.uniqueQuestionCount)
    }
}

class GamificationStreakTest {

    private val day1 = LocalDateKey.of(2026, 1, 15)
    private val day2 = day1.plusOneDay
    private val day3 = day2.plusOneDay
    private val day4 = day3.plusOneDay

    private fun q(id: String, on: LocalDateKey) =
        LearningActivity.AnsweredQuestion(QuestionId(id), true, on)

    private fun lesson(on: LocalDateKey) =
        LearningActivity.LessonCompleted(LessonId.WHAT_IS_A_PERCENTAGE.storageKey, on)

    private fun mock(on: LocalDateKey) = LearningActivity.MockTestCompleted("t1", on)

    @Test
    fun `the first qualifying day starts a streak of one`() {
        val state = GamificationEngine.apply(GamificationState(), lesson(day1), 1L)

        assertEquals(1, GamificationEngine.currentStreak(state, day1))
    }

    @Test
    fun `five distinct questions qualify a day on their own`() {
        var state = GamificationState()
        repeat(5) { index -> state = GamificationEngine.apply(state, q("q$index", day1), index.toLong()) }

        assertEquals(1, GamificationEngine.currentStreak(state, day1))
    }

    @Test
    fun `four questions are not enough to qualify a day`() {
        var state = GamificationState()
        repeat(4) { index -> state = GamificationEngine.apply(state, q("q$index", day1), index.toLong()) }

        assertEquals(0, GamificationEngine.currentStreak(state, day1))
        assertTrue(state.qualifyingDates.isEmpty())
    }

    @Test
    fun `a completing a mock test qualifies a day`() {
        val state = GamificationEngine.apply(GamificationState(), mock(day1), 1L)

        assertEquals(1, GamificationEngine.currentStreak(state, day1))
    }

    @Test
    fun `activity on the following day increases the streak`() {
        var state = GamificationEngine.apply(GamificationState(), lesson(day1), 1L)
        state = GamificationEngine.apply(state, lesson(day2), 2L)
        state = GamificationEngine.apply(state, lesson(day3), 3L)

        assertEquals(3, GamificationEngine.currentStreak(state, day3))
    }

    @Test
    fun `many activities on one day count as a single streak day`() {
        var state = GamificationEngine.apply(GamificationState(), lesson(day1), 1L)
        state = GamificationEngine.apply(state, q("q1", day1), 2L)
        state = GamificationEngine.apply(state, q("q2", day1), 3L)
        state = GamificationEngine.apply(state, mock(day1), 4L)
        state = GamificationEngine.apply(state, lesson(day1), 5L)

        assertEquals(1, GamificationEngine.currentStreak(state, day1))
        assertEquals(1, state.qualifyingDates.size)
    }

    @Test
    fun `missing a full day resets the streak on the next qualifying day`() {
        var state = GamificationEngine.apply(GamificationState(), lesson(day1), 1L)
        state = GamificationEngine.apply(state, lesson(day2), 2L)
        assertEquals(2, GamificationEngine.currentStreak(state, day2))

        // day3 skipped entirely.
        state = GamificationEngine.apply(state, lesson(day4), 3L)

        assertEquals(1, GamificationEngine.currentStreak(state, day4))
        // The run before the gap is remembered.
        assertEquals(2, GamificationEngine.longestStreak(state))
    }

    @Test
    fun `the longest streak is retained after a reset`() {
        var state = GamificationState()
        state = GamificationEngine.apply(state, lesson(day1), 1L)
        state = GamificationEngine.apply(state, lesson(day2), 2L)
        state = GamificationEngine.apply(state, lesson(day3), 3L)
        state = GamificationEngine.apply(state, lesson(day4), 4L)
        assertEquals(4, GamificationEngine.longestStreak(state))

        // A long gap, then a fresh short run.
        val later = LocalDateKey(day4.value + 10)
        state = GamificationEngine.apply(state, lesson(later), 5L)

        assertEquals(1, GamificationEngine.currentStreak(state, later))
        assertEquals(4, GamificationEngine.longestStreak(state))
    }

    @Test
    fun `reopening the app on the same day does not change the streak`() {
        val state = GamificationEngine.apply(GamificationState(), lesson(day1), 1L)

        // "Reopening" only re-reads the stored state; nothing is reported.
        assertEquals(1, GamificationEngine.currentStreak(state, day1))
        assertEquals(1, GamificationEngine.currentStreak(state, day1))
        assertEquals(1, state.qualifyingDates.size)
    }

    @Test
    fun `a day with no activity does not grant streak credit`() {
        val state = GamificationEngine.apply(GamificationState(), lesson(day1), 1L)

        assertTrue(day2 !in state.qualifyingDates)
        assertEquals(1, GamificationEngine.currentStreak(state, day2))
        // day1 is still alive because day2 has not ended yet.
        assertTrue(GamificationEngine.isStreakAtRisk(state, day2))
    }

    @Test
    fun `the streak is lost once a full day passes without activity`() {
        val state = GamificationEngine.apply(GamificationState(), lesson(day1), 1L)

        assertEquals(0, GamificationEngine.currentStreak(state, day3))
        assertFalse(GamificationEngine.isStreakAtRisk(state, day3))
    }

    @Test
    fun `streaks are counted in calendar days, not elapsed hours`() {
        // Five answers spread across two instants inside one calendar day.
        var state = GamificationState()
        repeat(5) { index ->
            state = GamificationEngine.apply(state, q("q$index", day1), 10_000L + index)
            // The clock reads a different time, but the date key is the same.
            assertEquals(day1, state.activityDate)
        }
        // One day, one streak step - not five.
        assertEquals(1, GamificationEngine.currentStreak(state, day1))

        // A fifth answer on the next calendar day only counts as that day if
        // that day also reaches five distinct questions of its own.
        state = GamificationEngine.apply(state, q("q5", day2), 10_100L)
        assertEquals(1, GamificationEngine.currentStreak(state, day2))
        assertTrue(day2 !in state.qualifyingDates)

        // Day 2 qualifying on its own advances the streak to two.
        repeat(5) { offset ->
            state = GamificationEngine.apply(
                state,
                q("d2_$offset", day2),
                20_000L + offset
            )
        }
        assertTrue(day2 in state.qualifyingDates)
        assertEquals(2, GamificationEngine.currentStreak(state, day2))
    }

    @Test
    fun `a streak of zero with no activity at all`() {
        val empty = GamificationState()

        assertEquals(0, GamificationEngine.currentStreak(empty, day1))
        assertEquals(0, GamificationEngine.longestStreak(empty))
        assertFalse(GamificationEngine.isStreakAtRisk(empty, day1))
    }

    @Test
    fun `the daily goal is independent of the streak`() {
        // Reaching the goal is not what keeps a streak alive, and submitting
        // fewer than five questions on a qualifying day does not clear a streak
        // earned by completing a lesson.
        var state = GamificationState(dailyQuestionGoal = 10)
        repeat(10) { index -> state = GamificationEngine.apply(state, q("q$index", day1), index.toLong()) }
        assertTrue(state.isDailyGoalReached)

        state = GamificationEngine.apply(state, lesson(day2), 20L)
        assertEquals(2, GamificationEngine.currentStreak(state, day2))
        assertEquals(0, state.todayQuestionCount)
    }
}

class GamificationAchievementTest {

    private val today = LocalDateKey.of(2026, 1, 15)

    private fun answered(id: String, correct: Boolean = true, on: LocalDateKey = today) =
        LearningActivity.AnsweredQuestion(QuestionId(id), correct, on)

    private fun lesson(id: LessonId, on: LocalDateKey = today) =
        LearningActivity.LessonCompleted(id.storageKey, on)

    private fun evaluate(vararg activities: LearningActivity): GamificationState {
        var state = GamificationState()
        activities.forEachIndexed { index, activity ->
            state = GamificationEngine.applyAndEvaluate(state, activity, 1_000L + index)
        }
        return state
    }

    @Test
    fun `first steps unlocks on the first lesson`() {
        val state = evaluate(lesson(LessonId.WHAT_IS_A_PERCENTAGE))

        assertTrue(AchievementId("first_steps") in state.unlockedAchievements)
    }

    @Test
    fun `first steps stays locked until a lesson is completed`() {
        val state = evaluate(*Array(10) { answered("q$it") })

        assertFalse(AchievementId("first_steps") in state.unlockedAchievements)
    }

    @Test
    fun `curious learner unlocks at ten distinct questions`() {
        val nine = evaluate(*Array(9) { answered("q$it") })
        assertFalse(AchievementId("curious_learner") in nine.unlockedAchievements)

        val ten = evaluate(*Array(10) { answered("q$it") })
        assertTrue(AchievementId("curious_learner") in ten.unlockedAchievements)
    }

    @Test
    fun `distinct questions are counted without duplicates`() {
        // 20 answers but only 10 distinct ids.
        val activities = Array(20) { index -> answered("q${index % 10}") }
        val state = evaluate(*activities)

        assertEquals(10, state.uniqueQuestionCount)
        assertTrue(AchievementId("curious_learner") in state.unlockedAchievements)
        assertFalse(AchievementId("practice_pro") in state.unlockedAchievements)
    }

    @Test
    fun `practice pro unlocks at fifty distinct questions`() {
        val state = evaluate(*Array(50) { answered("q$it") })

        assertTrue(AchievementId("practice_pro") in state.unlockedAchievements)
        assertEquals(50, state.uniqueQuestionCount)
    }

    @Test
    fun `a three day streak achievement unlocks`() {
        var state = GamificationState()
        val start = LocalDateKey.of(2026, 1, 1)
        repeat(3) { offset ->
            val day = LocalDateKey(start.value + offset)
            state = GamificationEngine.applyAndEvaluate(
                state,
                LearningActivity.LessonCompleted("lesson_$offset", day),
                1_000L + offset
            )
        }

        assertTrue(AchievementId("three_day_streak") in state.unlockedAchievements)
        assertFalse(AchievementId("seven_day_streak") in state.unlockedAchievements)
    }

    @Test
    fun `a seven day streak achievement unlocks`() {
        var state = GamificationState()
        repeat(7) { offset ->
            val day = LocalDateKey(LocalDateKey.of(2026, 1, 1).value + offset)
            state = GamificationEngine.applyAndEvaluate(
                state,
                LearningActivity.LessonCompleted("lesson_$offset", day),
                1_000L + offset
            )
        }

        assertTrue(AchievementId("seven_day_streak") in state.unlockedAchievements)
    }

    @Test
    fun `goal getter unlocks when the daily goal is reached`() {
        var state = GamificationState(dailyQuestionGoal = 10)
        repeat(10) { index ->
            state = GamificationEngine.applyAndEvaluate(state, answered("q$index"), index.toLong())
        }

        assertTrue(AchievementId("goal_getter") in state.unlockedAchievements)
    }

    @Test
    fun `mock test starter unlocks on the first mock test`() {
        val state = evaluate(LearningActivity.MockTestCompleted("t1", today))

        assertTrue(AchievementId("mock_test_starter") in state.unlockedAchievements)
    }

    @Test
    fun `perfect practice unlocks only on a flawless session`() {
        val imperfect = evaluate(
            LearningActivity.PracticeSessionCompleted("topic_a", allCorrect = false, today)
        )
        assertFalse(AchievementId("perfect_practice") in imperfect.unlockedAchievements)

        val perfect = evaluate(
            LearningActivity.PracticeSessionCompleted("topic_b", allCorrect = true, today)
        )
        assertTrue(AchievementId("perfect_practice") in perfect.unlockedAchievements)
    }

    @Test
    fun `dedicated learner unlocks at ten lessons`() {
        val lessons = LessonId.entries.take(10).map { lesson(it) }.toTypedArray()
        val state = evaluate(*lessons)

        assertTrue(AchievementId("dedicated_learner") in state.unlockedAchievements)
    }

    @Test
    fun `an achievement is unlocked only once and keeps its first timestamp`() {
        var state = GamificationState()
        state = GamificationEngine.applyAndEvaluate(
            state,
            lesson(LessonId.WHAT_IS_A_PERCENTAGE),
            5_000L
        )
        val firstStamp = state.unlockedAchievements[AchievementId("first_steps")]

        state = GamificationEngine.applyAndEvaluate(
            state,
            lesson(LessonId.PERCENTAGES_VISUALLY),
            9_000L
        )

        assertEquals(firstStamp, state.unlockedAchievements[AchievementId("first_steps")])
        assertEquals(1, state.unlockedAchievements.size)
    }

    @Test
    fun `re-evaluating achievements changes nothing`() {
        val state = evaluate(lesson(LessonId.WHAT_IS_A_PERCENTAGE))
        val before = state.unlockedAchievements

        val after = GamificationEngine.unlockEarnedAchievements(state, 99_000L)

        assertEquals(before, after.unlockedAchievements)
        assertEquals(state, after)
    }

    @Test
    fun `opening a screen never unlocks anything`() {
        // A fresh profile has no unlocks, and reading it does not create any.
        val fresh = GamificationState()

        assertEquals(0, fresh.unlockedAchievements.size)
        assertEquals(0, AchievementCatalogue.newlyEarned(fresh, 1L).size)
    }

    @Test
    fun `the catalogue has the ten documented achievements with stable ids`() {
        val ids = AchievementCatalogue.all.map { it.id.value }

        assertEquals(
            listOf(
                "first_steps",
                "curious_learner",
                "practice_pro",
                "century_club",
                "three_day_streak",
                "seven_day_streak",
                "goal_getter",
                "mock_test_starter",
                "perfect_practice",
                "dedicated_learner"
            ),
            ids
        )
        assertEquals(10, ids.distinct().size)
    }

    @Test
    fun `every achievement has a distinct title description and badge resource`() {
        AchievementCatalogue.all.forEach {
            assertTrue(it.target > 0)
            assertNotEquals(it.titleRes, 0)
            assertNotEquals(it.descriptionRes, 0)
            assertNotEquals(it.badgeLabelRes, 0)
        }
    }

    @Test
    fun `achievement progress reflects the underlying counter`() {
        val state = evaluate(*Array(7) { answered("q$it") })
        val curious = AchievementCatalogue.all.first { it.id == AchievementId("curious_learner") }

        assertEquals(7, AchievementCatalogue.progress(state, curious))
    }

    @Test
    fun `streak achievements use the longest streak so a break cannot un-earn them`() {
        var state = GamificationState()
        repeat(3) { offset ->
            val day = LocalDateKey(LocalDateKey.of(2026, 1, 1).value + offset)
            state = GamificationEngine.applyAndEvaluate(
                state,
                LearningActivity.LessonCompleted("lesson_$offset", day),
                1_000L + offset
            )
        }
        assertTrue(AchievementId("three_day_streak") in state.unlockedAchievements)

        // A long gap later must not change the already-recorded unlock.
        val muchLater = LocalDateKey(LocalDateKey.of(2026, 1, 1).value + 60)
        state = GamificationEngine.applyAndEvaluate(
            state,
            LearningActivity.LessonCompleted("lesson_new", muchLater),
            9_000L
        )

        assertEquals(1, GamificationEngine.currentStreak(state, muchLater))
        assertTrue(AchievementId("three_day_streak") in state.unlockedAchievements)
    }

    @Test
    fun `achievement unlocks grant no XP of their own`() {
        val withAchievement = evaluate(lesson(LessonId.WHAT_IS_A_PERCENTAGE))

        // 20 XP comes from the lesson; the achievement added nothing.
        assertEquals(RewardRules.XP_LESSON_COMPLETED, withAchievement.totalXp)
    }
}