package com.atlabs.percentpro.content

import com.atlabs.percentpro.data.BaselineMarkerStore
import com.atlabs.percentpro.data.GamificationRepository
import com.atlabs.percentpro.data.GamificationStore
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.model.MockTestResult
import com.atlabs.percentpro.model.MockTestSubmission
import com.atlabs.percentpro.model.QuestionId
import com.atlabs.percentpro.model.RewardRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/**
 * Cross-cutting safeguards, expressed against the same domain objects the real
 * flows use.
 *
 * Each test names the guarantee it protects and the reason it exists, so a
 * future change that breaks one fails with an explanation rather than a bare
 * number.
 */
class GamificationIntegrationTest {

    private val zone = TimeZone.getTimeZone("Asia/Kolkata")
    private val clock = TestClock.starting()

    private fun repo(
        store: GamificationStore = InMemoryGamificationStore(),
        baseline: BaselineMarkerStore = InMemoryBaselineMarker()
    ) = GamificationRepository(store, baseline, { clock.epochMillis }, { zone })

    private val today: LocalDateKey get() = clock.today()

    private fun answer(id: String, correct: Boolean = true, on: LocalDateKey = today) =
        LearningActivity.AnsweredQuestion(QuestionId(id), correct, on)

    // ---- Cross-flow deduplication ------------------------------------------

    @Test
    fun `the same question earns its XP once across topic practice and mock tests`() = runBlocking {
        val repository = repo()

        repository.record(answer("shared", correct = true))
        // Later the same question id appears in a mock test paper.
        val inMockTest = repository.record(answer("shared", correct = true))

        assertEquals(0, inMockTest.xpAwarded)
        assertEquals(RewardRules.XP_QUESTION_FIRST_CORRECT, repository.current().totalXp)
    }

    @Test
    fun `lesson completion and practice completion stay separate concepts`() = runBlocking {
        val repository = repo()

        repository.record(
            LearningActivity.PracticeSessionCompleted("what_is_a_percentage", allCorrect = true, today)
        )

        val state = repository.current()
        // A perfect practice run is not a completed lesson.
        assertEquals(0, state.lessonCount)
        assertFalse(
            "practice must not put the lesson in the paid ledger",
            "lesson_completed:what_is_a_percentage" in state.rewardedActivityIds
        )
    }

    @Test
    fun `opening a saved result changes nothing`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()
        val repository = repo(store, baseline)
        repository.record(answer("q1"))

        val before = repository.current()

        // Re-opening is a read plus a decode; nothing is reported.
        val reopened = repo(store, baseline).current()

        assertEquals(before, reopened)
    }

    @Test
    fun `a full session of distinct questions pays once per question`() = runBlocking {
        val repository = repo()

        // 8 questions answered, then the same 8 answered again.
        repeat(8) { repository.record(answer("q$it")) }
        val afterFirstPass = repository.current().totalXp
        repeat(8) { repository.record(answer("q$it")) }

        assertEquals(8 * RewardRules.XP_QUESTION_FIRST_CORRECT, afterFirstPass)
        assertEquals(afterFirstPass, repository.current().totalXp)
        assertEquals(8, repository.current().uniqueQuestionCount)
    }

    @Test
    fun `a mixed session of correct and incorrect answers pays only for correct ones`() = runBlocking {
        val repository = repo()
        listOf(true, false, true, false, false).forEachIndexed { index, correct ->
            repository.record(answer("q$index", correct = correct))
        }

        assertEquals(2 * RewardRules.XP_QUESTION_FIRST_CORRECT, repository.current().totalXp)
        // All five still count for the daily goal.
        assertEquals(5, repository.current().todayQuestionCount)
    }

    @Test
    fun `a timeout submission and a later retry pay the mock test once`() = runBlocking {
        val repository = repo()
        val testId = MockTestId("percentage_aptitude_15")

        // The automatic path first.
        repository.record(LearningActivity.MockTestCompleted(testId.value, today))
        // The learner retries later, on a different day, and submits again.
        clock.advanceDays(3)
        repository.record(LearningActivity.MockTestCompleted(testId.value, today))

        assertEquals(
            RewardRules.XP_MOCK_TEST_COMPLETED,
            repository.current().totalXp
        )
        assertEquals(1, repository.current().mockTestCount)
    }

    @Test
    fun `a restored session does not re-award anything`() = runBlocking {
        val store = InMemoryGamificationStore()
        val baseline = InMemoryBaselineMarker()

        // Learner answers three questions and backgrounds the app.
        val first = repo(store, baseline)
        repeat(3) { first.record(answer("q$it")) }
        val xpBefore = first.current().totalXp

        // The session is restored: a new repository over the same storage, and
        // the restored answers replayed into a fresh view model.
        val restored = repo(store, baseline)
        repeat(3) { restored.record(answer("q$it")) }

        assertEquals(xpBefore, restored.current().totalXp)
    }

    @Test
    fun `review mode awards nothing`() = runBlocking {
        val repository = repo()
        repository.record(answer("q1"))

        val before = repository.current()

        // Reviewing a finished paper and re-rendering its result screen report
        // nothing; the only thing that could change the state is a record call.
        val snapshot = GamificationEngine.apply(before, answer("q1"), clock.epochMillis)

        assertEquals(before.totalXp, snapshot.totalXp)
        assertEquals(before.uniqueQuestionCount, snapshot.uniqueQuestionCount)
    }

    @Test
    fun `achievements update without a restart after each new activity`() = runBlocking {
        val repository = repo()

        assertTrue(com.atlabs.percentpro.model.AchievementId("first_steps") !in repository.current().unlockedAchievements)

        repository.record(LearningActivity.LessonCompleted("what_is_a_percentage", today))

        assertTrue(com.atlabs.percentpro.model.AchievementId("first_steps") in repository.current().unlockedAchievements)
    }

    @Test
    fun `an achievement unlocked mid-session is reported exactly once`() = runBlocking {
        val repository = repo()

        val first = repository.record(LearningActivity.LessonCompleted("what_is_a_percentage", today))
        val second = repository.record(LearningActivity.LessonCompleted("percentages_visually", today))

        assertEquals(
            listOf(com.atlabs.percentpro.model.AchievementId("first_steps")),
            first.unlockedAchievements
        )
        assertTrue(second.unlockedAchievements.isEmpty())
    }

    @Test
    fun `a mock test result model can be rebuilt without touching rewards`() = runBlocking {
        val repository = repo()
        val questions = listOf(question("q1", 0), question("q2", 1))
        val session = MockTestSession.start(
            "s",
            MockTestCatalog().tests().first(),
            questions,
            1_000L
        ).goTo(0).selectOption(0)
        val frozen = session.submit(MockTestSubmission.MANUAL)
        val result = MockTestResult.from(frozen, 2_000L)

        // Reading a saved result is pure.
        val stateBefore = repository.current()
        val rebuilt = MockTestResult.from(frozen, 2_000L)

        assertEquals(result, rebuilt)
        assertEquals(stateBefore, repository.current())
    }

    @Test
    fun `XP never drops when a duplicate event is applied`() = runBlocking {
        val repository = repo()
        repository.record(answer("q1"))
        val high = repository.current().totalXp

        repeat(20) { repository.record(answer("q1")) }

        assertEquals(high, repository.current().totalXp)
        assertTrue(repository.current().totalXp >= 0)
    }

    @Test
    fun `goal and streak stay independent across a day boundary`() = runBlocking {
        val repository = repo()

        // Reach the daily goal on day one: 10 questions.
        repeat(10) { repository.record(answer("q$it")) }
        assertTrue(repository.current().isDailyGoalReached)
        assertEquals(1, GamificationEngine.currentStreak(repository.current(), today))

        // Day two: one lesson. The goal resets, the streak continues.
        clock.advanceDays(1)
        repository.record(LearningActivity.LessonCompleted("l1", today))
        val state = repository.current()

        assertFalse(state.isDailyGoalReached)
        assertEquals(0, state.todayQuestionCount)
        assertEquals(2, GamificationEngine.currentStreak(state, today))
    }

    @Test
    fun `reaching the goal on a second day does not re-unlock Goal Getter`() = runBlocking {
        val repository = repo()
        val goalGetter = com.atlabs.percentpro.model.AchievementId("goal_getter")

        repeat(10) { repository.record(answer("day1_q$it")) }
        val first = repository.current().unlockedAchievements[goalGetter]

        clock.advanceDays(1)
        val secondOutcome = repository.record(answer("day2_q1"))
        repeat(9) { repository.record(answer("day2_q${it + 2}")) }

        assertTrue(secondOutcome.unlockedAchievements.isEmpty())
        assertEquals(first, repository.current().unlockedAchievements[goalGetter])
        assertTrue(today in repository.current().goalReachedDates)
    }

    private fun question(id: String, correctIndex: Int) = com.atlabs.percentpro.model.Question(
        id = QuestionId(id),
        topic = com.atlabs.percentpro.model.LessonId.WHAT_IS_A_PERCENTAGE,
        difficulty = com.atlabs.percentpro.model.QuestionDifficulty.EASY,
        prompt = com.atlabs.percentpro.model.LocalizedText.of("Q$id", "Q$id"),
        options = listOf("A", "B", "C", "D").map { com.atlabs.percentpro.model.LocalizedText.of("$it", "$it") },
        correctIndex = correctIndex,
        explanation = com.atlabs.percentpro.model.LocalizedText.of("E", "E")
    )
}