package com.atlabs.percentpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R
import com.atlabs.percentpro.content.PercentageMath

private const val GRID_COLUMNS = 10

/**
 * A 10 by 10 grid where every square is 1% of the whole.
 *
 * The shaded count comes from [PercentageMath.highlightedSquareCount] so the
 * picture and the maths cannot disagree. Plain Boxes are used rather than a lazy
 * grid because there are only 100 of them and they must all be laid out together
 * to read as a single picture.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PercentageGrid(
    selectablePercents: List<Int>,
    modifier: Modifier = Modifier
) {
    val initialPercent = selectablePercents.firstOrNull() ?: 0
    var selectedPercent by remember(selectablePercents) { mutableIntStateOf(initialPercent) }
    val shadedCount = PercentageMath.highlightedSquareCount(selectedPercent.toDouble())
    val gridDescription = stringResource(R.string.cd_percentage_grid, selectedPercent)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .semantics { contentDescription = gridDescription }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(GRID_COLUMNS) { row ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(GRID_COLUMNS) { column ->
                            val index = row * GRID_COLUMNS + column
                            GridCell(
                                shaded = index < shadedCount,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.grid_caption_value, selectedPercent),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        Text(
            text = stringResource(R.string.grid_shade_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )

        // FlowRow, not Row: a fixed Row cannot wrap, so wide labels at a large
        // font scale would push the last option off the edge with no way to
        // reach it.
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            selectablePercents.forEach { percent ->
                PercentSelector(
                    percent = percent,
                    selected = percent == selectedPercent,
                    onSelect = { selectedPercent = percent }
                )
            }
        }
    }
}

@Composable
private fun GridCell(shaded: Boolean, modifier: Modifier = Modifier) {
    val fill = if (shaded) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    // fillMaxHeight is required: the Row only hands down a max-height
    // constraint, and a Box with no content would otherwise collapse to zero.
    Box(modifier = modifier.fillMaxHeight().background(color = fill))
}

@Composable
private fun PercentSelector(
    percent: Int,
    selected: Boolean,
    onSelect: () -> Unit
) {
    // primary/onPrimary rather than secondary/onSecondary: white on MainPurple
    // measured 3.96:1 for the 14sp label.
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val foreground = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val label = stringResource(R.string.grid_value, percent)

    Box(
        modifier = Modifier
            // 18dp of horizontal padding around a 19sp line is 43dp tall, which
            // is under the 48dp minimum target. Lifting it keeps the tap area
            // compliant without changing the visual size of the pill.
            .heightIn(min = 48.dp)
            .background(color = background, shape = MaterialTheme.shapes.small)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            )
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = androidx.compose.ui.semantics.Role.RadioButton
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = foreground)
    }
}