package com.atlabs.percentpro.content

import androidx.annotation.StringRes
import com.atlabs.percentpro.R
import com.atlabs.percentpro.model.Difficulty
import com.atlabs.percentpro.model.Lesson
import com.atlabs.percentpro.model.LessonId

/**
 * A named band of the path, so the course reads as a course rather than one
 * long list.
 *
 * Sections are derived from lesson order rather than stored on the lesson, which
 * keeps a lesson's own metadata about the lesson.
 */
enum class CourseSection(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val firstOrder: Int,
    val lastOrder: Int
) {
    BEGINNER(
        titleRes = R.string.section_beginner_title,
        descriptionRes = R.string.section_beginner_description,
        firstOrder = 1,
        lastOrder = 6
    ),
    INTERMEDIATE(
        titleRes = R.string.section_intermediate_title,
        descriptionRes = R.string.section_intermediate_description,
        firstOrder = 7,
        lastOrder = 12
    ),
    COMPETITIVE_EXAM(
        titleRes = R.string.section_competitive_title,
        descriptionRes = R.string.section_competitive_description,
        firstOrder = 13,
        lastOrder = 21
    );

    val lessonCount: Int get() = lastOrder - firstOrder + 1

    companion object {
        fun forOrder(order: Int): CourseSection =
            entries.firstOrNull { order in it.firstOrder..it.lastOrder }
                ?: entries.last()

        /** Section ids that have no lessons yet, for tests and tooling. */
        val all: List<CourseSection> get() = entries.toList()
    }
}

/**
 * The ordered path every learner walks.
 *
 * All twenty-one lessons are listed, including the ones whose content is not
 * written yet, so the shape of the course is visible up front. A lesson is only
 * playable when [isImplemented] is true, and [LessonAvailability] refuses to
 * open an unimplemented one regardless of what saved progress claims.
 *
 * Lesson ids are stable strings, so growing the course never invalidates a
 * learner's saved progress.
 */
class LessonCatalog {

    fun lessons(): List<Lesson> = LESSONS

    fun lessonById(id: LessonId): Lesson? = LESSONS.firstOrNull { it.id == id }

    /** The path grouped into its named sections, in order. */
    fun sections(): List<SectionUi> = CourseSection.all.map { section ->
        SectionUi(
            section = section,
            lessons = LESSONS.filter { it.order in section.firstOrder..section.lastOrder }
        )
    }

    /** Section metadata attached to a lesson, for building the path screen. */
    fun sectionFor(lesson: Lesson): CourseSection = CourseSection.forOrder(lesson.order)

    /** Metadata for one section of the path. */
    data class SectionUi(
        val section: CourseSection,
        val lessons: List<Lesson>
    )

    private companion object {

        val LESSONS = listOf(
            // ---- Beginner ----------------------------------------------------
            lesson(
                id = LessonId.WHAT_IS_A_PERCENTAGE,
                order = 1,
                titleRes = R.string.lesson_1_title,
                descriptionRes = R.string.lesson_1_description,
                difficulty = Difficulty.EASY,
                durationMinutes = 6,
                isImplemented = true
            ),
            lesson(
                id = LessonId.PERCENTAGES_VISUALLY,
                order = 2,
                titleRes = R.string.lesson_2_title,
                descriptionRes = R.string.lesson_2_description,
                difficulty = Difficulty.EASY,
                durationMinutes = 8,
                isImplemented = true
            ),
            lesson(
                id = LessonId.FRACTIONS_TO_PERCENTAGES,
                order = 3,
                titleRes = R.string.lesson_3_title,
                descriptionRes = R.string.lesson_3_description,
                difficulty = Difficulty.EASY,
                durationMinutes = 9,
                isImplemented = true
            ),
            lesson(
                id = LessonId.DECIMALS_TO_PERCENTAGES,
                order = 4,
                titleRes = R.string.lesson_4_title,
                descriptionRes = R.string.lesson_4_description,
                difficulty = Difficulty.EASY,
                durationMinutes = 8,
                isImplemented = true
            ),
            lesson(
                id = LessonId.PERCENTAGE_OF_A_NUMBER,
                order = 5,
                titleRes = R.string.lesson_5_title,
                descriptionRes = R.string.lesson_5_description,
                difficulty = Difficulty.EASY,
                durationMinutes = 10,
                isImplemented = true
            ),
            lesson(
                id = LessonId.ORIGINAL_WHOLE,
                order = 6,
                titleRes = R.string.lesson_6_title,
                descriptionRes = R.string.lesson_6_description,
                difficulty = Difficulty.MEDIUM,
                durationMinutes = 12,
                isImplemented = true
            ),

            // ---- Intermediate ------------------------------------------------
            lesson(
                id = LessonId.PERCENTAGE_INCREASE_DECREASE,
                order = 7,
                titleRes = R.string.lesson_7_title,
                descriptionRes = R.string.lesson_7_description,
                difficulty = Difficulty.MEDIUM,
                durationMinutes = 12,
                isImplemented = true
            ),
            lesson(
                id = LessonId.SUCCESSIVE_PERCENTAGE_CHANGES,
                order = 8,
                titleRes = R.string.lesson_8_title,
                descriptionRes = R.string.lesson_8_description,
                difficulty = Difficulty.HARD,
                durationMinutes = 14,
                isImplemented = true
            ),
            lesson(
                id = LessonId.PERCENTAGE_COMPARISON,
                order = 9,
                titleRes = R.string.lesson_9_title,
                descriptionRes = R.string.lesson_9_description,
                difficulty = Difficulty.HARD,
                durationMinutes = 14,
                isImplemented = true
            ),
            lesson(
                id = LessonId.PERCENTAGE_POINTS_VS_CHANGE,
                order = 10,
                titleRes = R.string.lesson_11_title,
                descriptionRes = R.string.lesson_11_description,
                difficulty = Difficulty.MEDIUM,
                durationMinutes = 10,
                isImplemented = true
            ),
            lesson(
                id = LessonId.COMPARING_REPRESENTATIONS,
                order = 11,
                titleRes = R.string.lesson_12_title,
                descriptionRes = R.string.lesson_12_description,
                difficulty = Difficulty.MEDIUM,
                durationMinutes = 9,
                isImplemented = true
            ),
            lesson(
                id = LessonId.EXAM_SHORTCUTS,
                order = 12,
                titleRes = R.string.lesson_13_title,
                descriptionRes = R.string.lesson_13_description,
                difficulty = Difficulty.HARD,
                durationMinutes = 16,
                isImplemented = true
            ),

            // ---- Competitive-exam applications (catalogued, not written yet) --
            lesson(
                id = LessonId.POPULATION_CHANGE,
                order = 13,
                titleRes = R.string.lesson_14_title,
                descriptionRes = R.string.lesson_14_description,
                difficulty = Difficulty.MEDIUM,
                durationMinutes = 12,
                isImplemented = false
            ),
            lesson(
                id = LessonId.SALARY_EXPENDITURE_SAVINGS,
                order = 14,
                titleRes = R.string.lesson_15_title,
                descriptionRes = R.string.lesson_15_description,
                difficulty = Difficulty.MEDIUM,
                durationMinutes = 13,
                isImplemented = false
            ),
            lesson(
                id = LessonId.MARKS_AND_RESULTS,
                order = 15,
                titleRes = R.string.lesson_16_title,
                descriptionRes = R.string.lesson_16_description,
                difficulty = Difficulty.MEDIUM,
                durationMinutes = 12,
                isImplemented = false
            ),
            lesson(
                id = LessonId.ELECTION_VOTING,
                order = 16,
                titleRes = R.string.lesson_17_title,
                descriptionRes = R.string.lesson_17_description,
                difficulty = Difficulty.HARD,
                durationMinutes = 14,
                isImplemented = false
            ),
            lesson(
                id = LessonId.PRICE_DISCOUNT_TAX,
                order = 17,
                titleRes = R.string.lesson_18_title,
                descriptionRes = R.string.lesson_18_description,
                difficulty = Difficulty.MEDIUM,
                durationMinutes = 14,
                isImplemented = false
            ),
            lesson(
                id = LessonId.PROFIT_LOSS,
                order = 18,
                titleRes = R.string.lesson_19_title,
                descriptionRes = R.string.lesson_19_description,
                difficulty = Difficulty.HARD,
                durationMinutes = 15,
                isImplemented = false
            ),
            lesson(
                id = LessonId.REVERSE_PERCENTAGE,
                order = 19,
                titleRes = R.string.lesson_20_title,
                descriptionRes = R.string.lesson_20_description,
                difficulty = Difficulty.HARD,
                durationMinutes = 14,
                isImplemented = false
            ),
            lesson(
                id = LessonId.MIXED_WORD_PROBLEMS,
                order = 20,
                titleRes = R.string.lesson_21_title,
                descriptionRes = R.string.lesson_21_description,
                difficulty = Difficulty.HARD,
                durationMinutes = 18,
                isImplemented = false
            ),
            lesson(
                id = LessonId.COMPETITIVE_EXAM_CHALLENGE,
                order = 21,
                titleRes = R.string.lesson_10_title,
                descriptionRes = R.string.lesson_10_description,
                difficulty = Difficulty.HARD,
                durationMinutes = 20,
                isImplemented = false
            )
        )

        fun lesson(
            id: LessonId,
            order: Int,
            @StringRes titleRes: Int,
            @StringRes descriptionRes: Int,
            difficulty: Difficulty,
            durationMinutes: Int,
            isImplemented: Boolean
        ) = Lesson(
            id = id,
            order = order,
            titleRes = titleRes,
            descriptionRes = descriptionRes,
            difficulty = difficulty,
            durationMinutes = durationMinutes,
            isImplemented = isImplemented
        )
    }
}