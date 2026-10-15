package com.atlabs.percentpro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R
import com.atlabs.percentpro.ui.gamification.GamificationSnapshot

/**
 * Total XP, the streak and today's goal.
 *
 * Every number comes from the gamification snapshot, which is derived from
 * persisted state, so this card shows zero on a fresh install rather than
 * anything invented. Streak and goal are shown as separate values because they
 * are separate features: reaching the goal is not what maintains a streak.
 */
@Composable
fun GamificationCard(
    snapshot: GamificationSnapshot,
    modifier: Modifier = Modifier
) {
    val goalDescription = stringResource(
        R.string.gam_daily_goal_progress_a11y,
        snapshot.todayQuestionCount,
        snapshot.dailyGoal
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.gam_section_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.gam_section_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GamificationStat(
                    value = stringResource(R.string.gam_total_xp_value, snapshot.totalXp),
                    label = stringResource(R.string.gam_total_xp),
                    modifier = Modifier.weight(1f)
                )
                GamificationStat(
                    value = if (snapshot.currentStreak > 0) {
                        pluralStringResource(
                            R.plurals.gam_streak_days,
                            snapshot.currentStreak,
                            snapshot.currentStreak
                        )
                    } else {
                        stringResource(R.string.gam_streak_zero)
                    },
                    label = stringResource(R.string.gam_streak_current),
                    modifier = Modifier.weight(1f)
                )
                GamificationStat(
                    value = stringResource(
                        R.string.gam_achievements_short,
                        snapshot.unlockedAchievementCount,
                        snapshot.totalAchievementCount
                    ),
                    label = stringResource(R.string.gam_streak_achievements_label),
                    modifier = Modifier.weight(1f)
                )
            }

            // Daily goal
            Text(
                text = stringResource(R.string.gam_daily_goal),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                text = stringResource(
                    R.string.gam_daily_goal_progress,
                    snapshot.todayQuestionCount,
                    snapshot.dailyGoal
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            LinearProgressIndicator(
                progress = { snapshot.dailyGoalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(8.dp)
                    .clearAndSetSemantics { contentDescription = goalDescription },
                color = if (snapshot.isDailyGoalReached) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                drawStopIndicator = {}
            )
            Text(
                text = when {
                    snapshot.isDailyGoalReached -> stringResource(R.string.gam_daily_goal_reached)
                    snapshot.currentStreak == 0 && !snapshot.hasAnyActivity ->
                        stringResource(R.string.gam_streak_none)
                    snapshot.isStreakAtRisk -> stringResource(R.string.gam_streak_at_risk)
                    else -> stringResource(R.string.gam_streak_explainer)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (snapshot.longestStreak > 0) {
                Text(
                    text = stringResource(R.string.gam_streak_longest_value, snapshot.longestStreak),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun GamificationStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}