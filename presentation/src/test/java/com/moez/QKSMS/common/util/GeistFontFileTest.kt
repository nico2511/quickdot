package dev.octoshrimpy.quik.common.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

class GeistFontFileTest {

    @Test
    fun variableFileIsNamedGeist() {
        val data = File("src/main/res/font/geist_wght.ttf").readBytes()
        val names = nameRecords(data)
        assertTrue(names.any { it.contains("Geist") })
        assertTrue(tags(data).contains("fvar"))
    }

    private fun tags(data: ByteArray): Set<String> {
        val count = u16(data, 4)
        return (0 until count).map { index ->
            val at = 12 + index * 16
            String(data, at, 4, StandardCharsets.US_ASCII)
        }.toSet()
    }

    private fun nameRecords(data: ByteArray): List<String> {
        val name = tableOffset(data, "name")
        val count = u16(data, name + 2)
        val storage = u16(data, name + 4)
        val out = mutableListOf<String>()
        for (i in 0 until count) {
            val rec = name + 6 + i * 12
            val platform = u16(data, rec)
            val length = u16(data, rec + 8)
            val offset = u16(data, rec + 10)
            if (platform != 3) continue
            val start = name + storage + offset
            out += String(data, start, length, StandardCharsets.UTF_16BE)
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
