package com.atlabs.percentpro.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlabs.percentpro.content.GamificationState
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.data.GamificationRepository
import com.atlabs.percentpro.data.ProgressRepository
import com.atlabs.percentpro.data.SettingsRepository
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.AchievementStatus
import com.atlabs.percentpro.model.RewardRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the settings screen displays, resolved from storage.
 *
 * Kept as one immutable snapshot so the screen has no data sources of its own
 * and no "loading" branch to get wrong: the values are always either present or
 * explicitly zero, never absent.
 */
data class SettingsUiState(
    val language: AppLanguage,
    val dailyGoal: Int,
    val unlockedAchievements: Int,
    val totalAchievements: Int,
    val completedLessons: Int,
    val totalLessons: Int,
    val totalXp: Int
) {
    val isFirstLanguage: Boolean get() = language == AppLanguage.ENGLISH
    val isFirstGoal: Boolean get() = dailyGoal == RewardRules.dailyGoalOptions.firstOrNull()

    companion object {
        val EMPTY = SettingsUiState(
            language = AppLanguage.forDeviceLocale(),
            dailyGoal = RewardRules.DEFAULT_DAILY_GOAL,
            unlockedAchievements = 0,
            totalAchievements = 0,
            completedLessons = 0,
            totalLessons = 0,
            totalXp = 0
        )
    }
}

/**
 * Maps stored state onto the snapshot the screen renders.
 *
 * Separated from the ViewModel so the mapping can be tested with real domain
 * objects and no Context, DataStore or device. Everything it needs is an
 * immutable value type.
 */
internal fun buildSettingsUiState(
    language: AppLanguage,
    gamification: GamificationState,
    achievements: List<AchievementStatus>,
    completedLessonIds: Set<String>,
    totalLessons: Int
): SettingsUiState = SettingsUiState(
    language = language,
    dailyGoal = gamification.dailyQuestionGoal,
    unlockedAchievements = achievements.count(AchievementStatus::isUnlocked),
    totalAchievements = achievements.size,
    // The catalogue is the denominator even when it grows past the stored
    // progress, so "4 of 21" never reads as "4 of 4" on an updated course.
    completedLessons = completedLessonIds.size,
    totalLessons = totalLessons,
    totalXp = gamification.totalXp
)

/**
 * Backs the settings screen.
 *
 * Read-only apart from two deliberate changes: the language and the daily goal.
 * Both go through the same repository methods the rest of the app uses, so a
 * goal set here and a goal set on the achievements screen are the same stored
 * value rather than two competing copies.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val gamificationRepository: GamificationRepository,
    progressRepository: ProgressRepository,
    totalLessons: Int
) : ViewModel() {

    val goalOptions: List<Int> = RewardRules.dailyGoalOptions

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.language,
        gamificationRepository.state,
        gamificationRepository.achievements,
        progressRepository.progress
    ) { language, gamification, achievements, progress ->
        buildSettingsUiState(
            language = language,
            gamification = gamification,
            achievements = achievements,
            completedLessonIds = progress.completedLessonIds.map { it.storageKey }.toSet(),
            totalLessons = totalLessons
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState.EMPTY
    )

    fun setLanguage(language: AppLanguage) {
        if (uiState.value.language == language) return
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun setDailyGoal(questions: Int) {
        if (uiState.value.dailyGoal == questions) return
        viewModelScope.launch { gamificationRepository.setDailyGoal(questions) }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    gamificationRepository = container.gamificationRepository,
                    progressRepository = container.progressRepository,
                    totalLessons = container.lessonCatalog.lessons().size
                )
            }
        }
    }
}