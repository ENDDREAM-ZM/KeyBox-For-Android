package com.enddream.keybox

import android.content.Context
import com.enddream.keybox.core.Json
import com.enddream.keybox.core.VaultCrypto
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

object Totp {
    private const val FILE = "totp.pmv"

    data class Token(
        val id: String,
        val issuer: String,
        val account: String,
        val secret: String,
        val digits: Int,
        val period: Int
    ) {
        fun label(): String = when {
            issuer.isNotEmpty() && account.isNotEmpty() -> "$issuer · $account"
            issuer.isNotEmpty() -> issuer
            account.isNotEmpty() -> account
            else -> "未命名"
        }
    }

    private var cache: MutableList<Token>? = null

    private val io = Executors.newSingleThreadExecutor { r ->
        Thread(r, "keybox-totp").apply { isDaemon = true }
    }

    private fun file(c: Context) = File(c.filesDir, FILE)

    private fun serialize(list: List<Token>): ByteArray {
        val sb = StringBuilder("[")
        list.forEachIndexed { i, t ->
            if (i > 0) sb.append(',')
            sb.append("{\"id\":").append(Json.quote(t.id))
            sb.append(",\"issuer\":").append(Json.quote(t.issuer))
            sb.append(",\"account\":").append(Json.quote(t.account))
            sb.append(",\"secret\":").append(Json.quote(t.secret))
            sb.append(",\"digits\":").append(t.digits)
            sb.append(",\"period\":").append(t.period)
            sb.append('}')
        }
        sb.append(']')
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun parse(raw: ByteArray): MutableList<Token> {
        val out = mutableListOf<Token>()
        try {
            val arr = Json.asList(Json.parse(String(raw, Charsets.UTF_8))) ?: return out
            arr.forEach { o ->
                val m = Json.asObj(o) ?: return@forEach
                val secret = Json.getStr(m, "secret", "")
                if (secret.isEmpty()) return@forEach
                out.add(
                    Token(
                        id = Json.getStr(m, "id", "").ifEmpty { UUID.randomUUID().toString().take(16) },
                        issuer = Json.getStr(m, "issuer", ""),
                        account = Json.getStr(m, "account", ""),
                        secret = secret,
                        digits = Json.getNum(m, "digits", 6).toInt(),
                        period = Json.getNum(m, "period", 30).toInt()
                    )
                )
            }
        } catch (t: Throwable) {
        }
        return out
    }

    fun load(c: Context): List<Token> {
        cache?.let { return it }
        val f = file(c)
        if (!f.exists()) {
            cache = mutableListOf()
            return cache!!
        }
        val list = try {
            parse(VaultCrypto.openVault(Store.master ?: DeviceKey.get(c), f.readBytes()))
        } catch (t: Throwable) {
            mutableListOf()
        }
        cache = list
        return list
    }

    fun save(c: Context) {
        val list = cache ?: return
        val payload = serialize(list)
        val pw = Store.master ?: DeviceKey.get(c)
        val iter = Store.iter
        val f = file(c)
        io.execute {
            try {
                val out = VaultCrypto.sealVault(pw, payload, iter)
                val tmp = File(f.parentFile, FILE + ".tmp")
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

    fun clearCache() {
        cache = null
    }

    fun all(c: Context): List<Token> = load(c).toList()

    fun find(c: Context, id: String): Token? = load(c).firstOrNull { it.id == id }

    fun add(c: Context, issuer: String, account: String, secret: String, digits: Int, period: Int): String {
        val clean = secret.trim().replace(" ", "").replace("-", "").uppercase()
        if (clean.isEmpty()) return ""
        val id = UUID.randomUUID().toString().replace("-", "").take(16)
        val list = load(c).toMutableList()
        list.add(Token(id, issuer.trim(), account.trim(), clean, digits, period))
        cache = list
        save(c)
        return id
    }

    fun update(c: Context, token: Token) {
        val list = load(c).toMutableList()
        val i = list.indexOfFirst { it.id == token.id }
        if (i < 0) return
        list[i] = token.copy(secret = token.secret.trim().replace(" ", "").replace("-", "").uppercase())
        cache = list
        save(c)
    }

    fun remove(c: Context, id: String) {
        val list = load(c).toMutableList()
        list.removeAll { it.id == id }
        cache = list
        save(c)
    }

    fun reseal(c: Context) {
        if (cache != null) save(c)
    }

    private fun base32Decode(s: String): ByteArray {
        val map = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        var buffer = 0
        var bits = 0
        val out = java.io.ByteArrayOutputStream()
        for (ch in s) {
            val v = map.indexOf(ch)
            if (v < 0) continue
            buffer = (buffer shl 5) or v
            bits += 5
            if (bits >= 8) {
                bits -= 8
                out.write((buffer ushr bits) and 0xff)
            }
        }
        return out.toByteArray()
    }

    fun code(secret: String, digits: Int, period: Int, atMs: Long = System.currentTimeMillis()): String {
        return try {
            val key = base32Decode(secret.replace(" ", "").replace("-", "").uppercase())
            if (key.isEmpty()) return ""
            val counter = atMs / 1000L / period
            val msg = ByteArray(8)
            var v = counter
            for (i in 7 downTo 0) {
                msg[i] = (v and 0xff).toByte()
                v = v ushr 8
            }
            val name = "HmacSHA1"
            val mac = javax.crypto.Mac.getInstance(name)
            mac.init(javax.crypto.spec.SecretKeySpec(key, name))
            val h = mac.doFinal(msg)
            val off = h[h.size - 1].toInt() and 0x0f
            val bin = ((h[off].toInt() and 0x7f) shl 24) or
                    ((h[off + 1].toInt() and 0xff) shl 16) or
                    ((h[off + 2].toInt() and 0xff) shl 8) or
                    (h[off + 3].toInt() and 0xff)
            val mod = when (digits) {
                8 -> 100000000
                7 -> 10000000
                else -> 1000000
            }
            (bin % mod).toString().padStart(digits, '0')
        } catch (t: Throwable) {
            ""
        }
    }

    fun remain(period: Int, atMs: Long = System.currentTimeMillis()): Int =
        (period - (atMs / 1000L) % period).toInt()

    data class Parsed(
        val issuer: String,
        val account: String,
        val secret: String,
        val digits: Int,
        val period: Int
    )

    fun parseOtpUri(uri: String): Parsed? {
        val u = uri.trim()
        if (!u.startsWith("otpauth://", true)) return null
        return try {
            val parsed = android.net.Uri.parse(u)
            if ((parsed.host ?: "").lowercase() != "totp") return null
            val label = android.net.Uri.decode(parsed.path?.removePrefix("/") ?: "")
            val issuerQ = parsed.getQueryParameter("issuer") ?: ""
            val secret = parsed.getQueryParameter("secret") ?: return null
            val digits = parsed.getQueryParameter("digits")?.toIntOrNull() ?: 6
            val period = parsed.getQueryParameter("period")?.toIntOrNull() ?: 30
            var issuer = issuerQ
            var account = label
            if (label.contains(":")) {
                val left = label.substringBefore(":").trim()
                account = label.substringAfter(":").trim()
                if (issuer.isEmpty()) issuer = left
            }
            Parsed(issuer, account, secret, digits, period)
        } catch (t: Throwable) {
            null
        }
    }
}
