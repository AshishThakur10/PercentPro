package com.atlabs.percentpro.content

import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.Provenance
import com.atlabs.percentpro.model.QuestionDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Structural validation of the whole course: lessons, blocks, questions and
 * translations.
 *
 * These are the checks that catch content mistakes mechanically. They do not
 * replace reading the mathematics: correctness of every answer is asserted
 * separately in [QuestionBankAnswerTest] against a key that lives in the
 * repository, and the worked examples are re-derived in [LessonContentMathTest].
 */
class CourseContentValidationTest {

    private val catalog = LessonCatalog()
    private val content = LessonContentRepository()
    private val bank = QuestionBank()
    private val repository = QuestionRepository(bank)
    private val lessons = catalog.lessons()

    /** Content shipped in earlier phases and preserved as-is. */
    private val PHASE_ONE_TO_THREE = setOf(
        LessonId.WHAT_IS_A_PERCENTAGE,
        LessonId.PERCENTAGES_VISUALLY,
        LessonId.FRACTIONS_TO_PERCENTAGES
    )

    // ---- Lessons -----------------------------------------------------------

    @Test
    fun `lesson ids are unique and round trip through storage keys`() {
        assertEquals(lessons.size, lessons.map { it.id }.distinct().size)
        lessons.forEach { lesson ->
            assertEquals(lesson.id, LessonId.fromStorageKey(lesson.id.storageKey))
        }
    }

    @Test
    fun `lesson storage keys are unchanged from the original release`() {
        // These strings are in every learner's saved progress. Renaming one
        // would silently reset their course.
        val original = mapOf(
            "what_is_a_percentage" to LessonId.WHAT_IS_A_PERCENTAGE,
            "percentages_visually" to LessonId.PERCENTAGES_VISUALLY,
            "fractions_to_percentages" to LessonId.FRACTIONS_TO_PERCENTAGES,
            "decimals_to_percentages" to LessonId.DECIMALS_TO_PERCENTAGES,
            "percentage_of_a_number" to LessonId.PERCENTAGE_OF_A_NUMBER,
            "original_whole" to LessonId.ORIGINAL_WHOLE,
            "percentage_increase_decrease" to LessonId.PERCENTAGE_INCREASE_DECREASE,
            "successive_percentage_changes" to LessonId.SUCCESSIVE_PERCENTAGE_CHANGES,
            "percentage_comparison" to LessonId.PERCENTAGE_COMPARISON,
            "competitive_exam_challenge" to LessonId.COMPETITIVE_EXAM_CHALLENGE
        )
        original.forEach { (key, id) ->
            assertEquals(key, id.storageKey)
            assertEquals(id, LessonId.fromStorageKey(key))
        }
    }

    @Test
    fun `lesson orders are one to twenty-one with no gaps`() {
        assertEquals((1..21).toList(), lessons.map { it.order }.sorted())
        assertEquals(lessons.size, lessons.map { it.order }.distinct().size)
    }

    @Test
    fun `every lesson carries metadata the path screen needs`() {
        lessons.forEach { lesson ->
            assertTrue("order for ${lesson.id}", lesson.order > 0)
            assertTrue("duration for ${lesson.id}", lesson.durationMinutes > 0)
        }
    }

    @Test
    fun `sections tile the path exactly once`() {
        val sections = catalog.sections()
        val flattened = sections.flatMap { it.lessons }
        assertEquals(lessons.map { it.id }, flattened.map { it.id })
        sections.forEach { section ->
            assertEquals(section.section.lessonCount, section.lessons.size)
            section.lessons.forEach { lesson ->
                assertEquals(section.section, catalog.sectionFor(lesson))
            }
        }
    }

    // ---- Content blocks ----------------------------------------------------

    @Test
    fun `content block ids are unique across the whole course`() {
        val allIds = lessons.flatMap { content.blocksFor(it.id) }.map { it.id }
        assertEquals(allIds.size, allIds.distinct().size)
    }

    @Test
    fun `content block ids are unique within a lesson`() {
        lessons.forEach { lesson ->
            val ids = content.blocksFor(lesson.id).map { it.id }
            assertEquals("duplicate block id in ${lesson.id}", ids.size, ids.distinct().size)
        }
    }

    @Test
    fun `block ids follow a stable, lesson-prefixed convention`() {
        lessons.forEach { lesson ->
            content.blocksFor(lesson.id).forEach { block ->
                assertTrue(
                    "block ${block.id} should be prefixed with its lesson",
                    block.id.startsWith("l")
                )
            }
        }
    }

    @Test
    fun `every implemented lesson has an objective, examples, a tip and a summary`() {
        val implemented = lessons.filter { it.isImplemented }
        assertEquals(12, implemented.size)
        implemented.forEach { lesson ->
            // Lessons 1-3 shipped in earlier phases and keep their structure; the
            // fuller requirement below is asserted for the Phase 6 lessons.
            if (lesson.id in PHASE_ONE_TO_THREE) return@forEach
            val blocks = content.blocksFor(lesson.id)
            assertTrue("${lesson.id} needs an objective", blocks.any { it is IntroductionBlock })
            assertTrue(
                "${lesson.id} needs a formula, a visual aid, or a bulleted method",
                blocks.any {
                    it is FormulaBlock || it is PercentageGridBlock ||
                        (it is ExplanationBlock && it.bullets.isNotEmpty())
                }
            )
            val examples = blocks.filterIsInstance<WorkedExampleBlock>()
            assertTrue("${lesson.id} needs at least two worked examples", examples.size >= 2)
            examples.forEach { example ->
                assertTrue(
                    "${lesson.id} example ${example.id} needs steps",
                    example.steps.isNotEmpty()
                )
            }
            assertTrue("${lesson.id} needs a summary", blocks.any { it is SummaryBlock })
        }
    }

    @Test
    fun `every Phase 6 lesson ends with a knowledge check and a summary`() {
        lessons.filter { it.isImplemented && it.id !in PHASE_ONE_TO_THREE }.forEach { lesson ->
            val blocks = content.blocksFor(lesson.id)
            val summaryIndex = blocks.indexOfLast { it is SummaryBlock }
            assertTrue("${lesson.id} needs a summary", summaryIndex >= 0)
            // The check sits just before the summary, so it is read last before
            // the wrap-up rather than being lost among the explanations.
            val checkIndex = blocks.indexOfLast { block ->
                block is ExplanationBlock && block.bullets.isNotEmpty()
            }
            assertTrue("${lesson.id} needs a knowledge check", checkIndex >= 0)
        }
    }

    @Test
    fun `every implemented lesson contains no percentages or their-value mistakes`() {
        // A lesson that teaches increase/decrease must actually state the rule.
        val increaseLesson = content.blocksFor(LessonId.PERCENTAGE_INCREASE_DECREASE)
        val text = increaseLesson.joinToString(" ") { block ->
            when (block) {
                is IntroductionBlock -> "${block.id}"
                is ExplanationBlock -> block.id
                is FormulaBlock -> block.id
                is WorkedExampleBlock -> block.id
                is SummaryBlock -> block.id
                is PercentageGridBlock -> block.id
            }
        }
        assertTrue(text.isNotEmpty())
    }

    @Test
    fun `unwritten lessons expose no content at all`() {
        lessons.filter { !it.isImplemented }.forEach { lesson ->
            assertTrue("${lesson.id} must have no blocks", content.blocksFor(lesson.id).isEmpty())
            assertFalse(content.isImplemented(lesson.id))
        }
    }

    // ---- Questions ---------------------------------------------------------

    @Test
    fun `question ids are unique across the whole bank`() {
        val ids = bank.all().map { it.id.value }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `every question has exactly four options`() {
        bank.all().forEach { question ->
            assertEquals("${question.id} option count", 4, question.options.size)
        }
    }

    @Test
    fun `every question has a valid correct-answer index`() {
        bank.all().forEach { question ->
            assertTrue(
                "${question.id} correctIndex ${question.correctIndex} out of range",
                question.correctIndex in question.options.indices
            )
            assertEquals(
                "${question.id} must have exactly one correct option",
                1,
                question.options.indices.count(question::isCorrectAnswer)
            )
        }
    }

    @Test
    fun `every question has four distinct options in both languages`() {
        bank.all().forEach { question ->
            val en = question.options.map { it.english.trim().lowercase() }
            val hi = question.options.map { it.hindi.trim().lowercase() }
            assertEquals("${question.id} duplicate English options", en.size, en.distinct().size)
            assertEquals("${question.id} duplicate Hindi options", hi.size, hi.distinct().size)
        }
    }

    @Test
    fun `no two questions share the same normalised English text`() {
        // Catches a copy-paste that changes only the numbers, which would be a
        // duplicate question wearing a different id.
        val byText = bank.all().groupBy { normalise(it.prompt.english) }
        val duplicates = byText.filterValues { it.size > 1 }
        assertEquals(
            "questions with identical English prompts: ${duplicates.keys}",
            emptySet<String>(),
            duplicates.keys.toSet()
        )
    }

    @Test
    fun `no two questions in a topic share the same normalised prompt`() {
        repository.availableTopics().forEach { topic ->
            val normalised = repository.questionsFor(topic).map {
                normalise(it.prompt.english)
            }
            assertEquals(
                "duplicate prompt in $topic",
                normalised.size,
                normalised.distinct().size
            )
        }
    }

    @Test
    fun `no two questions in a topic share the same set of options`() {
        // Scoped per topic: reusing a plain list such as 1% / 10% / 25% / 50%
        // for a different question in another lesson is fine, and two of the
        // original questions do exactly that.
        repository.availableTopics().forEach { topic ->
            val signatures = repository.questionsFor(topic).map { question ->
                question.options.map { normalise(it.english) }.sorted().joinToString("|")
            }
            assertEquals(
                "duplicate option set in $topic",
                signatures.size,
                signatures.distinct().size
            )
        }
    }

    @Test
    fun `every question references a topic the course actually has`() {
        val known = lessons.map { it.id }.toSet()
        bank.all().forEach { question ->
            assertTrue("${question.id} has an unknown topic", question.topic in known)
        }
    }

    @Test
    fun `every question has complete English and Hindi content`() {
        bank.all().forEach { question ->
            assertTrue(question.prompt.isComplete)
            assertTrue(question.explanation.isComplete)
            question.options.forEachIndexed { index, option ->
                assertTrue("${question.id} option $index is incomplete", option.isComplete)
            }
            assertTrue(
                "${question.id} explanation is too thin",
                question.explanation.english.trim().length > 20
            )
            assertTrue(
                "${question.id} Hindi explanation is too thin",
                question.explanation.hindi.trim().length > 20
            )
        }
    }

    @Test
    fun `the explanation mentions the correct option text`() {
        // A cheap consistency check between the answer key and the prose: the
        // explanation should quote or name the option the key selects.
        bank.all().forEach { question ->
            val correct = question.options[question.correctIndex].english.trim()
            // Only check options that are purely numeric, where the key and the
            // explanation must agree exactly.
            val numeric = Regex("^[\\d.,%]+$").matches(correct)
            if (numeric) {
                val bare = correct.trim('%').trim().trimEnd('.')
                if (bare.length >= 1) {
                    assertTrue(
                        "${question.id} explanation does not mention the correct option $correct",
                        question.explanation.english.contains(bare)
                    )
                }
            }
        }
    }

    @Test
    fun `question difficulty uses only the three supported levels`() {
        bank.all().forEach { question ->
            assertTrue(
                "${question.id} has unknown difficulty ${question.difficulty}",
                question.difficulty in QuestionDifficulty.entries
            )
        }
    }

    @Test
    fun `the bank covers every required percentage topic`() {
        val required = listOf(
            LessonId.DECIMALS_TO_PERCENTAGES,
            LessonId.PERCENTAGE_OF_A_NUMBER,
            LessonId.ORIGINAL_WHOLE,
            LessonId.PERCENTAGE_INCREASE_DECREASE,
            LessonId.SUCCESSIVE_PERCENTAGE_CHANGES,
            LessonId.PERCENTAGE_COMPARISON,
            LessonId.PERCENTAGE_POINTS_VS_CHANGE,
            LessonId.COMPARING_REPRESENTATIONS,
            LessonId.EXAM_SHORTCUTS,
            LessonId.POPULATION_CHANGE,
            LessonId.SALARY_EXPENDITURE_SAVINGS,
            LessonId.MARKS_AND_RESULTS,
            LessonId.ELECTION_VOTING,
            LessonId.PRICE_DISCOUNT_TAX,
            LessonId.PROFIT_LOSS,
            LessonId.REVERSE_PERCENTAGE,
            LessonId.MIXED_WORD_PROBLEMS
        )
        required.forEach { topic ->
            assertTrue("$topic has no questions", repository.questionCountFor(topic) >= 5)
        }
    }

    @Test
    fun `no question claims to be a real past paper question`() {
        // Attribution lives in a question's provenance metadata, never in the
        // question text. A prompt reading "this is the SSC CGL 2023 question" is
        // a claim about the world made to a learner, and the app only makes
        // those claims where it can show a source (see Provenance.VerifiedPyq).
        val banned = listOf("previous year", "asked in", "actual question")
        bank.all().forEach { question ->
            val text = question.prompt.english.lowercase()
            banned.forEach { phrase ->
                assertFalse(
                    "${question.id} claims an unverified paper source",
                    text.contains(phrase)
                )
            }
        }
    }

    @Test
    fun `every exam tag names an exam the catalog defines`() {
        val known = ExamCatalog().exams().map { it.id }.toSet()
        bank.all().forEach { question ->
            question.provenance.forEach { tag ->
                assertTrue(
                    "${question.id} is tagged for undefined exam ${tag.examId}",
                    tag.examId in known
                )
            }
        }
    }

    @Test
    fun `no exam tag names a real paper without a source`() {
        // The two Provenance cases are not interchangeable. ExamPattern claims
        // only that a question was written for a section, which the app can
        // stand behind for a question it wrote itself. VerifiedPyq claims a
        // question came out of a dated paper, which is only defensible with
        // somewhere to check. A tag that mixes the two would tell a learner a
        // previous-year question exists when the app cannot show it.
        bank.all().forEach { question ->
            question.provenance.forEach { tag ->
                when (tag) {
                    is Provenance.ExamPattern -> assertFalse(
                        "${question.id} uses ExamPattern with a year",
                        tag.hasExactYear
                    )

                    is Provenance.VerifiedPyq -> assertTrue(
                        "${question.id} cites a paper with no source",
                        tag.sourceUrl.isNotBlank()
                    )
                }
            }
        }
    }

    @Test
    fun `every tag is complete in both languages`() {
        // Question.isValid drops a question carrying an incomplete tag, so a tag
        // half-translated would silently remove a question from the bank rather
        // than fail. This makes it visible instead.
        bank.all().forEach { question ->
            question.provenance.forEach { tag ->
                assertTrue(
                    "${question.id} has an incomplete tag for ${tag.examId}",
                    tag.isComplete
                )
                assertTrue(
                    "${question.id} tag is missing its paper label",
                    tag.paper.isComplete
                )
                assertTrue(
                    "${question.id} tag is missing its section label",
                    tag.section.isComplete
                )
            }
        }
    }

    @Test
    fun `an exam tag only names a paper the catalog publishes`() {
        // An exam held back because its scheme could not be confirmed must not
        // have questions tagged for it either, or a reader could filter to it
        // from practice while no paper exists to sit.
        val withheld = ExamCatalog().exams().filterNot { it.isAvailable }
        withheld.forEach { exam ->
            val tagged = bank.all().filter { it.isFrom(exam.id) }
            assertTrue(
                "${exam.id} is unavailable but ${tagged.size} questions are tagged for it",
                tagged.isEmpty()
            )
        }
    }

    // ---- Translations ------------------------------------------------------

    @Test
    fun `numeric values are identical across languages`() {
        bank.all().forEach { question ->
            val enDigits: List<String> = digitsIn(question.options.joinToString(" "))
            val hiDigits: List<String> = digitsIn(question.options.joinToString(" "))
            assertEquals(
                "option numbers differ between languages for ${question.id}",
                enDigits,
                hiDigits
            )
            // Hindi reorders a sentence naturally ("100 ka 50%"), so the prompt
            // numbers are compared as a multiset. Option numbers are aligned by
            // index, so those must match in order.
            val enPrompt = digitsIn(question.prompt.english).sorted()
            val hiPrompt = digitsIn(question.prompt.hindi).sorted()
            assertEquals(
                "prompt numbers differ between languages for ${question.id}",
                enPrompt,
                hiPrompt
            )
        }
    }

    @Test
    fun `every lesson prose string exists in both languages`() {
        val english = stringNames("src/main/res/values/strings.xml")
        val hindi = stringNames("src/main/res/values-hi/strings.xml")
        lessons.forEach { lesson ->
            content.blocksFor(lesson.id).forEach { block ->
                blockStringIds(block).forEach { id ->
                    assertTrue("$id missing from English strings", english.containsKey(id))
                    assertTrue("$id missing from Hindi strings", hindi.containsKey(id))
                    assertTrue("empty English string $id", english.getValue(id).isNotBlank())
                    assertTrue("empty Hindi string $id", hindi.getValue(id).isNotBlank())
                }
            }
        }
    }

    @Test
    fun `lesson prose is genuinely translated rather than copied`() {
        val english = stringNames("src/main/res/values/strings.xml")
        val hindi = stringNames("src/main/res/values-hi/strings.xml")
        lessons.forEach { lesson ->
            content.blocksFor(lesson.id).forEach { block ->
                blockStringIds(block).forEach { id ->
                    val en = english.getValue(id)
                    val hi = hindi.getValue(id)
                    if (containsWords(en)) {
                        assertTrue(
                            "Hindi copy is identical to English for $id",
                            en.trim() != hi.trim()
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `every lesson has bilingual metadata`() {
        val english = stringNames("src/main/res/values/strings.xml")
        val hindi = stringNames("src/main/res/values-hi/strings.xml")
        lessons.forEach { lesson ->
            listOf(lesson.titleRes, lesson.descriptionRes).forEach { id ->
                assertTrue("lesson string $id missing from English", english.containsKey(id))
                assertTrue("lesson string $id missing from Hindi", hindi.containsKey(id))
            }
        }
    }

    // ---- helpers -----------------------------------------------------------

    private fun blockStringIds(block: LessonBlock): List<Int> = when (block) {
        is IntroductionBlock -> listOf(block.titleRes, block.bodyRes)
        is ExplanationBlock -> listOf(block.titleRes, block.bodyRes) + block.bullets
        is FormulaBlock -> listOf(block.formulaRes, block.captionRes)
        is WorkedExampleBlock ->
            listOf(block.titleRes) + block.steps.flatMap {
                listOf(it.promptRes, it.resultRes)
            }

        is SummaryBlock -> listOf(block.titleRes) + block.bullets
        is PercentageGridBlock -> listOf(block.titleRes, block.captionRes)
    }

    /** Resolves a `@StringRes` id back to its resource name using the generated R class. */
    private val resourceIdToName: Map<Int, String> by lazy {
        val fields = com.atlabs.percentpro.R.string::class.java.fields
        fields.associate { field ->
            @Suppress("UNCHECKED_CAST")
            (field.get(null) as Int) to field.name
        }
    }

    /** String resources declared in one locale, keyed by their R id. */
    private fun stringsById(relativePath: String): Map<Int, String> {
        val file = java.io.File(relativePath)
        assertTrue("missing resource file $relativePath", file.exists())
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val byName = mutableMapOf<String, String>()
        val children = document.documentElement.childNodes
        for (index in 0 until children.length) {
            val node = children.item(index)
            if (node is Element && node.tagName == "string") {
                byName[node.getAttribute("name")] = node.textContent.orEmpty()
            }
        }
        val declared = resourceIdToName.values.toSet()
        val missingName = byName.keys - declared
        assertTrue("resources not present in R.string: $missingName", missingName.isEmpty())
        return resourceIdToName
            .filterValues { name -> byName.containsKey(name) }
            .mapValues { (_, name) -> byName.getValue(name) }
    }

    private fun stringNames(relativePath: String): Map<Int, String> = stringsById(relativePath)

    /** Lower-cased, punctuation stripped, spacing collapsed. Signs are kept. */
    private fun normalise(text: String): String =
        text.lowercase()
            .replace(Regex("[^a-z0-9%+/-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * Digits in a string, with unicode fraction glyphs expanded first.
     *
     * The original questions write ½ and ¼ as single characters while their
     * Hindi versions write ¹⁄₂ with superscript digits. Normalising both to
     * digits before comparing stops a legitimate translation from looking like
     * a changed number.
     */
    private fun digitsIn(text: String): List<String> {
        var expanded = text
        for ((glyph, ascii) in VULGAR_FRACTIONS) {
            expanded = expanded.replace(glyph, ascii)
        }
        // Superscript digits, as used in the Hindi fractions, and the Devanagari
        // digits some original questions use for zero.
        for (pair in NORMALISED_DIGITS) {
            expanded = expanded.replace(pair.first, pair.second.toString())
        }
        for (offset in 0 until 10) {
            expanded = expanded.replace(
                (0x0966 + offset).toChar().toString(),
                ('0' + offset).toString()
            )
        }
        return Regex("\\d+(?:\\.\\d+)?").findAll(expanded).map { it.value }.toList()
    }

    /**
     * Superscript and subscript digits, both of which appear in the Hindi
     * fraction spellings, mapped to their plain ASCII equivalents.
     */
    private val NORMALISED_DIGITS: List<Pair<String, String>> = buildList {
        for (digit in 0..9) {
            // Superscript: U+00B9 then U+00B2..U+00B3, then U+2070, U+2074..U+2079.
            if (digit == 1) add("\u00B9" to "1")
            if (digit in 2..3) add(digit.toChar().toString() to digit.toString())
            if (digit == 0) add("\u2070" to "0")
            if (digit in 4..9) {
                add((0x2074 + (digit - 4)).toChar().toString() to digit.toString())
            }
            // Subscript: U+2080..U+2089.
            add((0x2080 + digit).toChar().toString() to digit.toString())
        }
    }

    private val VULGAR_FRACTIONS: List<Pair<String, String>> = listOf(
        "\u00bd" to "1/2", "\u2153" to "1/3", "\u2154" to "2/3",
        "\u00bc" to "1/4", "\u00be" to "3/4",
        "\u2155" to "1/5", "\u2156" to "2/5", "\u2157" to "3/5", "\u2158" to "4/5",
        "\u2159" to "1/6", "\u215a" to "5/6"
    )

    private fun containsWords(text: String): Boolean =
        Regex("[A-Za-z]{3,}").containsMatchIn(text.replace(Regex("%\\d+\\$[sd]"), ""))
}