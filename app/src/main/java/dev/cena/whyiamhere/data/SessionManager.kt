package dev.cena.whyiamhere.data

class SessionManager {
    private val sessions = mutableMapOf<String, Session>()

    fun restore(saved: List<Session>) {
        sessions.clear()
        saved.forEach { sessions[it.packageName] = it }
    }

    fun session(packageName: String): Session? = sessions[packageName]

    fun active(packageName: String, now: Long): Session? =
        session(packageName)?.takeIf { it.remainingMillis(now) > 0 }

    fun start(session: Session) { sessions[session.packageName] = session }

    fun finish(packageName: String) { sessions.remove(packageName) }

    companion object {
        fun create(packageName: String, goal: String, durationMin: Int, now: Long): Session = Session(
            packageName = packageName,
            goal = goal.trim(),
            durationMin = durationMin,
            startedAt = now,
            endAt = Math.addExact(now, durationMin.toLong() * 60_000L),
        )
    }
}
