package com.sifa.sifa_go.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordValidatorTest {

    @Test
    fun `validate returns 4 requirements`() {
        val result = PasswordValidator.validate("Abcdef1g")
        assertEquals(4, result.size)
    }

    @Test
    fun `validate detects short password`() {
        val result = PasswordValidator.validate("Ab1")
        assertFalse(result[0].isValid)
        assertEquals("Al menos 8 caracteres", result[0].label)
    }

    @Test
    fun `validate accepts password with exactly 8 characters`() {
        val result = PasswordValidator.validate("Abcd1234")
        assertTrue(result[0].isValid)
    }

    @Test
    fun `validate detects missing uppercase`() {
        val result = PasswordValidator.validate("abcdef1g")
        assertFalse(result[1].isValid)
        assertEquals("Al menos una mayúscula", result[1].label)
    }

    @Test
    fun `validate accepts password with uppercase`() {
        val result = PasswordValidator.validate("Abcdef1g")
        assertTrue(result[1].isValid)
    }

    @Test
    fun `validate detects missing lowercase`() {
        val result = PasswordValidator.validate("ABCDEF1G")
        assertFalse(result[2].isValid)
        assertEquals("Al menos una minúscula", result[2].label)
    }

    @Test
    fun `validate accepts password with lowercase`() {
        val result = PasswordValidator.validate("Abcdef1g")
        assertTrue(result[2].isValid)
    }

    @Test
    fun `validate detects missing digit`() {
        val result = PasswordValidator.validate("Abcdefgh")
        assertFalse(result[3].isValid)
        assertEquals("Al menos un número", result[3].label)
    }

    @Test
    fun `validate accepts password with digit`() {
        val result = PasswordValidator.validate("Abcdef1g")
        assertTrue(result[3].isValid)
    }

    @Test
    fun `validate fails for empty password`() {
        val result = PasswordValidator.validate("")
        assertFalse(result[0].isValid)
        assertFalse(result[1].isValid)
        assertFalse(result[2].isValid)
        assertFalse(result[3].isValid)
    }

    @Test
    fun `validate fails for password with only numbers`() {
        val result = PasswordValidator.validate("12345678")
        assertTrue(result[0].isValid)
        assertFalse(result[1].isValid)
        assertFalse(result[2].isValid)
        assertTrue(result[3].isValid)
    }

    @Test
    fun `validate accepts valid password`() {
        val result = PasswordValidator.validate("Secure1Pass")
        assertTrue(result.all { it.isValid })
    }

    @Test
    fun `isFullyValid returns true for valid password`() {
        assertTrue(PasswordValidator.isFullyValid("Secure1Pass"))
    }

    @Test
    fun `isFullyValid returns false for short password`() {
        assertFalse(PasswordValidator.isFullyValid("Ab1"))
    }

    @Test
    fun `isFullyValid returns false for missing uppercase`() {
        assertFalse(PasswordValidator.isFullyValid("abcdef1gh"))
    }

    @Test
    fun `isFullyValid returns false for missing digit`() {
        assertFalse(PasswordValidator.isFullyValid("Abcdefghi"))
    }

    @Test
    fun `isFullyValid returns false for empty password`() {
        assertFalse(PasswordValidator.isFullyValid(""))
    }

    @Test
    fun `isFullyValid returns false for missing lowercase`() {
        assertFalse(PasswordValidator.isFullyValid("ABCDEF1GH"))
    }

    @Test
    fun `password with special characters passes validation if other rules met`() {
        assertTrue(PasswordValidator.isFullyValid("Secure!@#1"))
    }

    @Test
    fun `password with only whitespace fails`() {
        val result = PasswordValidator.validate("        ")
        assertTrue(result[0].isValid)
        assertFalse(result[1].isValid)
        assertFalse(result[2].isValid)
        assertFalse(result[3].isValid)
        assertFalse(PasswordValidator.isFullyValid("        "))
    }
}
