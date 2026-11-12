package com.enddream.keybox

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.enddream.keybox.core.Entry
import com.enddream.keybox.core.Vault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalTopInset = staticCompositionLocalOf { 76.dp }
val LocalBottomInset = staticCompositionLocalOf { 84.dp }

object AppState {
    var locked by mutableStateOf(true)
    var route by mutableStateOf<Route>(Route.Main)
    var tab by mutableStateOf(0)
}

sealed class Route {
    data object Main : Route()
    data class Edit(val entry: Entry, val fresh: Boolean) : Route()
    data object About : Route()
}

val navItems = listOf(
    Triple("密码", Icons.Rounded.Key, 0),
    Triple("双重认证", Icons.Rounded.VerifiedUser, 1),
    Triple("生成器", Icons.Rounded.Casino, 2),
    Triple("回收站", Icons.Rounded.Delete, 3),
    Triple("设置", Icons.Rounded.Settings, 4)
)

@Composable
fun SystemBars(dark: Boolean) {
    val view = LocalView.current
    val ctx = LocalContext.current
    SideEffect {
        val w = (ctx as? Activity)?.window ?: return@SideEffect
        val c = WindowCompat.getInsetsController(w, view)
        c.isAppearanceLightStatusBars = !dark
        c.isAppearanceLightNavigationBars = !dark
    }
}

@Composable
fun KeyBoxRoot() {
    val ctx = LocalContext.current
    var style by remember { mutableStateOf(if (Prefs.style(ctx) == "miuix") UiStyle.Miuix else UiStyle.Material) }
    var seed by remember { mutableStateOf(Prefs.seed(ctx)) }
    var dynamic by remember { mutableStateOf(Prefs.dynamic(ctx)) }
    var mode by remember { mutableStateOf(Prefs.themeMode(ctx)) }
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
    var booted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) { Store.openAuto(ctx) }
            AppState.locked = false
        } catch (t: Throwable) {
            AppState.locked = true
        }
        booted = true
    }

    KeyBoxTheme(style, seed, dynamic, dark) {
        CompositionLocalProvider(LocalStyle provides style) {
            SystemBars(dark)
            val bg = if (isMiuix()) MiuixTheme.colorScheme.background else MaterialTheme.colorScheme.background
            Box(Modifier.fillMaxSize().background(bg)) {
                if (!booted) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        KText("KeyBox", txt = Txt.Title1)
                    }
                } else if (AppState.locked) {
                    UnlockScreen { AppState.locked = false }
                } else {
                    if (AppState.route !is Route.Main) {
                        androidx.activity.compose.BackHandler { AppState.route = Route.Main }
                    }
                    LaunchedEffect(Store.version, BannerState.dismissed) {
                        BannerState.visible = Store.master == null &&
                                !Prefs.noMasterRemind(ctx) && !BannerState.dismissed
                    }
                    AnimatedContent(
                        targetState = AppState.route,
                        transitionSpec = {
                            val forward = targetState !is Route.Main
                            val enter = slideInHorizontally(tween(240)) { if (forward) it else -it / 4 } + fadeIn(tween(200))
                            val exit = slideOutHorizontally(tween(240)) { if (forward) -it / 4 else it } + fadeOut(tween(160))
                            enter togetherWith exit
                        },
                        label = "route"
                    ) { r ->
                        when (r) {
                            is Route.Main -> MainScaffold(
                                onLock = {
                                    if (Store.master != null) {
                                        Store.lock()
                                        AppState.locked = true
                                    }
                                },
                                style = style,
                                onStyle = {
                                    style = it
                                    Prefs.setStyle(ctx, if (it == UiStyle.Miuix) "miuix" else "material")
                                },
                                seed = seed,
                                onSeed = { seed = it; Prefs.setSeed(ctx, it) },
                                dynamic = dynamic,
                                onDynamic = { dynamic = it; Prefs.setDynamic(ctx, it) },
                                themeMode = mode,
                                onThemeMode = { mode = it; Prefs.setThemeMode(ctx, it) }
                            )
                            is Route.Edit -> EditScreen(r.entry, r.fresh) { AppState.route = Route.Main }
                            is Route.About -> AboutScreen { AppState.route = Route.Main }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnlockScreen(onUnlocked: () -> Unit) {
    val ctx = LocalContext.current
    val act = ctx as? Activity
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showRecovery by remember { mutableStateOf(false) }
    val bioOn = remember { Bio.isEnabled(ctx) }
    val scope = rememberCoroutineScope()

    fun doUnlock(master: String) {
        if (busy) return
        busy = true
        err = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { Store.unlock(ctx, master) }
                AppState.locked = false
                onUnlocked()
            } catch (e: Exception) {
                err = e.message ?: "解锁失败"
                pw = ""
            }
            busy = false
        }
    }

    fun doBio() {
        if (act == null) {
            err = "当前环境不支持生物识别"
            return
        }
        Bio.authenticate(act, { m -> doUnlock(m) }, { msg ->
            if (msg != "已取消") err = msg
        })
    }

    LaunchedEffect(Unit) {
        if (bioOn) doBio()
    }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val tint = if (isMiuix()) MiuixTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
        Icon(Icons.Rounded.Lock, null, Modifier.size(60.dp), tint = tint)
        Spacer(Modifier.height(14.dp))
        KText("KeyBox", txt = Txt.Title1)
        Spacer(Modifier.height(6.dp))
        KText(
            if (bioOn) "已开启生物识别解锁" else "输入主密码解锁保险库",
            txt = Txt.Footnote2,
            color = secondaryColor()
        )
        Spacer(Modifier.height(20.dp))
        KField(pw, { pw = it }, "主密码", Modifier.fillMaxWidth(), password = true, enabled = !busy)
        if (err != null) {
            Spacer(Modifier.height(8.dp))
            KText(err!!, txt = Txt.Footnote2, color = errorColor())
        }
        Spacer(Modifier.height(16.dp))
        if (busy) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KSpinner(34.dp)
                Spacer(Modifier.height(10.dp))
                KText("正在加载密码库", txt = Txt.Footnote2, color = secondaryColor())
            }
        } else {
            KButton(
                "解锁",
                onClick = { doUnlock(pw) },
                modifier = Modifier.fillMaxWidth(),
                enabled = pw.isNotEmpty()
            )
            if (bioOn) {
                Spacer(Modifier.height(6.dp))
                KTextButton("生物识别验证", onClick = { doBio() }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(2.dp))
            KTextButton("忘记密码？重置密码库", onClick = { showRecovery = true }, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showRecovery) {
        ResetDialog(
            onDismiss = { showRecovery = false },
            onDone = {
                showRecovery = false
                AppState.locked = false
                onUnlocked()
            }
        )
    }
}

@Composable
fun ResetDialog(onDismiss: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val act = ctx as? Activity
    var step by remember { mutableIntStateOf(0) }
    var np1 by remember { mutableStateOf("") }
    var np2 by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    KDialog(
        title = if (step == 0) "重置密码库" else "设置新主密码",
        onDismiss = onDismiss,
        content = {
            if (step == 0) {
                KText(
                    "主密码无法找回：密码库里所有内容都用它加密，没有它就打不开。" +
                            "如果确实想不起来，只能清空密码库重新开始，这一步不可撤销。",
                    txt = Txt.Body2
                )
                Spacer(Modifier.height(8.dp))
                KText("点击下面的按钮，先验证本机指纹，再清空并重设。", txt = Txt.Footnote2, color = secondaryColor())
            } else {
                KText("密码库已清空，设置一个新的主密码。", txt = Txt.Footnote2, color = secondaryColor())
                Spacer(Modifier.height(10.dp))
                KField(np1, { np1 = it }, "新主密码", Modifier.fillMaxWidth(), password = true)
                Spacer(Modifier.height(10.dp))
                KField(np2, { np2 = it }, "再次输入", Modifier.fillMaxWidth(), password = true)
            }
            msg?.let {
                Spacer(Modifier.height(8.dp))
                KText(it, txt = Txt.Footnote2, color = errorColor())
            }
        },
        buttons = {
            KTextButton("取消", onDismiss, Modifier.weight(1f))
            if (step == 0) {
                KButton("验证并清空", {
                    if (busy) return@KButton
                    if (act == null) {
                        msg = "当前环境无法验证"
                        return@KButton
                    }
                    busy = true
                    msg = null
                    Bio.verify(act, {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    Store.file(ctx).delete()
                                    java.io.File(ctx.filesDir, "totp.pmv").delete()
                                }
                                Totp.clearCache()
                                Store.lock()
                                busy = false
                                step = 1
                            } catch (t: Throwable) {
                                busy = false
                                msg = "清空失败"
                            }
                        }
                    }, { m ->
                        busy = false
                        if (m != "已取消") msg = m
                    })
                }, Modifier.weight(1f), enabled = !busy)
            } else {
                KButton("完成", {
                    when {
                        np1.length < 6 -> msg = "新主密码至少 6 位"
                        np1 != np2 -> msg = "两次输入不一致"
                        else -> {
                            busy = true
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        Store.createFresh(ctx)
                                        Store.setMaster(ctx, np1)
                                    }
                                    busy = false
                                    onDone()
                                } catch (t: Throwable) {
                                    busy = false
                                    msg = "设置失败"
                                }
                            }
                        }
                    }
                }, Modifier.weight(1f), enabled = !busy)
            }
        }
    )
}

@Composable
fun MainScaffold(
    onLock: () -> Unit,
    style: UiStyle,
    onStyle: (UiStyle) -> Unit,
    seed: Int,
    onSeed: (Int) -> Unit,
    dynamic: Boolean,
    onDynamic: (Boolean) -> Unit,
    themeMode: String,
    onThemeMode: (String) -> Unit
) {
    val ctx = LocalContext.current
    val tab = AppState.tab
    var yiyan by remember { mutableStateOf("") }
    @Suppress("UNUSED_VARIABLE")
    val ver = Store.version
    var selecting by remember { mutableStateOf(false) }
    var sel by remember { mutableStateOf(setOf<String>()) }
    var viewing by remember { mutableStateOf<Entry?>(null) }
    var bannerMaster by remember { mutableStateOf(!Prefs.bannerOff(ctx) && Store.master == null) }
    var bannerRecovery by remember { mutableStateOf(false) }
    var q by remember(tab) { mutableStateOf("") }
    var cat by remember(tab) { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }
    var longPress by remember { mutableStateOf<Entry?>(null) }

    LaunchedEffect(Unit) {
        IconLoader.ensureVersion(ctx, 2)
        yiyan = Yiyan.get()
    }
    LaunchedEffect(tab) { selecting = false; sel = emptySet() }

    val all = Store.vault.entries
    val live = all.filter { it.del == 0 }
    val trashed = all.filter { it.del == 1 }

    LaunchedEffect(ver, tab) {
        IconLoader.prefetch(ctx, if (tab == 3) trashed else live)
    }

    fun newEntry() {
        val e = Entry().also {
            it.id = Vault.newId()
            it.ctime = System.currentTimeMillis()
            it.utime = it.ctime
        }
        AppState.route = Route.Edit(e, true)
    }

    KScaffold(
        topBar = {
            if (selecting) {
                KTopBar(
                    title = "已选 ${sel.size} 项",
                    onBack = { selecting = false; sel = emptySet() },
                    actions = {
                        val scope = if (tab == 0) live else trashed
                        KTextButton(
                            if (sel.size == scope.size && scope.isNotEmpty()) "取消全选" else "全选",
                            {
                                sel = if (sel.size == scope.size) emptySet()
                                else scope.map { it.id }.toSet()
                            }
                        )
                    }
                )
            } else {
                KTopBar(
                    title = "KeyBox",
                    subtitle = yiyan.ifEmpty { null },
                    actions = { KIconButton(Icons.Rounded.Lock, "锁定", onLock) }
                )
            }
        },
        topContent = {
            if (tab == 0 || tab == 3) {
                SearchRow(
                    q = q,
                    onQ = { q = it },
                    placeholder = "搜索密码",
                    selecting = selecting,
                    onToggleSelect = {
                        selecting = !selecting
                        if (!selecting) sel = emptySet()
                    },
                    onFilter = { showFilter = true }
                )
            }
        },
        bottomBar = {
            if (selecting) {
                SelectionBar(
                    count = sel.size,
                    trashMode = tab == 3,
                    onFav = {
                        val ids = sel.toList()
                        ids.forEach { id ->
                            Store.vault.entries.firstOrNull { it.id == id }?.let {
                                it.fav = if (it.fav == 1) 0 else 1
                                it.utime = System.currentTimeMillis()
                            }
                        }
                        Store.save(ctx)
                        selecting = false
                        sel = emptySet()
                    },
                    onTrash = {
                        val ids = sel.toList()
                        ids.forEach { Store.vault.trash(it) }
                        Store.save(ctx)
                        selecting = false
                        sel = emptySet()
                    },
                    onDelete = {
                        val ids = sel.toList()
                        ids.forEach { Store.vault.remove(it) }
                        Store.save(ctx)
                        selecting = false
                        sel = emptySet()
                    }
                )
            } else {
                KNavBar(navItems, tab) { t -> AppState.tab = t }
            }
        },
        fab = {
            if (tab == 0 && !selecting) {
                Column(horizontalAlignment = Alignment.End) {
                    KFab(onClick = {
                        IconLoader.retryMissing()
                        Store.touch()
                    }, icon = Icons.Rounded.Refresh, desc = "刷新")
                    Spacer(Modifier.height(12.dp))
                    KFab(onClick = { newEntry() }, icon = Icons.Rounded.Add, desc = "新建密码")
                }
            }
        }
    ) { _ ->
        when (tab) {
            0 -> EntryListScreen(
                q = q,
                cat = cat,
                banner = {
                    BannerHost(
                        masterMissing = false,
                        recoveryMissing = bannerRecovery,
                        onMaster = { bannerMaster = false },
                        onRecovery = { bannerRecovery = false },
                        onDisableAll = {
                            Prefs.setBannerOff(ctx, true)
                            bannerMaster = false
                            bannerRecovery = false
                        }
                    )
                },
                key = "pwd",
                list = live,
                placeholder = "搜索",
                emptyText = "还没有记录",
                selecting = selecting,
                sel = sel,
                onToggleSelect = { selecting = !selecting; if (!selecting) sel = emptySet() },
                onToggle = { id -> sel = if (id in sel) sel - id else sel + id },
                onLongClick = { e -> longPress = e },
                onClick = { e -> viewing = e }
            )
            3 -> EntryListScreen(
                banner = null,
                key = "trash",
                q = q,
                cat = cat,
                list = trashed,
                placeholder = "搜索",
                emptyText = "回收站为空",
                selecting = selecting,
                sel = sel,
                onToggleSelect = { selecting = !selecting; if (!selecting) sel = emptySet() },
                onToggle = { id -> sel = if (id in sel) sel - id else sel + id },
                onLongClick = { e -> longPress = e },
                onClick = { e ->
                    KConfirm(
                        title = e.displayTitle(),
                        text = "回收站中的密码可以恢复，恢复后回到密码列表。",
                        confirm = "恢复",
                        dismiss = "彻底删除",
                        onConfirm = {
                            Store.vault.restore(e.id)
                            Store.save(ctx)
                        },
                        onDismiss = {
                            Store.vault.remove(e.id)
                            Store.save(ctx)
                        }
                    )
                }
            )
            1 -> TotpScreen(0)
            2 -> GeneratorScreen(
                onUsePass = { p ->
                    val e = Entry().also {
                        it.id = Vault.newId(); it.pass = p
                        it.ctime = System.currentTimeMillis(); it.utime = it.ctime
                    }
                    AppState.route = Route.Edit(e, true)
                },
                onUseName = { u ->
                    val e = Entry().also {
                        it.id = Vault.newId(); it.user = u
                        it.ctime = System.currentTimeMillis(); it.utime = it.ctime
                    }
                    AppState.route = Route.Edit(e, true)
                }
            )
            else -> SettingsScreen(
                style, onStyle, seed, onSeed, dynamic, onDynamic, onLock,
                themeMode, onThemeMode,
                onAbout = { AppState.route = Route.About }
            )
        }
        longPress?.let { e ->
            EntryActionsDialog(
                e = e,
                inTrash = e.del == 1,
                onFav = {
                    e.fav = if (e.fav == 1) 0 else 1
                    e.utime = System.currentTimeMillis()
                    Store.save(ctx)
                    longPress = null
                },
                onDelete = {
                    Store.vault.remove(e.id)
                    Store.save(ctx)
                    longPress = null
                },
                onTrash = {
                    if (e.del == 1) Store.vault.restore(e.id) else Store.vault.trash(e.id)
                    Store.save(ctx)
                    longPress = null
                },
                onClose = { longPress = null }
            )
        }
        if (showFilter) {
            CategoryFilterDialog(cat, { c -> cat = c; showFilter = false }, { showFilter = false })
        }
        viewing?.let { e ->
            DetailDialog(
                e,
                onDismiss = { viewing = null },
                onEdit = {
                    viewing = null
                    AppState.route = Route.Edit(e, false)
                }
            )
        }
        ConfirmHost()
    }
}

data class ConfirmSpec(
    val title: String,
    val text: String,
    val confirm: String,
    val dismiss: String,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit
)

var confirmSpec by mutableStateOf<ConfirmSpec?>(null)

fun KConfirm(
    title: String,
    text: String,
    confirm: String,
    dismiss: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    confirmSpec = ConfirmSpec(title, text, confirm, dismiss, onConfirm, onDismiss)
}

@Composable
fun ConfirmHost() {
    val spec = confirmSpec ?: return
    KDialog(
        title = spec.title,
        onDismiss = { confirmSpec = null },
        content = { KText(spec.text, txt = Txt.Body2) },
        buttons = {
            KButton(spec.dismiss, {
                confirmSpec = null
                spec.onDismiss()
            }, Modifier.weight(1f), primary = false)
            KButton(spec.confirm, {
                confirmSpec = null
                spec.onConfirm()
            }, Modifier.weight(1f))
        }
    )
}

@Composable
fun SelectionBar(
    count: Int,
    trashMode: Boolean,
    onFav: () -> Unit,
    onTrash: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KButton(
                "收藏",
                onClick = onFav,
                modifier = Modifier.weight(1f),
                enabled = count > 0,
                primary = false
            )
            if (!trashMode) {
                KButton(
                    "移入回收站",
                    onClick = onTrash,
                    modifier = Modifier.weight(1f),
                    enabled = count > 0,
                    primary = false
                )
            }
            KButton(
                "彻底删除",
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                enabled = count > 0
            )
        }
    }
}

object Yiyan {
    private val local = arrayOf(
        "山有木兮木有枝。", "海上生明月，天涯共此时。", "人生若只如初见。",
        "落霞与孤鹜齐飞。", "行到水穷处，坐看云起时。", "此心安处是吾乡。",
        "人间有味是清欢。", "一蓑烟雨任平生。", "千里共婵娟。"
    )

    var cached: String? = null

    suspend fun get(): String {
        cached?.let { return it }
        val v = fetch()
        cached = v
        return v
    }

    suspend fun fetch(): String = withContext(Dispatchers.IO) {
        try {
            val conn = java.net.URL("https://v1.hitokoto.cn/?encode=json&c=i&max_length=15")
                .openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            conn.disconnect()
            val m = Regex("\"hitokoto\":\"(.*?)\"").find(body)
            val s = m?.groupValues?.get(1)
            if (s != null && s.all { it.code in 0x4E00..0x9FFF || "，。、；：？！“”‘’（）《》—…·".contains(it) }
                && s.count { it.code in 0x4E00..0x9FFF } <= 15
            ) s else local.random()
        } catch (e: Exception) {
            local.random()
        }
    }
}

@Composable
fun EntryListScreen(
    banner: (@Composable () -> Unit)?,
    key: String,
    list: List<Entry>,
    placeholder: String,
    emptyText: String,
    selecting: Boolean,
    sel: Set<String>,
    q: String,
    cat: String,
    onToggleSelect: () -> Unit,
    onToggle: (String) -> Unit,
    onClick: (Entry) -> Unit,
    onLongClick: (Entry) -> Unit = {}
) {
    val ver = Store.version
    val filtered = remember(list, q, cat, key, ver) {
        list.filter { e ->
            (q.isEmpty() || (e.title + e.user + e.primaryUrl() + e.tags + e.pass).contains(q, true)) &&
                    (cat.isEmpty() || (if (cat == "__none__") e.cat.isEmpty() else e.cat == cat))
        }.sortedWith(compareByDescending<Entry> { it.fav }.thenBy { it.title })
    }

    KCard(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        padding = 0.dp
    ) {
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KText(
                    if (q.isEmpty() && cat.isEmpty()) emptyText else "没有匹配的记录",
                    txt = Txt.Body2,
                    color = secondaryColor()
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = LocalTopInset.current + 8.dp,
                    bottom = LocalBottomInset.current
                )
            ) {
                items(filtered, key = { it.id }) { e ->
                    EntryRow(
                        e = e,
                        checked = if (selecting) e.id in sel else null,
                        onToggle = { onToggle(e.id) },
                        onClick = { onClick(e) },
                        onLongClick = { onLongClick(e) }
                    )
                    KDivider(Modifier.padding(start = if (selecting) 86.dp else 62.dp))
                }
            }
        }
    }
}

@Composable
fun SearchRow(
    q: String,
    onQ: (String) -> Unit,
    placeholder: String,
    selecting: Boolean,
    onToggleSelect: () -> Unit,
    onFilter: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 8.dp, top = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KField(
            value = q,
            onChange = onQ,
            label = placeholder,
            modifier = Modifier.weight(1f),
            asPlaceholder = true,
            cornerRadius = 28.dp
        )
        Spacer(Modifier.width(2.dp))
        KIconButton(Icons.Rounded.FilterList, "筛选", onFilter)
        KIconButton(
            if (selecting) Icons.Rounded.Close else Icons.Rounded.Checklist,
            if (selecting) "退出多选" else "多选",
            onToggleSelect
        )
    }
}

@Composable
fun EntryActionsDialog(
    e: Entry,
    inTrash: Boolean,
    onFav: () -> Unit,
    onDelete: () -> Unit,
    onTrash: () -> Unit,
    onClose: () -> Unit
) {
    KDialog(
        title = e.displayTitle(),
        onDismiss = onClose,
        content = {
            ActionRow(if (e.fav == 1) "取消收藏" else "收藏", onFav)
            ActionRow("彻底删除", onDelete)
            ActionRow(if (inTrash) "恢复到密码库" else "加入回收站", onTrash)
        },
        buttons = { KButton("关闭", onClose, Modifier.fillMaxWidth(), primary = false) }
    )
}

@Composable
fun ActionRow(text: String, onClick: () -> Unit) {
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KText(text, txt = Txt.Body1)
    }
}

@Composable
fun CategoryFilterDialog(cat: String, onPick: (String) -> Unit, onClose: () -> Unit) {
    KDialog(
        title = "按分类筛选",
        onDismiss = onClose,
        content = {
            FilterRow("全部", cat.isEmpty()) { onPick("") }
            FilterRow("未分类", cat == "__none__") { onPick("__none__") }
            Store.vault.cats.forEach { c -> FilterRow(c.name, cat == c.id) { onPick(c.id) } }
        },
        buttons = { KTextButton("关闭", onClose, Modifier.weight(1f)) }
    )
}

@Composable
fun BannerHost(
    masterMissing: Boolean,
    recoveryMissing: Boolean,
    onMaster: () -> Unit,
    onRecovery: () -> Unit,
    onDisableAll: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        if (masterMissing) {
            NoticeBanner(
                text = "未设置主密码，仅用本机密钥加密",
                onClose = onMaster,
                onDontShow = onDisableAll
            )
            Spacer(Modifier.height(6.dp))
        }
        if (recoveryMissing) {
            NoticeBanner(
                text = "还没有设置安全问题，忘记主密码可能无法找回",
                onClose = onRecovery,
                onDontShow = onDisableAll
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
fun NoticeBanner(text: String, onClose: () -> Unit, onDontShow: () -> Unit) {
    var dontShow by remember { mutableStateOf(false) }
    val bg = MaterialTheme.colorScheme.errorContainer
    val fg = MaterialTheme.colorScheme.onErrorContainer
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KText(text, modifier = Modifier.weight(1f), txt = Txt.Footnote2, color = fg)
            KIconButton(Icons.Rounded.Close, "关闭", {
                if (dontShow) onDontShow() else onClose()
            })
        }
        Row(
            Modifier.clickable { dontShow = !dontShow }.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KCheckbox(dontShow, { dontShow = it })
            Spacer(Modifier.width(4.dp))
            KText("不再提示", txt = Txt.Footnote2, color = fg)
        }
    }
}

@Composable
fun FilterRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KText(
            text,
            modifier = Modifier.weight(1f),
            txt = Txt.Body1,
            color = if (selected) accentColor() else null
        )
        if (selected) {
            KText("✓", txt = Txt.Body1, color = accentColor())
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EntryRow(
    e: Entry,
    checked: Boolean?,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (checked != null) onToggle() else onClick() },
                onLongClick = { if (checked == null) onLongClick() }
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (checked != null) {
            KCheckbox(checked, { onToggle() })
            Spacer(Modifier.width(6.dp))
        }
        EntryIcon(e)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            KText(
                (if (e.fav == 1) "★ " else "") + e.displayTitle(),
                txt = Txt.Body1,
                weight = androidx.compose.ui.text.font.FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            KText(
                if (e.user.isNotEmpty()) e.user else e.primaryUrl(),
                txt = Txt.Footnote2,
                color = secondaryColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (checked == null) {
            val cn = Store.vault.catName(e.cat)
            if (cn.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                KChip(cn)
            }
        }
    }
}

object BannerState {
    var visible by mutableStateOf(false)
    var dismissed by mutableStateOf(false)
}

@Composable
fun MasterBannerStrip() {
    val ctx = LocalContext.current
    var asking by remember { mutableStateOf(false) }
    var dontShow by remember { mutableStateOf(false) }
    val bg = MaterialTheme.colorScheme.errorContainer
    val fg = MaterialTheme.colorScheme.onErrorContainer

    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(bg.copy(alpha = 0.72f))
                .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KText(
                "未设置主密码，仅用本机密钥加密",
                modifier = Modifier.weight(1f),
                txt = Txt.Footnote2,
                color = fg
            )
            Box(
                Modifier.size(26.dp).clickable { asking = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, "关闭", tint = fg, modifier = Modifier.size(17.dp))
            }
        }
    }

    if (asking) {
        KDialog(
            title = "提示",
            onDismiss = { asking = false },
            content = {
                KText("关闭后不再提示。", txt = Txt.Body2)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { dontShow = !dontShow },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMiuix()) {
                        val accent = MiuixTheme.colorScheme.primary
                        val line = MiuixTheme.colorScheme.outline
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (dontShow) accent else Color.Transparent)
                                .border(1.5.dp, if (dontShow) accent else line, RoundedCornerShape(5.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dontShow) {
                                Icon(
                                    Icons.Rounded.Check,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                    } else {
                        Box(Modifier.offset(x = (-12).dp)) {
                            KCheckbox(dontShow, { dontShow = it })
                        }
                        Spacer(Modifier.width(2.dp))
                    }
                    KText("不再提醒", txt = Txt.Body2)
                }
            },
            buttons = {
                KTextButton("取消", { asking = false }, Modifier)
                KButton("关闭", {
                    if (dontShow) Prefs.setNoMasterRemind(ctx, true)
                    else BannerState.dismissed = true
                    BannerState.visible = false
                    asking = false
                }, Modifier)
            }
        )
    }
}
