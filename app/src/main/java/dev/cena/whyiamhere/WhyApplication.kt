package dev.cena.whyiamhere

import android.app.Application
import androidx.room.Room
import dev.cena.whyiamhere.data.WhyDatabase

class WhyApplication : Application() {
    val database: WhyDatabase by lazy {
        Room.databaseBuilder(this, WhyDatabase::class.java, "why.db").build()
    }
}
