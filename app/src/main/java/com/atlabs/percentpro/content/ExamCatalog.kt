package com.atlabs.percentpro.content

import com.atlabs.percentpro.R
import com.atlabs.percentpro.model.ExamDefinition
import com.atlabs.percentpro.model.ExamId
import com.atlabs.percentpro.model.ExamYearPaper
import com.atlabs.percentpro.model.ExamYearScheme
import com.atlabs.percentpro.model.LocalizedText
import com.atlabs.percentpro.model.QuestionDifficulty

/**
 * The examinations this app writes practice papers for.
 *
 * Each entry records the *published shape* of a real paper: how many questions
 * the section holds, how it is marked, how long it runs. That is public scheme
 * information a learner is entitled to, and it is what the drill is built to
 * feel like.
 *
 * It is deliberately not a copy of any paper. The questions themselves are
 * written for this app; see [com.atlabs.percentpro.model.Provenance] for how a
 * question is tagged and what a tag does and does not claim.
 *
 * Marks are in twelfths. Two of the schemes here are not expressible in quarter
 * marks: the Railway papers deduct exactly one third, and SSC CGL and CHSL award
 * two marks a question rather than one.
 */
class ExamCatalog(private val currentYear: Int = systemYear()) {

    fun exams(): List<ExamDefinition> = EXAMS

    fun available(): List<ExamDefinition> = exams().filter { it.isAvailable }

    fun examById(id: ExamId): ExamDefinition? =
        exams().firstOrNull { it.id == id }

    fun examById(value: String): ExamDefinition? =
        exams().firstOrNull { it.id.value == value }

    // ---- year-by-year papers ----------------------------------------------

    /** Every recorded scheme for an exam, oldest span first. */
    fun schemesFor(examId: ExamId): List<ExamYearScheme> =
        YEAR_SCHEMES.filter { it.examId == examId }.sortedBy { it.fromYear }

    /** The scheme in force in a given year, whether or not it is verified. */
    fun schemeFor(examId: ExamId, year: Int): ExamYearScheme? =
        schemesFor(examId).lastOrNull { it.covers(year) }

    /**
     * Whether this exam ran that year.
     *
     * A year outside every recorded span is not a paper that failed, it is a year
     * the app has nothing recorded for, and the two are shown differently. A
     * scheme still marked open-ended only counts up to the current year: without
     * that bound an open scheme would claim every year to 9999.
     */
    fun hasYear(examId: ExamId, year: Int): Boolean {
        if (year > currentYear) return false
        return schemeFor(examId, year) != null
    }

    /** The earliest year this exam is recorded as having run. */
    fun firstYear(examId: ExamId): Int? = schemesFor(examId).minOfOrNull { it.fromYear }

    /** The latest year this exam is recorded as having run, never a future one. */
    fun lastYear(examId: ExamId): Int? =
        schemesFor(examId).maxOfOrNull { minOf(it.lastYear, currentYear) }

    /**
     * The papers offered for an exam, oldest scheme first.
     *
     * By default this is one entry per scheme span, because a run of years that
     * shared a marking scheme is a single thing to practise; listing 2020 to 2026
     * separately would offer the same paper seven times over. With
     * [includeEveryYear] each year gets its own entry, for a reader who wants to
     * see the shape of the archive rather than practise it.
     *
     * A span whose scheme was never confirmed against the official notice is
     * dropped here and appears only through [unverifiedRanges], so the reader can
     * see that the history has a gap instead of finding it silently missing.
     */
    fun papersFor(examId: ExamId, includeEveryYear: Boolean = false): List<ExamYearPaper> {
        val schemes = schemesFor(examId).filter { it.isVerified }
        return if (includeEveryYear) {
            schemes.flatMap { scheme ->
                val last = minOf(scheme.lastYear, currentYear)
                (scheme.fromYear..last).map {
                    ExamYearPaper(examId, it, scheme, EXAM_PAPER_QUESTIONS)
                }
            }
        } else {
            schemes.map { ExamYearPaper(examId, null, it, EXAM_PAPER_QUESTIONS) }
        }
    }

    /**
     * Spans an exam has but cannot confirm.
     *
     * Shown to the reader as a named gap. Hiding an unverified era would make the
     * archive look complete when it is not, which is the one thing this whole
     * feature must not be.
     */
    fun unverifiedRanges(examId: ExamId): List<ExamYearScheme> =
        schemesFor(examId).filterNot { it.isVerified }

    private companion object {

        // ---- SSC. Tier 1 awards two marks a question and deducts half a mark.
        val SSC_TIER_ONE = ExamDefinition(
            id = ExamId("ssc_cgl"),
            titleRes = R.string.exam_title_ssc_cgl,
            descriptionRes = R.string.exam_desc_ssc_cgl,
            paper = LocalizedText.of("Tier 1", "टियर 1"),
            section = LocalizedText.of("Quantitative Aptitude", "संख्यात्मक अभिरुचि"),
            realSectionQuestionCount = 25,
            marksPerCorrectInTwelfths = 24,
            negativeMarkInTwelfths = 6,
            paperDurationMinutes = 60,
            sectionDurationMinutes = 0,
            hasSectionalTimer = false,
            difficultyMix = mapOf(
                QuestionDifficulty.EASY to 0.25,
                QuestionDifficulty.MEDIUM to 0.45,
                QuestionDifficulty.HARD to 0.30
            )
        )

        val SSC_CHSL = ExamDefinition(
            id = ExamId("ssc_chsl"),
            titleRes = R.string.exam_title_ssc_chsl,
            descriptionRes = R.string.exam_desc_ssc_chsl,
            paper = LocalizedText.of("Tier 1", "टियर 1"),
            section = LocalizedText.of("Quantitative Aptitude", "संख्यात्मक अभिरुचि"),
            realSectionQuestionCount = 25,
            marksPerCorrectInTwelfths = 24,
            negativeMarkInTwelfths = 6,
            paperDurationMinutes = 60,
            // CHSL times each section on its own even though CGL does not, so
            // the two are not interchangeable and are not described as if they
            // were.
            sectionDurationMinutes = 15,
            hasSectionalTimer = true,
            difficultyMix = mapOf(
                QuestionDifficulty.EASY to 0.30,
                QuestionDifficulty.MEDIUM to 0.45,
                QuestionDifficulty.HARD to 0.25
            )
        )

        val SSC_GD = ExamDefinition(
            id = ExamId("ssc_gd"),
            titleRes = R.string.exam_title_ssc_gd,
            descriptionRes = R.string.exam_desc_ssc_gd,
            paper = LocalizedText.of("CBT", "सीबीटी"),
            section = LocalizedText.of("Elementary Mathematics", "प्राथमिक गणित"),
            realSectionQuestionCount = 20,
            marksPerCorrectInTwelfths = 24,
            negativeMarkInTwelfths = 3,
            paperDurationMinutes = 60,
            sectionDurationMinutes = 0,
            hasSectionalTimer = false,
            difficultyMix = mapOf(
                QuestionDifficulty.EASY to 0.50,
                QuestionDifficulty.MEDIUM to 0.40,
                QuestionDifficulty.HARD to 0.10
            )
        )

        /**
         * Held back deliberately. The published MTS pattern has been revised
         * more than once and the sources disagree, so rather than print a
         * marking scheme nobody has checked, the exam is listed as unavailable
         * and no paper is offered.
         */
        val SSC_MTS = ExamDefinition(
            id = ExamId("ssc_mts"),
            titleRes = R.string.exam_title_ssc_mts,
            descriptionRes = R.string.exam_desc_ssc_mts,
            paper = LocalizedText.of("CBT", "सीबीटी"),
            section = LocalizedText.of("Arithmetical Ability", "अंकगणितीय योग्यता"),
            realSectionQuestionCount = 0,
            marksPerCorrectInTwelfths = 0,
            negativeMarkInTwelfths = 0,
            paperDurationMinutes = 0,
            sectionDurationMinutes = 0,
            hasSectionalTimer = false,
            difficultyMix = emptyMap(),
            isAvailable = false
        )

        // ---- Railway. Both deduct exactly one third, which is four twelfths.
        val RRB_NTPC = ExamDefinition(
            id = ExamId("rrb_ntpc"),
            titleRes = R.string.exam_title_rrb_ntpc,
            descriptionRes = R.string.exam_desc_rrb_ntpc,
            paper = LocalizedText.of("CBT 1", "सीबीटी 1"),
            section = LocalizedText.of("Mathematics", "गणित"),
            realSectionQuestionCount = 30,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 4,
            paperDurationMinutes = 90,
            sectionDurationMinutes = 0,
            hasSectionalTimer = false,
            difficultyMix = mapOf(
                QuestionDifficulty.EASY to 0.20,
                QuestionDifficulty.MEDIUM to 0.45,
                QuestionDifficulty.HARD to 0.35
            )
        )

        val RRB_GROUP_D = ExamDefinition(
            id = ExamId("rrb_group_d"),
            titleRes = R.string.exam_title_rrb_group_d,
            descriptionRes = R.string.exam_desc_rrb_group_d,
            paper = LocalizedText.of("CBT", "सीबीटी"),
            section = LocalizedText.of("Mathematics", "गणित"),
            // RRB calls the section split "indicative" in its own notification
            // and says the actual paper may vary slightly, so 25 is the nominal
            // figure rather than a fixed guarantee.
            realSectionQuestionCount = 25,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 4,
            paperDurationMinutes = 90,
            sectionDurationMinutes = 0,
            hasSectionalTimer = false,
            difficultyMix = mapOf(
                QuestionDifficulty.EASY to 0.40,
                QuestionDifficulty.MEDIUM to 0.45,
                QuestionDifficulty.HARD to 0.15
            )
        )

        // ---- Banking. Both prelims time each section separately at twenty
        // minutes, which is the one structural difference from every paper
        // above.
        val IBPS_PO = ExamDefinition(
            id = ExamId("ibps_po"),
            titleRes = R.string.exam_title_ibps_po,
            descriptionRes = R.string.exam_desc_ibps_po,
            paper = LocalizedText.of("Prelims", "प्रीलिम्स"),
            section = LocalizedText.of("Quantitative Aptitude", "संख्यात्मक अभिरुचि"),
            realSectionQuestionCount = 35,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 3,
            paperDurationMinutes = 60,
            sectionDurationMinutes = 20,
            hasSectionalTimer = true,
            difficultyMix = mapOf(
                QuestionDifficulty.EASY to 0.35,
                QuestionDifficulty.MEDIUM to 0.50,
                QuestionDifficulty.HARD to 0.15
            )
        )

        val IBPS_CLERK = ExamDefinition(
            id = ExamId("ibps_clerk"),
            titleRes = R.string.exam_title_ibps_clerk,
            descriptionRes = R.string.exam_desc_ibps_clerk,
            paper = LocalizedText.of("Prelims", "प्रीलिम्स"),
            section = LocalizedText.of("Numerical Ability", "संख्यात्मक क्षमता"),
            realSectionQuestionCount = 35,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 3,
            paperDurationMinutes = 60,
            sectionDurationMinutes = 20,
            hasSectionalTimer = true,
            difficultyMix = mapOf(
                QuestionDifficulty.EASY to 0.45,
                QuestionDifficulty.MEDIUM to 0.45,
                QuestionDifficulty.HARD to 0.10
            )
        )

        // ---- Scheme history ------------------------------------------------
        //
        // Each entry is a span, not a year, because the schemes have mostly held
        // for years and restating an unchanged row twelve times would give a
        // future correction twelve places to land inconsistently.
        //
        // Every figure below was checked against the Commission's or the Board's
        // own published notice rather than a coaching-site summary, and the
        // source is named in the comment on the row it applies to.

        /** SSC CGL Tier 1. Tier 1 became computer based and 100 questions in
         *  2016 with a 75 minute allowance; the 60 minute allowance is in the
         *  2018 notice and has not moved since. */
        val SCHEME_CGL_2016 = ExamYearScheme(
            examId = ExamId("ssc_cgl"),
            fromYear = 2016,
            toYear = 2016,
            sectionQuestionCount = 25,
            marksPerCorrectInTwelfths = 24,
            negativeMarkInTwelfths = 6,
            paperDurationMinutes = 75,
            sectionDurationMinutes = 0,
            hasSectionalTimer = false
        )

        val SCHEME_CGL_FROM_2017 = ExamYearScheme(
            examId = ExamId("ssc_cgl"),
            fromYear = 2017,
            toYear = null,
            sectionQuestionCount = 25,
            marksPerCorrectInTwelfths = 24,
            negativeMarkInTwelfths = 6,
            paperDurationMinutes = 60,
            sectionDurationMinutes = 0,
            hasSectionalTimer = false
        )

        /** SSC CHSL Tier 1, which replaced the Data Entry and LSA examination.
         *  Unchanged since it began: 25 questions at two marks with half a mark
         *  deducted, and each section timed at fifteen minutes. */
        val SCHEME_CHSL = ExamYearScheme(
            examId = ExamId("ssc_chsl"),
            fromYear = 2018,
            toYear = null,
            sectionQuestionCount = 25,
            marksPerCorrectInTwelfths = 24,
            negativeMarkInTwelfths = 6,
            paperDurationMinutes = 60,
            sectionDurationMinutes = 15,
            hasSectionalTimer = true
        )

        /** SSC GD Constable before the 2022 change: 100 questions worth 100
         *  marks in 90 minutes, one mark a question. No notice giving this
         *  era's penalty could be confirmed, so the era is not offered as a
         *  paper rather than guessing at a figure a reader would score against. */
        val SCHEME_GD_EARLY = ExamYearScheme(
            examId = ExamId("ssc_gd"),
            fromYear = 2018,
            toYear = 2021,
            sectionQuestionCount = 25,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 0,
            paperDurationMinutes = 90,
            sectionDurationMinutes = 0,
            hasSectionalTimer = false,
            isVerified = false
        )

        /** SSC GD Constable from 2022. Confirmed against the SSC Constable (GD)
         *  notice on ssc.gov.in, which gives 80 questions carrying 2 marks each,
         *  Elementary Mathematics at 20 of those 80, and "a negative marking of
         *  0.25 marks for each wrong answer". */
        val SCHEME_GD_FROM_2022 = ExamYearScheme(
            examId = ExamId("ssc_gd"),
            fromYear = 2022,
            toYear = null,
            sectionQuestionCount = 20,
            marksPerCorrectInTwelfths = 24,
            negativeMarkInTwelfths = 3,
            paperDurationMinutes = 60,
            sectionDurationMinutes = 0,
            hasSectionalTimer = false
        )

        /** RRB NTPC CBT-1: stable across the whole of its life, 30 mathematics
         *  questions in a 100 question test with one third of a mark deducted. */
        val SCHEME_NTPC = ExamYearScheme(
            examId = ExamId("rrb_ntpc"),
            fromYear = 2015,
            toYear = null,
            sectionQuestionCount = 30,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 4,
            paperDurationMinutes = 90,
            sectionDurationMinutes = 0,
            hasSectionalTimer = false
        )

        /** RRB Group D, first held in 2018 and renamed Level 1 in 2022 without a
         *  change to the CBT. RRB's own notice calls the section split indicative
         *  and says the actual paper may vary, so the counts are nominal. */
        val SCHEME_GROUP_D = ExamYearScheme(
            examId = ExamId("rrb_group_d"),
            fromYear = 2018,
            toYear = null,
            sectionQuestionCount = 25,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 4,
            paperDurationMinutes = 90,
            sectionDurationMinutes = 0,
            hasSectionalTimer = false
        )

        /** IBPS PO Prelims before the 2025 revision, when the quantitative
         *  aptitude section was worth 35 marks rather than 30. */
        val SCHEME_IBPS_PO_EARLY = ExamYearScheme(
            examId = ExamId("ibps_po"),
            fromYear = 2015,
            toYear = 2024,
            sectionQuestionCount = 35,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 3,
            paperDurationMinutes = 60,
            sectionDurationMinutes = 20,
            hasSectionalTimer = true
        )

        /** IBPS PO Prelims from 2025, when the 2025 notification cut the
         *  quantitative aptitude section from 35 marks to 30. Thirty-five
         *  questions no longer divide evenly into the marks, so the per-question
         *  figure here is the practice convention rather than the published one
         *  and the card says the section is not evenly weighted. */
        val SCHEME_IBPS_PO_FROM_2025 = ExamYearScheme(
            examId = ExamId("ibps_po"),
            fromYear = 2025,
            toYear = null,
            sectionQuestionCount = 35,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 3,
            paperDurationMinutes = 60,
            sectionDurationMinutes = 20,
            hasSectionalTimer = true
        )

        val SCHEME_IBPS_CLERK = ExamYearScheme(
            examId = ExamId("ibps_clerk"),
            fromYear = 2015,
            toYear = null,
            sectionQuestionCount = 35,
            marksPerCorrectInTwelfths = 12,
            negativeMarkInTwelfths = 3,
            paperDurationMinutes = 60,
            sectionDurationMinutes = 20,
            hasSectionalTimer = true
        )

        val YEAR_SCHEMES = listOf(
            SCHEME_CGL_2016,
            SCHEME_CGL_FROM_2017,
            SCHEME_CHSL,
            SCHEME_GD_EARLY,
            SCHEME_GD_FROM_2022,
            SCHEME_NTPC,
            SCHEME_GROUP_D,
            SCHEME_IBPS_PO_EARLY,
            SCHEME_IBPS_PO_FROM_2025,
            SCHEME_IBPS_CLERK
        )

        val EXAMS = listOf(
            SSC_TIER_ONE,
            SSC_CHSL,
            SSC_GD,
            SSC_MTS,
            RRB_NTPC,
            RRB_GROUP_D,
            IBPS_PO,
            IBPS_CLERK
        )
    }
}
/**
 * The current calendar year, read the same way the rest of the app reads dates.
 *
 * [com.atlabs.percentpro.model.LocalDateKey] deliberately avoids `java.time`
 * because minSdk is 23 and this module has no core library desugaring, so
 * [java.util.Calendar] it is. Injectable through [ExamCatalog]'s constructor so
 * the year list can be tested without depending on when the suite runs.
 */
private fun systemYear(): Int =
    java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
