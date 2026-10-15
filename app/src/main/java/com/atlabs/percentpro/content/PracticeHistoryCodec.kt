package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.PracticeResult

/**
 * Serialises practice history for DataStore.
 *
 * Format is one line per session, fields separated by `|`:
 * `topic|epochMillis|total|correct|incorrect|unanswered|accuracy`
 *
 * Decoding is deliberately forgiving. Saved data survives app updates, so a
 * malformed line, an unknown topic or a nonsensical count is skipped instead of
 * failing the whole history or crashing the screen.
 */
object PracticeHistoryCodec {

    const val MAX_STORED_RESULTS = 50

    private const val FIELD_SEPARATOR = "|"
    private const val RECORD_SEPARATOR = "\n"
    private const val FIELD_COUNT = 7

    fun encode(results: List<PracticeResult>): String =
        results
            .filter { it.isValid }
            .takeLast(MAX_STORED_RESULTS)
            .joinToString(RECORD_SEPARATOR) { result ->
                listOf(
                    result.topic.storageKey,
                    result.completedAtEpochMillis.toString(),
                    result.total.toString(),
                    result.correct.toString(),
                    result.incorrect.toString(),
                    result.unanswered.toString(),
                    result.accuracyPercent.toString()
                ).joinToString(FIELD_SEPARATOR)
            }

    fun decode(raw: String?): List<PracticeResult> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(RECORD_SEPARATOR)
            .mapNotNull(::decodeLine)
            .sortedBy { it.completedAtEpochMillis }
            .takeLast(MAX_STORED_RESULTS)
            .toList()
    }

    /** Newest result per topic, used by the topic selection screen. */
    fun latestByTopic(results: List<PracticeResult>): Map<LessonId, PracticeResult> =
        results
            .sortedBy { it.completedAtEpochMillis }
            .associateBy { it.topic }

    private fun decodeLine(line: String): PracticeResult? {
        if (line.isBlank()) return null
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size != FIELD_COUNT) return null

        val topic = LessonId.fromStorageKey(fields[0]) ?: return null
        val epoch = fields[1].toLongOrNull() ?: return null
        val total = fields[2].toIntOrNull() ?: return null
        val correct = fields[3].toIntOrNull() ?: return null
        val incorrect = fields[4].toIntOrNull() ?: return null
        val unanswered = fields[5].toIntOrNull() ?: return null
        val accuracy = fields[6].toIntOrNull() ?: return null

        val candidate = PracticeResult(
            topic = topic,
            completedAtEpochMillis = epoch,
            total = total,
            correct = correct,
            incorrect = incorrect,
            unanswered = unanswered,
            accuracyPercent = accuracy
        )
        // A record whose counts do not add up, or whose accuracy was computed
        // from something else, is discarded rather than shown.
        return candidate.takeIf { it.isValid }
    }
}