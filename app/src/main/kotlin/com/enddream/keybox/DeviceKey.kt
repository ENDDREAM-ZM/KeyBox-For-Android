package com.enddream.keybox

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object DeviceKey {
    private const val KS = "AndroidKeyStore"
    private const val ALIAS = "keybox_device_key"
    private const val PREFS = "keybox_dev"
    private const val K_BLOB = "blob"
    private const val K_IV = "iv"

    private var cached: String? = null

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KS)
        ks.load(null)
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KS)
        kg.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return kg.generateKey()
    }

    fun get(c: Context): String {
        cached?.let { return it }
        val sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val blob = sp.getString(K_BLOB, null)
        val iv = sp.getString(K_IV, null)
        if (blob != null && iv != null) {
            try {
                val cip = Cipher.getInstance("AES/GCM/NoPadding")
                cip.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, B64.decode(iv)))
                val s = String(cip.doFinal(B64.decode(blob)), Charsets.UTF_8)
                cached = s
                return s
            } catch (t: Throwable) {
            }
        }
        val raw = ByteArray(32)
        java.security.SecureRandom().nextBytes(raw)
        val s = B64.encode(raw)
        try {
            val cip = Cipher.getInstance("AES/GCM/NoPadding")
            cip.init(Cipher.ENCRYPT_MODE, key())
            val enc = cip.doFinal(s.toByteArray(Charsets.UTF_8))
            sp.edit()
                .putString(K_BLOB, B64.encode(enc))
                .putString(K_IV, B64.encode(cip.iv))
                .apply()
        } catch (t: Throwable) {
        }
        cached = s
        return s
    }

    fun exists(c: Context): Boolean =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(K_BLOB)

    fun reset(c: Context) {
        cached = null
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        try {
            val ks = KeyStore.getInstance(KS)
            ks.load(null)
            if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS)
        } catch (t: Throwable) {
        }
    }
}
