package com.atlabs.percentpro.ui.gamification

import com.atlabs.percentpro.content.AchievementCatalogue
import com.atlabs.percentpro.content.GamificationEngine
import com.atlabs.percentpro.content.GamificationState
import com.atlabs.percentpro.model.AchievementId
import com.atlabs.percentpro.model.AchievementStatus
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.RewardRules

/**
 * Everything the gamification UI needs, resolved for one specific day.
 *
 * Built in one place so Home and Achievements cannot disagree about a streak or
 * a goal, and so a day change is handled by re-mapping rather than by each
 * screen remembering to reset something.
 */
data class GamificationSnapshot(
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val isStreakAtRisk: Boolean = false,
    val hasAnyActivity: Boolean = false,
    val dailyGoal: Int = RewardRules.DEFAULT_DAILY_GOAL,
    val todayQuestionCount: Int = 0,
    val dailyGoalProgress: Float = 0f,
    val isDailyGoalReached: Boolean = false,
    val uniqueQuestionCount: Int = 0,
    val lessonCount: Int = 0,
    val mockTestCount: Int = 0,
    val achievements: List<AchievementStatus> = emptyList(),
    val today: LocalDateKey? = null
) {
    val unlockedAchievementCount: Int get() = achievements.count { it.isUnlocked }

    val totalAchievementCount: Int get() = achievements.size

    companion object {
        fun from(state: GamificationState, today: LocalDateKey): GamificationSnapshot {
            val statuses = AchievementCatalogue.all.map { definition ->
                AchievementStatus(
                    definition = definition,
                    unlockedAtEpochMillis = state.unlockedAchievements[definition.id],
                    current = AchievementCatalogue.progress(state, definition)
                )
            }
            return GamificationSnapshot(
                totalXp = state.totalXp.coerceAtLeast(0),
                currentStreak = GamificationEngine.currentStreak(state, today),
                longestStreak = GamificationEngine.longestStreak(state),
                isStreakAtRisk = GamificationEngine.isStreakAtRisk(state, today),
                hasAnyActivity = state.qualifyingDates.isNotEmpty(),
                dailyGoal = state.dailyQuestionGoal,
                todayQuestionCount = state.todayQuestionCount,
                dailyGoalProgress = state.dailyGoalProgress,
                isDailyGoalReached = state.isDailyGoalReached,
                uniqueQuestionCount = state.uniqueQuestionCount,
                lessonCount = state.lessonCount,
                mockTestCount = state.mockTestCount,
                achievements = statuses.sortedWith(
                    compareByDescending<AchievementStatus> { it.isUnlocked }
                        .thenByDescending { it.progressFraction }
                ),
                today = today
            )
        }
    }
}

/** Achievement ids unlocked by an activity, for one-shot UI feedback. */
typealias UnlockedAchievements = List<AchievementId>