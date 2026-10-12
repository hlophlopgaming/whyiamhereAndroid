package dev.cena.whyiamhere

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.cena.whyiamhere.data.HistoryItem
import dev.cena.whyiamhere.data.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GateActivity : ComponentActivity() {
    private var targetPackage by mutableStateOf<String?>(null)
    private var completed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE)
        if (targetPackage.isNullOrBlank()) { cancelGate(); return }
        val app = application as WhyApplication
        setContent {
            val packageName = targetPackage ?: return@setContent
            key(packageName) {
                val scope = rememberCoroutineScope()
                val ready by app.isReady.collectAsState()
                val settings by app.settings.collectAsState()
                val storageError by app.storageError.collectAsState()
                var goal by rememberSaveable { mutableStateOf("") }
                var minutes by rememberSaveable { mutableStateOf("15") }
                var history by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
                var label by remember { mutableStateOf(packageName) }
                var busy by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }
                BackHandler { cancelGate() }

                LaunchedEffect(packageName) {
                    try {
                        app.ready.await()
                        if (packageName !in app.settings.value.selectedPackages) { cancelGate(); return@LaunchedEffect }
                        history = app.database.dao().recentGoals(packageName).distinctBy { it.goal }
                        label = withContext(Dispatchers.IO) {
                            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
                        }
                    } catch (failure: Exception) {
                        if (failure is CancellationException) throw failure
                        error = getString(R.string.error_load_gate)
                    }
                }

                WhyTheme {
                    Scaffold { padding ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                                .verticalScroll(rememberScrollState()).padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                            Text(stringResource(R.string.gate_intro))
                            OutlinedTextField(
                                value = goal, onValueChange = { goal = it },
                                label = { Text(stringResource(R.string.goal_label)) }, placeholder = { Text(stringResource(R.string.goal_example)) },
                                enabled = !busy, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
                            )
                            Text(stringResource(R.string.duration_title), style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(5, 15, 30).forEach { preset ->
                                    FilterChip(selected = minutes == preset.toString(), enabled = !busy,
                                        onClick = { minutes = preset.toString() }, label = { Text(stringResource(R.string.duration_minutes, preset)) })
                                }
                            }
                            OutlinedTextField(
                                value = minutes, onValueChange = { minutes = it },
                                label = { Text(stringResource(R.string.custom_minutes)) }, singleLine = true, enabled = !busy,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            (storageError?.let { stringResource(it) } ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            if (!ready && storageError == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                            Button(
                                enabled = ready && !busy && goal.isNotBlank() && minutes.toIntOrNull()?.let { it > 0 } == true &&
                                    packageName in settings.selectedPackages,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = click@{
                                    if (busy) return@click
                                    busy = true
                                    error = null
                                    scope.launch {
                                        var started: Session? = null
                                        try {
                                            val launch = packageManager.getLaunchIntentForPackage(packageName)
                                                ?: throw IllegalStateException(getString(R.string.error_app_removed))
                                            started = app.startSession(packageName, goal, minutes.toInt())
                                            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                            completed = true
                                            finishAndRemoveTask()
                                        } catch (failure: Exception) {
                                            if (failure is CancellationException) throw failure
                                            started?.let { session ->
                                                try { app.endSession(session) } catch (cleanup: Exception) {
                                                    if (cleanup is CancellationException) throw cleanup
                                                }
                                            }
                                            error = getString(R.string.error_start_session)
                                            busy = false
                                        }
                                    }
                                },
                            ) { Text(stringResource(if (busy) R.string.opening_app else R.string.start_session)) }
                            TextButton(onClick = { cancelGate() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.return_home)) }
                            if (history.isNotEmpty()) {
                                Text(stringResource(R.string.recent_goals), style = MaterialTheme.typography.titleSmall)
                                Column(Modifier.fillMaxWidth().heightIn(max = 160.dp)
                                    .verticalScroll(rememberScrollState())) {
                                    history.forEach { item ->
                                        OutlinedButton(enabled = !busy, onClick = {
                                            goal = item.goal; minutes = item.durationMin.toString()
                                        }, modifier = Modifier.fillMaxWidth()) { Text(item.goal) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        completed = false
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE)
        if (targetPackage.isNullOrBlank()) cancelGate()
    }

    private fun cancelGate() {
        completed = true
        WhyService.goHome(this)
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        if (!completed && isFinishing && !isChangingConfigurations) WhyService.goHome(this)
        super.onDestroy()
    }

    companion object { const val EXTRA_PACKAGE = "package_name" }
}
