package dev.cena.whyiamhere.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "history", indices = [Index(value = ["packageName", "startedAt"])])
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val goal: String,
    val durationMin: Int,
    val startedAt: Long,
)
