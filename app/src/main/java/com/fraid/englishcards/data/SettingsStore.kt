package com.fraid.englishcards.data

import android.content.Context

/** AI settings kept in the app's private storage on the phone (never in the repository). */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var model: String
        get() = prefs.getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() } ?: AiClient.DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL, value.trim()).apply()

    private companion object {
        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
    }
}
