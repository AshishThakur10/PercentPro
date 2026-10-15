package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalizedText
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.QuestionDifficulty
import com.atlabs.percentpro.model.QuestionId

/**
 * The offline question bank for the practice engine.
 *
 * Ordering is fixed so a session is reproducible and a question's position is
 * stable. Identifiers are explicit strings rather than list positions, so
 * reordering or inserting questions never changes what a learner already
 * answered.
 *
 * Every question is covered by [QuestionBankTest], which checks the answer key,
 * option uniqueness and that both translations are present, so a typo in an
 * answer index fails the build rather than shipping.
 */
class QuestionBank {

    fun all(): List<Question> = QUESTIONS

    fun questionsFor(topic: LessonId): List<Question> = QUESTIONS.filter { it.topic == topic }

    fun question(topic: LessonId, id: QuestionId): Question? =
        QUESTIONS.firstOrNull { it.topic == topic && it.id == id }

    fun topicsWithQuestions(): List<LessonId> =
        QUESTIONS.map { it.topic }.distinct()

    private companion object {

        fun q(
            id: String,
            topic: LessonId,
            difficulty: QuestionDifficulty,
            en: String,
            hi: String,
            options: List<Pair<String, String>>,
            correctIndex: Int,
            explanationEn: String,
            explanationHi: String
        ) = Question(
            id = QuestionId(id),
            topic = topic,
            difficulty = difficulty,
            prompt = LocalizedText.of(en, hi),
            options = options.map { LocalizedText.of(it.first, it.second) },
            correctIndex = correctIndex,
            explanation = LocalizedText.of(explanationEn, explanationHi)
        )

        private val E = QuestionDifficulty.EASY
        private val M = QuestionDifficulty.MEDIUM
        private val H = QuestionDifficulty.HARD

        val WHAT_IS_A_PERCENTAGE = LessonId.WHAT_IS_A_PERCENTAGE
        val PERCENTAGES_VISUALLY = LessonId.PERCENTAGES_VISUALLY
        val FRACTIONS_TO_PERCENTAGES = LessonId.FRACTIONS_TO_PERCENTAGES

        // Added in Phase 6. These identifiers are new, so nothing saved before
        // this phase can refer to them; the three above keep their original keys.
        val DECIMALS_TO_PERCENTAGES = LessonId.DECIMALS_TO_PERCENTAGES
        val PERCENTAGE_OF_A_NUMBER = LessonId.PERCENTAGE_OF_A_NUMBER
        val ORIGINAL_WHOLE = LessonId.ORIGINAL_WHOLE
        val PERCENTAGE_INCREASE_DECREASE = LessonId.PERCENTAGE_INCREASE_DECREASE
        val SUCCESSIVE_PERCENTAGE_CHANGES = LessonId.SUCCESSIVE_PERCENTAGE_CHANGES
        val PERCENTAGE_COMPARISON = LessonId.PERCENTAGE_COMPARISON
        val PERCENTAGE_POINTS_VS_CHANGE = LessonId.PERCENTAGE_POINTS_VS_CHANGE
        val COMPARING_REPRESENTATIONS = LessonId.COMPARING_REPRESENTATIONS
        val EXAM_SHORTCUTS = LessonId.EXAM_SHORTCUTS
        val POPULATION_CHANGE = LessonId.POPULATION_CHANGE
        val SALARY_EXPENDITURE_SAVINGS = LessonId.SALARY_EXPENDITURE_SAVINGS
        val MARKS_AND_RESULTS = LessonId.MARKS_AND_RESULTS
        val ELECTION_VOTING = LessonId.ELECTION_VOTING
        val PRICE_DISCOUNT_TAX = LessonId.PRICE_DISCOUNT_TAX
        val PROFIT_LOSS = LessonId.PROFIT_LOSS
        val REVERSE_PERCENTAGE = LessonId.REVERSE_PERCENTAGE
        val MIXED_WORD_PROBLEMS = LessonId.MIXED_WORD_PROBLEMS

        val QUESTIONS: List<Question> = listOf(

            // ---- Topic 1: What Is a Percentage? ----------------------------
            q(
                id = "what_is_pct_meaning",
                topic = WHAT_IS_A_PERCENTAGE,
                difficulty = E,
                en = "What does the symbol % mean?",
                hi = "% चिह्न का क्या अर्थ है?",
                options = listOf(
                    "Per ten" to "प्रति दस",
                    "Per hundred" to "प्रति सौ",
                    "Per thousand" to "प्रति हज़ार",
                    "Per half" to "प्रति आधा"
                ),
                correctIndex = 1,
                explanationEn = "% is the percent symbol. The word comes from \"per hundred\", so a percentage always measures against a base of 100.",
                explanationHi = "% प्रतिशत चिह्न है। यह शब्द \"प्रति सौ\" से बना है, इसलिए प्रतिशत की गणना हमेशा आधार 100 के सापेक्ष होती है।"
            ),
            q(
                id = "what_is_pct_25_out_of_100",
                topic = WHAT_IS_A_PERCENTAGE,
                difficulty = E,
                en = "25% means how many parts out of 100?",
                hi = "25% का अर्थ 100 में से कितने भाग हैं?",
                options = listOf(
                    "2.5 parts" to "2.5 भाग",
                    "25 parts" to "25 भाग",
                    "250 parts" to "250 भाग",
                    "1 part" to "1 भाग"
                ),
                correctIndex = 1,
                explanationEn = "25% is read as \"25 out of 100\", so it stands for 25 of the 100 equal parts.",
                explanationHi = "25% को \"100 में से 25\" पढ़ा जाता है, यानी यह 100 बराबर भागों में से 25 भागों को दर्शाता है।"
            ),
            q(
                id = "what_is_pct_100_meaning",
                topic = WHAT_IS_A_PERCENTAGE,
                difficulty = E,
                en = "100% of a quantity represents:",
                hi = "किसी मात्रा का 100% क्या दर्शाता है:",
                options = listOf(
                    "Half of the quantity" to "उस मात्रा का आधा",
                    "Double the quantity" to "उस मात्रा का दोगुना",
                    "The entire quantity" to "पूरी मात्रा",
                    "None of the quantity" to "उस मात्रा का शून्य भाग"
                ),
                correctIndex = 2,
                explanationEn = "100% means 100 out of 100, which is the complete quantity itself.",
                explanationHi = "100% का अर्थ है 100 में से 100, यानी वह पूरी मात्रा।"
            ),
            q(
                id = "what_is_pct_50_of_100",
                topic = WHAT_IS_A_PERCENTAGE,
                difficulty = E,
                en = "What is 50% of 100?",
                hi = "100 का 50% क्या है?",
                options = listOf(
                    "5" to "5",
                    "25" to "25",
                    "50" to "50",
                    "500" to "500"
                ),
                correctIndex = 2,
                explanationEn = "50% of 100 is 100 × 50 ÷ 100 = 50.",
                explanationHi = "100 का 50% यानी 100 × 50 ÷ 100 = 50।"
            ),
            q(
                id = "what_is_pct_25_of_200",
                topic = WHAT_IS_A_PERCENTAGE,
                difficulty = E,
                en = "What is 25% of 200?",
                hi = "200 का 25% क्या है?",
                options = listOf(
                    "25" to "25",
                    "40" to "40",
                    "50" to "50",
                    "75" to "75"
                ),
                correctIndex = 2,
                explanationEn = "25% = 25/100 = 1/4, and 200 ÷ 4 = 50.",
                explanationHi = "25% = ¼, और 200 ÷ 4 = 50।"
            ),
            q(
                id = "what_is_pct_largest",
                topic = WHAT_IS_A_PERCENTAGE,
                difficulty = E,
                en = "Which of these is the largest percentage?",
                hi = "इनमें से सबसे बड़ा प्रतिशत कौन सा है?",
                options = listOf(
                    "10%" to "10%",
                    "25%" to "25%",
                    "50%" to "50%",
                    "75%" to "75%"
                ),
                correctIndex = 3,
                explanationEn = "75% means 75 out of 100, which is more than 50, 25 or 10.",
                explanationHi = "75% का अर्थ है 100 में से 75, जो 50, 25 या 10 से अधिक है।"
            ),

            q(
                id = "what_is_pct_10_of_100",
                topic = WHAT_IS_A_PERCENTAGE,
                difficulty = E,
                en = "What is 10% of 100?",
                hi = "100 का 10% क्या है?",
                options = listOf(
                    "1" to "1",
                    "10" to "10",
                    "100" to "100",
                    "1000" to "1000"
                ),
                correctIndex = 1,
                explanationEn = "10% of 100 is 100 × 10 ÷ 100 = 10.",
                explanationHi = "100 का 10% यानी 100 × 10 ÷ 100 = 10।"
            ),
            q(
                id = "what_is_pct_20_of_50",
                topic = WHAT_IS_A_PERCENTAGE,
                difficulty = M,
                en = "What is 20% of 50?",
                hi = "50 का 20% क्या है?",
                options = listOf(
                    "5" to "5",
                    "10" to "10",
                    "20" to "20",
                    "25" to "25"
                ),
                correctIndex = 1,
                explanationEn = "20% = 1/5, and 50 ÷ 5 = 10.",
                explanationHi = "20% = ⅕, और 50 ÷ 5 = 10।"
            ),

            // ---- Topic 2: Understanding Percentages Visually ---------------
            q(
                id = "visual_grid_square_count",
                topic = PERCENTAGES_VISUALLY,
                difficulty = E,
                en = "A 10 by 10 grid is divided into how many equal squares?",
                hi = "10 गुणा 10 का जाल कितने बराबर वर्गों में बँटा है?",
                options = listOf(
                    "10" to "10",
                    "20" to "20",
                    "50" to "50",
                    "100" to "100"
                ),
                correctIndex = 3,
                explanationEn = "10 rows × 10 columns = 100 squares.",
                explanationHi = "10 पंक्तियाँ × 10 स्तंभ = 100 वर्ग।"
            ),
            q(
                id = "visual_one_square_value",
                topic = PERCENTAGES_VISUALLY,
                difficulty = E,
                en = "What does one square in a 100-square grid represent?",
                hi = "100 वर्गों के जाल में एक वर्ग क्या दर्शाता है?",
                options = listOf(
                    "1%" to "1%",
                    "10%" to "10%",
                    "25%" to "25%",
                    "50%" to "50%"
                ),
                correctIndex = 0,
                explanationEn = "The grid has 100 equal squares and stands for the whole, so one square is 1/100 = 1%.",
                explanationHi = "जाल में 100 बराबर वर्ग हैं और यह पूरी मात्रा को दर्शाता है, इसलिए एक वर्ग 1/100 = 1% है।"
            ),
            q(
                id = "visual_50_shaded",
                topic = PERCENTAGES_VISUALLY,
                difficulty = E,
                en = "If 50 of the 100 squares are shaded, what percentage is shaded?",
                hi = "यदि 100 में से 50 वर्ग छायांकित हों, तो कितना प्रतिशत छायांकित है?",
                options = listOf(
                    "5%" to "5%",
                    "25%" to "25%",
                    "50%" to "50%",
                    "500%" to "500%"
                ),
                correctIndex = 2,
                explanationEn = "Each square is 1%, so 50 shaded squares means 50%.",
                explanationHi = "हर वर्ग 1% है, इसलिए 50 छायांकित वर्गों का अर्थ 50% है।"
            ),
            q(
                id = "visual_25_shaded",
                topic = PERCENTAGES_VISUALLY,
                difficulty = E,
                en = "If 25 squares out of 100 are shaded, what percentage is shaded?",
                hi = "यदि 100 में से 25 वर्ग छायांकित हों, तो कितना प्रतिशत छायांकित है?",
                options = listOf(
                    "25%" to "25%",
                    "4%" to "4%",
                    "75%" to "75%",
                    "250%" to "250%"
                ),
                correctIndex = 0,
                explanationEn = "One square equals 1%, so 25 shaded squares means 25%.",
                explanationHi = "एक वर्ग बराबर 1% है, इसलिए 25 छायांकित वर्गों का अर्थ 25% है।"
            ),
            q(
                id = "visual_ten_square_grid",
                topic = PERCENTAGES_VISUALLY,
                difficulty = M,
                en = "A grid of only 10 squares shows percentages most easily in steps of:",
                hi = "केवल 10 वर्गों के जाल में प्रतिशत सबसे आसानी से कितने के चरणों में दिखता है?",
                options = listOf(
                    "Multiples of 10%" to "10% के गुणज",
                    "Multiples of 25%" to "25% के गुणज",
                    "Any percentage at all" to "कोई भी प्रतिशत",
                    "Only whole numbers" to "केवल पूर्ण संख्याएँ"
                ),
                correctIndex = 0,
                explanationEn = "10 squares give only ten equal parts, so each part stands for 10% of the whole. A 100-square grid is needed for 1% steps.",
                explanationHi = "10 वर्ग केवल दस बराबर भाग देते हैं, इसलिए हर भाग पूरे का 10% होता है। 1% के चरणों के लिए 100 वर्गों का जाल चाहिए।"
            ),
            q(
                id = "visual_all_shaded",
                topic = PERCENTAGES_VISUALLY,
                difficulty = E,
                en = "If all 100 squares are shaded, what percentage is shaded?",
                hi = "यदि 100 में से सभी वर्ग छायांकित हों, तो कितना प्रतिशत छायांकित है?",
                options = listOf(
                    "1%" to "1%",
                    "50%" to "50%",
                    "99%" to "99%",
                    "100%" to "100%"
                ),
                correctIndex = 3,
                explanationEn = "All 100 squares shaded means the whole, which is 100%.",
                explanationHi = "100 में से सभी वर्ग छायांकित होने का अर्थ है पूरी मात्रा, यानी 100%।"
            ),

            q(
                id = "visual_unshaded_40",
                topic = PERCENTAGES_VISUALLY,
                difficulty = M,
                en = "In a 100-square grid, 40 squares are shaded. What percentage is unshaded?",
                hi = "100 वर्गों के जाल में 40 वर्ग छायांकित हैं। कितना प्रतिशत अछायांकित नहीं है?",
                options = listOf(
                    "60%" to "60%",
                    "40%" to "40%",
                    "50%" to "50%",
                    "10%" to "10%"
                ),
                correctIndex = 0,
                explanationEn = "100 squares is the whole. If 40 are shaded, the remaining 100 − 40 = 60 squares are unshaded, which is 60%.",
                explanationHi = "100 वर्ग ही पूरी मात्रा है। यदि 40 छायांकित हैं, तो शेष 100 − 40 = 60 वर्ग अछायांकित हैं, यानी 60%।"
            ),
            q(
                id = "visual_shade_90",
                topic = PERCENTAGES_VISUALLY,
                difficulty = E,
                en = "If 90 squares of a 100-square grid are shaded, what percentage is shaded?",
                hi = "यदि 100 वर्गों के जाल में 90 वर्ग छायांकित हों, तो कितना प्रतिशत छायांकित है?",
                options = listOf(
                    "9%" to "9%",
                    "90%" to "90%",
                    "100%" to "100%",
                    "10%" to "10%"
                ),
                correctIndex = 1,
                explanationEn = "Each square is 1%, so 90 shaded squares means 90%.",
                explanationHi = "हर वर्ग 1% है, इसलिए 90 छायांकित वर्गों का अर्थ 90% है।"
            ),

            // ---- Topic 3: Fractions to Percentages -------------------------
            q(
                id = "fraction_half",
                topic = FRACTIONS_TO_PERCENTAGES,
                difficulty = E,
                en = "What is 1/2 as a percentage?",
                hi = "½ प्रतिशत में क्या है?",
                options = listOf(
                    "25%" to "25%",
                    "50%" to "50%",
                    "75%" to "75%",
                    "20%" to "20%"
                ),
                correctIndex = 1,
                explanationEn = "1/2 = 50/100, so 1/2 = 50%.",
                explanationHi = "½ = 50/100, इसलिए ½ = 50%।"
            ),
            q(
                id = "fraction_quarter",
                topic = FRACTIONS_TO_PERCENTAGES,
                difficulty = E,
                en = "What is 1/4 as a percentage?",
                hi = "¼ प्रतिशत में क्या है?",
                options = listOf(
                    "20%" to "20%",
                    "25%" to "25%",
                    "40%" to "40%",
                    "75%" to "75%"
                ),
                correctIndex = 1,
                explanationEn = "1/4 = 25/100, so 1/4 = 25%.",
                explanationHi = "¼ = 25/100, इसलिए ¼ = 25%।"
            ),
            q(
                id = "fraction_three_quarters",
                topic = FRACTIONS_TO_PERCENTAGES,
                difficulty = E,
                en = "What is 3/4 as a percentage?",
                hi = "¾ प्रतिशत में क्या है?",
                options = listOf(
                    "25%" to "25%",
                    "50%" to "50%",
                    "75%" to "75%",
                    "34%" to "34%"
                ),
                correctIndex = 2,
                explanationEn = "3/4 = 75/100, so 3/4 = 75%.",
                explanationHi = "¾ = 75/100, इसलिए ¾ = 75%।"
            ),
            q(
                id = "fraction_fifth",
                topic = FRACTIONS_TO_PERCENTAGES,
                difficulty = E,
                en = "What is 1/5 as a percentage?",
                hi = "⅕ प्रतिशत में क्या है?",
                options = listOf(
                    "10%" to "10%",
                    "20%" to "20%",
                    "25%" to "25%",
                    "50%" to "50%"
                ),
                correctIndex = 1,
                explanationEn = "1/5 = 20/100, so 1/5 = 20%.",
                explanationHi = "⅕ = 20/100, इसलिए ⅕ = 20%।"
            ),
            q(
                id = "fraction_half_plus_quarter",
                topic = FRACTIONS_TO_PERCENTAGES,
                difficulty = M,
                en = "1/2 + 1/4 is what percentage?",
                hi = "½ + ¼ कितने प्रतिशत का योग है?",
                options = listOf(
                    "50%" to "50%",
                    "25%" to "25%",
                    "75%" to "75%",
                    "100%" to "100%"
                ),
                correctIndex = 2,
                explanationEn = "1/2 = 50% and 1/4 = 25%. Adding them gives 50% + 25% = 75%.",
                explanationHi = "½ = 50% और ¼ = 25%। दोनों जोड़ने पर 50% + 25% = 75%।"
            ),
            q(
                id = "fraction_awkward_denominator",
                topic = FRACTIONS_TO_PERCENTAGES,
                difficulty = M,
                en = "A fraction's denominator does not divide 100 evenly. How should you convert it to a percentage?",
                hi = "किसी भिन्न की निचली संख्या 100 में बराबर नहीं बँटती। इसे प्रतिशत में कैसे बदलें?",
                options = listOf(
                    "Round it to the nearest whole percent" to "निकटतम पूर्ण प्रतिशत तक गोल करें",
                    "Divide the numerator by the denominator, then multiply by 100" to "ऊपरी संख्या को निचली से भाग कीजिए, फिर 100 से गुणा कीजिए",
                    "Add 100 to the numerator" to "ऊपरी संख्या में 100 जोड़ दीजिए",
                    "Multiply the denominator by 100" to "निचली संख्या को 100 से गुणा कीजिए"
                ),
                correctIndex = 1,
                explanationEn = "If the denominator will not fit into 100, divide first and then multiply by 100. For example 1 ÷ 3 = 0.333…, then × 100 = 33.33%.",
                explanationHi = "यदि निचली संख्या 100 में बराबर नहीं बँटती, तो पहले भाग कीजिए और फिर 100 से गुणा कीजिए। जैसे 1 ÷ 3 = 0.333…, फिर × 100 = 33.33%।"
            ),
            q(
                id = "fraction_tenth",
                topic = FRACTIONS_TO_PERCENTAGES,
                difficulty = E,
                en = "What is 1/10 as a percentage?",
                hi = "¹⁄₁₀ प्रतिशत में क्या है?",
                options = listOf(
                    "10%" to "10%",
                    "1%" to "1%",
                    "20%" to "20%",
                    "25%" to "25%"
                ),
                correctIndex = 0,
                explanationEn = "1/10 = 10/100, so 1/10 = 10%.",
                explanationHi = "¹⁄₁₀ = 10/100, इसलिए ¹⁄₁₀ = 10%।"
            ),
            q(
                id = "fraction_hundredth",
                topic = FRACTIONS_TO_PERCENTAGES,
                difficulty = E,
                en = "What is 1/100 as a percentage?",
                hi = "¹⁄₁₀₀ प्रतिशत में क्या है?",
                options = listOf(
                    "1%" to "1%",
                    "10%" to "10%",
                    "25%" to "25%",
                    "50%" to "50%"
                ),
                correctIndex = 0,
                explanationEn = "1/100 = 1/100, so 1/100 is exactly 1%.",
                explanationHi = "¹⁄₁₀₀ = 1/100, इसलिए ¹⁄₁₀₀ ठीक 1% है।"
            ),
        // ==== BEGIN GENERATED PHASE 6 QUESTIONS ====
            q(
                id = "dec_035",
                topic = DECIMALS_TO_PERCENTAGES,
                difficulty = E,
                en = "What is 0.35 written as a percentage?",
                hi = "0.35 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "35%" to "35%",
                    "3.5%" to "3.5%",
                    "17.5%" to "17.5%",
                    "350%" to "350%"
                ),
                correctIndex = 0,
                explanationEn = "Shift the point two places right: 0.35 x 100 = 35. So 0.35 = 35%.",
                explanationHi = "Shift the point two places right: 0.35 x 100 = 35. अतः 0.35 = 35%।"
            ),
            q(
                id = "dec_060",
                topic = DECIMALS_TO_PERCENTAGES,
                difficulty = E,
                en = "What is 0.6 written as a percentage?",
                hi = "0.6 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "60%" to "60%",
                    "6%" to "6%",
                    "30%" to "30%",
                    "600%" to "600%"
                ),
                correctIndex = 0,
                explanationEn = "Add a zero so you shift two places: 0.60 x 100 = 60. So 0.6 = 60%.",
                explanationHi = "Add a zero so you shift two places: 0.60 x 100 = 60. अतः 0.6 = 60%।"
            ),
            q(
                id = "dec_0125",
                topic = DECIMALS_TO_PERCENTAGES,
                difficulty = H,
                en = "What is 0.125 written as a percentage?",
                hi = "0.125 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "12.5%" to "12.5%",
                    "1.25%" to "1.25%",
                    "6.25%" to "6.25%",
                    "125%" to "125%"
                ),
                correctIndex = 0,
                explanationEn = "0.125 x 100 = 12.5, because one eighth is 12.5%. So 0.125 = 12.5%.",
                explanationHi = "0.125 x 100 = 12.5, because one eighth is 12.5%. अतः 0.125 = 12.5%।"
            ),
            q(
                id = "dec_008",
                topic = DECIMALS_TO_PERCENTAGES,
                difficulty = E,
                en = "What is 0.08 written as a percentage?",
                hi = "0.08 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "8%" to "8%",
                    "0.8%" to "0.8%",
                    "4%" to "4%",
                    "80%" to "80%"
                ),
                correctIndex = 0,
                explanationEn = "0.08 x 100 = 8. So 0.08 = 8%.",
                explanationHi = "0.08 x 100 = 8. अतः 0.08 = 8%।"
            ),
            q(
                id = "dec_0045",
                topic = DECIMALS_TO_PERCENTAGES,
                difficulty = M,
                en = "What is 0.0045 written as a percentage?",
                hi = "0.0045 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "0.45%" to "0.45%",
                    "0.045%" to "0.045%",
                    "0.225%" to "0.225%",
                    "4.5%" to "4.5%"
                ),
                correctIndex = 0,
                explanationEn = "0.0045 x 100 = 0.45. So 0.0045 = 0.45%.",
                explanationHi = "0.0045 x 100 = 0.45. अतः 0.0045 = 0.45%।"
            ),
            q(
                id = "dec_125",
                topic = DECIMALS_TO_PERCENTAGES,
                difficulty = E,
                en = "What is 1.25 written as a percentage?",
                hi = "1.25 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "125%" to "125%",
                    "12.5%" to "12.5%",
                    "62.5%" to "62.5%",
                    "1250%" to "1250%"
                ),
                correctIndex = 0,
                explanationEn = "A decimal above 1 gives a percentage above 100: 1.25 x 100 = 125. So 1.25 = 125%.",
                explanationHi = "A decimal above 1 gives a percentage above 100: 1.25 x 100 = 125. अतः 1.25 = 125%।"
            ),
            q(
                id = "dec_35",
                topic = DECIMALS_TO_PERCENTAGES,
                difficulty = M,
                en = "What is 3.5 written as a percentage?",
                hi = "3.5 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "350%" to "350%",
                    "35%" to "35%",
                    "175%" to "175%",
                    "3500%" to "3500%"
                ),
                correctIndex = 0,
                explanationEn = "3.5 x 100 = 350. So 3.5 = 350%.",
                explanationHi = "3.5 x 100 = 350. अतः 3.5 = 350%।"
            ),
            q(
                id = "dec_3dp",
                topic = DECIMALS_TO_PERCENTAGES,
                difficulty = H,
                en = "What is 0.4567 written as a percentage?",
                hi = "0.4567 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "45.67%" to "45.67%",
                    "4.567%" to "4.567%",
                    "22.835%" to "22.835%",
                    "456.7%" to "456.7%"
                ),
                correctIndex = 0,
                explanationEn = "0.4567 x 100 = 45.67. So 0.4567 = 45.67%.",
                explanationHi = "0.4567 x 100 = 45.67. अतः 0.4567 = 45.67%।"
            ),
            q(
                id = "dec_which_is_40",
                topic = DECIMALS_TO_PERCENTAGES,
                difficulty = M,
                en = "Which decimal is the same as 40%?",
                hi = "कौन-सा दशमलव 40% के बराबर है?",
                options = listOf(
                    "0.4" to "0.4",
                    "0.04" to "0.04",
                    "4.0" to "4.0",
                    "0.4004" to "0.4004"
                ),
                correctIndex = 0,
                explanationEn = "40% = 40/100 = 0.4. Dividing by 100 moves the point two places left.",
                explanationHi = "40% = 40/100 = 0.4। 100 से भाग करने पर बिंदु दो स्थान बाएँ चला जाता है।"
            ),
            q(
                id = "pct_20_of_350",
                topic = PERCENTAGE_OF_A_NUMBER,
                difficulty = E,
                en = "What is 20% of 350?",
                hi = "350 का 20% कितना है?",
                options = listOf(
                    "70" to "70",
                    "700" to "700",
                    "1050" to "1050",
                    "280" to "280"
                ),
                correctIndex = 0,
                explanationEn = "20% of 350 = 20/100 x 350 = 70.",
                explanationHi = "350 का 20% = 20/100 x 350 = 70।"
            ),
            q(
                id = "pct_25_of_80",
                topic = PERCENTAGE_OF_A_NUMBER,
                difficulty = E,
                en = "What is 25% of 80?",
                hi = "80 का 25% कितना है?",
                options = listOf(
                    "20" to "20",
                    "200" to "200",
                    "280" to "280",
                    "60" to "60"
                ),
                correctIndex = 0,
                explanationEn = "25% of 80 = 25/100 x 80 = 20.",
                explanationHi = "80 का 25% = 25/100 x 80 = 20।"
            ),
            q(
                id = "pct_15_of_240",
                topic = PERCENTAGE_OF_A_NUMBER,
                difficulty = E,
                en = "What is 15% of 240?",
                hi = "240 का 15% कितना है?",
                options = listOf(
                    "36" to "36",
                    "360" to "360",
                    "600" to "600",
                    "204" to "204"
                ),
                correctIndex = 0,
                explanationEn = "15% of 240 = 15/100 x 240 = 36.",
                explanationHi = "240 का 15% = 15/100 x 240 = 36।"
            ),
            q(
                id = "pct_40_of_250",
                topic = PERCENTAGE_OF_A_NUMBER,
                difficulty = E,
                en = "What is 40% of 250?",
                hi = "250 का 40% कितना है?",
                options = listOf(
                    "100" to "100",
                    "1000" to "1000",
                    "1250" to "1250",
                    "150" to "150"
                ),
                correctIndex = 0,
                explanationEn = "40% of 250 = 40/100 x 250 = 100.",
                explanationHi = "250 का 40% = 40/100 x 250 = 100।"
            ),
            q(
                id = "pct_60_of_45",
                topic = PERCENTAGE_OF_A_NUMBER,
                difficulty = E,
                en = "What is 60% of 45?",
                hi = "45 का 60% कितना है?",
                options = listOf(
                    "27" to "27",
                    "270" to "270",
                    "315" to "315",
                    "18" to "18"
                ),
                correctIndex = 0,
                explanationEn = "60% of 45 = 60/100 x 45 = 27.",
                explanationHi = "45 का 60% = 60/100 x 45 = 27।"
            ),
            q(
                id = "pct_35_of_200",
                topic = PERCENTAGE_OF_A_NUMBER,
                difficulty = M,
                en = "What is 35% of 200?",
                hi = "200 का 35% कितना है?",
                options = listOf(
                    "70" to "70",
                    "700" to "700",
                    "900" to "900",
                    "130" to "130"
                ),
                correctIndex = 0,
                explanationEn = "35% of 200 = 35/100 x 200 = 70.",
                explanationHi = "200 का 35% = 35/100 x 200 = 70।"
            ),
            q(
                id = "pct_125_of_320",
                topic = PERCENTAGE_OF_A_NUMBER,
                difficulty = H,
                en = "What is 12.5% of 320?",
                hi = "320 का 12.5% कितना है?",
                options = listOf(
                    "40" to "40",
                    "400" to "400",
                    "720" to "720",
                    "280" to "280"
                ),
                correctIndex = 0,
                explanationEn = "12.5% of 320 = 12.5/100 x 320 = 40.",
                explanationHi = "320 का 12.5% = 12.5/100 x 320 = 40।"
            ),
            q(
                id = "pct_7_of_6400",
                topic = PERCENTAGE_OF_A_NUMBER,
                difficulty = H,
                en = "What is 7% of 6400?",
                hi = "6400 का 7% कितना है?",
                options = listOf(
                    "448" to "448",
                    "4480" to "4480",
                    "10880" to "10880",
                    "5952" to "5952"
                ),
                correctIndex = 0,
                explanationEn = "7% of 6400 = 7/100 x 6400 = 448.",
                explanationHi = "6400 का 7% = 7/100 x 6400 = 448।"
            ),
            q(
                id = "pct_class_girls",
                topic = PERCENTAGE_OF_A_NUMBER,
                difficulty = M,
                en = "A class has 40 students and 45% of them are girls. How many girls are there?",
                hi = "एक कक्षा में 40 विद्यार्थी हैं और उनमें 45% लड़कियाँ हैं। कितनी लड़कियाँ हैं?",
                options = listOf(
                    "18" to "18",
                    "16" to "16",
                    "20" to "20",
                    "22" to "22"
                ),
                correctIndex = 0,
                explanationEn = "45% of 40 = 45/100 x 40 = 18 girls.",
                explanationHi = "40 का 45% = 45/100 x 40 = 18 लड़कियाँ।"
            ),
            q(
                id = "whole_40_is_20pct",
                topic = ORIGINAL_WHOLE,
                difficulty = E,
                en = "40 is 20% of what number?",
                hi = "40, किस संख्या का 20% है?",
                options = listOf(
                    "200" to "200",
                    "8" to "8",
                    "100" to "100",
                    "400" to "400"
                ),
                correctIndex = 0,
                explanationEn = "40 / (20/100) = 40 x 100 / 20 = 200. Check: 20% of 200 = 40.",
                explanationHi = "40 / (20/100) = 40 x 100 / 20 = 200। जाँच: 200 का 20% = 40।"
            ),
            q(
                id = "whole_30_is_15pct",
                topic = ORIGINAL_WHOLE,
                difficulty = E,
                en = "30 is 15% of what number?",
                hi = "30, किस संख्या का 15% है?",
                options = listOf(
                    "200" to "200",
                    "4.5" to "4.5",
                    "100" to "100",
                    "400" to "400"
                ),
                correctIndex = 0,
                explanationEn = "30 / (15/100) = 30 x 100 / 15 = 200. Check: 15% of 200 = 30.",
                explanationHi = "30 / (15/100) = 30 x 100 / 15 = 200। जाँच: 200 का 15% = 30।"
            ),
            q(
                id = "whole_45_is_25pct",
                topic = ORIGINAL_WHOLE,
                difficulty = E,
                en = "45 is 25% of what number?",
                hi = "45, किस संख्या का 25% है?",
                options = listOf(
                    "180" to "180",
                    "11.25" to "11.25",
                    "90" to "90",
                    "360" to "360"
                ),
                correctIndex = 0,
                explanationEn = "45 / (25/100) = 45 x 100 / 25 = 180. Check: 25% of 180 = 45.",
                explanationHi = "45 / (25/100) = 45 x 100 / 25 = 180। जाँच: 180 का 25% = 45।"
            ),
            q(
                id = "whole_63_is_30pct",
                topic = ORIGINAL_WHOLE,
                difficulty = M,
                en = "63 is 30% of what number?",
                hi = "63, किस संख्या का 30% है?",
                options = listOf(
                    "210" to "210",
                    "18.9" to "18.9",
                    "105" to "105",
                    "420" to "420"
                ),
                correctIndex = 0,
                explanationEn = "63 / (30/100) = 63 x 100 / 30 = 210. Check: 30% of 210 = 63.",
                explanationHi = "63 / (30/100) = 63 x 100 / 30 = 210। जाँच: 210 का 30% = 63।"
            ),
            q(
                id = "whole_240_is_60pct",
                topic = ORIGINAL_WHOLE,
                difficulty = M,
                en = "240 is 60% of what number?",
                hi = "240, किस संख्या का 60% है?",
                options = listOf(
                    "400" to "400",
                    "144" to "144",
                    "200" to "200",
                    "800" to "800"
                ),
                correctIndex = 0,
                explanationEn = "240 / (60/100) = 240 x 100 / 60 = 400. Check: 60% of 400 = 240.",
                explanationHi = "240 / (60/100) = 240 x 100 / 60 = 400। जाँच: 400 का 60% = 240।"
            ),
            q(
                id = "whole_132_is_66pct",
                topic = ORIGINAL_WHOLE,
                difficulty = M,
                en = "132 is 66% of what number?",
                hi = "132, किस संख्या का 66% है?",
                options = listOf(
                    "200" to "200",
                    "87.12" to "87.12",
                    "100" to "100",
                    "400" to "400"
                ),
                correctIndex = 0,
                explanationEn = "132 / (66/100) = 132 x 100 / 66 = 200. Check: 66% of 200 = 132.",
                explanationHi = "132 / (66/100) = 132 x 100 / 66 = 200। जाँच: 200 का 66% = 132।"
            ),
            q(
                id = "whole_85_is_17pct",
                topic = ORIGINAL_WHOLE,
                difficulty = H,
                en = "85 is 17% of what number?",
                hi = "85, किस संख्या का 17% है?",
                options = listOf(
                    "500" to "500",
                    "14.45" to "14.45",
                    "250" to "250",
                    "1000" to "1000"
                ),
                correctIndex = 0,
                explanationEn = "85 / (17/100) = 85 x 100 / 17 = 500. Check: 17% of 500 = 85.",
                explanationHi = "85 / (17/100) = 85 x 100 / 17 = 500। जाँच: 500 का 17% = 85।"
            ),
            q(
                id = "whole_exam_marks",
                topic = ORIGINAL_WHOLE,
                difficulty = M,
                en = "A student scored 36 marks, which is 45% of the maximum. What is the maximum?",
                hi = "एक विद्यार्थी ने 36 अंक अर्जित किए, जो अधिकतम अंकों का 45% हैं। अधिकतम अंक कितने हैं?",
                options = listOf(
                    "80" to "80",
                    "72" to "72",
                    "90" to "90",
                    "16.2" to "16.2"
                ),
                correctIndex = 0,
                explanationEn = "36 / 0.45 = 80. Check: 45% of 80 = 36.",
                explanationHi = "36 / 0.45 = 80। जाँच: 80 का 45% = 36।"
            ),
            q(
                id = "inc_450_up_12",
                topic = PERCENTAGE_INCREASE_DECREASE,
                difficulty = E,
                en = "A value of 450 is increased by 12%. What is the new value?",
                hi = "450 का मान 12% से बढ़ा है। नया मान क्या है?",
                options = listOf(
                    "504" to "504",
                    "396" to "396",
                    "540" to "540",
                    "558" to "558"
                ),
                correctIndex = 0,
                explanationEn = "12% of 450 = 54, and 450 + 54 = 504.",
                explanationHi = "450 का 12% = 54, और 450 + 54 = 504।"
            ),
            q(
                id = "inc_800_down_25",
                topic = PERCENTAGE_INCREASE_DECREASE,
                difficulty = E,
                en = "A value of 800 is decreased by 25%. What is the new value?",
                hi = "800 का मान 25% से घटा है। नया मान क्या है?",
                options = listOf(
                    "600" to "600",
                    "1000" to "1000",
                    "2000" to "2000",
                    "800" to "800"
                ),
                correctIndex = 0,
                explanationEn = "25% of 800 = 200, and 800 - 200 = 600.",
                explanationHi = "800 का 25% = 200, और 800 - 200 = 600।"
            ),
            q(
                id = "inc_2500_up_8",
                topic = PERCENTAGE_INCREASE_DECREASE,
                difficulty = E,
                en = "A value of 2500 is increased by 8%. What is the new value?",
                hi = "2500 का मान 8% से बढ़ा है। नया मान क्या है?",
                options = listOf(
                    "2700" to "2700",
                    "2300" to "2300",
                    "2000" to "2000",
                    "2900" to "2900"
                ),
                correctIndex = 0,
                explanationEn = "8% of 2500 = 200, and 2500 + 200 = 2700.",
                explanationHi = "2500 का 8% = 200, और 2500 + 200 = 2700।"
            ),
            q(
                id = "inc_600_down_15",
                topic = PERCENTAGE_INCREASE_DECREASE,
                difficulty = E,
                en = "A value of 600 is decreased by 15%. What is the new value?",
                hi = "600 का मान 15% से घटा है। नया मान क्या है?",
                options = listOf(
                    "510" to "510",
                    "690" to "690",
                    "900" to "900",
                    "600" to "600"
                ),
                correctIndex = 0,
                explanationEn = "15% of 600 = 90, and 600 - 90 = 510.",
                explanationHi = "600 का 15% = 90, और 600 - 90 = 510।"
            ),
            q(
                id = "inc_320_up_5",
                topic = PERCENTAGE_INCREASE_DECREASE,
                difficulty = E,
                en = "A value of 320 is increased by 5%. What is the new value?",
                hi = "320 का मान 5% से बढ़ा है। नया मान क्या है?",
                options = listOf(
                    "336" to "336",
                    "304" to "304",
                    "160" to "160",
                    "352" to "352"
                ),
                correctIndex = 0,
                explanationEn = "5% of 320 = 16, and 320 + 16 = 336.",
                explanationHi = "320 का 5% = 16, और 320 + 16 = 336।"
            ),
            q(
                id = "inc_150_down_40",
                topic = PERCENTAGE_INCREASE_DECREASE,
                difficulty = M,
                en = "A value of 150 is decreased by 40%. What is the new value?",
                hi = "150 का मान 40% से घटा है। नया मान क्या है?",
                options = listOf(
                    "90" to "90",
                    "210" to "210",
                    "600" to "600",
                    "150" to "150"
                ),
                correctIndex = 0,
                explanationEn = "40% of 150 = 60, and 150 - 60 = 90.",
                explanationHi = "150 का 40% = 60, और 150 - 60 = 90।"
            ),
            q(
                id = "inc_1500_down_12",
                topic = PERCENTAGE_INCREASE_DECREASE,
                difficulty = H,
                en = "A value of 1500 is decreased by 12%. What is the new value?",
                hi = "1500 का मान 12% से घटा है। नया मान क्या है?",
                options = listOf(
                    "1320" to "1320",
                    "1680" to "1680",
                    "1800" to "1800",
                    "1500" to "1500"
                ),
                correctIndex = 0,
                explanationEn = "12% of 1500 = 180, and 1500 - 180 = 1320.",
                explanationHi = "1500 का 12% = 180, और 1500 - 180 = 1320।"
            ),
            q(
                id = "inc_240_up_35",
                topic = PERCENTAGE_INCREASE_DECREASE,
                difficulty = H,
                en = "A value of 240 is increased by 35%. What is the new value?",
                hi = "240 का मान 35% से बढ़ा है। नया मान क्या है?",
                options = listOf(
                    "324" to "324",
                    "156" to "156",
                    "840" to "840",
                    "408" to "408"
                ),
                correctIndex = 0,
                explanationEn = "35% of 240 = 84, and 240 + 84 = 324.",
                explanationHi = "240 का 35% = 84, और 240 + 84 = 324।"
            ),
            q(
                id = "inc_jacket_price",
                topic = PERCENTAGE_INCREASE_DECREASE,
                difficulty = M,
                en = "A shop raised all prices by 10%. A jacket marked at 1800 now costs how much?",
                hi = "एक दुकान ने सभी मूल्य 10% बढ़ा दिए। 1800 पर अंकित जैकेट का अब मूल्य कितना है?",
                options = listOf(
                    "1980" to "1980",
                    "1800" to "1800",
                    "2000" to "2000",
                    "2160" to "2160"
                ),
                correctIndex = 0,
                explanationEn = "10% of 1800 = 180, so 1800 + 180 = 1980.",
                explanationHi = "1800 का 10% = 180, इसलिए 1800 + 180 = 1980।"
            ),
            q(
                id = "succ_10_then_20",
                topic = SUCCESSIVE_PERCENTAGE_CHANGES,
                difficulty = E,
                en = "A value of 100 changes by +10% and then +20%. What is the net change?",
                hi = "100 का मान पहले +10% और फिर +20% बदलता है। शुद्ध परिवर्तन क्या है?",
                options = listOf(
                    "32%" to "32%",
                    "30%" to "30%",
                    "20%" to "20%",
                    "10%" to "10%"
                ),
                correctIndex = 0,
                explanationEn = "Step 1: 100 -> 110. Step 2: 110 -> 132. Net = 132 - 100 = 32%.",
                explanationHi = "चरण 1: 100 -> 110। चरण 2: 110 -> 132। शुद्ध = 132 - 100 = 32%।"
            ),
            q(
                id = "succ_10_then_minus_10",
                topic = SUCCESSIVE_PERCENTAGE_CHANGES,
                difficulty = M,
                en = "A value of 100 changes by +10% and then -10%. What is the net change?",
                hi = "100 का मान पहले +10% और फिर -10% बदलता है। शुद्ध परिवर्तन क्या है?",
                options = listOf(
                    "-1%" to "-1%",
                    "0%" to "0%",
                    "-10%" to "-10%",
                    "10%" to "10%"
                ),
                correctIndex = 0,
                explanationEn = "Step 1: 100 -> 110. Step 2: 110 -> 99. Net = 99 - 100 = -1%.",
                explanationHi = "चरण 1: 100 -> 110। चरण 2: 110 -> 99। शुद्ध = 99 - 100 = -1%।"
            ),
            q(
                id = "succ_50_then_minus_50",
                topic = SUCCESSIVE_PERCENTAGE_CHANGES,
                difficulty = M,
                en = "A value of 200 changes by +50% and then -50%. What is the net change?",
                hi = "200 का मान पहले +50% और फिर -50% बदलता है। शुद्ध परिवर्तन क्या है?",
                options = listOf(
                    "-25%" to "-25%",
                    "0%" to "0%",
                    "-50%" to "-50%",
                    "50%" to "50%"
                ),
                correctIndex = 0,
                explanationEn = "Step 1: 200 -> 300. Step 2: 300 -> 150. Net = 150 - 200 = -25%.",
                explanationHi = "चरण 1: 200 -> 300। चरण 2: 300 -> 150। शुद्ध = 150 - 200 = -25%।"
            ),
            q(
                id = "succ_minus_20_then_25",
                topic = SUCCESSIVE_PERCENTAGE_CHANGES,
                difficulty = M,
                en = "A value of 500 changes by -20% and then +25%. What is the net change?",
                hi = "500 का मान पहले -20% और फिर +25% बदलता है। शुद्ध परिवर्तन क्या है?",
                options = listOf(
                    "0%" to "0%",
                    "5%" to "5%",
                    "25%" to "25%",
                    "-20%" to "-20%"
                ),
                correctIndex = 0,
                explanationEn = "Step 1: 500 -> 400. Step 2: 400 -> 500. Net = 500 - 500 = 0%.",
                explanationHi = "चरण 1: 500 -> 400। चरण 2: 400 -> 500। शुद्ध = 500 - 500 = 0%।"
            ),
            q(
                id = "succ_20_then_minus_20",
                topic = SUCCESSIVE_PERCENTAGE_CHANGES,
                difficulty = M,
                en = "A value of 250 changes by +20% and then -20%. What is the net change?",
                hi = "250 का मान पहले +20% और फिर -20% बदलता है। शुद्ध परिवर्तन क्या है?",
                options = listOf(
                    "-4%" to "-4%",
                    "0%" to "0%",
                    "-20%" to "-20%",
                    "20%" to "20%"
                ),
                correctIndex = 0,
                explanationEn = "Step 1: 250 -> 300. Step 2: 300 -> 240. Net = 240 - 250 = -4%.",
                explanationHi = "चरण 1: 250 -> 300। चरण 2: 300 -> 240। शुद्ध = 240 - 250 = -4%।"
            ),
            q(
                id = "succ_minus_25_twice",
                topic = SUCCESSIVE_PERCENTAGE_CHANGES,
                difficulty = H,
                en = "A value of 400 changes by -25% and then -25%. What is the net change?",
                hi = "400 का मान पहले -25% और फिर -25% बदलता है। शुद्ध परिवर्तन क्या है?",
                options = listOf(
                    "-43.75%" to "-43.75%",
                    "-50%" to "-50%",
                    "-25%" to "-25%",
                    "-68.75%" to "-68.75%"
                ),
                correctIndex = 0,
                explanationEn = "Step 1: 400 -> 300. Step 2: 300 -> 225. Net = 225 - 400 = -43.75%.",
                explanationHi = "चरण 1: 400 -> 300। चरण 2: 300 -> 225। शुद्ध = 225 - 400 = -43.75%।"
            ),
            q(
                id = "succ_25_twice",
                topic = SUCCESSIVE_PERCENTAGE_CHANGES,
                difficulty = H,
                en = "A value of 200 changes by +25% and then +25%. What is the net change?",
                hi = "200 का मान पहले +25% और फिर +25% बदलता है। शुद्ध परिवर्तन क्या है?",
                options = listOf(
                    "56.25%" to "56.25%",
                    "50%" to "50%",
                    "25%" to "25%",
                    "81.25%" to "81.25%"
                ),
                correctIndex = 0,
                explanationEn = "Step 1: 200 -> 250. Step 2: 250 -> 312.5. Net = 312.5 - 200 = 56.25%.",
                explanationHi = "चरण 1: 200 -> 250। चरण 2: 250 -> 312.5। शुद्ध = 312.5 - 200 = 56.25%।"
            ),
            q(
                id = "succ_40_then_minus_40",
                topic = SUCCESSIVE_PERCENTAGE_CHANGES,
                difficulty = M,
                en = "A value of 100 changes by +40% and then -40%. What is the net change?",
                hi = "100 का मान पहले +40% और फिर -40% बदलता है। शुद्ध परिवर्तन क्या है?",
                options = listOf(
                    "-16%" to "-16%",
                    "0%" to "0%",
                    "-40%" to "-40%",
                    "40%" to "40%"
                ),
                correctIndex = 0,
                explanationEn = "Step 1: 100 -> 140. Step 2: 140 -> 84. Net = 84 - 100 = -16%.",
                explanationHi = "चरण 1: 100 -> 140। चरण 2: 140 -> 84। शुद्ध = 84 - 100 = -16%।"
            ),
            q(
                id = "comp_25_more",
                topic = PERCENTAGE_COMPARISON,
                difficulty = E,
                en = "If A is 25% more than B, what is A : B in lowest whole numbers?",
                hi = "यदि A, B से 25% more है, तो A : B न्यूनतम पूर्णांकों में क्या है?",
                options = listOf(
                    "5 : 4" to "5 : 4",
                    "4 : 5" to "4 : 5",
                    "6 : 4" to "6 : 4",
                    "5 : 5" to "5 : 5"
                ),
                correctIndex = 0,
                explanationEn = "Take B as 100. A is 125, so A : B = 125 : 100 = 5 : 4.",
                explanationHi = "B को 100 मानें। A 125 है, इसलिए A : B = 125 : 100 = 5 : 4।"
            ),
            q(
                id = "comp_20_less",
                topic = PERCENTAGE_COMPARISON,
                difficulty = E,
                en = "If A is 20% less than B, what is A : B in lowest whole numbers?",
                hi = "यदि A, B से 20% less है, तो A : B न्यूनतम पूर्णांकों में क्या है?",
                options = listOf(
                    "4 : 5" to "4 : 5",
                    "5 : 4" to "5 : 4",
                    "5 : 5" to "5 : 5",
                    "4 : 6" to "4 : 6"
                ),
                correctIndex = 0,
                explanationEn = "Take B as 100. A is 80, so A : B = 80 : 100 = 4 : 5.",
                explanationHi = "B को 100 मानें। A 80 है, इसलिए A : B = 80 : 100 = 4 : 5।"
            ),
            q(
                id = "comp_10_more",
                topic = PERCENTAGE_COMPARISON,
                difficulty = M,
                en = "If A is 10% more than B, what is A : B in lowest whole numbers?",
                hi = "यदि A, B से 10% more है, तो A : B न्यूनतम पूर्णांकों में क्या है?",
                options = listOf(
                    "11 : 10" to "11 : 10",
                    "10 : 11" to "10 : 11",
                    "12 : 10" to "12 : 10",
                    "11 : 11" to "11 : 11"
                ),
                correctIndex = 0,
                explanationEn = "Take B as 100. A is 110, so A : B = 110 : 100 = 11 : 10.",
                explanationHi = "B को 100 मानें। A 110 है, इसलिए A : B = 110 : 100 = 11 : 10।"
            ),
            q(
                id = "comp_40_more",
                topic = PERCENTAGE_COMPARISON,
                difficulty = M,
                en = "If A is 40% more than B, what is A : B in lowest whole numbers?",
                hi = "यदि A, B से 40% more है, तो A : B न्यूनतम पूर्णांकों में क्या है?",
                options = listOf(
                    "7 : 5" to "7 : 5",
                    "5 : 7" to "5 : 7",
                    "8 : 5" to "8 : 5",
                    "7 : 6" to "7 : 6"
                ),
                correctIndex = 0,
                explanationEn = "Take B as 100. A is 140, so A : B = 140 : 100 = 7 : 5.",
                explanationHi = "B को 100 मानें। A 140 है, इसलिए A : B = 140 : 100 = 7 : 5।"
            ),
            q(
                id = "comp_30_less",
                topic = PERCENTAGE_COMPARISON,
                difficulty = M,
                en = "If A is 30% less than B, what is A : B in lowest whole numbers?",
                hi = "यदि A, B से 30% less है, तो A : B न्यूनतम पूर्णांकों में क्या है?",
                options = listOf(
                    "7 : 10" to "7 : 10",
                    "10 : 7" to "10 : 7",
                    "8 : 10" to "8 : 10",
                    "7 : 11" to "7 : 11"
                ),
                correctIndex = 0,
                explanationEn = "Take B as 100. A is 70, so A : B = 70 : 100 = 7 : 10.",
                explanationHi = "B को 100 मानें। A 70 है, इसलिए A : B = 70 : 100 = 7 : 10।"
            ),
            q(
                id = "comp_12_5_more",
                topic = PERCENTAGE_COMPARISON,
                difficulty = H,
                en = "If A is 12.5% more than B, what is A : B in lowest whole numbers?",
                hi = "यदि A, B से 12.5% more है, तो A : B न्यूनतम पूर्णांकों में क्या है?",
                options = listOf(
                    "9 : 8" to "9 : 8",
                    "8 : 9" to "8 : 9",
                    "10 : 8" to "10 : 8",
                    "9 : 9" to "9 : 9"
                ),
                correctIndex = 0,
                explanationEn = "Take B as 100. A is 112.5, so A : B = 112.5 : 100 = 9 : 8.",
                explanationHi = "B को 100 मानें। A 112.5 है, इसलिए A : B = 112.5 : 100 = 9 : 8।"
            ),
            q(
                id = "comp_chain",
                topic = PERCENTAGE_COMPARISON,
                difficulty = H,
                en = "A is 50% more than B, and B is 20% less than C. By what percent is A more than C?",
                hi = "A, B से 50% अधिक है, और B, C से 20% कम है। A, C से कितने प्रतिशत अधिक है?",
                options = listOf(
                    "20%" to "20%",
                    "30%" to "30%",
                    "70%" to "70%",
                    "10%" to "10%"
                ),
                correctIndex = 0,
                explanationEn = "Take C as 100. B = 100 x 0.80 = 80. A = 80 x 1.50 = 120. So A is 120 - 100 = 20% more than C.",
                explanationHi = "C को 100 मानें। B = 100 x 0.80 = 80। A = 80 x 1.50 = 120। अतः A, C से 120 - 100 = 20% अधिक है।"
            ),
            q(
                id = "comp_chain_less",
                topic = PERCENTAGE_COMPARISON,
                difficulty = H,
                en = "B is 25% more than A, and C is 20% more than B. By what percent is A less than C?",
                hi = "B, A से 25% अधिक है, और C, B से 20% अधिक है। A, C से कितने प्रतिशत कम है?",
                options = listOf(
                    "33.33%" to "33.33%",
                    "45%" to "45%",
                    "20%" to "20%",
                    "40%" to "40%"
                ),
                correctIndex = 0,
                explanationEn = "Take A as 100. B = 125, then C = 125 x 1.20 = 150. So A is 50 short of 150, and 50/150 x 100 = 33.33% less than C.",
                explanationHi = "A को 100 मानें। B = 125, फिर C = 125 x 1.20 = 150। अतः A, 150 से 50 कम है, और 50/150 x 100 = 33.33% कम है।"
            ),
            q(
                id = "pts_20_to_25",
                topic = PERCENTAGE_POINTS_VS_CHANGE,
                difficulty = E,
                en = "A percentage changes from 20% to 25%. What is the change in percentage points?",
                hi = "एक प्रतिशत 20% से बदलकर 25% हो जाता है। प्रतिशत अंकों में परिवर्तन क्या है?",
                options = listOf(
                    "+5 points" to "+5 अंक",
                    "-5 points" to "-5 अंक",
                    "+10 points" to "+10 अंक",
                    "+2 points" to "+2 अंक"
                ),
                correctIndex = 0,
                explanationEn = "Percentage points is a plain subtraction: 25 - 20 = +5. The relative percentage change would be 25%, which is a different statement.",
                explanationHi = "प्रतिशत अंक सादा घटाव है: 25 - 20 = +5। सापेक्ष प्रतिशत परिवर्तन 25% होता, जो एक भिन्न कथन है।"
            ),
            q(
                id = "pts_40_to_30",
                topic = PERCENTAGE_POINTS_VS_CHANGE,
                difficulty = E,
                en = "A percentage changes from 40% to 30%. What is the change in percentage points?",
                hi = "एक प्रतिशत 40% से बदलकर 30% हो जाता है। प्रतिशत अंकों में परिवर्तन क्या है?",
                options = listOf(
                    "-10 points" to "-10 अंक",
                    "-5 points" to "-5 अंक",
                    "-2 points" to "-2 अंक",
                    "+10 points" to "+10 अंक"
                ),
                correctIndex = 0,
                explanationEn = "Percentage points is a plain subtraction: 30 - 40 = -10. The relative percentage change would be -25%, which is a different statement.",
                explanationHi = "प्रतिशत अंक सादा घटाव है: 30 - 40 = -10। सापेक्ष प्रतिशत परिवर्तन -25% होता, जो एक भिन्न कथन है।"
            ),
            q(
                id = "pts_8_to_10",
                topic = PERCENTAGE_POINTS_VS_CHANGE,
                difficulty = M,
                en = "A percentage changes from 8% to 10%. What is the change in percentage points?",
                hi = "एक प्रतिशत 8% से बदलकर 10% हो जाता है। प्रतिशत अंकों में परिवर्तन क्या है?",
                options = listOf(
                    "+2 points" to "+2 अंक",
                    "-2 points" to "-2 अंक",
                    "+4 points" to "+4 अंक",
                    "+5 points" to "+5 अंक"
                ),
                correctIndex = 0,
                explanationEn = "Percentage points is a plain subtraction: 10 - 8 = +2. The relative percentage change would be 25%, which is a different statement.",
                explanationHi = "प्रतिशत अंक सादा घटाव है: 10 - 8 = +2। सापेक्ष प्रतिशत परिवर्तन 25% होता, जो एक भिन्न कथन है।"
            ),
            q(
                id = "pts_50_to_55",
                topic = PERCENTAGE_POINTS_VS_CHANGE,
                difficulty = M,
                en = "A percentage changes from 50% to 55%. What is the change in percentage points?",
                hi = "एक प्रतिशत 50% से बदलकर 55% हो जाता है। प्रतिशत अंकों में परिवर्तन क्या है?",
                options = listOf(
                    "+5 points" to "+5 अंक",
                    "-5 points" to "-5 अंक",
                    "+10 points" to "+10 अंक",
                    "-2 points" to "-2 अंक"
                ),
                correctIndex = 0,
                explanationEn = "Percentage points is a plain subtraction: 55 - 50 = +5. The relative percentage change would be 10%, which is a different statement.",
                explanationHi = "प्रतिशत अंक सादा घटाव है: 55 - 50 = +5। सापेक्ष प्रतिशत परिवर्तन 10% होता, जो एक भिन्न कथन है।"
            ),
            q(
                id = "pts_33_to_40",
                topic = PERCENTAGE_POINTS_VS_CHANGE,
                difficulty = H,
                en = "A percentage changes from 33% to 40%. What is the change in percentage points?",
                hi = "एक प्रतिशत 33% से बदलकर 40% हो जाता है। प्रतिशत अंकों में परिवर्तन क्या है?",
                options = listOf(
                    "+7 points" to "+7 अंक",
                    "-7 points" to "-7 अंक",
                    "+5 points" to "+5 अंक",
                    "+10 points" to "+10 अंक"
                ),
                correctIndex = 0,
                explanationEn = "Percentage points is a plain subtraction: 40 - 33 = +7. The relative percentage change would be 21.21%, which is a different statement.",
                explanationHi = "प्रतिशत अंक सादा घटाव है: 40 - 33 = +7। सापेक्ष प्रतिशत परिवर्तन 21.21% होता, जो एक भिन्न कथन है।"
            ),
            q(
                id = "pts_16_to_20",
                topic = PERCENTAGE_POINTS_VS_CHANGE,
                difficulty = M,
                en = "A percentage changes from 16% to 20%. What is the change in percentage points?",
                hi = "एक प्रतिशत 16% से बदलकर 20% हो जाता है। प्रतिशत अंकों में परिवर्तन क्या है?",
                options = listOf(
                    "+4 points" to "+4 अंक",
                    "-4 points" to "-4 अंक",
                    "+2 points" to "+2 अंक",
                    "+5 points" to "+5 अंक"
                ),
                correctIndex = 0,
                explanationEn = "Percentage points is a plain subtraction: 20 - 16 = +4. The relative percentage change would be 25%, which is a different statement.",
                explanationHi = "प्रतिशत अंक सादा घटाव है: 20 - 16 = +4। सापेक्ष प्रतिशत परिवर्तन 25% होता, जो एक भिन्न कथन है।"
            ),
            q(
                id = "pts_relative",
                topic = PERCENTAGE_POINTS_VS_CHANGE,
                difficulty = M,
                en = "A pass mark rises from 10% to 15% of the total. What is the relative percentage change in the marks required?",
                hi = "उत्तीर्ण अंक कुल के 10% से बढ़कर 15% हो जाता है। आवश्यक अंकों में सापेक्ष प्रतिशत परिवर्तन क्या है?",
                options = listOf(
                    "50%" to "50%",
                    "5%" to "5%",
                    "45%" to "45%",
                    "25%" to "25%"
                ),
                correctIndex = 0,
                explanationEn = "The relative change is (15 - 10) / 10 x 100 = 5/10 x 100 = 50%. The rise is 5 percentage points, which is a different statement.",
                explanationHi = "सापेक्ष परिवर्तन (15 - 10) / 10 x 100 = 5/10 x 100 = 50%। वृद्धि 5 प्रतिशत अंक की है, जो एक भिन्न कथन है।"
            ),
            q(
                id = "pts_interest",
                topic = PERCENTAGE_POINTS_VS_CHANGE,
                difficulty = H,
                en = "An interest rate moves from 8% to 10%. On a principal of 1000, how much more interest is paid each year?",
                hi = "एक ब्याज दर 8% से 10% हो जाती है। 1000 के मूलधन पर प्रति वर्ष कितना अधिक ब्याज दिया जाता है?",
                options = listOf(
                    "20" to "20",
                    "200" to "200",
                    "2" to "2",
                    "25" to "25"
                ),
                correctIndex = 0,
                explanationEn = "8% of 1000 is 80 and 10% of 1000 is 100, so the rise of 2 percentage points costs 20 more each year.",
                explanationHi = "1000 का 8% = 80 और 1000 का 10% = 100, अतः 2 प्रतिशत अंक की वृद्धि प्रति वर्ष 20 अधिक की है।"
            ),
            q(
                id = "repr_3_4",
                topic = COMPARING_REPRESENTATIONS,
                difficulty = E,
                en = "What is 3/4 written as a percentage?",
                hi = "3/4 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "75%" to "75%",
                    "7.5%" to "7.5%",
                    "37.5%" to "37.5%",
                    "150%" to "150%"
                ),
                correctIndex = 0,
                explanationEn = "A percentage is a fraction out of 100, so 3/4 x 100 = 75%. Multiply by 100 rather than dividing, because the denominator is being compared against 100.",
                explanationHi = "प्रतिशत 100 में से एक भिन्न होता है, इसलिए 3/4 x 100 = 75%। 100 से गुणा कीजिए, भाग न कीजिए, क्योंकि निचली संख्या की तुलना 100 से हो रही है।"
            ),
            q(
                id = "repr_5_8",
                topic = COMPARING_REPRESENTATIONS,
                difficulty = E,
                en = "What is 5/8 written as a percentage?",
                hi = "5/8 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "62.5%" to "62.5%",
                    "6.25%" to "6.25%",
                    "31.25%" to "31.25%",
                    "125%" to "125%"
                ),
                correctIndex = 0,
                explanationEn = "A percentage is a fraction out of 100, so 5/8 x 100 = 62.5%. Multiply by 100 rather than dividing, because the denominator is being compared against 100.",
                explanationHi = "प्रतिशत 100 में से एक भिन्न होता है, इसलिए 5/8 x 100 = 62.5%। 100 से गुणा कीजिए, भाग न कीजिए, क्योंकि निचली संख्या की तुलना 100 से हो रही है।"
            ),
            q(
                id = "repr_7_20",
                topic = COMPARING_REPRESENTATIONS,
                difficulty = E,
                en = "What is 7/20 written as a percentage?",
                hi = "7/20 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "35%" to "35%",
                    "3.5%" to "3.5%",
                    "17.5%" to "17.5%",
                    "70%" to "70%"
                ),
                correctIndex = 0,
                explanationEn = "A percentage is a fraction out of 100, so 7/20 x 100 = 35%. Multiply by 100 rather than dividing, because the denominator is being compared against 100.",
                explanationHi = "प्रतिशत 100 में से एक भिन्न होता है, इसलिए 7/20 x 100 = 35%। 100 से गुणा कीजिए, भाग न कीजिए, क्योंकि निचली संख्या की तुलना 100 से हो रही है।"
            ),
            q(
                id = "repr_1_5",
                topic = COMPARING_REPRESENTATIONS,
                difficulty = E,
                en = "What is 1/5 written as a percentage?",
                hi = "1/5 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "20%" to "20%",
                    "2%" to "2%",
                    "10%" to "10%",
                    "40%" to "40%"
                ),
                correctIndex = 0,
                explanationEn = "A percentage is a fraction out of 100, so 1/5 x 100 = 20%. Multiply by 100 rather than dividing, because the denominator is being compared against 100.",
                explanationHi = "प्रतिशत 100 में से एक भिन्न होता है, इसलिए 1/5 x 100 = 20%। 100 से गुणा कीजिए, भाग न कीजिए, क्योंकि निचली संख्या की तुलना 100 से हो रही है।"
            ),
            q(
                id = "repr_2_3",
                topic = COMPARING_REPRESENTATIONS,
                difficulty = E,
                en = "What is 2/3 written as a percentage?",
                hi = "2/3 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "66.67%" to "66.67%",
                    "6.67%" to "6.67%",
                    "33.33%" to "33.33%",
                    "133.33%" to "133.33%"
                ),
                correctIndex = 0,
                explanationEn = "A percentage is a fraction out of 100, so 2/3 x 100 = 66.67%. Multiply by 100 rather than dividing, because the denominator is being compared against 100.",
                explanationHi = "प्रतिशत 100 में से एक भिन्न होता है, इसलिए 2/3 x 100 = 66.67%। 100 से गुणा कीजिए, भाग न कीजिए, क्योंकि निचली संख्या की तुलना 100 से हो रही है।"
            ),
            q(
                id = "repr_9_16",
                topic = COMPARING_REPRESENTATIONS,
                difficulty = E,
                en = "What is 9/16 written as a percentage?",
                hi = "9/16 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "56.25%" to "56.25%",
                    "5.625%" to "5.625%",
                    "28.125%" to "28.125%",
                    "112.5%" to "112.5%"
                ),
                correctIndex = 0,
                explanationEn = "A percentage is a fraction out of 100, so 9/16 x 100 = 56.25%. Multiply by 100 rather than dividing, because the denominator is being compared against 100.",
                explanationHi = "प्रतिशत 100 में से एक भिन्न होता है, इसलिए 9/16 x 100 = 56.25%। 100 से गुणा कीजिए, भाग न कीजिए, क्योंकि निचली संख्या की तुलना 100 से हो रही है।"
            ),
            q(
                id = "repr_11_40",
                topic = COMPARING_REPRESENTATIONS,
                difficulty = E,
                en = "What is 11/40 written as a percentage?",
                hi = "11/40 को प्रतिशत में लिखने पर क्या आता है?",
                options = listOf(
                    "27.5%" to "27.5%",
                    "2.75%" to "2.75%",
                    "13.75%" to "13.75%",
                    "55%" to "55%"
                ),
                correctIndex = 0,
                explanationEn = "A percentage is a fraction out of 100, so 11/40 x 100 = 27.5%. Multiply by 100 rather than dividing, because the denominator is being compared against 100.",
                explanationHi = "प्रतिशत 100 में से एक भिन्न होता है, इसलिए 11/40 x 100 = 27.5%। 100 से गुणा कीजिए, भाग न कीजिए, क्योंकि निचली संख्या की तुलना 100 से हो रही है।"
            ),
            q(
                id = "repr_largest",
                topic = COMPARING_REPRESENTATIONS,
                difficulty = M,
                en = "Which of these is the largest?",
                hi = "इनमें से कौन-सा सबसे बड़ा है?",
                options = listOf(
                    "3/4" to "3/4",
                    "0.65" to "0.65",
                    "70%" to "70%",
                    "2/5" to "2/5"
                ),
                correctIndex = 0,
                explanationEn = "3/4 = 75%, 0.65 = 65%, 70% = 70% and 2/5 = 40%. So 75% is the largest.",
                explanationHi = "3/4 = 75%, 0.65 = 65%, 70% = 70% और 2/5 = 40%। अतः 75% सबसे बड़ा है।"
            ),
            q(
                id = "repr_029_vs_two_sevenths",
                topic = COMPARING_REPRESENTATIONS,
                difficulty = H,
                en = "Which is larger, 0.29 or 2/7?",
                hi = "कौन बड़ा है, 0.29 या 2/7?",
                options = listOf(
                    "0.29" to "0.29",
                    "2/7" to "2/7",
                    "They are equal" to "दोनों बराबर हैं",
                    "Cannot be compared" to "तुलना नहीं की जा सकती"
                ),
                correctIndex = 0,
                explanationEn = "0.29 = 29%, and 2/7 is 28.57% when rounded to two decimals. So 0.29 is larger.",
                explanationHi = "0.29 = 29%, और 2/7 दशमलव के दो स्थानों तक 28.57% है। अतः 0.29 बड़ा है।"
            ),
            q(
                id = "short_12_5_of_640",
                topic = EXAM_SHORTCUTS,
                difficulty = E,
                en = "What is 12.5% of 640?",
                hi = "640 का 12.5% कितना है?",
                options = listOf(
                    "80" to "80",
                    "560" to "560",
                    "160" to "160",
                    "41" to "41"
                ),
                correctIndex = 0,
                explanationEn = "12.5% is one eighth, so divide by 8. 80.",
                explanationHi = "12.5% is one eighth, so divide by 8. 80।"
            ),
            q(
                id = "short_25_of_180",
                topic = EXAM_SHORTCUTS,
                difficulty = E,
                en = "What is 25% of 180?",
                hi = "180 का 25% कितना है?",
                options = listOf(
                    "45" to "45",
                    "135" to "135",
                    "90" to "90",
                    "22.5" to "22.5"
                ),
                correctIndex = 0,
                explanationEn = "25% is one quarter, so divide by 4. 45.",
                explanationHi = "25% is one quarter, so divide by 4. 45।"
            ),
            q(
                id = "short_20_of_250",
                topic = EXAM_SHORTCUTS,
                difficulty = E,
                en = "What is 20% of 250?",
                hi = "250 का 20% कितना है?",
                options = listOf(
                    "50" to "50",
                    "200" to "200",
                    "100" to "100",
                    "26" to "26"
                ),
                correctIndex = 0,
                explanationEn = "20% is one fifth, so divide by 5. 50.",
                explanationHi = "20% is one fifth, so divide by 5. 50।"
            ),
            q(
                id = "short_50_of_90",
                topic = EXAM_SHORTCUTS,
                difficulty = E,
                en = "What is 50% of 90?",
                hi = "90 का 50% कितना है?",
                options = listOf(
                    "45" to "45",
                    "90" to "90",
                    "22.5" to "22.5",
                    "50" to "50"
                ),
                correctIndex = 0,
                explanationEn = "50% is one half, so halve the number. 45.",
                explanationHi = "50% is one half, so halve the number. 45।"
            ),
            q(
                id = "short_10_of_350",
                topic = EXAM_SHORTCUTS,
                difficulty = E,
                en = "What is 10% of 350?",
                hi = "350 का 10% कितना है?",
                options = listOf(
                    "35" to "35",
                    "315" to "315",
                    "70" to "70",
                    "17.5" to "17.5"
                ),
                correctIndex = 0,
                explanationEn = "10% is one tenth, so divide by 10. 35.",
                explanationHi = "10% is one tenth, so divide by 10. 35।"
            ),
            q(
                id = "short_16_67_of_240",
                topic = EXAM_SHORTCUTS,
                difficulty = M,
                en = "What is 16.67% of 240?",
                hi = "240 का 16.67% कितना है?",
                options = listOf(
                    "40" to "40",
                    "200" to "200",
                    "80" to "80",
                    "21" to "21"
                ),
                correctIndex = 0,
                explanationEn = "16.67% is one sixth, so divide by 6. 40.",
                explanationHi = "16.67% is one sixth, so divide by 6. 40।"
            ),
            q(
                id = "short_net_40",
                topic = EXAM_SHORTCUTS,
                difficulty = H,
                en = "A value rises 40% and then falls 40%. What is the net change?",
                hi = "एक मान 40% बढ़ता है और फिर 40% घटता है। शुद्ध परिवर्तन क्या है?",
                options = listOf(
                    "-16%" to "-16%",
                    "0%" to "0%",
                    "-20%" to "-20%",
                    "16%" to "16%"
                ),
                correctIndex = 0,
                explanationEn = "100 x 1.40 = 140, then 140 x 0.60 = 84. The net change is 84 - 100 = -16%. The two percentages do not cancel.",
                explanationHi = "100 x 1.40 = 140, फिर 140 x 0.60 = 84। शुद्ध परिवर्तन 84 - 100 = -16%। दोनों प्रतिशत रद्द नहीं होते।"
            ),
            q(
                id = "short_ratio_20_more",
                topic = EXAM_SHORTCUTS,
                difficulty = M,
                en = "If A is 20% more than B, write A : B in lowest whole numbers.",
                hi = "यदि A, B से 20% अधिक है, तो A : B न्यूनतम पूर्णांकों में लिखें।",
                options = listOf(
                    "6 : 5" to "6 : 5",
                    "5 : 6" to "5 : 6",
                    "120 : 100" to "120 : 100",
                    "20 : 1" to "20 : 1"
                ),
                correctIndex = 0,
                explanationEn = "Take B as 100, so A is 120. Then 120 : 100 = 6 : 5.",
                explanationHi = "B को 100 मानें, तो A 120 है। फिर 120 : 100 = 6 : 5।"
            ),
            q(
                id = "pop_50k_up_12",
                topic = POPULATION_CHANGE,
                difficulty = E,
                en = "A town of 50000 people changes by 12%. What is the new population?",
                hi = "50000 की आबादी 12% से बदलती है। नई आबादी क्या होगी?",
                options = listOf(
                    "56000" to "56000",
                    "44000" to "44000",
                    "62000" to "62000",
                    "-6000" to "-6000"
                ),
                correctIndex = 0,
                explanationEn = "12% of 50000 = 6000, so 50000 + 6000 = 56000.",
                explanationHi = "50000 का 12% = 6000, अतः 50000 + 6000 = 56000।"
            ),
            q(
                id = "pop_20k_down_15",
                topic = POPULATION_CHANGE,
                difficulty = E,
                en = "A town of 20000 people changes by 15%. What is the new population?",
                hi = "20000 की आबादी 15% से बदलती है। नई आबादी क्या होगी?",
                options = listOf(
                    "17000" to "17000",
                    "23000" to "23000",
                    "20000" to "20000",
                    "3000" to "3000"
                ),
                correctIndex = 0,
                explanationEn = "15% of 20000 = 3000, so 20000 - 3000 = 17000.",
                explanationHi = "20000 का 15% = 3000, अतः 20000 - 3000 = 17000।"
            ),
            q(
                id = "pop_32k_up_12",
                topic = POPULATION_CHANGE,
                difficulty = M,
                en = "A town of 32000 people changes by 12%. What is the new population?",
                hi = "32000 की आबादी 12% से बदलती है। नई आबादी क्या होगी?",
                options = listOf(
                    "35840" to "35840",
                    "28160" to "28160",
                    "39680" to "39680",
                    "-3840" to "-3840"
                ),
                correctIndex = 0,
                explanationEn = "12% of 32000 = 3840, so 32000 + 3840 = 35840.",
                explanationHi = "32000 का 12% = 3840, अतः 32000 + 3840 = 35840।"
            ),
            q(
                id = "pop_45k_down_8",
                topic = POPULATION_CHANGE,
                difficulty = M,
                en = "A town of 45000 people changes by 8%. What is the new population?",
                hi = "45000 की आबादी 8% से बदलती है। नई आबादी क्या होगी?",
                options = listOf(
                    "41400" to "41400",
                    "48600" to "48600",
                    "45000" to "45000",
                    "3600" to "3600"
                ),
                correctIndex = 0,
                explanationEn = "8% of 45000 = 3600, so 45000 - 3600 = 41400.",
                explanationHi = "45000 का 8% = 3600, अतः 45000 - 3600 = 41400।"
            ),
            q(
                id = "pop_two_changes",
                topic = POPULATION_CHANGE,
                difficulty = H,
                en = "A town of 12000 people grows by 25% and then falls by 10%. What is the population at the end?",
                hi = "12000 की आबादी 25% बढ़ती है और फिर 10% घटती है। अंत में आबादी क्या होगी?",
                options = listOf(
                    "13500" to "13500",
                    "14400" to "14400",
                    "13000" to "13000",
                    "13800" to "13800"
                ),
                correctIndex = 0,
                explanationEn = "12000 x 1.25 = 15000, then 15000 x 0.90 = 13500. The fall is measured on the population after the rise.",
                explanationHi = "12000 x 1.25 = 15000, फिर 15000 x 0.90 = 13500। गिरावट वृद्धि के बाद की आबादी पर मापी जाती है।"
            ),
            q(
                id = "pop_rate_of_change",
                topic = POPULATION_CHANGE,
                difficulty = M,
                en = "A city's population grew from 40000 to 46000 in one year. What was the percentage growth?",
                hi = "एक शहर की आबादी एक वर्ष में 40000 से बढ़कर 46000 हो गई। प्रतिशत वृद्धि क्या थी?",
                options = listOf(
                    "15%" to "15%",
                    "60%" to "60%",
                    "13%" to "13%",
                    "16%" to "16%"
                ),
                correctIndex = 0,
                explanationEn = "The growth is 46000 - 40000 = 6000, and 6000 / 40000 x 100 = 15%.",
                explanationHi = "वृद्धि 46000 - 40000 = 6000 है, और 6000 / 40000 x 100 = 15%।"
            ),
            q(
                id = "pop_comparison",
                topic = POPULATION_CHANGE,
                difficulty = M,
                en = "Town A's population is 25% more than town B's. If town B has 48000 people, how many does town A have?",
                hi = "कस्बे A की आबादी कस्बे B से 25% अधिक है। यदि कस्बे B में 48000 लोग हैं, तो कस्बे A में कितने हैं?",
                options = listOf(
                    "60000" to "60000",
                    "64000" to "64000",
                    "55200" to "55200",
                    "52000" to "52000"
                ),
                correctIndex = 0,
                explanationEn = "48000 x 1.25 = 60000.",
                explanationHi = "48000 x 1.25 = 60000।"
            ),
            q(
                id = "pop_fall_20",
                topic = POPULATION_CHANGE,
                difficulty = E,
                en = "A population falls by 20% from 25000. What is the new population?",
                hi = "25000 की आबादी 20% घटती है। नई आबादी क्या है?",
                options = listOf(
                    "20000" to "20000",
                    "23000" to "23000",
                    "18000" to "18000",
                    "25000" to "25000"
                ),
                correctIndex = 0,
                explanationEn = "20% of 25000 = 5000, so 25000 - 5000 = 20000.",
                explanationHi = "25000 का 20% = 5000, अतः 25000 - 5000 = 20000।"
            ),
            q(
                id = "sal_spend_40",
                topic = SALARY_EXPENDITURE_SAVINGS,
                difficulty = E,
                en = "A monthly salary is 25000 and 40% of it is spent. How much is saved?",
                hi = "मासिक वेतन 25000 है और उसका 40% खर्च होता है। कितना बचता है?",
                options = listOf(
                    "15000" to "15000",
                    "10000" to "10000",
                    "6000" to "6000",
                    "20000" to "20000"
                ),
                correctIndex = 0,
                explanationEn = "40% of 25000 = 10000 is spent, so 25000 - 10000 = 15000 is saved.",
                explanationHi = "25000 का 40% = 10000 खर्च होता है, अतः 25000 - 10000 = 15000 बचता है।"
            ),
            q(
                id = "sal_savings_percent",
                topic = SALARY_EXPENDITURE_SAVINGS,
                difficulty = M,
                en = "Out of a salary of 40000, the expenditure is 24000. What percentage of the salary is the savings?",
                hi = "40000 के वेतन में से 24000 खर्च होते हैं। वेतन का कितने प्रतिशत बचत है?",
                options = listOf(
                    "40%" to "40%",
                    "60%" to "60%",
                    "37.5%" to "37.5%",
                    "16%" to "16%"
                ),
                correctIndex = 0,
                explanationEn = "Savings = 40000 - 24000 = 16000, and 16000 / 40000 x 100 = 40%.",
                explanationHi = "बचत = 40000 - 24000 = 16000, और 16000 / 40000 x 100 = 40%।"
            ),
            q(
                id = "sal_exp_60",
                topic = SALARY_EXPENDITURE_SAVINGS,
                difficulty = M,
                en = "A person spends 60% of a salary of 50000. How much is left after spending?",
                hi = "एक व्यक्ति 50000 के वेतन का 60% खर्च करता है। खर्च के बाद कितना बचता है?",
                options = listOf(
                    "20000" to "20000",
                    "30000" to "30000",
                    "25000" to "25000",
                    "40000" to "40000"
                ),
                correctIndex = 0,
                explanationEn = "60% of 50000 = 30000 is spent, so 50000 - 30000 = 20000 remains.",
                explanationHi = "50000 का 60% = 30000 खर्च होता है, अतः 50000 - 30000 = 20000 बचता है।"
            ),
            q(
                id = "sal_exp_is_80pct",
                topic = SALARY_EXPENDITURE_SAVINGS,
                difficulty = M,
                en = "The monthly expenditure is 12000, which is 80% of the salary. What is the salary?",
                hi = "मासिक व्यय 12000 है, जो वेतन का 80% है। वेतन क्या है?",
                options = listOf(
                    "15000" to "15000",
                    "12000" to "12000",
                    "9600" to "9600",
                    "18000" to "18000"
                ),
                correctIndex = 0,
                explanationEn = "12000 / 0.80 = 15000. Check: 80% of 15000 = 12000.",
                explanationHi = "12000 / 0.80 = 15000। जाँच: 15000 का 80% = 12000।"
            ),
            q(
                id = "sal_exp_rises",
                topic = SALARY_EXPENDITURE_SAVINGS,
                difficulty = H,
                en = "Out of an income of 36000, expenditure was 24000. If expenditure rises by 20%, what is the new savings?",
                hi = "36000 की आय में से 24000 खर्च होते हैं। यदि व्यय 20% बढ़ जाए, तो नई बचत क्या होगी?",
                options = listOf(
                    "7200" to "7200",
                    "8160" to "8160",
                    "9600" to "9600",
                    "12000" to "12000"
                ),
                correctIndex = 0,
                explanationEn = "Expenditure rises by 24000 x 0.20 = 4800 to 28800. Savings = 36000 - 28800 = 7200.",
                explanationHi = "व्यय 24000 x 0.20 = 4800 बढ़कर 28800 हो जाता है। बचत = 36000 - 28800 = 7200।"
            ),
            q(
                id = "sal_save_30pct",
                topic = SALARY_EXPENDITURE_SAVINGS,
                difficulty = E,
                en = "A person saves 30% of 9000. How much is spent?",
                hi = "एक व्यक्ति 9000 का 30% बचाता है। कितना खर्च होता है?",
                options = listOf(
                    "6300" to "6300",
                    "2700" to "2700",
                    "6000" to "6000",
                    "3300" to "3300"
                ),
                correctIndex = 0,
                explanationEn = "Savings = 30% of 9000 = 2700, so expenditure = 9000 - 2700 = 6300.",
                explanationHi = "बचत = 9000 का 30% = 2700, अतः व्यय = 9000 - 2700 = 6300।"
            ),
            q(
                id = "sal_raise_then_cut",
                topic = SALARY_EXPENDITURE_SAVINGS,
                difficulty = M,
                en = "A salary of 20000 rises by 25% and then falls by 10%. What is the final salary?",
                hi = "20000 का वेतन 25% बढ़ता है और फिर 10% घटता है। अंतिम वेतन क्या है?",
                options = listOf(
                    "22500" to "22500",
                    "21000" to "21000",
                    "24000" to "24000",
                    "20000" to "20000"
                ),
                correctIndex = 0,
                explanationEn = "20000 x 1.25 = 25000, then 25000 x 0.90 = 22500. The net change is 12.5%.",
                explanationHi = "20000 x 1.25 = 25000, फिर 25000 x 0.90 = 22500। शुद्ध परिवर्तन 12.5% है।"
            ),
            q(
                id = "marks_pass_40",
                topic = MARKS_AND_RESULTS,
                difficulty = E,
                en = "The pass mark is 40% of a maximum of 500. How many marks are needed to pass?",
                hi = "500 के अधिकतम अंकों पर उत्तीर्ण अंक 40% है। उत्तीर्ण होने के लिए कितने अंक चाहिए?",
                options = listOf(
                    "200" to "200",
                    "250" to "250",
                    "150" to "150",
                    "220" to "220"
                ),
                correctIndex = 0,
                explanationEn = "40% of 500 = 200 marks are needed to pass.",
                explanationHi = "उत्तीर्ण होने के लिए 500 का 40% = 200 अंक चाहिए।"
            ),
            q(
                id = "marks_score_80pct",
                topic = MARKS_AND_RESULTS,
                difficulty = E,
                en = "A student scored 360 marks out of 450. What percentage did the student score?",
                hi = "एक विद्यार्थी ने 450 में से 360 अंक अर्जित किए। विद्यार्थी ने कितने प्रतिशत अंक ले?",
                options = listOf(
                    "80%" to "80%",
                    "75%" to "75%",
                    "86.67%" to "86.67%",
                    "70%" to "70%"
                ),
                correctIndex = 0,
                explanationEn = "360 / 450 x 100 = 80%.",
                explanationHi = "360 / 450 x 100 = 80%।"
            ),
            q(
                id = "marks_shortfall",
                topic = MARKS_AND_RESULTS,
                difficulty = M,
                en = "A candidate needs 240 marks to pass. The candidate scores 216. By how many marks did the candidate fall short?",
                hi = "उत्तीर्ण होने के लिए 240 अंक चाहिए। उम्मीदवार ने 216 अंक ले। कितने अंक की कमी रह गई?",
                options = listOf(
                    "24" to "24",
                    "20" to "20",
                    "25" to "25",
                    "30" to "30"
                ),
                correctIndex = 0,
                explanationEn = "240 - 216 = 24 marks short.",
                explanationHi = "240 - 216 = 24 अंक की कमी।"
            ),
            q(
                id = "marks_pass_rate",
                topic = MARKS_AND_RESULTS,
                difficulty = M,
                en = "In an examination of 500 candidates, 250 passed. What percentage passed?",
                hi = "500 उम्मीदवारों की परीक्षा में 250 उत्तीर्ण हुए। कितने प्रतिशत उत्तीर्ण हुए?",
                options = listOf(
                    "50%" to "50%",
                    "25%" to "25%",
                    "75%" to "75%",
                    "45%" to "45%"
                ),
                correctIndex = 0,
                explanationEn = "250 / 500 x 100 = 50% passed.",
                explanationHi = "250 / 500 x 100 = 50% उत्तीर्ण हुए।"
            ),
            q(
                id = "marks_avg_vs_topper",
                topic = MARKS_AND_RESULTS,
                difficulty = H,
                en = "The class average is 60% and the topper scored 85%. By what percentage is the topper's score higher than the average?",
                hi = "कक्षा का औसत 60% है और शीर्षक ने 85% ले। शीर्षक के अंक औसत से कितने प्रतिशत अधिक हैं?",
                options = listOf(
                    "41.67%" to "41.67%",
                    "25%" to "25%",
                    "45%" to "45%",
                    "40%" to "40%"
                ),
                correctIndex = 0,
                explanationEn = "The gap is 85 - 60 = 25 percentage points, and measured against the average of 60 that is 25 / 60 x 100 = 41.67%.",
                explanationHi = "अंतर 85 - 60 = 25 प्रतिशत अंक है, और औसत 60 के सापेक्ष यह 25 / 60 x 100 = 41.67% है।"
            ),
            q(
                id = "marks_pass_rises",
                topic = MARKS_AND_RESULTS,
                difficulty = M,
                en = "Pass marks rise from 40 to 50 out of 200. What is the percentage change in the marks required?",
                hi = "200 में से उत्तीर्ण अंक 40 से बढ़कर 50 हो जाते हैं। आवश्यक अंकों में प्रतिशत परिवर्तन क्या है?",
                options = listOf(
                    "25%" to "25%",
                    "10%" to "10%",
                    "20%" to "20%",
                    "12.5%" to "12.5%"
                ),
                correctIndex = 0,
                explanationEn = "The relative change is (50 - 40) / 40 x 100 = 25%. The pass percentage also moved from 20% to 25%, which is 5 percentage points.",
                explanationHi = "सापेक्ष परिवर्तन (50 - 40) / 40 x 100 = 25%। उत्तीर्ण प्रतिशत भी 20% से 25% हुआ, जो 5 प्रतिशत अंक है।"
            ),
            q(
                id = "marks_needs_21",
                topic = MARKS_AND_RESULTS,
                difficulty = M,
                en = "Out of 300, a student must score 40% to pass. The student has 99. How many more marks are needed?",
                hi = "300 में से उत्तीर्ण होने के लिए 40% चाहिए। विद्यार्थी के पास 99 हैं। कितने और अंक चाहिए?",
                options = listOf(
                    "21" to "21",
                    "20" to "20",
                    "22" to "22",
                    "25" to "25"
                ),
                correctIndex = 0,
                explanationEn = "40% of 300 = 120, so 120 - 99 = 21 more marks are needed.",
                explanationHi = "300 का 40% = 120, अतः 120 - 99 = 21 और अंक चाहिए।"
            ),
            q(
                id = "vote_55pct",
                topic = ELECTION_VOTING,
                difficulty = E,
                en = "In an election, 55% of 60000 valid votes were cast for candidate A. How many votes did A receive?",
                hi = "चुनाव में 60000 वैध मतों के 55% उम्मीदवार A को मिले। A को कितने मत मिले?",
                options = listOf(
                    "33000" to "33000",
                    "35000" to "35000",
                    "60000" to "60000",
                    "30000" to "30000"
                ),
                correctIndex = 0,
                explanationEn = "55% of 60000 = 0.55 x 60000 = 33000 votes.",
                explanationHi = "60000 का 55% = 0.55 x 60000 = 33000 मत।"
            ),
            q(
                id = "vote_turnout",
                topic = ELECTION_VOTING,
                difficulty = M,
                en = "There are 75000 registered voters and 45000 of them cast a ballot. What was the voting percentage?",
                hi = "75000 पंजीकृत मतदाता हैं और उनमें से 45000 ने मत डाला। मतदान प्रतिशत क्या था?",
                options = listOf(
                    "60%" to "60%",
                    "45%" to "45%",
                    "55%" to "55%",
                    "66.67%" to "66.67%"
                ),
                correctIndex = 0,
                explanationEn = "45000 / 75000 x 100 = 60% of registered voters turned out.",
                explanationHi = "45000 / 75000 x 100 = 60% पंजीकृत मतदाताओं ने मत डाला।"
            ),
            q(
                id = "vote_others_share",
                topic = ELECTION_VOTING,
                difficulty = M,
                en = "In an election, candidate A got 40% of the votes and candidate B got 35%. What percentage went to all other candidates together?",
                hi = "चुनाव में उम्मीदवार A को 40% मत मिले और B को 35%। शेष सभी उम्मीदवारों को कुल कितने प्रतिशत मिले?",
                options = listOf(
                    "25%" to "25%",
                    "15%" to "15%",
                    "5%" to "5%",
                    "75%" to "75%"
                ),
                correctIndex = 0,
                explanationEn = "The two leading candidates take 40% + 35% = 75%, so the rest take 100% - 75% = 25%.",
                explanationHi = "दो शीर्ष उम्मीदवार 40% + 35% = 75% लेते हैं, अतः शेष को 100% - 75% = 25%।"
            ),
            q(
                id = "vote_margin_points",
                topic = ELECTION_VOTING,
                difficulty = M,
                en = "Candidate A polled 44% of the votes and candidate B polled 38%. What is the margin in percentage points?",
                hi = "उम्मीदवार A को 44% मत मिले और B को 38%। प्रतिशत अंकों में अंतर क्या है?",
                options = listOf(
                    "6 points" to "6 अंक",
                    "16%" to "16%",
                    "5%" to "5%",
                    "8%" to "8%"
                ),
                correctIndex = 0,
                explanationEn = "The margin in percentage points is 44 - 38 = 6. Measured against A's share the relative margin would be 6 / 38 x 100 = 15.79%.",
                explanationHi = "प्रतिशत अंकों में अंतर 44 - 38 = 6 है। A के हिस्से के सापेक्ष सापेक्ष अंतर 6 / 38 x 100 = 15.79% होता।"
            ),
            q(
                id = "vote_comparison",
                topic = ELECTION_VOTING,
                difficulty = M,
                en = "Candidate A got 36000 votes, which is 20% more than candidate B. How many votes did B get?",
                hi = "उम्मीदवार A को 36000 मत मिले, जो B से 20% अधिक हैं। B को कितने मत मिले?",
                options = listOf(
                    "30000" to "30000",
                    "36000" to "36000",
                    "40000" to "40000",
                    "28800" to "28800"
                ),
                correctIndex = 0,
                explanationEn = "If B = x then A = 1.2x, so 1.2x = 36000 and x = 30000.",
                explanationHi = "यदि B = x तो A = 1.2x, अतः 1.2x = 36000 और x = 30000।"
            ),
            q(
                id = "vote_two_changes",
                topic = ELECTION_VOTING,
                difficulty = H,
                en = "Voters at a booth rose by 20% and then fell by 20%. What was the net change in the number of voters?",
                hi = "एक बूथ पर मतदाता 20% बढ़े और फिर 20% घटे। मतदाताओं की संख्या में शुद्ध परिवर्तन क्या था?",
                options = listOf(
                    "-4%" to "-4%",
                    "0%" to "0%",
                    "-20%" to "-20%",
                    "4%" to "4%"
                ),
                correctIndex = 0,
                explanationEn = "Taking 100 voters: 120 after the rise, then 120 x 0.80 = 96. That is 4 fewer than the original, so -4%.",
                explanationHi = "100 मतदाता लें: वृद्धि के बाद 120, फिर 120 x 0.80 = 96। यह मूल से 4 कम है, अतः -4%।"
            ),
            q(
                id = "vote_45pct_2000",
                topic = ELECTION_VOTING,
                difficulty = E,
                en = "A party polled 45% of 2000 votes in a ward. How many votes did it get?",
                hi = "एक पार्टी को एक वार्ड में 2000 मतों के 45% मिले। उसे कितने मत मिले?",
                options = listOf(
                    "900" to "900",
                    "950" to "950",
                    "1100" to "1100",
                    "855" to "855"
                ),
                correctIndex = 0,
                explanationEn = "45% of 2000 = 0.45 x 2000 = 900 votes.",
                explanationHi = "2000 का 45% = 0.45 x 2000 = 900 मत।"
            ),
            q(
                id = "price_disc_20",
                topic = PRICE_DISCOUNT_TAX,
                difficulty = E,
                en = "A jacket marked at 2500 is sold with a 20% discount. What is the selling price?",
                hi = "2500 पर अंकित जैकेट 20% छूट पर बेचा जाता है। विक्रय मूल्य क्या है?",
                options = listOf(
                    "2000" to "2000",
                    "2300" to "2300",
                    "2100" to "2100",
                    "1800" to "1800"
                ),
                correctIndex = 0,
                explanationEn = "20% of 2500 = 500, so 2500 - 500 = 2000.",
                explanationHi = "2500 का 20% = 500, अतः 2500 - 500 = 2000।"
            ),
            q(
                id = "price_two_discounts",
                topic = PRICE_DISCOUNT_TAX,
                difficulty = H,
                en = "Two discounts of 20% and then 10% are applied in turn to a price of 1000. What is the final price?",
                hi = "1000 के मूल्य पर क्रमशः 20% और फिर 10% की छूट लागू होती है। अंतिम मूल्य क्या है?",
                options = listOf(
                    "720" to "720",
                    "700" to "700",
                    "750" to "750",
                    "800" to "800"
                ),
                correctIndex = 0,
                explanationEn = "1000 x 0.80 = 800, then 800 x 0.90 = 720. The net discount is 28%, not 30%, because the second discount is measured on 800.",
                explanationHi = "1000 x 0.80 = 800, फिर 800 x 0.90 = 720। शुद्ध छूट 30% नहीं, 28% है, क्योंकि दूसरी छूट 800 पर मापी जाती है।"
            ),
            q(
                id = "price_net_discount",
                topic = PRICE_DISCOUNT_TAX,
                difficulty = M,
                en = "Successive discounts of 20% and 10% give an overall discount of how much percent?",
                hi = "20% और 10% की लगातार छूट से कुल छूट कितने प्रतिशत होती है?",
                options = listOf(
                    "28%" to "28%",
                    "30%" to "30%",
                    "25%" to "25%",
                    "32%" to "32%"
                ),
                correctIndex = 0,
                explanationEn = "On a price of 100: 100 x 0.80 x 0.90 = 72, so the overall discount is 100 - 72 = 28%.",
                explanationHi = "100 के मूल्य पर: 100 x 0.80 x 0.90 = 72, अतः कुल छूट 100 - 72 = 28%।"
            ),
            q(
                id = "price_tax_incl",
                topic = PRICE_DISCOUNT_TAX,
                difficulty = H,
                en = "A price of 550 already includes 10% tax. How much of it is tax?",
                hi = "550 का मूल्य में 10% कर पहले से शामिल है। उसमें से कितना कर है?",
                options = listOf(
                    "50" to "50",
                    "55" to "55",
                    "60" to "60",
                    "45" to "45"
                ),
                correctIndex = 0,
                explanationEn = "If 550 is 110% of the price before tax, then the price before tax is 550 / 1.10 = 500, so the tax is 550 - 500 = 50.",
                explanationHi = "यदि 550, कर से पहले मूल्य का 110% है, तो कर से पहले मूल्य 550 / 1.10 = 500 है, अतः कर 550 - 500 = 50 है।"
            ),
            q(
                id = "price_disc_then_rise",
                topic = PRICE_DISCOUNT_TAX,
                difficulty = M,
                en = "A price of 1000 falls 40% and then rises 25%. What is the final price?",
                hi = "1000 का मूल्य 40% घटता है और फिर 25% बढ़ता है। अंतिम मूल्य क्या है?",
                options = listOf(
                    "750" to "750",
                    "850" to "850",
                    "600" to "600",
                    "900" to "900"
                ),
                correctIndex = 0,
                explanationEn = "1000 x 0.60 = 600, then 600 x 1.25 = 750. The net change is -25%.",
                explanationHi = "1000 x 0.60 = 600, फिर 600 x 1.25 = 750। शुद्ध परिवर्तन -25% है।"
            ),
            q(
                id = "price_discount_then_tax",
                topic = PRICE_DISCOUNT_TAX,
                difficulty = M,
                en = "A 2000 rupee item gets 20% off, then 10% tax is added on the reduced price. What is the final amount?",
                hi = "2000 रुपये की वस्तु पर 20% छूट, फिर घटी हुई मूल्य पर 10% कर जोड़ा जाता है। अंतिम राशि क्या है?",
                options = listOf(
                    "1760" to "1760",
                    "1800" to "1800",
                    "1980" to "1980",
                    "1700" to "1700"
                ),
                correctIndex = 0,
                explanationEn = "2000 x 0.80 = 1600, then 1600 x 1.10 = 1760.",
                explanationHi = "2000 x 0.80 = 1600, फिर 1600 x 1.10 = 1760।"
            ),
            q(
                id = "price_disc_25_640",
                topic = PRICE_DISCOUNT_TAX,
                difficulty = E,
                en = "What is 25% off a price of 640?",
                hi = "640 के मूल्य पर 25% छूट कितनी है?",
                options = listOf(
                    "160" to "160",
                    "480" to "480",
                    "615" to "615",
                    "1600" to "1600"
                ),
                correctIndex = 0,
                explanationEn = "25% of 640 = 640 / 4 = 160, so the discount is 160.",
                explanationHi = "640 का 25% = 640 / 4 = 160, अतः छूट 160 है।"
            ),
            q(
                id = "profit_25_on_400",
                topic = PROFIT_LOSS,
                difficulty = E,
                en = "An item is bought for 400 and sold for 500. What is the profit percentage?",
                hi = "एक वस्तु 400 में खरीदी और 500 में बेची जाती है। लाभ प्रतिशत क्या है?",
                options = listOf(
                    "25%" to "25%",
                    "20%" to "20%",
                    "125%" to "125%",
                    "33.33%" to "33.33%"
                ),
                correctIndex = 0,
                explanationEn = "Profit is 500 - 400 = 100, and profit percentage is measured on the cost price: 100 / 400 x 100 = 25%.",
                explanationHi = "लाभ 500 - 400 = 100 है, और लाभ प्रतिशत क्रय मूल्य पर मापा जाता है: 100 / 400 x 100 = 25%।"
            ),
            q(
                id = "loss_10_on_600",
                topic = PROFIT_LOSS,
                difficulty = E,
                en = "An item is bought for 600 and sold for 540. What is the loss percentage?",
                hi = "एक वस्तु 600 में खरीदी और 540 में बेची जाती है। हानि प्रतिशत क्या है?",
                options = listOf(
                    "10%" to "10%",
                    "11.11%" to "11.11%",
                    "60%" to "60%",
                    "90%" to "90%"
                ),
                correctIndex = 0,
                explanationEn = "Loss is 600 - 540 = 60, and 60 / 600 x 100 = 10%. The percentage is measured on the cost price, not the selling price.",
                explanationHi = "हानि 600 - 540 = 60 है, और 60 / 600 x 100 = 10%। प्रतिशत क्रय मूल्य पर मापा जाता है, विक्रय मूल्य पर नहीं।"
            ),
            q(
                id = "profit_cp_from_sp",
                topic = PROFIT_LOSS,
                difficulty = M,
                en = "An item sold for 1200 makes a 20% profit. What was its cost price?",
                hi = "एक वस्तु 1200 में बेची जाती है और 20% लाभ होता है। उसका क्रय मूल्य क्या था?",
                options = listOf(
                    "1000" to "1000",
                    "1200" to "1200",
                    "960" to "960",
                    "1100" to "1100"
                ),
                correctIndex = 0,
                explanationEn = "The selling price is 120% of the cost, so cost = 1200 / 1.20 = 1000.",
                explanationHi = "विक्रय मूल्य क्रय का 120% है, अतः क्रय = 1200 / 1.20 = 1000।"
            ),
            q(
                id = "loss_cp_from_sp",
                topic = PROFIT_LOSS,
                difficulty = M,
                en = "An item sold for 900 at a 10% loss. What was its cost price?",
                hi = "एक वस्तु 10% हानि पर 900 में बेची जाती है। उसका क्रय मूल्य क्या था?",
                options = listOf(
                    "1000" to "1000",
                    "900" to "900",
                    "810" to "810",
                    "990" to "990"
                ),
                correctIndex = 0,
                explanationEn = "The selling price is 90% of the cost, so cost = 900 / 0.90 = 1000.",
                explanationHi = "विक्रय मूल्य क्रय का 90% है, अतः क्रय = 900 / 0.90 = 1000।"
            ),
            q(
                id = "profit_then_loss",
                topic = PROFIT_LOSS,
                difficulty = H,
                en = "An item bought for 100 rises 25% and then falls 25%. What is the final price, and how does it compare with the cost?",
                hi = "100 में खरीदी गई वस्तु 25% बढ़ती है और फिर 25% घटती है। अंतिम मूल्य क्या है, और यह क्रय की तुलना में कैसा है?",
                options = listOf(
                    "93.75, a loss of 6.25%" to "93.75, 6.25% की हानि",
                    "100, no change" to "100, कोई परिवर्तन नहीं",
                    "106.25, a gain of 6.25%" to "106.25, 6.25% का लाभ",
                    "90, a loss of 10%" to "90, 10% की हानि"
                ),
                correctIndex = 0,
                explanationEn = "100 x 1.25 = 125, then 125 x 0.75 = 93.75. That is 6.25% below the cost price, so the two percentages do not cancel.",
                explanationHi = "100 x 1.25 = 125, फिर 125 x 0.75 = 93.75। यह क्रय मूल्य से 6.25% नीचे है, अतः दोनों प्रतिशत रद्द नहीं होते।"
            ),
            q(
                id = "profit_amount",
                topic = PROFIT_LOSS,
                difficulty = E,
                en = "Selling with a 20% profit gives a selling price of 1200. What is the profit?",
                hi = "20% लाभ पर विक्रय मूल्य 1200 है। लाभ कितना है?",
                options = listOf(
                    "200" to "200",
                    "240" to "240",
                    "300" to "300",
                    "1000" to "1000"
                ),
                correctIndex = 0,
                explanationEn = "Cost = 1200 / 1.20 = 1000, so profit = 1200 - 1000 = 200. The 20% is measured on the cost price, which is smaller than the selling price.",
                explanationHi = "क्रय = 1200 / 1.20 = 1000, अतः लाभ = 1200 - 1000 = 200। 20% क्रय मूल्य पर मापा जाता है, जो विक्रय मूल्य से छोटा है।"
            ),
            q(
                id = "profit_28_on_250",
                topic = PROFIT_LOSS,
                difficulty = M,
                en = "An item bought for 250 is sold for 320. What is the profit percentage?",
                hi = "250 में खरीदी गई वस्तु 320 में बेची जाती है। लाभ प्रतिशत क्या है?",
                options = listOf(
                    "28%" to "28%",
                    "22%" to "22%",
                    "70%" to "70%",
                    "25%" to "25%"
                ),
                correctIndex = 0,
                explanationEn = "Profit = 320 - 250 = 70, and 70 / 250 x 100 = 28%.",
                explanationHi = "लाभ = 320 - 250 = 70, और 70 / 250 x 100 = 28%।"
            ),
            q(
                id = "rev_up_20_to_720",
                topic = REVERSE_PERCENTAGE,
                difficulty = E,
                en = "A value 20% increases to become 720. What was the original value?",
                hi = "एक मान 20% बढ़कर 720 हो जाता है। मूल मान क्या था?",
                options = listOf(
                    "600" to "600",
                    "720" to "720",
                    "144" to "144",
                    "620" to "620"
                ),
                correctIndex = 0,
                explanationEn = "The final value is 120% of the original, so the original is 720 / 1.2 = 600. Check: 20% of 600 changes it to 720.",
                explanationHi = "अंतिम मान मूल का 1.2 है, अतः मूल 720 / 1.2 = 600। जाँच: 600 का 20% बदलकर 720 कर देता है।"
            ),
            q(
                id = "rev_down_10_to_450",
                topic = REVERSE_PERCENTAGE,
                difficulty = E,
                en = "A value 10% decreases to become 450. What was the original value?",
                hi = "एक मान 10% घटकर 450 हो जाता है। मूल मान क्या था?",
                options = listOf(
                    "500" to "500",
                    "450" to "450",
                    "45" to "45",
                    "510" to "510"
                ),
                correctIndex = 0,
                explanationEn = "The final value is 90% of the original, so the original is 450 / 0.9 = 500. Check: 10% of 500 changes it to 450.",
                explanationHi = "अंतिम मान मूल का 0.9 है, अतः मूल 450 / 0.9 = 500। जाँच: 500 का 10% बदलकर 450 कर देता है।"
            ),
            q(
                id = "rev_up_25_to_1250",
                topic = REVERSE_PERCENTAGE,
                difficulty = M,
                en = "A value 25% increases to become 1250. What was the original value?",
                hi = "एक मान 25% बढ़कर 1250 हो जाता है। मूल मान क्या था?",
                options = listOf(
                    "1000" to "1000",
                    "1250" to "1250",
                    "312.5" to "312.5",
                    "1025" to "1025"
                ),
                correctIndex = 0,
                explanationEn = "The final value is 125% of the original, so the original is 1250 / 1.25 = 1000. Check: 25% of 1000 changes it to 1250.",
                explanationHi = "अंतिम मान मूल का 1.25 है, अतः मूल 1250 / 1.25 = 1000। जाँच: 1000 का 25% बदलकर 1250 कर देता है।"
            ),
            q(
                id = "rev_down_20_to_640",
                topic = REVERSE_PERCENTAGE,
                difficulty = M,
                en = "A value 20% decreases to become 640. What was the original value?",
                hi = "एक मान 20% घटकर 640 हो जाता है। मूल मान क्या था?",
                options = listOf(
                    "800" to "800",
                    "640" to "640",
                    "128" to "128",
                    "820" to "820"
                ),
                correctIndex = 0,
                explanationEn = "The final value is 80% of the original, so the original is 640 / 0.8 = 800. Check: 20% of 800 changes it to 640.",
                explanationHi = "अंतिम मान मूल का 0.8 है, अतः मूल 640 / 0.8 = 800। जाँच: 800 का 20% बदलकर 640 कर देता है।"
            ),
            q(
                id = "rev_up_40_to_1260",
                topic = REVERSE_PERCENTAGE,
                difficulty = M,
                en = "A value 40% increases to become 1260. What was the original value?",
                hi = "एक मान 40% बढ़कर 1260 हो जाता है। मूल मान क्या था?",
                options = listOf(
                    "900" to "900",
                    "1260" to "1260",
                    "504" to "504",
                    "940" to "940"
                ),
                correctIndex = 0,
                explanationEn = "The final value is 140% of the original, so the original is 1260 / 1.4 = 900. Check: 40% of 900 changes it to 1260.",
                explanationHi = "अंतिम मान मूल का 1.4 है, अतः मूल 1260 / 1.4 = 900। जाँच: 900 का 40% बदलकर 1260 कर देता है।"
            ),
            q(
                id = "rev_two_rises",
                topic = REVERSE_PERCENTAGE,
                difficulty = H,
                en = "A value rises by 10% and then by 10% again to become 605. What was the original value?",
                hi = "एक मान 10% और फिर 10% बढ़कर 605 हो जाता है। मूल मान क्या था?",
                options = listOf(
                    "500" to "500",
                    "550" to "550",
                    "600" to "600",
                    "605" to "605"
                ),
                correctIndex = 0,
                explanationEn = "Two 10% rises multiply by 1.21, so the original is 605 / 1.21 = 500. Check: 500 x 1.1 = 550, then 550 x 1.1 = 605.",
                explanationHi = "दो 10% वृद्धि 1.21 से गुणा करती हैं, अतः मूल 605 / 1.21 = 500 है। जाँच: 500 x 1.1 = 550, फिर 550 x 1.1 = 605।"
            ),
            q(
                id = "rev_two_changes",
                topic = REVERSE_PERCENTAGE,
                difficulty = H,
                en = "A value falls 30% and then rises 25% to become 1400. What was the original value?",
                hi = "एक मान 30% घटकर और फिर 25% बढ़कर 1400 हो जाता है। मूल मान क्या था?",
                options = listOf(
                    "1600" to "1600",
                    "1400" to "1400",
                    "1750" to "1750",
                    "1550" to "1550"
                ),
                correctIndex = 0,
                explanationEn = "The combined factor is 0.70 x 1.25 = 0.875, so the original is 1400 / 0.875 = 1600. Check: 1600 x 0.70 = 1120, then 1120 x 1.25 = 1400.",
                explanationHi = "संयुक्त गुणक 0.70 x 1.25 = 0.875 है, अतः मूल 1400 / 0.875 = 1600 है। जाँच: 1600 x 0.70 = 1120, फिर 1120 x 1.25 = 1400।"
            ),
            q(
                id = "mix_salary_raise_cut",
                topic = MIXED_WORD_PROBLEMS,
                difficulty = M,
                en = "A salary of 40000 rises by 10% and then falls by 20%. What is the final salary?",
                hi = "40000 का वेतन 10% बढ़ता है और फिर 20% घटता है। अंतिम वेतन क्या है?",
                options = listOf(
                    "35200" to "35200",
                    "36000" to "36000",
                    "38000" to "38000",
                    "40000" to "40000"
                ),
                correctIndex = 0,
                explanationEn = "40000 x 1.10 = 44000, then 44000 x 0.80 = 35200. The net change is -12%, because the fall is measured on the already-raised salary.",
                explanationHi = "40000 x 1.10 = 44000, फिर 44000 x 0.80 = 35200। शुद्ध परिवर्तन -12% है, क्योंकि गिरावट पहले बढ़े हुए वेतन पर मापी जाती है।"
            ),
            q(
                id = "mix_price_up_down",
                topic = MIXED_WORD_PROBLEMS,
                difficulty = M,
                en = "A price of 500 rises by 10% and then falls by 20%. What is the final price?",
                hi = "500 का मूल्य 10% बढ़ता है और फिर 20% घटता है। अंतिम मूल्य क्या है?",
                options = listOf(
                    "440" to "440",
                    "450" to "450",
                    "480" to "480",
                    "500" to "500"
                ),
                correctIndex = 0,
                explanationEn = "500 x 1.10 = 550, then 550 x 0.80 = 440. The net change is -12%.",
                explanationHi = "500 x 1.10 = 550, फिर 550 x 0.80 = 440। शुद्ध परिवर्तन -12% है।"
            ),
            q(
                id = "mix_marks_relative",
                topic = MIXED_WORD_PROBLEMS,
                difficulty = M,
                en = "A student needs 40 marks. Passing needs 10% more marks than that. How many marks must the student score to pass?",
                hi = "एक विद्यार्थी को 40 अंक चाहिए। उत्तीर्ण होने के लिए उससे 10% अधिक अंक चाहिए। उत्तीर्ण होने के लिए कितने अंक चाहिए?",
                options = listOf(
                    "44" to "44",
                    "50" to "50",
                    "48" to "48",
                    "40" to "40"
                ),
                correctIndex = 0,
                explanationEn = "10% more than 40 is 40 x 1.10 = 44 marks.",
                explanationHi = "40 से 10% अधिक 40 x 1.10 = 44 अंक है।"
            ),
            q(
                id = "mix_a_b_c",
                topic = MIXED_WORD_PROBLEMS,
                difficulty = H,
                en = "A is 150. B is 20% more than A. C is 10% less than B. What is C?",
                hi = "A = 150 है। B, A से 20% अधिक है। C, B से 10% कम है। C क्या है?",
                options = listOf(
                    "162" to "162",
                    "168" to "168",
                    "170" to "170",
                    "180" to "180"
                ),
                correctIndex = 0,
                explanationEn = "B = 150 x 1.20 = 180, then C = 180 x 0.90 = 162.",
                explanationHi = "B = 150 x 1.20 = 180, फिर C = 180 x 0.90 = 162।"
            ),
            q(
                id = "mix_pop_growth_deaths",
                topic = MIXED_WORD_PROBLEMS,
                difficulty = H,
                en = "A town of 30000 people grows by 10%, and then 5% of the new population moves away. What is the population now?",
                hi = "30000 की आबादी 10% बढ़ती है, और फिर नई आबादी का 5% चला जाता है। अब आबादी क्या है?",
                options = listOf(
                    "31350" to "31350",
                    "33000" to "33000",
                    "31500" to "31500",
                    "30000" to "30000"
                ),
                correctIndex = 0,
                explanationEn = "30000 x 1.10 = 33000, then 33000 x 0.95 = 31350. The 5% is measured on the population after the growth.",
                explanationHi = "30000 x 1.10 = 33000, फिर 33000 x 0.95 = 31350। 5% वृद्धि के बाद की आबादी पर मापा गया है।"
            ),
            q(
                id = "mix_discount_then_tax",
                topic = MIXED_WORD_PROBLEMS,
                difficulty = M,
                en = "A 1000 rupee item gets a 20% discount, then 10% tax is added on the reduced price. What is the final amount?",
                hi = "1000 रुपये की वस्तु पर 20% छूट, फिर घटी मूल्य पर 10% कर। अंतिम राशि क्या है?",
                options = listOf(
                    "880" to "880",
                    "900" to "900",
                    "990" to "990",
                    "860" to "860"
                ),
                correctIndex = 0,
                explanationEn = "1000 x 0.80 = 800, then 800 x 1.10 = 880.",
                explanationHi = "1000 x 0.80 = 800, फिर 800 x 1.10 = 880।"
            ),
            q(
                id = "mix_tax_income",
                topic = MIXED_WORD_PROBLEMS,
                difficulty = E,
                en = "A person earns 50000 in a year and pays 30% of it as tax. What is the income left?",
                hi = "एक व्यक्ति वर्ष में 50000 कमाता है और उसका 30% कर के रूप में देता है। कितना शेष रहता है?",
                options = listOf(
                    "35000" to "35000",
                    "15000" to "15000",
                    "20000" to "20000",
                    "40000" to "40000"
                ),
                correctIndex = 0,
                explanationEn = "30% of 50000 = 15000 is tax, so 50000 - 15000 = 35000 is left.",
                explanationHi = "50000 का 30% = 15000 कर है, अतः 50000 - 15000 = 35000 शेष रहता है。"
            ),
        // ==== END GENERATED PHASE 6 QUESTIONS ===
        )
    }
}