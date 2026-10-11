package dev.cena.whyiamhere.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.map

val Context.settingsStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val selectedPackages: Set<String> = emptySet(),
    val warningSeconds: Int = 60,
    val widgetX: Int = 24,
    val widgetY: Int = 160,
    val collapsed: Boolean = false,
)

class SettingsStore(private val context: Context) {
    private val selected = stringSetPreferencesKey("selected_packages")
    private val warning = intPreferencesKey("warning_seconds")
    private val x = intPreferencesKey("widget_x")
    private val y = intPreferencesKey("widget_y")
    private val collapsed = booleanPreferencesKey("widget_collapsed")

    val settings = context.settingsStore.data.map { values ->
        AppSettings(
            selectedPackages = values[selected] ?: emptySet(),
            warningSeconds = (values[warning] ?: 60).coerceAtLeast(0),
            widgetX = values[x] ?: 24,
            widgetY = values[y] ?: 160,
            collapsed = values[collapsed] ?: false,
        )
    }

    suspend fun selectPackage(packageName: String, enabled: Boolean) {
        require(packageName.isNotBlank())
        context.settingsStore.edit { values ->
            val packages = values[selected] ?: emptySet()
            values[selected] = if (enabled) packages + packageName else packages - packageName
        }
    }

    suspend fun setWarningSeconds(seconds: Int) {
        require(seconds >= 0)
        context.settingsStore.edit { it[warning] = seconds }
    }

    suspend fun saveWidgetPosition(newX: Int, newY: Int) {
        context.settingsStore.edit { it[x] = newX; it[y] = newY }
    }

    suspend fun setCollapsed(value: Boolean) {
        context.settingsStore.edit { it[collapsed] = value }
    }
}
