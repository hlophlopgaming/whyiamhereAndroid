package dev.cena.whyiamhere.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryTest {
    @Test fun startHistoryAndClear() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhyDatabase::class.java).build()
        try {
            val dao = db.dao()
            dao.startSession(SessionManager.create("app.a", "Первая цель", 5, 1_000))
            dao.startSession(SessionManager.create("app.b", "Другая цель", 15, 2_000))
            dao.startSession(SessionManager.create("app.a", "Новая цель", 30, 3_000))
            assertEquals(listOf("Новая цель", "Первая цель"), dao.recentGoals("app.a").map { it.goal })
            assertEquals(2, dao.sessions().size)
            assertEquals("Другая цель", dao.recentGoals("app.b").single().goal)
            dao.deleteSession("app.a")
            assertEquals(1, dao.sessions().size)
            assertEquals(2, dao.recentGoals("app.a").size)
            dao.clearHistory()
            assertTrue(dao.recentGoals("app.a").isEmpty())
            assertEquals("app.b", dao.sessions().single().packageName)
        } finally { db.close() }
    }
}
