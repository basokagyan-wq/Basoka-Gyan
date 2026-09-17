package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class ApiKeyPreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("basoka_api_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
    }

    fun getCustomApiKey(): String {
        return prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun clearCustomApiKey() {
        prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
    }

    fun getEffectiveApiKey(): String {
        val custom = getCustomApiKey()
        if (custom.isNotBlank()) {
            return custom
        }
        val defaultKey = BuildConfig.GEMINI_API_KEY
        return if (defaultKey.isNotBlank() && defaultKey != "MY_GEMINI_API_KEY") {
            defaultKey
        } else {
            ""
        }
    }

    fun isUsingCustomKey(): Boolean {
        return getCustomApiKey().isNotBlank()
    }

    fun hasAutomaticKey(): Boolean {
        val defaultKey = BuildConfig.GEMINI_API_KEY
        return defaultKey.isNotBlank() && defaultKey != "MY_GEMINI_API_KEY"
    }
}
