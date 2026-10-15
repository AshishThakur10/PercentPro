package com.atlabs.percentpro.model

import androidx.annotation.StringRes

/** Stable identifier for an achievement. Never localised, never renumbered. */
@JvmInline
value class AchievementId(val value: String) {
    override fun toString(): String = value

    companion object {
        fun parse(value: String): AchievementId? =
            value.takeIf { it.isNotBlank() && it.none { ch -> ch == '|' || ch == ',' || ch == '=' } }
                ?.let(::AchievementId)
    }
}

/** The measurable quantity an achievement is built on. */
enum class AchievementMetric {
    LESSONS_COMPLETED,
    DISTINCT_QUESTIONS_ANSWERED,
    STREAK_DAYS,
    DAILY_GOAL_REACHED,
    MOCK_TESTS_COMPLETED,
    PERFECT_PRACTICE_SESSION
}

/**
 * One entry in the achievement catalogue.
 *
 * Definitions are data, not logic: adding an achievement means adding a row
 * here and evaluating it centrally, with no screen needing to know it exists.
 */
data class AchievementDefinition(
    val id: AchievementId,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val badgeLabelRes: Int,
    val metric: AchievementMetric,
    val target: Int
)

/**
 * An achievement as the UI sees it: the definition, whether it is unlocked, when,
 * and how far along the learner is.
 */
data class AchievementStatus(
    val definition: AchievementDefinition,
    val unlockedAtEpochMillis: Long?,
    val current: Int
) {
    val isUnlocked: Boolean get() = unlockedAtEpochMillis != null

    val currentValue: Int get() = if (isUnlocked) definition.target else current

    val progressFraction: Float
        get() = if (definition.target <= 0) {
            1f
        } else {
            (currentValue.toFloat() / definition.target).coerceIn(0f, 1f)
        }

    /** The headline number a locked achievement shows, e.g. 27/50. */
    val progressText: String get() = "${currentValue.coerceAtMost(definition.target)}/${definition.target}"
}