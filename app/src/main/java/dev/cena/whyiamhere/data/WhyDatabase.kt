package dev.cena.whyiamhere.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase

@Dao
interface WhyDao {
    @Query("SELECT * FROM sessions")
    suspend fun sessions(): List<Session>

    @Query("SELECT * FROM history WHERE packageName = :packageName ORDER BY startedAt DESC, id DESC LIMIT 10")
    suspend fun recentGoals(packageName: String): List<HistoryItem>
}

@Database(entities = [Session::class, HistoryItem::class], version = 1, exportSchema = true)
abstract class WhyDatabase : RoomDatabase() {
    abstract fun dao(): WhyDao
}
