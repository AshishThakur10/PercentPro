package com.atlabs.percentpro.ui.screens.learningpath

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlabs.percentpro.R
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.ui.components.LearningPathNode
import com.atlabs.percentpro.ui.components.SectionHeader

@Composable
fun LearningPathRoute(
    container: AppContainer,
    onLessonSelected: (LessonNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LearningPathViewModel =
        viewModel(factory = LearningPathViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LearningPathScreen(
        uiState = uiState,
        onLessonSelected = onLessonSelected,
        modifier = modifier
    )
}

@Composable
fun LearningPathScreen(
    uiState: LearningPathUiState,
    onLessonSelected: (LessonNode) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val contentWidth = if (maxWidth > 640.dp) 640.dp else maxWidth

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LearningPathList(
                    uiState = uiState,
                    onLessonSelected = onLessonSelected,
                    contentWidth = contentWidth
                )
            }
        }
    }
}

@Composable
fun LearningPathList(
    uiState: LearningPathUiState,
    onLessonSelected: (LessonNode) -> Unit,
    contentWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .width(contentWidth)
            .testTag("learning_path_list"),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item(key = "header") {
            SectionHeader(
                title = stringResource(R.string.learning_path_full_title),
                subtitle = stringResource(R.string.learning_path_full_subtitle)
            )
        }
        item(key = "progress") {
            Text(
                text = stringResource(
                    R.string.progress_lessons_done,
                    uiState.completedCount,
                    uiState.implementedCount
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 18.dp)
            )
        }

        // Rendered as explicit sections so the course reads as three bands
        // rather than one long list, with each band's progress visible.
        val sections = uiState.sections.ifEmpty {
            listOf(PathSection(com.atlabs.percentpro.content.CourseSection.forOrder(1), uiState.nodes))
        }
        sections.forEach { section ->
            if (section.nodes.isNotEmpty()) {
                item(key = "section_${section.section.name}") {
                    CourseSectionHeader(section = section)
                }
            }
            items(
                items = section.nodes,
                key = { it.lesson.id.storageKey }
            ) { node ->
                LearningPathNode(
                    node = node,
                    onClick = onLessonSelected,
                    showConnector = node.lesson.order != section.nodes.last().lesson.order
                )
            }
        }
    }
}

/**
 * Header for one course section: its name, what it covers, and how much of it
 * is done.
 */
@Composable
private fun CourseSectionHeader(
    section: PathSection,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = stringResource(section.section.titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(section.section.descriptionRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(
                R.string.section_progress,
                section.completedCount,
                section.nodes.size
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
