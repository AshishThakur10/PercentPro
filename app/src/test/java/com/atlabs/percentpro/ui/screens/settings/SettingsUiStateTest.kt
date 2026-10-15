package com.atlabs.percentpro.ui.screens.settings

import com.atlabs.percentpro.content.AchievementCatalogue
import com.atlabs.percentpro.content.GamificationState
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.AchievementStatus
import com.atlabs.percentpro.model.RewardRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The numbers the settings screen shows.
 *
 * The mapping from stored state to the displayed snapshot is separated from the
 * ViewModel so it can be checked against real domain objects. These are the
 * values a reader uses to decide whether the app is working, so a mapping that
 * quietly reports "21 of 21 lessons" or "0 achievements" is a real bug rather
 * than a cosmetic one.
 */
class SettingsUiStateTest {

    private fun statusOf(unlockedIds: Set<String>): List<AchievementStatus> =
        AchievementCatalogue.all.map { definition ->
            AchievementStatus(
                definition = definition,
                unlockedAtEpochMillis = unlockedIds
                    .takeIf { definition.id.value in it }
                    ?.let { 1_000L },
                current = 0
            )
        }

    private fun snapshot(
        language: AppLanguage = AppLanguage.ENGLISH,
        gamification: GamificationState = GamificationState(),
        unlocked: Set<String> = emptySet(),
        completed: Set<String> = emptySet(),
        totalLessons: Int = 21
    ) = buildSettingsUiState(
        language = language,
        gamification = gamification,
        achievements = statusOf(unlocked),
        completedLessonIds = completed,
        totalLessons = totalLessons
    )

    @Test
    fun `a fresh install reports zeroes, not blanks`() {
        val state = snapshot()

        assertEquals(0, state.completedLessons)
        assertEquals(0, state.totalXp)
        assertEquals(0, state.unlockedAchievements)
        assertEquals(21, state.totalLessons)
    }

    @Test
    fun `the default daily goal is reported when nothing is stored`() {
        assertEquals(RewardRules.DEFAULT_DAILY_GOAL, snapshot().dailyGoal)
    }

    @Test
    fun `a stored daily goal is reported back`() {
        listOf(10, 20, 30).forEach { goal ->
            val state = snapshot(gamification = GamificationState(dailyQuestionGoal = goal))
            assertEquals(goal, state.dailyGoal)
        }
    }

    @Test
    fun `completed lessons are counted, not listed`() {
        val state = snapshot(
            completed = setOf("what_is_a_percentage", "percent_of_a_number")
        )
        assertEquals(2, state.completedLessons)
    }

    @Test
    fun `progress is counted against the whole catalogue, not just what is done`() {
        // The failure this guards against is "2 of 2 lessons" after a course
        // grows: the denominator has to come from the catalogue, not from the
        // stored progress.
        val state = snapshot(completed = setOf("a", "b"), totalLessons = 21)
        assertEquals(2, state.completedLessons)
        assertEquals(21, state.totalLessons)
    }

    @Test
    fun `unlocked achievements are counted against the full list`() {
        val all = AchievementCatalogue.all
        val two = all.take(2).map { it.id.value }.toSet()

        val state = snapshot(unlocked = two)

        assertEquals(all.size, state.totalAchievements)
        assertEquals(2, state.unlockedAchievements)
    }

    @Test
    fun `no achievements are reported when none are unlocked`() {
        assertEquals(0, snapshot().unlockedAchievements)
        assertEquals(AchievementCatalogue.all.size, snapshot().totalAchievements)
    }

    @Test
    fun `the selected language is reported`() {
        assertEquals(AppLanguage.HINDI, snapshot(language = AppLanguage.HINDI).language)
        assertEquals(AppLanguage.ENGLISH, snapshot(language = AppLanguage.ENGLISH).language)
    }

    @Test
    fun `xp is reported from the ledger`() {
        val state = snapshot(gamification = GamificationState(totalXp = 340))
        assertEquals(340, state.totalXp)
    }

    @Test
    fun `the language predicates name the first entry, not English specifically`() {
        // Written so adding a third language does not silently mislabel it.
        assertTrue(snapshot(language = AppLanguage.entries.first()).isFirstLanguage)
        assertFalse(snapshot(language = AppLanguage.entries.last()).isFirstLanguage)
    }

    @Test
    fun `the goal predicate compares against the first configured option`() {
        assertTrue(
            snapshot(
                gamification = GamificationState(
                    dailyQuestionGoal = RewardRules.dailyGoalOptions.first()
                )
            ).isFirstGoal
        )
        RewardRules.dailyGoalOptions.drop(1).forEach { goal ->
            assertFalse(
                snapshot(gamification = GamificationState(dailyQuestionGoal = goal)).isFirstGoal
            )
        }
    }

    @Test
    fun `the empty snapshot is usable before the first emission`() {
        // Rendering from EMPTY must not produce a divide-by-zero or an
        // impossible "5 of 0" while DataStore is still loading.
        val empty = SettingsUiState.EMPTY
        assertEquals(0, empty.completedLessons)
        assertEquals(0, empty.totalLessons)
        assertEquals(0, empty.unlockedAchievements)
        assertEquals(0, empty.totalXp)
    }

    @Test
    fun `every selectable goal has a settings chip`() {
        // The screen renders whatever list it is handed, so the list itself has
        // to stay non-empty and strictly increasing.
        val options = RewardRules.dailyGoalOptions
        assertTrue(options.isNotEmpty())
        assertEquals(options.sorted().distinct(), options)
    }
}