package com.enddream.keybox

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.enddream.keybox.core.Entry
import com.enddream.keybox.core.Vault
import com.enddream.keybox.core.VaultCrypto
import java.io.File
import java.util.concurrent.Executors

object Store {
    const val FILE = "vault.pmv"
    const val DEFAULT_ITER = 300000

    var vault: Vault = Vault()
        private set
    var master: String? = null
        private set
    var open: Boolean = false
        private set
    var iter: Int = DEFAULT_ITER

    var version by mutableStateOf(0)
        private set

    private val io = Executors.newSingleThreadExecutor { r ->
        Thread(r, "keybox-io").apply { isDaemon = true }
    }
    private val main = Handler(Looper.getMainLooper())

    fun file(c: Context) = File(c.filesDir, FILE)
    fun exists(c: Context) = file(c).exists() && file(c).length() > 0

    fun touch() {
        if (Looper.myLooper() == Looper.getMainLooper()) version++
        else main.post { version++ }
    }

    fun openAuto(c: Context) {
        if (!exists(c)) {
            vault.entries.clear()
            vault.cats.clear()
            master = null
            open = true
            save(c)
            return
        }
        val raw = file(c).readBytes()
        vault.open(DeviceKey.get(c), raw)
        master = null
        open = true
        Totp.clearCache()
        touch()
    }

    fun createFresh(c: Context) {
        vault.entries.clear()
        vault.cats.clear()
        master = null
        open = true
        save(c)
        Totp.clearCache()
        touch()
    }

    fun isProtected(c: Context): Boolean {
        if (!exists(c)) return false
        return try {
            val raw = file(c).readBytes()
            VaultCrypto.openVault(DeviceKey.get(c), raw)
            false
        } catch (t: Throwable) {
            true
        }
    }

    fun unlock(c: Context, password: String) {
        val raw = file(c).readBytes()
        vault.open(password, raw)
        master = password
        open = true
        Totp.clearCache()
        touch()
    }

    fun setMaster(c: Context, pw: String) {
        master = pw
        save(c)
        Totp.reseal(c)
    }

    fun clearMaster(c: Context) {
        master = null
        save(c)
        if (Bio.isEnabled(c)) Bio.disable(c)
    }

    fun save(c: Context) {
        if (!open) return
        vault.purge(Prefs.retentionDays(c) * 24L * 3600L * 1000L)
        val payload = vault.toPayload()
        val m = master ?: DeviceKey.get(c)
        val itr = iter
        val f = file(c)
        touch()
        io.execute {
            try {
                val out = VaultCrypto.sealVault(m, payload, itr)
                val tmp = File(f.parentFile, "$FILE.tmp")
                tmp.writeBytes(out)
                if (f.exists()) f.delete()
                if (!tmp.renameTo(f)) {
                    f.writeBytes(out)
                    tmp.delete()
                }
            } catch (t: Throwable) {
            }
        }
    }

    fun lock() {
        vault.entries.clear()
        vault.cats.clear()
        master = null
        open = false
        Totp.clearCache()
        touch()
    }

    fun find(id: String): Entry? = vault.find(id)

    fun post(r: Runnable) {
        main.post(r)
    }
}
