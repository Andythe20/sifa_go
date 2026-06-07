package com.sifa.sifa_go.domain.push.usecase

import com.sifa.sifa_go.core.network.DeviceApiService
import com.sifa.sifa_go.data.model.DeviceRegisterRequest
import com.sifa.sifa_go.domain.push.PushTokenRepository
import com.sifa.sifa_go.exception.NetworkErrorHandler

class RegisterDeviceUseCase(
    private val tokenRepository: PushTokenRepository,
    private val deviceApi: DeviceApiService
) {
    suspend operator fun invoke(): Result<Unit> {
        val pushToken = tokenRepository.getToken()
            ?: return Result.failure(IllegalStateException("FCM token not available yet"))

        return try {
            val response = deviceApi.registerDevice(
                DeviceRegisterRequest(
                    token = pushToken.value,
                    platform = "ANDROID"
                )
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(NetworkErrorHandler.getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.getExceptionMessage(e).let { Exception(it) })
        }
    }
}
