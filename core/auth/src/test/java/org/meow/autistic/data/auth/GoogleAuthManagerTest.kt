package org.meow.autistic.data.auth

import android.accounts.Account
import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoogleAuthManagerTest {

    private lateinit var context: Context
    private lateinit var tokenStore: TokenStore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var testScope: CoroutineScope
    private lateinit var manager: GoogleAuthManager

    private val mockAccount = mockk<Account>()
    private val mockGoogleIdCred = mockk<GoogleIdTokenCredential> {
        every { id } returns "user@example.com"
        every { idToken } returns "test-id-token"
    }

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        tokenStore = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)
        credentialManager = mockk(relaxed = true)
        testScope = CoroutineScope(UnconfinedTestDispatcher())
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns firebaseAuth
        manager = GoogleAuthManager(
            context, tokenStore, "web-client-id", testScope, credentialManager,
            androidAccountOf = { mockAccount },
            googleIdTokenOf = { mockGoogleIdCred },
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
    }

    @Test
    fun `isAuthenticated returns false when no stored account`() {
        every { tokenStore.getAccountEmail() } returns null
        assertFalse(manager.isAuthenticated())
    }

    @Test
    fun `isAuthenticated returns false when no Firebase user`() {
        every { tokenStore.getAccountEmail() } returns "user@example.com"
        every { firebaseAuth.currentUser } returns null
        assertFalse(manager.isAuthenticated())
    }

    @Test
    fun `isAuthenticated returns true when account stored and Firebase signed in`() {
        every { tokenStore.getAccountEmail() } returns "user@example.com"
        every { firebaseAuth.currentUser } returns mockk()
        assertTrue(manager.isAuthenticated())
    }

    @Test
    fun `signIn saves account email and signs into Firebase on success`() = runTest {
        val activity = mockk<Activity>()
        val credential = mockk<CustomCredential>(relaxed = true) {
            every { type } returns GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        }
        val response = mockk<GetCredentialResponse> {
            every { this@mockk.credential } returns credential
        }
        val mockFirebaseUser = mockk<FirebaseUser> { every { uid } returns "firebase-uid-123" }
        val mockAuthResult = mockk<AuthResult> { every { user } returns mockFirebaseUser }
        val authTask = mockk<Task<AuthResult>>(relaxed = true) {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { exception } returns null
            every { result } returns mockAuthResult
        }
        every { firebaseAuth.signInWithCredential(any()) } returns authTask
        coEvery { credentialManager.getCredential(activity, any<GetCredentialRequest>()) } returns response

        val result = manager.signIn(activity)

        assertTrue(result)
        verify { tokenStore.saveAccount("user@example.com") }
        verify { firebaseAuth.signInWithCredential(any()) }
    }

    @Test
    fun `signIn returns false when user cancels`() = runTest {
        val activity = mockk<Activity>()
        coEvery { credentialManager.getCredential(activity, any<GetCredentialRequest>()) } throws
            GetCredentialCancellationException()

        assertFalse(manager.signIn(activity))
    }

    @Test(expected = IllegalStateException::class)
    fun `signIn throws when firebaseWebClientId is empty`() = runTest {
        val emptyClientManager = GoogleAuthManager(
            context, tokenStore, "", testScope, credentialManager,
            androidAccountOf = { mockAccount },
            googleIdTokenOf = { mockGoogleIdCred },
        )
        emptyClientManager.signIn(mockk())
    }

    @Test(expected = RuntimeException::class)
    fun `signIn throws on credential failure`() = runTest {
        val activity = mockk<Activity>()
        coEvery { credentialManager.getCredential(activity, any<GetCredentialRequest>()) } throws
            GetCredentialInterruptedException("test failure")

        manager.signIn(activity)
    }

    @Test
    fun `getValidToken returns cached token when still valid`() = runTest {
        every { tokenStore.isTokenValid() } returns true
        every { tokenStore.getAccessToken() } returns "cached_token"

        assertEquals("cached_token", manager.getValidToken())
    }

    @Test(expected = IllegalStateException::class)
    fun `getValidToken throws when not authenticated`() = runTest {
        every { tokenStore.isTokenValid() } returns false
        every { tokenStore.getAccountEmail() } returns null
        manager.getValidToken()
    }

    @Test
    fun `getValidToken propagates IOException from GoogleAuthUtil`() = runTest {
        mockkStatic(GoogleAuthUtil::class)
        every { tokenStore.isTokenValid() } returns false
        every { tokenStore.getAccountEmail() } returns "user@example.com"
        every { GoogleAuthUtil.getToken(context, mockAccount, any<String>()) } throws IOException("Network failure")
        var threw = false
        try {
            manager.getValidToken()
        } catch (e: IOException) {
            threw = true
        } finally {
            unmockkStatic(GoogleAuthUtil::class)
        }
        assertTrue(threw)
    }

    @Test
    fun `getValidToken fetches fresh token and caches it when expired`() = runTest {
        mockkStatic(GoogleAuthUtil::class)
        every { tokenStore.isTokenValid() } returns false
        every { tokenStore.getAccountEmail() } returns "user@example.com"
        every { GoogleAuthUtil.getToken(context, mockAccount, any<String>()) } returns "fresh_token"

        val token = manager.getValidToken()

        assertEquals("fresh_token", token)
        verify { tokenStore.saveAccessToken("fresh_token", any()) }
        unmockkStatic(GoogleAuthUtil::class)
    }

    @Test
    fun `getFirebaseUid returns uid when Firebase user is present`() {
        val mockUser = mockk<FirebaseUser> { every { uid } returns "firebase-uid-123" }
        every { firebaseAuth.currentUser } returns mockUser
        manager = GoogleAuthManager(
            context, tokenStore, "", testScope, credentialManager,
            androidAccountOf = { mockAccount },
            googleIdTokenOf = { mockGoogleIdCred },
        )
        assertEquals("firebase-uid-123", manager.getFirebaseUid())
    }

    @Test(expected = IllegalStateException::class)
    fun `getFirebaseUid throws when not signed in to Firebase`() {
        every { firebaseAuth.currentUser } returns null
        manager = GoogleAuthManager(
            context, tokenStore, "", testScope, credentialManager,
            androidAccountOf = { mockAccount },
            googleIdTokenOf = { mockGoogleIdCred },
        )
        manager.getFirebaseUid()
    }

    @Test(expected = IllegalStateException::class)
    fun `signOut clears Firebase session`() = runTest {
        val mockUser = mockk<FirebaseUser> { every { uid } returns "firebase-uid-123" }
        var currentUserValue: FirebaseUser? = mockUser
        every { firebaseAuth.currentUser } answers { currentUserValue }
        every { firebaseAuth.signOut() } answers { currentUserValue = null }
        coEvery { credentialManager.clearCredentialState(any<ClearCredentialStateRequest>()) } returns Unit
        manager = GoogleAuthManager(
            context, tokenStore, "", testScope, credentialManager,
            androidAccountOf = { mockAccount },
            googleIdTokenOf = { mockGoogleIdCred },
        )
        manager.signOut()
        manager.getFirebaseUid()
    }

    @Test
    fun `signOut calls Firebase signOut and clears token store`() = runTest {
        coEvery { credentialManager.clearCredentialState(any<ClearCredentialStateRequest>()) } returns Unit

        manager.signOut()

        verify { firebaseAuth.signOut() }
        verify { tokenStore.clear() }
    }
}
