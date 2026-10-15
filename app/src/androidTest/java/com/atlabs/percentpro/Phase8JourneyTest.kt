package com.atlabs.percentpro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.RewardRules
import com.atlabs.percentpro.ui.screens.onboarding.OnboardingScreen
import com.atlabs.percentpro.ui.screens.onboarding.TEST_TAG_ADVANCE
import com.atlabs.percentpro.ui.screens.onboarding.TEST_TAG_ONBOARDING
import com.atlabs.percentpro.ui.screens.onboarding.TEST_TAG_SKIP
import com.atlabs.percentpro.ui.screens.settings.SettingsScreen
import com.atlabs.percentpro.ui.screens.settings.SettingsUiState
import com.atlabs.percentpro.ui.screens.settings.TEST_TAG_ACHIEVEMENTS
import com.atlabs.percentpro.ui.screens.settings.TEST_TAG_DAILY_GOAL
import com.atlabs.percentpro.ui.screens.settings.TEST_TAG_LANGUAGE
import com.atlabs.percentpro.ui.screens.settings.TEST_TAG_VERSION
import com.atlabs.percentpro.ui.theme.PercentProTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * On-device journeys for the two surfaces Phase 8 added.
 *
 * Both screens take their state as parameters and hold no repository of their
 * own, so these drive the composables directly instead of launching the
 * activity. That keeps them deterministic: there is no DataStore to clear
 * between runs and no dependence on what a previous test left behind.
 *
 * Requires a device or emulator: `./gradlew connectedDebugAndroidTest`.
 */
class Phase8JourneyTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ---- onboarding --------------------------------------------------------

    @Test
    fun onboardingShowsTheFirstPage() {
        composeRule.setContent {
            PercentProTheme {
                OnboardingScreen(onFinished = {})
            }
        }

        composeRule.onNodeWithTag(TEST_TAG_ONBOARDING).assertExists()
        composeRule.onNodeWithText(composeRule.activityString(R.string.welcome_title))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_SKIP).assertExists()
    }

    @Test
    fun onboardingAdvancesThroughEveryPageAndFinishesOnTheLast() {
        var finished = false
        composeRule.setContent {
            PercentProTheme {
                OnboardingScreen(onFinished = { finished = true })
            }
        }

        val pageCount = 3
        repeat(pageCount - 1) {
            composeRule.onNodeWithTag(TEST_TAG_ADVANCE).performClick()
        }

        // On the last page the primary action becomes the finishing action.
        composeRule.onNodeWithText(composeRule.activityString(R.string.onboarding_start))
            .assertIsDisplayed()
        assertEquals(false, finished)

        composeRule.onNodeWithTag(TEST_TAG_ADVANCE).performClick()
        assertEquals(true, finished)
    }

    @Test
    fun onboardingCanBeSkippedFromTheFirstPage() {
        var finished = false
        composeRule.setContent {
            PercentProTheme {
                OnboardingScreen(onFinished = { finished = true })
            }
        }

        composeRule.onNodeWithTag(TEST_TAG_SKIP).performClick()
        assertEquals(true, finished)
    }

    @Test
    fun onboardingBackIsHiddenOnTheFirstPageAndShownAfterwards() {
        composeRule.setContent {
            PercentProTheme {
                OnboardingScreen(onFinished = {})
            }
        }

        // There is nowhere to go back to on page one.
        composeRule.onNodeWithText(composeRule.activityString(R.string.onboarding_back))
            .assertDoesNotExist()

        composeRule.onNodeWithTag(TEST_TAG_ADVANCE).performClick()

        composeRule.onNodeWithText(composeRule.activityString(R.string.onboarding_back))
            .assertExists()
    }

    @Test
    fun onboardingSkipDisappearsOnTheLastPage() {
        composeRule.setContent {
            PercentProTheme {
                OnboardingScreen(onFinished = {})
            }
        }

        repeat(2) { composeRule.onNodeWithTag(TEST_TAG_ADVANCE).performClick() }

        // Nothing to skip once you are on the final page: leaving is the action.
        composeRule.onNodeWithTag(TEST_TAG_SKIP).assertDoesNotExist()
    }

    // ---- settings ----------------------------------------------------------

    @Test
    fun settingsShowsTheVersionAndThePrivacyExplanation() {
        composeRule.setContent {
            PercentProTheme {
                SettingsScreen(
                    uiState = SettingsUiState(
                        language = AppLanguage.ENGLISH,
                        dailyGoal = RewardRules.DEFAULT_DAILY_GOAL,
                        unlockedAchievements = 3,
                        totalAchievements = 10,
                        completedLessons = 4,
                        totalLessons = 21,
                        totalXp = 240
                    ),
                    goalOptions = RewardRules.dailyGoalOptions,
                    versionName = "1.0",
                    onLanguageSelected = {},
                    onDailyGoalSelected = {},
                    onOpenAchievements = {}
                )
            }
        }

        composeRule.onNodeWithTag(TEST_TAG_VERSION).assertExists()
        composeRule.onNodeWithText(composeRule.activityString(R.string.settings_privacy_title))
            .assertExists()
        composeRule.onNodeWithTag(TEST_TAG_LANGUAGE).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_DAILY_GOAL).assertExists()
    }

    @Test
    fun settingsOffersEveryDailyGoalOption() {
        composeRule.setContent {
            PercentProTheme {
                SettingsScreen(
                    uiState = SettingsUiState.EMPTY,
                    goalOptions = RewardRules.dailyGoalOptions,
                    versionName = "1.0",
                    onLanguageSelected = {},
                    onDailyGoalSelected = {},
                    onOpenAchievements = {}
                )
            }
        }

        // Labelled as a per-day target, not as progress: the earlier wording
        // rendered "10 of 10 completed", which reads as a finished goal.
        RewardRules.dailyGoalOptions.forEach { goal ->
            composeRule
                .onNodeWithText(composeRule.activityString(R.string.gam_goal_option, goal))
                .assertExists()
        }
    }

    @Test
    fun settingsReportsTheAchievementDestination() {
        var opened = false
        composeRule.setContent {
            PercentProTheme {
                SettingsScreen(
                    uiState = SettingsUiState.EMPTY,
                    goalOptions = RewardRules.dailyGoalOptions,
                    versionName = "1.0",
                    onLanguageSelected = {},
                    onDailyGoalSelected = {},
                    onOpenAchievements = { opened = true }
                )
            }
        }

        composeRule.onNodeWithTag(TEST_TAG_ACHIEVEMENTS).performClick()
        assertEquals(true, opened)
    }

    @Test
    fun settingsReportsAChangedLanguage() {
        var selected: AppLanguage? = null
        composeRule.setContent {
            PercentProTheme {
                SettingsScreen(
                    uiState = SettingsUiState.EMPTY,
                    goalOptions = RewardRules.dailyGoalOptions,
                    versionName = "1.0",
                    onLanguageSelected = { selected = it },
                    onDailyGoalSelected = {},
                    onOpenAchievements = {}
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activityString(R.string.language_hindi))
            .performClick()

        assertEquals(AppLanguage.HINDI, selected)
    }

    @Test
    fun settingsExplainsWhyThereIsNoReset() {
        composeRule.setContent {
            PercentProTheme {
                SettingsScreen(
                    uiState = SettingsUiState.EMPTY,
                    goalOptions = RewardRules.dailyGoalOptions,
                    versionName = "1.0",
                    onLanguageSelected = {},
                    onDailyGoalSelected = {},
                    onOpenAchievements = {}
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activityString(R.string.settings_no_reset))
            .assertExists()

        // The privacy body is one multi-paragraph string. The screen must render
        // it in full rather than truncating it; the no-reset note explains the
        // absence of a destructive control rather than leaving it unexplained.
        composeRule
            .onNodeWithText(
                composeRule.activityString(R.string.settings_no_reset_body)
            )
            .assertExists()
    }
}

/** Resolves a string resource against the app under test. */
private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.activityString(
    resId: Int,
    vararg args: Any
): String {
    val context = androidx.test.platform.app.InstrumentationRegistry
        .getInstrumentation().targetContext
    return if (args.isEmpty()) context.getString(resId) else context.getString(resId, *args)
}