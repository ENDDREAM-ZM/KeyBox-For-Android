package com.enddream.keybox

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object Bio {
    private const val KS = "AndroidKeyStore"
    private const val ALIAS = "keybox_bio_v3"
    private const val PREFS = "keybox_bio"
    private const val K_BLOB = "blob"
    private const val K_IV = "iv"

    fun canAuthenticate(c: Context): Int {
        if (Build.VERSION.SDK_INT < 28) return -1
        if (Build.VERSION.SDK_INT >= 29) {
            return try {
                val bm = c.getSystemService(BiometricManager::class.java)
                bm?.canAuthenticate() ?: -1
            } catch (t: Throwable) {
                -1
            }
        }
        return try {
            if (!c.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) return 12
            val km = c.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            if (km.isDeviceSecure) 0 else 11
        } catch (t: Throwable) {
            -1
        }
    }

    fun statusText(c: Context): String {
        if (Build.VERSION.SDK_INT < 28) return "系统版本过低（需 Android 9 及以上）"
        return when (canAuthenticate(c)) {
            0 -> "可用"
            11 -> "系统里还没有录入指纹或人脸"
            12 -> "此设备没有可用的生物识别硬件"
            1 -> "生物识别硬件暂时不可用"
            -1 -> "无法查询（可用「去设置」后重试）"
            else -> "不可用"
        }
    }

    fun hasHardware(c: Context): Boolean = canAuthenticate(c) == 0

    fun hasBlob(c: Context): Boolean =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(K_BLOB)

    fun isEnabled(c: Context): Boolean = Prefs.biometric(c) && hasBlob(c)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KS)
        ks.load(null)
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KS)
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(false)
        if (Build.VERSION.SDK_INT >= 30) {
            spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }
        kg.init(spec.build())
        return kg.generateKey()
    }

    private fun prompt(act: Activity, cipher: Cipher, title: String, onOk: (Cipher) -> Unit, onFail: (String) -> Unit) {
        val cs = CancellationSignal()
        try {
            val ex = act.mainExecutor
            val prompt = BiometricPrompt.Builder(act)
                .setTitle(title)
                .setNegativeButton("取消", ex) { _, _ ->
                    cs.cancel()
                    onFail("已取消")
                }
                .build()
            prompt.authenticate(
                BiometricPrompt.CryptoObject(cipher),
                cs,
                ex,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        val c = result?.cryptoObject?.cipher
                        if (c == null) onFail("验证结果异常") else onOk(c)
                    }

                    override fun onAuthenticationError(code: Int, msg: CharSequence?) {
                        onFail(msg?.toString() ?: "验证失败")
                    }
                }
            )
        } catch (t: Throwable) {
            onFail("生物识别不可用：" + (t.message ?: ""))
        }
    }

    fun enable(act: Activity, master: String, onOk: () -> Unit, onFail: (String) -> Unit) {
        if (master.isEmpty()) {
            onFail("请先设置主密码")
            return
        }
        val cipher = try {
            Cipher.getInstance("AES/GCM/NoPadding").also { it.init(Cipher.ENCRYPT_MODE, key()) }
        } catch (t: Throwable) {
            onFail("无法初始化加密：" + (t.message ?: ""))
            return
        }
        prompt(act, cipher, "验证生物识别以开启解锁", { c ->
            try {
                val blob = c.doFinal(master.toByteArray(Charsets.UTF_8))
                act.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(K_BLOB, B64.encode(blob))
                    .putString(K_IV, B64.encode(c.iv))
                    .apply()
                Prefs.setBiometric(act, true)
                onOk()
            } catch (t: Throwable) {
                onFail("保存失败：" + (t.message ?: ""))
            }
        }, onFail)
    }

    fun authenticate(act: Activity, onOk: (String) -> Unit, onFail: (String) -> Unit) {
        if (!isEnabled(act)) {
            onFail("未启用指纹解锁")
            return
        }
        val sp = act.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val blob = sp.getString(K_BLOB, null)
        val iv = sp.getString(K_IV, null)
        if (blob == null || iv == null) {
            onFail("指纹数据缺失，请用主密码解锁")
            return
        }
        val cipher = try {
            Cipher.getInstance("AES/GCM/NoPadding").also {
                it.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, B64.decode(iv)))
            }
        } catch (t: Throwable) {
            onFail("指纹已失效，请用主密码解锁后重新开启")
            return
        }
        prompt(act, cipher, "生物识别验证", { c ->
            try {
                onOk(String(c.doFinal(B64.decode(blob)), Charsets.UTF_8))
            } catch (t: Throwable) {
                onFail("验证后解密失败，请用主密码")
            }
        }, onFail)
    }

    fun verify(act: Activity, onOk: () -> Unit, onFail: (String) -> Unit) {
        val cipher = try {
            Cipher.getInstance("AES/GCM/NoPadding").also { it.init(Cipher.ENCRYPT_MODE, key()) }
        } catch (t: Throwable) {
            onFail("生物识别不可用")
            return
        }
        prompt(act, cipher, "验证身份", { onOk() }, onFail)
    }

    fun disable(c: Context) {
        Prefs.setBiometric(c, false)
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        try {
            val ks = KeyStore.getInstance(KS)
            ks.load(null)
            if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS)
            if (ks.containsAlias("keybox_bio_v2")) ks.deleteEntry("keybox_bio_v2")
        } catch (t: Throwable) {
        }
    }
}

object B64 {
    private val CH = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray()

    fun encode(d: ByteArray): String {
        val b = StringBuilder()
        var i = 0
        while (i < d.size) {
            val b0 = d[i].toInt() and 0xff
            val b1 = if (i + 1 < d.size) d[i + 1].toInt() and 0xff else 0
            val b2 = if (i + 2 < d.size) d[i + 2].toInt() and 0xff else 0
            b.append(CH[(b0 ushr 2) and 63])
            b.append(CH[((b0 shl 4) or (b1 ushr 4)) and 63])
            b.append(if (i + 1 < d.size) CH[((b1 shl 2) or (b2 ushr 6)) and 63] else '=')
            b.append(if (i + 2 < d.size) CH[b2 and 63] else '=')
            i += 3
        }
        return b.toString()
    }

    fun decode(s: String): ByteArray {
        val out = ByteArray(s.length * 3 / 4 + 3)
        var o = 0
        var acc = 0
        var bits = 0
        for (ch in s) {
            val v = when {
                ch in 'A'..'Z' -> ch - 'A'
                ch in 'a'..'z' -> ch - 'a' + 26
                ch in '0'..'9' -> ch - '0' + 52
                ch == '+' -> 62
                ch == '/' -> 63
                else -> continue
            }
            acc = (acc shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out[o++] = ((acc ushr bits) and 0xff).toByte()
            }
        }
        return out.copyOf(o)
    }
}
