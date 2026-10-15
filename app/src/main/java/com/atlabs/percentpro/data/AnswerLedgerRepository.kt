package com.atlabs.percentpro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.atlabs.percentpro.content.AnswerEventCodec
import com.atlabs.percentpro.model.MockTestResult
import com.atlabs.percentpro.content.QuestionRepository
import com.atlabs.percentpro.model.AnswerEvent
import com.atlabs.percentpro.model.AnswerSource
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.LocalDates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.TimeZone

private val Context.answerLedgerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "percentpro_answer_ledger"
)

/**
 * The canonical record of which question the learner answered, and how.
 *
 * Why this exists: Phase 3 practice history stores only per-session counts, so
 * it cannot answer "how am I doing on *this* question" or "which topic is weak".
 * Mock-test results do carry per-question records, so their answers are
 * recoverable. Rather than inferring practice answers that were never saved, this
 * ledger records each submitted answer as it happens, and every metric in the
 * insights screen is derived from it.
 *
 * Idempotency: an event is keyed by source, session and question id. Recording
 * the same answer twice writes nothing, so a recomposition, a restored session or
 * a repeated callback cannot inflate a count. Writes go through a single atomic
 * [GamificationStore]-style update so the read, the check and the append cannot
 * interleave.
 */
class AnswerLedgerRepository(
    private val store: GamificationStore,
    private val marker: BaselineMarkerStore,
    private val questionRepository: QuestionRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val timeZone: () -> TimeZone = TimeZone::getDefault
) {

    val events: Flow<List<AnswerEvent>> = store.payload.map { AnswerEventCodec.decode(it) }

    suspend fun currentEvents(): List<AnswerEvent> = AnswerEventCodec.decode(store.read())

    /**
     * Records one submitted answer.
     *
     * Returns false when the answer was already stored, which is the signal that
     * a duplicate callback was ignored rather than counted.
     */
    suspend fun record(event: AnswerEvent): Boolean {
        var stored = false
        store.update { raw ->
            val existing = AnswerEventCodec.decode(raw)
            if (existing.any { it.attemptKey == event.attemptKey }) {
                raw
            } else {
                stored = true
                AnswerEventCodec.encode(existing + event)
            }
        }
        return stored
    }

    /** Convenience that stamps the clock and the local day. */
    suspend fun recordAnswer(
        questionId: com.atlabs.percentpro.model.QuestionId,
        topic: LessonId,
        source: AnswerSource,
        sessionId: String,
        wasCorrect: Boolean
    ): Boolean {
        val question = questionRepository.findAcrossTopics(questionId) ?: return false
        return record(
            AnswerEvent(
                questionId = questionId,
                topicKey = topic.storageKey,
                difficulty = question.difficulty,
                wasCorrect = wasCorrect,
                source = source,
                sessionId = sessionId,
                answeredAtEpochMillis = now(),
                answeredOn = LocalDates.fromEpochMillis(now(), timeZone())
            )
        )
    }

    /**
     * Rebuilds mock-test answers from results saved before this phase.
     *
     * Only mock-test history is recovered, because only it recorded which option
     * was chosen per question. Practice history keeps its existing counts-only
     * shape and is deliberately not guessed at: inventing per-question records
     * would put fabricated data into every accuracy figure on the screen.
     *
     * Runs once, guarded by the marker.
     */
    suspend fun backfillFromMockTests(results: List<MockTestResult>) {
        if (marker.isSeeded()) return

        val recovered = buildList {
            results.forEach { result ->
                result.records.forEach { record ->
                    val selected = record.selectedOptionIndex ?: return@forEach
                    val question = questionRepository.findAcrossTopics(record.questionId)
                        ?: return@forEach
                    add(
                        AnswerEvent(
                            questionId = record.questionId,
                            topicKey = question.topic.storageKey,
                            difficulty = question.difficulty,
                            wasCorrect = question.isCorrectAnswer(selected),
                            source = AnswerSource.MOCK_TEST,
                            sessionId = result.sessionId,
                            // The answer was given at submission time, which the
                            // result stores. Records carry no per-question clock.
                            answeredAtEpochMillis = result.completedAtEpochMillis,
                            answeredOn = LocalDates.fromEpochMillis(
                                result.completedAtEpochMillis,
                                timeZone()
                            )
                        )
                    )
                }
            }
        }

        if (recovered.isNotEmpty()) {
            store.update { raw ->
                // Insert order is existing-then-recovered, so an answer already
                // captured live is never replaced by a backfilled copy of itself.
                // LinkedHashMap.putIfAbsent is API 24 and this app supports 23,
                // so membership is checked directly.
                val merged = LinkedHashMap<String, AnswerEvent>()
                (AnswerEventCodec.decode(raw) + recovered).forEach { event ->
                    if (!merged.containsKey(event.attemptKey)) {
                        merged[event.attemptKey] = event
                    }
                }
                AnswerEventCodec.encode(merged.values.sortedBy { it.answeredAtEpochMillis })
            }
        }
        marker.markSeeded()
    }

    /** Clears the ledger. Used by tests and by a future reset control. */
    suspend fun clearAll() {
        store.update { null }
        marker.clear()
    }
}

/** Production wiring for the answer ledger. */
class DataStoreAnswerLedgerStore(context: Context) : GamificationStore {

    private val dataStore = context.applicationContext.answerLedgerDataStore

    private val safeData: Flow<Preferences> = dataStore.data
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
        val STATE_KEY = stringPreferencesKey("answer_events")
    }
}

class DataStoreAnswerLedgerMarker(context: Context) : BaselineMarkerStore {

    private val dataStore = context.applicationContext.answerLedgerDataStore

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
        val SEEDED_KEY = booleanPreferencesKey("answer_ledger_backfilled")
    }
}
