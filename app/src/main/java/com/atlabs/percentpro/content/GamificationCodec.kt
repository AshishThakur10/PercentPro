package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.ActivityId
import com.atlabs.percentpro.model.AchievementId
import com.atlabs.percentpro.model.AchievementId.Companion.parse as parseAchievementId
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.QuestionId

/**
 * Serialises [GamificationState].
 *
 * Format: one `key=value` line per field, joined by `\n`, in a fixed order and
 * identified by key rather than position, so adding a field later cannot
 * invalidate an existing file the way a positional format would.
 *
 * Sets are stored as comma-joined values, with every value restricted to a safe
 * character set. Anything that fails to parse is dropped rather than throwing,
 * so one damaged entry costs a single achievement or a single counted question
 * instead of the whole profile.
 */
object GamificationCodec {

    const val CURRENT_VERSION = 1

    private const val SEP = "\n"
    private const val PAIR = ","
    private const val KV = "="
    private const val FIELD_COUNT = 14

    private const val VERSION = "version"
    private const val TOTAL_XP = "total_xp"
    private const val DAILY_GOAL = "daily_goal"
    private const val LEDGER = "ledger"
    private const val PRE_EXISTING = "preexisting"
    private const val ANSWERED = "answered"
    private const val LESSONS = "lessons"
    private const val MOCK_TESTS = "mock_tests"
    private const val PERFECT_TOPICS = "perfect_topics"
    private const val QUALIFYING = "qualifying_dates"
    private const val GOAL_DATES = "goal_reached_dates"
    private const val ACTIVITY_DATE = "activity_date"
    private const val TODAY_ANSWERED = "today_answered"
    private const val UNLOCKED = "unlocked"

    fun encode(state: GamificationState): String = listOf(
        "$VERSION$KV$CURRENT_VERSION",
        "$TOTAL_XP$KV${state.totalXp.coerceAtLeast(0)}",
        "$DAILY_GOAL$KV${state.dailyQuestionGoal.coerceAtLeast(0)}",
        "$LEDGER$KV${state.rewardedActivityIds.sorted().joinToString(PAIR)}",
        "$PRE_EXISTING$KV${state.preExistingActivityIds.sorted().joinToString(PAIR)}",
        "$ANSWERED$KV${state.distinctAnsweredQuestionIds.joinToString(PAIR) { it.value }}",
        "$LESSONS$KV${state.completedLessonIds.sorted().joinToString(PAIR)}",
        "$MOCK_TESTS$KV${state.completedMockTestIds.sorted().joinToString(PAIR)}",
        "$PERFECT_TOPICS$KV${state.perfectPracticeTopics.sorted().joinToString(PAIR)}",
        "$QUALIFYING$KV${state.qualifyingDates.joinToString(PAIR) { it.value.toString() }}",
        "$GOAL_DATES$KV${state.goalReachedDates.joinToString(PAIR) { it.value.toString() }}",
        "$ACTIVITY_DATE$KV${state.activityDate?.value?.toString().orEmpty()}",
        "$TODAY_ANSWERED$KV${state.todayAnsweredQuestionIds.joinToString(PAIR) { it.value }}",
        // Entries joined by PAIR, id and timestamp joined by KV. Both separators
        // are rejected inside an id, so neither can be forged from a value.
        "$UNLOCKED$KV${state.unlockedAchievements.entries
            .sortedBy { it.key.value }
            .joinToString(PAIR) { "${it.key.value}$KV${it.value}" }}"
    ).joinToString(SEP)

    /**
     * Reads a stored profile, repairing whatever is inconsistent.
     *
     * Returns a clean default for missing, empty or unreadable input. A wrong
     * version is treated as unusable rather than guessed at, because guessing
     * could resurrect a ledger with the wrong XP values. Within a readable
     * file, total XP is recomputed from the ledger so the two always agree, and
     * an achievement id the app no longer knows is dropped.
     */
    fun decode(raw: String?): GamificationState {
        if (raw.isNullOrBlank()) return GamificationState()

        val fields = parseFields(raw)
        if (fields.size != FIELD_COUNT) return GamificationState()
        if (fields[VERSION]?.toIntOrNull() != CURRENT_VERSION) return GamificationState()

        val ledger = fields[LEDGER].orEmptyValues()
            .mapNotNull { ActivityId.parse(it) }
            .map { it.value }
            .toSet()

        // Drop answered questions and activity dates that are not today, so a
        // stale file cannot resurrect yesterday's daily progress.
        val activityDate = fields[ACTIVITY_DATE]?.toIntOrNull()?.let(::LocalDateKey)
        val todayAnswered = fields[TODAY_ANSWERED].orEmptyValues()
            .filter { isSafeToken(it) }
            .map { QuestionId(it) }
            .toSet()

        val unlocked = fields[UNLOCKED].orEmptyValues().mapNotNull { entry ->
            val parts = entry.split(KV)
            if (parts.size != 2) return@mapNotNull null
            val id = parseAchievementId(parts[0]) ?: return@mapNotNull null
            if (!AchievementCatalogue.isKnown(id)) return@mapNotNull null
            parts[1].toLongOrNull()?.takeIf { it >= 0L }?.let { id to it }
        }.toMap()

        return GamificationState(
            // Always derived, never trusted from the file.
            totalXp = GamificationEngine.xpOf(ledger),
            dailyQuestionGoal = fields[DAILY_GOAL]?.toIntOrNull()?.coerceAtLeast(0) ?: 10,
            rewardedActivityIds = ledger,
            preExistingActivityIds = fields[PRE_EXISTING].orEmptyValues()
                .mapNotNull { ActivityId.parse(it)?.value }
                .toSet(),
            distinctAnsweredQuestionIds = fields[ANSWERED].orEmptyValues()
                .filter { isSafeToken(it) }
                .map { QuestionId(it) }
                .toSet(),
            completedLessonIds = fields[LESSONS].orEmptyValues()
                .filter { isSafeToken(it) && LessonId.fromStorageKey(it) != null }
                .toSet(),
            completedMockTestIds = fields[MOCK_TESTS].orEmptyValues().filter { isSafeToken(it) }.toSet(),
            perfectPracticeTopics = fields[PERFECT_TOPICS].orEmptyValues()
                .filter { isSafeToken(it) && LessonId.fromStorageKey(it) != null }
                .toSet(),
            qualifyingDates = fields[QUALIFYING].orEmptyValues()
                .mapNotNull { it.toIntOrNull() }
                .map(::LocalDateKey)
                .toSet(),
            goalReachedDates = fields[GOAL_DATES].orEmptyValues()
                .mapNotNull { it.toIntOrNull() }
                .map(::LocalDateKey)
                .toSet(),
            activityDate = activityDate,
            todayAnsweredQuestionIds = todayAnswered,
            unlockedAchievements = unlocked
        )
    }

    private fun parseFields(raw: String): Map<String, String> =
        raw.split(SEP)
            .mapNotNull { line ->
                val index = line.indexOf(KV)
                if (index <= 0) return@mapNotNull null
                line.substring(0, index) to line.substring(index + KV.length)
            }
            .toMap()

    private fun String?.orEmptyValues(): List<String> =
        if (isNullOrBlank()) emptyList() else split(PAIR).filter { it.isNotBlank() }

    /** Ledger entries use `:` and ids use `_`, so nothing may contain the separators. */
    private fun isSafeToken(value: String): Boolean =
        value.isNotBlank() && value.none { it == '|' || it == ',' || it == '=' || it == '\n' }
}