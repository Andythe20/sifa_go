package com.sifa.sifa_go.core.utils

import android.content.Context
import android.content.pm.PackageManager

data class AppVersionInfo(
    val versionName: String,
    val versionCode: Long,
    val buildType: String
)

fun getAppVersionInfo(context: Context): AppVersionInfo {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            0
        )
        AppVersionInfo(
            versionName = packageInfo.versionName ?: "N/A",
            versionCode = packageInfo.longVersionCode,
            buildType = if (packageInfo.versionName?.contains("SNAPSHOT") == true) "DEBUG" else "RELEASE"
        )
    } catch (e: PackageManager.NameNotFoundException) {
        AppVersionInfo("N/A", 0, "UNKNOWN")
    }
}
