package com.atlabs.percentpro.model

/**
 * The kinds of learning activity that can earn a reward.
 *
 * The kind is recovered from the id prefix, which is what lets total XP be
 * rebuilt from the ledger alone instead of trusting a stored running total.
 */
enum class ActivityKind(val idPrefix: String) {
    /** A lesson reached its final block for the first time. */
    LESSON_COMPLETED("lesson_completed"),

    /** A question was answered correctly for the first time, ever. */
    QUESTION_FIRST_CORRECT("question_first_correct"),

    /** A mock test was completed for the first time, by test definition. */
    MOCK_TEST_COMPLETED("mock_test_completed"),

    /**
     * A topic-practice session finished with every question correct.
     *
     * Awards no XP in this phase. It exists only so the achievement it unlocks
     * is recorded in the same ledger as everything else.
     */
    PERFECT_PRACTICE_SESSION("perfect_practice");

    companion object {
        private val byPrefix = entries.associateBy { it.idPrefix }

        fun fromPrefix(prefix: String): ActivityKind? = byPrefix[prefix]
    }
}

/**
 * A stable identifier for a one-off reward.
 *
 * Ids are derived from the thing that happened, never from a timestamp or a
 * counter, so the same lesson or question always produces the same id no matter
 * how many times it is replayed. That is what makes the reward ledger
 * idempotent: recording an id twice is a no-op, so a double tap, a screen
 * recomposition or a process restart cannot pay out twice.
 */
@JvmInline
value class ActivityId(val value: String) {

    val kind: ActivityKind? get() = ActivityKind.fromPrefix(value.substringBefore(':'))

    /** True when this id's kind is worth XP. */
    val awardsXp: Boolean
        get() = kind?.let { RewardRules.xpFor(it) > 0 } ?: false

    override fun toString(): String = value

    companion object {
        /**
         * One id per lesson, so reopening or repeating a lesson pays out once.
         */
        fun lessonCompleted(lessonId: LessonId): ActivityId = lessonCompletedOf(lessonId.storageKey)

        /** String form, for the persistence layer and for already-known ids. */
        fun lessonCompletedOf(lessonId: String): ActivityId =
            ActivityId("${ActivityKind.LESSON_COMPLETED.idPrefix}:$lessonId")

        /**
         * One id per question, shared by topic practice and mock tests, so the
         * first correct answer is rewarded once across both modes.
         */
        fun questionFirstCorrect(questionId: QuestionId): ActivityId =
            questionFirstCorrectOf(questionId.value)

        /** String form, for the persistence layer and for already-known ids. */
        fun questionFirstCorrectOf(questionId: String): ActivityId =
            ActivityId("${ActivityKind.QUESTION_FIRST_CORRECT.idPrefix}:$questionId")

        /**
         * One id per test *definition*, so retrying a paper does not pay again.
         */
        fun mockTestCompleted(testId: MockTestId): ActivityId = mockTestCompletedOf(testId.value)

        /** String form, for the persistence layer and for already-known ids. */
        fun mockTestCompletedOf(testId: String): ActivityId =
            ActivityId("${ActivityKind.MOCK_TEST_COMPLETED.idPrefix}:$testId")

        /** One id per topic. */
        fun perfectPracticeSession(topic: LessonId): ActivityId = perfectPracticeOf(topic.storageKey)

        /** String form, for the persistence layer and for already-known ids. */
        fun perfectPracticeOf(topic: String): ActivityId =
            ActivityId("${ActivityKind.PERFECT_PRACTICE_SESSION.idPrefix}:$topic")

        /**
         * Rebuilds an id from stored text, rejecting anything unrecognised so a
         * corrupted value cannot inject an XP-bearing entry.
         */
        fun parse(value: String): ActivityId? {
            val kind = ActivityKind.fromPrefix(value.substringBefore(':')) ?: return null
            val subject = value.substringAfter(':', missingDelimiterValue = "")
            if (subject.isBlank()) return null
            if (subject.any { it == '|' || it == ',' || it == '=' || it == '\n' }) return null
            return ActivityId("${kind.idPrefix}:$subject")
        }
    }
}

/**
 * The single source of truth for what each kind of activity is worth.
 *
 * Every XP figure in the app is read from here, so there is exactly one place
 * to change a reward and no way for two screens to disagree about it.
 */
object RewardRules {

    const val XP_LESSON_COMPLETED = 20

    const val XP_QUESTION_FIRST_CORRECT = 5

    const val XP_MOCK_TEST_COMPLETED = 10

    /** Achievements grant no XP in this phase. */
    const val XP_PERFECT_PRACTICE_SESSION = 0

    fun xpFor(kind: ActivityKind): Int = when (kind) {
        ActivityKind.LESSON_COMPLETED -> XP_LESSON_COMPLETED
        ActivityKind.QUESTION_FIRST_CORRECT -> XP_QUESTION_FIRST_CORRECT
        ActivityKind.MOCK_TEST_COMPLETED -> XP_MOCK_TEST_COMPLETED
        ActivityKind.PERFECT_PRACTICE_SESSION -> XP_PERFECT_PRACTICE_SESSION
    }

    /** Selectable daily question targets. */
    val dailyGoalOptions: List<Int> = listOf(10, 20, 30)

    const val DEFAULT_DAILY_GOAL = 10

    /** Questions needed on one day to keep a streak alive on that day. */
    const val QUESTIONS_PER_QUALIFYING_DAY = 5
}