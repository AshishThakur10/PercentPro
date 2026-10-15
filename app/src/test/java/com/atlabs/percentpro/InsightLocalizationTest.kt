package com.atlabs.percentpro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Localization coverage for the Phase 7 interface.
 *
 * Reads the resource XML directly so it runs as a plain JVM test. It catches the
 * failure that actually happens: a string added to English and forgotten in
 * Hindi, which would silently fall back at runtime, and a format specifier that
 * disagrees between the two.
 */
class InsightLocalizationTest {

    private fun strings(language: String): Map<String, Element> {
        val path = if (language == "en") {
            "src/main/res/values/strings.xml"
        } else {
            "src/main/res/values-hi/strings.xml"
        }
        val file = java.io.File(path)
        assertTrue("missing $path", file.exists())
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val map = mutableMapOf<String, Element>()
        val children = document.documentElement.childNodes
        for (index in 0 until children.length) {
            val node = children.item(index)
            if (node is Element && node.tagName == "string") {
                map[node.getAttribute("name")] = node
            }
        }
        return map
    }

    private val english: Map<String, Element> by lazy { strings("en") }
    private val hindi: Map<String, Element> by lazy { strings("hi") }

    private val phase7Names = listOf(
        // Dashboard
        "insights_title", "insights_subtitle", "insights_overall_attempted",
        "insights_overall_accuracy", "insights_strongest_topic", "insights_weakest_topic", "insights_suggested_next",
        "insights_by_topic", "insights_recent", "insights_no_data_title",
        "insights_no_data_body", "insights_legacy_note", "insights_back",
        // Topic statuses
        "insights_status_not_enough_data", "insights_status_needs_practice",
        "insights_status_improving", "insights_status_strong",
        "insights_status_not_enough_data_body", "insights_easy_only_caution",
        "insights_topic_counts", "insights_accuracy_value",
        "insights_recent_accuracy_value", "insights_lessons_completed_value",
        "insights_topics_covered",
        // Trends
        "insights_trend_up", "insights_trend_down", "insights_trend_steady",
        "insights_trend_locked", "insights_recent_window", "insights_vs_previous",
        // Recommendations
        "rec_title_resume_lesson", "rec_title_continue_course", "rec_title_practice_weak",
        "rec_title_step_up", "rec_title_mixed_review", "rec_title_start_lesson_one",
        "rec_title_keep_practising", "rec_continue_learning", "rec_view_insights", "rec_start_practice", "rec_open_insights",
        "rec_reason_lesson_in_progress", "rec_reason_next_lesson", "rec_reason_low_accuracy",
        "rec_reason_strong_on_easy", "rec_reason_balanced", "rec_reason_no_history",
        "rec_reason_course_complete",
        // Targeted practice
        "targeted_title_mixed", "targeted_mixed_subtitle", "targeted_start",
        // Accessibility
        "cd_insights_accuracy", "cd_topic_performance", "cd_recommendation"
    )

    @Test
    fun `every Phase 7 string exists in English`() {
        val missing = phase7Names.filter { it !in english }
        assertEquals("missing English strings: $missing", emptyList<String>(), missing)
    }

    @Test
    fun `every Phase 7 string exists in Hindi`() {
        val missing = phase7Names.filter { it !in hindi }
        assertEquals("missing Hindi strings: $missing", emptyList<String>(), missing)
    }

    @Test
    fun `no Phase 7 string is empty`() {
        phase7Names.forEach { name ->
            assertTrue("English $name is empty", english.getValue(name).textContent.orEmpty().isNotBlank())
            assertTrue("Hindi $name is empty", hindi.getValue(name).textContent.orEmpty().isNotBlank())
        }
    }

    @Test
    fun `the same format arguments are available in both languages`() {
        // Android substitutes by index, so a sentence may reorder them freely.
        // What must not differ is which arguments the string consumes at all.
        val mismatched = phase7Names
            .filter {
                placeholders(english.getValue(it)).sorted() !=
                    placeholders(hindi.getValue(it)).sorted()
            }
        assertEquals("placeholder mismatch: $mismatched", emptyList<String>(), mismatched)
    }

    @Test
    fun `prose is translated rather than copied`() {
        val identical = phase7Names
            .filter { containsWords(english.getValue(it).textContent.orEmpty()) }
            .filter {
                english.getValue(it).textContent?.trim() ==
                    hindi.getValue(it).textContent?.trim()
            }
        assertEquals("untranslated copy: $identical", emptyList<String>(), identical)
    }

    @Test
    fun `Hindi uses Devanagari for user-facing copy`() {
        listOf(
            "insights_title", "insights_status_needs_practice",
            "rec_title_practice_weak"
        ).forEach { name ->
            val text = hindi.getValue(name).textContent.orEmpty()
            assertTrue("$name has no Devanagari: $text", text.any { it.code in 0x0900..0x097F })
        }
    }

    @Test
    fun `the trend copy names both directions and the steady case`() {
        listOf("insights_trend_up", "insights_trend_down", "insights_trend_steady")
            .forEach { assertTrue("$it missing", it in english && it in hindi) }
    }

    @Test
    fun `the insufficient-data message is present in both languages`() {
        listOf(
            "insights_status_not_enough_data",
            "insights_status_not_enough_data_body",
            "insights_trend_locked",
            "insights_no_data_title",
            "insights_no_data_body"
        ).forEach { name ->
            assertTrue("$name missing from English", name in english)
            assertTrue("$name missing from Hindi", name in hindi)
        }
    }

    @Test
    fun `accessibility descriptions exist for every visual indicator`() {
        listOf(
            "cd_insights_accuracy", "cd_topic_performance", "cd_recommendation"
        ).forEach { name ->
            assertTrue("$name missing from English", name in english)
            assertTrue("$name missing from Hindi", name in hindi)
            assertTrue(
                "$name has no arguments, so it cannot describe a value",
                placeholders(english.getValue(name)).isNotEmpty()
            )
        }
    }

    @Test
    fun `the legacy-data note is translated rather than omitted`() {
        val en = english.getValue("insights_legacy_note").textContent.orEmpty()
        val hi = hindi.getValue("insights_legacy_note").textContent.orEmpty()

        assertTrue(en.contains("before this feature"))
        assertTrue(hi.any { it.code in 0x0900..0x097F })
    }

    /**
     * Format specifiers in a string's text.
     *
     * Read from the text rather than the `formatted` attribute: these strings are
     * marked `formatted="false"` so aapt does not warn about literal percent
     * signs elsewhere in the copy, but [android.content.res.Resources.getString]
     * still substitutes the arguments at runtime.
     */
    private fun placeholders(element: Element): List<String> =
        Regex("%\\d+\\$[sd]").findAll(element.textContent.orEmpty())
            .map { it.value }
            .toList()

    private fun containsWords(text: String): Boolean =
        Regex("[A-Za-z]{3,}").containsMatchIn(text.replace(Regex("%\\d+\\$[sd]"), ""))
}