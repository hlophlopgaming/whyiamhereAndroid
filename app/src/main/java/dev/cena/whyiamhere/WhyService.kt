package dev.cena.whyiamhere

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import dev.cena.whyiamhere.data.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WhyService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val app get() = application as WhyApplication
    private lateinit var filter: PackageFilter
    private lateinit var widget: OverlayWidget
    private lateinit var alerts: Alerts
    private var currentPackage: String? = null
    private var lastLogged: String? = null
    private var pendingGate: String? = null
    private var gateTimeoutJob: Job? = null
    private var refreshJob: Job? = null
    private var eventJob: Job? = null
    private var timerJob: Job? = null
    private var timerSession: Session? = null
    private var endingPackage: String? = null
    private var receiverRegistered = false
    // shortcut: повторное подключение службы может повторить звук; сохранять флаг в Room, если нужен ровно один звук за сессию.
    private val warned = mutableSetOf<Pair<String, Long>>()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                currentPackage = null
                lastLogged = null
                pendingGate = null
                hideWidget()
            } else {
                currentPackage = readForegroundPackage()
                refresh()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        connected.value = true
        filter = createPackageFilter()
        alerts = Alerts(this)
        widget = OverlayWidget(this,
            onFinish = { session -> scope.launch {
                endingPackage = session.packageName
                try {
                    app.endSession(session)
                    if (currentPackage == session.packageName) home()
                } catch (error: Exception) { report(error, appLanguageContext().getString(R.string.error_finish_session)) }
                finally { endingPackage = null }
            } },
            onPosition = { x, y -> saveSetting { app.store.saveWidgetPosition(x, y) } },
            onCollapsed = { value -> saveSetting { app.store.setCollapsed(value) } },
        )
        val intents = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, intents, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(screenReceiver, intents)
        receiverRegistered = true
        scope.launch {
            try {
                app.ready.await()
                currentPackage = readForegroundPackage()
                refresh()
                launch { app.settings.collect { refresh() } }
                launch { app.sessionChanges.collect { refresh() } }
            } catch (error: Exception) { report(error, appLanguageContext().getString(R.string.error_restore_sessions)) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::filter.isInitialized || event == null) return
        val eventPackage = event.packageName?.toString()
        val fallback = if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) eventPackage else currentPackage
        if (eventPackage != null && filter.isTransient(eventPackage)) return
        eventJob?.cancel()
        eventJob = scope.launch {
            delay(80)
            val packageName = readForegroundPackage() ?: fallback ?: return@launch
            if (filter.isTransient(packageName)) return@launch
            if (filter.canTrack(packageName)) {
                if (lastLogged != packageName) Log.d(TAG, "Foreground: $packageName")
                lastLogged = packageName
            } else lastLogged = null
            currentPackage = packageName
            if (packageName != this@WhyService.packageName && packageName !in app.settings.value.selectedPackages) pendingGate = null
            refresh()
        }
    }

    private fun readForegroundPackage(): String? {
        return windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .sortedWith(compareByDescending<AccessibilityWindowInfo> { it.isFocused }
                .thenByDescending { it.isActive }.thenByDescending { it.layer })
            .firstNotNullOfOrNull { window ->
                val root = window.root ?: return@firstNotNullOfOrNull null
                try { root.packageName?.toString() } finally {
                    @Suppress("DEPRECATION")
                    root.recycle()
                }
            }
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            try {
                app.ready.await()
                val packageName = currentPackage
                if (packageName != null && packageName == endingPackage) return@launch
                if (!screenAvailable() || packageName == null || !filter.canTrack(packageName) ||
                    packageName !in app.settings.value.selectedPackages) {
                    hideWidget()
                    return@launch
                }
                val session = app.sessions.session(packageName)
                if (session == null || session.remainingMillis(System.currentTimeMillis()) == 0L) {
                    hideWidget()
                    if (session != null) app.endSession(session)
                    if (currentPackage == packageName && screenAvailable()) openGate(packageName)
                } else {
                    pendingGate = null
                    widget.show(session, app.settings.value)
                    startTimer(session)
                }
            } catch (error: Exception) {
                report(error, appLanguageContext().getString(R.string.error_show_goal))
                home()
            }
        }
    }

    private fun openGate(packageName: String) {
        if (pendingGate == packageName) return
        pendingGate = packageName
        gateTimeoutJob?.cancel()
        startActivity(Intent(this, GateActivity::class.java)
            .putExtra(GateActivity.EXTRA_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        gateTimeoutJob = scope.launch {
            // shortcut: launches over five seconds can still time out; revisit if seen on slow devices.
            delay(5_000)
            if (pendingGate == packageName && currentPackage == packageName &&
                app.sessions.active(packageName, System.currentTimeMillis()) == null) {
                Log.w(TAG, "Gate did not become visible; returning home")
                home()
            }
        }
    }

    private fun gateStarted(packageName: String?) {
        if (pendingGate == packageName) gateTimeoutJob?.cancel()
    }

    private fun startTimer(session: Session) {
        if (timerSession == session && timerJob?.isActive == true) return
        timerJob?.cancel()
        timerSession = session
        timerJob = scope.launch {
            try {
                while (isActive && currentPackage == session.packageName && screenAvailable() &&
                    app.sessions.session(session.packageName) == session) {
                    val now = System.currentTimeMillis()
                    val remaining = session.remainingMillis(now)
                    if (remaining == 0L) {
                        endingPackage = session.packageName
                        try { app.endSession(session) }
                        finally {
                            endingPackage = null
                            if (currentPackage == session.packageName) home()
                        }
                        return@launch
                    }
                    val warning = session.shouldWarn(now, app.settings.value.warningSeconds)
                    widget.update(remaining, warning)
                    if (warning && warned.add(session.packageName to session.startedAt)) alerts.warn()
                    delay(1_000)
                }
                widget.hide()
            } catch (error: Exception) { report(error, appLanguageContext().getString(R.string.error_timer)) }
        }
    }

    private fun screenAvailable(): Boolean = getSystemService(PowerManager::class.java)?.isInteractive == true &&
        getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == false

    private fun hideWidget() {
        timerJob?.cancel()
        timerJob = null
        timerSession = null
        if (::widget.isInitialized) widget.hide()
        if (::alerts.isInitialized) alerts.stop()
    }

    private fun home() {
        currentPackage = null
        pendingGate = null
        lastLogged = null
        hideWidget()
        if (!performGlobalAction(GLOBAL_ACTION_HOME)) launchHome(this)
    }

    private fun saveSetting(action: suspend () -> Unit) {
        scope.launch {
            try { action() } catch (error: Exception) { report(error, appLanguageContext().getString(R.string.error_widget_position)) }
        }
    }

    private fun report(error: Exception, message: String) {
        if (error is CancellationException) throw error
        Log.e(TAG, message, error)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::widget.isInitialized) { widget.hide(); refresh() }
    }

    override fun onInterrupt() { hideWidget() }

    private fun disconnect() {
        currentPackage = null
        pendingGate = null
        connected.value = false
        if (instance === this) instance = null
        hideWidget()
        scope.cancel()
        if (receiverRegistered) { unregisterReceiver(screenReceiver); receiverRegistered = false }
    }

    override fun onUnbind(intent: Intent?): Boolean { disconnect(); return super.onUnbind(intent) }
    override fun onDestroy() { disconnect(); super.onDestroy() }

    companion object {
        private const val TAG = "WhyService"
        private var instance: WhyService? = null
        val connected = MutableStateFlow(false)

        fun goHome(context: Context) { instance?.home() ?: launchHome(context) }

        fun gateStarted(packageName: String?) { instance?.gateStarted(packageName) }

        private fun launchHome(context: Context) {
            try {
                context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (error: Exception) { Log.e(TAG, "Cannot open home screen", error) }
        }
    }
}
