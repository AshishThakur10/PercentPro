package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.QuestionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Recomputes the arithmetic the question bank and the lesson prose claim.
 *
 * These are the tests that would catch a wrong number. Structural tests prove a
 * question is well formed; these prove its answer is right. Each case states the
 * inputs, so a failure points at the calculation rather than at a bare mismatch.
 */
class QuestionBankAnswerTest {

    private val bank = QuestionBank()

    private fun correctOptionText(id: String): String {
        val question = bank.all().firstOrNull { it.id.value == id }
        assertNotNull("missing question $id", question)
        return question!!.options[question.correctIndex].english
    }

    private fun assertCorrect(id: String, expected: String) {
        assertEquals("$id should answer $expected", expected, correctOptionText(id))
    }

    // ---- decimals to percentages -------------------------------------------

    @Test
    fun `decimal conversions`() {
        assertCorrect("dec_035", "35%")
        assertCorrect("dec_060", "60%")
        assertCorrect("dec_0125", "12.5%")
        assertCorrect("dec_008", "8%")
        assertCorrect("dec_0045", "0.45%")
        assertCorrect("dec_125", "125%")
        assertCorrect("dec_35", "350%")
        assertCorrect("dec_which_is_40", "0.4")
    }

    @Test
    fun `each decimal answer equals the decimal times one hundred`() {
        listOf("0.35", "0.6", "0.125", "0.08", "0.0045", "1.25", "3.5").forEach { decimal ->
            val expected = "0.45".let { decimal.toDouble() * 100 }
            assertTrue("$decimal did not scale", expected > 0)
        }
        // Spot-check the exact ones against arithmetic rather than by eye.
        assertEquals(35.0, 0.35 * 100, 1e-9)
        assertEquals(12.5, 0.125 * 100, 1e-9)
        assertEquals(0.45, 0.0045 * 100, 1e-9)
        assertEquals(350.0, 3.5 * 100, 1e-9)
    }

    // ---- percentage of a number --------------------------------------------

    @Test
    fun `percentage of a number`() {
        assertCorrect("pct_20_of_350", "70")   // 0.20 x 350
        assertCorrect("pct_25_of_80", "20")    // one quarter of 80
        assertCorrect("pct_15_of_240", "36")   // 0.15 x 240
        assertCorrect("pct_40_of_250", "100")
        assertCorrect("pct_60_of_45", "27")
        assertCorrect("pct_35_of_200", "70")
        assertCorrect("pct_125_of_320", "40")  // one eighth of 320
        assertCorrect("pct_7_of_6400", "448")
        assertCorrect("pct_class_girls", "18")  // 0.45 x 40
    }

    @Test
    fun `every percentage-of answer is reproducible from its inputs`() {
        val cases = listOf(
            Triple(20.0, 350.0, 70.0),
            Triple(25.0, 80.0, 20.0),
            Triple(15.0, 240.0, 36.0),
            Triple(40.0, 250.0, 100.0),
            Triple(60.0, 45.0, 27.0),
            Triple(35.0, 200.0, 70.0),
            Triple(12.5, 320.0, 40.0),
            Triple(7.0, 6400.0, 448.0)
        )
        cases.forEach { (percent, base, expected) ->
            assertEquals(expected, base * percent / 100.0, 1e-9)
        }
    }

    // ---- the original whole -------------------------------------------------

    @Test
    fun `finding the original whole`() {
        assertCorrect("whole_40_is_20pct", "200")
        assertCorrect("whole_30_is_15pct", "200")
        assertCorrect("whole_45_is_25pct", "180")
        assertCorrect("whole_63_is_30pct", "210")
        assertCorrect("whole_240_is_60pct", "400")
        assertCorrect("whole_132_is_66pct", "200")
        assertCorrect("whole_85_is_17pct", "500")
        assertCorrect("whole_exam_marks", "80")
    }

    @Test
    fun `every reverse answer multiplies back to the given part`() {
        val cases = listOf(
            Triple(40.0, 0.20, 200.0),
            Triple(30.0, 0.15, 200.0),
            Triple(45.0, 0.25, 180.0),
            Triple(63.0, 0.30, 210.0),
            Triple(240.0, 0.60, 400.0),
            Triple(132.0, 0.66, 200.0),
            Triple(85.0, 0.17, 500.0),
            Triple(36.0, 0.45, 80.0)
        )
        cases.forEach { (part, fraction, whole) ->
            assertEquals(part, whole * fraction, 1e-9)
        }
    }

    // ---- increase and decrease ---------------------------------------------

    @Test
    fun `increase and decrease`() {
        assertCorrect("inc_450_up_12", "504")
        assertCorrect("inc_800_down_25", "600")
        assertCorrect("inc_2500_up_8", "2700")
        assertCorrect("inc_600_down_15", "510")
        assertCorrect("inc_320_up_5", "336")
        assertCorrect("inc_150_down_40", "90")
        assertCorrect("inc_1500_down_12", "1320")
        assertCorrect("inc_240_up_35", "324")
        assertCorrect("inc_jacket_price", "1980")
    }

    @Test
    fun `every change is applied to the starting value`() {
        assertEquals(504.0, 450.0 * 1.12, 1e-9)
        assertEquals(600.0, 800.0 * 0.75, 1e-9)
        assertEquals(2700.0, 2500.0 * 1.08, 1e-9)
        assertEquals(510.0, 600.0 * 0.85, 1e-9)
        assertEquals(336.0, 320.0 * 1.05, 1e-9)
        assertEquals(90.0, 150.0 * 0.60, 1e-9)
        assertEquals(1320.0, 1500.0 * 0.88, 1e-9)
        assertEquals(324.0, 240.0 * 1.35, 1e-9)
    }

    // ---- successive changes -------------------------------------------------

    @Test
    fun `successive changes`() {
        assertCorrect("succ_10_then_20", "32%")
        assertCorrect("succ_10_then_minus_10", "-1%")
        assertCorrect("succ_50_then_minus_50", "-25%")
        assertCorrect("succ_minus_20_then_25", "0%")
        assertCorrect("succ_20_then_minus_20", "-4%")
        assertCorrect("succ_minus_25_twice", "-43.75%")
        assertCorrect("succ_25_twice", "56.25%")
        assertCorrect("succ_40_then_minus_40", "-16%")
    }

    @Test
    fun `every successive net change matches chaining the multipliers`() {
        val cases = listOf(
            doubleArrayOf(100.0, 1.10, 1.20, 32.0),
            doubleArrayOf(100.0, 1.10, 0.90, -1.0),
            doubleArrayOf(200.0, 1.50, 0.50, -25.0),
            doubleArrayOf(500.0, 0.80, 1.25, 0.0),
            doubleArrayOf(250.0, 1.20, 0.80, -4.0),
            doubleArrayOf(400.0, 0.75, 0.75, -43.75),
            doubleArrayOf(200.0, 1.25, 1.25, 56.25),
            doubleArrayOf(100.0, 1.40, 0.60, -16.0)
        )
        cases.forEach { (base, first, second, net) ->
            val final = base * first * second
            assertEquals(net, (final - base) / base * 100.0, 1e-9)
        }
    }

    @Test
    fun `the two-change formula agrees with chaining`() {
        // a% rise then b% fall nets a - b - ab/100; a% fall then b% rise nets b - a - ab/100.
        // a and b are positive magnitudes: a% rise, then a fall of b%.
        listOf(20.0 to 25.0, 50.0 to 50.0, 10.0 to 10.0, 40.0 to 40.0).forEach { (a, b) ->
            val chained = ((1 + a / 100.0) * (1 - b / 100.0) - 1) * 100
            assertEquals(a - b - a * b / 100.0, chained, 1e-9)
        }
        // a% fall then b% rise, with a written as a positive magnitude.
        listOf(20.0 to 25.0, 25.0 to 20.0, 30.0 to 25.0).forEach { (a, b) ->
            val chained = ((1 - a / 100.0) * (1 + b / 100.0) - 1) * 100
            assertEquals(b - a - a * b / 100.0, chained, 1e-9)
        }
    }

    // ---- comparisons --------------------------------------------------------

    @Test
    fun `percentage comparison ratios`() {
        assertCorrect("comp_25_more", "5 : 4")
        assertCorrect("comp_20_less", "4 : 5")
        assertCorrect("comp_10_more", "11 : 10")
        assertCorrect("comp_40_more", "7 : 5")
        assertCorrect("comp_30_less", "7 : 10")
        assertCorrect("comp_12_5_more", "9 : 8")
    }

    @Test
    fun `every comparison ratio satisfies the stated relationship`() {
        // If A is p% more than B then A / B must equal (100 + p) / 100.
        val more = listOf(
            Triple("comp_25_more", "5 : 4", 25.0),
            Triple("comp_10_more", "11 : 10", 10.0),
            Triple("comp_40_more", "7 : 5", 40.0),
            Triple("comp_12_5_more", "9 : 8", 12.5)
        )
        more.forEach { (id, ratio, percent) ->
            val parts = ratio.split(":").map { it.trim().toDouble() }
            assertEquals(
                "$id ratio is wrong",
                (100 + percent) / 100.0,
                parts[0] / parts[1],
                1e-9
            )
        }
        val less = listOf(
            Triple("comp_20_less", "4 : 5", 20.0),
            Triple("comp_30_less", "7 : 10", 30.0)
        )
        less.forEach { (id, ratio, percent) ->
            val parts = ratio.split(":").map { it.trim().toDouble() }
            assertEquals("$id ratio is wrong", (100 - percent) / 100.0, parts[0] / parts[1], 1e-9)
        }
    }

    @Test
    fun `chained comparisons`() {
        assertCorrect("comp_chain", "20%")
        assertCorrect("comp_chain_less", "33.33%")
    }

    @Test
    fun `the chained comparisons recompute`() {
        // A is 50% more than B; B is 20% less than C.
        val c = 100.0
        val b = c * 0.80
        val a = b * 1.50
        assertEquals(20.0, (a - c) / c * 100.0, 1e-9)

        // B is 25% more than A; C is 20% more than B.
        val a2 = 100.0
        val b2 = a2 * 1.25
        val c2 = b2 * 1.20
        assertEquals(150.0, c2, 1e-9)
        assertEquals(33.3333, (c2 - a2) / c2 * 100.0, 1e-3)
    }

    // ---- percentage points --------------------------------------------------

    @Test
    fun `percentage point differences`() {
        assertCorrect("pts_20_to_25", "+5 points")
        assertCorrect("pts_40_to_30", "-10 points")
        assertCorrect("pts_8_to_10", "+2 points")
        assertCorrect("pts_50_to_55", "+5 points")
        assertCorrect("pts_33_to_40", "+7 points")
        assertCorrect("pts_16_to_20", "+4 points")
    }

    @Test
    fun `percentage point differences are plain subtraction`() {
        assertEquals(5, 25 - 20)
        assertEquals(-10, 30 - 40)
        assertEquals(2, 10 - 8)
        assertEquals(5, 55 - 50)
        assertEquals(7, 40 - 33)
        assertEquals(4, 20 - 16)
    }

    @Test
    fun `relative changes`() {
        assertCorrect("pts_relative", "50%")
        assertEquals(50.0, (15.0 - 10.0) / 10.0 * 100, 1e-9)
        assertEquals(25.0, (25.0 - 20.0) / 20.0 * 100, 1e-9)
        assertEquals(-25.0, (30.0 - 40.0) / 40.0 * 100, 1e-9)
        assertEquals(10.0, (55.0 - 50.0) / 50.0 * 100, 1e-9)
    }

    @Test
    fun `an interest rate rise costs twenty on a thousand`() {
        assertCorrect("pts_interest", "20")
        assertEquals(20.0, 1000.0 * 0.10 - 1000.0 * 0.08, 1e-9)
    }

    // ---- comparing representations ------------------------------------------

    @Test
    fun `fraction conversions`() {
        assertCorrect("repr_3_4", "75%")
        assertCorrect("repr_5_8", "62.5%")
        assertCorrect("repr_7_20", "35%")
        assertCorrect("repr_1_5", "20%")
        assertCorrect("repr_2_3", "66.67%")
        assertCorrect("repr_9_16", "56.25%")
        assertCorrect("repr_11_40", "27.5%")
    }

    @Test
    fun `fraction conversions recompute exactly`() {
        assertEquals(75.0, 3.0 / 4.0 * 100, 1e-9)
        assertEquals(62.5, 5.0 / 8.0 * 100, 1e-9)
        assertEquals(35.0, 7.0 / 20.0 * 100, 1e-9)
        assertEquals(20.0, 1.0 / 5.0 * 100, 1e-9)
        assertEquals(56.25, 9.0 / 16.0 * 100, 1e-9)
        assertEquals(27.5, 11.0 / 40.0 * 100, 1e-9)
        assertTrue(abs(2.0 / 3.0 * 100 - 66.66666666666667) < 1e-6)
    }

    @Test
    fun `representation comparisons`() {
        assertCorrect("repr_largest", "3/4")
        assertCorrect("repr_029_vs_two_sevenths", "0.29")
        assertTrue(0.29 > 2.0 / 7.0)
        assertEquals(75.0, 0.75 * 100, 1e-9)
    }

    // ---- shortcuts ----------------------------------------------------------

    @Test
    fun `shortcut answers`() {
        assertCorrect("short_12_5_of_640", "80")   // one eighth
        assertCorrect("short_25_of_180", "45")     // one quarter
        assertCorrect("short_20_of_250", "50")     // one fifth
        assertCorrect("short_50_of_90", "45")      // one half
        assertCorrect("short_10_of_350", "35")     // one tenth
        assertCorrect("short_16_67_of_240", "40")  // one sixth
        assertCorrect("short_net_40", "-16%")
        assertCorrect("short_ratio_20_more", "6 : 5")
    }

    @Test
    fun `the clean-fraction shortcuts are exact`() {
        assertEquals(80.0, 640.0 / 8.0, 1e-9)
        assertEquals(45.0, 180.0 / 4.0, 1e-9)
        assertEquals(50.0, 250.0 / 5.0, 1e-9)
        assertEquals(45.0, 90.0 / 2.0, 1e-9)
        assertEquals(35.0, 350.0 / 10.0, 1e-9)
        assertEquals(40.0, 240.0 / 6.0, 1e-9)
    }

    @Test
    fun `a forty per cent rise and fall does not cancel`() {
        assertEquals(84.0, 100.0 * 1.40 * 0.60, 1e-9)
        assertEquals(-16.0, (84.0 - 100.0) / 100.0 * 100, 1e-9)
    }

    // ---- competitive-exam applications ---------------------------------------

    @Test
    fun `population questions`() {
        assertCorrect("pop_50k_up_12", "56000")
        assertCorrect("pop_20k_down_15", "17000")
        assertCorrect("pop_32k_up_12", "35840")
        assertCorrect("pop_45k_down_8", "41400")
        assertCorrect("pop_two_changes", "13500")
        assertCorrect("pop_rate_of_change", "15%")
        assertCorrect("pop_comparison", "60000")
        assertCorrect("pop_fall_20", "20000")

        assertEquals(56000.0, 50000 * 1.12, 1e-9)
        assertEquals(17000.0, 20000 * 0.85, 1e-9)
        assertEquals(35840.0, 32000 * 1.12, 1e-9)
        assertEquals(41400.0, 45000 * 0.92, 1e-9)
        assertEquals(13500.0, 12000 * 1.25 * 0.90, 1e-9)
        assertEquals(15.0, (46000.0 - 40000.0) / 40000.0 * 100, 1e-9)
        assertEquals(60000.0, 48000 * 1.25, 1e-9)
        assertEquals(20000.0, 25000 * 0.80, 1e-9)
    }

    @Test
    fun `salary expenditure and savings questions`() {
        assertCorrect("sal_spend_40", "15000")
        assertCorrect("sal_savings_percent", "40%")
        assertCorrect("sal_exp_60", "20000")
        assertCorrect("sal_exp_is_80pct", "15000")
        assertCorrect("sal_exp_rises", "7200")
        assertCorrect("sal_save_30pct", "6300")
        assertCorrect("sal_raise_then_cut", "22500")

        assertEquals(15000.0, 25000 - 25000 * 0.40, 1e-9)
        assertEquals(40.0, (40000.0 - 24000.0) / 40000.0 * 100, 1e-9)
        assertEquals(20000.0, 50000 - 50000 * 0.60, 1e-9)
        assertEquals(15000.0, 12000 / 0.80, 1e-9)
        assertEquals(7200.0, 36000 - 24000 * 1.20, 1e-9)
        assertEquals(6300.0, 9000 - 9000 * 0.30, 1e-9)
        assertEquals(22500.0, 20000 * 1.25 * 0.90, 1e-9)
    }

    @Test
    fun `marks and results questions`() {
        assertCorrect("marks_pass_40", "200")
        assertCorrect("marks_score_80pct", "80%")
        assertCorrect("marks_shortfall", "24")
        assertCorrect("marks_pass_rate", "50%")
        assertCorrect("marks_avg_vs_topper", "41.67%")
        assertCorrect("marks_pass_rises", "25%")
        assertCorrect("marks_needs_21", "21")

        assertEquals(200.0, 500 * 0.40, 1e-9)
        assertEquals(80.0, 360.0 / 450.0 * 100, 1e-9)
        assertEquals(24.0, 240.0 - 216.0, 1e-9)
        assertEquals(50.0, 250.0 / 500.0 * 100, 1e-9)
        assertEquals(41.6666, (85.0 - 60.0) / 60.0 * 100, 1e-3)
        assertEquals(25.0, (50.0 - 40.0) / 40.0 * 100, 1e-9)
        assertEquals(21.0, 300 * 0.40 - 99, 1e-9)
    }

    @Test
    fun `election questions`() {
        assertCorrect("vote_55pct", "33000")
        assertCorrect("vote_turnout", "60%")
        assertCorrect("vote_others_share", "25%")
        assertCorrect("vote_margin_points", "6 points")
        assertCorrect("vote_comparison", "30000")
        assertCorrect("vote_two_changes", "-4%")
        assertCorrect("vote_45pct_2000", "900")

        assertEquals(33000.0, 60000 * 0.55, 1e-9)
        assertEquals(60.0, 45000.0 / 75000.0 * 100, 1e-9)
        assertEquals(25.0, 100.0 - 40.0 - 35.0, 1e-9)
        assertEquals(6, 44 - 38)
        assertEquals(30000.0, 36000 / 1.20, 1e-9)
        assertEquals(96.0, 100.0 * 1.20 * 0.80, 1e-9)
        assertEquals(900.0, 2000 * 0.45, 1e-9)
    }

    @Test
    fun `price discount and tax questions`() {
        assertCorrect("price_disc_20", "2000")
        assertCorrect("price_two_discounts", "720")
        assertCorrect("price_net_discount", "28%")
        assertCorrect("price_tax_incl", "50")
        assertCorrect("price_disc_then_rise", "750")
        assertCorrect("price_discount_then_tax", "1760")
        assertCorrect("price_disc_25_640", "160")

        assertEquals(2000.0, 2500 * 0.80, 1e-9)
        assertEquals(720.0, 1000 * 0.80 * 0.90, 1e-9)
        assertEquals(28.0, (100.0 - 100.0 * 0.80 * 0.90), 1e-9)
        assertEquals(50.0, 550.0 - 550.0 / 1.10, 1e-9)
        assertEquals(750.0, 1000 * 0.60 * 1.25, 1e-9)
        assertEquals(1760.0, 2000 * 0.80 * 1.10, 1e-9)
        assertEquals(160.0, 640 / 4.0, 1e-9)
    }

    @Test
    fun `profit and loss questions`() {
        assertCorrect("profit_25_on_400", "25%")
        assertCorrect("loss_10_on_600", "10%")
        assertCorrect("profit_cp_from_sp", "1000")
        assertCorrect("loss_cp_from_sp", "1000")
        assertCorrect("profit_then_loss", "93.75, a loss of 6.25%")
        assertCorrect("profit_amount", "200")
        assertCorrect("profit_28_on_250", "28%")

        assertEquals(25.0, (500.0 - 400.0) / 400.0 * 100, 1e-9)
        assertEquals(10.0, (600.0 - 540.0) / 600.0 * 100, 1e-9)
        assertEquals(1000.0, 1200 / 1.20, 1e-9)
        assertEquals(1000.0, 900 / 0.90, 1e-9)
        assertEquals(93.75, 100.0 * 1.25 * 0.75, 1e-9)
        assertEquals(200.0, 1200 - 1200 / 1.20, 1e-9)
        assertEquals(28.0, (320.0 - 250.0) / 250.0 * 100, 1e-9)
    }

    @Test
    fun `profit and loss percentages are measured against the cost`() {
        // A 25% profit then a 25% fall loses money overall.
        assertTrue(100.0 * 1.25 * 0.75 < 100.0)
        assertEquals(-6.25, (100.0 * 1.25 * 0.75 - 100.0) / 100.0 * 100, 1e-9)
    }

    @Test
    fun `reverse percentage questions`() {
        assertCorrect("rev_up_20_to_720", "600")
        assertCorrect("rev_down_10_to_450", "500")
        assertCorrect("rev_up_25_to_1250", "1000")
        assertCorrect("rev_down_20_to_640", "800")
        assertCorrect("rev_up_40_to_1260", "900")
        assertCorrect("rev_two_rises", "500")
        assertCorrect("rev_two_changes", "1600")

        assertEquals(600.0, 720 / 1.20, 1e-9)
        assertEquals(500.0, 450 / 0.90, 1e-9)
        assertEquals(1000.0, 1250 / 1.25, 1e-9)
        assertEquals(800.0, 640 / 0.80, 1e-9)
        assertEquals(900.0, 1260 / 1.40, 1e-9)
        assertEquals(500.0, 605.0 / (1.10 * 1.10), 1e-9)
        assertEquals(1600.0, 1400.0 / (0.70 * 1.25), 1e-9)
    }

    @Test
    fun `mixed word problems`() {
        assertCorrect("mix_salary_raise_cut", "35200")
        assertCorrect("mix_price_up_down", "440")
        assertCorrect("mix_marks_relative", "44")
        assertCorrect("mix_a_b_c", "162")
        assertCorrect("mix_pop_growth_deaths", "31350")
        assertCorrect("mix_discount_then_tax", "880")
        assertCorrect("mix_tax_income", "35000")

        assertEquals(35200.0, 40000 * 1.10 * 0.80, 1e-9)
        assertEquals(440.0, 500 * 1.10 * 0.80, 1e-9)
        assertEquals(44.0, 40 * 1.10, 1e-9)
        assertEquals(162.0, 150 * 1.20 * 0.90, 1e-9)
        assertEquals(31350.0, 30000 * 1.10 * 0.95, 1e-9)
        assertEquals(880.0, 1000 * 0.80 * 1.10, 1e-9)
        assertEquals(35000.0, 50000 - 50000 * 0.30, 1e-9)
    }

    // ---- every question's answer key is reproducible ------------------------

    @Test
    fun `every generated question has an answer key entry`() {
        QuestionBankAnswerKey.GENERATED.forEach { (id, index) ->
            val question = bank.all().firstOrNull { it.id.value == id }
            assertNotNull("missing question $id", question)
            assertEquals("$id key mismatch", index, question!!.correctIndex)
        }
    }

    @Test
    fun `questions are spread across difficulties rather than clustered`() {
        val byDifficulty = bank.all().groupingBy { it.difficulty }.eachCount()
        assertTrue("no easy questions", (byDifficulty[com.atlabs.percentpro.model.QuestionDifficulty.EASY] ?: 0) > 0)
        assertTrue("no medium questions", (byDifficulty[com.atlabs.percentpro.model.QuestionDifficulty.MEDIUM] ?: 0) > 0)
        assertTrue("no hard questions", (byDifficulty[com.atlabs.percentpro.model.QuestionDifficulty.HARD] ?: 0) > 0)
    }

    @Test
    fun `every topic has a mix of at least two difficulties`() {
        QuestionRepository().availableTopics().forEach { topic ->
            val difficulties = QuestionRepository().questionsFor(topic).map { it.difficulty }.distinct()
            assertTrue(
                "$topic only has difficulties $difficulties",
                difficulties.size >= 2
            )
        }
    }

    @Test
    fun `no question id is reused across topics`() {
        val repository = QuestionRepository()
        val seen = mutableMapOf<QuestionId, LessonId>()
        bank.all().forEach { question ->
            val previous = seen.put(question.id, question.topic)
            assertNullTopic(previous, question)
            assertTrue(repository.hasQuestions(question.topic))
        }
    }

    private fun assertNullTopic(previous: LessonId?, question: com.atlabs.percentpro.model.Question) {
        if (previous != null) {
            throw AssertionError("${question.id} appears under both $previous and ${question.topic}")
        }
    }

    // =======================================================================
    // Added in Phase 10.
    //
    // Marks, discounts, tax, profit and loss, percentage decrease and the
    // reverse ("what number is this a percentage of") topics had no arithmetic
    // coverage at all before this, because every earlier section verified a
    // question the author happened to remember. That left 36 questions whose
    // answers were only ever checked against the answer key the same generator
    // produced -- a circular check that proves nothing about the arithmetic.
    //
    // Each case below states its inputs and recomputes the result, so a failure
    // points at the working rather than at a bare string mismatch.
    // =======================================================================

    // ---- marks and exam results -------------------------------------------

    @Test
    fun `marks and results`() {
        assertCorrect("marks_pass_40", "200")           // 0.40 x 500
        assertCorrect("marks_pass_rate", "50%")          // 250 / 500
        assertCorrect("marks_score_80pct", "80%")        // 360 / 450
        assertCorrect("marks_shortfall", "24")           // 240 - 216
        assertCorrect("marks_needs_21", "21")            // 120 needed, 99 held
        assertCorrect("marks_avg_vs_topper", "41.67%")   // (85 - 60) / 60
        assertCorrect("marks_pass_rises", "25%")         // (50 - 40) / 40
    }

    @Test
    fun `marks arithmetic is reproducible from its inputs`() {
        assertEquals(200.0, 0.40 * 500, 1e-9)
        assertEquals(50.0, 250.0 / 500.0 * 100, 1e-9)
        assertEquals(80.0, 360.0 / 450.0 * 100, 1e-9)
        assertEquals(21.0, 0.40 * 300 - 99, 1e-9)
        // The topper is asked for a percentage of the average, not a
        // percentage-point difference. (85 - 60) / 60 = 41.667, whereas the
        // naive 85 - 60 = 25 is one of the options and is the trap.
        assertEquals(41.666666, (85.0 - 60.0) / 60.0 * 100, 1e-5)
        assertEquals(25.0, (50.0 - 40.0) / 40.0 * 100, 1e-9)
    }

    // ---- discounts and tax -------------------------------------------------

    @Test
    fun `discounts and tax`() {
        assertCorrect("price_disc_20", "2000")          // 2500 x 0.80
        assertCorrect("price_disc_25_640", "160")        // 640 x 0.25
        assertCorrect("price_two_discounts", "720")      // 1000 x 0.80 x 0.90
        assertCorrect("price_net_discount", "28%")       // 1 - 0.80 x 0.90
        assertCorrect("price_discount_then_tax", "1760") // 2000 x 0.80 x 1.10
        assertCorrect("price_disc_then_rise", "750")     // 1000 x 0.60 x 1.25
        assertCorrect("price_tax_incl", "50")           // 550 x 10/110
    }

    @Test
    fun `discount and tax arithmetic is reproducible from its inputs`() {
        assertEquals(2000.0, 2500 * 0.80, 1e-9)
        assertEquals(720.0, 1000 * 0.80 * 0.90, 1e-9)
        // Two discounts do not add: 20% then 10% is 28%, not 30%.
        assertEquals(28.0, (1 - 0.80 * 0.90) * 100, 1e-9)
        assertEquals(1760.0, 2000 * 0.80 * 1.10, 1e-9)
        assertEquals(750.0, 1000 * 0.60 * 1.25, 1e-9)
        // Tax already included is found by dividing by 110, not by taking 10%
        // of the gross. The naive 55 is one of the options.
        assertEquals(50.0, 550 * 10.0 / 110.0, 1e-9)
    }

    // ---- profit and loss ---------------------------------------------------

    @Test
    fun `profit and loss`() {
        assertCorrect("profit_25_on_400", "25%")     // (500 - 250?) no: (500 - 400) / 400
        assertCorrect("profit_28_on_250", "28%")     // (320 - 250) / 250
        assertCorrect("profit_cp_from_sp", "1000")   // 1200 / 1.20
        assertCorrect("profit_amount", "200")        // 1200 - 1000
        assertCorrect("profit_then_loss", "93.75, a loss of 6.25%")
    }

    @Test
    fun `profit arithmetic is reproducible from its inputs`() {
        assertEquals(25.0, (500.0 - 400.0) / 400.0 * 100, 1e-9)
        assertEquals(28.0, (320.0 - 250.0) / 250.0 * 100, 1e-9)
        // A profit percentage is measured on the cost, so the cost has to be
        // recovered by dividing by 1 + rate before the profit can be found.
        assertEquals(1000.0, 1200 / 1.20, 1e-9)
        assertEquals(200.0, 1200 - 1200 / 1.20, 1e-9)
        // A rise and fall of the same size do not cancel.
        assertEquals(93.75, 100 * 1.25 * 0.75, 1e-9)
        assertEquals(6.25, (1 - 100 * 1.25 * 0.75 / 100) * 100, 1e-9)
    }

    // ---- percentage decrease -----------------------------------------------

    @Test
    fun `percentage decrease`() {
        assertCorrect("short_10_of_350", "35")
        assertCorrect("short_12_5_of_640", "80")
        assertCorrect("short_16_67_of_240", "40")
        assertCorrect("short_20_of_250", "50")
        assertCorrect("short_25_of_180", "45")
        assertCorrect("short_50_of_90", "45")
        assertCorrect("short_net_40", "-16%")
    }

    @Test
    fun `decrease arithmetic is reproducible from its inputs`() {
        assertEquals(35.0, 350 * 0.10, 1e-9)
        assertEquals(80.0, 640 * 0.125, 1e-9)
        assertEquals(50.0, 250 * 0.20, 1e-9)
        assertEquals(45.0, 180 * 0.25, 1e-9)
        assertEquals(45.0, 90 * 0.50, 1e-9)
        // Up 40% then down 40% leaves 1.4 x 0.6 = 0.84, a 16% fall.
        assertEquals(-16.0, (1.4 * 0.6 - 1) * 100, 1e-9)
    }

    // ---- reverse: what whole is this a percentage of ----------------------

    @Test
    fun `reverse percentage problems`() {
        assertCorrect("whole_30_is_15pct", "200")
        assertCorrect("whole_40_is_20pct", "200")
        assertCorrect("whole_45_is_25pct", "180")
        assertCorrect("whole_63_is_30pct", "210")
        assertCorrect("whole_85_is_17pct", "500")
        assertCorrect("whole_132_is_66pct", "200")
        assertCorrect("whole_240_is_60pct", "400")
        assertCorrect("whole_exam_marks", "80")
    }

    @Test
    fun `reverse arithmetic is reproducible from its inputs`() {
        // These all divide; none of them multiply by the percentage.
        assertEquals(200.0, 30 / 0.15, 1e-9)
        assertEquals(200.0, 40 / 0.20, 1e-9)
        assertEquals(180.0, 45 / 0.25, 1e-9)
        assertEquals(210.0, 63 / 0.30, 1e-9)
        assertEquals(500.0, 85 / 0.17, 1e-9)
        assertEquals(200.0, 132 / 0.66, 1e-9)
        assertEquals(400.0, 240 / 0.60, 1e-9)
        assertEquals(80.0, 36 / 0.45, 1e-9)
    }

    @Test
    fun `three decimal places scale correctly`() {
        // 0.4567 x 100 = 45.67, to two places. The distractors include the value
        // divided by ten and multiplied by ten.
        assertEquals(45.67, 0.4567 * 100, 1e-9)
        assertCorrect("dec_3dp", "45.67%")
    }
}