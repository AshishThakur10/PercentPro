package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.AnswerEvent
import com.atlabs.percentpro.model.AnswerSource
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId

/**
 * Serialises the answer ledger for DataStore.
 *
 * One line per event, fields separated by `|`:
 * `questionId|topic|difficulty|source|sessionId|answeredAt|answeredOnDay|correct`
 *
 * Decoding drops any line that does not parse rather than failing the whole
 * ledger, so one damaged entry costs a single attempt rather than the profile.
 * A line whose attempt key is already seen is dropped too, which is the second
 * guard against double counting after the repository's own check.
 */
object AnswerEventCodec {

    /**
     * Cap on stored events.
     *
     * The most recent window is 20 answers, compared against the 20 before it,
     * so keeping a few hundred gives every topic a usable history while the file
     * stays small enough to decode on each read.
     */
    const val MAX_STORED_EVENTS = 600

    private const val SEP = "|"
    private const val RECORD = "\n"
    private const val FIELD_COUNT = 8

    private val DIFFICULTIES = QuestionDifficulty.entries.associateBy { it.name }

    fun encode(events: List<AnswerEvent>): String =
        events
            .takeLast(MAX_STORED_EVENTS)
            .joinToString(RECORD) { event ->
                listOf(
                    event.questionId.value,
                    event.topicKey,
                    event.difficulty.name,
                    event.source.storageKey,
                    event.sessionId,
                    event.answeredAtEpochMillis.toString(),
                    event.answeredOn.value.toString(),
                    if (event.wasCorrect) "1" else "0"
                ).joinToString(SEP)
            }

    fun decode(raw: String?): List<AnswerEvent> {
        if (raw.isNullOrBlank()) return emptyList()
        val seen = mutableSetOf<String>()
        return raw.split(RECORD)
            .mapNotNull { decodeLine(it) }
            .filter { seen.add(it.attemptKey) }
            .sortedBy { it.answeredAtEpochMillis }
            .takeLast(MAX_STORED_EVENTS)
    }

    private fun decodeLine(line: String): AnswerEvent? {
        if (line.isBlank()) return null
        val fields = line.split(SEP)
        if (fields.size != FIELD_COUNT) return null

        val questionId = QuestionId(fields[0]).takeIf { it.value.isNotBlank() } ?: return null
        val topic = fields[1].takeIf { it.isNotBlank() } ?: return null
        val difficulty = DIFFICULTIES[fields[2]] ?: return null
        val source = AnswerSource.fromKey(fields[3]) ?: return null
        val sessionId = fields[4].takeIf { it.isNotBlank() } ?: return null
        val answeredAt = fields[5].toLongOrNull() ?: return null
        val answeredOn = fields[6].toIntOrNull() ?: return null
        val correct = when (fields[7]) {
            "1" -> true
            "0" -> false
            else -> return null
        }
        if (answeredAt < 0L) return null

        return AnswerEvent(
            questionId = questionId,
            topicKey = topic,
            difficulty = difficulty,
            wasCorrect = correct,
            source = source,
            sessionId = sessionId,
            answeredAtEpochMillis = answeredAt,
            answeredOn = LocalDateKey(answeredOn)
        )
    }
}