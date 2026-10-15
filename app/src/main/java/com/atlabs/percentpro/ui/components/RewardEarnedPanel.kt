package com.atlabs.percentpro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R
import com.atlabs.percentpro.content.AchievementCatalogue
import com.atlabs.percentpro.model.RewardOutcome

/**
 * The reward a single action earned: XP gained, and anything newly unlocked.
 *
 * Rendered from a [RewardOutcome], which is the same value the repository wrote,
 * so what is announced is always what was actually credited. Renders nothing at
 * all for an outcome with nothing in it, which is what a repeated action returns.
 */
@Composable
fun RewardEarnedPanel(
    outcome: RewardOutcome,
    modifier: Modifier = Modifier
) {
    if (outcome.isEmpty) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (outcome.hasXp) {
            Text(
                text = stringResource(R.string.gam_xp_earned_generic, outcome.xpAwarded),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (outcome.hasAchievements) {
            Text(
                text = stringResource(R.string.gam_achievements_unlocked_header),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            outcome.unlockedAchievements.forEach { id ->
                val definition = AchievementCatalogue.byId(id) ?: return@forEach
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(definition.titleRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}