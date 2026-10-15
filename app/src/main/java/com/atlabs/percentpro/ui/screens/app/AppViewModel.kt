package com.atlabs.percentpro.ui.screens.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlabs.percentpro.content.OnboardingStatus
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.data.SettingsRepository
import com.atlabs.percentpro.model.AppLanguage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide state that is not tied to a single screen. The language lives here
 * because it is read by the root composable to re-resolve resources for the
 * whole tree, not just by the home screen. So does the one-time onboarding flag,
 * because it gates the whole tree rather than one destination.
 */
class AppViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val language: StateFlow<AppLanguage> = settingsRepository.language
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppLanguage.forDeviceLocale()
        )

    /**
     * Whether the welcome pages belong on screen, as [OnboardingStatus.Loading]
     * until the stored flag has actually been read from DataStore.
     *
     * `Loading` is only ever the pre-read value; the repository cannot produce
     * it. That is what stops the loading sentinel from colliding with a real
     * stored value, which is how an absent flag used to be mistaken for an
     * unread store.
     */
    val onboardingStatus: StateFlow<OnboardingStatus> =
        settingsRepository.onboardingStatus
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = OnboardingStatus.Loading
            )

    fun setLanguage(language: AppLanguage) {
        if (this.language.value == language) return
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    /**
     * Called when onboarding is left, whether it was read to the end or skipped.
     * Both paths write the same flag, so the two routes can never disagree about
     * what counts as "seen".
     */
    fun completeOnboarding() {
        // Only skip the write once onboarding is genuinely finished. The guard
        // used to test for the opposite, so it bailed out in the one case where
        // there was nothing to do and proceeded in every case where there was.
        if (onboardingStatus.value == OnboardingStatus.Done) return
        viewModelScope.launch { settingsRepository.setOnboardingSeen() }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { AppViewModel(container.settingsRepository) }
        }
    }
}