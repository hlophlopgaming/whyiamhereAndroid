package dev.cena.whyiamhere

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerFormatTest {
    @Test fun `round up partial seconds without showing zero before expiry`() {
        assertEquals("0:00", formatRemaining(0))
        assertEquals("0:00", formatRemaining(-1))
        assertEquals("0:01", formatRemaining(1))
        assertEquals("0:01", formatRemaining(1_000))
        assertEquals("1:00", formatRemaining(60_000))
        assertEquals("1:01", formatRemaining(60_001))
        assertEquals("90:00", formatRemaining(5_400_000))
    }
}
