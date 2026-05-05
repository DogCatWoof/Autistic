package org.meow.autistic.data.backup

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEY_ALIAS = "org.meow.autistic.backup_key"
private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val CIPHER_TRANSFORM = "AES/GCM/NoPadding"
private const val GCM_IV_BYTES = 12
private const val GCM_TAG_BITS = 128

/**
 * Encrypts and decrypts backup payloads using AES-256-GCM.
 *
 * The key is stored in Android Keystore and persists across reinstalls on the same device.
 * Wire format: 12-byte IV || GCM ciphertext+tag.
 *
 * @param key Injectable for testing (use [create] in production).
 */
class BackupEncryptor(private val key: SecretKey) {

    companion object {
        /** Creates an encryptor backed by Android Keystore. */
        fun create(): BackupEncryptor = BackupEncryptor(getOrCreateKey())

        private fun getOrCreateKey(): SecretKey {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                .apply {
                    init(
                        KeyGenParameterSpec.Builder(
                            KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(256)
                            .build()
                    )
                }
                .generateKey()
        }
    }

    /** Encrypts [plaintext] and returns the IV-prefixed ciphertext. */
    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(plaintext)
        return cipher.iv + ciphertext
    }

    /**
     * Decrypts data produced by [encrypt].
     * @throws javax.crypto.AEADBadTagException if data is corrupt or from a different key.
     */
    fun decrypt(data: ByteArray): ByteArray {
        require(data.size > GCM_IV_BYTES) { "Payload too short to contain IV" }
        val iv = data.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = data.copyOfRange(GCM_IV_BYTES, data.size)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }
}
