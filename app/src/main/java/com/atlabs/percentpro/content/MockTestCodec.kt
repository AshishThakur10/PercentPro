package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.model.MockTestQuestionRecord
import com.atlabs.percentpro.model.MockTestResult
import com.atlabs.percentpro.model.MockTestSubmission
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionId

/**
 * Serialises an in-progress mock test and its finished results.
 *
 * Fields are `|` separated. Answers are written as `questionId=optionIndex`
 * pairs; ids cannot contain `|`, `=` or `,` because they are authored strings
 * validated when the bank is written.
 *
 * Decoding is deliberately unforgiving about the *shape* of a record and
 * forgiving about its contents: a session that cannot be rebuilt is reported as
 * unusable rather than half-restored, because a mock test with missing answers
 * would produce a misleading score.
 */
object MockTestCodec {

    private const val SEP = "|"
    private const val PAIR = ","
    private const val KV = "="
    private const val VERSION_PREFIX = "v1"

    /** Why a stored session could not be used. */
    enum class RestoreFailure {
        /** Nothing was stored. */
        NONE,

        /** Stored bytes were present but could not be parsed. */
        CORRUPT,

        /** The test id is no longer part of the app. */
        UNKNOWN_TEST,

        /** Questions in the stored order are missing from the bank. */
        MISSING_QUESTIONS,

        /** The stored deadline has already passed. */
        EXPIRED
    }

    sealed interface SessionRestore {
        data object None : SessionRestore
        data class Ready(val session: MockTestSession) : SessionRestore
        data class Unusable(val reason: RestoreFailure) : SessionRestore
    }

    // ---- active session ----------------------------------------------------

    fun encodeSession(session: MockTestSession): String = listOf(
        VERSION_PREFIX,
        session.sessionId,
        session.config.id.value,
        session.startedAtEpochMillis.toString(),
        session.deadlineEpochMillis.toString(),
        session.currentIndex.toString(),
        session.questions.joinToString(PAIR) { it.id.value },
        session.answers.entries.joinToString(PAIR) { "${it.key.value}${KV}${it.value}" },
        session.markedForReview.joinToString(PAIR) { it.value },
        session.visitedQuestionIds.joinToString(PAIR) { it.value },
        (session.submission?.name ?: "").ifEmpty { "NONE" }
    ).joinToString(SEP)

    fun decodeSession(
        raw: String?,
        catalog: MockTestCatalog,
        questionsById: (QuestionId) -> Question?,
        nowEpochMillis: Long,
        allowExpired: Boolean = false
    ): SessionRestore {
        if (raw.isNullOrBlank()) return SessionRestore.None

        val fields = raw.split(SEP)
        if (fields.size != 11 || fields[0] != VERSION_PREFIX) {
            return SessionRestore.Unusable(RestoreFailure.CORRUPT)
        }

        val sessionId = fields[1]
        if (sessionId.isBlank()) return SessionRestore.Unusable(RestoreFailure.CORRUPT)

        val testId = MockTestId(fields[2])
        val config = catalog.testById(testId)
            ?: return SessionRestore.Unusable(RestoreFailure.UNKNOWN_TEST)

        val startedAt = fields[3].toLongOrNull()
            ?: return SessionRestore.Unusable(RestoreFailure.CORRUPT)
        val deadline = fields[4].toLongOrNull()
            ?: return SessionRestore.Unusable(RestoreFailure.CORRUPT)
        val currentIndex = fields[5].toIntOrNull()
            ?: return SessionRestore.Unusable(RestoreFailure.CORRUPT)

        val questionIds = fields[6].splitPairs()
            .map { QuestionId(it) }
        if (questionIds.isEmpty()) return SessionRestore.Unusable(RestoreFailure.CORRUPT)
        if (questionIds.distinct().size != questionIds.size) {
            return SessionRestore.Unusable(RestoreFailure.CORRUPT)
        }

        // Every question must still exist and still have the same number of
        // options, otherwise a stored response index could point at nothing.
        val questions = questionIds.map { id ->
            val question = questionsById(id)
                ?: return SessionRestore.Unusable(RestoreFailure.MISSING_QUESTIONS)
            if (question.options.size != com.atlabs.percentpro.model.OPTION_COUNT) {
                return SessionRestore.Unusable(RestoreFailure.MISSING_QUESTIONS)
            }
            question
        }

        val answers = mutableMapOf<QuestionId, Int>()
        for (pair in fields[7].splitPairs()) {
            val (key, value) = pair.splitAt(KV) ?: return SessionRestore.Unusable(RestoreFailure.CORRUPT)
            val id = QuestionId(key)
            val optionIndex = value.toIntOrNull()
                ?: return SessionRestore.Unusable(RestoreFailure.CORRUPT)
            val question = questions.firstOrNull { it.id == id }
                ?: return SessionRestore.Unusable(RestoreFailure.MISSING_QUESTIONS)
            if (optionIndex !in question.options.indices) {
                return SessionRestore.Unusable(RestoreFailure.CORRUPT)
            }
            answers[id] = optionIndex
        }

        val questionIdSet = questionIds.toSet()
        val marked = fields[8].splitPairs()
            .filter { it.isNotBlank() }
            .map { QuestionId(it) }
            .filter { it in questionIdSet }
            .toSet()

        val visited = fields[9].splitPairs()
            .filter { it.isNotBlank() }
            .map { QuestionId(it) }
            .filter { it in questionIdSet }
            .toSet()

        val submission = when (fields[10]) {
            "MANUAL" -> MockTestSubmission.MANUAL
            "TIMEOUT" -> MockTestSubmission.TIMEOUT
            else -> null
        }

        // A session whose deadline has passed is not restorable as a live paper.
        // The recovery path passes allowExpired so it can still be scored, but it
        // is never handed back as something the learner can keep writing on.
        if (!allowExpired && deadline - nowEpochMillis <= 0L) {
            return SessionRestore.Unusable(RestoreFailure.EXPIRED)
        }

        val session = MockTestSession(
            sessionId = sessionId,
            config = config,
            questions = questions,
            startedAtEpochMillis = startedAt,
            deadlineEpochMillis = deadline,
            currentIndex = currentIndex.coerceIn(0, questions.lastIndex),
            answers = answers,
            markedForReview = marked,
            visitedQuestionIds = visited,
            submission = submission
        )
        return SessionRestore.Ready(session)
    }

    // ---- finished results --------------------------------------------------

    fun encodeResults(results: List<MockTestResult>): String =
        results.joinToString("\n") { encodeResult(it) }

    fun decodeResults(raw: String?): List<MockTestResult> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("\n")
            .mapNotNull(::decodeResult)
            .sortedByDescending { it.completedAtEpochMillis }
            .take(MAX_STORED_RESULTS)
    }

    fun decodeResult(line: String): MockTestResult? {
        if (line.isBlank()) return null
        val fields = line.split(SEP)
        // encodeResult writes 18 fields: version, sessionId, testId, titleRes,
        // startedAt, completedAt, records, total, correct, incorrect, unanswered,
        // marked, score, maxMarks, marksPerCorrect, penalty, timeTaken, submission.
        if (fields.size != 18 || fields[0] != VERSION_PREFIX) return null

        fun intAt(index: Int): Int? = fields[index].toIntOrNull()
        fun longAt(index: Int): Long? = fields[index].toLongOrNull()

        val sessionId = fields[1].ifBlank { return null }
        val testId = fields[2].ifBlank { return null }
        val titleRes = intAt(3) ?: return null
        val startedAt = longAt(4) ?: return null
        val completedAt = longAt(5) ?: return null
        val records = decodeRecords(fields[6]) ?: return null
        val total = intAt(7) ?: return null
        val correct = intAt(8) ?: return null
        val incorrect = intAt(9) ?: return null
        val unanswered = intAt(10) ?: return null
        val marked = intAt(11) ?: return null
        val scoreQuarters = intAt(12) ?: return null
        val maxQuarters = intAt(13) ?: return null
        val correctQuarters = intAt(14) ?: return null
        val penaltyQuarters = fields[15].toIntOrNull() ?: return null
        val timeTaken = longAt(16) ?: return null
        val submission = when (fields[17]) {
            "MANUAL" -> MockTestSubmission.MANUAL
            "TIMEOUT" -> MockTestSubmission.TIMEOUT
            else -> return null
        }

        // Counts must agree with the stored responses, otherwise the review
        // screen and the summary would disagree.
        if (correct + incorrect + unanswered != total) return null
        if (records.size != total) return null
        val answeredRecords = records.count { it.selectedOptionIndex != null }
        if (answeredRecords != correct + incorrect) return null
        if (scoreQuarters != correct * correctQuarters - incorrect * penaltyQuarters) return null

        return MockTestResult(
            sessionId = sessionId,
            testId = MockTestId(testId),
            testTitleRes = titleRes,
            startedAtEpochMillis = startedAt,
            completedAtEpochMillis = completedAt,
            records = records,
            totalQuestions = total,
            correct = correct,
            incorrect = incorrect,
            unanswered = unanswered,
            markedForReview = marked,
            scoreInQuarters = scoreQuarters,
            maxMarksInQuarters = maxQuarters,
            marksPerCorrectInQuarters = correctQuarters,
            negativeMarkInQuarters = penaltyQuarters,
            timeTakenMillis = timeTaken,
            submission = submission
        )
    }

    private fun encodeResult(result: MockTestResult): String = listOf(
        VERSION_PREFIX,
        result.sessionId,
        result.testId.value,
        result.testTitleRes.toString(),
        result.startedAtEpochMillis.toString(),
        result.completedAtEpochMillis.toString(),
        result.records.joinToString(PAIR) { record ->
            listOf(
                record.questionId.value,
                record.order.toString(),
                record.selectedOptionIndex?.toString() ?: "",
                if (record.markedForReview) "1" else "0"
            ).joinToString(KV)
        },
        result.totalQuestions.toString(),
        result.correct.toString(),
        result.incorrect.toString(),
        result.unanswered.toString(),
        result.markedForReview.toString(),
        result.scoreInQuarters.toString(),
        result.maxMarksInQuarters.toString(),
        result.marksPerCorrectInQuarters.toString(),
        result.negativeMarkInQuarters.toString(),
        result.timeTakenMillis.toString(),
        result.submission.name
    ).joinToString(SEP) + "\n"

    private fun decodeRecords(raw: String): List<MockTestQuestionRecord>? {
        if (raw.isBlank()) return null
        return raw.splitPairs().map { pair ->
            val parts = pair.splitAtMulti(KV, expected = 4) ?: return null
            val selected = parts[2]
            // An empty slot means the question was never answered, which is not
            // the same as a malformed index.
            val selectedIndex = if (selected.isBlank()) {
                null
            } else {
                selected.toIntOrNull() ?: return null
            }
            MockTestQuestionRecord(
                questionId = QuestionId(parts[0]),
                order = parts[1].toIntOrNull() ?: return null,
                selectedOptionIndex = selectedIndex,
                markedForReview = when (parts[3]) {
                    "1" -> true
                    "0" -> false
                    else -> return null
                }
            )
        }
    }

    private const val MAX_STORED_RESULTS = 30

    private fun String.splitPairs(): List<String> =
        if (isBlank()) emptyList() else split(PAIR).filter { it.isNotBlank() }

    private fun String.splitAt(separator: String): Pair<String, String>? {
        val index = indexOf(separator)
        if (index <= 0) return null
        return substring(0, index) to substring(index + separator.length)
    }

    private fun String.splitAtMulti(separator: String, expected: Int): List<String>? {
        val parts = split(separator)
        if (parts.size != expected) return null
        return parts
    }
}