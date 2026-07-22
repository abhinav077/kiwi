package com.abhinavsirohi.kiwi.data.local

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.kiwiSettingsDataStore by preferencesDataStore(name = "kiwi_settings")

enum class KiwiThemePreference { SYSTEM, LIGHT, DARK, SUNRISE, GROVE }

enum class KiwiFontPreference { SYSTEM, SOFT, EDITORIAL }

data class KiwiSettings(
    val theme: KiwiThemePreference = KiwiThemePreference.SYSTEM,
    val font: KiwiFontPreference = KiwiFontPreference.SYSTEM,
    val hideNotificationContent: Boolean = true,
    val protectRecentScreen: Boolean = true,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
)

class KiwiSettingsStore(private val context: Context) {
    val settings: Flow<KiwiSettings> = context.kiwiSettingsDataStore.data.map(Preferences::toKiwiSettings)

    suspend fun setTheme(value: KiwiThemePreference) = update(THEME, value.name)

    suspend fun setFont(value: KiwiFontPreference) = update(FONT, value.name)

    suspend fun setHideNotificationContent(value: Boolean) {
        context.kiwiSettingsDataStore.edit { it[HIDE_NOTIFICATION_CONTENT] = value }
        notificationPrivacyPreferences(context).edit().putBoolean(HIDE_NOTIFICATION_CONTENT.name, value).apply()
    }

    suspend fun setProtectRecentScreen(value: Boolean) = update(PROTECT_RECENT_SCREEN, value)

    suspend fun setBiometricEnabled(value: Boolean) = update(BIOMETRIC_ENABLED, value)

    suspend fun configurePin(pin: CharArray) {
        require(isValidPin(pin)) { "PIN must contain 4 to 8 digits" }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val hash = derivePinHash(pin, salt)
        pin.fill('\u0000')
        context.kiwiSettingsDataStore.edit {
            it[PIN_SALT] = salt.encode()
            it[PIN_HASH] = hash.encode()
            it[APP_LOCK_ENABLED] = true
        }
    }

    suspend fun verifyPin(pin: CharArray): Boolean {
        if (!isValidPin(pin)) {
            pin.fill('\u0000')
            return false
        }
        val preferences = context.kiwiSettingsDataStore.data.first()
        val encodedSalt = preferences[PIN_SALT]
        val encodedHash = preferences[PIN_HASH]
        val salt = encodedSalt?.decode() ?: return false.also { pin.fill('\u0000') }
        val expected = encodedHash?.decode() ?: return false.also { pin.fill('\u0000') }
        val actual = derivePinHash(pin, salt)
        pin.fill('\u0000')
        return MessageDigest.isEqual(expected, actual)
    }

    suspend fun disableAppLock() {
        context.kiwiSettingsDataStore.edit {
            it.remove(PIN_SALT)
            it.remove(PIN_HASH)
            it[APP_LOCK_ENABLED] = false
            it[BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun clearAll() {
        context.kiwiSettingsDataStore.edit { it.clear() }
        notificationPrivacyPreferences(context).edit().clear().apply()
    }

    private suspend fun update(key: Preferences.Key<String>, value: String) {
        context.kiwiSettingsDataStore.edit { it[key] = value }
    }

    private suspend fun update(key: Preferences.Key<Boolean>, value: Boolean) {
        context.kiwiSettingsDataStore.edit { it[key] = value }
    }

    internal companion object {
        val THEME = stringPreferencesKey("theme")
        val FONT = stringPreferencesKey("font")
        val HIDE_NOTIFICATION_CONTENT = booleanPreferencesKey("hide_notification_content")
        val PROTECT_RECENT_SCREEN = booleanPreferencesKey("protect_recent_screen")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        const val SALT_BYTES = 16
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
    }
}

fun notificationContentIsHidden(context: Context): Boolean =
    notificationPrivacyPreferences(context).getBoolean("hide_notification_content", true)

internal fun isValidPin(pin: CharArray): Boolean = pin.size in 4..8 && pin.all(Char::isDigit)

private fun derivePinHash(pin: CharArray, salt: ByteArray): ByteArray =
    PBEKeySpec(pin, salt, KiwiSettingsStore.ITERATIONS, KiwiSettingsStore.KEY_BITS).let { spec ->
        try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.NO_WRAP)

private fun String.decode(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

private fun Preferences.toKiwiSettings() = KiwiSettings(
    theme = enumValueOrDefault(this[KiwiSettingsStore.THEME], KiwiThemePreference.SYSTEM),
    font = enumValueOrDefault(this[KiwiSettingsStore.FONT], KiwiFontPreference.SYSTEM),
    hideNotificationContent = this[KiwiSettingsStore.HIDE_NOTIFICATION_CONTENT] ?: true,
    protectRecentScreen = this[KiwiSettingsStore.PROTECT_RECENT_SCREEN] ?: true,
    appLockEnabled = this[KiwiSettingsStore.APP_LOCK_ENABLED] ?: false,
    biometricEnabled = this[KiwiSettingsStore.BIOMETRIC_ENABLED] ?: false,
)

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

private fun notificationPrivacyPreferences(context: Context) =
    context.getSharedPreferences("kiwi_notification_privacy", Context.MODE_PRIVATE)
