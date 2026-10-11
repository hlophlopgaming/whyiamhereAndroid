package dev.cena.whyiamhere

import android.app.Application
import androidx.room.Room
import dev.cena.whyiamhere.data.WhyDatabase
import dev.cena.whyiamhere.data.AppSettings
import dev.cena.whyiamhere.data.Session
import dev.cena.whyiamhere.data.SessionManager
import dev.cena.whyiamhere.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException

class WhyApplication : Application() {
    val database: WhyDatabase by lazy {
        Room.databaseBuilder(this, WhyDatabase::class.java, "why.db").build()
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val store by lazy { SettingsStore(this) }
    val sessions = SessionManager()
    val settings = MutableStateFlow(AppSettings())
    val sessionChanges = MutableStateFlow(0)
    val storageError = MutableStateFlow<Int?>(null)
    val isReady = MutableStateFlow(false)
    private val mutex = Mutex()

    val ready: Deferred<Unit> by lazy {
        scope.async<Unit> {
            try {
                settings.value = store.settings.first()
                sessions.restore(database.dao().sessions())
                isReady.value = true
                scope.launch {
                    try {
                        store.settings.collect { settings.value = it }
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        storageError.value = R.string.error_read_settings
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                storageError.value = R.string.error_open_data
                throw error
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ready
    }

    suspend fun startSession(packageName: String, goal: String, durationMin: Int): Session {
        ready.await()
        return mutex.withLock {
            require(packageName in settings.value.selectedPackages)
            val now = System.currentTimeMillis()
            require(sessions.active(packageName, now) == null)
            val session = SessionManager.create(packageName, goal, durationMin, now)
            database.dao().startSession(session)
            sessions.start(session)
            sessionChanges.value++
            session
        }
    }

    suspend fun endSession(session: Session) {
        ready.await()
        mutex.withLock {
            if (sessions.session(session.packageName) == session) {
                database.dao().deleteSession(session.packageName)
                sessions.finish(session.packageName)
                sessionChanges.value++
            }
        }
    }
}
