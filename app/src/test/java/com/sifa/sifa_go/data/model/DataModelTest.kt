package com.sifa.sifa_go.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModelTest {

    @Test
    fun `UserResponse with all fields`() {
        val user = UserResponse(
            rut = "12345678",
            dv = "9",
            name = "Juan",
            lastName = "Pérez",
            birthDate = "1990-01-01",
            email = "juan@test.com",
            phone = "+56912345678",
            role = "USER_APP",
            createdAt = "2024-01-01T00:00:00",
            modifiedAt = "2024-06-01T00:00:00",
            active = true
        )

        assertEquals("12345678", user.rut)
        assertEquals("9", user.dv)
        assertEquals("Juan", user.name)
        assertEquals("Pérez", user.lastName)
        assertEquals("1990-01-01", user.birthDate)
        assertEquals("juan@test.com", user.email)
        assertEquals("+56912345678", user.phone)
        assertEquals("USER_APP", user.role)
        assertEquals("2024-01-01T00:00:00", user.createdAt)
        assertEquals("2024-06-01T00:00:00", user.modifiedAt)
        assertTrue(user.active)
    }

    @Test
    fun `UserResponse with nullable fields null`() {
        val user = UserResponse(
            rut = null,
            dv = null,
            name = null,
            lastName = null,
            birthDate = null,
            email = null,
            phone = null,
            role = null,
            createdAt = null,
            modifiedAt = null,
            active = false
        )

        assertNull(user.rut)
        assertNull(user.dv)
        assertNull(user.name)
        assertNull(user.lastName)
        assertNull(user.birthDate)
        assertNull(user.email)
        assertNull(user.phone)
        assertNull(user.role)
        assertNull(user.createdAt)
        assertNull(user.modifiedAt)
        assertFalse(user.active)
    }

    @Test
    fun `UserResponse copy and modify`() {
        val original = UserResponse(
            rut = "11111111", dv = "1", name = "Ana",
            lastName = "López", birthDate = null, email = "ana@test.com",
            phone = null, role = null, createdAt = null, modifiedAt = null,
            active = true
        )
        val modified = original.copy(phone = "+56999999999", email = "ana.nueva@test.com")

        assertEquals("11111111", modified.rut)
        assertEquals("ana.nueva@test.com", modified.email)
        assertEquals("+56999999999", modified.phone)
        assertTrue(modified.active)
    }

    @Test
    fun `LoginRequest construction`() {
        val request = LoginRequest(email = "test@test.com", password = "mypassword")
        assertEquals("test@test.com", request.email)
        assertEquals("mypassword", request.password)
    }

    @Test
    fun `LoginResponse all fields`() {
        val response = LoginResponse(
            accessToken = "token123",
            refreshToken = "refresh123",
            tokenType = "Bearer",
            sub = "test@test.com",
            iat = 1000L,
            exp = 2000L,
            roles = listOf("USER_APP")
        )

        assertEquals("token123", response.accessToken)
        assertEquals("refresh123", response.refreshToken)
        assertEquals("Bearer", response.tokenType)
        assertEquals("test@test.com", response.sub)
        assertEquals(1000L, response.iat)
        assertEquals(2000L, response.exp)
        assertEquals(listOf("USER_APP"), response.roles)
    }

    @Test
    fun `LoginResponse toString does not expose sensitive data`() {
        val response = LoginResponse(
            accessToken = "secret",
            refreshToken = "refresh-secret",
            tokenType = "Bearer",
            sub = "user@test.com",
            iat = 1000L,
            exp = 2000L,
            roles = listOf("USER_APP")
        )
        val str = response.toString()
        assertTrue(str.contains("accessToken=secret"))
        assertTrue(str.contains("refreshToken=refresh-secret"))
    }

    @Test
    fun `ChangePasswordRequest construction`() {
        val request = ChangePasswordRequest(oldPassword = "old123", newPassword = "NewSecure1Pass")
        assertEquals("old123", request.oldPassword)
        assertEquals("NewSecure1Pass", request.newPassword)
    }

    @Test
    fun `ChangePasswordResponse construction`() {
        val response = ChangePasswordResponse(message = "Password changed")
        assertEquals("Password changed", response.message)
    }

    @Test
    fun `LoginResult Success construction`() {
        val result: LoginResult = LoginResult.Success(token = "tok", email = "a@b.com", roles = listOf("ADMIN"))
        assertTrue(result is LoginResult.Success)
        assertEquals("a@b.com", (result as LoginResult.Success).email)
    }

    @Test
    fun `LoginResult Error construction`() {
        val result: LoginResult = LoginResult.Error(code = 401, message = "Unauthorized")
        assertTrue(result is LoginResult.Error)
        assertEquals(401, (result as LoginResult.Error).code)
        assertEquals("Unauthorized", (result as LoginResult.Error).message)
    }
}
