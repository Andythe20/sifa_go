package com.sifa.sifa_go.core.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

/**
 * Migración one-time de los tokens almacenados en texto plano (SharedPreferences
 * legacy) hacia el almacenamiento cifrado vía [SecureCipher].
 *
 * Objetivos de diseño:
 *  - **Idempotente**: si el proceso muere entre medio, en el próximo arranque se
 *    reintenta y ningún dato se duplica ni corrompe (la flag se escribe al final).
 *  - **No destructivo bajo fallo**: el archivo legacy en texto plano solo se elimina
 *    después de haber cifrado TODO y persistido de forma síncrona (commit()).
 *  - Después de la migración, el archivo legacy plano ya no existe en disco, por lo
 *    que se elimina el riesgo de exfiltración vía backup.
 */
object StorageMigration {

    /** Flag que marca la migración como completada (se guarda en el prefs destino). */
    const val MIGRATED_FLAG = "MIGRATED_V1"

    /**
     * Cifra todos los pares clave/valor del [legacyPrefsName] hacia [targetPrefs].
     *
     * Los valores se normalizan a String cifrado (los tipos originales —Long, Int,
     * Set<String>— quedan codificados como texto): la capa de lectura de cada
     * repositorio conoce el formato y lo reconvierte al tipo final.
     *
     * @param context            Contexto para acceder y eliminar el archivo legacy.
     * @param legacyPrefsName    Nombre del archivo SharedPreferences en texto plano.
     * @param targetPrefs        SharedPreferences destino donde se guarda el ciphertext.
     * @param cipher             Implementación de cifrado a utilizar.
     * @param keyAlias           Alias de la clave maestro del Keystore.
     */
    // ApplySharedPref: commit() es intencional (migración síncrona e idempotente).
    // UseKtx: no usamos el KTX edit porque necesitamos capturar el resultado de commit().
    @SuppressLint("ApplySharedPref", "UseKtx")
    fun migrateLegacyToEncrypted(
        context: Context,
        legacyPrefsName: String,
        targetPrefs: SharedPreferences,
        cipher: SecureCipher,
        keyAlias: String,
    ) {
        val alreadyMigrated = targetPrefs.getBoolean(MIGRATED_FLAG, false)
        if (alreadyMigrated) return

        val legacyPrefs = context.getSharedPreferences(legacyPrefsName, Context.MODE_PRIVATE)
        val legacyValues = legacyPrefs.all

        if (legacyValues.isEmpty()) {
            // No había sesión legacy: solo marcamos como migrado para no repetir
            // el escaneo en cada arranque y limpiamos un archivo vacío si existía.
            // commit() es intencional: la flag debe quedar persistida de inmediato
            // para que la migración sea idempotente ante crashes.
            targetPrefs.edit().putBoolean(MIGRATED_FLAG, true).commit()
            context.deleteSharedPreferences(legacyPrefsName)
            return
        }

        // commit() (en vez de apply()) es DELIBERADO: si el proceso muere antes de
        // persistir, la flag no quedaría escrita y la migración se reintentaría de
        // forma segura; si morimos después, ya no se vuelve a tocar el legacy.
        val editor = targetPrefs.edit()
        for ((key, value) in legacyValues) {
            // Normalizamos cualquier tipo del archivo legacy a texto antes de cifrar.
            val plainText = when (value) {
                is String -> value
                is Long -> value.toString()
                is Int -> value.toString()
                is Float -> value.toString()
                is Boolean -> value.toString()
                is Set<*> -> value.joinToString(SEPARATOR)
                else -> continue
            }
            editor.putString(key, cipher.encrypt(keyAlias, plainText))
        }
        // La flag se escribe SOLO después de haber cifrado todos los valores.
        editor.putBoolean(MIGRATED_FLAG, true)
        val committed = editor.commit()

        if (committed) {
            // Último paso: eliminar el archivo legacy en texto plano.
            context.deleteSharedPreferences(legacyPrefsName)
        }
    }

    private const val SEPARATOR = "\n"
}