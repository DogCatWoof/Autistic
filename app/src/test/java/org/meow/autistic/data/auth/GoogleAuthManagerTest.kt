package org.meow.autistic.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.io.IOException
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
    private lateinit var manager: GoogleAuthManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        tokenStore = mockk(relaxed = true)
        mockkStatic(GoogleSignIn::class)
        manager = GoogleAuthManager(context, tokenStore)
    }

    @After
    fun tearDown() {
        unmockkStatic(GoogleSignIn::class)
    }

    @Test
    fun `isAuthenticated returns false when no signed-in account`() {
        every { GoogleSignIn.getLastSignedInAccount(context) } returns null
        assertFalse(manager.isAuthenticated())
    }

    @Test
    fun `isAuthenticated returns true when account has all permissions`() {
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
    fun `handleSignInResult returns false on ApiException`() {
        val task = mockk<com.google.android.gms.tasks.Task<GoogleSignInAccount>> {
            every { getResult(ApiException::class.java) } throws ApiException(Status(7))
        }
        every { GoogleSignIn.getSignedInAccountFromIntent(any()) } returns task

        assertFalse(manager.handleSignInResult(mockk<Intent>()))
    }

    @Test
    fun `getValidToken returns cached token when still valid`() = runTest {
        every { tokenStore.isTokenValid() } returns true
        every { tokenStore.getAccessToken() } returns "cached_token"

        assertEquals("cached_token", manager.getValidToken())
    }

    @Test
    fun `handleSignInResult returns false when account email is null`() {
        val account = mockk<GoogleSignInAccount> { every { email } returns null }
        val task = mockk<com.google.android.gms.tasks.Task<GoogleSignInAccount>> {
            every { getResult(ApiException::class.java) } returns account
        }
        every { GoogleSignIn.getSignedInAccountFromIntent(any()) } returns task
        assertFalse(manager.handleSignInResult(mockk<Intent>()))
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
}
