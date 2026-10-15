package com.atlabs.percentpro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.atlabs.percentpro.content.PracticeHistoryCodec
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.PracticeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.practiceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "percentpro_practice"
)

/**
 * Stores finished practice sessions.
 *
 * Kept in its own DataStore file and its own repository so it can never be
 * confused with lesson completion in [ProgressRepository]: practising a topic
 * must not mark a lesson complete.
 *
 * Only the topic, a timestamp and four counts are written. No free text, no
 * personal data, no network.
 */
class PracticeHistoryRepository(context: Context) {

    private val dataStore = context.applicationContext.practiceDataStore

    private val results: Flow<List<PracticeResult>> = dataStore.data
        // A corrupt or unreadable file must not take the Practice tab down.
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> PracticeHistoryCodec.decode(preferences[RESULTS_KEY]) }

    val history: Flow<List<PracticeResult>> = results

    val latestByTopic: Flow<Map<LessonId, PracticeResult>> =
        results.map(PracticeHistoryCodec::latestByTopic)

    suspend fun currentHistory(): List<PracticeResult> = results.first()

    suspend fun latestFor(topic: LessonId): PracticeResult? = latestByTopic.first()[topic]

    suspend fun record(result: PracticeResult) {
        if (!result.isValid) return
        dataStore.edit { preferences ->
            val existing = PracticeHistoryCodec.decode(preferences[RESULTS_KEY])
            preferences[RESULTS_KEY] = PracticeHistoryCodec.encode(existing + result)
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(RESULTS_KEY) }
    }

    private companion object {
        val RESULTS_KEY = stringPreferencesKey("practice_results")
    }
}