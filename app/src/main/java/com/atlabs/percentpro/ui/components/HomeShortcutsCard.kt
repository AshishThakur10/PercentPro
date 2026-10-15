package com.atlabs.percentpro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R

/** A jump target offered on the home screen. */
enum class ShortcutAction {
    CONTINUE_LEARNING,
    TOPIC_PRACTICE,
    MOCK_TESTS,
    ACHIEVEMENTS,
    INSIGHTS
}

/**
 * The home shortcuts row.
 *
 * Deliberately a single compact card rather than four large tiles: the home
 * screen already carries the brand header, language selector, welcome card,
 * primary lesson action, progress summary and the full learning path, so
 * anything heavier would push the path off a phone screen.
 */
@Composable
fun HomeShortcutsCard(
    onAction: (ShortcutAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
            Text(
                text = stringResource(R.string.gam_shortcuts_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp, top = 6.dp, bottom = 2.dp)
                    .semantics { heading() }
            )
            ShortcutRow(
                labelRes = R.string.gam_shortcut_continue,
                onClick = { onAction(ShortcutAction.CONTINUE_LEARNING) }
            )
            ShortcutRow(
                labelRes = R.string.gam_shortcut_practice,
                onClick = { onAction(ShortcutAction.TOPIC_PRACTICE) }
            )
            ShortcutRow(
                labelRes = R.string.gam_shortcut_mock,
                onClick = { onAction(ShortcutAction.MOCK_TESTS) }
            )
            ShortcutRow(
                labelRes = R.string.gam_shortcut_achievements,
                onClick = { onAction(ShortcutAction.ACHIEVEMENTS) }
            )
            ShortcutRow(
                labelRes = R.string.rec_view_insights,
                onClick = { onAction(ShortcutAction.INSIGHTS) }
            )
        }
    }
}

@Composable
private fun ShortcutRow(
    labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(labelRes)
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start
            )
            Text(
                text = stringResource(R.string.gam_shortcut_go),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}