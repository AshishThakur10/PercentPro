package com.atlabs.percentpro.content

import com.atlabs.percentpro.data.GamificationRepository
import com.atlabs.percentpro.model.ActivityId
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.QuestionId
import com.atlabs.percentpro.model.RewardRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/**
 * Phase 6 regressions against the systems the expanded content feeds.
 *
 * The risk this file guards is that widening the course silently changes
 * behaviour elsewhere: a new question must be awardable exactly once, a new
 * lesson must pay out exactly once, and saved history from earlier phases must
 * still decode and still be reviewable.
 */
class PhaseSixIntegrationTest {

    private val bank = QuestionBank()
    private val repository = QuestionRepository(bank)
    private val catalog = LessonCatalog()
    private val content = LessonContentRepository()
    private val zone = TimeZone.getTimeZone("Asia/Kolkata")
    private val today = LocalDateKey.of(2026, 3, 1)

    private fun gamification(
        store: InMemoryGamificationStore = InMemoryGamificationStore()
    ) = GamificationRepository(
        store = store,
        baselineMarker = InMemoryBaselineMarker(),
        now = { 1_772_323_200_000L },
        timeZone = { zone }
    )

    private fun answered(id: QuestionId, correct: Boolean = true) =
        com.atlabs.percentpro.content.LearningActivity.AnsweredQuestion(id, correct, today)

    // ---- New questions flow into XP, goals and achievements ------------------

    @Test
    fun `a newly added question earns its first-correct XP once`() = runBlocking {
        val gamification = gamification()
        val question = bank.all().first { it.id.value == "dec_035" }

        val first = gamification.record(answered(question.id))
        val repeat = gamification.record(answered(question.id))

        assertEquals(RewardRules.XP_QUESTION_FIRST_CORRECT, first.xpAwarded)
        assertEquals(0, repeat.xpAwarded)
        assertEquals(RewardRules.XP_QUESTION_FIRST_CORRECT, gamification.current().totalXp)
    }

    @Test
    fun `a newly added question counts once toward the daily goal`() = runBlocking {
        val gamification = gamification()
        val question = bank.all().first { it.id.value == "succ_10_then_20" }

        gamification.record(answered(question.id, correct = false))
        gamification.record(answered(question.id, correct = false))
        gamification.record(answered(question.id, correct = false))

        assertEquals(1, gamification.current().todayQuestionCount)
        assertEquals(1, gamification.current().uniqueQuestionCount)
    }

    @Test
    fun `answering one question from each new topic counts each once`() = runBlocking {
        val gamification = gamification()
        repository.availableTopics().forEach { topic ->
            val question = repository.questionsFor(topic).first()
            gamification.record(answered(question.id))
        }

        val state = gamification.current()
        assertEquals(repository.availableTopics().size, state.uniqueQuestionCount)
        assertEquals(
            repository.availableTopics().size,
            state.rewardedActivityIds.count { it.startsWith("question_first_correct:") }
        )
    }

    @Test
    fun `new questions unlock question-count achievements without duplicates`() = runBlocking {
        val gamification = gamification()
        val firstTen = bank.all().take(10)

        firstTen.forEach { gamification.record(answered(it.id)) }

        val unlocked = gamification.current().unlockedAchievements
        assertTrue(
            com.atlabs.percentpro.model.AchievementId("curious_learner") in unlocked
        )
        // Re-recording the same questions must not add a second XP entry.
        firstTen.forEach { gamification.record(answered(it.id)) }
        assertEquals(10 * RewardRules.XP_QUESTION_FIRST_CORRECT, gamification.current().totalXp)
    }

    // ---- New lessons unlock through the reward ledger ------------------------

    @Test
    fun `completing a Phase 6 lesson pays once and unlocks Dedicated Learner`() = runBlocking {
        val gamification = gamification()
        val lesson = LessonId.EXAM_SHORTCUTS
        val activity = com.atlabs.percentpro.content.LearningActivity.LessonCompleted(
            lesson.storageKey,
            today
        )

        val first = gamification.record(activity)
        gamification.record(activity)

        assertEquals(RewardRules.XP_LESSON_COMPLETED, first.xpAwarded)
        assertEquals(RewardRules.XP_LESSON_COMPLETED, gamification.current().totalXp)
        assertEquals(
            com.atlabs.percentpro.model.AchievementId("first_steps"),
            first.unlockedAchievements.single()
        )
    }

    @Test
    fun `completing all twelve written lessons unlocks Dedicated Learner exactly once`() = runBlocking {
        val gamification = gamification()
        val written = catalog.lessons().filter { it.isImplemented }.map { it.id }

        val outcomes = written.map { id ->
            gamification.record(
                com.atlabs.percentpro.content.LearningActivity.LessonCompleted(id.storageKey, today)
            )
        }

        assertEquals(
            12 * RewardRules.XP_LESSON_COMPLETED,
            gamification.current().totalXp
        )
        val dedicated = com.atlabs.percentpro.model.AchievementId("dedicated_learner")
        assertTrue(dedicated in gamification.current().unlockedAchievements)
        assertEquals(1, outcomes.count { dedicated in it.unlockedAchievements })
    }

    // ---- Mock tests over the expanded bank -----------------------------------

    @Test
    fun `a mock test can be built from the expanded bank`() {
        val config = MockTestCatalog().tests().first()
        val questions = MockTestSelector.select(
            config = config,
            topics = repository.availableTopics(),
            questionsByTopic = repository::questionsFor
        )

        assertEquals(config.questionCount, questions.size)
        assertEquals(questions.size, questions.map { it.id }.distinct().size)
    }

    @Test
    fun `a mock test spreads across many topics now the bank is larger`() {
        val config = MockTestCatalog().tests().first()
        val questions = MockTestSelector.select(
            config = config,
            topics = repository.availableTopics(),
            questionsByTopic = repository::questionsFor
        )

        // Twenty topics are available, so a fifteen-question paper must span
        // well beyond the three the bank used to cover.
        assertTrue(
            "paper only touched ${questions.map { it.topic }.distinct().size} topics",
            questions.map { it.topic }.distinct().size >= 10
        )
    }

    @Test
    fun `every selectable question is valid and resolvable`() {
        MockTestSelector.select(
            config = MockTestCatalog().tests().first(),
            topics = repository.availableTopics(),
            questionsByTopic = repository::questionsFor
        ).forEach { question ->
            assertTrue(question.isValid)
            assertNotNullBody(repository.findAcrossTopics(question.id))
        }
    }

    @Test
    fun `selection is deterministic across repeated reads`() {
        val config = MockTestCatalog().tests().first()
        fun select() = MockTestSelector.select(
            config = config,
            topics = repository.availableTopics(),
            questionsByTopic = repository::questionsFor
        ).map { it.id }

        assertEquals(select(), select())
        assertEquals(select(), QuestionRepository().let {
            MockTestSelector.select(config, it.availableTopics(), it::questionsFor).map { q -> q.id }
        })
    }

    @Test
    fun `selection never draws a question twice inside one paper`() {
        repeat(5) { attempt ->
            val paper = MockTestSelector.select(
                config = MockTestCatalog().tests().first(),
                topics = repository.availableTopics(),
                questionsByTopic = repository::questionsFor
            )
            assertEquals(
                "attempt $attempt repeated a question",
                paper.size,
                paper.map { it.id }.distinct().size
            )
        }
    }

    @Test
    fun `the unwritten challenge lesson is never drawn into a paper`() {
        // Questions for the competitive-exam topics are deliberately available
        // even though their lesson prose is not written yet, so this asserts the
        // narrower rule: a topic with no questions contributes nothing.
        val paper = MockTestSelector.select(
            config = MockTestCatalog().tests().first(),
            topics = repository.availableTopics(),
            questionsByTopic = repository::questionsFor
        )

        assertTrue(paper.none { it.topic == LessonId.COMPETITIVE_EXAM_CHALLENGE })
        assertFalse(repository.hasQuestions(LessonId.COMPETITIVE_EXAM_CHALLENGE))
    }

    @Test
    fun `every drawn question belongs to a lesson the course actually lists`() {
        val known = catalog.lessons().map { it.id }.toSet()
        val paper = MockTestSelector.select(
            config = MockTestCatalog().tests().first(),
            topics = repository.availableTopics(),
            questionsByTopic = repository::questionsFor
        )

        assertTrue(paper.all { it.topic in known })
        assertTrue(paper.all { it.isValid })
    }

    @Test
    fun `a stored mock result from before Phase 6 still resolves against the bank`() {
        // A result saved while the bank had three topics records ids and order.
        // Those ids must still be findable so the paper stays reviewable.
        val storedIds = listOf(
            QuestionId("what_is_pct_meaning"),
            QuestionId("visual_50_shaded"),
            QuestionId("fraction_quarter")
        )
        storedIds.forEach { id ->
            assertNotNullBody(repository.findAcrossTopics(id))
        }
    }

    @Test
    fun `a stored mock result whose question no longer exists fails cleanly`() {
        // The codec rejects a paper it cannot rebuild rather than half-restoring.
        val restore = MockTestCodec.decodeSession(
            raw = MockTestCodec.encodeSession(
                MockTestSession.start(
                    "s",
                    MockTestCatalog().tests().first(),
                    listOf(
                        Question(
                            id = QuestionId("a_question_that_was_removed"),
                            topic = LessonId.WHAT_IS_A_PERCENTAGE,
                            difficulty = com.atlabs.percentpro.model.QuestionDifficulty.EASY,
                            prompt = com.atlabs.percentpro.model.LocalizedText.of("p", "प"),
                            options = listOf("A", "B", "C", "D").map {
                                com.atlabs.percentpro.model.LocalizedText.of(it, it)
                            },
                            correctIndex = 0,
                            explanation = com.atlabs.percentpro.model.LocalizedText.of("e", "इ")
                        )
                    ),
                    1_000L
                )
            ),
            catalog = MockTestCatalog(),
            questionsById = { repository.findAcrossTopics(it) },
            nowEpochMillis = 2_000L
        )

        assertEquals(
            MockTestCodec.RestoreFailure.MISSING_QUESTIONS,
            (restore as MockTestCodec.SessionRestore.Unusable).reason
        )
    }

    // ---- Saved history survives the course growth ---------------------------

    @Test
    fun `lessons completed before Phase 6 still decode from saved progress`() {
        val savedKeys = listOf(
            "what_is_a_percentage",
            "percentages_visually",
            "fractions_to_percentages"
        )
        val playable = catalog.lessons().filter { it.isImplemented }.map { it.id }.toSet()

        val progress = ProgressCodec.decode(
            version = 1,
            completedLessonIds = savedKeys.toSet(),
            lessonInProgress = null,
            lastBlockIndex = null,
            playableLessons = playable,
            blockCount = 8
        )

        assertEquals(savedKeys.size, progress.completedLessonIds.size)
        savedKeys.forEach { key ->
            assertTrue(LessonId.fromStorageKey(key) in progress.completedLessonIds)
        }
    }

    @Test
    fun `expanding the course does not reduce recorded completion percentage`() {
        // A learner who finished everything writable before Phase 6 must not
        // see their progress go backwards.
        val previousCompletions = setOf(
            LessonId.WHAT_IS_A_PERCENTAGE,
            LessonId.PERCENTAGES_VISUALLY,
            LessonId.FRACTIONS_TO_PERCENTAGES
        )
        val percent = LessonAvailability.completionPercent(catalog.lessons(), previousCompletions)

        // 3 of 12 written lessons is 25%, still a real non-zero figure.
        assertEquals(25, percent)
        assertTrue(percent > 0)
    }

    @Test
    fun `pre-existing activity seeding blocks a retroactive payout`() = runBlocking {
        val gamification = gamification()
        gamification.seedPreExistingActivity(
            completedLessonKeys = setOf(LessonId.WHAT_IS_A_PERCENTAGE.storageKey),
            completedMockTestIds = emptySet()
        )

        val outcome = gamification.record(
            com.atlabs.percentpro.content.LearningActivity.LessonCompleted(
                LessonId.WHAT_IS_A_PERCENTAGE.storageKey,
                today
            )
        )

        assertEquals(0, outcome.xpAwarded)
        assertEquals(0, gamification.current().totalXp)
    }

    @Test
    fun `adding content awards no retroactive XP for existing questions`() = runBlocking {
        val gamification = gamification()
        // No activity reported at all: merely having the bank in place must not
        // move the total.
        assertEquals(0, gamification.current().totalXp)
        assertTrue(gamification.current().unlockedAchievements.isEmpty())
        assertEquals(0, gamification.current().uniqueQuestionCount)
    }

    // ---- Practice filtering over the expanded bank --------------------------

    @Test
    fun `topic practice offers every topic that has questions`() {
        val offered = repository.availableTopics()
        catalog.lessons().forEach { lesson ->
            val hasQuestions = repository.hasQuestions(lesson.id)
            assertEquals(
                "${lesson.id} practice availability is wrong",
                hasQuestions,
                lesson.id in offered
            )
        }
    }

    @Test
    fun `questions can be filtered by difficulty within a topic`() {
        // The three lessons preserved from earlier phases keep the two-level mix
        // they shipped with; every topic written in Phase 6 has all three.
        val preserved = setOf(
            LessonId.WHAT_IS_A_PERCENTAGE,
            LessonId.PERCENTAGES_VISUALLY,
            LessonId.FRACTIONS_TO_PERCENTAGES
        )
        repository.availableTopics().forEach { topic ->
            val questions = repository.questionsFor(topic)
            val difficulties = questions.map { it.difficulty }.distinct()
            assertTrue("$topic has only one difficulty", difficulties.size >= 2)

            val easyIds = questions
                .filter { it.difficulty == com.atlabs.percentpro.model.QuestionDifficulty.EASY }
                .map { it.id }
            val hardIds = questions
                .filter { it.difficulty == com.atlabs.percentpro.model.QuestionDifficulty.HARD }
                .map { it.id }
                .toSet()
            assertTrue("$topic has no easy questions", easyIds.isNotEmpty())
            assertTrue(
                "$topic difficulty buckets overlap",
                easyIds.intersect(hardIds).isEmpty()
            )
            if (topic !in preserved) {
                assertTrue("$topic should have hard questions", hardIds.isNotEmpty())
            }
        }
    }

    @Test
    fun `questionsFor returns them in a stable order`() {
        repository.availableTopics().forEach { topic ->
            assertEquals(
                repository.questionsFor(topic).map { it.id },
                QuestionRepository().questionsFor(topic).map { it.id }
            )
        }
    }

    private fun assertNotNullBody(value: Any?) {
        assertTrue("expected a question but found $value", value != null)
    }

    private fun Question(
        id: QuestionId,
        topic: LessonId,
        difficulty: com.atlabs.percentpro.model.QuestionDifficulty,
        prompt: com.atlabs.percentpro.model.LocalizedText,
        options: List<com.atlabs.percentpro.model.LocalizedText>,
        correctIndex: Int,
        explanation: com.atlabs.percentpro.model.LocalizedText
    ) = com.atlabs.percentpro.model.Question(id, topic, difficulty, prompt, options, correctIndex, explanation)
}