package dev.cena.whyiamhere

import org.junit.Assert.*
import org.junit.Test

class PackageFilterTest {
    private val filter = PackageFilter("why.app", setOf("keyboard.app"), setOf("home.app"))

    @Test fun `keyboards and system dialogs are transient`() {
        listOf("keyboard.app", "com.android.systemui", "com.google.android.permissioncontroller").forEach {
            assertTrue(filter.isTransient(it))
            assertFalse(filter.canTrack(it))
        }
    }

    @Test fun `home and own app cannot be selected but are not transient`() {
        listOf("why.app", "home.app").forEach {
            assertFalse(filter.canTrack(it))
            assertFalse(filter.isTransient(it))
        }
    }

    @Test fun `ordinary apps including vendors are accepted`() {
        assertTrue(filter.canTrack("com.example.app"))
        assertTrue(filter.canTrack("com.samsung.android.video"))
    }
}
