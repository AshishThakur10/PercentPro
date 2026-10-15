package com.atlabs.percentpro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.atlabs.percentpro.content.LearningProgress
import com.atlabs.percentpro.content.ProgressCodec
import com.atlabs.percentpro.model.LessonId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "percentpro_progress"
)

/**
 * Persists what the learner has finished and where they were.
 *
 * A lesson only enters the completed set through an explicit confirmation on the
 * final content block, never by opening a lesson, so the saved set is always a
 * genuine record of finished work.
 */
class ProgressRepository(
    context: Context,
    private val playableLessonIds: Set<LessonId>,
    private val blockCountFor: (LessonId) -> Int
) {

    private val dataStore = context.applicationContext.progressDataStore

    /**
     * A corrupt or unreadable file must not take the learning path down.
     *
     * Lesson progress is the one store whose loss is most visible: without it a
     * learner's completed lessons appear to vanish. The other repositories
     * already degrade to defaults on IOException rather than propagating, and
     * this one was the second of the two that did not. DataStore reports a
     * damaged file with CorruptionException, which extends IOException.
     */
    private val safeData: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }

    val progress: Flow<LearningProgress> = safeData.map { preferences ->
        decode(preferences)
    }

    suspend fun current(): LearningProgress = progress.first()

    suspend fun markLessonCompleted(lessonId: LessonId) {
        dataStore.edit { preferences ->
            val updated = decode(preferences).markCompleted(lessonId)
            preferences[VERSION] = ProgressCodec.CURRENT_VERSION
            preferences[COMPLETED_LESSON_IDS] =
                updated.completedLessonIds.map { it.storageKey }.toSet()
            writeActiveLesson(preferences, updated)
        }
    }

    /**
     * Records that [lessonId] is the lesson being worked on and that the learner
     * has reached [blockIndex].
     */
    suspend fun saveLessonPosition(lessonId: LessonId, blockIndex: Int) {
        dataStore.edit { preferences ->
            val safeIndex = blockIndex.coerceIn(0, (blockCountFor(lessonId) - 1).coerceAtLeast(0))
            val updated = decode(preferences).copy(
                lessonInProgress = lessonId,
                lastBlockIndex = safeIndex
            )
            preferences[VERSION] = ProgressCodec.CURRENT_VERSION
            writeActiveLesson(preferences, updated)
        }
    }

    suspend fun reset() {
        dataStore.edit { preferences ->
            preferences.remove(VERSION)
            preferences.remove(COMPLETED_LESSON_IDS)
            preferences.remove(LESSON_IN_PROGRESS)
            preferences.remove(LAST_BLOCK_INDEX)
        }
    }

    private fun decode(preferences: Preferences): LearningProgress {
        val activeId = LessonId.fromStorageKey(preferences[LESSON_IN_PROGRESS])
        return ProgressCodec.decode(
            version = preferences[VERSION],
            completedLessonIds = preferences[COMPLETED_LESSON_IDS],
            lessonInProgress = preferences[LESSON_IN_PROGRESS],
            lastBlockIndex = preferences[LAST_BLOCK_INDEX],
            playableLessons = playableLessonIds,
            blockCount = activeId?.let(blockCountFor) ?: 0
        )
    }

    private fun writeActiveLesson(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        progress: LearningProgress
    ) {
        val active = progress.lessonInProgress
        if (active == null) {
            preferences.remove(LESSON_IN_PROGRESS)
            preferences.remove(LAST_BLOCK_INDEX)
        } else {
            preferences[LESSON_IN_PROGRESS] = active.storageKey
            preferences[LAST_BLOCK_INDEX] = progress.lastBlockIndex
        }
    }

    private companion object {
        val VERSION = intPreferencesKey("progress_version")
        val COMPLETED_LESSON_IDS = stringSetPreferencesKey("completed_lesson_ids")
        val LESSON_IN_PROGRESS = stringPreferencesKey("lesson_in_progress")
        val LAST_BLOCK_INDEX = intPreferencesKey("last_block_index")
    }
}