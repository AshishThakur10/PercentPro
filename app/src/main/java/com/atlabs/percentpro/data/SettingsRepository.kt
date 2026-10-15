package com.atlabs.percentpro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.atlabs.percentpro.content.OnboardingStatus
import com.atlabs.percentpro.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "percentpro_settings"
)

/**
 * Persists user settings. The chosen language lives here, as does the one-time
 * onboarding flag added in Phase 8. DataStore is used rather than
 * SharedPreferences because it is the supported home for preference data: writes
 * are transactional and suspend instead of blocking the main thread.
 *
 * Takes its [DataStore] directly rather than building one from a [Context], with
 * [Context] offered as the convenience constructor the app actually uses. The
 * reason is testability: the onboarding flag is the one piece of startup logic
 * that decides whether a user sees the welcome tour or the app, and a repository
 * that insists on a Context cannot be covered by a plain JVM test. The original
 * bug lived exactly in this file for exactly that reason.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    /**
     * A corrupt or unreadable file must not stop the app launching.
     *
     * This store gates the whole app: `onboardingStatus` decides whether the
     * welcome pages or the Home screen is drawn, and it is collected eagerly by
     * AppViewModel. An exception escaping this flow would therefore be a launch
     * crash, not a degraded feature. DataStore signals a damaged file with
     * CorruptionException, which extends IOException, so this covers both a
     * truncated file and an unreadable one; anything else is a programming
     * error and is deliberately allowed through.
     *
     * The other four repositories already do this. This one and
     * ProgressRepository were the two that did not.
     */
    private val safeData: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }

    val language: Flow<AppLanguage> = safeData.map { preferences ->
        AppLanguage.fromTag(preferences[LANGUAGE_KEY]) ?: AppLanguage.forDeviceLocale()
    }

/**
 * Whether the welcome pages belong on screen right now.
 *
 * The stored value is interpreted by [OnboardingStatus.fromStored] rather than
 * passed on raw. That mapping is the only place storage becomes a screen
 * decision: an absent key means "never seen" and becomes [OnboardingStatus.Pending],
 * and a stored flag means [OnboardingStatus.Done].
 *
 * Note what this does *not* return. There is no "not read yet" value here,
 * because DataStore's first emission already reflects what is on disk.
 * [OnboardingStatus.Loading] belongs to the ViewModel as the value before that
 * first emission arrives.
 */
val onboardingStatus: Flow<OnboardingStatus> = safeData.map { preferences ->
    OnboardingStatus.fromStored(preferences[ONBOARDING_SEEN_KEY])
}

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences -> preferences[LANGUAGE_KEY] = language.tag }
    }

    /**
     * Records that onboarding has been dealt with, whether it was read to the
     * end or skipped. Idempotent, so a double tap cannot cause harm.
     */
    suspend fun setOnboardingSeen() {
        dataStore.edit { preferences -> preferences[ONBOARDING_SEEN_KEY] = true }
    }

    /**
     * Marks onboarding as seen for someone who was already using the app before
     * it existed.
     *
     * This runs once at launch against real data: a user who already has a
     * completed lesson or a saved result is never shown a welcome tour they did
     * not need. It only ever writes `true`, so it can never re-onboard anybody
     * who has deliberately skipped.
     */
    suspend fun skipOnboardingForExistingUser() {
        dataStore.edit { preferences ->
            if (preferences[ONBOARDING_SEEN_KEY] == null) {
                preferences[ONBOARDING_SEEN_KEY] = true
            }
        }
    }

    private companion object {
        val LANGUAGE_KEY = stringPreferencesKey("app_language")
        val ONBOARDING_SEEN_KEY = booleanPreferencesKey("onboarding_seen")
    }
}
