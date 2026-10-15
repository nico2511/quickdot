package dev.octoshrimpy.quik.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class HintFitTest {

    @Test
    fun hintThatFitsKeepsTheRestingSize() {
        assertEquals(36.75f, fittedHintSizePx(36.75f, 640f, 660f, 31.5f), 0.01f)
    }

    @Test
    fun longHintScalesDownToTheAvailableWidth() {
        // 14sp Doto bold of the French inbox hint is ~750px; the pill has ~660px.
        val fitted = fittedHintSizePx(36.75f, 750f, 660f, 31.5f)
        assertEquals(36.75f * 660f / 750f, fitted, 0.01f)
    }

    @Test
    fun floorStopsANarrowFieldFromShrinkingTheHintFurther() {
        assertEquals(31.5f, fittedHintSizePx(36.75f, 900f, 400f, 31.5f), 0.01f)
    }

    @Test
    fun floorAboveTheRestingSizeDoesNotGrowTheHint() {
        assertEquals(20f, fittedHintSizePx(20f, 800f, 400f, 31.5f), 0.01f)
    }

    @Test
    fun missingMeasurementKeepsTheRestingSize() {
        assertEquals(36.75f, fittedHintSizePx(36.75f, 0f, 660f, 31.5f), 0.01f)
        assertEquals(36.75f, fittedHintSizePx(36.75f, 750f, 0f, 31.5f), 0.01f)
    }
}
