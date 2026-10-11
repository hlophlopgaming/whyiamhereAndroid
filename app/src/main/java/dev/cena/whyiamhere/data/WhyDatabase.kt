package dev.cena.whyiamhere.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Insert
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface WhyDao {
    @Query("SELECT * FROM sessions")
    suspend fun sessions(): List<Session>

    @Query("SELECT * FROM history WHERE packageName = :packageName ORDER BY startedAt DESC, id DESC LIMIT 10")
    suspend fun recentGoals(packageName: String): List<HistoryItem>

    @Upsert
    suspend fun saveSession(session: Session)

    @Insert
    suspend fun addHistory(item: HistoryItem)

    @Transaction
    suspend fun startSession(session: Session) {
        saveSession(session)
        addHistory(HistoryItem(
            packageName = session.packageName,
            goal = session.goal,
            durationMin = session.durationMin,
            startedAt = session.startedAt,
        ))
    }

    @Query("DELETE FROM sessions WHERE packageName = :packageName")
    suspend fun deleteSession(packageName: String)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}

@Database(entities = [Session::class, HistoryItem::class], version = 1, exportSchema = true)
abstract class WhyDatabase : RoomDatabase() {
    abstract fun dao(): WhyDao
}
