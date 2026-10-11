package dev.cena.whyiamhere

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocaleTest {
    private fun context(language: String): Context {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(app.resources.configuration)
        configuration.setLocales(LocaleList(Locale.forLanguageTag(language)))
        return app.createConfigurationContext(configuration)
    }

    @Test fun selectsSystemLanguageAndFallsBackToEnglish() {
        assertEquals("Why am I here?", context("en-US").getString(R.string.app_name))
        assertEquals("Зачем я здесь?", context("ru-RU").getString(R.string.app_name))
        assertEquals("¿Por qué estoy aquí?", context("es-ES").getString(R.string.app_name))
        assertEquals("¿Por qué estoy aquí?", context("es-MX").getString(R.string.app_name))
        assertEquals("Why am I here?", context("fr-FR").getString(R.string.app_name))
    }

    @Test fun formatsTranslatedLabelsAndWidgetDescription() {
        assertEquals("Выбранные приложения: 2", context("ru").getString(R.string.selected_apps, 2))
        assertEquals("15 min", context("es").getString(R.string.duration_minutes, 15))
        assertEquals("Read. Time left: 5:00. Tap to expand.",
            context("en").getString(R.string.widget_description_collapsed, "Read", "5:00"))
    }
}
