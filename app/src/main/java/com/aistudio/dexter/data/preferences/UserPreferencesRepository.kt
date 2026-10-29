package com.aistudio.dexter.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserPreferences(
    val userName: String = "User",
    val avatarEmoji: String = "🤖",
    val languageCode: String = "en",
    val currencyCode: String = "INR",
    val currencySymbol: String = "₹",
    val is24HourFormat: Boolean = false,
    val dateFormatPattern: String = "dd MMM yyyy"
) : Serializable

data class LanguageOption(
    val code: String,
    val englishName: String,
    val nativeName: String,
    val flagEmoji: String
) : Serializable

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val name: String,
    val flagEmoji: String
) : Serializable

object SupportedPreferences {
    val languages = listOf(
        LanguageOption("en", "English", "English", "🇺🇸"),
        LanguageOption("zh-CN", "Chinese (Simplified)", "中文 (简体)", "🇨🇳"),
        LanguageOption("zh-TW", "Chinese (Traditional)", "中文 (繁體)", "🇹🇼"),
        LanguageOption("ru", "Russian", "Русский", "🇷🇺"),
        LanguageOption("hi", "Hindi", "हिन्दी", "🇮🇳"),
        LanguageOption("es", "Spanish", "Español", "🇪🇸"),
        LanguageOption("fr", "French", "Français", "🇫🇷"),
        LanguageOption("de", "German", "Deutsch", "🇩🇪"),
        LanguageOption("ja", "Japanese", "日本語", "🇯🇵"),
        LanguageOption("ko", "Korean", "한국어", "🇰🇷"),
        LanguageOption("ar", "Arabic", "العربية", "🇸🇦"),
        LanguageOption("pt", "Portuguese", "Português", "🇧🇷"),
        LanguageOption("it", "Italian", "Italiano", "🇮🇹"),
        LanguageOption("tr", "Turkish", "Türkçe", "🇹🇷")
    )

    val currencies = listOf(
        CurrencyOption("CNY", "¥", "Chinese Yuan Renminbi (元)", "🇨🇳"),
        CurrencyOption("RUB", "₽", "Russian Ruble (руб)", "🇷🇺"),
        CurrencyOption("INR", "₹", "Indian Rupee (₹)", "🇮🇳"),
        CurrencyOption("USD", "$", "US Dollar ($)", "🇺🇸"),
        CurrencyOption("EUR", "€", "Euro (€)", "🇪🇺"),
        CurrencyOption("GBP", "£", "British Pound (£)", "🇬🇧"),
        CurrencyOption("JPY", "¥", "Japanese Yen (円)", "🇯🇵"),
        CurrencyOption("KRW", "₩", "South Korean Won (₩)", "🇰🇷"),
        CurrencyOption("CAD", "C$", "Canadian Dollar (C$)", "🇨🇦"),
        CurrencyOption("AUD", "A$", "Australian Dollar (A$)", "🇦🇺"),
        CurrencyOption("BRL", "R$", "Brazilian Real (R$)", "🇧🇷"),
        CurrencyOption("TRY", "₺", "Turkish Lira (₺)", "🇹🇷"),
        CurrencyOption("AED", "د.إ", "UAE Dirham (AED)", "🇦🇪"),
        CurrencyOption("CHF", "CHF", "Swiss Franc (CHF)", "🇨🇭"),
        CurrencyOption("SGD", "S$", "Singapore Dollar (S$)", "🇸🇬")
    )

    val datePatterns = listOf(
        "dd MMM yyyy" to "28 Oct 2026 (Standard)",
        "dd/MM/yyyy" to "28/10/2026 (Day First)",
        "MM/dd/yyyy" to "10/28/2026 (Month First)",
        "yyyy-MM-dd" to "2026-10-28 (ISO 8601 / China)",
        "dd.MM.yyyy" to "28.10.2026 (Russian / European)"
    )

    val avatarEmojis = listOf(
        "🤖", "⚡", "🌟", "🛡️", "🚀", "🎯", "🧠", "💼", "🌌", "🦾", "🦊", "👑"
    )

    fun formatCurrency(amount: Double, symbol: String): String {
        return "$symbol" + String.format(Locale.US, "%,.2f", amount)
    }

    fun formatDate(timestampMillis: Long, pattern: String): String {
        return try {
            SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMillis))
        } catch (e: Exception) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestampMillis))
        }
    }

    fun formatTime(timestampMillis: Long, is24Hour: Boolean): String {
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMillis))
    }
}

class UserPreferencesRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _preferences = MutableStateFlow(readPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private fun readPreferences(): UserPreferences {
        return UserPreferences(
            userName = prefs.getString(KEY_USER_NAME, "User") ?: "User",
            avatarEmoji = prefs.getString(KEY_AVATAR, "🤖") ?: "🤖",
            languageCode = prefs.getString(KEY_LANGUAGE, "en") ?: "en",
            currencyCode = prefs.getString(KEY_CURRENCY_CODE, "INR") ?: "INR",
            currencySymbol = prefs.getString(KEY_CURRENCY_SYMBOL, "₹") ?: "₹",
            is24HourFormat = prefs.getBoolean(KEY_IS_24_HOUR, false),
            dateFormatPattern = prefs.getString(KEY_DATE_FORMAT, "dd MMM yyyy") ?: "dd MMM yyyy"
        )
    }

    fun updateUserName(name: String) {
        val clean = name.trim().ifBlank { "User" }
        prefs.edit().putString(KEY_USER_NAME, clean).apply()
        _preferences.value = _preferences.value.copy(userName = clean)
    }

    fun updateAvatar(avatar: String) {
        prefs.edit().putString(KEY_AVATAR, avatar).apply()
        _preferences.value = _preferences.value.copy(avatarEmoji = avatar)
    }

    fun updateLanguage(code: String) {
        prefs.edit().putString(KEY_LANGUAGE, code).apply()
        _preferences.value = _preferences.value.copy(languageCode = code)
    }

    fun updateCurrency(code: String, symbol: String) {
        prefs.edit()
            .putString(KEY_CURRENCY_CODE, code)
            .putString(KEY_CURRENCY_SYMBOL, symbol)
            .apply()
        _preferences.value = _preferences.value.copy(
            currencyCode = code,
            currencySymbol = symbol
        )
    }

    fun updateTimeFormat(is24Hour: Boolean) {
        prefs.edit().putBoolean(KEY_IS_24_HOUR, is24Hour).apply()
        _preferences.value = _preferences.value.copy(is24HourFormat = is24Hour)
    }

    fun updateDateFormat(pattern: String) {
        prefs.edit().putString(KEY_DATE_FORMAT, pattern).apply()
        _preferences.value = _preferences.value.copy(dateFormatPattern = pattern)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _preferences.value = UserPreferences()
    }

    companion object {
        private const val PREFS_NAME = "dexter_user_prefs"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_AVATAR = "user_avatar"
        private const val KEY_LANGUAGE = "language_code"
        private const val KEY_CURRENCY_CODE = "currency_code"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_IS_24_HOUR = "is_24_hour"
        private const val KEY_DATE_FORMAT = "date_format"

        @Volatile
        private var instance: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: UserPreferencesRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
