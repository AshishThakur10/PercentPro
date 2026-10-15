package com.atlabs.percentpro.model

/**
 * What one reported activity actually earned.
 *
 * Returned rather than shown from a screen's own arithmetic, so the number a
 * learner sees is always the number that was written to the ledger.
 */
data class RewardOutcome(
    val xpAwarded: Int = 0,
    val unlockedAchievements: List<AchievementId> = emptyList()
) {
    val hasXp: Boolean get() = xpAwarded > 0

    val hasAchievements: Boolean get() = unlockedAchievements.isNotEmpty()

    val isEmpty: Boolean get() = !hasXp && !hasAchievements

    companion object {
        val NONE = RewardOutcome()
    }
}
