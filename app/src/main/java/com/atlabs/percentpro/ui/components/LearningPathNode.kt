package com.atlabs.percentpro.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R
import com.atlabs.percentpro.model.Lesson
import com.atlabs.percentpro.model.LessonStatus
import com.atlabs.percentpro.ui.screens.learningpath.LessonNode

/**
 * One lesson on the path, drawn as a node joined to the next by a vertical
 * connector so the order is visible at a glance.
 */
@Composable
fun LearningPathNode(
    node: LessonNode,
    onClick: (LessonNode) -> Unit,
    modifier: Modifier = Modifier,
    showConnector: Boolean = true
) {
    Row(modifier = modifier.fillMaxWidth()) {
        PathConnectorColumn(
            status = node.status,
            order = node.lesson.order,
            isFirst = node.lesson.order == 1,
            showBelow = showConnector
        )
        LessonNodeCard(
            node = node,
            onClick = { onClick(node) },
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (showConnector) 14.dp else 0.dp)
        )
    }
}

@Composable
private fun PathConnectorColumn(
    status: LessonStatus,
    order: Int,
    isFirst: Boolean,
    showBelow: Boolean
) {
    val railColor = when (status) {
        LessonStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
        LessonStatus.LOCKED_SEQUENCE, LessonStatus.LOCKED_UPCOMING ->
            MaterialTheme.colorScheme.outlineVariant

        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(48.dp)
    ) {
        if (!isFirst) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(railColor)
            )
        } else {
            Box(modifier = Modifier.height(18.dp))
        }
        StatusBadge(status = status, order = order)
        if (showBelow) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(26.dp)
                    .background(railColor)
            )
        }
    }
}

@Composable
private fun StatusBadge(status: LessonStatus, order: Int) {
    // White on MainPurple measures 3.96:1, below the 4.5:1 floor for the 20dp
    // glyph, so the completed badge uses the container pair instead.
    val fill = when (status) {
        LessonStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
        LessonStatus.AVAILABLE, LessonStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        LessonStatus.LOCKED_SEQUENCE, LessonStatus.LOCKED_UPCOMING ->
            MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (status) {
        LessonStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
        LessonStatus.AVAILABLE, LessonStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onPrimary
        LessonStatus.LOCKED_SEQUENCE, LessonStatus.LOCKED_UPCOMING ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }

    // This badge is a sibling of LessonNodeCard, not a child of it, so the
    // card's merged contentDescription never covered it. It is given its own
    // label here: a tick or a padlock with contentDescription = null announces
    // as nothing at all, and the reader loses the one thing the rail conveys.
    val statusText = stringResource(status.labelRes())
    val badgeDescription = "$order. $statusText"

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color = fill, shape = CircleShape)
            .clearAndSetSemantics { contentDescription = badgeDescription },
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            LessonStatus.COMPLETED -> Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            LessonStatus.LOCKED_SEQUENCE, LessonStatus.LOCKED_UPCOMING -> Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )

            else -> Text(
                text = order.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

@Composable
private fun LessonNodeCard(
    node: LessonNode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lesson = node.lesson
    val statusText = stringResource(node.status.labelRes())
    val durationText = stringResource(R.string.lesson_duration, lesson.durationMinutes)
    val difficultyText = stringResource(lesson.difficulty.labelRes)
    val lockedReason = lockedReasonFor(node)
    val nodeDescription = stringResource(
        R.string.cd_lesson_node,
        lesson.order,
        stringResource(lesson.titleRes),
        statusText
    )

    Card(
        onClick = onClick,
        enabled = node.isOpenable,
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 92.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = nodeDescription
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = when (node.status) {
                LessonStatus.AVAILABLE, LessonStatus.IN_PROGRESS ->
                    MaterialTheme.colorScheme.primaryContainer

                else -> MaterialTheme.colorScheme.surface
            },
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (node.isOpenable) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.lesson_number, lesson.order),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(node.status)
            }
            Text(
                text = stringResource(lesson.titleRes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = stringResource(lesson.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // Was 3, which truncated the longest lesson summaries to an
                // ellipsis on a 320dp screen. Every lesson now shows in full;
                // the card grows rather than hiding content.
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = stringResource(
                    R.string.lesson_meta,
                    difficultyText,
                    durationText
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
            // A lesson that is catalogued but unwritten says so explicitly, so a
            // locked row is never mistaken for "finish the one before".
            if (node.isBlockedByUpcomingContent) {
                Text(
                    text = stringResource(R.string.lesson_upcoming_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            if (lockedReason != null) {
                Text(
                    text = lockedReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.extraSmall
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun lockedReasonFor(node: LessonNode): String? = when (node.status) {
    LessonStatus.LOCKED_SEQUENCE -> node.blockedByTitleRes?.let { titleRes ->
        stringResource(R.string.lesson_locked_sequence_reason, stringResource(titleRes))
    }

    LessonStatus.LOCKED_UPCOMING -> stringResource(R.string.lesson_locked_upcoming_reason)
    else -> null
}

@Composable
private fun StatusPill(status: LessonStatus) {
    // COMPLETED and IN_PROGRESS used to sit on `secondary` with `onSecondary`
    // text, which is white on MainPurple: 3.96:1, under the 4.5:1 floor for
    // 12sp label text. `primaryContainer` with `onPrimaryContainer` is the pair
    // the rest of the app already uses for a selected control, and measures
    // 8.6:1 in light and 6.0:1 in dark.
    val background = when (status) {
        LessonStatus.COMPLETED, LessonStatus.IN_PROGRESS ->
            MaterialTheme.colorScheme.primaryContainer

        LessonStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
        LessonStatus.LOCKED_SEQUENCE, LessonStatus.LOCKED_UPCOMING ->
            MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = when (status) {
        LessonStatus.COMPLETED, LessonStatus.IN_PROGRESS ->
            MaterialTheme.colorScheme.onPrimaryContainer

        LessonStatus.AVAILABLE -> MaterialTheme.colorScheme.onPrimary
        LessonStatus.LOCKED_SEQUENCE, LessonStatus.LOCKED_UPCOMING ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = stringResource(status.labelRes()),
        style = MaterialTheme.typography.labelMedium,
        color = foreground,
        modifier = Modifier
            .background(color = background, shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@StringRes
fun LessonStatus.labelRes(): Int = when (this) {
    LessonStatus.AVAILABLE -> R.string.lesson_status_available
    LessonStatus.IN_PROGRESS -> R.string.lesson_status_in_progress
    LessonStatus.COMPLETED -> R.string.lesson_status_completed
    LessonStatus.LOCKED_SEQUENCE -> R.string.lesson_status_locked
    LessonStatus.LOCKED_UPCOMING -> R.string.lesson_status_upcoming
}