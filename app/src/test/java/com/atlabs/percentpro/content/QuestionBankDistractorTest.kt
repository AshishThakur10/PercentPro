package com.atlabs.percentpro.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Whether each question's answer options are actually usable.
 *
 * This exists because of a defect that shipped. Seven questions carried a
 * literal `+1` as one of their four options, including "What is 10% of 350?"
 * (options 35 / 70 / 315 / **+1**) and a run of net-change questions where `+1`
 * is not a plausible answer to anything. The cause was in the question
 * generator: `q_num` topped up short option lists from a fixed list of
 * perturbations beginning `+1`, `-1`, `+2`, which also hid the real problem,
 * because those seven callers were generating duplicate or colliding
 * distractors.
 *
 * Structural tests already prove a question is well formed. These prove a
 * learner can make a sensible choice between the options offered.
 */
class QuestionBankDistractorTest {

    private val bank = QuestionBank()

    /**
     * The padding list `q_num` used to fall back on. None of these is ever a
     * meaningful distractor on its own: they are arbitrary offsets from nothing
     * in particular.
     */
    private val paddingTokens = setOf(
        "+1", "-1", "+2", "-2", "+3", "-3", "+5", "-5", "+10", "-10"
    )

    @Test
    fun `no option is a bare padding token`() {
        val offenders = bank.all().filter { question ->
            question.options.any { it.english.trim() in paddingTokens }
        }
        assertEquals(
            "these questions offer an arbitrary +/- offset as a choice: " +
                offenders.map { it.id.value },
            emptyList<String>(),
            offenders.map { it.id.value }
        )
    }

    @Test
    fun `no option repeats within a question`() {
        // Two identical options make a question unanswerable, because two
        // different choices are marked correct or two are marked wrong.
        bank.all().forEach { question ->
            val texts = question.options.map { it.english }
            assertEquals(
                "${question.id.value} repeats an option: $texts",
                texts.size,
                texts.toSet().size
            )
        }
    }

    @Test
    fun `the correct answer is not offered twice`() {
        bank.all().forEach { question ->
            val answer = question.options[question.correctIndex].english
            val occurrences = question.options.count { it.english == answer }
            assertEquals(
                "${question.id.value} shows its answer $occurrences times",
                1,
                occurrences
            )
        }
    }

    @Test
    fun `every question offers four options`() {
        bank.all().forEach { question ->
            assertEquals(
                "${question.id.value} has ${question.options.size} options",
                4,
                question.options.size
            )
        }
    }

    @Test
    fun `no option is blank`() {
        bank.all().forEach { question ->
            question.options.forEachIndexed { index, option ->
                assertTrue(
                    "${question.id.value} option $index is blank in English",
                    option.english.isNotBlank()
                )
                assertTrue(
                    "${question.id.value} option $index is blank in Hindi",
                    option.hindi.isNotBlank()
                )
            }
        }
    }

    @Test
    fun `no option is wildly longer than the answer`() {
        // A distractor an order of magnitude longer than the answer is almost
        // always a formatting accident rather than a thought-out choice.
        bank.all().forEach { question ->
            val answer = question.options[question.correctIndex].english.length
            question.options.forEachIndexed { index, option ->
                assertTrue(
                    "${question.id.value} option $index is ${option.english.length} " +
                        "characters against a ${answer}-character answer",
                    option.english.length <= maxOf(answer * 3, answer + 40)
                )
            }
        }
    }

    @Test
    fun `every question has at least three plausible wrong answers`() {
        // The same condition the generator now enforces, restated as a check on
        // the shipped bank so a hand edit cannot bypass it.
        bank.all().forEach { question ->
            val distinct = question.options.map { it.english }.toSet()
            assertTrue(
                "${question.id.value} has only ${distinct.size} distinct options",
                distinct.size == 4
            )
        }
    }

    @Test
    fun `options stay aligned between the two languages`() {
        // Each option is a single LocalizedText, so alignment is structural. What
        // this really guards is the answer index staying inside the list after a
        // regeneration, which would otherwise crash the practice screen.
        bank.all().forEach { question ->
            assertTrue(
                "${question.id.value} answer index ${question.correctIndex} is out of range",
                question.correctIndex in question.options.indices
            )
            assertTrue(
                "${question.id.value} has a negative answer index",
                question.correctIndex >= 0
            )
        }
    }

    @Test
    fun `worded options are translated rather than copied`() {
        // A Hindi option identical to an English one that contains actual words
        // usually means the Hindi was never filled in and the learner silently
        // sees English.

        // Numerals, fractions and ratios are deliberately identical in both
        // languages -- "50", "3/4" and "6 : 5" are the same token either way --
        // so only worded options are checked.
        fun isLanguageNeutral(text: String) =
            text.none { it.isLetter() && it.code !in 0x0900..0x097F }

        bank.all().forEach { question ->
            question.options.forEachIndexed { index, option ->
                // Language-neutral tokens are expected to be identical; only a
                // worded English option must have real Hindi.
                if (isLanguageNeutral(option.english)) return@forEachIndexed
                assertTrue(
                    "${question.id.value} option $index has no Hindi translation",
                    option.hindi != option.english
                )
            }
        }
    }
}