package org.meow.autistic.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
    private lateinit var testScope: CoroutineScope
    private lateinit var manager: GoogleAuthManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        tokenStore = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)
        testScope = CoroutineScope(UnconfinedTestDispatcher())
        mockkStatic(GoogleSignIn::class)
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns firebaseAuth
        manager = GoogleAuthManager(context, tokenStore, "", testScope)
    }

    @After
    fun tearDown() {
        unmockkStatic(GoogleSignIn::class)
        unmockkStatic(FirebaseAuth::class)
    }

    @Test
    fun `isAuthenticated returns false when no signed-in account`() {
        every { GoogleSignIn.getLastSignedInAccount(context) } returns null
        assertFalse(manager.isAuthenticated())
    }

    @Test
    fun `isAuthenticated returns true when Google account has all permissions`() {
        val account = mockk<GoogleSignInAccount>()
        every { GoogleSignIn.getLastSignedInAccount(context) } returns account
        every { GoogleSignIn.hasPermissions(account, any<Scope>(), any<Scope>(), any<Scope>()) } returns true
        assertTrue(manager.isAuthenticated())
    }

    @Test
    fun `isAuthenticated returns false when account lacks permissions`() {
        val account = mockk<GoogleSignInAccount>()
        every { GoogleSignIn.getLastSignedInAccount(context) } returns account
        every { GoogleSignIn.hasPermissions(account, any<Scope>(), any<Scope>(), any<Scope>()) } returns false
        assertFalse(manager.isAuthenticated())
    }

    @Test
    fun `handleSignInResult saves account email and returns true on success`() {
        val account = mockk<GoogleSignInAccount> {
            every { email } returns "user@example.com"
            every { idToken } returns null
        }
        val task = mockk<com.google.android.gms.tasks.Task<GoogleSignInAccount>> {
            every { getResult(ApiException::class.java) } returns account
        }
        every { GoogleSignIn.getSignedInAccountFromIntent(any()) } returns task

        val result = manager.handleSignInResult(mockk<Intent>())

        assertTrue(result)
        verify { tokenStore.saveAccount("user@example.com") }
    }

    @Test
    fun `handleSignInResult fires Firebase sign-in when idToken is present`() {
        val idToken = "test-id-token"
        val account = mockk<GoogleSignInAccount> {
            every { email } returns "user@example.com"
            every { this@mockk.idToken } returns idToken
        }
        val googleTask = mockk<com.google.android.gms.tasks.Task<GoogleSignInAccount>> {
            every { getResult(ApiException::class.java) } returns account
        }
        val credentialTask = mockk<Task<AuthResult>>(relaxed = true) {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { exception } returns null
            every { result } returns mockk(relaxed = true)
        }
        every { GoogleSignIn.getSignedInAccountFromIntent(any()) } returns googleTask
        every { firebaseAuth.signInWithCredential(any()) } returns credentialTask

        manager.handleSignInResult(mockk<Intent>())

        verify { firebaseAuth.signInWithCredential(any()) }
    }

    @Test
    fun `handleSignInResult skips Firebase sign-in when idToken is null`() {
        val account = mockk<GoogleSignInAccount> {
            every { email } returns "user@example.com"
            every { idToken } returns null
        }
        val task = mockk<com.google.android.gms.tasks.Task<GoogleSignInAccount>> {
            every { getResult(ApiException::class.java) } returns account
        }
        every { GoogleSignIn.getSignedInAccountFromIntent(any()) } returns task

        val result = manager.handleSignInResult(mockk<Intent>())

        assertTrue(result)
        verify(exactly = 0) { firebaseAuth.signInWithCredential(any()) }
    }

    @Test(expected = RuntimeException::class)
    fun `handleSignInResult throws on ApiException`() {
        val task = mockk<com.google.android.gms.tasks.Task<GoogleSignInAccount>> {
            every { getResult(ApiException::class.java) } throws ApiException(Status(7))
        }
        every { GoogleSignIn.getSignedInAccountFromIntent(any()) } returns task

        manager.handleSignInResult(mockk<Intent>())
    }

    @Test(expected = IllegalStateException::class)
    fun `handleSignInResult throws when account email is null`() {
        val account = mockk<GoogleSignInAccount> {
            every { email } returns null
            every { idToken } returns null
        }
        val task = mockk<com.google.android.gms.tasks.Task<GoogleSignInAccount>> {
            every { getResult(ApiException::class.java) } returns account
        }
        every { GoogleSignIn.getSignedInAccountFromIntent(any()) } returns task
        manager.handleSignInResult(mockk<Intent>())
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
        every { GoogleSignIn.getLastSignedInAccount(context) } returns null
        manager.getValidToken()
    }

    @Test
    fun `getValidToken throws when account has no Android account`() = runTest {
        mockkStatic(GoogleAuthUtil::class)
        val signInAccount = mockk<GoogleSignInAccount> { every { account } returns null }
        every { tokenStore.isTokenValid() } returns false
        every { GoogleSignIn.getLastSignedInAccount(context) } returns signInAccount
        var threw = false
        try {
            manager.getValidToken()
        } catch (e: IllegalStateException) {
            threw = true
        } finally {
            unmockkStatic(GoogleAuthUtil::class)
        }
        assertTrue(threw)
    }

    @Test
    fun `getValidToken propagates IOException from GoogleAuthUtil`() = runTest {
        mockkStatic(GoogleAuthUtil::class)
        val androidAccount = mockk<android.accounts.Account>()
        val signInAccount = mockk<GoogleSignInAccount> { every { account } returns androidAccount }
        every { tokenStore.isTokenValid() } returns false
        every { GoogleSignIn.getLastSignedInAccount(context) } returns signInAccount
        every { GoogleAuthUtil.getToken(context, androidAccount, any<String>()) } throws IOException("Network failure")
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
        val androidAccount = mockk<android.accounts.Account>()
        val signInAccount = mockk<GoogleSignInAccount> {
            every { account } returns androidAccount
        }
        every { tokenStore.isTokenValid() } returns false
        every { GoogleSignIn.getLastSignedInAccount(context) } returns signInAccount
        every { GoogleAuthUtil.getToken(context, androidAccount, any<String>()) } returns "fresh_token"

        val token = manager.getValidToken()

        assertEquals("fresh_token", token)
        verify { tokenStore.saveAccessToken("fresh_token", any()) }
        unmockkStatic(GoogleAuthUtil::class)
    }

    @Test
    fun `getFirebaseUid returns uid when Firebase user is present`() {
        val mockUser = mockk<FirebaseUser> { every { uid } returns "firebase-uid-123" }
        every { firebaseAuth.currentUser } returns mockUser
        manager = GoogleAuthManager(context, tokenStore, "", testScope)
        assertEquals("firebase-uid-123", manager.getFirebaseUid())
    }

    @Test(expected = IllegalStateException::class)
    fun `getFirebaseUid throws when not signed in to Firebase`() {
        every { firebaseAuth.currentUser } returns null
        manager = GoogleAuthManager(context, tokenStore, "", testScope)
        manager.getFirebaseUid()
    }

    @Test(expected = IllegalStateException::class)
    fun `signOut clears cached Firebase user`() = runTest {
        val mockUser = mockk<FirebaseUser> { every { uid } returns "firebase-uid-123" }
        var currentUserValue: FirebaseUser? = mockUser
        every { firebaseAuth.currentUser } answers { currentUserValue }
        every { firebaseAuth.signOut() } answers { currentUserValue = null }
        val mockSignInClient = mockk<GoogleSignInClient>(relaxed = true)
        val mockTask = mockk<Task<Void>> {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { result } returns null
            every { exception } returns null
            every { isCanceled } returns false
        }
        every { GoogleSignIn.getClient(context, any()) } returns mockSignInClient
        every { mockSignInClient.signOut() } returns mockTask
        manager = GoogleAuthManager(context, tokenStore, "", testScope)
        manager.signOut()
        manager.getFirebaseUid()
    }

    @Test
    fun `signOut calls Firebase signOut and clears token store`() = runTest {
        val mockSignInClient = mockk<GoogleSignInClient>(relaxed = true)
        val mockTask = mockk<Task<Void>> {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { result } returns null
            every { exception } returns null
            every { isCanceled } returns false
        }
        every { GoogleSignIn.getClient(context, any()) } returns mockSignInClient
        every { mockSignInClient.signOut() } returns mockTask

        manager.signOut()

        verify { firebaseAuth.signOut() }
        verify { tokenStore.clear() }
    }
}
