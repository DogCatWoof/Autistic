package org.meow.autistic.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Encrypted storage for OAuth tokens and account identity.
 *
 * Uses [EncryptedSharedPreferences] in production. Accepts a raw [SharedPreferences]
 * for testability — use [TokenStore.create] to build the production instance.
 */
class TokenStore(private val prefs: SharedPreferences) {

    companion object {
        private const val TAG = "TokenStore"
        private const val FILE_NAME = "auth_token_store"
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_TOKEN_EXPIRY_MS = "token_expiry_ms"

        /** 1-minute buffer — refresh before the token actually expires. */
        private const val REFRESH_BUFFER_MS = 60_000L

        /** Creates the production [TokenStore] backed by [EncryptedSharedPreferences]. */
        fun create(context: Context): TokenStore {
            return try {
                createEncryptedPrefs(context)
            } catch (e: Exception) {
                // AEADBadTagException / VERIFICATION_FAILED means the Tink keyset in the prefs
                // was encrypted with a different or now-invalid KeyStore key (e.g. app reinstall
                // with data retained, signing key change, or biometric enrollment change).
                // Delete the prefs file from disk and rebuild everything from scratch.
                Log.e(TAG, "EncryptedSharedPreferences corrupted — wiping and retrying", e)
                deletePrefsFile(context)
                try {
                    createEncryptedPrefs(context)
                } catch (e2: Exception) {
                    Log.e(TAG, "EncryptedSharedPreferences still failing after wipe — falling back to plaintext", e2)
                    TokenStore(context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE))
                }
            }
        }

        private fun deletePrefsFile(context: Context) {
            runCatching {
                // Delete the XML on disk so Android has no cached bad state.
                File(context.applicationInfo.dataDir, "shared_prefs/$FILE_NAME.xml").delete()
                // Also clear the in-memory SharedPreferences cache.
                context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit(commit = true) { clear() }
            }
        }

        private fun createEncryptedPrefs(context: Context): TokenStore {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            return TokenStore(encryptedPrefs)
        }
    }

    fun saveAccount(email: String) {
        prefs.edit { putString(KEY_ACCOUNT_EMAIL, email) }
    }

    fun getAccountEmail(): String? = try {
        prefs.getString(KEY_ACCOUNT_EMAIL, null)
    } catch (e: Exception) {
        Log.e(TAG, "Error reading account email", e)
        null
    }

    fun saveAccessToken(token: String, expiryMs: Long) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, token)
            putLong(KEY_TOKEN_EXPIRY_MS, expiryMs)
        }
    }

    fun getAccessToken(): String? = try {
        prefs.getString(KEY_ACCESS_TOKEN, null)
    } catch (e: Exception) {
        Log.e(TAG, "Error reading access token", e)
        null
    }

    fun getExpiryMs(): Long = try {
        prefs.getLong(KEY_TOKEN_EXPIRY_MS, 0L)
    } catch (e: Exception) {
        Log.e(TAG, "Error reading expiry", e)
        0L
    }

    /** Returns true if a cached token exists and won't expire within [REFRESH_BUFFER_MS]. */
    fun isTokenValid(): Boolean {
        val token = getAccessToken() ?: return false
        return token.isNotEmpty() && getExpiryMs() > System.currentTimeMillis() + REFRESH_BUFFER_MS
    }

    /** Clears only the access token and its expiry, leaving the account email intact. */
    fun clearAccessToken() {
        prefs.edit {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_TOKEN_EXPIRY_MS)
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }
}
