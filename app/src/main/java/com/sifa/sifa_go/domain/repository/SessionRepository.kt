package com.sifa.sifa_go.domain.repository

interface SessionRepository {
    fun saveSession(
        token: String,
        refreshToken: String,
        username: String,
        roles: List<String>,
        expiry: Long? = null,
        iat: Long? = null
    )

    fun getToken(): String?
    fun getRefreshToken(): String?
    fun getTokenExpiry(): Long
    fun getTokenIat(): Long
    fun getUsername(): String?
    fun getRoles(): List<String>
    fun hasUserAppRole(): Boolean
    fun hasValidSession(): Boolean
    fun logout()
}
