package com.atlabs.percentpro.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.data.GamificationRepository
import com.atlabs.percentpro.model.RewardRules
import com.atlabs.percentpro.model.AchievementStatus
import com.atlabs.percentpro.ui.gamification.GamificationSnapshot
import com.atlabs.percentpro.ui.gamification.gamificationSnapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val snapshot: GamificationSnapshot = GamificationSnapshot()
)

/**
 * Reads the gamification profile for the achievements screen.
 *
 * Holds no reward logic: it observes persisted state and lets the engine decide
 * what is unlocked. Changing the daily goal here goes through the repository, so
 * today's progress survives.
 */
class AchievementsViewModel(
    private val gamificationRepository: GamificationRepository,
    now: () -> Long = System::currentTimeMillis
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> = gamificationSnapshots(
        repository = gamificationRepository,
        now = now
    )
        .mapToAchievementsUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AchievementsUiState()
        )

    /** Options offered by the goal picker. */
    val goalOptions: List<Int> = RewardRules.dailyGoalOptions

    fun setDailyGoal(questions: Int) {
        viewModelScope.launch { gamificationRepository.setDailyGoal(questions) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { AchievementsViewModel(container.gamificationRepository) }
        }
    }
}

private fun Flow<GamificationSnapshot>.mapToAchievementsUiState(): Flow<AchievementsUiState> =
    map { snapshot -> AchievementsUiState(isLoading = false, snapshot = snapshot) }