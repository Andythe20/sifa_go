package com.sifa.sifa_go.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

data class AppVersionInfo(
    val versionName: String,
    val versionCode: Long,
    val buildType: String
)

data class DeviceInfo(
    val deviceId: String,
    val deviceModel: String,
    val manufacturer: String
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

@SuppressLint("HardwareIds")
fun getDeviceInfo(context: Context): DeviceInfo {
    val deviceId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: "DESCONOCIDO"
    return DeviceInfo(
        deviceId = deviceId,
        deviceModel = Build.MODEL ?: "Desconocido",
        manufacturer = Build.BRAND ?: "Desconocida"
    )
}
