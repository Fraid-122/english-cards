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

    /** Calendar day of the last study session (see Streak). */
    var lastStudyDay: Long
        get() = prefs.getLong(KEY_LAST_DAY, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_DAY, value).apply()

    /** Days in a row with study. */
    var streak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_STREAK, value).apply()

    private companion object {
        const val KEY_LAST_DAY = "last_study_day"
        const val KEY_STREAK = "streak"
        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
    }
}
