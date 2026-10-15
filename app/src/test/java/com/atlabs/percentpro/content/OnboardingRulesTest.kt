package com.atlabs.percentpro.content

import com.atlabs.percentpro.content.OnboardingRules.Evidence
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule that decides whether somebody gets the first-use welcome pages.
 *
 * Phase 8's whole onboarding feature rests on this one question, and the answer
 * is only observable on a device. Pulling it into [OnboardingRules] makes the
 * full matrix testable here instead, which is the only practical way to be sure
 * that every existing user is skipped and every new user is shown them.
 */
class OnboardingRulesTest {

    @Test
    fun `an empty install is new and is shown onboarding`() {
        assertFalse(OnboardingRules.isReturningUser(OnboardingRules.NEW_INSTALL))
        assertFalse(OnboardingRules.shouldSkipOnboarding(Evidence()))
    }

    @Test
    fun `a finished lesson marks the install as returning`() {
        val evidence = Evidence(completedLessons = 1)
        assertTrue(OnboardingRules.shouldSkipOnboarding(evidence))
    }

    @Test
    fun `a single practice run marks the install as returning`() {
        assertTrue(OnboardingRules.shouldSkipOnboarding(Evidence(practiceRuns = 1)))
    }

    @Test
    fun `a single saved mock test marks the install as returning`() {
        assertTrue(OnboardingRules.shouldSkipOnboarding(Evidence(savedMockTests = 1)))
    }

    @Test
    fun `spent XP alone is enough to skip onboarding`() {
        // The visible history can be empty while XP is not, for instance if a
        // reward was recorded and its result was later cleared.
        assertTrue(OnboardingRules.shouldSkipOnboarding(Evidence(totalXp = 20)))
    }

    @Test
    fun `answered questions alone are enough to skip onboarding`() {
        assertTrue(OnboardingRules.shouldSkipOnboarding(Evidence(answeredQuestions = 1)))
    }

    @Test
    fun `a pre-phase-5 ledger entry skips onboarding even with no XP`() {
        // Pre-Phase-5 activity is deliberately recorded at zero XP so it cannot
        // manufacture a reward. It is still proof the app was in use, so it must
        // still count here or an upgrading user gets the welcome tour.
        val evidence = Evidence(
            ledgerLessons = 1,
            ledgerMockTests = 0,
            totalXp = 0
        )
        assertTrue(OnboardingRules.shouldSkipOnboarding(evidence))
    }

    @Test
    fun `a pre-phase-5 mock test entry skips onboarding`() {
        assertTrue(
            OnboardingRules.shouldSkipOnboarding(Evidence(ledgerMockTests = 2))
        )
    }

    @Test
    fun `every source independently triggers the skip`() {
        val sources = listOf(
            Evidence(completedLessons = 3),
            Evidence(practiceRuns = 2),
            Evidence(savedMockTests = 1),
            Evidence(totalXp = 5),
            Evidence(answeredQuestions = 9),
            Evidence(ledgerLessons = 1),
            Evidence(ledgerMockTests = 1)
        )
        sources.forEach { evidence ->
            assertTrue(
                "expected $evidence to count as prior activity",
                OnboardingRules.shouldSkipOnboarding(evidence)
            )
        }
    }

    @Test
    fun `every field at zero is still a new install`() {
        assertFalse(
            OnboardingRules.isReturningUser(
                Evidence(
                    completedLessons = 0,
                    practiceRuns = 0,
                    savedMockTests = 0,
                    totalXp = 0,
                    answeredQuestions = 0,
                    ledgerLessons = 0,
                    ledgerMockTests = 0
                )
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative evidence is rejected rather than silently treated as new`() {
        // A negative count means a caller mis-wired a field. Failing loudly here
        // is better than quietly reporting "new" and replaying the tour.
        Evidence(completedLessons = -1)
    }

    @Test
    fun `a user with a lot of activity is still only skipped once`() {
        // The rule is idempotent by nature: it is asked a question about stored
        // data, it never changes that data, so asking twice gives one answer.
        val evidence = Evidence(completedLessons = 12, totalXp = 340, practiceRuns = 30)
        val first = OnboardingRules.shouldSkipOnboarding(evidence)
        val second = OnboardingRules.shouldSkipOnboarding(evidence)
        assertTrue(first)
        assertTrue(second)
        assertTrue(first == second)
    }
}