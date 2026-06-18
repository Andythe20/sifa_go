package com.sifa.sifa_go.core.preferences

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val PREF_NAME = "sifa_app_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val DEFAULT_URL = "http://32.197.72.219"

    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var baseUrl: String
        get() = preferences.getString(KEY_BASE_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(value) = preferences.edit().putString(KEY_BASE_URL, value).apply()
}
