package org.meow.autistic.data.auth

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
import com.google.api.services.tasks.TasksScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

    /**
     * Manages Google Sign-In via Firebase Auth and provides OAuth access tokens for Google APIs.
     *
     * Sign-in flow:
     * 1. User signs in with Google via Credential Manager
     * 2. Google ID token is exchanged for a Firebase Auth session
     * 3. Access tokens for Google APIs (Tasks, Calendar) are retrieved via [getValidToken]
     *
     * @param context Android context for Credential Manager and token retrieval.
     * @param tokenStore Encrypted storage for OAuth tokens.
     * @param firebaseWebClientId Web client ID from Firebase Console for Google Sign-In.
     */
    class GoogleAuthManager(
        private val context: Context,
        private val tokenStore: TokenStore,
        private val firebaseWebClientId: String = "",
        private val credentialManager: CredentialManager = CredentialManager.create(context),
        internal val googleIdTokenOf: (android.os.Bundle) -> GoogleIdTokenCredential = GoogleIdTokenCredential::createFrom,
    ) {
        companion object {
            private const val TAG = "GoogleAuthManager"
            private val SCOPES = listOf(TasksScopes.TASKS, CalendarScopes.CALENDAR)
        }

    /** Returns true if a user is signed into Firebase Auth and has a stored account. */
    fun isAuthenticated(): Boolean =
        tokenStore.getAccountEmail() != null && FirebaseAuth.getInstance().currentUser != null

    /**
     * Launches the Credential Manager sign-in flow and signs the user into Firebase Auth.
     *
     * @return true if sign-in succeeded, false if the user cancelled.
     * @throws IllegalStateException if [firebaseWebClientId] is not configured or Firebase returns no uid.
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
     * Returns a valid OAuth access token for Google APIs.
     * Uses the cached token if valid; otherwise fetches a fresh one.
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
        val token = GoogleAuthUtil.getToken(context, android.accounts.Account(email, "com.google"), scope)
        tokenStore.saveAccessToken(token, System.currentTimeMillis() + 3_600_000L)
        token
    }

    /** Invalidates the cached access token so the next call to [getValidToken] fetches a fresh one. */
    fun invalidateTokenCache() {
        tokenStore.clearAccessToken()
    }

    /** Returns the Firebase UID of the currently signed-in user. */
    fun getFirebaseUid(): String =
        FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Not signed in to Firebase Auth")

    /**
     * Signs out the user, clears credentials, and removes all stored tokens.
     */
    suspend fun signOut() {
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }.onFailure { Log.w(TAG, "Clear credential state failed", it) }
        FirebaseAuth.getInstance().signOut()
        tokenStore.clear()
    }
}
