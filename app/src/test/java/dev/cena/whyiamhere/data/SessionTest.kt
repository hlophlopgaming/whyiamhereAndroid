package dev.cena.whyiamhere.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    @Test
    fun `remaining time uses persisted wall clock deadline`() {
        val session = Session("com.example.app", "Ответить другу", 5, 1_000L, 301_000L)
        assertEquals(300_000L, session.remainingMillis(1_000L))
        assertEquals(120_000L, session.copy().remainingMillis(181_000L))
        assertEquals(0L, session.remainingMillis(301_000L))
        assertEquals(0L, session.remainingMillis(400_000L))
    }

    @Test
    fun `invalid sessions are rejected`() {
        val valid = Session("com.example.app", "Ответить другу", 5, 1_000L, 301_000L)
        assertThrows(IllegalArgumentException::class.java) { valid.copy(packageName = " ") }
        assertThrows(IllegalArgumentException::class.java) { valid.copy(goal = " ") }
        assertThrows(IllegalArgumentException::class.java) { valid.copy(durationMin = 0) }
        assertThrows(IllegalArgumentException::class.java) { valid.copy(endAt = 1_000L) }
    }

    @Test
    fun `warning starts at threshold and is disabled after expiry or at zero setting`() {
        val session = Session("app", "Цель", 5, 1_000L, 301_000L)
        assertFalse(session.shouldWarn(240_999, 60))
        assertTrue(session.shouldWarn(241_000, 60))
        assertTrue(session.shouldWarn(300_999, 60))
        assertFalse(session.shouldWarn(301_000, 60))
        assertFalse(session.shouldWarn(241_000, 0))
        assertFalse(session.shouldWarn(241_000, -1))
    }
}
