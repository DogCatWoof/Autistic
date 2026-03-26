package org.meow.autistic.data.auth

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenStoreTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var store: TokenStore

    @Before
    fun setUp() {
        editor = mockk(relaxed = true) {
            every { putString(any(), any()) } returns this
            every { putLong(any(), any()) } returns this
            every { clear() } returns this
        }
        prefs = mockk {
            every { edit() } returns editor
        }
        store = TokenStore(prefs)
    }

    @Test
    fun `saveAccount stores email`() {
        store.saveAccount("user@example.com")
        verify { editor.putString("account_email", "user@example.com") }
        verify { editor.apply() }
    }

    @Test
    fun `getAccountEmail returns stored value`() {
        every { prefs.getString("account_email", null) } returns "user@example.com"
        assertEquals("user@example.com", store.getAccountEmail())
    }

    @Test
    fun `getAccountEmail returns null when not set`() {
        every { prefs.getString("account_email", null) } returns null
        assertNull(store.getAccountEmail())
    }

    @Test
    fun `saveAccessToken stores token and expiry`() {
        store.saveAccessToken("tok", 9999L)
        verify { editor.putString("access_token", "tok") }
        verify { editor.putLong("token_expiry_ms", 9999L) }
        verify { editor.apply() }
    }

    @Test
    fun `isTokenValid returns false when no token`() {
        every { prefs.getString("access_token", null) } returns null
        assertFalse(store.isTokenValid())
    }

    @Test
    fun `isTokenValid returns false when token is expired`() {
        every { prefs.getString("access_token", null) } returns "tok"
        every { prefs.getLong("token_expiry_ms", 0L) } returns System.currentTimeMillis() - 1_000
        assertFalse(store.isTokenValid())
    }

    @Test
    fun `isTokenValid returns false when token expires within buffer`() {
        every { prefs.getString("access_token", null) } returns "tok"
        every { prefs.getLong("token_expiry_ms", 0L) } returns System.currentTimeMillis() + 30_000
        assertFalse(store.isTokenValid())
    }

    @Test
    fun `isTokenValid returns true when token is fresh`() {
        every { prefs.getString("access_token", null) } returns "tok"
        every { prefs.getLong("token_expiry_ms", 0L) } returns System.currentTimeMillis() + 3_600_000
        assertTrue(store.isTokenValid())
    }

    @Test
    fun `clear calls prefs clear and apply`() {
        store.clear()
        verify { editor.clear() }
        verify { editor.apply() }
    }
}
