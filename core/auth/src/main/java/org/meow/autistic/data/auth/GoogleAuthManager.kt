package org.meow.autistic.data.auth

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.drive.DriveScopes
import com.google.api.services.tasks.TasksScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manages Google OAuth sign-in, access-token retrieval, and sign-out via the Credential Manager API.
 * Also signs users into Firebase Auth using their Google credential so that
 * Firestore Security Rules can reference request.auth.uid.
 *
 * Requested scopes:
 * - [TasksScopes.TASKS] — read + write access to Google Tasks
 * - [CalendarScopes.CALENDAR] — read/write access to Google Calendar
 * - [DriveScopes.DRIVE_FILE] — read/write access to files created by this app
 */
class GoogleAuthManager(
    private val context: Context,
    private val tokenStore: TokenStore,
    private val firebaseWebClientId: String = "",
    private val authScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val credentialManager: CredentialManager = CredentialManager.create(context),
    private val androidAccountOf: (String) -> Account = { email -> Account(email, "com.google") },
    private val googleIdTokenOf: (android.os.Bundle) -> GoogleIdTokenCredential = GoogleIdTokenCredential::createFrom,
) {

    companion object {
        private const val TAG = "GoogleAuthManager"
        private val SCOPES = listOf(TasksScopes.TASKS, CalendarScopes.CALENDAR, DriveScopes.DRIVE_FILE)
    }

    /** Returns true if a Google account is stored and a Firebase session is active. */
    fun isAuthenticated(): Boolean =
        tokenStore.getAccountEmail() != null && FirebaseAuth.getInstance().currentUser != null

    /**
     * Launches the Credential Manager sign-in flow and signs the user into Firebase Auth.
     * Returns true on success, false if the user cancelled.
     *
     * @throws IllegalStateException if [firebaseWebClientId] is not configured, or Firebase returns no uid.
     * @throws RuntimeException on sign-in failure.
     */
    suspend fun signIn(activity: Activity): Boolean {
        if (firebaseWebClientId.isEmpty()) {
            throw IllegalStateException(
                "Firebase web client ID is not configured. " +
                "Enable Google Sign-In in Firebase Console and add firebase.web.client.id to local.properties."
            )
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(firebaseWebClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        return try {
            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                throw IllegalStateException("Unexpected credential type: ${credential.type}")
            }
            val googleIdToken = googleIdTokenOf(credential.data)
            tokenStore.saveAccount(googleIdToken.id)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
            val authResult = FirebaseAuth.getInstance()
                .signInWithCredential(firebaseCredential)
                .await()
            if (authResult.user?.uid.isNullOrBlank()) {
                throw IllegalStateException("Firebase sign-in returned no user/uid")
            }
            true
        } catch (e: GetCredentialCancellationException) {
            false
        } catch (e: GetCredentialException) {
            throw RuntimeException("Sign-in failed: ${e.message}", e)
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
        val email = tokenStore.getAccountEmail()
            ?: throw IllegalStateException("Not authenticated — no signed-in account")
        val scope = "oauth2:${SCOPES.joinToString(" ")}"
        val token = GoogleAuthUtil.getToken(context, androidAccountOf(email), scope)
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
     * Returns the Firebase UID of the currently signed-in user.
     *
     * @throws IllegalStateException if not signed into Firebase Auth.
     */
    fun getFirebaseUid(): String =
        FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Not signed in to Firebase Auth")

    /**
     * Revokes the current session and clears all stored tokens and account data.
     */
    suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }.onFailure { Log.w(TAG, "Clear credential state failed", it) }
        FirebaseAuth.getInstance().signOut()
        tokenStore.clear()
    }
}
