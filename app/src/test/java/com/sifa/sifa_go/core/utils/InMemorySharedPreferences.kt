package com.sifa.sifa_go.core.utils

import android.content.SharedPreferences

/**
 * Implementación en memoria de [SharedPreferences] para pruebas unitarias JVM.
 * Respeta la semántica de commit()/apply() (la persistencia del map se efectúa en
 * ambos casos, como hace SharedPreferences real en Android).
 */
class InMemorySharedPreferences : SharedPreferences {

    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values

    override fun getString(key: String, defValue: String?): String? =
        values[key] as? String ?: defValue

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        values[key] as? MutableSet<String> ?: defValues

    override fun getInt(key: String, defValue: Int): Int =
        values[key] as? Int ?: defValue

    override fun getLong(key: String, defValue: Long): Long =
        values[key] as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float): Float =
        values[key] as? Float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = InMemoryEditor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = Unit

    private inner class InMemoryEditor : SharedPreferences.Editor {

        private val updates = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()

        override fun putString(key: String, value: String?): SharedPreferences.Editor =
            apply { updates[key] = value }

        override fun putStringSet(
            key: String,
            values: MutableSet<String>?
        ): SharedPreferences.Editor = apply { updates[key] = values }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor =
            apply { updates[key] = value }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor =
            apply { updates[key] = value }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor =
            apply { updates[key] = value }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor =
            apply { updates[key] = value }

        override fun remove(key: String): SharedPreferences.Editor =
            apply { removals.add(key) }

        override fun clear(): SharedPreferences.Editor =
            apply {
                updates.clear()
                removals.clear()
                values.keys.toList().forEach { values.remove(it) }
            }

        override fun commit(): Boolean {
            flush()
            return true
        }

        override fun apply() {
            flush()
        }

        private fun flush() {
            removals.forEach { values.remove(it) }
            values.putAll(updates)
        }
    }
}