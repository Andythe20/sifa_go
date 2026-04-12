package com.example.sifa_go.data.model

// Lo que enviamos a la api de auth service
data class LoginRequest(
    val username: String, // el email irá aquí
    val password: String
)

// Lo que recibimos de la api de auth service
data class LoginResponse(
    val message: String,
    val token: String,
    val username: String
)