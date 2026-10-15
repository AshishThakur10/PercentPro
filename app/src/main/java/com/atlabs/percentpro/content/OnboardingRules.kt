package com.atlabs.percentpro.content

/**
 * Whether an install is new enough to deserve the first-use welcome pages.
 *
 * Phase 8 added onboarding, and the obvious implementation — show it when the
 * stored flag is absent — would replay a welcome tour at every user who was
 * already studying when the app was updated. Nobody who has finished a lesson or
 * sat a mock test needs to be told what the app is for.
 *
 * So the flag alone is not the answer. The flag says whether onboarding has been
 * *dealt with*; this says whether there is any reason to *offer* it. Both are
 * needed, and they are deliberately separate: [isReturningUser] can never
 * un-seen a flag that is already set, and the flag can never override a real
 * store full of progress.
 *
 * Pure and separate from [com.atlabs.percentpro.data.AppContainer] so the rule
 * can be tested exhaustively without a Context, a DataStore or a device.
 */
object OnboardingRules {

    /**
     * A count of anything that could only have come from using the app.
     *
     * Counts rather than booleans so a caller cannot quietly miss one field and
     * silently report "new": every source has to be supplied on purpose.
     */
    data class Evidence(
        /** Finished lessons in the progress store. */
        val completedLessons: Int = 0,
        /** Practice runs recorded in practice history. */
        val practiceRuns: Int = 0,
        /** Saved mock test results. */
        val savedMockTests: Int = 0,
        /** XP earned, which is derived from the paid reward ledger. */
        val totalXp: Int = 0,
        /** Distinct questions submitted since gamification existed. */
        val answeredQuestions: Int = 0,
        /** Lessons present in the reward ledger, including pre-Phase-5 ones. */
        val ledgerLessons: Int = 0,
        /** Mock tests present in the reward ledger, including pre-Phase-5 ones. */
        val ledgerMockTests: Int = 0
    ) {
        init {
            require(
                completedLessons >= 0 && practiceRuns >= 0 && savedMockTests >= 0 &&
                    totalXp >= 0 && answeredQuestions >= 0 && ledgerLessons >= 0 &&
                    ledgerMockTests >= 0
            ) { "Evidence counts cannot be negative" }
        }
    }

    /** No trace of use at all: a genuinely new install. */
    val NEW_INSTALL = Evidence()

    /**
     * True when there is any sign the app was already in use.
     *
     * XP and the reward-ledger counts are checked as well as the visible
     * history because a user can hold XP from a reward that has since been
     * superseded, and because a pre-Phase-5 install has ledger entries with zero
     * XP attached. Either one means the app was open before onboarding existed.
     */
    fun isReturningUser(evidence: Evidence): Boolean =
        evidence.completedLessons > 0 ||
            evidence.practiceRuns > 0 ||
            evidence.savedMockTests > 0 ||
            evidence.totalXp > 0 ||
            evidence.answeredQuestions > 0 ||
            evidence.ledgerLessons > 0 ||
            evidence.ledgerMockTests > 0

    /**
     * The single question the launch sequence asks: should the welcome pages be
     * marked as already dealt with, so they are never shown?
     */
    fun shouldSkipOnboarding(evidence: Evidence): Boolean = isReturningUser(evidence)
}

/**
 * Whether the welcome pages should be on screen, as three distinct states.
 *
 * This type exists because of a bug. It was previously a `Boolean?` read
 * straight out of DataStore, where an absent key emitted `null` and a stored
 * flag emitted `true`. The root composable then read that as "is onboarding
 * pending?", so `true` — the value written *after* onboarding was dismissed —
 * meant "show onboarding", and dismissing it was a no-op that left the user
 * stuck on the welcome pages with no way into the app. The same `null` also had
 * to double as "not read from disk yet", so a genuinely new user, whose key is
 * simply absent, was indistinguishable from an unread store.
 *
 * Three named states remove both problems at once: the mapping from storage to
 * screen lives in exactly one place ([fromStored]), and `Loading` can never be
 * produced by that mapping, so the sentinel cannot collide with real data.
 */
sealed interface OnboardingStatus {

    /** The stored flag has not been read yet. Draw nothing rather than guess. */
    data object Loading : OnboardingStatus

    /** Never seen, or explicitly skipped: the welcome pages belong on screen. */
    data object Pending : OnboardingStatus

    /** Already seen or skipped: go straight into the app. */
    data object Done : OnboardingStatus

    companion object {

        /**
         * Interprets the stored flag.
         *
         * An absent key means "never seen", so it maps to [Pending] rather than
         * to a null that would be confused with not-having-read-yet. `false` is
         * treated the same way: if the key is present but not set, onboarding
         * still has not happened.
         */
        fun fromStored(seen: Boolean?): OnboardingStatus =
            if (seen == true) Done else Pending
    }
}