package com.atlabs.percentpro.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R

/**
 * Progress across the lessons that actually exist today.
 *
 * XP and streak stay at zero: they are wired up but nothing awards them yet, and
 * inventing a reward here would misrepresent the app.
 */
@Composable
fun ProgressSummaryCard(
    completedCount: Int,
    implementedCount: Int,
    completionPercent: Int,
    hasStarted: Boolean,
    modifier: Modifier = Modifier
) {
    val completionDescription =
        stringResource(R.string.progress_completion_indicator, completionPercent)
    val animatedProgress by animateFloatAsState(
        targetValue = completionPercent / 100f,
        label = "courseCompletion"
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
                text = stringResource(R.string.progress_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(
                    value = stringResource(R.string.progress_xp_value, 0),
                    label = stringResource(R.string.progress_stat_xp),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = pluralStringResource(R.plurals.progress_streak_value, 0, 0),
                    label = stringResource(R.string.progress_stat_streak),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = stringResource(R.string.progress_completion_value, completionPercent),
                    label = stringResource(R.string.progress_stat_completion),
                    modifier = Modifier.weight(1f)
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(8.dp)
                    .clearAndSetSemantics { contentDescription = completionDescription },
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                drawStopIndicator = {}
            )

            Text(
                text = if (hasStarted) {
                    completionDescription
                } else {
                    stringResource(R.string.progress_nothing_yet)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = stringResource(
                    R.string.progress_lessons_done,
                    completedCount,
                    implementedCount
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
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