package com.atlabs.percentpro.content

import com.atlabs.percentpro.R
import com.atlabs.percentpro.model.LessonId

/**
 * Body content for the lessons that have been written.
 *
 * Lessons that exist on the path but have no entry here are reported by
 * [blocksFor] as an empty list; callers treat that as "not implemented" rather
 * than opening an empty screen.
 */
class LessonContentRepository {

    fun blocksFor(lessonId: LessonId): List<LessonBlock> = when (lessonId) {
        LessonId.WHAT_IS_A_PERCENTAGE -> lessonOne()
        LessonId.PERCENTAGES_VISUALLY -> lessonTwo()
        LessonId.FRACTIONS_TO_PERCENTAGES -> lessonThree()
        LessonId.DECIMALS_TO_PERCENTAGES -> lesson04()
        LessonId.PERCENTAGE_OF_A_NUMBER -> lesson05()
        LessonId.ORIGINAL_WHOLE -> lesson06()
        LessonId.PERCENTAGE_INCREASE_DECREASE -> lesson07()
        LessonId.SUCCESSIVE_PERCENTAGE_CHANGES -> lesson08()
        LessonId.PERCENTAGE_COMPARISON -> lesson09()
        LessonId.PERCENTAGE_POINTS_VS_CHANGE -> lesson10()
        LessonId.COMPARING_REPRESENTATIONS -> lesson11()
        LessonId.EXAM_SHORTCUTS -> lesson12()
        else -> emptyList()
    }

    fun isImplemented(lessonId: LessonId): Boolean = blocksFor(lessonId).isNotEmpty()

    private fun lessonOne(): List<LessonBlock> = listOf(
        IntroductionBlock(
            id = "l1_intro",
            titleRes = R.string.l1_intro_title,
            bodyRes = R.string.l1_intro_body
        ),
        ExplanationBlock(
            id = "l1_per_hundred",
            titleRes = R.string.l1_per_hundred_title,
            bodyRes = R.string.l1_per_hundred_body,
            bullets = listOf(
                R.string.l1_b1,
                R.string.l1_b2,
                R.string.l1_b3
            )
        ),
        ExplanationBlock(
            id = "l1_symbol",
            titleRes = R.string.l1_symbol_title,
            bodyRes = R.string.l1_symbol_body,
            bullets = listOf(
                R.string.l1_symbol_b1,
                R.string.l1_symbol_b2,
                R.string.l1_symbol_b3
            )
        ),
        FormulaBlock(
            id = "l1_formula",
            formulaRes = R.string.l1_formula,
            captionRes = R.string.l1_formula_caption
        ),
        WorkedExampleBlock(
            id = "l1_example_50",
            titleRes = R.string.l1_example_50_title,
            steps = listOf(
                ExampleStep(
                    promptRes = R.string.l1_e50_prompt,
                    resultRes = R.string.l1_e50_result
                )
            )
        ),
        WorkedExampleBlock(
            id = "l1_example_25",
            titleRes = R.string.l1_example_25_title,
            steps = listOf(
                ExampleStep(
                    promptRes = R.string.l1_e25_prompt,
                    resultRes = R.string.l1_e25_result
                )
            )
        ),
        WorkedExampleBlock(
            id = "l1_example_100",
            titleRes = R.string.l1_example_100_title,
            steps = listOf(
                ExampleStep(
                    promptRes = R.string.l1_e100_prompt,
                    resultRes = R.string.l1_e100_result
                )
            )
        ),
        SummaryBlock(
            id = "l1_summary",
            titleRes = R.string.l1_summary_title,
            bullets = listOf(
                R.string.l1_s1,
                R.string.l1_s2,
                R.string.l1_s3
            )
        )
    )

    private fun lessonTwo(): List<LessonBlock> = listOf(
        IntroductionBlock(
            id = "l2_intro",
            titleRes = R.string.l2_intro_title,
            bodyRes = R.string.l2_intro_body
        ),
        PercentageGridBlock(
            id = "l2_grid",
            titleRes = R.string.l2_grid_title,
            captionRes = R.string.l2_grid_caption,
            selectablePercents = listOf(25, 50, 75)
        ),
        ExplanationBlock(
            id = "l2_explain",
            titleRes = R.string.l2_explain_title,
            bodyRes = R.string.l2_explain_body,
            bullets = listOf(
                R.string.l2_b25,
                R.string.l2_b50,
                R.string.l2_b75
            )
        ),
        ExplanationBlock(
            id = "l2_note",
            titleRes = R.string.l2_note_title,
            bodyRes = R.string.l2_note_body
        ),
        SummaryBlock(
            id = "l2_summary",
            titleRes = R.string.l2_summary_title,
            bullets = listOf(
                R.string.l2_s1,
                R.string.l2_s2,
                R.string.l2_s3
            )
        )
    )

    private fun lessonThree(): List<LessonBlock> = listOf(
        IntroductionBlock(
            id = "l3_intro",
            titleRes = R.string.l3_intro_title,
            bodyRes = R.string.l3_intro_body
        ),
        ExplanationBlock(
            id = "l3_method",
            titleRes = R.string.l3_method_title,
            bodyRes = R.string.l3_method_body,
            bullets = listOf(
                R.string.l3_method_b1,
                R.string.l3_method_b2
            )
        ),
        FormulaBlock(
            id = "l3_formula",
            formulaRes = R.string.l3_formula,
            captionRes = R.string.l3_formula_caption
        ),
        WorkedExampleBlock(
            id = "l3_example_1_2",
            titleRes = R.string.l3_example_1_2_title,
            steps = listOf(
                ExampleStep(R.string.l3_e1_2_prompt, R.string.l3_e1_2_result)
            )
        ),
        WorkedExampleBlock(
            id = "l3_example_1_4",
            titleRes = R.string.l3_example_1_4_title,
            steps = listOf(
                ExampleStep(R.string.l3_e1_4_prompt, R.string.l3_e1_4_result)
            )
        ),
        WorkedExampleBlock(
            id = "l3_example_3_4",
            titleRes = R.string.l3_example_3_4_title,
            steps = listOf(
                ExampleStep(R.string.l3_e3_4_prompt, R.string.l3_e3_4_result)
            )
        ),
        WorkedExampleBlock(
            id = "l3_example_1_5",
            titleRes = R.string.l3_example_1_5_title,
            steps = listOf(
                ExampleStep(R.string.l3_e1_5_prompt, R.string.l3_e1_5_result)
            )
        ),
        ExplanationBlock(
            id = "l3_caution",
            titleRes = R.string.l3_caution_title,
            bodyRes = R.string.l3_caution_body
        ),
        SummaryBlock(
            id = "l3_summary",
            titleRes = R.string.l3_summary_title,
            bullets = listOf(
                R.string.l3_s1,
                R.string.l3_s2,
                R.string.l3_s3
            )
        )
    )


    private fun lesson04(): List<LessonBlock> = listOf(
        IntroductionBlock(
            id = "l04_b1",
            titleRes = R.string.l04_b1_title,
            bodyRes = R.string.l04_b1_body
        ),
        ExplanationBlock(
            id = "l04_b2",
            titleRes = R.string.l04_b2_title,
            bodyRes = R.string.l04_b2_body,
            bullets = listOf(
                R.string.l04_b2_b1,
                R.string.l04_b2_b2,
                R.string.l04_b2_b3,
                R.string.l04_b2_b4,
            )
        ),
        FormulaBlock(
            id = "l04_b3",
            formulaRes = R.string.l04_b3_formula,
            captionRes = R.string.l04_b3_caption
        ),
        ExplanationBlock(
            id = "l04_b4",
            titleRes = R.string.l04_b4_title,
            bodyRes = R.string.l04_b4_body
        ),
        WorkedExampleBlock(
            id = "l04_b5",
            titleRes = R.string.l04_b5_title,
            steps = listOf(
                ExampleStep(R.string.l04_b5_s1_prompt, R.string.l04_b5_s1_result),
                ExampleStep(R.string.l04_b5_s2_prompt, R.string.l04_b5_s2_result),
                ExampleStep(R.string.l04_b5_s3_prompt, R.string.l04_b5_s3_result),
            )
        ),
        WorkedExampleBlock(
            id = "l04_b6",
            titleRes = R.string.l04_b6_title,
            steps = listOf(
                ExampleStep(R.string.l04_b6_s1_prompt, R.string.l04_b6_s1_result),
                ExampleStep(R.string.l04_b6_s2_prompt, R.string.l04_b6_s2_result),
                ExampleStep(R.string.l04_b6_s3_prompt, R.string.l04_b6_s3_result),
            )
        ),
        ExplanationBlock(
            id = "l04_b7",
            titleRes = R.string.l04_b7_title,
            bodyRes = R.string.l04_b7_body
        ),
        ExplanationBlock(
            id = "l04_b8",
            titleRes = R.string.l04_b8_title,
            bodyRes = R.string.l04_b8_body,
            bullets = listOf(
                R.string.l04_b8_answer
            )
        ),
        SummaryBlock(
            id = "l04_b9",
            titleRes = R.string.l04_b9_title,
            bullets = listOf(
                R.string.l04_b9_b1,
                R.string.l04_b9_b2,
                R.string.l04_b9_b3,
            )
        )
    )

    private fun lesson05(): List<LessonBlock> = listOf(
        IntroductionBlock(
            id = "l05_b1",
            titleRes = R.string.l05_b1_title,
            bodyRes = R.string.l05_b1_body
        ),
        ExplanationBlock(
            id = "l05_b2",
            titleRes = R.string.l05_b2_title,
            bodyRes = R.string.l05_b2_body,
            bullets = listOf(
                R.string.l05_b2_b1,
                R.string.l05_b2_b2,
                R.string.l05_b2_b3,
            )
        ),
        FormulaBlock(
            id = "l05_b3",
            formulaRes = R.string.l05_b3_formula,
            captionRes = R.string.l05_b3_caption
        ),
        ExplanationBlock(
            id = "l05_b4",
            titleRes = R.string.l05_b4_title,
            bodyRes = R.string.l05_b4_body
        ),
        WorkedExampleBlock(
            id = "l05_b5",
            titleRes = R.string.l05_b5_title,
            steps = listOf(
                ExampleStep(R.string.l05_b5_s1_prompt, R.string.l05_b5_s1_result),
                ExampleStep(R.string.l05_b5_s2_prompt, R.string.l05_b5_s2_result),
                ExampleStep(R.string.l05_b5_s3_prompt, R.string.l05_b5_s3_result),
            )
        ),
        WorkedExampleBlock(
            id = "l05_b6",
            titleRes = R.string.l05_b6_title,
            steps = listOf(
                ExampleStep(R.string.l05_b6_s1_prompt, R.string.l05_b6_s1_result),
                ExampleStep(R.string.l05_b6_s2_prompt, R.string.l05_b6_s2_result),
                ExampleStep(R.string.l05_b6_s3_prompt, R.string.l05_b6_s3_result),
            )
        ),
        WorkedExampleBlock(
            id = "l05_b7",
            titleRes = R.string.l05_b7_title,
            steps = listOf(
                ExampleStep(R.string.l05_b7_s1_prompt, R.string.l05_b7_s1_result),
                ExampleStep(R.string.l05_b7_s2_prompt, R.string.l05_b7_s2_result),
                ExampleStep(R.string.l05_b7_s3_prompt, R.string.l05_b7_s3_result),
            )
        ),
        ExplanationBlock(
            id = "l05_b8",
            titleRes = R.string.l05_b8_title,
            bodyRes = R.string.l05_b8_body
        ),
        ExplanationBlock(
            id = "l05_b9",
            titleRes = R.string.l05_b9_title,
            bodyRes = R.string.l05_b9_body,
            bullets = listOf(
                R.string.l05_b9_answer
            )
        ),
        SummaryBlock(
            id = "l05_b10",
            titleRes = R.string.l05_b10_title,
            bullets = listOf(
                R.string.l05_b10_b1,
                R.string.l05_b10_b2,
                R.string.l05_b10_b3,
            )
        )
    )

    private fun lesson06(): List<LessonBlock> = listOf(
        IntroductionBlock(
            id = "l06_b1",
            titleRes = R.string.l06_b1_title,
            bodyRes = R.string.l06_b1_body
        ),
        ExplanationBlock(
            id = "l06_b2",
            titleRes = R.string.l06_b2_title,
            bodyRes = R.string.l06_b2_body
        ),
        FormulaBlock(
            id = "l06_b3",
            formulaRes = R.string.l06_b3_formula,
            captionRes = R.string.l06_b3_caption
        ),
        WorkedExampleBlock(
            id = "l06_b4",
            titleRes = R.string.l06_b4_title,
            steps = listOf(
                ExampleStep(R.string.l06_b4_s1_prompt, R.string.l06_b4_s1_result),
                ExampleStep(R.string.l06_b4_s2_prompt, R.string.l06_b4_s2_result),
                ExampleStep(R.string.l06_b4_s3_prompt, R.string.l06_b4_s3_result),
            )
        ),
        WorkedExampleBlock(
            id = "l06_b5",
            titleRes = R.string.l06_b5_title,
            steps = listOf(
                ExampleStep(R.string.l06_b5_s1_prompt, R.string.l06_b5_s1_result),
                ExampleStep(R.string.l06_b5_s2_prompt, R.string.l06_b5_s2_result),
                ExampleStep(R.string.l06_b5_s3_prompt, R.string.l06_b5_s3_result),
            )
        ),
        WorkedExampleBlock(
            id = "l06_b6",
            titleRes = R.string.l06_b6_title,
            steps = listOf(
                ExampleStep(R.string.l06_b6_s1_prompt, R.string.l06_b6_s1_result),
                ExampleStep(R.string.l06_b6_s2_prompt, R.string.l06_b6_s2_result),
                ExampleStep(R.string.l06_b6_s3_prompt, R.string.l06_b6_s3_result),
            )
        ),
        ExplanationBlock(
            id = "l06_b7",
            titleRes = R.string.l06_b7_title,
            bodyRes = R.string.l06_b7_body
        ),
        ExplanationBlock(
            id = "l06_b8",
            titleRes = R.string.l06_b8_title,
            bodyRes = R.string.l06_b8_body,
            bullets = listOf(
                R.string.l06_b8_answer
            )
        ),
        SummaryBlock(
            id = "l06_b9",
            titleRes = R.string.l06_b9_title,
            bullets = listOf(
                R.string.l06_b9_b1,
                R.string.l06_b9_b2,
                R.string.l06_b9_b3,
            )
        )
    )

    private fun lesson07(): List<LessonBlock> = listOf(
        IntroductionBlock(
            id = "l07_b1",
            titleRes = R.string.l07_b1_title,
            bodyRes = R.string.l07_b1_body
        ),
        ExplanationBlock(
            id = "l07_b2",
            titleRes = R.string.l07_b2_title,
            bodyRes = R.string.l07_b2_body
        ),
        FormulaBlock(
            id = "l07_b3",
            formulaRes = R.string.l07_b3_formula,
            captionRes = R.string.l07_b3_caption
        ),
        WorkedExampleBlock(
            id = "l07_b4",
            titleRes = R.string.l07_b4_title,
            steps = listOf(
                ExampleStep(R.string.l07_b4_s1_prompt, R.string.l07_b4_s1_result),
                ExampleStep(R.string.l07_b4_s2_prompt, R.string.l07_b4_s2_result),
                ExampleStep(R.string.l07_b4_s3_prompt, R.string.l07_b4_s3_result),
            )
        ),
        WorkedExampleBlock(
            id = "l07_b5",
            titleRes = R.string.l07_b5_title,
            steps = listOf(
                ExampleStep(R.string.l07_b5_s1_prompt, R.string.l07_b5_s1_result),
                ExampleStep(R.string.l07_b5_s2_prompt, R.string.l07_b5_s2_result),
                ExampleStep(R.string.l07_b5_s3_prompt, R.string.l07_b5_s3_result),
            )
        ),
        ExplanationBlock(
            id = "l07_b6",
            titleRes = R.string.l07_b6_title,
            bodyRes = R.string.l07_b6_body
        ),
        WorkedExampleBlock(
            id = "l07_b7",
            titleRes = R.string.l07_b7_title,
            steps = listOf(
                ExampleStep(R.string.l07_b7_s1_prompt, R.string.l07_b7_s1_result),
                ExampleStep(R.string.l07_b7_s2_prompt, R.string.l07_b7_s2_result),
                ExampleStep(R.string.l07_b7_s3_prompt, R.string.l07_b7_s3_result),
            )
        ),
        ExplanationBlock(
            id = "l07_b8",
            titleRes = R.string.l07_b8_title,
            bodyRes = R.string.l07_b8_body
        ),
        ExplanationBlock(
            id = "l07_b9",
            titleRes = R.string.l07_b9_title,
            bodyRes = R.string.l07_b9_body,
            bullets = listOf(
                R.string.l07_b9_answer
            )
        ),
        SummaryBlock(
            id = "l07_b10",
            titleRes = R.string.l07_b10_title,
            bullets = listOf(
                R.string.l07_b10_b1,
                R.string.l07_b10_b2,
                R.string.l07_b10_b3,
            )
        )
    )

    private fun lesson08(): List<LessonBlock> = listOf(
        IntroductionBlock(
            id = "l08_b1",
            titleRes = R.string.l08_b1_title,
            bodyRes = R.string.l08_b1_body
        ),
        ExplanationBlock(
            id = "l08_b2",
            titleRes = R.string.l08_b2_title,
            bodyRes = R.string.l08_b2_body
        ),
        FormulaBlock(
            id = "l08_b3",
            formulaRes = R.string.l08_b3_formula,
            captionRes = R.string.l08_b3_caption
        ),
        WorkedExampleBlock(
            id = "l08_b4",
            titleRes = R.string.l08_b4_title,
            steps = listOf(
                ExampleStep(R.string.l08_b4_s1_prompt, R.string.l08_b4_s1_result),
                ExampleStep(R.string.l08_b4_s2_prompt, R.string.l08_b4_s2_result),
                ExampleStep(R.string.l08_b4_s3_prompt, R.string.l08_b4_s3_result),
                ExampleStep(R.string.l08_b4_s4_prompt, R.string.l08_b4_s4_result),
            )
        ),
        WorkedExampleBlock(
            id = "l08_b5",
            titleRes = R.string.l08_b5_title,
            steps = listOf(
                ExampleStep(R.string.l08_b5_s1_prompt, R.string.l08_b5_s1_result),
                ExampleStep(R.string.l08_b5_s2_prompt, R.string.l08_b5_s2_result),
                ExampleStep(R.string.l08_b5_s3_prompt, R.string.l08_b5_s3_result),
                ExampleStep(R.string.l08_b5_s4_prompt, R.string.l08_b5_s4_result),
            )
        ),
        ExplanationBlock(
            id = "l08_b6",
            titleRes = R.string.l08_b6_title,
            bodyRes = R.string.l08_b6_body
        ),
        WorkedExampleBlock(
            id = "l08_b7",
            titleRes = R.string.l08_b7_title,
            steps = listOf(
                ExampleStep(R.string.l08_b7_s1_prompt, R.string.l08_b7_s1_result),
                ExampleStep(R.string.l08_b7_s2_prompt, R.string.l08_b7_s2_result),
                ExampleStep(R.string.l08_b7_s3_prompt, R.string.l08_b7_s3_result),
                ExampleStep(R.string.l08_b7_s4_prompt, R.string.l08_b7_s4_result),
            )
        ),
        ExplanationBlock(
            id = "l08_b8",
            titleRes = R.string.l08_b8_title,
            bodyRes = R.string.l08_b8_body
        ),
        ExplanationBlock(
            id = "l08_b9",
            titleRes = R.string.l08_b9_title,
            bodyRes = R.string.l08_b9_body,
            bullets = listOf(
                R.string.l08_b9_answer
            )
        ),
        SummaryBlock(
            id = "l08_b10",
            titleRes = R.string.l08_b10_title,
            bullets = listOf(
                R.string.l08_b10_b1,
                R.string.l08_b10_b2,
                R.string.l08_b10_b3,
            )
        )
    )

    private fun lesson09(): List<LessonBlock> = listOf(
        IntroductionBlock(
            id = "l09_b1",
            titleRes = R.string.l09_b1_title,
            bodyRes = R.string.l09_b1_body
        ),
        ExplanationBlock(
            id = "l09_b2",
            titleRes = R.string.l09_b2_title,
            bodyRes = R.string.l09_b2_body
        ),
        FormulaBlock(
            id = "l09_b3",
            formulaRes = R.string.l09_b3_formula,
            captionRes = R.string.l09_b3_caption
        ),
        WorkedExampleBlock(
            id = "l09_b4",
            titleRes = R.string.l09_b4_title,
            steps = listOf(
                ExampleStep(R.string.l09_b4_s1_prompt, R.string.l09_b4_s1_result),
                ExampleStep(R.string.l09_b4_s2_prompt, R.string.l09_b4_s2_result),
                ExampleStep(R.string.l09_b4_s3_prompt, R.string.l09_b4_s3_result),
            )
        ),
        WorkedExampleBlock(
            id = "l09_b5",
            titleRes = R.string.l09_b5_title,
            steps = listOf(
                ExampleStep(R.string.l09_b5_s1_prompt, R.string.l09_b5_s1_result),
                ExampleStep(R.string.l09_b5_s2_prompt, R.string.l09_b5_s2_result),
                ExampleStep(R.string.l09_b5_s3_prompt, R.string.l09_b5_s3_result),
            )
        ),
        ExplanationBlock(
            id = "l09_b6",
            titleRes = R.string.l09_b6_title,
            bodyRes = R.string.l09_b6_body
        ),
        WorkedExampleBlock(
            id = "l09_b7",
            titleRes = R.string.l09_b7_title,
            steps = listOf(
                ExampleStep(R.string.l09_b7_s1_prompt, R.string.l09_b7_s1_result),
                ExampleStep(R.string.l09_b7_s2_prompt, R.string.l09_b7_s2_result),
                ExampleStep(R.string.l09_b7_s3_prompt, R.string.l09_b7_s3_result),
                ExampleStep(R.string.l09_b7_s4_prompt, R.string.l09_b7_s4_result),
            )
        ),
        ExplanationBlock(
            id = "l09_b8",
            titleRes = R.string.l09_b8_title,
            bodyRes = R.string.l09_b8_body
        ),
        ExplanationBlock(
            id = "l09_b9",
            titleRes = R.string.l09_b9_title,
            bodyRes = R.string.l09_b9_body,
            bullets = listOf(
                R.string.l09_b9_answer
            )
        ),
        SummaryBlock(
            id = "l09_b10",
            titleRes = R.string.l09_b10_title,
            bullets = listOf(
                R.string.l09_b10_b1,
                R.string.l09_b10_b2,
                R.string.l09_b10_b3,
            )
        )
    )

    private fun lesson10(): List<LessonBlock> = listOf(
        IntroductionBlock(
            id = "l10_b1",
            titleRes = R.string.l10_b1_title,
            bodyRes = R.string.l10_b1_body
        ),
        ExplanationBlock(
            id = "l10_b2",
            titleRes = R.string.l10_b2_title,
            bodyRes = R.string.l10_b2_body
        ),
        FormulaBlock(
            id = "l10_b3",
            formulaRes = R.string.l10_b3_formula,
            captionRes = R.string.l10_b3_caption
        ),
        WorkedExampleBlock(
            id = "l10_b4",
            titleRes = R.string.l10_b4_title,
            steps = listOf(
                ExampleStep(R.string.l10_b4_s1_prompt, R.string.l10_b4_s1_result),
                ExampleStep(R.string.l10_b4_s2_prompt, R.string.l10_b4_s2_result),
                ExampleStep(R.string.l10_b4_s3_prompt, R.string.l10_b4_s3_result),
            )
        ),
        WorkedExampleBlock(
            id = "l10_b5",
            titleRes = R.string.l10_b5_title,
            steps = listOf(
                ExampleStep(R.string.l10_b5_s1_prompt, R.string.l10_b5_s1_result),
                ExampleStep(R.string.l10_b5_s2_prompt, R.string.l10_b5_s2_result),
                ExampleStep(R.string.l10_b5_s3_prompt, R.string.l10_b5_s3_result),
            )
        ),
        ExplanationBlock(
            id = "l10_b6",
            titleRes = R.string.l10_b6_title,
            bodyRes = R.string.l10_b6_body
        ),
        WorkedExampleBlock(
            id = "l10_b7",
            titleRes = R.string.l10_b7_title,
            steps = listOf(
                ExampleStep(R.string.l10_b7_s1_prompt, R.string.l10_b7_s1_result),
                ExampleStep(R.string.l10_b7_s2_prompt, R.string.l10_b7_s2_result),
                ExampleStep(R.string.l10_b7_s3_prompt, R.string.l10_b7_s3_result),
            )
        ),
        ExplanationBlock(
            id = "l10_b8",
            titleRes = R.string.l10_b8_title,
            bodyRes = R.string.l10_b8_body
        ),
        ExplanationBlock(
            id = "l10_b9",
            titleRes = R.string.l10_b9_title,
            bodyRes = R.string.l10_b9_body,
            bullets = listOf(
                R.string.l10_b9_answer
            )
        ),
        SummaryBlock(
            id = "l10_b10",
            titleRes = R.string.l10_b10_title,
            bullets = listOf(
                R.string.l10_b10_b1,
                R.string.l10_b10_b2,
                R.string.l10_b10_b3,
            )
        )
    )

    private fun lesson11(): List<LessonBlock> = listOf(
        IntroductionBlock(
            id = "l11_b1",
            titleRes = R.string.l11_b1_title,
            bodyRes = R.string.l11_b1_body
        ),
        ExplanationBlock(
            id = "l11_b2",
            titleRes = R.string.l11_b2_title,
            bodyRes = R.string.l11_b2_body
        ),
        FormulaBlock(
            id = "l11_b3",
            formulaRes = R.string.l11_b3_formula,
            captionRes = R.string.l11_b3_caption
        ),
        ExplanationBlock(
            id = "l11_b4",
            titleRes = R.string.l11_b4_title,
            bodyRes = R.string.l11_b4_body,
            bullets = listOf(
                R.string.l11_b4_b1,
                R.string.l11_b4_b2,
                R.string.l11_b4_b3,
            )
        ),
        WorkedExampleBlock(
            id = "l11_b5",
            titleRes = R.string.l11_b5_title,
            steps = listOf(
                ExampleStep(R.string.l11_b5_s1_prompt, R.string.l11_b5_s1_result),
                ExampleStep(R.string.l11_b5_s2_prompt, R.string.l11_b5_s2_result),
                ExampleStep(R.string.l11_b5_s3_prompt, R.string.l11_b5_s3_result),
                ExampleStep(R.string.l11_b5_s4_prompt, R.string.l11_b5_s4_result),
            )
        ),
        WorkedExampleBlock(
            id = "l11_b6",
            titleRes = R.string.l11_b6_title,
            steps = listOf(
                ExampleStep(R.string.l11_b6_s1_prompt, R.string.l11_b6_s1_result),
                ExampleStep(R.string.l11_b6_s2_prompt, R.string.l11_b6_s2_result),
                ExampleStep(R.string.l11_b6_s3_prompt, R.string.l11_b6_s3_result),
            )
        ),
        ExplanationBlock(
            id = "l11_b7",
            titleRes = R.string.l11_b7_title,
            bodyRes = R.string.l11_b7_body
        ),
        ExplanationBlock(
            id = "l11_b8",
            titleRes = R.string.l11_b8_title,
            bodyRes = R.string.l11_b8_body,
            bullets = listOf(
                R.string.l11_b8_answer
            )
        ),
        SummaryBlock(
            id = "l11_b9",
            titleRes = R.string.l11_b9_title,
            bullets = listOf(
                R.string.l11_b9_b1,
                R.string.l11_b9_b2,
                R.string.l11_b9_b3,
            )
        )
    )

    private fun lesson12(): List<LessonBlock> = listOf(
        IntroductionBlock(
            id = "l12_b1",
            titleRes = R.string.l12_b1_title,
            bodyRes = R.string.l12_b1_body
        ),
        ExplanationBlock(
            id = "l12_b2",
            titleRes = R.string.l12_b2_title,
            bodyRes = R.string.l12_b2_body
        ),
        WorkedExampleBlock(
            id = "l12_b3",
            titleRes = R.string.l12_b3_title,
            steps = listOf(
                ExampleStep(R.string.l12_b3_s1_prompt, R.string.l12_b3_s1_result),
                ExampleStep(R.string.l12_b3_s2_prompt, R.string.l12_b3_s2_result),
                ExampleStep(R.string.l12_b3_s3_prompt, R.string.l12_b3_s3_result),
            )
        ),
        ExplanationBlock(
            id = "l12_b4",
            titleRes = R.string.l12_b4_title,
            bodyRes = R.string.l12_b4_body
        ),
        WorkedExampleBlock(
            id = "l12_b5",
            titleRes = R.string.l12_b5_title,
            steps = listOf(
                ExampleStep(R.string.l12_b5_s1_prompt, R.string.l12_b5_s1_result),
                ExampleStep(R.string.l12_b5_s2_prompt, R.string.l12_b5_s2_result),
                ExampleStep(R.string.l12_b5_s3_prompt, R.string.l12_b5_s3_result),
            )
        ),
        ExplanationBlock(
            id = "l12_b6",
            titleRes = R.string.l12_b6_title,
            bodyRes = R.string.l12_b6_body
        ),
        WorkedExampleBlock(
            id = "l12_b7",
            titleRes = R.string.l12_b7_title,
            steps = listOf(
                ExampleStep(R.string.l12_b7_s1_prompt, R.string.l12_b7_s1_result),
                ExampleStep(R.string.l12_b7_s2_prompt, R.string.l12_b7_s2_result),
                ExampleStep(R.string.l12_b7_s3_prompt, R.string.l12_b7_s3_result),
            )
        ),
        ExplanationBlock(
            id = "l12_b8",
            titleRes = R.string.l12_b8_title,
            bodyRes = R.string.l12_b8_body
        ),
        ExplanationBlock(
            id = "l12_b9",
            titleRes = R.string.l12_b9_title,
            bodyRes = R.string.l12_b9_body,
            bullets = listOf(
                R.string.l12_b9_answer
            )
        ),
        SummaryBlock(
            id = "l12_b10",
            titleRes = R.string.l12_b10_title,
            bullets = listOf(
                R.string.l12_b10_b1,
                R.string.l12_b10_b2,
                R.string.l12_b10_b3,
                R.string.l12_b10_b4,
            )
        )
    )
}
