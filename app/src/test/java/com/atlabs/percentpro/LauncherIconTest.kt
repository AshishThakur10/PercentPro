package com.atlabs.percentpro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.hypot

/**
 * Guards the launcher icon.
 *
 * The project shipped the stock Android Studio icon — Android's green with the
 * white robot — for its entire life, because nothing checked what the icon
 * looked like, only that the files existed. These tests check the content, so
 * dragging a fresh template in again fails the build.
 *
 * Paths are relative to the `app` module, which is the working directory for
 * JVM unit tests.
 */
class LauncherIconTest {

    private fun res(path: String) = File("src/main/res/$path")

    private fun read(path: String) = res(path).readText()

    // ---- not the template --------------------------------------------------

    @Test
    fun `the launcher background is not the android template green`() {
        val background = read("drawable/ic_launcher_background.xml")

        assertFalse(
            "the Android Studio template green must not come back",
            background.contains("3DDC84", ignoreCase = true)
        )
        assertTrue(
            "the background should be the brand DeepPurple",
            background.contains("4A2C8E", ignoreCase = true)
        )
    }

    @Test
    fun `no launcher asset still references the template green`() {
        res("drawable").listFiles()
            .orEmpty()
            .filter { it.name.startsWith("ic_launcher") }
            .forEach { file ->
                assertFalse(
                    "${file.name} still contains the template green",
                    file.readText().contains("3DDC84", ignoreCase = true)
                )
            }
    }

    @Test
    fun `the icon background is the DeepPurple used by the app theme`() {
        // Guards against the generator and the Kotlin palette drifting apart.
        val palette = File("src/main/java/com/atlabs/percentpro/ui/theme/Color.kt").readText()
        val deepPurple = Regex("""val DeepPurple = Color\(0x([0-9A-Fa-f]{8})\)""")
            .find(palette)?.groupValues?.get(1)
            ?: error("DeepPurple not found in Color.kt")

        val icon = read("drawable/ic_launcher_background.xml")
        assertTrue(
            "icon background 4A2C8E does not match theme DeepPurple $deepPurple",
            icon.contains(deepPurple.takeLast(6), ignoreCase = true)
        )
    }

    // ---- the mark fits the adaptive safe area ------------------------------

    @Test
    fun `the mark stays inside the adaptive icon safe zone`() {
        // Android's adaptive canvas is 108 units and a launcher mask may crop to
        // the centre 72, i.e. a circle of radius 36 around (54, 54). If any part
        // of the mark sits outside that, a circular launcher clips it.
        //
        // Parsed out of the shipped vector rather than restated here, so changing
        // the geometry in the generator is actually measured instead of assumed.
        val foreground = read("drawable/ic_launcher_foreground.xml")
        val centreX = 54.0
        val centreY = 54.0
        val safeRadius = 36.0

        // One <path> per element. Attributes appear in whatever order the
        // generator wrote them, so each element is read whole and its
        // strokeWidth and pathData pulled out independently.
        val elements = foreground.split("<path").drop(1)
        assertTrue("the mark should be built from several paths", elements.size >= 3)

        var circles = 0
        var lines = 0
        elements.forEach { element ->
            val stroke = Regex("""strokeWidth="([\d.]+)"""")
                .find(element)?.groupValues?.get(1)?.toDouble()
                ?: error("every mark path needs a strokeWidth: $element")
            val path = Regex("""pathData="([^"]+)"""")
                .find(element)?.groupValues?.get(1)
                ?: error("every mark path needs pathData: $element")

            // Circle: "M<x>,<y> a<r>,<r> ...". The two arcs meet back at the
            // start, so the path begins at the circle's LEFT edge and the centre
            // is one radius along x from there.
            val circle = Regex("""M([\d.]+),([\d.]+) a([\d.]+),[\d.]+""").find(path)
            if (circle != null) {
                val (edgeX, y, r) = circle.destructured
                val x = edgeX.toDouble() + r.toDouble()
                val farthest = hypot(x - centreX, y.toDouble() - centreY) +
                    r.toDouble() + stroke / 2
                assertTrue(
                    "circle centred at ($x, $y) reaches $farthest, " +
                        "outside the $safeRadius safe radius",
                    farthest <= safeRadius
                )
                circles++
            }

            // Slash: "M<x1>,<y1> L<x2>,<y2>". Round caps extend by half the stroke.
            val line = Regex("""M([\d.]+),([\d.]+) L([\d.]+),([\d.]+)""").find(path)
            if (line != null) {
                val reach = listOf(1 to 2, 3 to 4).maxOf { (a, b) ->
                    hypot(
                        line.groupValues[a].toDouble() - centreX,
                        line.groupValues[b].toDouble() - centreY
                    )
                } + stroke / 2
                assertTrue(
                    "slash reaches $reach, outside the $safeRadius safe radius",
                    reach <= safeRadius
                )
                lines++
            }
        }

        assertEquals("expected two circles in the mark", 2, circles)
        assertEquals("expected one slash in the mark", 1, lines)
    }

    // ---- adaptive icon wiring ---------------------------------------------

    @Test
    fun `the adaptive icon uses the themed icon layer`() {
        listOf("mipmap-anydpi-v26/ic_launcher.xml", "mipmap-anydpi-v26/ic_launcher_round.xml")
            .forEach { name ->
                val xml = read(name)
                assertTrue(
                    "$name must reference the brand background",
                    xml.contains("<background android:drawable=\"@drawable/ic_launcher_background\"")
                )
                assertTrue(
                    "$name must reference the brand foreground",
                    xml.contains("<foreground android:drawable=\"@drawable/ic_launcher_foreground\"")
                )
                assertTrue(
                    "$name must declare a monochrome layer for themed icons",
                    xml.contains("<monochrome")
                )
            }
    }

    @Test
    fun `the monochrome layer is its own drawable, not the coloured foreground`() {
        // Pointing monochrome at the coloured foreground is what made themed
        // icons show the coloured template robot.
        assertTrue(
            "a dedicated monochrome drawable should exist",
            res("drawable/ic_launcher_monochrome.xml").exists()
        )
        assertTrue(
            "monochrome must point at ic_launcher_monochrome",
            read("mipmap-anydpi-v26/ic_launcher.xml")
                .contains("<monochrome android:drawable=\"@drawable/ic_launcher_monochrome\"")
        )
    }

    // ---- every density is present -----------------------------------------

    @Test
    fun `every density has a correctly sized launcher icon`() {
        val expected = mapOf(
            "mdpi" to 48, "hdpi" to 72, "xhdpi" to 96,
            "xxhdpi" to 144, "xxxhdpi" to 192
        )

        expected.forEach { (density, size) ->
            listOf("ic_launcher", "ic_launcher_round").forEach { name ->
                val file = res("mipmap-$density/$name.png")
                assertTrue("$density/$name.png is missing", file.exists())
                assertEquals(
                    "$density/$name.png has the wrong pixel size",
                    listOf(size, size),
                    pngSize(file)
                )
            }
        }
    }

    @Test
    fun `no stale webp launcher icon is left behind`() {
        // Two files defining the same resource in one density bucket makes the
        // choice of which one wins ambiguous.
        res("").listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("mipmap-") }
            .forEach { folder ->
                folder.listFiles().orEmpty()
                    .filter { it.name.startsWith("ic_launcher") && it.name.endsWith(".webp") }
                    .forEach { stale ->
                        assertTrue("${stale.path} should have been deleted", !stale.exists())
                    }
            }
    }

    // ---- the Play listing icon --------------------------------------------

    @Test
    fun `the play store icon meets its published requirements`() {
        val icon = File("../store/graphics/ic_launcher_play_512.png")

        assertTrue("the 512x512 Play icon should exist", icon.exists())
        assertEquals(
            "Play requires exactly 512x512",
            listOf(512, 512),
            pngSize(icon)
        )
        assertTrue(
            "Play caps the icon at 1024 KB",
            icon.length() < 1024 * 1024
        )
    }

    /** PNG width and height, read from the IHDR chunk. */
    private fun pngSize(file: File): List<Int> {
        val bytes = file.readBytes()
        fun byteAt(index: Int) = bytes[index].toInt() and 0xff
        require(bytes.size > 24 &&
                byteAt(1) == 'P'.code && byteAt(2) == 'N'.code && byteAt(3) == 'G'.code
        ) { "${file.name} is not a PNG" }
        fun be(index: Int) = (byteAt(index) shl 24) or (byteAt(index + 1) shl 16) or
                (byteAt(index + 2) shl 8) or byteAt(index + 3)
        return listOf(be(16), be(20))
    }
}