package com.atlabs.percentpro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.atlabs.percentpro.content.MockTestCatalog
import com.atlabs.percentpro.content.MockTestCodec
import com.atlabs.percentpro.content.MockTestSession
import com.atlabs.percentpro.content.QuestionRepository
import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.model.MockTestResult
import com.atlabs.percentpro.model.QuestionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.mockTestDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "percentpro_mocktest"
)

/**
 * Stores the one in-progress mock test and every finished mock-test result.
 *
 * Its own DataStore file, separate from lesson progress and from practice
 * history, so a mock test can never mark a lesson complete or overwrite a
 * practice score.
 *
 * Only question ids, option indices, counts and timestamps are written. There is
 * no free text, no personal data and no network access.
 */
class MockTestRepository(
    context: Context,
    private val questionRepository: QuestionRepository,
    private val catalog: MockTestCatalog
) {

    private val dataStore = context.applicationContext.mockTestDataStore

    private val safeData: Flow<Preferences> = dataStore.data
        // A corrupt or unreadable file must not take the Practice tab down.
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }

    val results: Flow<List<MockTestResult>> =
        safeData.map { preferences -> MockTestCodec.decodeResults(preferences[RESULTS_KEY]) }

    suspend fun currentResults(): List<MockTestResult> = results.first()

    suspend fun resultFor(sessionId: String): MockTestResult? =
        currentResults().firstOrNull { it.sessionId == sessionId }

    /**
     * Rebuilds the in-progress paper, if one can be rebuilt safely.
     *
     * Returns [MockTestCodec.SessionRestore.Unusable] rather than a half-filled
     * session when anything is wrong, so the caller can offer a clean restart
     * instead of showing a score based on missing answers.
     */
    suspend fun restoreActiveSession(nowEpochMillis: Long): MockTestCodec.SessionRestore {
        val raw = safeData.first()[ACTIVE_SESSION_KEY]
        return MockTestCodec.decodeSession(
            raw = raw,
            catalog = catalog,
            questionsById = { id: QuestionId -> questionRepository.findAcrossTopics(id) },
            nowEpochMillis = nowEpochMillis
        )
    }

    /** Reads the raw stored session without validating it, for expiry recovery. */
    suspend fun rawActiveSession(): String? = safeData.first()[ACTIVE_SESSION_KEY]

    suspend fun saveActiveSession(session: MockTestSession) {
        if (session.isSubmitted) return
        dataStore.edit { preferences ->
            preferences[ACTIVE_SESSION_KEY] = MockTestCodec.encodeSession(session)
        }
    }

    suspend fun clearActiveSession() {
        dataStore.edit { preferences -> preferences.remove(ACTIVE_SESSION_KEY) }
    }

    /** Records a finished paper. Repeating the same session id is a no-op. */
    suspend fun recordResult(result: MockTestResult) {
        dataStore.edit { preferences ->
            val existing = MockTestCodec.decodeResults(preferences[RESULTS_KEY])
            if (existing.any { it.sessionId == result.sessionId }) return@edit
            preferences[RESULTS_KEY] = MockTestCodec.encodeResults(
                (existing + result).sortedBy { it.completedAtEpochMillis }
            )
            preferences.remove(ACTIVE_SESSION_KEY)
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.remove(ACTIVE_SESSION_KEY)
            preferences.remove(RESULTS_KEY)
        }
    }

    private companion object {
        val ACTIVE_SESSION_KEY = stringPreferencesKey("mock_active_session")
        val RESULTS_KEY = stringPreferencesKey("mock_results")
    }
}