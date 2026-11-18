package com.aistudio.dexter.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-Backed Security & Encryption Manager for Dexter's 16 SQLite Segments.
 * Manages SQLCipher database passphrases backed by the Android KeyStore (TEE / StrongBox).
 * Guarantees Zero-Cloud Privacy: data is encrypted on disk at rest with AES-256.
 */
class DexterKeyStoreManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "DexterKeyStoreManager"
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "dexter_master_sqlcipher_key"
        private const val PREFS_NAME = "dexter_secure_vault_prefs"
        private const val KEY_ENCRYPTED_PASSPHRASE = "enc_db_passphrase"
        private const val KEY_GCM_IV = "gcm_iv"
        private const val GCM_TAG_LENGTH = 128

        @Volatile
        private var INSTANCE: DexterKeyStoreManager? = null

        fun getInstance(context: Context): DexterKeyStoreManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DexterKeyStoreManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Initializes SQLCipher native JNI libraries.
     */
    fun initializeSqlCipher() {
        try {
            System.loadLibrary("sqlcipher")
            Log.i(TAG, "SQLCipher native cryptographic engine loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Notice loading sqlcipher library: ${e.message}")
        }
    }

    /**
     * Retrieves or creates a hardware-backed 256-bit encryption passphrase for SQLCipher.
     */
    @Synchronized
    fun getOrCreateDatabasePassphrase(): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val ivBase64 = prefs.getString(KEY_GCM_IV, null)

        val masterKey = getOrCreateMasterKey()

        if (encryptedBase64 != null && ivBase64 != null) {
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
                return cipher.doFinal(encrypted)
            } catch (e: Exception) {
                Log.e(TAG, "Error decrypting database passphrase from KeyStore", e)
            }
        }

        // Generate brand new 256-bit cryptographic passphrase
        val rawPassphrase = ByteArray(32)
        SecureRandom().nextBytes(rawPassphrase)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, masterKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(rawPassphrase)

            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(KEY_GCM_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()

            Log.i(TAG, "Generated and protected new 256-bit database passphrase in Android KeyStore.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed storing encrypted passphrase in KeyStore", e)
        }

        return rawPassphrase
    }

    private fun getOrCreateMasterKey(): SecretKey {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        val key = keyGenerator.generateKey()
        Log.i(TAG, "Generated hardware-backed Master Key in Android KeyStore (AES-256 GCM).")
        return key
    }
}
