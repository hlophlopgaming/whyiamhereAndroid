package dev.cena.whyiamhere

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build

fun Context.appLanguageContext(): Context {
    if (Build.VERSION.SDK_INT < 33) return this
    val manager = getSystemService(LocaleManager::class.java) ?: return this
    val locales = manager.applicationLocales.takeUnless { it.isEmpty } ?: manager.systemLocales
    if (resources.configuration.locales == locales) return this
    // Accessibility service contexts can retain the system language after an app-language change.
    val configuration = Configuration(resources.configuration).apply { setLocales(locales) }
    return createConfigurationContext(configuration)
}
