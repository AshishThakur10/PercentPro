package com.atlabs.percentpro.data

import com.atlabs.percentpro.content.AchievementCatalogue
import com.atlabs.percentpro.content.GamificationCodec
import com.atlabs.percentpro.content.GamificationEngine
import com.atlabs.percentpro.content.GamificationState
import com.atlabs.percentpro.content.LearningActivity
import com.atlabs.percentpro.model.ActivityId
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.LocalDates
import com.atlabs.percentpro.model.QuestionId
import com.atlabs.percentpro.model.RewardOutcome
import kotlinx.coroutines.flow.map
import java.util.TimeZone

/**
 * The one place gamification state changes.
 *
 * Every mutation goes through [GamificationStore.update], so the decode, the
 * apply and the re-encode all happen against the stored value inside a single
 * atomic step. There is deliberately no "add XP" method: the only way to change
 * the total is to report a learning activity, and reporting the same activity
 * twice yields the same ledger entry and adds nothing.
 *
 * Its own storage, separate from lesson progress, practice history and mock-test
 * history, so no other feature can accidentally award or revoke a reward.
 */
class GamificationRepository(
    private val store: GamificationStore,
    private val baselineMarker: BaselineMarkerStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val timeZone: () -> TimeZone = TimeZone::getDefault
) {

    val state = store.payload.map { decode(it) }

    /** Achievements in display order: unlocked first, then closest to unlocking. */
    val achievements = state.map { current ->
        AchievementCatalogue.all.map { definition ->
            com.atlabs.percentpro.model.AchievementStatus(
                definition = definition,
                unlockedAtEpochMillis = current.unlockedAchievements[definition.id],
                current = AchievementCatalogue.progress(current, definition)
            )
        }.sortedWith(
            compareByDescending<com.atlabs.percentpro.model.AchievementStatus> { it.isUnlocked }
                .thenByDescending { it.progressFraction }
        )
    }

    suspend fun current(): GamificationState = decode(store.read())

    /**
     * Records activity and reports what it actually earned.
     *
     * Both fields are zero or empty for a repeated event, because the ledger
     * already holds the id. That is what stops a duplicate callback from paying
     * twice, and it is also why a screen may announce the reward
     * unconditionally: it only ever has something to say the first time.
     *
     * The whole read-apply-write runs inside one atomic update, so two callers
     * reporting the same activity at the same moment cannot both be paid.
     */
    suspend fun record(activity: LearningActivity): RewardOutcome {
        var outcome = RewardOutcome.NONE
        store.update { raw ->
            val before = decode(raw)
            val applied = GamificationEngine.applyAndEvaluate(before, activity, now())
            if (applied == before) {
                raw
            } else {
                outcome = RewardOutcome(
                    xpAwarded = applied.totalXp - before.totalXp,
                    unlockedAchievements = (applied.unlockedAchievements.keys -
                        before.unlockedAchievements.keys).toList()
                )
                GamificationCodec.encode(applied)
            }
        }
        return outcome
    }

    /** Convenience for the common case of a submitted answer. */
    suspend fun recordAnswer(questionId: QuestionId, wasCorrect: Boolean): RewardOutcome =
        record(
            LearningActivity.AnsweredQuestion(
                questionId = questionId,
                wasCorrect = wasCorrect,
                occurredOn = today()
            )
        )

    /**
     * Writes pre-existing activity into the ledger once, at zero XP.
     *
     * Lessons and mock tests completed before this feature existed are entered so
     * that repeating them later cannot pay out. They are deliberately **not**
     * added to the achievement counters, so an existing user's XP and
     * achievements start from zero rather than having history invented for them.
     *
     * The caller supplies what is already recorded, which keeps this class
     * independent of the other repositories and makes the policy directly
     * testable.
     *
     * Topic-practice history is deliberately absent: a stored practice result
     * records only counts, not which question ids were answered correctly, so
     * there is nothing trustworthy to seed from. A question answered correctly
     * that way before this feature may therefore still earn its first-correct XP
     * once. This is documented rather than guessed at.
     */
    suspend fun seedPreExistingActivity(
        completedLessonKeys: Set<String>,
        completedMockTestIds: Set<String>
    ) {
        if (baselineMarker.isSeeded()) return

        // Written to `preExistingActivityIds`, not the paid ledger: these must
        // block a payout without contributing XP, because total XP is derived
        // from the ledger.
        val seedIds = buildSet {
            completedLessonKeys.forEach { add(ActivityId.lessonCompletedOf(it).value) }
            completedMockTestIds.forEach { add(ActivityId.mockTestCompletedOf(it).value) }
        }

        if (seedIds.isNotEmpty()) {
            store.update { raw ->
                val current = decode(raw)
                // Ledger entries only. totalXp is written as 0 because these
                // entries exist to block a payout, not to make one.
                GamificationCodec.encode(
                    current.copy(preExistingActivityIds = current.preExistingActivityIds + seedIds)
                )
            }
        }
        baselineMarker.markSeeded()
    }

    /** Changes the daily target. Today's progress is preserved. */
    suspend fun setDailyGoal(questions: Int) {
        val clamped = questions.coerceAtLeast(0)
        store.update { raw ->
            GamificationCodec.encode(GamificationEngine.setDailyGoal(decode(raw), clamped))
        }
    }

    /** Clears the whole profile. Used by tests and by a future reset control. */
    suspend fun clearAll() {
        store.update { null }
        baselineMarker.clear()
    }

    /** The local calendar day, re-read each call so a clock change is picked up. */
    fun today(): LocalDateKey = LocalDates.fromEpochMillis(now(), timeZone())

    private fun decode(raw: String?): GamificationState =
        GamificationEngine.rollOverTo(GamificationCodec.decode(raw), today())
}