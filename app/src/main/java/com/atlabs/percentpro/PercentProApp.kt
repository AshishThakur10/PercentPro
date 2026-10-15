package com.atlabs.percentpro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.AppTab
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.ui.components.PercentProBottomBar
import com.atlabs.percentpro.ui.components.ShortcutAction
import com.atlabs.percentpro.content.OnboardingStatus
import com.atlabs.percentpro.content.PracticePlanner
import com.atlabs.percentpro.model.DifficultyFilter
import com.atlabs.percentpro.model.PracticePlan
import com.atlabs.percentpro.model.RecommendationDestination
import com.atlabs.percentpro.ui.localization.ProvideAppLocale
import com.atlabs.percentpro.ui.screens.achievements.AchievementsRoute
import com.atlabs.percentpro.ui.screens.app.AppViewModel
import com.atlabs.percentpro.ui.screens.home.HomeRoute
import com.atlabs.percentpro.ui.screens.home.HomeViewModel
import com.atlabs.percentpro.ui.screens.insights.LearningInsightsRoute
import com.atlabs.percentpro.ui.screens.learningpath.LearningPathRoute
import com.atlabs.percentpro.ui.screens.learningpath.LearningPathViewModel
import com.atlabs.percentpro.ui.screens.lesson.LessonRoute
import com.atlabs.percentpro.ui.screens.mocktest.MockTestExamRoute
import com.atlabs.percentpro.ui.screens.mocktest.MockTestLandingRoute
import com.atlabs.percentpro.ui.screens.mocktest.MockTestSavedReviewRoute
import com.atlabs.percentpro.ui.screens.onboarding.OnboardingScreen
import com.atlabs.percentpro.ui.screens.settings.SettingsRoute
import com.atlabs.percentpro.ui.screens.practice.PracticeSessionRoute
import com.atlabs.percentpro.ui.screens.practice.PracticeTopicRoute
import com.atlabs.percentpro.ui.screens.practice.PracticeTopicViewModel

@Composable
fun PercentProApp(container: AppContainer) {
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.factory(container))
    val language by appViewModel.language.collectAsStateWithLifecycle()

    val onboardingStatus by appViewModel.onboardingStatus.collectAsStateWithLifecycle()

    // Re-resolving the configuration here is what makes a language change take
    // effect immediately, without recreating the activity.
    ProvideAppLocale(language = language) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // An exhaustive when, not a chain of boolean comparisons. The stored
            // flag used to be read as "onboarding pending?" when it actually
            // meant "onboarding seen?", so finishing the tour wrote the value
            // that kept the tour on screen and the buttons did nothing. Named
            // states make that inversion impossible to express, and a new state
            // would fail to compile here instead of quietly falling through.
            when (onboardingStatus) {
                OnboardingStatus.Loading -> Box(modifier = Modifier.fillMaxSize())

                OnboardingStatus.Pending -> OnboardingScreen(
                    onFinished = appViewModel::completeOnboarding,
                    modifier = Modifier.fillMaxSize()
                )

                OnboardingStatus.Done -> AppShell(
                    container = container,
                    language = language,
                    onLanguageSelected = appViewModel::setLanguage
                )
            }
        }
    }
}

/**
 * Full screen destinations that sit above the tab shell.
 *
 * A single optional key is all the navigation state this app needs, so no
 * navigation library is involved. The key is a string so it survives
 * configuration changes through [rememberSaveable].
 */
private sealed interface RootDestination {
    data class Lesson(val id: LessonId) : RootDestination
    data class Practice(val topic: LessonId) : RootDestination

    /** Mock test list, reached from the Practice tab. */
    data object MockTests : RootDestination
    data class MockTestExam(val id: MockTestId) : RootDestination

    /** A finished paper opened from the results list. Read-only. */
    data class MockTestSavedReview(val sessionId: String) : RootDestination

    /** Phase 5 achievements, reached from the home shortcuts. */
    data object Achievements : RootDestination

    /** Phase 7 personalised dashboard. */
    data object Insights : RootDestination

    /**
     * A targeted or mixed practice run.
     *
     * Held as a [PracticePlan] rather than a key so the exact question ids chosen
     * by the planner survive the trip; no serialisation is needed because the
     * screen is not restored from process death with an empty shell.
     */
    data class TargetedPractice(val plan: PracticePlan) : RootDestination
}

private const val LESSON_PREFIX = "lesson:"
private const val PRACTICE_PREFIX = "practice:"
private const val MOCK_TESTS_KEY = "mock_tests"
private const val MOCK_TEST_EXAM_PREFIX = "mock_test_exam:"
private const val MOCK_TEST_REVIEW_PREFIX = "mock_test_review:"
private const val ACHIEVEMENTS_KEY = "achievements"
private const val INSIGHTS_KEY = "insights"
private const val TARGETED_PRACTICE_KEY = "targeted_practice"

private fun destinationToKey(destination: RootDestination): String = when (destination) {
    is RootDestination.Lesson -> "$LESSON_PREFIX${destination.id.storageKey}"
    is RootDestination.Practice -> "$PRACTICE_PREFIX${destination.topic.storageKey}"
    RootDestination.MockTests -> MOCK_TESTS_KEY
    is RootDestination.MockTestExam -> "$MOCK_TEST_EXAM_PREFIX${destination.id.value}"
    is RootDestination.MockTestSavedReview -> "$MOCK_TEST_REVIEW_PREFIX${destination.sessionId}"
    RootDestination.Achievements -> ACHIEVEMENTS_KEY
    RootDestination.Insights -> INSIGHTS_KEY
    // Practice plans carry question ids, which are not encoded into the saved
    // destination key. Restoring one therefore falls back to the tab shell.
    is RootDestination.TargetedPractice -> TARGETED_PRACTICE_KEY
}

private fun destinationFromKey(key: String): RootDestination? = when {
    key.startsWith(LESSON_PREFIX) -> LessonId.fromStorageKey(key.removePrefix(LESSON_PREFIX))
        ?.let { RootDestination.Lesson(it) }

    key.startsWith(PRACTICE_PREFIX) -> LessonId.fromStorageKey(key.removePrefix(PRACTICE_PREFIX))
        ?.let { RootDestination.Practice(it) }

    key == MOCK_TESTS_KEY -> RootDestination.MockTests

    key.startsWith(MOCK_TEST_EXAM_PREFIX) -> MockTestId(
        key.removePrefix(MOCK_TEST_EXAM_PREFIX)
    ).let { RootDestination.MockTestExam(it) }

    key.startsWith(MOCK_TEST_REVIEW_PREFIX) -> key.removePrefix(MOCK_TEST_REVIEW_PREFIX)
        .takeIf { it.isNotBlank() }
        ?.let { RootDestination.MockTestSavedReview(it) }

    key == ACHIEVEMENTS_KEY -> RootDestination.Achievements

    key == INSIGHTS_KEY -> RootDestination.Insights

    // A saved key with no plan cannot rebuild the run; the caller shows the
    // practice tab instead of an empty session.
    key == TARGETED_PRACTICE_KEY -> null

    else -> null
}

@Composable
private fun AppShell(
    container: AppContainer,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    var selectedTabOrdinal by rememberSaveable { mutableIntStateOf(AppTab.HOME.ordinal) }
    var destinationKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTab = AppTab.entries[selectedTabOrdinal]

    // One path view model serves both the guard and the has-next calculation, so
    // the lesson screen and the path can never disagree.
    val pathViewModel: LearningPathViewModel =
        viewModel(factory = LearningPathViewModel.factory(container))
    val pathState by pathViewModel.uiState.collectAsStateWithLifecycle()

    val practiceTopicViewModel: PracticeTopicViewModel =
        viewModel(factory = PracticeTopicViewModel.factory(container))

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))

    val openLesson: (LessonId) -> Unit = { lessonId ->
        // The check happens here and again inside LessonViewModel; neither is
        // trusted on its own.
        if (pathViewModel.canOpen(lessonId)) {
            destinationKey = destinationToKey(RootDestination.Lesson(lessonId))
        }
    }
    val openPractice: (LessonId) -> Unit = { topic ->
        if (practiceTopicViewModel.canOpen(topic)) {
            destinationKey = destinationToKey(RootDestination.Practice(topic))
        }
    }
    val openMockTests = { destinationKey = destinationToKey(RootDestination.MockTests) }
    val openMockTestExam: (MockTestId) -> Unit = { testId ->
        destinationKey = destinationToKey(RootDestination.MockTestExam(testId))
    }
    val openMockTestResult: (String) -> Unit = { sessionId ->
        destinationKey = destinationToKey(RootDestination.MockTestSavedReview(sessionId))
    }
    val openAchievements = { destinationKey = destinationToKey(RootDestination.Achievements) }
    val openInsights = { destinationKey = destinationToKey(RootDestination.Insights) }

    val startPlannedPractice: (PracticePlan) -> Unit = { plan ->
        if (!plan.isEmpty) {
            destinationKey = destinationToKey(RootDestination.TargetedPractice(plan))
        }
    }

    // One place that turns any destination into a runnable plan, so a
    // recommendation and a dashboard card behave identically.
    val openRecommendation: (RecommendationDestination) -> Unit = { target ->
        when (target) {
            is RecommendationDestination.OpenLesson -> openLesson(target.lessonId)
            is RecommendationDestination.PracticeTopic ->
                startPlannedPractice(
                    PracticePlanner.planTopic(target.topic, container.questionRepository::questionsFor)
                )

            is RecommendationDestination.PracticeDifficulty -> startPlannedPractice(
                PracticePlanner.planTopic(
                    topic = target.topic,
                    questionsFor = container.questionRepository::questionsFor,
                    difficulty = DifficultyFilter.entries
                        .first { it.associatedDifficulty == target.difficulty }
                )
            )

            RecommendationDestination.MixedReview -> startPlannedPractice(
                PracticePlanner.planMixedReview(
                    topics = container.questionRepository.availableTopics(),
                    questionsFor = container.questionRepository::questionsFor
                )
            )

            RecommendationDestination.OpenInsights -> openInsights()
        }
    }

    // Home shortcuts only change where the user is; none of them can award a
    // reward, because every reward is reported by a ViewModel at the moment the
    // underlying activity happens.
    val openShortcut: (ShortcutAction) -> Unit = { action ->
        when (action) {
            ShortcutAction.CONTINUE_LEARNING -> {
                val next = homeViewModel.uiState.value.nextLesson
                if (next != null) openLesson(next.id) else selectedTabOrdinal = AppTab.LEARN.ordinal
            }

            ShortcutAction.TOPIC_PRACTICE -> selectedTabOrdinal = AppTab.PRACTICE.ordinal
            ShortcutAction.MOCK_TESTS -> openMockTests()
            ShortcutAction.ACHIEVEMENTS -> openAchievements()
            ShortcutAction.INSIGHTS -> openInsights()
        }
    }

    val closeDestination = { destinationKey = null }

    val savedDestinationKey = destinationKey
    val destination: RootDestination? = if (savedDestinationKey == null) {
        null
    } else {
        destinationFromKey(savedDestinationKey)
    }

    when (destination) {
        is RootDestination.Lesson -> {
            val lessonId = destination.id
            BackHandler(onBack = closeDestination)
            val lessonOrder = pathState.nodes
                .firstOrNull { it.lesson.id == lessonId }
                ?.lesson
                ?.order
            val hasNextLesson = remember(pathState, lessonOrder) {
                lessonOrder != null && pathState.nodes.any { it.lesson.order > lessonOrder }
            }
            LessonRoute(
                container = container,
                lessonId = lessonId,
                hasNextLesson = hasNextLesson,
                onExit = closeDestination
            )
            return
        }

        is RootDestination.Practice -> {
            BackHandler(onBack = closeDestination)
            PracticeSessionRoute(
                container = container,
                topic = destination.topic,
                language = language,
                onExit = closeDestination
            )
            return
        }

        RootDestination.MockTests -> {
            BackHandler(onBack = closeDestination)
            MockTestLandingRoute(
                container = container,
                onStartTest = openMockTestExam,
                onOpenResult = openMockTestResult,
                modifier = Modifier.fillMaxSize()
            )
            return
        }

        is RootDestination.MockTestExam -> {
            // No BackHandler here: MockTestExamRoute owns the back gesture so
            // that leaving a live paper goes through the same discard
            // confirmation as its close button.
            MockTestExamRoute(
                container = container,
                testId = destination.id,
                language = language,
                onExit = closeDestination,
                modifier = Modifier.fillMaxSize()
            )
            return
        }

        is RootDestination.TargetedPractice -> {
            val plan = destination.plan
            val primary = plan.primaryTopic
            if (primary == null) {
                closeDestination()
            } else {
                BackHandler(onBack = closeDestination)
                PracticeSessionRoute(
                    container = container,
                    topic = primary,
                    language = language,
                    plan = plan,
                    onExit = closeDestination,
                    modifier = Modifier.fillMaxSize()
                )
                return
            }
        }

        RootDestination.Insights -> {
            BackHandler(onBack = closeDestination)
            LearningInsightsRoute(
                container = container,
                onExit = closeDestination,
                onStartPractice = startPlannedPractice,
                onOpenLesson = { lessonId ->
                    destinationKey = destinationToKey(RootDestination.Lesson(lessonId))
                },
                modifier = Modifier.fillMaxSize()
            )
            return
        }

        RootDestination.Achievements -> {
            BackHandler(onBack = closeDestination)
            AchievementsRoute(
                container = container,
                language = language,
                onExit = closeDestination,
                modifier = Modifier.fillMaxSize()
            )
            return
        }

        is RootDestination.MockTestSavedReview -> {
            BackHandler(onBack = closeDestination)
            MockTestSavedReviewRoute(
                container = container,
                sessionId = destination.sessionId,
                language = language,
                onExit = closeDestination,
                modifier = Modifier.fillMaxSize()
            )
            return
        }

        null -> Unit
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            PercentProBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTabOrdinal = it.ordinal }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.HOME -> HomeRoute(
                    container = container,
                    selectedLanguage = language,
                    onLanguageSelected = onLanguageSelected,
                    onLessonSelected = openLesson,
                    onOpenLearningPath = { selectedTabOrdinal = AppTab.LEARN.ordinal },
                    onShortcut = openShortcut,
                    onRecommendation = openRecommendation
                )

                AppTab.LEARN -> LearningPathRoute(
                    container = container,
                    onLessonSelected = { node -> openLesson(node.lesson.id) }
                )

                AppTab.PRACTICE -> PracticeTopicRoute(
                    container = container,
                    onTopicSelected = openPractice,
                    onOpenMockTests = openMockTests
                )

                AppTab.SETTINGS -> SettingsRoute(
                    container = container,
                    onOpenAchievements = openAchievements
                )
            }
        }
    }
}