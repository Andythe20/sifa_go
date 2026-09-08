package com.sifa.sifa_go.core.security

/**
 * Cifrador falso para pruebas unitarias JVM.
 * El Android Keystore real no se puede ejercitar fuera de un device/emulador,
 * por lo que las pruebas de la lógica de repositorios usan esta implementación
 * reversible (no criptográfica).
 */
class FakeCipher : SecureCipher {

    /** Si es `true`, [decrypt] devuelve null simulando clave invalidada/corrupta. */
    var failDecrypt: Boolean = false

    override fun encrypt(alias: String, plainText: String): String =
        "enc:|$alias|$plainText"

    override fun decrypt(alias: String, cipherText: String): String? {
        if (failDecrypt) return null
        val prefix = "enc:|$alias|"
        return if (cipherText.startsWith(prefix)) {
            cipherText.removePrefix(prefix)
        } else {
            null
        }
    }
}