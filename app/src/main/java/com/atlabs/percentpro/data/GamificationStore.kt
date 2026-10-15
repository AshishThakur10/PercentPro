package com.atlabs.percentpro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.gamificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "percentpro_gamification"
)

/**
 * Storage for the serialised gamification profile.
 *
 * [update] is the important one: it must apply its transform atomically against
 * the stored value, because the whole correctness argument for XP rests on an
 * award and its ledger entry never being able to disagree. A read-modify-write
 * from outside would leave a window where two callers both read the old value and
 * one overwrites the other's award.
 */
interface GamificationStore {
    val payload: Flow<String?>

    suspend fun read(): String?

    /** Atomically transforms the stored payload. Returning null clears it. */
    suspend fun update(transform: (String?) -> String?)
}

/** Production store, backed by this app's own DataStore file. */
class DataStoreGamificationStore(context: Context) : GamificationStore {

    private val dataStore = context.applicationContext.gamificationDataStore

    private val safeData: Flow<Preferences> = dataStore.data
        // A corrupt or unreadable file must not take Home or Achievements down.
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }

    override val payload: Flow<String?> = safeData.map { it[STATE_KEY] }

    override suspend fun read(): String? = safeData.first()[STATE_KEY]

    override suspend fun update(transform: (String?) -> String?) {
        dataStore.edit { preferences ->
            val updated = transform(preferences[STATE_KEY])
            if (updated == null) preferences.remove(STATE_KEY) else preferences[STATE_KEY] = updated
        }
    }

    private companion object {
        val STATE_KEY = stringPreferencesKey("gamification_state")
    }
}

/** Whether the one-time baseline has been written. Separate from the profile. */
interface BaselineMarkerStore {
    val seeded: Flow<Boolean>

    suspend fun isSeeded(): Boolean

    suspend fun markSeeded()

    suspend fun clear()
}

class DataStoreBaselineMarker(context: Context) : BaselineMarkerStore {

    private val dataStore = context.applicationContext.gamificationDataStore

    private val safeData: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }

    override val seeded: Flow<Boolean> = safeData.map { it[SEEDED_KEY] == true }

    override suspend fun isSeeded(): Boolean = safeData.first()[SEEDED_KEY] == true

    override suspend fun markSeeded() {
        dataStore.edit { preferences -> preferences[SEEDED_KEY] = true }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(SEEDED_KEY) }
    }

    private companion object {
        val SEEDED_KEY = booleanPreferencesKey("pre_existing_seeded")
    }
}