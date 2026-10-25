package dev.octoshrimpy.quik.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BundledFontLicensesTest {

    @Test
    fun listsGeistDotoAndSpaceMono() {
        val titles = BundledFontLicenses.entries.map { it.title }
        assertEquals(listOf("Geist", "Doto", "Space Mono"), titles)
    }

    @Test
    fun shipsOflTextForEachFont() {
        val assetsRoot = File("src/main/assets")
        BundledFontLicenses.entries.forEach { entry ->
            val file = File(assetsRoot, entry.licenseAssetPath)
            assertTrue("${entry.title} license missing", file.isFile)
            assertTrue(file.readText().contains("SIL Open Font License"))
        }
    }
}
