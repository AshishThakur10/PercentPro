package com.atlabs.percentpro.ui.screens.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R
import com.atlabs.percentpro.content.ExplanationBlock
import com.atlabs.percentpro.content.FormulaBlock
import com.atlabs.percentpro.content.IntroductionBlock
import com.atlabs.percentpro.content.LessonBlock
import com.atlabs.percentpro.content.PercentageGridBlock
import com.atlabs.percentpro.content.SummaryBlock
import com.atlabs.percentpro.content.WorkedExampleBlock
import com.atlabs.percentpro.ui.components.PercentageGrid

/**
 * Renders one content block.
 *
 * The `when` here is the only place that knows the block shapes; adding a new
 * block type means adding a case, and no lesson screen changes.
 */
@Composable
fun LessonBlockContent(
    block: LessonBlock,
    modifier: Modifier = Modifier
) {
    when (block) {
        is IntroductionBlock -> BlockShell(
            label = stringResource(R.string.block_intro_label),
            title = stringResource(block.titleRes),
            modifier = modifier
        ) {
            BodyText(stringResource(block.bodyRes))
        }

        is ExplanationBlock -> BlockShell(
            label = stringResource(R.string.block_explanation_label),
            title = stringResource(block.titleRes),
            modifier = modifier
        ) {
            BodyText(stringResource(block.bodyRes))
            block.bullets.forEach { bulletRes ->
                BulletRow(stringResource(bulletRes))
            }
        }

        is FormulaBlock -> FormulaBlockContent(block)

        is WorkedExampleBlock -> WorkedExampleBlockContent(block)

        is PercentageGridBlock -> PercentageGridBlockContent(block)

        is SummaryBlock -> BlockShell(
            label = stringResource(R.string.block_summary_label),
            title = stringResource(block.titleRes),
            modifier = modifier
        ) {
            block.bullets.forEach { bulletRes ->
                BulletRow(stringResource(bulletRes), emphasised = true)
            }
        }
    }
}

@Composable
private fun BlockShell(
    label: String,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
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
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .semantics { heading() }
            )
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun FormulaBlockContent(block: FormulaBlock, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.block_formula_label).uppercase(),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = stringResource(block.formulaRes),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .semantics { heading() }
            )
            Text(
                text = stringResource(block.captionRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun WorkedExampleBlockContent(block: WorkedExampleBlock, modifier: Modifier = Modifier) {
    BlockShell(
        label = stringResource(R.string.block_example_label),
        title = stringResource(block.titleRes),
        modifier = modifier
    ) {
        block.steps.forEach { step ->
            Column(modifier = Modifier.fillMaxWidth()) {
                BodyText(stringResource(step.promptRes))
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(step.resultRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun PercentageGridBlockContent(block: PercentageGridBlock, modifier: Modifier = Modifier) {
    BlockShell(
        label = stringResource(R.string.block_visual_label),
        title = stringResource(block.titleRes),
        modifier = modifier
    ) {
        BodyText(stringResource(block.captionRes))
        PercentageGrid(
            selectablePercents = block.selectablePercents,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun BulletRow(text: String, emphasised: Boolean = false) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .background(
                    color = if (emphasised) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    shape = CircleShape
                )
                .padding(4.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}