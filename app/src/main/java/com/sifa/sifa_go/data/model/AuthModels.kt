package com.sifa.sifa_go.data.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val sub: String,
    val iat: Long,
    val exp: Long,
    val roles: List<String>
)

sealed class LoginResult {
    data class Success(val token: String, val email: String, val roles: List<String>) : LoginResult()
    data class Error(val code: Int, val message: String) : LoginResult()
}