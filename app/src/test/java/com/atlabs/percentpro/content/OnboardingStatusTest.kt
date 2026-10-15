package com.atlabs.percentpro.content

import com.atlabs.percentpro.content.OnboardingStatus.Done
import com.atlabs.percentpro.content.OnboardingStatus.Loading
import com.atlabs.percentpro.content.OnboardingStatus.Pending
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How the stored onboarding flag becomes a screen decision.
 *
 * This mapping is the whole of the "does the welcome tour show" logic, and it is
 * exactly where a real bug shipped: the flag was passed through to the root
 * composable raw, where an absent key arrived as `null` and a stored flag as
 * `true`. The root read both as "onboarding pending", so pressing Skip or Start
 * learning wrote `true` — the value that kept the tour on screen. The tour could
 * not be dismissed and the home screen was unreachable.
 *
 * These tests are the regression net for that. They need no Context, no
 * DataStore and no device.
 */
class OnboardingStatusTest {

    // ---- the bug itself ----------------------------------------------------

    @Test
    fun `a stored flag means the tour is done, not pending`() {
        // This is the assertion that would have failed before the fix: `true`
        // used to reach the screen as "show onboarding".
        assertEquals(Done, OnboardingStatus.fromStored(true))
    }

    @Test
    fun `an absent key means the tour is pending, not unread`() {
        // An absent key used to arrive as `null`, which collided with the
        // "not read from disk yet" sentinel and left a new user on a blank
        // screen with no way forward.
        assertEquals(Pending, OnboardingStatus.fromStored(null))
    }

    @Test
    fun `an explicit false is still pending`() {
        // The key exists but onboarding has not happened. Treated as pending so
        // that a stored `false` can never lock a user out of the tour.
        assertEquals(Pending, OnboardingStatus.fromStored(false))
    }

    @Test
    fun `finishing the tour changes the state`() {
        // The transition that was broken end to end: dismissing the tour must
        // move the app from showing onboarding to showing the app.
        val before = OnboardingStatus.fromStored(null)
        val after = OnboardingStatus.fromStored(true)
        assertEquals(Pending, before)
        assertEquals(Done, after)
        assertNotEquals(before, after)
    }

    // ---- the loading sentinel ---------------------------------------------

    @Test
    fun `reading storage never yields the loading state`() {
        // Loading is the ViewModel's pre-read value only. If the mapping could
        // produce it, the sentinel would collide with real data again.
        listOf(true, false, null).forEach { stored ->
            assertNotEquals(
                "fromStored($stored) must not produce Loading",
                Loading,
                OnboardingStatus.fromStored(stored)
            )
        }
    }

    @Test
    fun `there are exactly three states and none is equal to another`() {
        val states = listOf(Loading, Pending, Done)
        assertEquals(3, states.toSet().size)
    }

    // ---- combined with the existing-user rule ------------------------------

    @Test
    fun `an existing user is Done from their stored evidence`() {
        // hasPriorActivity decides whether the launch sequence writes the flag
        // at all. Either it writes it, and fromStored reports Done, or it does
        // not, and a genuinely new user stays on Pending. Both paths must agree.
        val returning = OnboardingRules.Evidence(completedLessons = 1)
        assertTrue(OnboardingRules.shouldSkipOnboarding(returning))
        // The seed writes `true`; that is what the reader then sees.
        assertEquals(Done, OnboardingStatus.fromStored(true))

        val fresh = OnboardingRules.NEW_INSTALL
        assertTrue(!OnboardingRules.shouldSkipOnboarding(fresh))
        // No seed, key stays absent, and the reader keeps showing the tour.
        assertEquals(Pending, OnboardingStatus.fromStored(null))
    }
}