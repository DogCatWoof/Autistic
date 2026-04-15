package org.meow.autistic.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Unit tests for [BackupEncryptor] using an in-memory key (no Android Keystore required). */
class BackupEncryptorTest {

    private fun makeKey(): SecretKey =
        KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private val encryptor = BackupEncryptor(makeKey())

    @Test
    fun `encrypt then decrypt returns original plaintext`() {
        val plaintext = "Hello, backup!".toByteArray()
        val decrypted = encryptor.decrypt(encryptor.encrypt(plaintext))
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt produces output longer than input by at least IV plus tag`() {
        val plaintext = ByteArray(64) { it.toByte() }
        val encrypted = encryptor.encrypt(plaintext)
        // 12-byte IV + 16-byte GCM tag = 28 overhead minimum
        assertTrue(encrypted.size >= plaintext.size + 28)
    }

    @Test
    fun `encrypting same plaintext twice produces different ciphertexts`() {
        val plaintext = "same data".toByteArray()
        val first = encryptor.encrypt(plaintext)
        val second = encryptor.encrypt(plaintext)
        assertNotEquals(first.toList(), second.toList())
    }

    @Test(expected = AEADBadTagException::class)
    fun `decrypt with different key throws AEADBadTagException`() {
        val ciphertext = encryptor.encrypt("secret".toByteArray())
        val otherEncryptor = BackupEncryptor(makeKey())
        otherEncryptor.decrypt(ciphertext)
    }

    @Test(expected = AEADBadTagException::class)
    fun `decrypt with tampered ciphertext throws AEADBadTagException`() {
        val ciphertext = encryptor.encrypt("secret".toByteArray()).also { it[20] = it[20].inc() }
        encryptor.decrypt(ciphertext)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decrypt with payload shorter than IV throws IllegalArgumentException`() {
        encryptor.decrypt(ByteArray(4))
    }

    @Test
    fun `roundtrip preserves empty array`() {
        val plaintext = ByteArray(0)
        val decrypted = encryptor.decrypt(encryptor.encrypt(plaintext))
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `roundtrip preserves large payload`() {
        val plaintext = ByteArray(1024 * 1024) { (it % 256).toByte() }
        val decrypted = encryptor.decrypt(encryptor.encrypt(plaintext))
        assertArrayEquals(plaintext, decrypted)
    }
}
