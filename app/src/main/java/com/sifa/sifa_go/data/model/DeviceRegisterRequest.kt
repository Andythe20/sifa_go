package com.sifa.sifa_go.data.model

data class DeviceRegisterRequest(
    val token: String,
    val platform: String,
    val appVersion: String? = null,
    val deviceId: String? = null,
    val deviceModel: String? = null,
    val manufacturer: String? = null
)
