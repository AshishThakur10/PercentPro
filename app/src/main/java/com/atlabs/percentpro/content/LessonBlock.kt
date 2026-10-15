package com.atlabs.percentpro.content

import androidx.annotation.StringRes

/**
 * One screen of lesson content. The lesson screen renders these generically, so
 * adding a lesson means adding blocks rather than writing a new composable.
 *
 * All text is a string resource id: the same block renders in every language the
 * app ships.
 */
sealed interface LessonBlock {
    val id: String
}

data class IntroductionBlock(
    override val id: String,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int
) : LessonBlock

data class ExplanationBlock(
    override val id: String,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val bullets: List<Int> = emptyList()
) : LessonBlock

data class FormulaBlock(
    override val id: String,
    @StringRes val formulaRes: Int,
    @StringRes val captionRes: Int
) : LessonBlock

/** A single line of a worked example: a prompt and the value it produces. */
data class ExampleStep(
    @StringRes val promptRes: Int,
    @StringRes val resultRes: Int
)

data class WorkedExampleBlock(
    override val id: String,
    @StringRes val titleRes: Int,
    val steps: List<ExampleStep>
) : LessonBlock

/** A tappable 10x10 grid where each square is 1% of the whole. */
data class PercentageGridBlock(
    override val id: String,
    @StringRes val titleRes: Int,
    @StringRes val captionRes: Int,
    val selectablePercents: List<Int>
) : LessonBlock

data class SummaryBlock(
    override val id: String,
    @StringRes val titleRes: Int,
    val bullets: List<Int>
) : LessonBlock