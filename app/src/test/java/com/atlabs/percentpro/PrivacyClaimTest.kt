package com.atlabs.percentpro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the promises the settings screen makes.
 *
 * The privacy text tells the reader that the app makes no network requests,
 * needs no account, and keeps everything on the device. Those are claims about
 * the build, not about the code, so nothing in the unit test suite would notice
 * if someone re-enabled Auto Backup and quietly turned all three into a lie.
 *
 * These tests read the packaged configuration directly, so a change that
 * contradicts the on-screen text has to fail here.
 */
class PrivacyClaimTest {

    private fun parse(path: String): Element =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(path))
            .documentElement

    private fun manifest(): Element = parse("src/main/AndroidManifest.xml")

    private fun applicationAttributes(): Map<String, String> {
        val application = manifest().getElementsByTagName("application").item(0) as Element
        val map = mutableMapOf<String, String>()
        val attributes = application.attributes
        for (index in 0 until attributes.length) {
            val attribute = attributes.item(index)
            map[attribute.nodeName] = attribute.nodeValue
        }
        return map
    }

    @Test
    fun `auto backup is disabled`() {
        // This is the load-bearing one. With backup on, lesson progress and
        // achievements are uploaded to a Google account, which is both a network
        // request and an account, and the settings screen would be stating a
        // falsehood.
        val allowBackup = applicationAttributes()["android:allowBackup"]
        assertEquals(
            "Auto Backup must stay off for the offline-only claim to be true",
            "false",
            allowBackup
        )
    }

    @Test
    fun `no backup or transfer rule includes any app data`() {
        val dataRules = parse("src/main/res/xml/data_extraction_rules.xml")

        listOf("cloud-backup", "device-transfer").forEach { section ->
            val nodes = dataRules.getElementsByTagName(section)
            assertTrue("expected a <$section> section", nodes.length == 1)
            val element = nodes.item(0) as Element
            val children = element.childNodes
            var excludes = 0
            for (index in 0 until children.length) {
                val child = children.item(index)
                if (child is Element && child.tagName == "exclude") excludes++
            }
            assertTrue(
                "<$section> must exclude every domain, found $excludes excludes",
                excludes >= 5
            )
            assertEquals(
                "<$section> must not include anything",
                0,
                element.getElementsByTagName("include").length
            )
        }
    }

    @Test
    fun `the pre-Android-12 backup rules exclude every domain`() {
        val legacy = parse("src/main/res/xml/backup_rules.xml")
        val excludes = legacy.getElementsByTagName("exclude").length
        assertTrue("full-backup-content must exclude every domain", excludes >= 5)
        assertEquals(
            "full-backup-content must not include anything",
            0,
            legacy.getElementsByTagName("include").length
        )
    }

    @Test
    fun `the app requests no permissions`() {
        // No INTERNET permission is the strongest single statement that no
        // network request can be made at all, and no other permission has been
        // requested that the app does not need.
        val usesPermission = manifest().getElementsByTagName("uses-permission")
        assertEquals(
            "PercentPro declares no permissions",
            0,
            usesPermission.length
        )
    }

    @Test
    fun `the app declares only the launcher activity`() {
        val activities = manifest().getElementsByTagName("activity")
        assertEquals(1, activities.length)
        val activity = activities.item(0) as Element
        assertEquals(".MainActivity", activity.getAttribute("android:name"))

        // android:exported is an attribute, so it has to be read as one; the
        // launcher activity is the app's only way in from outside.
        assertEquals("true", activity.getAttribute("android:exported"))

        val receivers = manifest().getElementsByTagName("receiver")
        val services = manifest().getElementsByTagName("service")
        val providers = manifest().getElementsByTagName("provider")
        assertEquals(0, receivers.length)
        assertEquals(0, services.length)
        assertEquals(0, providers.length)
    }

    @Test
    fun `the declared locales match the shipped translations`() {
        // A stale localeConfig would offer the user a language the app cannot
        // actually display.
        val declared = parse("src/main/res/xml/locales_config.xml")
            .getElementsByTagName("locale")
        val languages = (0 until declared.length).mapNotNull {
            (declared.item(it) as Element).getAttribute("android:name").takeIf(String::isNotBlank)
        }
        assertEquals(listOf("en", "hi"), languages)

        assertTrue(
            "English strings must exist",
            File("src/main/res/values/strings.xml").exists()
        )
        assertTrue(
            "Hindi strings must exist",
            File("src/main/res/values-hi/strings.xml").exists()
        )
    }

    @Test
    fun `the dark launch window is declared`() {
        // Without a values-night theme the system paints a light window before
        // the first Compose frame, which a dark-mode user sees as a white flash.
        assertTrue(
            "a night theme is required",
            File("src/main/res/values-night/themes.xml").exists()
        )
        assertTrue(
            "a night window background is required",
            File("src/main/res/values-night/colors.xml").exists()
        )
    }
}