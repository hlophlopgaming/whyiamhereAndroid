package dev.cena.whyiamhere.data

import org.junit.Assert.*
import org.junit.Test

class SessionManagerTest {
    private fun session(pkg: String = "app.a", minutes: Int = 5, now: Long = 1_000) =
        SessionManager.create(pkg, "  Ответить другу  ", minutes, now)

    @Test fun `start trims goal and computes deadline`() {
        val manager = SessionManager()
        val value = session()
        manager.start(value)
        assertEquals("Ответить другу", value.goal)
        assertEquals(301_000L, value.endAt)
        assertEquals(value, manager.active("app.a", 1_000))
    }

    @Test fun `expiry includes the exact deadline`() {
        val manager = SessionManager()
        manager.start(session())
        assertNotNull(manager.active("app.a", 300_999))
        assertNull(manager.active("app.a", 301_000))
        assertNull(manager.active("app.a", 400_000))
    }

    @Test fun `apps have independent timers and ending one keeps the other`() {
        val manager = SessionManager()
        manager.start(session("app.a", 5))
        manager.start(session("app.b", 15))
        assertNull(manager.active("app.a", 400_000))
        assertNotNull(manager.active("app.b", 400_000))
        manager.finish("app.a")
        assertNull(manager.session("app.a"))
        assertNotNull(manager.session("app.b"))
    }

    @Test fun `early finish requires a new session`() {
        val manager = SessionManager()
        manager.start(session())
        manager.finish("app.a")
        assertNull(manager.active("app.a", 10_000))
        val replacement = session(now = 20_000)
        manager.start(replacement)
        assertEquals(320_000L, manager.active("app.a", 30_000)?.endAt)
    }

    @Test fun `restore after restart uses original wall clock deadline`() {
        val saved = session()
        val manager = SessionManager()
        manager.restore(listOf(saved))
        assertEquals(120_000L, manager.active("app.a", 181_000)?.remainingMillis(181_000))
        assertNull(manager.active("app.a", 301_000))
    }

    @Test fun `restore replaces previous memory and start replaces only one package`() {
        val manager = SessionManager()
        manager.start(session("app.old"))
        manager.restore(listOf(session("app.a"), session("app.b")))
        assertNull(manager.session("app.old"))
        manager.start(session("app.a", 30))
        assertEquals(30, manager.session("app.a")?.durationMin)
        assertEquals(5, manager.session("app.b")?.durationMin)
    }

    @Test fun `invalid input and overflowing deadline are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { session(minutes = 0) }
        assertThrows(IllegalArgumentException::class.java) { session(minutes = -1) }
        assertThrows(IllegalArgumentException::class.java) { SessionManager.create("app", " ", 5, 100) }
        assertThrows(ArithmeticException::class.java) { session(now = Long.MAX_VALUE) }
    }
}
