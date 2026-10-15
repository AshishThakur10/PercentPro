package com.atlabs.percentpro.content

import com.atlabs.percentpro.data.GamificationRepository
import com.atlabs.percentpro.model.ActivityId
import com.atlabs.percentpro.model.AchievementId
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.LocalDates
import com.atlabs.percentpro.model.QuestionId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class GamificationRepositoryTest {

    private val zone = TimeZone.getTimeZone("Asia/Kolkata")
    private val clock = TestClock.starting()

    private fun repo(
        store: InMemoryGamificationStore = InMemoryGamificationStore(),
        baseline: InMemoryBaselineMarker = InMemoryBaselineMarker()
    ) = GamificationRepository(
        store = store,
        baselineMarker = baseline,
        now = { clock.epochMillis },
        timeZone = { zone }
    )

    private fun answer(id: String, correct: Boolean = true) =
        LearningActivity.AnsweredQuestion(QuestionId(id), correct, clock.today())

    // ---- First-time XP through the repository ------------------------------

    @Test
    fun `a first lesson completion pays once and reports the amount`() = runBlocking {
        val repository = repo()
        val activity = LearningActivity.LessonCompleted("what_is_a_percentage", clock.today())

        val first = repository.record(activity)
        val second = repository.record(activity)

        assertEquals(20, first.xpAwarded)
        assertEquals(0, second.xpAwarded)
        assertTrue(second.isEmpty)
        assertEquals(20, repository.current().totalXp)
    }

    @Test
    fun `a first correct answer pays once across practice and mock tests`() = runBlocking {
        val repository = repo()

        val practice = repository.record(answer("shared_question"))
        // The same question id answered again, as a mock test answer.
        val mockTest = repository.record(answer("shared_question"))

        assertEquals(5, practice.xpAwarded)
        assertEquals(0, mockTest.xpAwarded)
        assertEquals(5, repository.current().totalXp)
        assertEquals(1, repository.current().uniqueQuestionCount)
    }

    @Test
    fun `an incorrect answer pays nothing and reports nothing`() = runBlocking {
        val repository = repo()

        val outcome = repository.record(answer("q1", correct = false))

        assertEquals(0, outcome.xpAwarded)
        assertFalse(outcome.hasAchievements)
        assertTrue(outcome.isEmpty)
        assertEquals(0, repository.current().totalXp)
    }

    @Test
    fun `mock test completion pays exactly once across retries and timeouts`() = runBlocking {
        val repository = repo()
        val activity = LearningActivity.MockTestCompleted("percentage_aptitude_15", clock.today())

        val manual = repository.record(activity)
        val timeout = repository.record(activity)
        val retry = repository.record(activity)

        assertEquals(10, manual.xpAwarded)
        assertEquals(0, timeout.xpAwarded)
        assertEquals(0, retry.xpAwarded)
        assertEquals(10, repository.current().totalXp)
    }

    @Test
    fun `a repeated record writes nothing to storage`() = runBlocking {
        val store = InMemoryGamificationStore()
        val repository = repo(store)

        repository.record(answer("q1"))
        val writesAfterFirst = store.writeCount
        repository.record(answer("q1"))
        repository.record(answer("q1"))

        assertEquals(writesAfterFirst, store.writeCount)
    }

    @Test
    fun `concurrent records of the same activity pay only once`() = runBlocking {
        val repository = repo()

        val outcomes = coroutineScope {
            (1..10).map { async { repository.record(answer("q1")) } }.awaitAll()
        }

        assertEquals(1, outcomes.count { it.xpAwarded > 0 })
        assertEquals(5, repository.current().totalXp)
    }

    // ---- Persistence across recreation -------------------------------------

    @Test
    fun `state survives a new repository instance over the same store`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()

        repo(store, baseline).apply {
            record(answer("q1"))
            record(answer("q2", correct = false))
            record(LearningActivity.LessonCompleted("what_is_a_percentage", clock.today()))
        }

        // Simulates the process being recreated: brand new repository objects,
        // the same underlying storage.
        val recreated = repo(store, baseline)
        val restored = recreated.current()

        assertEquals(25, restored.totalXp)
        assertEquals(2, restored.uniqueQuestionCount)
        assertEquals(2, restored.todayQuestionCount)
        assertEquals(1, restored.lessonCount)
        assertTrue(AchievementId("first_steps") in restored.unlockedAchievements)
    }

    @Test
    fun `achievement unlock timestamps survive recreation`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()
        repo(store, baseline).record(LearningActivity.LessonCompleted("what_is_a_percentage", clock.today()))

        val restored = repo(store, baseline).current().unlockedAchievements

        assertEquals(
            clock.epochMillis,
            restored[AchievementId("first_steps")]
        )
    }

    @Test
    fun `a second app launch does not re-seed or re-pay`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()
        repo(store, baseline).seedPreExistingActivity(
            completedLessonKeys = setOf("what_is_a_percentage"),
            completedMockTestIds = emptySet()
        )

        // Launch two.
        repo(store, baseline).seedPreExistingActivity(
            completedLessonKeys = setOf("what_is_a_percentage", "percentages_visually"),
            completedMockTestIds = emptySet()
        )

        val state = repo(store, baseline).current()
        assertEquals(0, state.totalXp)
        // Only the originally seeded lesson is remembered.
        assertEquals(1, state.preExistingActivityIds.size)
    }

    // ---- Pre-existing activity ---------------------------------------------

    @Test
    fun `a lesson completed before the feature never pays afterwards`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()
        val repository = repo(store, baseline)

        repository.seedPreExistingActivity(
            completedLessonKeys = setOf(LessonId.WHAT_IS_A_PERCENTAGE.storageKey),
            completedMockTestIds = emptySet()
        )

        val outcome = repository.record(
            LearningActivity.LessonCompleted(LessonId.WHAT_IS_A_PERCENTAGE.storageKey, clock.today())
        )

        assertEquals(0, outcome.xpAwarded)
        assertEquals(0, repository.current().totalXp)
    }

    @Test
    fun `a mock test finished before the feature never pays afterwards`() = runBlocking {
        val repository = repo()
        repository.seedPreExistingActivity(
            completedLessonKeys = emptySet(),
            completedMockTestIds = setOf("percentage_aptitude_15")
        )

        val outcome = repository.record(
            LearningActivity.MockTestCompleted("percentage_aptitude_15", clock.today())
        )

        assertEquals(0, outcome.xpAwarded)
    }

    @Test
    fun `seeding awards no XP and no achievement progress for pre-existing work`() = runBlocking {
        val repository = repo()
        repository.seedPreExistingActivity(
            completedLessonKeys = LessonId.entries.take(5).map { it.storageKey }.toSet(),
            completedMockTestIds = setOf("percentage_aptitude_15")
        )

        val state = repository.current()

        assertEquals(0, state.totalXp)
        // The seeded ids block a payout but sit outside the paid ledger, so they
        // manufacture no XP and no achievement progress.
        assertEquals(6, state.preExistingActivityIds.size)
        assertTrue(state.rewardedActivityIds.isEmpty())
        assertEquals(0, state.lessonCount)
        assertEquals(0, state.mockTestCount)
        assertTrue(state.unlockedAchievements.isEmpty())
    }

    @Test
    fun `seeding then completing a new lesson still pays normally`() = runBlocking {
        val repository = repo()
        repository.seedPreExistingActivity(
            completedLessonKeys = setOf("what_is_a_percentage"),
            completedMockTestIds = emptySet()
        )

        val outcome = repository.record(
            LearningActivity.LessonCompleted(LessonId.PERCENTAGES_VISUALLY.storageKey, clock.today())
        )

        assertEquals(20, outcome.xpAwarded)
        assertEquals(1, repository.current().lessonCount)
    }

    // ---- Daily goal through the repository --------------------------------

    @Test
    fun `changing the goal keeps today's progress`() = runBlocking {
        val repository = repo()
        repeat(12) { repository.record(answer("q$it")) }
        assertEquals(12, repository.current().todayQuestionCount)

        repository.setDailyGoal(10)

        val state = repository.current()
        assertEquals(10, state.dailyQuestionGoal)
        assertEquals(12, state.todayQuestionCount)
        assertTrue(state.isDailyGoalReached)
    }

    @Test
    fun `the daily goal setting itself survives recreation`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()
        repo(store, baseline).setDailyGoal(30)

        assertEquals(30, repo(store, baseline).current().dailyQuestionGoal)
    }

    // ---- Date boundaries ---------------------------------------------------

    @Test
    fun `questions answered yesterday do not count toward today's goal`() = runBlocking {
        val repository = repo()
        repeat(4) { repository.record(answer("q$it")) }
        assertEquals(4, repository.current().todayQuestionCount)

        clock.advanceDays(1)

        assertEquals(0, repository.current().todayQuestionCount)
        assertEquals(4, repository.current().uniqueQuestionCount)
    }

    @Test
    fun `a streak is extended across a simulated app restart`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()
        val start = clock.today()

        // Day 1: complete a lesson.
        repo(store, baseline).record(LearningActivity.LessonCompleted("l1", start))

        // Restart.
        clock.advanceDays(1)
        val afterRestart = repo(store, baseline).current()
        assertEquals(1, com.atlabs.percentpro.content.GamificationEngine.currentStreak(afterRestart, clock.today()))

        // Day 2: five questions.
        repeat(5) { repo(store, baseline).record(answer("q$it")) }

        assertEquals(
            2,
            com.atlabs.percentpro.content.GamificationEngine.currentStreak(repo(store, baseline).current(), clock.today())
        )
    }

    @Test
    fun `a long gap breaks the streak and keeps the longest`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()
        val engine = com.atlabs.percentpro.content.GamificationEngine

        repo(store, baseline).record(LearningActivity.LessonCompleted("l1", clock.today()))
        clock.advanceDays(1)
        repo(store, baseline).record(LearningActivity.LessonCompleted("l2", clock.today()))
        assertEquals(2, engine.currentStreak(repo(store, baseline).current(), clock.today()))

        clock.advanceDays(5)
        repo(store, baseline).record(LearningActivity.LessonCompleted("l3", clock.today()))

        val state = repo(store, baseline).current()
        assertEquals(1, engine.currentStreak(state, clock.today()))
        assertEquals(2, engine.longestStreak(state))
    }

    @Test
    fun `activity is attributed to the device's local date`() = runBlocking {
        clock.epochMillis = startOfLocalDay(2026, 1, 16, zone)

        val kolkata = repo()
        val utc = GamificationRepository(
            store = InMemoryGamificationStore(),
            baselineMarker = InMemoryBaselineMarker(),
            now = { clock.epochMillis },
            timeZone = { TimeZone.getTimeZone("UTC") }
        )

        // The same instant is the 16th in Kolkata and the 15th in UTC, so the
        // same question lands on a different calendar day for each.
        assertEquals(LocalDateKey.of(2026, 1, 16), kolkata.today())
        assertEquals(LocalDateKey.of(2026, 1, 15), utc.today())

        kolkata.record(answer("q1"))
        val state = kolkata.current()
        assertEquals(LocalDateKey.of(2026, 1, 16), state.activityDate)
    }

    @Test
    fun `the same activity recorded in two zones lands on two calendar days`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()
        clock.epochMillis = startOfLocalDay(2026, 1, 16, zone)

        val kolkata = GamificationRepository(store, baseline, { clock.epochMillis }, { zone })
        val utcRepo =
            GamificationRepository(store, baseline, { clock.epochMillis }, { TimeZone.getTimeZone("UTC") })

        kolkata.record(answer("q1"))
        utcRepo.record(answer("q1"))

        val state = kolkata.current()
        assertEquals(LocalDateKey.of(2026, 1, 16), state.activityDate)
        assertEquals(1, state.todayQuestionCount)
        // One question, counted once, on the day Kolkata saw it.
        assertEquals(1, state.uniqueQuestionCount)
    }

    // ---- Corruption and missing data ---------------------------------------

    @Test
    fun `missing storage yields a clean zeroed profile`() = runBlocking {
        val state = repo().current()

        assertEquals(0, state.totalXp)
        assertEquals(10, state.dailyQuestionGoal)
        assertEquals(0, state.uniqueQuestionCount)
        assertTrue(state.unlockedAchievements.isEmpty())
    }

    @Test
    fun `corrupted storage yields a clean profile rather than throwing`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()
        val repository = repo(store, baseline)

        repository.record(answer("q1"))
        store.corruptWith("total_xp=9999\ngarbage-without-separator\nledger=lesson_completed:x")

        val state = repository.current()
        assertEquals(0, state.totalXp)
        assertEquals(10, state.dailyQuestionGoal)
    }

    @Test
    fun `a hand-edited XP total is ignored in favour of the ledger`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()
        val repository = repo(store, baseline)
        repository.record(answer("q1"))

        // Rewrite the stored total to something the ledger does not support.
        val raw = store.read()!!.replace("total_xp=5", "total_xp=5000")

        assertEquals(5, GamificationCodec.decode(raw).totalXp)
    }

    @Test
    fun `clearing removes the profile entirely`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()
        val repository = repo(store, baseline)
        repository.record(answer("q1"))
        repository.seedPreExistingActivity(setOf("l1"), setOf("t1"))

        repository.clearAll()

        val state = repository.current()
        assertEquals(0, state.totalXp)
        assertTrue(state.rewardedActivityIds.isEmpty())
        assertFalse(runBlocking { baseline.isSeeded() })
    }

    @Test
    fun `an achievement id that no longer exists is dropped on read`() = runBlocking {
        val store = InMemoryGamificationStore()
        val raw = GamificationCodec.encode(
            GamificationState().copy(
                totalXp = 20,
                unlockedAchievements = mapOf(
                    AchievementId("retired_achievement") to 1L,
                    AchievementId("first_steps") to 2L
                )
            )
        )
        store.corruptWith(raw)

        val state = repo(store).current()

        assertEquals(setOf(AchievementId("first_steps")), state.unlockedAchievements.keys)
    }
}

/**
 * Replaces the value of a whole `key=value` line.
 *
 * Replaces the entire value rather than prefixing it, so a corruption test
 * cannot accidentally leave the original entries in place.
 */
internal fun withField(raw: String, key: String, value: String): String =
    raw.lines()
        .joinToString("\n") { line ->
            if (line.startsWith("$key=")) "$key=$value" else line
        }

class GamificationCodecTest {

    private val today = LocalDateKey.of(2026, 1, 15)

    private fun populated() = GamificationState(
        totalXp = 35,
        dailyQuestionGoal = 20,
        rewardedActivityIds = setOf(
            ActivityId.lessonCompletedOf("what_is_a_percentage").value,
            ActivityId.questionFirstCorrectOf("q1").value,
            ActivityId.mockTestCompletedOf("t1").value
        ),
        distinctAnsweredQuestionIds = setOf(QuestionId("q1"), QuestionId("q2")),
        completedLessonIds = setOf("what_is_a_percentage"),
        completedMockTestIds = setOf("t1"),
        perfectPracticeTopics = setOf("percentage_of_a_number"),
        qualifyingDates = setOf(today, today.minusOneDay),
        goalReachedDates = setOf(today),
        activityDate = today,
        todayAnsweredQuestionIds = setOf(QuestionId("q1"), QuestionId("q2")),
        unlockedAchievements = mapOf(AchievementId("first_steps") to 5_000L)
    )

    @Test
    fun `a full profile round trips exactly`() {
        val original = populated()

        val restored = GamificationCodec.decode(GamificationCodec.encode(original))

        assertEquals(original, restored)
    }

    @Test
    fun `total XP is recomputed from the ledger on decode`() {
        val tampered = populated().copy(totalXp = 999)

        assertEquals(35, GamificationCodec.decode(GamificationCodec.encode(tampered)).totalXp)
    }

    @Test
    fun `null blank and empty input decode to a default profile`() {
        listOf(null, "", "   ", "\n\n").forEach {
            assertEquals(GamificationState(), GamificationCodec.decode(it))
        }
    }

    @Test
    fun `an unknown version is rejected rather than guessed at`() {
        val raw = GamificationCodec.encode(populated()).replaceFirst("version=1", "version=99")

        assertEquals(GamificationState(), GamificationCodec.decode(raw))
    }

    @Test
    fun `a truncated file decodes to a default profile`() {
        val raw = GamificationCodec.encode(populated()).substringBefore('\n')

        assertEquals(GamificationState(), GamificationCodec.decode(raw))
    }

    @Test
    fun `a damaged ledger entry is dropped and the rest survive`() {
        val raw = withField(
            GamificationCodec.encode(populated()),
            key = "ledger",
            value = "not_a_real_kind:x,lesson_completed:keep_me,question_first_correct:"
        )

        val restored = GamificationCodec.decode(raw)

        assertEquals(setOf("lesson_completed:keep_me"), restored.rewardedActivityIds)
        assertEquals(20, restored.totalXp)
    }

    @Test
    fun `a ledger entry with no subject is dropped`() {
        val raw = withField(
            GamificationCodec.encode(populated()),
            key = "ledger",
            value = "lesson_completed:,mock_test_completed:t1"
        )

        assertFalse("lesson_completed:" in GamificationCodec.decode(raw).rewardedActivityIds)
    }

    @Test
    fun `a separator inside a value cannot forge an extra entry`() {
        val raw = withField(
            GamificationCodec.encode(populated()),
            key = "answered",
            value = "q1|q2=q9,q2"
        )

        // The forged value is dropped rather than becoming two questions.
        assertFalse(QuestionId("q9") in GamificationCodec.decode(raw).distinctAnsweredQuestionIds)
    }

    @Test
    fun `a lesson id the app no longer knows is dropped and the rest survive`() {
        val raw = GamificationCodec.encode(populated())
            .replace("lessons=", "lessons=a_lesson_that_no_longer_exists,")

        assertEquals(
            setOf("what_is_a_percentage"),
            GamificationCodec.decode(raw).completedLessonIds
        )
    }

    @Test
    fun `a non-numeric date is dropped without losing the rest`() {
        val raw = withField(
            GamificationCodec.encode(populated()),
            key = "qualifying_dates",
            value = "not-a-number,still-not-a-number"
        )

        val restored = GamificationCodec.decode(raw)

        assertTrue(restored.qualifyingDates.isEmpty())
        // Unrelated fields survive a damaged one.
        assertEquals(today, restored.activityDate)
        assertEquals(2, restored.uniqueQuestionCount)
    }

    @Test
    fun `a negative unlock timestamp is dropped`() {
        val raw = GamificationCodec.encode(populated()).replace("first_steps=5000", "first_steps=-1")


        assertTrue(GamificationCodec.decode(raw).unlockedAchievements.isEmpty())
    }

    @Test
    fun `a negative stored goal is clamped to zero`() {
        val raw = withField(GamificationCodec.encode(populated()), key = "daily_goal", value = "-5")

        assertEquals(0, GamificationCodec.decode(raw).dailyQuestionGoal)
    }
}

class LocalDateKeyTest {

    @Test
    fun `the epoch day itself round trips`() {
        assertEquals(0, LocalDates.toEpochDay(1970, 1, 1))
        assertEquals("1970-01-01", LocalDates.toIso(LocalDateKey(0)))
    }

    @Test
    fun `known dates convert correctly`() {
        val cases = listOf(
            intArrayOf(1970, 1, 2, 1),
            intArrayOf(2000, 3, 1, 11017),
            intArrayOf(2026, 1, 15, 20468),
            intArrayOf(2024, 2, 29, 19782)
        )
        cases.forEach { (year, month, day, epochDay) ->
            assertEquals("y$year m$month d$day", epochDay, LocalDates.toEpochDay(year, month, day))
            assertEquals(
                "%04d-%02d-%02d".format(year, month, day),
                LocalDates.toIso(LocalDateKey(epochDay))
            )
        }
    }

    @Test
    fun `day arithmetic crosses month and year boundaries`() {
        val endOfYear = LocalDateKey.of(2025, 12, 31)

        assertEquals(LocalDateKey.of(2026, 1, 1), endOfYear.plusOneDay)
        assertEquals(LocalDateKey.of(2025, 12, 30), endOfYear.minusOneDay)
        assertEquals(1, endOfYear.daysUntil(LocalDateKey.of(2026, 1, 1)))
    }

    @Test
    fun `an instant maps to the local day in the given zone`() {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        // Local midnight on 16 Jan in Kolkata is 18:30 on 15 Jan UTC, so the
        // same instant is a different calendar day in each zone.
        val instant = startOfLocalDay(2026, 1, 16, ist)

        assertEquals(
            LocalDateKey.of(2026, 1, 16),
            LocalDates.fromEpochMillis(instant, ist)
        )
        assertEquals(
            LocalDateKey.of(2026, 1, 15),
            LocalDates.fromEpochMillis(instant, TimeZone.getTimeZone("UTC"))
        )
    }

    @Test
    fun `an instant just before local midnight stays on the previous day`() {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val justBefore = startOfLocalDay(2026, 1, 16, ist) - 60_000L

        assertEquals(LocalDateKey.of(2026, 1, 15), LocalDates.fromEpochMillis(justBefore, ist))
    }
}

/** Epoch millis of local midnight starting the given date in [zone]. */
private fun startOfLocalDay(year: Int, month: Int, day: Int, zone: TimeZone): Long =
    java.util.Calendar.getInstance(zone).apply {
        clear()
        set(year, month - 1, day, 0, 0, 0)
    }.timeInMillis
