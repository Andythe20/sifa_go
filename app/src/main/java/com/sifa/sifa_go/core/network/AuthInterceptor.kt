package com.sifa.sifa_go.core.network

import android.util.Log
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    private companion object {
        private const val TAG = "AuthInterceptor"
        private const val CONTENT_TYPE_JSON = "application/json"
        private const val EXPIRY_MARGIN_MS = 30_000L

        private val refreshRetrofit: AuthApiService by lazy {
            val client = OkHttpClient.Builder()
                .connectTimeout(ServerConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(ServerConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(ServerConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
            Retrofit.Builder()
                .baseUrl(ServerConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApiService::class.java)
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        if (path.contains("/auth/api/v1/login") || path.contains("/auth/api/v1/refresh")) {
            return chain.proceed(originalRequest)
        }

        val accessToken = sessionManager.getToken()
        if (accessToken == null) {
            Log.d(TAG, "No token found, proceeding without auth")
            return chain.proceed(originalRequest)
        }

        val expiry = sessionManager.getTokenExpiry()
        if (expiry > 0 && System.currentTimeMillis() >= (expiry * 1000) - EXPIRY_MARGIN_MS) {
            Log.d(TAG, "Token expired or about to expire, proactively refreshing")
            if (tryRefreshSession()) {
                val newToken = sessionManager.getToken()
                if (newToken != null) {
                    val authRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                    return chain.proceed(authRequest)
                }
            }
        }

        val authRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        val response = chain.proceed(authRequest)

        if (response.code == 401 || response.code == 403) {
            response.close()
            Log.d(TAG, "Received ${response.code}, attempting token refresh")

            synchronized(this) {
                val currentToken = sessionManager.getToken()
                if (currentToken != accessToken) {
                    Log.d(TAG, "Token already refreshed by another thread, retrying")
                    val retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                    return chain.proceed(retryRequest)
                }

                when (val result = tryRefresh()) {
                    is RefreshResult.Success -> {
                        Log.d(TAG, "Token refreshed successfully")
                        saveRefreshedSession(result)
                        val newToken = sessionManager.getToken()
                        val retryRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                        return chain.proceed(retryRequest)
                    }
                    is RefreshResult.Expired -> {
                        Log.d(TAG, "Refresh token expired, logging out")
                        sessionManager.logout()
                        SessionManager.notifySessionExpired()
                        return createUnauthorizedResponse(originalRequest)
                    }
                    is RefreshResult.NetworkError -> {
                        Log.d(TAG, "Network error during refresh, returning original 401")
                        return createUnauthorizedResponse(originalRequest)
                    }
                }
            }
        }

        return response
    }

    private fun saveRefreshedSession(result: RefreshResult.Success) {
        sessionManager.saveSession(
            token = result.accessToken,
            refreshToken = result.refreshToken,
            username = result.username,
            roles = result.roles,
            expiry = result.expiry,
            iat = result.iat
        )
    }

    private fun tryRefreshSession(): Boolean {
        val refreshToken = sessionManager.getRefreshToken() ?: return false
        return try {
            val response = runBlocking {
                refreshRetrofit.refresh(RefreshTokenRequest(refreshToken))
            }
            if (response.isSuccessful) {
                val body = response.body() ?: return false
                saveRefreshedSession(
                    RefreshResult.Success(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        username = body.sub,
                        roles = body.roles,
                        expiry = body.exp,
                        iat = body.iat
                    )
                )
                true
            } else {
                Log.w(TAG, "Proactive refresh failed: HTTP ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Proactive refresh threw exception", e)
            false
        }
    }

        private sealed class RefreshResult {
        data class Success(
            val accessToken: String,
            val refreshToken: String,
            val username: String,
            val roles: List<String>,
            val expiry: Long,
            val iat: Long
        ) : RefreshResult()

        data object Expired : RefreshResult()
        data object NetworkError : RefreshResult()
    }

    private fun tryRefresh(): RefreshResult {
        val refreshToken = sessionManager.getRefreshToken()
        if (refreshToken == null) {
            Log.w(TAG, "No refresh token available")
            return RefreshResult.Expired
        }

        return try {
            Log.d(TAG, "Calling refresh endpoint")
            val response = runBlocking {
                refreshRetrofit.refresh(RefreshTokenRequest(refreshToken))
            }

            when (response.code()) {
                in 200..299 -> {
                    val body = response.body()
                    if (body != null) {
                        Log.d(TAG, "Refresh successful")
                        RefreshResult.Success(
                            accessToken = body.accessToken,
                            refreshToken = body.refreshToken,
                            username = body.sub,
                            roles = body.roles,
                            expiry = body.exp,
                            iat = body.iat
                        )
                    } else {
                        Log.w(TAG, "Refresh returned 2xx but body is null")
                        RefreshResult.Expired
                    }
                }
                400, 409 -> {
                    Log.w(TAG, "Refresh rejected: HTTP ${response.code()} (invalid/expired token)")
                    RefreshResult.Expired
                }
                in 500..599 -> {
                    Log.e(TAG, "Refresh failed: HTTP ${response.code()} (server error)")
                    RefreshResult.NetworkError
                }
                else -> {
                    Log.w(TAG, "Refresh failed with unexpected code: ${response.code()}")
                    RefreshResult.NetworkError
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refresh threw exception: ${e.javaClass.simpleName}: ${e.message}")
            RefreshResult.NetworkError
        }
    }

    private fun createUnauthorizedResponse(request: Request): Response {
        return Response.Builder()
            .code(401)
            .message("Unauthorized")
            .body(
                "{\"error\":\"Sesión expirada\"}"
                    .toResponseBody(CONTENT_TYPE_JSON.toMediaType())
            )
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .build()
    }
}
