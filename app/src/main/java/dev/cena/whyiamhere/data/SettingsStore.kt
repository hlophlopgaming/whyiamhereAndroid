package dev.cena.whyiamhere.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsStore by preferencesDataStore(name = "settings")
