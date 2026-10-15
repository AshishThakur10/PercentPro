package com.atlabs.percentpro.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.atlabs.percentpro.content.OnboardingStatus
import com.atlabs.percentpro.content.OnboardingStatus.Done
import com.atlabs.percentpro.content.OnboardingStatus.Pending
import com.atlabs.percentpro.model.AppLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The onboarding flag against a real DataStore, on the JVM.
 *
 * This is the test that should have caught the shipped bug. The repository used
 * to require a `Context`, so nothing on the JVM could reach it, and the only
 * onboarding test drove the composable with its own callback — which proved the
 * buttons fired but said nothing about what the repository did with the value
 * they wrote. The result was a welcome tour that could not be dismissed.
 *
 * Each test gets its own temporary DataStore file, so there is no shared state
 * between tests and no ordering dependency.
 */
class SettingsRepositoryOnboardingTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store(name: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            folder.newFile(name).also { it.delete() }
        }

    private fun repository(name: String) = SettingsRepository(store(name))

    @Test
    fun `a store that has never been written reports onboarding as pending`() =
        runBlocking {
            val repository = repository("fresh.preferences_pb")

            assertEquals(Pending, repository.onboardingStatus.first())
        }

    @Test
    fun `finishing the onboarding tour moves the status from pending to done`() =
        runBlocking {
            // The exact user report: press Start learning or Skip, and nothing
            // should change except that the tour goes away and the app appears.
            val repository = repository("finish.preferences_pb")

            val before = repository.onboardingStatus.first()
            repository.setOnboardingSeen()
            val after = repository.onboardingStatus.first()

            assertEquals(Pending, before)
            assertEquals(Done, after)
            assertNotEquals("the tour must not still be pending", before, after)
        }

    @Test
    fun `skipping is recorded exactly like finishing`() =
        runBlocking {
            // Both routes write the same flag, so a skipped user must reach the
            // same state as one who read to the last page.
            val skipped = repository("skip.preferences_pb")
            skipped.setOnboardingSeen()

            assertEquals(Done, skipped.onboardingStatus.first())
        }

    @Test
    fun `the status survives a new repository over the same store`() =
        runBlocking {
            // Persistence across process restarts: a second launch must not show
            // the tour again to someone who already dismissed it.
            val dataStore = store("persist.preferences_pb")
            SettingsRepository(dataStore).setOnboardingSeen()

            val reopened = SettingsRepository(dataStore)
            assertEquals(Done, reopened.onboardingStatus.first())
        }

    @Test
    fun `the existing-user seed moves the status to done without displaying`() =
        runBlocking {
            // MainActivity calls this for anyone who already had progress, so
            // they never see the tour at all.
            val repository = repository("seed.preferences_pb")
            assertEquals(Pending, repository.onboardingStatus.first())

            repository.skipOnboardingForExistingUser()

            assertEquals(Done, repository.onboardingStatus.first())
        }

    @Test
    fun `the existing-user seed never re-onboards someone who finished`() =
        runBlocking {
            // The seed must only ever move the state forward. If it could reset
            // it, a returning user would be trapped in the tour on every launch.
            val repository = repository("seed-twice.preferences_pb")
            repository.skipOnboardingForExistingUser()
            repository.setOnboardingSeen()
            repository.skipOnboardingForExistingUser()

            assertEquals(Done, repository.onboardingStatus.first())
        }

    @Test
    fun `finishing the tour does not disturb the chosen language`() =
        runBlocking {
            // Both flags live in one file; writing one must not disturb the
            // other, or choosing a language would restart the tour.
            val repository = repository("language.preferences_pb")
            repository.setLanguage(AppLanguage.HINDI)

            repository.setOnboardingSeen()

            assertEquals(AppLanguage.HINDI, repository.language.first())
            assertEquals(Done, repository.onboardingStatus.first())
        }

    @Test
    fun `language defaults to the device locale until one is chosen`() =
        runBlocking {
            val repository = repository("default-language.preferences_pb")

            assertEquals(AppLanguage.forDeviceLocale(), repository.language.first())
        }

    // ---- the language choice survives a restart ----------------------------

    @Test
    fun `a chosen language survives reopening the store`() =
        runBlocking {
            // The journey a learner actually performs: switch to Hindi, kill the
            // app, open it again. A new repository over the same file stands in
            // for that restart.
            val dataStore = store("restart-language.preferences_pb")
            SettingsRepository(dataStore).setLanguage(AppLanguage.HINDI)

            val reopened = SettingsRepository(dataStore)
            assertEquals(AppLanguage.HINDI, reopened.language.first())
        }

    @Test
    fun `switching back to english also persists`() =
        runBlocking {
            val dataStore = store("switch-back.preferences_pb")
            val repository = SettingsRepository(dataStore)
            repository.setLanguage(AppLanguage.HINDI)
            repository.setLanguage(AppLanguage.ENGLISH)

            assertEquals(
                AppLanguage.ENGLISH,
                SettingsRepository(dataStore).language.first()
            )
        }

    @Test
    fun `changing the language does not replay the onboarding tour`() =
        runBlocking {
            // Language lives in the same file as the onboarding flag, so a write
            // to one must not be mistaken for activity in the other.
            val dataStore = store("language-vs-onboarding.preferences_pb")
            val repository = SettingsRepository(dataStore)

            repository.setOnboardingSeen()
            repository.setLanguage(AppLanguage.HINDI)

            val reopened = SettingsRepository(dataStore)
            assertEquals(OnboardingStatus.Done, reopened.onboardingStatus.first())
            assertEquals(AppLanguage.HINDI, reopened.language.first())
        }

    @Test
    fun `a corrupt settings file degrades instead of crashing`() = runBlocking {
        // DataStore reports a damaged file with CorruptionException, which
        // extends IOException. SettingsRepository catches it and emits empty
        // preferences.
        //
        // This matters more here than in the other stores: onboardingStatus
        // gates the entire app and is collected eagerly by AppViewModel, so an
        // exception escaping this flow would be a launch crash rather than a
        // degraded feature.
        val path = folder.newFile("corrupt.preferences_pb")
        path.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05))
        val repository = SettingsRepository(PreferenceDataStoreFactory.create { path })

        // Reaching a value at all is the assertion. Before the guard was added
        // this read propagated the failure.
        assertEquals(
            "a corrupt settings file must degrade to defaults, not throw",
            OnboardingStatus.Pending,
            repository.onboardingStatus.first()
        )
        assertEquals(
            AppLanguage.forDeviceLocale(),
            repository.language.first()
        )
    }

    @Test
    fun `a corrupt settings file does not report onboarding as done`() =
        runBlocking {
            // Degrading to defaults means "never seen", so the welcome pages are
            // shown rather than dropping the learner straight into a shell whose
            // settings were unreadable.
            val path = folder.newFile("corrupt-status.preferences_pb")
            path.writeBytes(ByteArray(32) { it.toByte() })
            val repository = SettingsRepository(PreferenceDataStoreFactory.create { path })

            assertNotEquals(
                OnboardingStatus.Done,
                repository.onboardingStatus.first()
            )
        }
}