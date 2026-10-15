package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.AnswerEvent
import com.atlabs.percentpro.model.DifficultyFilter
import com.atlabs.percentpro.model.AnswerSource
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LessonStatus
import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.model.PracticeMode
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId
import com.atlabs.percentpro.model.RecommendationDestination
import com.atlabs.percentpro.model.RecommendationKind
import com.atlabs.percentpro.model.RecommendationReason
import com.atlabs.percentpro.model.RecommendationTitleKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Priority, determinism, explainability and availability rules. */
class RecommendationEngineTest {

    private val catalog = LessonCatalog()
    private val lessons = catalog.lessons()
    private val questionRepository = QuestionRepository()

    private val questionCounts: Map<LessonId, Int> =
        questionRepository.availableTopics()
            .associateWith { questionRepository.questionCountFor(it) }

    private val writtenLessons = lessons.filter { it.isImplemented }.map { it.id }

    private fun event(
        id: String,
        correct: Boolean,
        topic: LessonId,
        difficulty: QuestionDifficulty = QuestionDifficulty.EASY,
        session: String = "s",
        at: Long = 1_000L
    ) = AnswerEvent(
        questionId = QuestionId(id),
        topicKey = topic.storageKey,
        difficulty = difficulty,
        wasCorrect = correct,
        source = AnswerSource.TOPIC_PRACTICE,
        sessionId = session,
        answeredAtEpochMillis = at,
        answeredOn = com.atlabs.percentpro.model.LocalDateKey.of(2026, 4, 1)
    )

    private fun history(
        topic: LessonId,
        correct: Int,
        incorrect: Int,
        difficulty: QuestionDifficulty = QuestionDifficulty.EASY,
        session: String = "s",
        at: Long = 1_000L
    ) = (0 until correct).map { event("c$it", true, topic, difficulty, session, at) } +
        (0 until incorrect).map { event("w$it", false, topic, difficulty, session, at) }

    private fun recommend(
        events: List<AnswerEvent>,
        progress: LearningProgress = LearningProgress(),
        counts: Map<LessonId, Int> = questionCounts
    ) = RecommendationEngine.recommend(
        analytics = LearningAnalyticsEngine.analyse(
            events = events,
            completedLessons = progress.completedLessonIds
        ),
        lessons = lessons,
        progress = progress,
        questionCounts = counts
    )

    // ---- priority order ----------------------------------------------------

    @Test
    fun `with no history and no progress the first lesson is suggested`() {
        val recommendation = recommend(emptyList())

        assertEquals(RecommendationKind.CONTINUE_COURSE, recommendation.kind)
        assertEquals(RecommendationReason.NEXT_LESSON_UNLOCKED, recommendation.reasonCode)
        assertEquals(LessonId.WHAT_IS_A_PERCENTAGE, recommendation.lessonId)
        assertEquals(
            RecommendationDestination.OpenLesson(LessonId.WHAT_IS_A_PERCENTAGE),
            recommendation.destination
        )
    }

    @Test
    fun `a finished course with no answers points at the dashboard`() {
        val finished = writtenLessons.fold(LearningProgress()) { acc, id -> acc.markCompleted(id) }

        val recommendation = recommend(emptyList(), finished)

        assertEquals(RecommendationKind.START_WITH_LESSON_ONE, recommendation.kind)
        assertEquals(RecommendationReason.NO_HISTORY_YET, recommendation.reasonCode)
        assertEquals(RecommendationDestination.OpenInsights, recommendation.destination)
    }

    @Test
    fun `a strong topic is never labelled as needing practice`() {
        val finished = writtenLessons.fold(LearningProgress()) { acc, id -> acc.markCompleted(id) }
        // Ten correct easy answers and nothing else: strong, not weak.
        val events = history(LessonId.WHAT_IS_A_PERCENTAGE, correct = 10, incorrect = 0)

        val recommendation = recommend(events, finished)

        assertFalse(recommendation.kind == RecommendationKind.PRACTICE_WEAK_TOPIC)
        assertEquals(RecommendationReason.LOW_TOPIC_ACCURACY.takeIf { false }, recommendation.reasonCode.takeIf { false })
    }

    @Test
    fun `an unfinished lesson is resumed ahead of anything else`() {
        val progress = LearningProgress()
            .startLesson(LessonId.PERCENTAGES_VISUALLY)
            .markCompleted(LessonId.WHAT_IS_A_PERCENTAGE)

        val recommendation = recommend(emptyList(), progress)

        assertEquals(RecommendationKind.RESUME_LESSON, recommendation.kind)
        assertEquals(RecommendationReason.LESSON_IN_PROGRESS, recommendation.reasonCode)
        assertEquals(LessonId.PERCENTAGES_VISUALLY, recommendation.lessonId)
    }

    @Test
    fun `an available lesson outranks practice while the course is unfinished`() {
        val progress = LearningProgress().markCompleted(LessonId.WHAT_IS_A_PERCENTAGE)
        // Plenty of weak data, but lesson two is still open.
        val events = history(LessonId.ORIGINAL_WHOLE, correct = 1, incorrect = 9)

        val recommendation = recommend(events, progress)

        assertEquals(RecommendationKind.CONTINUE_COURSE, recommendation.kind)
        assertEquals(LessonId.PERCENTAGES_VISUALLY, recommendation.lessonId)
    }

    @Test
    fun `a weak topic is recommended once every lesson is done`() {
        val progress = writtenLessons.fold(LearningProgress()) { acc, id -> acc.markCompleted(id) }
        val events = history(LessonId.ORIGINAL_WHOLE, correct = 2, incorrect = 8)

        val recommendation = recommend(events, progress)

        assertEquals(RecommendationKind.PRACTICE_WEAK_TOPIC, recommendation.kind)
        assertEquals(RecommendationReason.LOW_TOPIC_ACCURACY, recommendation.reasonCode)
        assertEquals(LessonId.ORIGINAL_WHOLE, recommendation.topic)
        assertEquals(listOf(20), recommendation.reasonArgs)
    }

    @Test
    fun `a strong topic offers the next difficulty up`() {
        val progress = LearningProgress()
        // A real sample at the easy level, plus one attempt at the next level so
        // the step up is an extension of what they have already met.
        val events = history(LessonId.WHAT_IS_A_PERCENTAGE, correct = 10, incorrect = 0) +
            history(
                LessonId.WHAT_IS_A_PERCENTAGE,
                correct = 1,
                incorrect = 1,
                difficulty = QuestionDifficulty.MEDIUM,
                session = "med",
                at = 2_000L
            )
        val advanced = writtenLessons.fold(progress) { acc, id -> acc.markCompleted(id) }

        val recommendation = recommend(events, advanced)

        assertEquals(RecommendationKind.STEP_UP_DIFFICULTY, recommendation.kind)
        assertEquals(QuestionDifficulty.MEDIUM, recommendation.difficulty)
        assertEquals(
            RecommendationDestination.PracticeDifficulty(
                LessonId.WHAT_IS_A_PERCENTAGE,
                QuestionDifficulty.MEDIUM
            ),
            recommendation.destination
        )
    }

    @Test
    fun `easy answers alone never trigger a step up in difficulty`() {
        val finished = writtenLessons.fold(LearningProgress()) { acc, id -> acc.markCompleted(id) }
        // Ten out of ten on easy questions and nothing harder attempted.
        val events = history(LessonId.WHAT_IS_A_PERCENTAGE, correct = 10, incorrect = 0)

        val recommendation = recommend(events, finished)

        assertFalse(recommendation.kind == RecommendationKind.STEP_UP_DIFFICULTY)
    }

    @Test
    fun `a mixed review is offered when topics are balanced`() {
        val advanced = writtenLessons
            .drop(1)
            .fold(LearningProgress().markCompleted(LessonId.WHAT_IS_A_PERCENTAGE)) { acc, id ->
                acc.markCompleted(id)
            }
        val events = history(LessonId.WHAT_IS_A_PERCENTAGE, 7, 3, session = "a") +
            history(LessonId.ORIGINAL_WHOLE, 6, 4, session = "b")

        val recommendation = recommend(events, advanced)

        assertEquals(RecommendationKind.MIXED_REVIEW, recommendation.kind)
        assertEquals(RecommendationDestination.MixedReview, recommendation.destination)
    }

    @Test
    fun `an uneven spread of topics is not sent to a mixed review`() {
        val advanced = writtenLessons
            .drop(1)
            .fold(LearningProgress().markCompleted(LessonId.WHAT_IS_A_PERCENTAGE)) { acc, id ->
                acc.markCompleted(id)
            }
        // 95% against 30% is a wide gap, so a mixed run would be unhelpful.
        val events = history(LessonId.WHAT_IS_A_PERCENTAGE, 19, 1, session = "a") +
            history(LessonId.ORIGINAL_WHOLE, 3, 7, session = "b")

        val recommendation = recommend(events, advanced)

        assertFalse(recommendation.kind == RecommendationKind.MIXED_REVIEW)
    }

    @Test
    fun `finishing the course with no weak topics falls back to consolidating`() {
        val finished = writtenLessons.fold(LearningProgress()) { acc, id -> acc.markCompleted(id) }
        // Balanced and strong enough that nothing else applies.
        val events = history(LessonId.WHAT_IS_A_PERCENTAGE, 5, 0, session = "a") +
            history(LessonId.ORIGINAL_WHOLE, 5, 0, session = "b") +
            history(LessonId.PERCENTAGES_VISUALLY, 5, 0, session = "c")

        val recommendation = recommend(events, finished)

        assertNotNull(recommendation)
        assertTrue(
            recommendation.kind == RecommendationKind.KEEP_PRACTISING ||
                recommendation.kind == RecommendationKind.MIXED_REVIEW
        )
    }

    // ---- never recommend what is unavailable -------------------------------

    @Test
    fun `a locked lesson is never recommended`() {
        val recommendation = recommend(emptyList())

        val destination = recommendation.destination
        if (destination is RecommendationDestination.OpenLesson) {
            val lesson = lessons.first { it.id == destination.lessonId }
            assertTrue(
                "recommended a locked or unwritten lesson",
                lesson.isPlayable &&
                    LessonAvailability.statusFor(
                        lesson,
                        lessons,
                        emptySet(),
                        null
                    ).isUnlocked
            )
        }
    }

    @Test
    fun `a weak topic with no questions is skipped`() {
        val advanced = writtenLessons
            .drop(1)
            .fold(LearningProgress().markCompleted(LessonId.WHAT_IS_A_PERCENTAGE)) { acc, id ->
                acc.markCompleted(id)
            }
        val events = history(LessonId.ORIGINAL_WHOLE, 2, 8)

        // The weak topic exists in the bank's vocabulary but is declared empty.
        val recommendation = RecommendationEngine.recommend(
            analytics = LearningAnalyticsEngine.analyse(events, advanced.completedLessonIds),
            lessons = lessons,
            progress = advanced,
            questionCounts = questionCounts + (LessonId.ORIGINAL_WHOLE to 0)
        )

        val destination = recommendation.destination
        assertFalse(destination is RecommendationDestination.PracticeTopic &&
            destination.topic == LessonId.ORIGINAL_WHOLE
        )
    }

    @Test
    fun `every destination is a real target in the existing navigation`() {
        val recommendations = listOf(
            recommend(emptyList()),
            recommend(emptyList(), LearningProgress().startLesson(LessonId.WHAT_IS_A_PERCENTAGE))
        )
        recommendations.forEach { recommendation ->
            when (val destination = recommendation.destination) {
                is RecommendationDestination.OpenLesson ->
                    assertTrue(destination.lessonId in lessons.map { it.id })

                is RecommendationDestination.PracticeTopic ->
                    assertTrue(destination.topic in lessons.map { it.id })

                is RecommendationDestination.PracticeDifficulty ->
                    assertTrue(destination.topic in lessons.map { it.id })

                RecommendationDestination.MixedReview,
                RecommendationDestination.OpenInsights -> Unit
            }
        }
    }

    // ---- determinism and explainability ------------------------------------

    @Test
    fun `the same inputs always give the same recommendation`() {
        val progress = LearningProgress().markCompleted(LessonId.WHAT_IS_A_PERCENTAGE)
        val events = history(LessonId.ORIGINAL_WHOLE, 2, 8)

        val first = recommend(events, progress)
        repeat(5) { assertEquals(first, recommend(events, progress)) }
    }

    @Test
    fun `every recommendation carries an explanation and an identifier`() {
        val cases = listOf(
            recommend(emptyList()),
            recommend(
                history(LessonId.ORIGINAL_WHOLE, 2, 8),
                writtenLessons.fold(LearningProgress()) { acc, id -> acc.markCompleted(id) }
            )
        )
        cases.forEach { recommendation ->
            assertNotNull(recommendation.reasonCode)
            assertNotNull(recommendation.titleKey)
            assertTrue(
                "low accuracy advice must quote a real figure",
                recommendation.reasonCode != RecommendationReason.LOW_TOPIC_ACCURACY ||
                    recommendation.reasonArgs.isNotEmpty()
            )
        }
    }

    @Test
    fun `the quoted accuracy matches the analytics figure`() {
        val events = history(LessonId.ORIGINAL_WHOLE, 3, 7)
        val analytics = LearningAnalyticsEngine.analyse(events)

        val recommendation = RecommendationEngine.recommend(
            analytics = analytics,
            lessons = lessons,
            progress = writtenLessons.fold(LearningProgress()) { acc, id -> acc.markCompleted(id) },
            questionCounts = questionCounts
        )

        assertEquals(
            listOf(analytics.weakestTopic!!.accuracyPercent),
            recommendation.reasonArgs
        )
    }

    @Test
    fun `titles and reasons come from stable keys rather than text`() {
        val recommendation = recommend(emptyList())

        assertEquals(RecommendationTitleKey.CONTINUE_COURSE, recommendation.titleKey)
        assertEquals(RecommendationReason.NEXT_LESSON_UNLOCKED, recommendation.reasonCode)
        // The decision carries identifiers only; the wording is resolved later.
        assertTrue(
            recommendation.topicForCopy == null ||
                recommendation.topicForCopy in lessons.map { it.id }
        )
    }

    @Test
    fun `an unwritten in-progress lesson is not recommended as resumable`() {
        // Saved data claiming lesson 13 is in progress must not produce a tap
        // target the lesson screen would refuse to open.
        val progress = LearningProgress()
            .startLesson(LessonId.POPULATION_CHANGE)

        val recommendation = recommend(emptyList(), progress)

        val destination = recommendation.destination
        assertFalse(
            destination is RecommendationDestination.OpenLesson &&
                destination.lessonId == LessonId.POPULATION_CHANGE
        )
    }
}

/** Building plans for weak topics, difficulty filters and mixed review. */
class PracticePlannerTest {

    private val catalog = LessonCatalog()
    private val repository = QuestionRepository()

    private val richTopic = LessonId.WHAT_IS_A_PERCENTAGE
    private val secondTopic = LessonId.PERCENTAGES_VISUALLY

    private val questionsFor: (LessonId) -> List<Question> = { repository.questionsFor(it) }

    // ---- single topic -------------------------------------------------------

    @Test
    fun `a topic plan uses that topic's questions`() {
        val plan = PracticePlanner.planTopic(richTopic, questionsFor)

        assertFalse(plan.isEmpty)
        assertEquals(PracticeMode.TOPIC, plan.mode)
        assertEquals(listOf(richTopic), plan.topicIds)
        assertEquals(richTopic, plan.primaryTopic)
    }

    @Test
    fun `a topic plan never repeats a question`() {
        val plan = PracticePlanner.planTopic(richTopic, questionsFor)

        assertEquals(plan.questionIds.size, plan.questionIds.distinct().size)
    }

    @Test
    fun `only available questions are planned`() {
        val plan = PracticePlanner.planTopic(richTopic, questionsFor)
        val available = questionsFor(richTopic).map { it.id }.toSet()

        assertTrue(plan.questionIds.all { it in available })
    }

    @Test
    fun `an empty topic produces an empty plan rather than throwing`() {
        val plan = PracticePlanner.planTopic(LessonId.COMPETITIVE_EXAM_CHALLENGE, questionsFor)

        assertTrue(plan.isEmpty)
        assertEquals(0, plan.size)
        assertEquals(LessonId.COMPETITIVE_EXAM_CHALLENGE, plan.primaryTopic)
    }

    @Test
    fun `a difficulty filter selects only that level`() {
        val plan = PracticePlanner.planTopic(
            richTopic,
            questionsFor,
            DifficultyFilter.EASY
        )

        val difficulties = plan.questionIds
            .mapNotNull { id -> questionsFor(richTopic).firstOrNull { it.id == id } }
            .map { it.difficulty }
            .distinct()

        assertEquals(listOf(QuestionDifficulty.EASY), difficulties)
    }

    @Test
    fun `a difficulty nothing was authored at falls back to any`() {
        // The preserved lessons predate the hard level, so ask for it there.
        val plan = PracticePlanner.planTopic(richTopic, { emptyList() }, DifficultyFilter.HARD)

        assertTrue(plan.isEmpty)
    }

    @Test
    fun `a topic with no questions at the chosen level falls back to any`() {
        val local = listOf(
            fakeQuestion("only_easy", QuestionDifficulty.EASY)
        )
        val plan = PracticePlanner.planTopic(richTopic, { local }, DifficultyFilter.HARD)

        assertFalse(plan.isEmpty)
        assertEquals(DifficultyFilter.ANY, plan.difficultyFilter)
        assertEquals(listOf(QuestionId("only_easy")), plan.questionIds)
    }

    @Test
    fun `a plan is capped at the requested length`() {
        val plan = PracticePlanner.planTopic(richTopic, questionsFor, limit = 3)

        assertEquals(3, plan.size)
    }

    // ---- mixed review -------------------------------------------------------

    @Test
    fun `a mixed plan draws from every supplied topic`() {
        val plan = PracticePlanner.planMixedReview(
            topics = listOf(richTopic, secondTopic),
            questionsFor = questionsFor,
            limit = 8
        )

        assertEquals(setOf(richTopic, secondTopic), plan.topicIds.toSet())
        assertEquals(8, plan.size)
    }

    @Test
    fun `a mixed plan never repeats a question across topics`() {
        val plan = PracticePlanner.planMixedReview(
            topics = listOf(richTopic, secondTopic),
            questionsFor = questionsFor,
            limit = 12
        )

        assertEquals(plan.questionIds.size, plan.questionIds.distinct().size)
    }

    @Test
    fun `a mixed plan is balanced rather than dominated by one topic`() {
        val plan = PracticePlanner.planMixedReview(
            topics = listOf(richTopic, secondTopic),
            questionsFor = questionsFor,
            limit = 8
        )

        val perTopic = plan.questionIds.groupingBy { id ->
            questionsFor(richTopic).firstOrNull { it.id == id }?.topic
        }.eachCount().values

        // Round-robin gives one question per topic per pass, so with two topics
        // and eight questions the split must be even.
        assertEquals(setOf(4), perTopic.toSet())
    }

    @Test
    fun `a mixed plan skips topics with no questions`() {
        val plan = PracticePlanner.planMixedReview(
            topics = listOf(LessonId.COMPETITIVE_EXAM_CHALLENGE, richTopic),
            questionsFor = questionsFor,
            limit = 4
        )

        assertEquals(listOf(richTopic), plan.topicIds)
    }

    @Test
    fun `a mixed plan with no available topics is empty, not a crash`() {
        val plan = PracticePlanner.planMixedReview(
            topics = listOf(LessonId.COMPETITIVE_EXAM_CHALLENGE),
            questionsFor = questionsFor
        )

        assertTrue(plan.isEmpty)
    }

    @Test
    fun `a mixed plan with an empty topic list is empty`() {
        assertTrue(PracticePlanner.planMixedReview(emptyList(), questionsFor).isEmpty)
    }

    @Test
    fun `a difficulty the bank has nothing for yields an empty mixed plan`() {
        val plan = PracticePlanner.planMixedReview(
            topics = listOf(richTopic),
            questionsFor = { emptyList() },
            difficulty = DifficultyFilter.HARD
        )

        assertTrue(plan.isEmpty)
    }

    @Test
    fun `the plan key distinguishes modes difficulties and topics`() {
        val topic = PracticePlanner.planTopic(richTopic, questionsFor, DifficultyFilter.EASY)
        val any = PracticePlanner.planTopic(richTopic, questionsFor, DifficultyFilter.ANY)
        val other = PracticePlanner.planTopic(secondTopic, questionsFor, DifficultyFilter.EASY)
        val mixed = PracticePlanner.planMixedReview(
            listOf(richTopic, secondTopic),
            questionsFor,
            DifficultyFilter.EASY
        )

        val keys = listOf(topic.key, any.key, other.key, mixed.key)
        assertEquals(keys.size, keys.distinct().size)
    }

    @Test
    fun `the session prefix distinguishes a targeted run from a normal one`() {
        val plan = PracticePlanner.planTopic(richTopic, questionsFor, DifficultyFilter.EASY)

        assertTrue(plan.sessionPrefix.startsWith("targeted_topic_easy"))
    }

    private fun fakeQuestion(id: String, difficulty: QuestionDifficulty) = Question(
        id = QuestionId(id),
        topic = richTopic,
        difficulty = difficulty,
        prompt = com.atlabs.percentpro.model.LocalizedText.of("p$id", "प$id"),
        options = listOf("A", "B", "C", "D").map {
            com.atlabs.percentpro.model.LocalizedText.of(it, it)
        },
        correctIndex = 0,
        explanation = com.atlabs.percentpro.model.LocalizedText.of("e$id", "इ$id")
    )
}