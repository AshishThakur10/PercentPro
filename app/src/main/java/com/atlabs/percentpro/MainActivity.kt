package com.atlabs.percentpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.ui.theme.PercentProTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Runs once and is then skipped by its own stored flag. Existing
        // completed lessons and finished mock tests are entered into the reward
        // ledger at zero XP so that repeating them cannot pay out later.
        lifecycleScope.launch {
            // Recovers mock-test answers saved before the ledger existed. Saved
            // practice history holds only counts, so its answers are not guessed
            // at; see AnswerLedgerRepository.backfillFromMockTests.
            runCatching { appContainer.seedAnswerLedgerFromMockTests() }
        }

        lifecycleScope.launch {
            runCatching {
                val preExisting = appContainer.preExistingGamificationActivity()
                appContainer.gamificationRepository.seedPreExistingActivity(
                    completedLessonKeys = preExisting.completedLessonKeys,
                    completedMockTestIds = preExisting.completedMockTestIds
                )
            }
        }

        // Phase 8: somebody who already has progress, answers or results was
        // using the app before onboarding existed, so the welcome pages are
        // marked as seen rather than replayed at them. Only ever writes when the
        // flag is unset, so it cannot override a deliberate skip.
        lifecycleScope.launch {
            runCatching {
                if (appContainer.hasPriorActivity()) {
                    appContainer.settingsRepository.skipOnboardingForExistingUser()
                }
            }
        }

        setContent {
            PercentProTheme {
                PercentProApp(container = appContainer)
            }
        }
    }
}