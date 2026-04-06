package org.meow.autistic.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Encrypted storage for OAuth tokens and account identity.
 *
 * Uses [EncryptedSharedPreferences] in production. Accepts a raw [SharedPreferences]
 * for testability — use [TokenStore.create] to build the production instance.
 */
class TokenStore(private val prefs: SharedPreferences) {

    companion object {
        private const val FILE_NAME = "auth_token_store"
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_TOKEN_EXPIRY_MS = "token_expiry_ms"

        /** 1-minute buffer — refresh before the token actually expires. */
        private const val REFRESH_BUFFER_MS = 60_000L

        /** Creates the production [TokenStore] backed by [EncryptedSharedPreferences]. */
        fun create(context: Context): TokenStore {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val encryptedPrefs = EncryptedSharedPreferences.create(
                FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            return TokenStore(encryptedPrefs)
        }
    }

    fun saveAccount(email: String) {
        prefs.edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
    }

    fun getAccountEmail(): String? = prefs.getString(KEY_ACCOUNT_EMAIL, null)

    fun saveAccessToken(token: String, expiryMs: Long) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putLong(KEY_TOKEN_EXPIRY_MS, expiryMs)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getExpiryMs(): Long = prefs.getLong(KEY_TOKEN_EXPIRY_MS, 0L)

    /** Returns true if a cached token exists and won't expire within [REFRESH_BUFFER_MS]. */
    fun isTokenValid(): Boolean {
        val token = getAccessToken() ?: return false
        return token.isNotEmpty() && getExpiryMs() > System.currentTimeMillis() + REFRESH_BUFFER_MS
    }

    /** Clears only the access token and its expiry, leaving the account email intact. */
    fun clearAccessToken() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_TOKEN_EXPIRY_MS).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
