package dev.cena.whyiamhere

import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import dev.cena.whyiamhere.data.HistoryItem
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test

class GateTest {
    @Test fun fullHistoryStillAllowsTypingNewGoal() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as WhyApplication
        app.ready.await()
        val packageName = app.packageName
        val previouslySelected = packageName in app.settings.value.selectedPackages
        val marker = "gate-test-${System.nanoTime()}"
        var activity: android.app.Activity? = null
        try {
            app.store.selectPackage(packageName, true)
            withTimeout(5_000) { app.settings.first { packageName in it.selectedPackages } }
            repeat(12) { index ->
                app.database.dao().addHistory(HistoryItem(
                    packageName = packageName, goal = "$marker-$index", durationMin = 15,
                    startedAt = System.currentTimeMillis() + index,
                ))
            }
            activity = instrumentation.startActivitySync(
                Intent(app, GateActivity::class.java).putExtra(GateActivity.EXTRA_PACKAGE, packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            val automation = instrumentation.uiAutomation
            val deadline = android.os.SystemClock.uptimeMillis() + 10_000
            var input: AccessibilityNodeInfo? = null
            while (input == null && android.os.SystemClock.uptimeMillis() < deadline) {
                instrumentation.waitForIdleSync()
                val root = automation.rootInActiveWindow
                if (root != null && root.findAccessibilityNodeInfosByText(app.getString(R.string.app_name)).size >= 2) {
                    input = findEditable(root)
                }
                if (input == null) android.os.SystemClock.sleep(100)
            }
            val field = requireNotNull(input) { "Goal input is unavailable with full history" }
            assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "My new goal")
            }))
            instrumentation.waitForIdleSync()
            assertTrue(requireNotNull(automation.rootInActiveWindow)
                .findAccessibilityNodeInfosByText("My new goal").isNotEmpty())
        } finally {
            activity?.let { instrumentation.runOnMainSync { it.finish() } }
            app.database.openHelper.writableDatabase.execSQL(
                "DELETE FROM history WHERE packageName = ? AND goal LIKE ?", arrayOf(packageName, "$marker%"),
            )
            app.store.selectPackage(packageName, previouslySelected)
        }
    }

    private fun findEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        repeat(node.childCount) { index ->
            node.getChild(index)?.let { child -> findEditable(child)?.let { return it } }
        }
        return null
    }
}
