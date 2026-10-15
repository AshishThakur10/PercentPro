package com.atlabs.percentpro.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlabs.percentpro.R
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.ui.components.BrandHeader
import com.atlabs.percentpro.ui.components.GamificationCard
import com.atlabs.percentpro.ui.components.HomeShortcutsCard
import com.atlabs.percentpro.ui.components.LanguageSelector
import com.atlabs.percentpro.ui.components.LearningPathNode
import com.atlabs.percentpro.ui.components.PrimaryLessonAction
import com.atlabs.percentpro.ui.components.ProgressSummaryCard
import com.atlabs.percentpro.ui.components.RecommendationCard
import com.atlabs.percentpro.ui.components.SectionHeader
import com.atlabs.percentpro.ui.components.ShortcutAction
import com.atlabs.percentpro.ui.screens.insights.recommendationActionLabel
import com.atlabs.percentpro.ui.screens.insights.recommendationReason
import com.atlabs.percentpro.ui.screens.insights.recommendationTitle
import com.atlabs.percentpro.ui.components.WelcomeCard

@Composable
fun HomeRoute(
    container: AppContainer,
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLessonSelected: (LessonId) -> Unit,
    onOpenLearningPath: () -> Unit,
    onShortcut: (ShortcutAction) -> Unit,
    onRecommendation: (com.atlabs.percentpro.model.RecommendationDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        selectedLanguage = selectedLanguage,
        onLanguageSelected = onLanguageSelected,
        onLessonSelected = { lessonId ->
            // Guarded here as well as on the node, so a stale tap cannot open a
            // lesson that has since been locked again.
            if (viewModel.canOpen(lessonId)) onLessonSelected(lessonId)
        },
        onOpenLearningPath = onOpenLearningPath,
        onShortcut = onShortcut,
        onRecommendation = onRecommendation,
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLessonSelected: (LessonId) -> Unit,
    onOpenLearningPath: () -> Unit,
    onShortcut: (ShortcutAction) -> Unit,
    onRecommendation: (com.atlabs.percentpro.model.RecommendationDestination) -> Unit,
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
                LazyColumn(
                    modifier = Modifier
                        .width(contentWidth)
                        .testTag("home_list"),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 20.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "brand") { BrandHeader() }

                    item(key = "language") {
                        LanguageSelector(
                            selected = selectedLanguage,
                            onLanguageSelected = onLanguageSelected,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item(key = "welcome") { WelcomeCard() }

                    item(key = "cta") {
                        PrimaryLessonAction(
                            uiState = uiState,
                            onStart = { uiState.nextLesson?.id?.let(onLessonSelected) },
                            onOpenLearningPath = onOpenLearningPath
                        )
                    }

                    uiState.recommendation?.let { recommendation ->
                        item(key = "recommendation") {
                            RecommendationCard(
                                title = recommendationTitle(recommendation.titleKey),
                                reason = recommendationReason(
                                    recommendation.reasonCode,
                                    recommendation.reasonArgs
                                ),
                                actionLabel = recommendationActionLabel(recommendation.destination),
                                onAction = { onRecommendation(recommendation.destination) }
                            )
                        }
                    }

                    item(key = "gamification") {
                        GamificationCard(snapshot = uiState.gamification)
                    }

                    item(key = "shortcuts") {
                        HomeShortcutsCard(onAction = onShortcut)
                    }

                    item(key = "progress") {
                        ProgressSummaryCard(
                            completedCount = uiState.completedCount,
                            implementedCount = uiState.implementedCount,
                            completionPercent = uiState.completionPercent,
                            hasStarted = uiState.hasStarted
                        )
                    }

                    item(key = "path_header") {
                        SectionHeader(
                            title = stringResource(R.string.learning_path_title),
                            subtitle = stringResource(R.string.learning_path_subtitle)
                        )
                    }

                    items(
                        items = uiState.path.nodes,
                        key = { it.lesson.id.storageKey }
                    ) { node ->
                        LearningPathNode(
                            node = node,
                            onClick = { onLessonSelected(node.lesson.id) },
                            showConnector = node.lesson.order != uiState.path.nodes.last().lesson.order
                        )
                    }
                }
            }
        }
    }
}