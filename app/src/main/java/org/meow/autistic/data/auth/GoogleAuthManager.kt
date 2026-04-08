package org.meow.autistic.data.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks as GmsTasks
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.drive.DriveScopes
import com.google.api.services.tasks.TasksScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Manages Google OAuth sign-in, access-token retrieval, and sign-out.
 *
 * Requested scopes:
 * - [TasksScopes.TASKS] — read + write access to Google Tasks
 * - [CalendarScopes.CALENDAR] — read/write access to Google Calendar
 * - [DriveScopes.DRIVE_APPDATA] — hidden app-data folder for DB backups
 */
class GoogleAuthManager(
    private val context: Context,
    private val tokenStore: TokenStore,
) {

    companion object {
        private const val TAG = "GoogleAuthManager"
        private const val SIGN_OUT_TIMEOUT_SEC = 10L
        private val SCOPES = listOf(TasksScopes.TASKS, CalendarScopes.CALENDAR, DriveScopes.DRIVE_APPDATA)
    }

    private val signInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(TasksScopes.TASKS), Scope(CalendarScopes.CALENDAR), Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        GoogleSignIn.getClient(context, options)
    }

    /** Returns true if the user is signed in and has granted all required scopes. */
    fun isAuthenticated(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(
            account,
            Scope(TasksScopes.TASKS),
            Scope(CalendarScopes.CALENDAR),
            Scope(DriveScopes.DRIVE_APPDATA),
        )
    }

    /** Returns the [Intent] to launch via an [ActivityResultLauncher] to start sign-in. */
    fun getSignInIntent(): Intent = signInClient.signInIntent

    /**
     * Processes the result delivered back from the sign-in Activity.
     *
     * @return true if the account was saved successfully, false on failure or cancellation.
     */
    fun handleSignInResult(data: Intent?): Boolean {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            tokenStore.saveAccount(account.email ?: return false)
            true
        } catch (e: ApiException) {
            Log.w(TAG, "Sign-in failed with status: ${e.statusCode}")
            false
        }
    }

    /**
     * Returns a valid access token, using the [TokenStore] cache when possible.
     * Fetches a fresh token via [GoogleAuthUtil] on the IO dispatcher when the cached
     * token is missing or near expiry.
     *
     * @throws IllegalStateException if the user is not signed in.
     * @throws java.io.IOException on network failure.
     */
    suspend fun getValidToken(): String = withContext(Dispatchers.IO) {
        if (tokenStore.isTokenValid()) {
            return@withContext tokenStore.getAccessToken()!!
        }
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw IllegalStateException("Not authenticated — no signed-in account")
        val scope = "oauth2:${SCOPES.joinToString(" ")}"
        val androidAccount = account.account
            ?: throw IllegalStateException("Sign-in account has no associated Android account")
        val token = GoogleAuthUtil.getToken(context, androidAccount, scope)
        tokenStore.saveAccessToken(token, System.currentTimeMillis() + 3_600_000L)
        token
    }

    /**
     * Clears the cached access token so the next [getValidToken] call re-fetches a fresh token.
     * Use this when an API call returns 403 due to a scope mismatch on the cached token.
     */
    fun invalidateTokenCache() {
        tokenStore.clearAccessToken()
    }

    /**
     * Revokes the current session and clears all stored tokens and account data.
     */
    suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        runCatching {
            GmsTasks.await(signInClient.signOut(), SIGN_OUT_TIMEOUT_SEC, TimeUnit.SECONDS)
        }.onFailure { Log.w(TAG, "Sign-out task failed", it) }
        tokenStore.clear()
    }
}
