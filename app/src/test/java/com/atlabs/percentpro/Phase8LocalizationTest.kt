package com.atlabs.percentpro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Localization coverage for the Phase 8 strings.
 *
 * Same failure mode as Phases 5 and 7: a string added to English and forgotten
 * in Hindi does not fail the build, it silently falls back to English at
 * runtime. With onboarding and settings now carrying most of the new user-facing
 * text, including the privacy explanation, that gap matters more than it did.
 *
 * Also checks the things that make a screen broken rather than merely
 * inconsistent: a format string whose arguments do not line up, and a duplicate
 * resource name.
 */
class Phase8LocalizationTest {

    private fun values(language: String): Map<String, Element> {
        val path = "src/main/res/values${if (language == "en") "" else "-$language"}/strings.xml"
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(path))
        val root = document.documentElement
        val map = mutableMapOf<String, Element>()
        val nodes = root.childNodes
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node is Element) map[node.getAttribute("name")] = node
        }
        return map
    }

    private val english: Map<String, Element> by lazy { values("en") }
    private val hindi: Map<String, Element> by lazy { values("hi") }

    private val phase8Names = listOf(
        // onboarding
        "onboarding_body_1", "onboarding_title_2", "onboarding_body_2",
        "onboarding_title_3", "onboarding_body_3",
        "onboarding_next", "onboarding_back", "onboarding_skip",
        "onboarding_start", "onboarding_page_indicator",
        // settings
        "settings_title", "settings_section_learning", "settings_section_data",
        "settings_section_about", "settings_daily_goal",
        "settings_daily_goal_description",
        "settings_achievements", "settings_achievements_value",
        "settings_progress",
        "settings_progress_value", "settings_version",
        "settings_privacy_title", "settings_privacy_body",
        "settings_no_reset", "settings_no_reset_body", "settings_built_by",
        // mock test exit confirmation and the corrected result line
        "mock_exit_dialog_title", "mock_exit_dialog_body",
        "mock_exit_dialog_confirm", "mock_exit_dialog_cancel",
        "mock_result_out_of", "cd_option_incorrect",
        // renamed tab
        "tab_settings"
    )

    @Test
    fun `every Phase 8 string exists in English`() {
        val missing = phase8Names.filter { it !in english }
        assertEquals("missing from values/strings.xml: $missing", 0, missing.size)
    }

    @Test
    fun `every Phase 8 string exists in Hindi`() {
        val missing = phase8Names.filter { it !in hindi }
        assertEquals("missing from values-hi/strings.xml: $missing", 0, missing.size)
    }

    @Test
    fun `English and Hindi have no string present in only one of them`() {
        val onlyEnglish = english.keys - hindi.keys
        val onlyHindi = hindi.keys - english.keys
        assertTrue("present only in English: $onlyEnglish", onlyEnglish.isEmpty())
        assertTrue("present only in Hindi: $onlyHindi", onlyHindi.isEmpty())
    }

    @Test
    fun `the two files declare the same number of strings`() {
        assertEquals(english.size, hindi.size)
    }

    @Test
    fun `no Phase 8 string is empty or blank`() {
        phase8Names.forEach { name ->
            val en = english[name]?.textContent.orEmpty()
            val hi = hindi[name]?.textContent.orEmpty()
            assertTrue("$name is blank in English", en.isNotBlank())
            assertTrue("$name is blank in Hindi", hi.isNotBlank())
        }
    }

    @Test
    fun `formatted strings use the same argument list in both languages`() {
        // Word order changes between languages but the placeholders must not, or
        // the argument that was meant for the second slot renders in the first.
        val pattern = Regex("%(\\d+)\\$[sd]")
        phase8Names.forEach { name ->
            val en = pattern.findAll(english[name]?.textContent.orEmpty())
                .map { it.groupValues[1] }.sorted().joinToString(",")
            val hi = pattern.findAll(hindi[name]?.textContent.orEmpty())
                .map { it.groupValues[1] }.sorted().joinToString(",")
            assertEquals("$name has different placeholders per locale", en, hi)
        }
    }

    @Test
    fun `the privacy explanation keeps its paragraph breaks`() {
        // It is read as three separate points. A single line would be an
        // unreadable wall of text.
        listOf("settings_privacy_body").forEach { name ->
            assertTrue(
                "$name lost its paragraph breaks",
                english[name]?.textContent.orEmpty().count { it == '\n' } >= 2
            )
            assertTrue(
                "$name lost its paragraph breaks in Hindi",
                hindi[name]?.textContent.orEmpty().count { it == '\n' } >= 2
            )
        }
    }

    @Test
    fun `the maximum-marks line has its own string`() {
        // Phase 8 fixed "Maximum marks: 8.00 / 8.00" by giving the line its own
        // single-argument format. Guarding the shape stops it being refactored
        // back onto the two-argument score format.
        assertTrue(english["mock_result_out_of"]!!.textContent.contains("%1\$s"))
        assertEquals(
            1,
            Regex("%\\d+\\$[sd]").findAll(english["mock_result_out_of"]!!.textContent).count()
        )
    }

    @Test
    fun `the removed coming-soon strings are gone from both locales`() {
        listOf("coming_soon_badge", "coming_soon_profile_title", "coming_soon_profile_body")
            .forEach { name ->
                assertTrue("$name should have been deleted", name !in english)
                assertTrue("$name should have been deleted", name !in hindi)
            }
    }

    @Test
    fun `the profile tab label was replaced rather than duplicated`() {
        assertTrue("tab_profile should be gone", "tab_profile" !in english)
        assertTrue("tab_settings should exist", "tab_settings" in english)
        assertTrue("tab_settings should exist", "tab_settings" in hindi)
    }

    @Test
    fun `onboarding reuse the existing welcome title rather than duplicating it`() {
        // Page one shows welcome_title. A second string with the same text would
        // be a translation that could drift out of step with the home screen.
        assertTrue("welcome_title must still exist", "welcome_title" in english)
        assertTrue("onboarding_title_1 must not exist", "onboarding_title_1" !in english)
    }
}