package com.atlabs.percentpro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Localization coverage for the Phase 5 strings.
 *
 * Parses the resource XML directly rather than loading it through Android, so
 * these run as plain JVM unit tests. The point is to catch the failure mode
 * that actually happens: a string added to English and forgotten in Hindi, which
 * would silently fall back to English at runtime.
 */
class GamificationLocalizationTest {

    private fun values(language: String): Map<String, Element> {
        val path = "src/main/res/values${if (language == "en") "" else "-$language"}/strings.xml"
        val document = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(java.io.File(path))
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

    private val phase5Names = listOf(
        // XP
        "gam_total_xp", "gam_total_xp_value", "gam_xp_earned_generic", // Streaks
        "gam_streak_current", "gam_streak_zero",
        "gam_streak_none", "gam_streak_at_risk", "gam_streak_explainer",
        "gam_streak_longest_value", "gam_streak_days",
        // Daily goal
        "gam_daily_goal", "gam_daily_goal_progress", "gam_daily_goal_reached",
        "gam_daily_goal_progress_a11y", "gam_goal_picker_title", "gam_goal_option",
        // Achievements screen
        "achievements_title", "achievements_subtitle", "achievements_empty",
        "achievements_locked", "achievements_unlocked_on", "achievements_progress_a11y", "gam_section_title", "gam_section_subtitle", "gam_achievements_unlocked_header",
        "gam_achievements_short", "gam_streak_achievements_label",
        // Shortcuts
        "gam_shortcuts_title", "gam_shortcut_continue", "gam_shortcut_practice",
        "gam_shortcut_mock", "gam_shortcut_achievements", "gam_shortcut_go"
    )

    private val achievementStringNames = listOf(
        "first_steps", "curious_learner", "practice_pro", "century_club",
        "three_day_streak", "seven_day_streak", "goal_getter",
        "mock_test_starter", "perfect_practice", "dedicated_learner"
    ).flatMap { id ->
        listOf(
            "achievement_${id}_title",
            "achievement_${id}_description",
            "achievement_${id}_badge"
        )
    }

    @Test
    fun `every Phase 5 string exists in English`() {
        val missing = phase5Names.filter { it !in english }
        assertEquals("missing English strings: $missing", emptyList<String>(), missing)
    }

    @Test
    fun `every Phase 5 string exists in Hindi`() {
        val missing = phase5Names.filter { it !in hindi }
        assertEquals("missing Hindi strings: $missing", emptyList<String>(), missing)
    }

    @Test
    fun `every achievement has a title description and badge in both languages`() {
        val all = achievementStringNames
        assertEquals(emptyList<String>(), all.filter { it !in english })
        assertEquals(emptyList<String>(), all.filter { it !in hindi })
    }

    @Test
    fun `Hindi copy is actually translated and not a copy of English`() {
        // Pure format strings and symbols ("%1$d", "—", "%1$d/%2$d") are meant to
        // be identical in both languages, so they are excluded: only copy that
        // contains words has to differ.
        val translatable = phase5Names + achievementStringNames
        val identical = translatable
            .filter { it in english && it in hindi }
            .filter { containsWords(english.getValue(it).textContent.orEmpty()) }
            .filter { english.getValue(it).textContent?.trim() == hindi.getValue(it).textContent?.trim() }
        assertEquals("untranslated Hindi copy: $identical", emptyList<String>(), identical)
    }

    private fun containsWords(text: String): Boolean =
        // Format specifiers carry a type letter ("%1$d"), which is not prose.
        text.replace(Regex("%\\d+\\$[sd]"), "").any { it.isLetter() }

    @Test
    fun `English copy is not empty`() {
        val blank = phase5Names.filter { english[it]?.textContent?.isNullOrBlank() != false }
        assertEquals(emptyList<String>(), blank)
    }

    @Test
    fun `Hindi copy is not empty`() {
        val blank = phase5Names.filter { hindi[it]?.textContent?.isNullOrBlank() != false }
        assertEquals(emptyList<String>(), blank)
    }

    @Test
    fun `format placeholders match between the two languages`() {
        // A mismatch here is the worst kind of bug: it renders as a crash or as
        // a literal "%1$s" on screen.
        val mismatched = phase5Names
            .filter { it in english && it in hindi }
            .filter { placeholders(english.getValue(it)) != placeholders(hindi.getValue(it)) }

        assertEquals("placeholder mismatch: $mismatched", emptyList<String>(), mismatched)
    }

    @Test
    fun `the achievement plural and count formats line up`() {
        assertEquals(placeholders(english.getValue("achievements_subtitle")), placeholders(hindi.getValue("achievements_subtitle")))
        assertEquals(placeholders(english.getValue("gam_daily_goal_progress")), placeholders(hindi.getValue("gam_daily_goal_progress")))
        assertEquals(placeholders(english.getValue("achievements_progress_a11y")), placeholders(hindi.getValue("achievements_progress_a11y")))
    }

    @Test
    fun `Hindi uses Devanagari for achievement copy`() {
        listOf("achievement_first_steps_title", "achievement_dedicated_learner_title").forEach { name ->
            val text = hindi.getValue(name).textContent
            assertTrue("$name has no Devanagari: $text", text.any { it.code in 0x0900..0x097F })
        }
    }

    @Test
    fun `achievement titles differ from English`() {
        achievementStringNames.filter { it.endsWith("_title") }.forEach { name ->
            assertNotEquals(name, english.getValue(name).textContent, hindi.getValue(name).textContent)
        }
    }

    private fun placeholders(element: Element): List<String> {
        val pattern = element.getAttribute("formatted")
        return Regex("%\\d+\\$[sd]").findAll(pattern).map { it.value }.toList()
    }
}