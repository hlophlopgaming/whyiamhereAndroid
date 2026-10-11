package dev.cena.whyiamhere.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val packageName: String,
    val goal: String,
    val durationMin: Int,
    val startedAt: Long,
    val endAt: Long,
) {
    init {
        require(packageName.isNotBlank())
        require(goal.isNotBlank())
        require(durationMin > 0)
        require(startedAt >= 0 && endAt > startedAt)
    }

    fun remainingMillis(now: Long): Long = (endAt - now).coerceAtLeast(0)

    fun shouldWarn(now: Long, warningSeconds: Int): Boolean {
        val remaining = remainingMillis(now)
        return warningSeconds > 0 && remaining > 0 && remaining <= warningSeconds.toLong() * 1000
    }
}
