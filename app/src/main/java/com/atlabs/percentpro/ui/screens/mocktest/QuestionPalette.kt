package com.atlabs.percentpro.ui.screens.mocktest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R
import com.atlabs.percentpro.content.QuestionPaletteState

/**
 * Legend for the palette.
 *
 * State is never signalled by colour alone: every palette cell also carries a
 * tick for answered and a star for marked, and every cell exposes its state in
 * its accessibility label.
 */
@Composable
fun QuestionPalette(
    states: List<QuestionPaletteState>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mock_palette_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.mock_palette_legend),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        states.chunked(PALETTE_COLUMNS).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEachIndexed { columnIndex, state ->
                    val index = rowIndex * PALETTE_COLUMNS + columnIndex
                    PaletteCell(
                        number = index + 1,
                        state = state,
                        isCurrent = index == currentIndex,
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Keep the last row aligned when it is not full.
                repeat(PALETTE_COLUMNS - row.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PaletteCell(
    number: Int,
    state: QuestionPaletteState,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = stringResource(
        R.string.cd_mock_palette_cell,
        number,
        stringResource(state.labelRes())
    )

    val container = state.containerColor()
    val content = state.contentColor()

    // The description sits on the clickable Surface itself, merged. It used to
    // sit on the wrapping Box, which is a separate semantics node from the
    // Surface that actually takes the tap — so the label was attached to
    // something that is not the target, and the number and the two state icons
    // below it were announced separately as well.
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(CELL_SIZE)
            .semantics(mergeDescendants = true) { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
        border = BorderStroke(
            width = if (isCurrent) 2.dp else 1.dp,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else state.borderColor()
        )
    ) {
        Box(
            modifier = Modifier.size(CELL_SIZE),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = content
            )
            if (state.isAnswered) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(12.dp)
                )
            }
            if (state.isMarkedForReview) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .size(12.dp)
                )
            }
        }
    }
}

@Composable
internal fun QuestionPaletteState.labelRes(): Int = when (this) {
    QuestionPaletteState.NOT_VISITED -> R.string.mock_palette_state_not_visited
    QuestionPaletteState.VISITED_UNANSWERED -> R.string.mock_palette_state_visited
    QuestionPaletteState.ANSWERED -> R.string.mock_palette_state_answered
    QuestionPaletteState.MARKED_REVIEW -> R.string.mock_palette_state_review
    QuestionPaletteState.ANSWERED_AND_MARKED_REVIEW -> R.string.mock_palette_state_answered_review
}

@Composable
private fun QuestionPaletteState.containerColor(): Color = when (this) {
    QuestionPaletteState.NOT_VISITED -> MaterialTheme.colorScheme.surfaceVariant
    QuestionPaletteState.VISITED_UNANSWERED -> MaterialTheme.colorScheme.tertiaryContainer
    QuestionPaletteState.ANSWERED -> MaterialTheme.colorScheme.primaryContainer
    QuestionPaletteState.MARKED_REVIEW -> MaterialTheme.colorScheme.secondaryContainer
    QuestionPaletteState.ANSWERED_AND_MARKED_REVIEW -> MaterialTheme.colorScheme.primary
}

@Composable
private fun QuestionPaletteState.contentColor(): Color = when (this) {
    QuestionPaletteState.NOT_VISITED -> MaterialTheme.colorScheme.onSurfaceVariant
    QuestionPaletteState.VISITED_UNANSWERED -> MaterialTheme.colorScheme.onTertiaryContainer
    QuestionPaletteState.ANSWERED -> MaterialTheme.colorScheme.onPrimaryContainer
    QuestionPaletteState.MARKED_REVIEW -> MaterialTheme.colorScheme.onSecondaryContainer
    QuestionPaletteState.ANSWERED_AND_MARKED_REVIEW -> MaterialTheme.colorScheme.onPrimary
}

@Composable
private fun QuestionPaletteState.borderColor(): Color = when (this) {
    QuestionPaletteState.NOT_VISITED -> MaterialTheme.colorScheme.outline
    QuestionPaletteState.VISITED_UNANSWERED -> MaterialTheme.colorScheme.tertiary
    QuestionPaletteState.ANSWERED -> MaterialTheme.colorScheme.primary
    QuestionPaletteState.MARKED_REVIEW -> MaterialTheme.colorScheme.secondary
    QuestionPaletteState.ANSWERED_AND_MARKED_REVIEW -> MaterialTheme.colorScheme.primary
}

private const val PALETTE_COLUMNS = 5
private val CELL_SIZE = 52.dp