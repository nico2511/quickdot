package dev.octoshrimpy.quik.feature.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuoteTextTest {

    @Test
    fun excerptCollapsesWhitespaceAndCutsAtEighty() {
        val text = "  hello\n\nworld  " + "x".repeat(90)
        val excerpt = QuoteText.excerpt(text)
        assertEquals(80, excerpt.length)
        assertEquals(true, excerpt.endsWith("..."))
        assertEquals(false, excerpt.contains('\n'))
    }

    @Test
    fun excerptKeepsShortText() {
        assertEquals("The sprocket is on the left", QuoteText.excerpt("The sprocket is on the left"))
    }

    @Test
    fun composeAndParseRoundTrip() {
        val body = QuoteText.composeBody("Mrelay", "The sprocket is on the left", "On my way")
        assertEquals("> Mrelay: The sprocket is on the left\nOn my way", body)
        val parsed = QuoteText.parse(body)
        assertEquals("Mrelay", parsed?.name)
        assertEquals("The sprocket is on the left", parsed?.excerpt)
        assertEquals("On my way", parsed?.reply)
    }

    @Test
    fun photoExcerptSurvivesTheWire() {
        val body = QuoteText.composeBody("Olivia Ames", "[Photo]", "Nice")
        assertEquals("> Olivia Ames: [Photo]\nNice", body)
        assertEquals("[Photo]", QuoteText.parse(body)?.excerpt)
    }

    @Test
    fun nameStripsColonsAndNewlines() {
        assertEquals("Ada Lovelace", QuoteText.sanitizeName("Ada: Lovelace\n"))
    }

    @Test
    fun singleLineIsNotAQuote() {
        assertNull(QuoteText.parse("> Mrelay: hello"))
    }

    @Test
    fun ordinaryMessageIsNotAQuote() {
        assertNull(QuoteText.parse("See you at 8"))
    }
}
