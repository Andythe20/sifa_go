package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    private companion object {
        private const val AUTH_BASE_URL = "http://44.196.188.33"
        private const val CONTENT_TYPE_JSON = "application/json"

        private val refreshRetrofit: AuthApiService by lazy {
            Retrofit.Builder()
                .baseUrl(AUTH_BASE_URL)
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
            return chain.proceed(originalRequest)
        }

        val authRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        val response = chain.proceed(authRequest)

        if (response.code == 401) {
            response.close()

            synchronized(this) {
                val currentToken = sessionManager.getToken()
                if (currentToken != accessToken) {
                    val retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                    return chain.proceed(retryRequest)
                }

                when (val result = tryRefresh()) {
                    is RefreshResult.Success -> {
                        sessionManager.saveSession(
                            token = result.accessToken,
                            refreshToken = result.refreshToken,
                            username = result.username,
                            roles = result.roles,
                            expiry = result.expiry
                        )
                        val newToken = sessionManager.getToken()
                        val retryRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                        return chain.proceed(retryRequest)
                    }
                    is RefreshResult.Expired -> {
                        sessionManager.logout()
                        return createUnauthorizedResponse(originalRequest)
                    }
                    is RefreshResult.NetworkError -> {
                        return createUnauthorizedResponse(originalRequest)
                    }
                }
            }
        }

        return response
    }

    private sealed class RefreshResult {
        data class Success(
            val accessToken: String,
            val refreshToken: String,
            val username: String,
            val roles: List<String>,
            val expiry: Long
        ) : RefreshResult()

        data object Expired : RefreshResult()
        data object NetworkError : RefreshResult()
    }

    private fun tryRefresh(): RefreshResult {
        val refreshToken = sessionManager.getRefreshToken() ?: return RefreshResult.Expired

        return try {
            val response = runBlocking {
                refreshRetrofit.refresh(RefreshTokenRequest(refreshToken))
            }
            if (response.isSuccessful) {
                val body = response.body() ?: return RefreshResult.Expired
                RefreshResult.Success(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    username = body.sub,
                    roles = body.roles,
                    expiry = body.exp
                )
            } else {
                RefreshResult.Expired
            }
        } catch (_: Exception) {
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
