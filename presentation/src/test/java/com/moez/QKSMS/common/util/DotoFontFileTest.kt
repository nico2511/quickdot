package dev.octoshrimpy.quik.common.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * The static Doto files shipped with the same family name ("Doto Black") for
 * regular and bold, plus a STAT table and no fvar. Android 14 resolves that
 * pair to one face and the thread title falls back to the system sans.
 */
class DotoFontFileTest {

    @Test
    fun regularAndBoldDoNotShareAFamilyName() {
        val regular = names(file("doto_regular.ttf"))
        val bold = names(file("doto_bold.ttf"))
        assertTrue(regular.getValue(1).contains("Doto Regular"))
        assertTrue(bold.getValue(1).contains("Doto Bold"))
        assertFalse(regular.getValue(1).contains("Doto Black"))
        assertFalse(bold.getValue(1).contains("Doto Black"))
        assertFalse(regular.getValue(1) == bold.getValue(1))
    }

    @Test
    fun weightsStayOnTheOs2TableAndStatIsGone() {
        assertTrue(weight(file("doto_regular.ttf")) == 400)
        assertTrue(weight(file("doto_bold.ttf")) == 700)
        assertTrue(weight(file("doto_black.ttf")) == 900)
        listOf("doto_regular.ttf", "doto_medium.ttf", "doto_bold.ttf", "doto_black.ttf").forEach { name ->
            assertFalse(name, tags(file(name)).contains("STAT"))
            assertFalse(name, tags(file(name)).contains("fvar"))
        }
    }

    private fun file(name: String): ByteArray {
        return File("src/main/res/font/$name").readBytes()
    }

    private fun tags(data: ByteArray): Set<String> {
        val count = u16(data, 4)
        return (0 until count).map { index ->
            val at = 12 + index * 16
            String(data, at, 4, StandardCharsets.US_ASCII)
        }.toSet()
    }

    private fun weight(data: ByteArray): Int {
        val os2 = tableOffset(data, "OS/2")
        return u16(data, os2 + 4)
    }

    private fun names(data: ByteArray): Map<Int, String> {
        val name = tableOffset(data, "name")
        val count = u16(data, name + 2)
        val storage = u16(data, name + 4)
        val out = linkedMapOf<Int, String>()
        for (i in 0 until count) {
            val rec = name + 6 + i * 12
            val platform = u16(data, rec)
            val id = u16(data, rec + 6)
            val length = u16(data, rec + 8)
            val offset = u16(data, rec + 10)
            if (platform != 3 || id !in setOf(1, 2, 6, 16)) continue
            val start = name + storage + offset
            out[id] = String(data, start, length, StandardCharsets.UTF_16BE)
        }
        return out
    }

    private fun tableOffset(data: ByteArray, tag: String): Int {
        val count = u16(data, 4)
        for (index in 0 until count) {
            val at = 12 + index * 16
            if (String(data, at, 4, StandardCharsets.US_ASCII) == tag) {
                return u32(data, at + 8)
            }
        }
        throw IllegalStateException("missing $tag")
    }

    private fun u16(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xff) shl 8) or (data[offset + 1].toInt() and 0xff)
    }

    private fun u32(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xff) shl 24) or
            ((data[offset + 1].toInt() and 0xff) shl 16) or
            ((data[offset + 2].toInt() and 0xff) shl 8) or
            (data[offset + 3].toInt() and 0xff)
    }
}
