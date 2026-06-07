package com.sifa.sifa_go.domain.push

import kotlinx.coroutines.flow.Flow

interface PushTokenRepository {
    fun getToken(): PushToken?
    suspend fun saveToken(token: PushToken)
    fun observeToken(): Flow<PushToken?>
}
