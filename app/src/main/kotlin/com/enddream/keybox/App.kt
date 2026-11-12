package com.enddream.keybox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.enddream.keybox.core.Entry
import com.enddream.keybox.core.Vault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class Cat(val icon: ImageVector, val bg: Long) {
    Social(Icons.Rounded.Groups, 0xFF4C6FD0),
    Shop(Icons.Rounded.ShoppingBag, 0xFFB4633C),
    Bank(Icons.Rounded.AccountBalance, 0xFF2E8B70),
    Mail(Icons.Rounded.Mail, 0xFF7A63C4),
    Game(Icons.Rounded.SportsEsports, 0xFFB8497B),
    Cloud(Icons.Rounded.Cloud, 0xFF3C8FA8),
    Video(Icons.Rounded.PlayCircle, 0xFFC0392B),
    Tool(Icons.Rounded.Terminal, 0xFF55637A),
    Other(Icons.Rounded.VpnKey, 0xFF6E7B8B)
}

private val rules: List<Pair<Cat, Array<String>>> = listOf(
    Cat.Social to arrayOf(
        "weibo", "wechat", "qq.com", "telegram", "twitter", "x.com", "discord",
        "facebook", "instagram", "whatsapp", "line.me", "douyin", "tiktok", "zhihu", "douban"
    ),
    Cat.Shop to arrayOf(
        "taobao", "tmall", "jd.com", "pinduoduo", "yangkeduo", "amazon", "ebay",
        "shopee", "meituan", "ele.me", "dianping", "suning", "vip.com", "xiaohongshu"
    ),
    Cat.Bank to arrayOf(
        "bank", "alipay", "paypal", "cmbchina", "icbc", "abchina", "ccb.com",
        "boc.cn", "unionpay", "wallet", "stripe"
    ),
    Cat.Mail to arrayOf(
        "mail", "gmail", "outlook", "163.com", "126.com", "protonmail", "yahoo", "mail.qq"
    ),
    Cat.Game to arrayOf(
        "steam", "epicgames", "blizzard", "battle.net", "xbox", "playstation",
        "riot", "game", "mihoyo", "hoyolab", "netease"
    ),
    Cat.Cloud to arrayOf(
        "pan.", "drive", "dropbox", "onedrive", "mega.nz", "aliyundrive",
        "cloud", "115.com", "baidupan"
    ),
    Cat.Video to arrayOf(
        "youtube", "bilibili", "netflix", "iqiyi", "youku", "v.qq.com",
        "spotify", "music", "kuaishou"
    ),
    Cat.Tool to arrayOf(
        "github", "gitlab", "openai", "chatgpt", "claude", "npm", "docker",
        "aws", "azure", "cloudflare", "vps", "router", "nas", "admin", "console", "ssh"
    )
)

fun categoryOf(e: Entry): Cat {
    val hay = ((e.url ?: "") + " " + (e.title ?: "")).lowercase()
    rules.forEach { (cat, keys) ->
        keys.forEach { if (hay.contains(it)) return cat }
    }
    return Cat.Other
}

private val tileColors = longArrayOf(
    0xFF4C6FD0, 0xFFB4633C, 0xFF2E8B70, 0xFF7A63C4,
    0xFFB8497B, 0xFF3C8FA8, 0xFFC0392B, 0xFF55637A
)

fun categoryColorFor(seed: String): Color {
    if (seed.isEmpty()) return Color(0xFF6E7B8B)
    val i = (seed.hashCode() and 0x7fffffff) % tileColors.size
    return Color(tileColors[i])
}

object IconLoader {
    private var appCtx: Context? = null
    private val mem = LruCache<String, Bitmap>(256)
    private val missing = HashSet<String>()
    private val locks = HashMap<String, Mutex>()

    var epoch by mutableStateOf(0)
        private set

    private fun lockFor(k: String): Mutex = synchronized(locks) {
        locks.getOrPut(k) { Mutex() }
    }

    private fun dir(c: Context): File {
        val d = File(c.filesDir, "icons")
        if (!d.exists()) d.mkdirs()
        return d
    }

    private fun sha1(s: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-1")
            md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
        } catch (t: Throwable) {
            s.hashCode().toString()
        }
    }

    private fun diskFile(c: Context, k: String) = File(dir(c), sha1(k) + ".png")

    fun clear(c: Context, deep: Boolean = true) {
        synchronized(mem) { mem.evictAll() }
        synchronized(missing) { missing.clear() }
        if (deep) {
            try {
                dir(c).listFiles()?.forEach { it.delete() }
            } catch (t: Throwable) {
            }
        }
        epoch++
    }

    fun retryMissing() {
        synchronized(missing) { missing.clear() }
        epoch++
    }

    fun ensureVersion(c: Context, ver: Int) {
        val sp = c.getSharedPreferences("keybox", Context.MODE_PRIVATE)
        if (sp.getInt("icon_cache_ver", 0) < ver) {
            clear(c, true)
            sp.edit().putInt("icon_cache_ver", ver).apply()
        }
    }

    fun cached(e: Entry): Bitmap? = try {
        synchronized(mem) { mem.get(key(e)) }
    } catch (t: Throwable) {
        null
    }

    fun siblingIcon(e: Entry): Bitmap? = try {
        val host = hostOf(e.urlList().firstOrNull() ?: "")
        if (host.isEmpty()) return null
        var out: Bitmap? = null
        val d = baseDomain(host)
        val list = Store.vault.entries.filter { o ->
            o !== e && o.del == 0 && o.urlList().any { u -> baseDomain(hostOf(u)) == d }
        }
        for (o in list) {
            val b = cached(o)
            if (b != null) {
                out = b
                break
            }
            val c0 = appCtx
            if (c0 != null) {
                val f = diskFile(c0, key(o))
                if (f.exists()) {
                    val d = BitmapFactory.decodeFile(f.absolutePath)
                    if (d != null) {
                        synchronized(mem) { mem.put(key(o), d) }
                        out = d
                        break
                    }
                }
            }
        }
        out
    } catch (t: Throwable) {
        null
    }

    private val TWO_PART = arrayOf(
        "com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn", "co.uk", "org.uk",
        "ac.uk", "co.jp", "ne.jp", "or.jp", "com.tw", "com.hk", "com.au", "co.kr"
    )

    fun baseDomain(host: String): String {
        val h = host.lowercase().removePrefix("www.")
        if (h.isEmpty()) return ""
        val parts = h.split(".")
        if (parts.size <= 2) return h
        val tail2 = parts.takeLast(2).joinToString(".")
        val keep = if (TWO_PART.any { h.endsWith(".$it") } || TWO_PART.contains(tail2)) 3 else 2
        return parts.takeLast(keep).joinToString(".")
    }

    private fun key(e: Entry): String {
        val d = baseDomain(hostOf(e.urlList().firstOrNull() ?: ""))
        if (d.isNotEmpty()) return "host:" + d
        return "pkg:" + (e.pkg ?: "")
    }

    fun appIcon(c: Context, pkg: String): Bitmap? {
        val k = "pkg:" + pkg
        synchronized(mem) { mem.get(k) }?.let { return it }
        return try {
            val d = c.packageManager.getApplicationIcon(pkg)
            val size = 96
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val cv = Canvas(bmp)
            d.setBounds(0, 0, size, size)
            d.draw(cv)
            synchronized(mem) { mem.put(k, bmp) }
            bmp
        } catch (t: Throwable) {
            null
        }
    }

    private val pool = java.util.concurrent.Executors.newFixedThreadPool(4) { r ->
        Thread(r, "keybox-icon").apply { isDaemon = true }
    }

    private fun fetchFirst(urls: List<String>, accept: (Bitmap) -> Boolean): Bitmap? {
        if (urls.isEmpty()) return null
        val futures = urls.map { u ->
            pool.submit(java.util.concurrent.Callable { fetch(u) })
        }
        val res = arrayOfNulls<Bitmap>(urls.size)
        for (i in futures.indices) {
            res[i] = try {
                futures[i].get(5, java.util.concurrent.TimeUnit.SECONDS)
            } catch (t: Throwable) {
                null
            }
        }
        return res.firstOrNull { it != null && accept(it) }
    }

    private fun fetch(url: String): Bitmap? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 2500
            conn.readTimeout = 2500
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 KeyBox")
            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }
            val bytes = conn.inputStream.use { it.readBytes() }
            conn.disconnect()
            if (bytes.size < 64) return null
            val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (b != null && b.width >= 8) b else null
        } catch (t: Throwable) {
            null
        }
    }

    private fun loadLocked(c: Context, e: Entry): Bitmap? {
        val k = key(e)
        synchronized(mem) { mem.get(k) }?.let { return it }
        val f = diskFile(c, k)
        if (f.exists()) {
            val b = BitmapFactory.decodeFile(f.absolutePath)
            if (b != null) {
                synchronized(mem) { mem.put(k, b) }
                return b
            }
            f.delete()
        }
        val host = hostOf(e.urlList().firstOrNull() ?: "")
        val pkg = sharedPkg(e, host)
        var bmp: Bitmap? = null
        if (host.isNotEmpty()) bmp = fetchHighRes(host)
        if (bmp == null && pkg.isNotEmpty()) bmp = appIcon(c, pkg)
        if (bmp == null && host.isNotEmpty()) bmp = fetchSmall(host)
        if (bmp != null) {
            synchronized(mem) { mem.put(k, bmp) }
            try {
                f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } catch (t: Throwable) {
            }
        } else {
            synchronized(missing) { missing.add(k) }
        }
        return bmp
    }

    private fun sharedPkg(e: Entry, host: String): String {
        val own = e.pkg ?: ""
        if (own.isNotEmpty()) return own
        if (host.isEmpty()) return ""
        return try {
            val d = baseDomain(host)
            Store.vault.entries.firstOrNull { other ->
                other !== e && !(other.pkg ?: "").isEmpty() && other.del == 0 &&
                        other.urlList().any { u -> baseDomain(hostOf(u)) == d }
            }?.pkg ?: ""
        } catch (t: Throwable) {
            ""
        }
    }

    private fun siblingDisk(c: Context, e: Entry, host: String): Bitmap? {
        return try {
            val other = Store.vault.entries.firstOrNull { o ->
                o !== e && o.del == 0 && !(o.pkg ?: "").isEmpty() &&
                        o.urlList().any { u -> hostOf(u) == host }
            } ?: return null
            val f = diskFile(c, key(other))
            if (!f.exists()) return null
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (t: Throwable) {
            null
        }
    }

    private fun fetchText(url: String): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 KeyBox")
            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }
            val type = conn.contentType ?: ""
            if (!type.contains("html", true)) {
                conn.disconnect()
                return null
            }
            val input = conn.inputStream
            val buf = ByteArray(160 * 1024)
            var n = 0
            var total = 0
            while (total < buf.size) {
                n = input.read(buf, total, buf.size - total)
                if (n <= 0) break
                total += n
            }
            conn.disconnect()
            String(buf, 0, total, Charsets.UTF_8)
        } catch (t: Throwable) {
            null
        }
    }

    private fun absolute(href: String, host: String): String {
        val h = href.trim()
        return when {
            h.startsWith("http://") || h.startsWith("https://") -> h
            h.startsWith("//") -> "https:" + h
            h.startsWith("/") -> "https://$host" + h
            else -> "https://$host/" + h
        }
    }

    private fun parseIconLinks(html: String, host: String): List<String> {
        val scored = ArrayList<Pair<Int, String>>()
        val linkRe = Regex("<link[^>]*>", RegexOption.IGNORE_CASE)
        val relRe = Regex("rel\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val hrefRe = Regex("href\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val sizesRe = Regex("sizes\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        linkRe.findAll(html).forEach { m ->
            val tag = m.value
            val rel = relRe.find(tag)?.groupValues?.get(1)?.lowercase() ?: return@forEach
            if (!rel.contains("icon") && !rel.contains("logo") && !rel.contains("image")) return@forEach
            val href = hrefRe.find(tag)?.groupValues?.get(1) ?: return@forEach
            val sizes = sizesRe.find(tag)?.groupValues?.get(1) ?: ""
            var score = sizes.split("x").firstOrNull()?.toIntOrNull() ?: 0
            if (href.contains("logo", true)) score += 2000
            if (rel.contains("apple-touch")) score += 500
            if (rel.contains("shortcut")) score += 100
            scored.add(score to absolute(href, host))
        }
        val metaRe = Regex("<meta[^>]*>", RegexOption.IGNORE_CASE)
        val propRe = Regex("(?:property|name)\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val contentRe = Regex("content\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        metaRe.findAll(html).forEach { m ->
            val tag = m.value
            val prop = propRe.find(tag)?.groupValues?.get(1)?.lowercase() ?: return@forEach
            if (prop != "og:image" && prop != "twitter:image") return@forEach
            val c = contentRe.find(tag)?.groupValues?.get(1) ?: return@forEach
            scored.add(-1 to absolute(c, host))
        }
        return scored.sortedByDescending { it.first }.map { it.second }.distinct()
    }

    private fun square(b: Bitmap): Bitmap {
        val w = b.width
        val h = b.height
        var out = b
        if (w > h * 1.25f || h > w * 1.25f) {
            val side = minOf(w, h)
            out = Bitmap.createBitmap(b, (w - side) / 2, (h - side) / 2, side, side)
        }
        if (out.width > 256) {
            out = Bitmap.createScaledBitmap(out, 256, 256, true)
        }
        return out
    }

    private fun fetchHighRes(host: String): Bitmap? {
        val urls = ArrayList<String>()
        urls.add("https://$host/apple-touch-icon.png")
        urls.add("https://$host/apple-touch-icon-precomposed.png")
        val html = fetchText("https://$host/") ?: fetchText("http://$host/")
        if (html != null) urls.addAll(parseIconLinks(html, host))
        urls.add("https://$host/logo.png")
        urls.add("https://$host/icon.png")
        urls.add("https://api.iowen.cn/favicon/$host.png")
        urls.add("https://favicon.cccyun.cc/$host")
        urls.add("https://favicon.im/$host?larger=true")
        urls.add("https://www.google.com/s2/favicons?sz=256&domain=$host")
        urls.add("https://icons.duckduckgo.com/ip3/$host.ico")
        urls.add("https://logo.clearbit.com/$host")
        val b = fetchFirst(urls) { it.width >= 96 && it.height >= 96 }
        return if (b == null) null else square(b)
    }

    private fun fetchSmall(host: String): Bitmap? {
        val urls = listOf(
            "https://$host/favicon.ico",
            "https://$host/favicon.png",
            "https://api.iowen.cn/favicon/$host.png",
            "https://favicon.cccyun.cc/$host",
            "https://icons.duckduckgo.com/ip3/$host.ico",
            "https://www.google.com/s2/favicons?sz=64&domain=$host"
        )
        return fetchFirst(urls) { true }
    }

    suspend fun load(c: Context, e: Entry): Bitmap? {
        appCtx = c.applicationContext
        val k = key(e)
        synchronized(mem) { mem.get(k) }?.let { return it }
        synchronized(missing) { if (k in missing) return null }
        return withContext(Dispatchers.IO) {
            lockFor(k).withLock { loadLocked(c, e) }
        }
    }

    suspend fun prefetch(c: Context, list: List<Entry>) {
        if (list.isEmpty()) return
        val sem = Semaphore(6)
        kotlinx.coroutines.coroutineScope {
            list.forEach { e ->
                launch(Dispatchers.IO) {
                    sem.withPermit {
                        try {
                            load(c, e)
                        } catch (t: Throwable) {
                        }
                    }
                }
            }
        }
    }
}

fun hostOf(url: String?): String {
    if (url.isNullOrEmpty()) return ""
    var u = url.trim()
    val i = u.indexOf("://")
    if (i >= 0) u = u.substring(i + 3)
    val j = u.indexOf('/')
    if (j >= 0) u = u.substring(0, j)
    val k = u.indexOf('?')
    if (k >= 0) u = u.substring(0, k)
    val at = u.indexOf('@')
    if (at >= 0) u = u.substring(at + 1)
    return u
}

@Composable
fun EntryIcon(e: Entry, size: Int = 40) {
    val ctx = LocalContext.current
    val ep = IconLoader.epoch
    var bmp by remember(e.id, e.pkg, e.url, e.title, ep) {
        mutableStateOf(IconLoader.cached(e) ?: IconLoader.siblingIcon(e))
    }
    LaunchedEffect(e.id, e.pkg, e.url, e.title, ep) {
        if (bmp == null) bmp = IconLoader.load(ctx, e)
    }
    val shape = RoundedCornerShape((size * 0.3f).dp)
    val b = bmp
    if (b != null) {
        Box(Modifier.size(size.dp).clip(shape)) {
            Image(b.asImageBitmap(), null, Modifier.size(size.dp))
        }
    } else {
        val cat = categoryOf(e)
        Box(
            Modifier.size(size.dp).clip(shape).background(Color(cat.bg)),
            contentAlignment = Alignment.Center
        ) {
            Icon(cat.icon, null, Modifier.size((size * 0.56f).dp), tint = Color.White)
        }
    }
}

private val clipHandler = android.os.Handler(android.os.Looper.getMainLooper())
private var clipToken = 0

fun copyText(c: Context, text: String) {
    val cm = c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("KeyBox", text)
    try {
        clip.description.extras = android.os.PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
    } catch (t: Throwable) {
    }
    cm.setPrimaryClip(clip)
    val sec = Prefs.clipboardSeconds(c)
    clipToken++
    val my = clipToken
    if (sec > 0) {
        clipHandler.postDelayed({
            if (my != clipToken) return@postDelayed
            try {
                val cur = cm.primaryClip
                val s = cur?.getItemAt(0)?.coerceToText(c)?.toString()
                if (s == text) {
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        cm.clearPrimaryClip()
                    } else {
                        cm.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            } catch (t: Throwable) {
            }
        }, sec * 1000L)
    }
}

fun launchApp(c: Context, pkg: String) {
    val i = c.packageManager.getLaunchIntentForPackage(pkg) ?: return
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    c.startActivity(i)
}

fun openUrl(c: Context, url: String) {
    var u = url.trim()
    if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
    try {
        c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
    } catch (e: Exception) {
    }
}

fun strengthLabel(p: String): String = when (Vault.strength(p)) {
    2 -> "强"
    1 -> "中"
    else -> "弱"
}

fun nz(s: String?): String = if (s.isNullOrEmpty()) "无" else s

object EntryFix {
    private fun split(raw: String): List<String> =
        raw.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }

    fun normalize(e: Entry) {
        val out = ArrayList<String>()
        e.urlList().forEach { raw ->
            split(raw).forEach { u ->
                if (u.startsWith("androidapp://", true)) {
                    val p = u.substring("androidapp://".length).trim().trimEnd('/')
                    if (p.isNotEmpty() && (e.pkg ?: "").isBlank()) e.pkg = p
                } else if (!out.contains(u)) {
                    out.add(u)
                }
            }
        }
        e.urls = out
        e.url = out.firstOrNull() ?: ""
    }

    fun normalizeAll() {
        Store.vault.entries.forEach { normalize(it) }
    }
}
