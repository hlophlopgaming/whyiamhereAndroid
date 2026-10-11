package dev.cena.whyiamhere

import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager

class PackageFilter(
    private val ownPackage: String,
    private val keyboards: Set<String>,
    val launchers: Set<String>,
) {
    fun isTransient(packageName: String): Boolean = packageName in keyboards || packageName in setOf(
        "com.android.systemui", "com.android.permissioncontroller",
        "com.google.android.permissioncontroller", "com.android.packageinstaller",
        "com.google.android.packageinstaller",
    )

    fun canTrack(packageName: String): Boolean =
        packageName != ownPackage && packageName !in launchers && !isTransient(packageName)
}

fun Context.createPackageFilter(): PackageFilter = PackageFilter(
    ownPackage = packageName,
    keyboards = getSystemService(InputMethodManager::class.java)?.inputMethodList.orEmpty().map { it.packageName }.toSet(),
    launchers = packageManager.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0,
    ).map { it.activityInfo.packageName }.toSet(),
)
