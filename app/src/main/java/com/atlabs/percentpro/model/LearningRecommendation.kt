package com.atlabs.percentpro.model

/** What the app is suggesting the learner do next. */
enum class RecommendationKind {
    /** Pick up the lesson that was left part-way. */
    RESUME_LESSON,

    /** Start the next unlocked lesson on the path. */
    CONTINUE_COURSE,

    /** Drill a topic the data says is weak. */
    PRACTICE_WEAK_TOPIC,

    /** The level is secure enough to try the next difficulty. */
    STEP_UP_DIFFICULTY,

    /** Several topics are comparable, so a mixed run is worthwhile. */
    MIXED_REVIEW,

    /** Nothing is unlocked yet and there is no history to analyse. */
    START_WITH_LESSON_ONE,

    /** Everything available has been done; keep consolidating. */
    KEEP_PRACTISING
}

/** Where a recommendation leads. */
sealed interface RecommendationDestination {
    data class OpenLesson(val lessonId: LessonId) : RecommendationDestination
    data class PracticeTopic(val topic: LessonId) : RecommendationDestination
    data class PracticeDifficulty(
        val topic: LessonId,
        val difficulty: QuestionDifficulty
    ) : RecommendationDestination

    data object MixedReview : RecommendationDestination
    data object OpenInsights : RecommendationDestination
}

/**
 * One recommendation, fully explainable.
 *
 * [reasonCode] plus its arguments is what the UI turns into a sentence, so the
 * wording is localisable while the decision stays numeric. Nothing here reads a
 * translated string.
 */
data class LearningRecommendation(
    val kind: RecommendationKind,
    val titleKey: RecommendationTitleKey,
    val reasonCode: RecommendationReason,
    val reasonArgs: List<Int> = emptyList(),
    val lessonId: LessonId? = null,
    val topic: LessonId? = null,
    val difficulty: QuestionDifficulty? = null,
    val destination: RecommendationDestination
) {
    /** Topic the sentence is about, falling back to the lesson's topic. */
    val topicForCopy: LessonId? get() = topic ?: lessonId
}

/** Stable keys for the recommendation headline. */
enum class RecommendationTitleKey {
    RESUME_LESSON,
    CONTINUE_COURSE,
    PRACTICE_WEAK_TOPIC,
    STEP_UP_DIFFICULTY,
    MIXED_REVIEW,
    START_WITH_LESSON_ONE,
    KEEP_PRACTISING
}

/**
 * Stable keys for the sentence explaining a recommendation.
 *
 * The UI resolves these with its own string resources, so English and Hindi can
 * differ in phrasing without the decision changing.
 */
enum class RecommendationReason {
    LESSON_IN_PROGRESS,
    NEXT_LESSON_UNLOCKED,
    LOW_TOPIC_ACCURACY,
    STRONG_ON_EASY,
    BALANCED_ACROSS_TOPICS,
    NO_HISTORY_YET,
    COURSE_COMPLETE,
    NO_RECOMMENDATION
}