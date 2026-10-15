package com.atlabs.percentpro.model

enum class LessonId(val storageKey: String) {
    WHAT_IS_A_PERCENTAGE("what_is_a_percentage"),
    PERCENTAGES_VISUALLY("percentages_visually"),
    FRACTIONS_TO_PERCENTAGES("fractions_to_percentages"),
    DECIMALS_TO_PERCENTAGES("decimals_to_percentages"),
    PERCENTAGE_OF_A_NUMBER("percentage_of_a_number"),
    ORIGINAL_WHOLE("original_whole"),
    PERCENTAGE_INCREASE_DECREASE("percentage_increase_decrease"),
    SUCCESSIVE_PERCENTAGE_CHANGES("successive_percentage_changes"),
    PERCENTAGE_COMPARISON("percentage_comparison"),

    // Added in Phase 6. These strings are new, so nothing already saved can
    // refer to them; every identifier above keeps its original value.
    PERCENTAGE_POINTS_VS_CHANGE("percentage_points_vs_change"),
    COMPARING_REPRESENTATIONS("comparing_representations"),
    EXAM_SHORTCUTS("exam_shortcuts"),
    POPULATION_CHANGE("population_change"),
    SALARY_EXPENDITURE_SAVINGS("salary_expenditure_savings"),
    MARKS_AND_RESULTS("marks_and_results"),
    ELECTION_VOTING("election_voting"),
    PRICE_DISCOUNT_TAX("price_discount_tax"),
    PROFIT_LOSS("profit_loss"),
    REVERSE_PERCENTAGE("reverse_percentage"),
    MIXED_WORD_PROBLEMS("mixed_word_problems"),
    COMPETITIVE_EXAM_CHALLENGE("competitive_exam_challenge");

    companion object {
        /**
         * Identifiers that may appear in old saved data but are no longer part of
         * the course. They are dropped instead of mapped.
         */
        private val RETIRED = emptySet<String>()

        fun fromStorageKey(key: String?): LessonId? {
            if (key == null || key in RETIRED) return null
            return entries.firstOrNull { it.storageKey == key }
        }
    }
}