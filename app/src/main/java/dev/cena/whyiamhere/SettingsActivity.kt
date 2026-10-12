package dev.cena.whyiamhere

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class InstalledApp(val packageName: String, val label: String, val icon: ImageBitmap)

class SettingsActivity : ComponentActivity() {
    private var serviceEnabled by mutableStateOf(false)
    private var resumeCount by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        serviceEnabled = requireNotNull(getSystemService(AccessibilityManager::class.java))
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName &&
                it.resolveInfo.serviceInfo.name == WhyService::class.java.name }
        resumeCount++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as WhyApplication
        setContent {
            val settings by app.settings.collectAsState()
            val ready by app.isReady.collectAsState()
            val storageError by app.storageError.collectAsState()
            val connected by WhyService.connected.collectAsState()
            val scope = rememberCoroutineScope()
            val snackbar = remember { SnackbarHostState() }
            var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
            var loading by remember { mutableStateOf(true) }
            var selectedOnly by rememberSaveable { mutableStateOf(false) }
            var search by remember { mutableStateOf("") }
            var warning by remember(settings.warningSeconds) { mutableStateOf(settings.warningSeconds.toString()) }
            var confirmClear by remember { mutableStateOf(false) }

            fun save(action: suspend () -> Unit) {
                scope.launch {
                    try { action() } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        snackbar.showSnackbar(getString(R.string.error_save))
                    }
                }
            }

            LaunchedEffect(resumeCount) {
                loading = true
                try { apps = withContext(Dispatchers.IO) { installedApps() } }
                catch (error: Exception) {
                    if (error is CancellationException) throw error
                    snackbar.showSnackbar(getString(R.string.error_app_list))
                } finally { loading = false }
            }

            WhyTheme {
                Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.settings_intro))
                        }
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(stringResource(R.string.service_title), style = MaterialTheme.typography.titleMedium)
                                    Text(when {
                                        connected -> getString(R.string.service_running)
                                        serviceEnabled -> getString(R.string.service_disconnected)
                                        else -> getString(R.string.service_disabled)
                                    })
                                    Text(stringResource(R.string.service_explanation))
                                    Button(onClick = {
                                        try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                                        catch (_: Exception) { scope.launch { snackbar.showSnackbar(getString(R.string.error_service_settings)) } }
                                    }) { Text(stringResource(R.string.open_service_settings)) }
                                    Text(stringResource(R.string.restricted_settings_help), style = MaterialTheme.typography.bodySmall)
                                    Text(stringResource(R.string.battery_help), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        item {
                            storageError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                            if (!ready && storageError == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(stringResource(R.string.warning_title), style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = warning, onValueChange = { warning = it },
                                    label = { Text(stringResource(R.string.seconds)) }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                )
                                Button(enabled = ready && warning.toIntOrNull()?.let { it >= 0 } == true,
                                    onClick = { save { app.store.setWarningSeconds(warning.toInt()) } }) { Text(stringResource(R.string.save)) }
                            }
                            Text(stringResource(R.string.warning_help), style = MaterialTheme.typography.bodySmall)
                        }
                        item { OutlinedButton(enabled = ready, onClick = { confirmClear = true }) { Text(stringResource(R.string.clear_history)) } }
                        item {
                            Text(stringResource(R.string.selected_apps, settings.selectedPackages.size), style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = search, onValueChange = { search = it }, label = { Text(stringResource(R.string.search_apps)) },
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                            )
                            FilterChip(
                                selected = selectedOnly, onClick = { selectedOnly = !selectedOnly },
                                label = { Text(stringResource(R.string.only_selected_apps)) },
                            )
                            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        val visibleApps = apps.filter { (!selectedOnly || it.packageName in settings.selectedPackages) &&
                            (it.label.contains(search.trim(), ignoreCase = true) ||
                                it.packageName.contains(search.trim(), ignoreCase = true)) }
                        if (!loading && visibleApps.isEmpty()) item { Text(stringResource(R.string.no_apps)) }
                        items(visibleApps, key = { it.packageName }) { installed ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Image(installed.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(installed.label)
                                    Text(installed.packageName, style = MaterialTheme.typography.bodySmall)
                                }
                                Checkbox(
                                    checked = installed.packageName in settings.selectedPackages, enabled = ready,
                                    modifier = Modifier.semantics { contentDescription = installed.label },
                                    onCheckedChange = { enabled -> save { app.store.selectPackage(installed.packageName, enabled) } },
                                )
                            }
                        }
                    }
                    if (confirmClear) AlertDialog(
                        onDismissRequest = { confirmClear = false }, title = { Text(stringResource(R.string.clear_history_title)) },
                        text = { Text(stringResource(R.string.clear_history_message)) },
                        confirmButton = { TextButton(onClick = {
                            confirmClear = false
                            save { app.database.dao().clearHistory(); snackbar.showSnackbar(getString(R.string.history_cleared)) }
                        }) { Text(stringResource(R.string.clear)) } },
                        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
                    )
                }
            }
        }
    }

    private fun installedApps(): List<InstalledApp> {
        val filter = createPackageFilter()
        val iconSize = (40 * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        return packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0,
        ).distinctBy { it.activityInfo.packageName }
            .filter { filter.canTrack(it.activityInfo.packageName) }
            .map { info ->
                val drawable = info.loadIcon(packageManager)
                val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                drawable.setBounds(0, 0, iconSize, iconSize)
                drawable.draw(Canvas(bitmap))
                InstalledApp(info.activityInfo.packageName, info.loadLabel(packageManager).toString(), bitmap.asImageBitmap())
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
}
