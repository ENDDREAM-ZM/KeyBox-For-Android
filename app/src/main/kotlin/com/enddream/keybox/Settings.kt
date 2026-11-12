package com.enddream.keybox

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.enddream.keybox.core.Exporter
import com.enddream.keybox.core.VaultCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    style: UiStyle,
    onStyle: (UiStyle) -> Unit,
    seed: Int,
    onSeed: (Int) -> Unit,
    dynamic: Boolean,
    onDynamic: (Boolean) -> Unit,
    onLock: () -> Unit,
    themeMode: String,
    onThemeMode: (String) -> Unit,
    onAbout: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasMaster by remember { mutableStateOf(Store.master != null) }
    var lockMin by remember { mutableIntStateOf(Prefs.lockMinutes(ctx)) }
    var retention by remember { mutableIntStateOf(Prefs.retentionDays(ctx)) }
    var bio by remember { mutableStateOf(Bio.isEnabled(ctx)) }
    var bioMsg by remember { mutableStateOf<String?>(null) }
    var showPwDialog by remember { mutableStateOf(false) }
    var showPerm by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var encrypt by remember { mutableStateOf(false) }
    var exportPw by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    var importPw by remember { mutableStateOf("") }
    var showImportPw by remember { mutableStateOf(false) }
    var importData by remember { mutableStateOf<ByteArray?>(null) }
    var pickLock by remember { mutableStateOf(false) }
    var pickRet by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    var hex by remember(seed) { mutableStateOf(String.format("#%06X", 0xFFFFFF and seed)) }
    var hexMsg by remember { mutableStateOf<String?>(null) }
    var askRemove by remember { mutableStateOf(false) }
    var removePw by remember { mutableStateOf("") }
    var removeErr by remember { mutableStateOf<String?>(null) }
    var pickTheme by remember { mutableStateOf(false) }
    var pickClip by remember { mutableStateOf(false) }
    var clipSec by remember { mutableIntStateOf(Prefs.clipboardSeconds(ctx)) }
    var resumeTick by remember { mutableIntStateOf(0) }
    OnResume { resumeTick++ }
    val afOn = remember(resumeTick) { Autofill.serviceEnabled(ctx) }
    val qfOn = remember(resumeTick) { Autofill.quickFillEnabled(ctx) }
    var shot by remember { mutableStateOf(Prefs.screenshot(ctx)) }

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val data = pending
        pending = null
        if (uri != null && data != null) {
            busy = "正在写入…"
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(data) }
                    }
                } catch (t: Throwable) {
                }
                busy = null
            }
        }
    }
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = "正在读取文件…"
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    try {
                        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (t: Throwable) {
                        null
                    }
                }
                busy = null
                if (bytes != null) {
                    importData = bytes
                    val kind = VaultCrypto.containerKind(bytes)
                    if (kind == 0 || kind == 1) {
                        showImportPw = true
                    } else {
                        busy = "正在导入…"
                        withContext(Dispatchers.IO) {
                            try {
                                Exporter.importAny(null, bytes, Store.vault, true)
                                EntryFix.normalizeAll()
                                Store.save(ctx)
                            } catch (t: Throwable) {
                            }
                        }
                        busy = null
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(top = LocalTopInset.current + 2.dp, bottom = LocalBottomInset.current + 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        KSection("主密码")
        KCard(padding = 0.dp) {
            Column(Modifier.padding(12.dp)) {
                KText(
                    if (hasMaster) "已设置主密码" else "未设置主密码，仅用本机密钥加密",
                    txt = Txt.Footnote2,
                    color = if (hasMaster) secondaryColor() else errorColor()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KButton(
                        if (hasMaster) "修改主密码" else "设置主密码",
                        { showPwDialog = true },
                        Modifier.weight(1f)
                    )
                    if (hasMaster) {
                        KButton("移除主密码", {
                            removePw = ""
                            removeErr = null
                            askRemove = true
                        }, Modifier.weight(1f), primary = false)
                    }
                }
            }
        }

        KSection("解锁")
        KCard(padding = 0.dp) {
            KSwitchRow(
                title = "生物识别解锁",
                subtitle = if (bio) "已启用，解锁时可使用生物识别" else "未启用，解锁需输入主密码",
                icon = Icons.Rounded.Fingerprint,
                checked = bio,
                onChange = { on ->
                    if (on && Store.master == null) {
                        bio = false
                        bioMsg = "请先设置主密码，再开启生物识别解锁"
                        showPwDialog = true
                    } else if (on) {
                        val act = ctx as? Activity
                        if (act == null) {
                            bio = false
                            bioMsg = "当前环境无法验证指纹"
                        } else {
                            bio = false
                            Bio.enable(act, Store.master ?: "", {
                                bio = true
                                bioMsg = null
                            }, { m ->
                                bio = false
                                if (m != "已取消") bioMsg = m
                            })
                        }
                    } else {
                        Bio.disable(ctx)
                        bio = false
                        bioMsg = null
                    }
                }
            )
            KDivider(Modifier.padding(start = 56.dp))
            KRow(
                title = "生物识别状态",
                subtitle = Bio.statusText(ctx),
                icon = Icons.Rounded.VerifiedUser
            )
            bioMsg?.let {
                KDivider(Modifier.padding(start = 56.dp))
                Column(Modifier.padding(12.dp)) {
                    KText(it, txt = Txt.Footnote2, color = errorColor())
                }
            }
        }

        KSection("安全")
        KCard(padding = 0.dp) {
            KRow(
                title = "自动锁定",
                subtitle = "退到后台超过设定时间后需要重新解锁",
                value = "$lockMin 分钟",
                icon = Icons.Rounded.Lock,
                onClick = { pickLock = true }
            )
            KDivider(Modifier.padding(start = 56.dp))
            KRow(
                title = "回收站保留",
                subtitle = "超期后自动彻底删除",
                value = "$retention 天",
                icon = Icons.Rounded.Storage,
                onClick = { pickRet = true }
            )
            KDivider(Modifier.padding(start = 56.dp))
            KRow(
                title = "剪贴板自动清除",
                subtitle = "复制密码后到时间自动清空剪贴板",
                value = when (clipSec) {
                    0 -> "从不"
                    60 -> "1 分钟"
                    300 -> "5 分钟"
                    else -> "30 秒"
                },
                icon = Icons.Rounded.ContentPaste,
                onClick = { pickClip = true }
            )
            KDivider(Modifier.padding(start = 56.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tileBg = if (isMiuix()) MiuixTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.secondaryContainer
                val tint = if (isMiuix()) MiuixTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSecondaryContainer
                Row(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tileBg),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) { Icon(Icons.Rounded.Screenshot, null, Modifier.size(18.dp), tint = tint) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    KText("允许屏幕截图", txt = Txt.Body1)
                    KText("关闭时，系统截图和录屏无法获取本应用内容", txt = Txt.Footnote2, color = secondaryColor())
                }
                KSwitch(shot, {
                    shot = it
                    Prefs.setScreenshot(ctx, it)
                    (ctx as? Activity)?.let { a ->
                        if (it) a.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        else a.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                })
            }
        }

        KSection("自动填充")
        KCard(padding = 0.dp) {
            KSwitchRow(
                title = "自动填充服务",
                subtitle = if (afOn) "已开启，登录界面可直接填入账号密码" else "开启后，网页和应用的登录框会显示 KeyBox 条目",
                icon = Icons.Rounded.Lock,
                checked = afOn,
                onChange = { Autofill.openAutofillSettings(ctx) }
            )
            KDivider(Modifier.padding(start = 56.dp))
            KSwitchRow(
                title = "快速填充",
                subtitle = if (qfOn) "已开启，进入登录界面会自动弹出可用条目" else "开启无障碍后，进入登录界面自动弹出所有可用密码",
                icon = Icons.Rounded.TouchApp,
                checked = qfOn,
                onChange = { on ->
                    Autofill.setQuickWanted(ctx, on)
                    Autofill.openAccessibilitySettings(ctx)
                }
            )
        }

        KSection("外观")
        KCard(padding = 0.dp) {
            KSwitchRow(
                title = "Miuix 风格",
                subtitle = "关闭为 Material 3",
                icon = Icons.Rounded.Palette,
                checked = style == UiStyle.Miuix,
                onChange = { onStyle(if (it) UiStyle.Miuix else UiStyle.Material) }
            )
            KDivider(Modifier.padding(start = 56.dp))
            KSwitchRow(
                title = "跟随系统动态取色",
                subtitle = if (Build.VERSION.SDK_INT >= 31) "读取系统壁纸配色" else "需要 Android 12 及以上",
                icon = Icons.Rounded.Palette,
                checked = dynamic,
                enabled = Build.VERSION.SDK_INT >= 31,
                onChange = { onDynamic(it) }
            )
            KDivider(Modifier.padding(start = 56.dp))
            KRow(
                title = "深浅色",
                subtitle = "跟随系统 / 始终浅色 / 始终深色",
                value = when (themeMode) {
                    "light" -> "浅色"
                    "dark" -> "深色"
                    else -> "跟随系统"
                },
                icon = Icons.Rounded.Contrast,
                onClick = { pickTheme = true }
            )
            KDivider(Modifier.padding(start = 56.dp))
            Column(Modifier.padding(12.dp)) {
                KText("主题取色", txt = Txt.Body1)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    PresetSeeds.forEach { c ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(Color(c), CircleShape)
                                .clickable {
                                    onDynamic(false)
                                    onSeed(c)
                                    hexMsg = null
                                }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KField(hex, { hex = it }, "自定义色号 #RRGGBB", Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    KButton("应用", {
                        val v = hex.trim().removePrefix("#")
                        val n = if (v.length == 6) v.toLongOrNull(16) else null
                        if (n == null) {
                            hexMsg = "请输入 6 位十六进制色号，如 #006A60"
                        } else {
                            hexMsg = null
                            onDynamic(false)
                            onSeed((0xFF000000L or n).toInt())
                        }
                    })
                }
                hexMsg?.let {
                    Spacer(Modifier.height(6.dp))
                    KText(it, txt = Txt.Footnote2, color = errorColor())
                }
            }
        }

        KSection("数据")
        KCard(padding = 0.dp) {
            Column(Modifier.padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KButton("导出密码", { showExport = true }, Modifier.weight(1f))
                    KButton("导入密码", { openDoc.launch(arrayOf("*/*")) }, Modifier.weight(1f), primary = false)
                }
                busy?.let {
                    Spacer(Modifier.height(8.dp))
                    KText(it, txt = Txt.Footnote2, color = secondaryColor())
                }
            }
        }

        KSection("其他")
        KCard(padding = 0.dp) {
            KRow("权限", icon = Icons.Rounded.Key, onClick = { showPerm = true })
            KDivider(Modifier.padding(start = 56.dp))
            KRow("立即锁定", icon = Icons.Rounded.Lock, onClick = onLock)
            KDivider(Modifier.padding(start = 56.dp))
            KRow("关于", icon = Icons.Rounded.Info, onClick = onAbout)
        }
        Spacer(Modifier.height(10.dp))
    }

    if (askRemove) {
        KDialog(
            title = "移除主密码",
            onDismiss = { askRemove = false },
            content = {
                KText("移除后密码库会变成明文保存，请先输入当前主密码确认。", txt = Txt.Body2)
                Spacer(Modifier.height(10.dp))
                KField(removePw, { removePw = it }, "当前主密码", Modifier.fillMaxWidth(), password = true)
                removeErr?.let {
                    Spacer(Modifier.height(8.dp))
                    KText(it, txt = Txt.Footnote2, color = errorColor())
                }
            },
            buttons = {
                KTextButton("取消", { askRemove = false }, Modifier.weight(1f))
                KButton("移除", {
                    if (removePw.isEmpty()) {
                        removeErr = "请输入当前主密码"
                    } else if (removePw != Store.master) {
                        removeErr = "主密码不正确"
                        removePw = ""
                    } else {
                        Store.clearMaster(ctx)
                        hasMaster = false
                        if (Bio.isEnabled(ctx)) {
                            Bio.disable(ctx)
                            bio = false
                        }
                        removeErr = null
                        askRemove = false
                    }
                }, Modifier.weight(1f))
            }
        )
    }
    if (pickClip) {
        KDialog(
            title = "剪贴板自动清除",
            onDismiss = { pickClip = false },
            content = {
                listOf(30 to "30 秒", 60 to "1 分钟", 300 to "5 分钟", 0 to "从不").forEach { (v, n) ->
                    FilterRow(n, clipSec == v) {
                        clipSec = v
                        Prefs.setClipboardSeconds(ctx, v)
                        pickClip = false
                    }
                }
            },
            buttons = { KTextButton("关闭", { pickClip = false }, Modifier.weight(1f)) }
        )
    }
    if (pickTheme) {
        KDialog(
            title = "深浅色",
            onDismiss = { pickTheme = false },
            content = {
                listOf("system" to "跟随系统", "light" to "始终浅色", "dark" to "始终深色").forEach { (k, n) ->
                    FilterRow(n, themeMode == k) { onThemeMode(k); pickTheme = false }
                }
            },
            buttons = { KTextButton("关闭", { pickTheme = false }, Modifier.weight(1f)) }
        )
    }
    if (showPwDialog) {
        MasterPasswordDialog(hasMaster) {
            hasMaster = Store.master != null
            if (!hasMaster && Bio.isEnabled(ctx)) Bio.disable(ctx)
            bio = Bio.isEnabled(ctx)
            showPwDialog = false
            val act = ctx as? Activity
            if (hasMaster && bio && act != null) {
                val m = Store.master ?: ""
                Bio.enable(act, m, {
                    bio = true
                    bioMsg = null
                }, { msg ->
                    Bio.disable(ctx)
                    bio = false
                    bioMsg = "主密码已修改，生物识别需要重新绑定：" + msg
                })
            }
        }
    }
    if (pickLock) {
        KDialog(
            title = "自动锁定",
            onDismiss = { pickLock = false },
            content = {
                listOf(5, 15, 30).forEach { m ->
                    FilterRow("$m 分钟", lockMin == m) {
                        lockMin = m
                        Prefs.setLockMinutes(ctx, m)
                        pickLock = false
                    }
                }
            },
            buttons = { KTextButton("关闭", { pickLock = false }, Modifier.weight(1f)) }
        )
    }
    if (pickRet) {
        KDialog(
            title = "回收站保留",
            onDismiss = { pickRet = false },
            content = {
                listOf(7, 15, 30).forEach { d ->
                    FilterRow("$d 天", retention == d) {
                        retention = d
                        Prefs.setRetentionDays(ctx, d)
                        pickRet = false
                    }
                }
            },
            buttons = { KTextButton("关闭", { pickRet = false }, Modifier.weight(1f)) }
        )
    }
    if (showPerm) PermissionsDialog { showPerm = false }
    if (showExport) {
        ExportDialog(encrypt, { encrypt = it }, exportPw, { exportPw = it }, { kind ->
            showExport = false
            busy = "正在生成导出文件…"
            scope.launch {
                val data = withContext(Dispatchers.IO) {
                    try {
                        val plain = Exporter.exportPlain(Store.vault, kind)
                        if (encrypt) VaultCrypto.sealExport(exportPw, kind, plain, Store.iter) else plain
                    } catch (t: Throwable) {
                        null
                    }
                }
                busy = null
                if (data != null) {
                    pending = data
                    val name = when (kind) {
                        1 -> "keybox.json"
                        2 -> "keybox.csv"
                        else -> "keybox.pmv"
                    } + if (encrypt) ".enc" else ""
                    createDoc.launch(name)
                }
            }
        }, { showExport = false })
    }
    if (showImportPw) {
        KDialog(
            title = "输入文件密码",
            onDismiss = { showImportPw = false },
            content = {
                KField(importPw, { importPw = it }, "密码", Modifier.fillMaxWidth(), password = true)
            },
            buttons = {
                KTextButton("取消", { showImportPw = false }, Modifier.weight(1f))
                KButton("导入", {
                    showImportPw = false
                    val data = importData
                    if (data != null) {
                        busy = "正在导入…"
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                try {
                                    Exporter.importAny(importPw, data, Store.vault, true)
                                    EntryFix.normalizeAll()
                                    Store.save(ctx)
                                } catch (t: Throwable) {
                                }
                            }
                            busy = null
                        }
                    }
                }, Modifier.weight(1f))
            }
        )
    }
}

@Composable
fun MasterPasswordDialog(hasMaster: Boolean, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var old by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    KDialog(
        title = if (hasMaster) "修改主密码" else "设置主密码",
        onDismiss = onClose,
        content = {
            if (hasMaster) {
                KField(old, { old = it }, "原主密码", Modifier.fillMaxWidth(), password = true)
                Spacer(Modifier.height(10.dp))
            }
            KField(a, { a = it }, "新主密码", Modifier.fillMaxWidth(), password = true)
            Spacer(Modifier.height(10.dp))
            KField(b, { b = it }, "再次输入", Modifier.fillMaxWidth(), password = true)
            msg?.let {
                Spacer(Modifier.height(8.dp))
                KText(it, txt = Txt.Footnote2, color = errorColor())
            }
        },
        buttons = {
            KTextButton("取消", onClose, Modifier.weight(1f))
            KButton("确定", {
                when {
                    hasMaster && old != Store.master -> msg = "原主密码不正确"
                    a.length < 6 -> msg = "新主密码至少 6 位"
                    a != b -> msg = "两次输入不一致"
                    else -> {
                        Store.setMaster(ctx, a)
                        onClose()
                    }
                }
            }, Modifier.weight(1f))
        }
    )
}

@Composable
fun PermissionsDialog(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }
    OnResume { refresh++ }

    KDialog(
        title = "权限",
        onDismiss = onClose,
        content = {
            @Suppress("UNUSED_EXPRESSION")
            refresh
            val notifGranted = if (Build.VERSION.SDK_INT >= 33) {
                ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
            val cameraGranted = ctx.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            val bioOk = Bio.hasHardware(ctx)
            val afOn = Autofill.serviceEnabled(ctx)
            val qfOn = Autofill.quickFillEnabled(ctx)

            PermRow("网络", true) { }
            KDivider()
            PermRow("通知", notifGranted) {
                if (Build.VERSION.SDK_INT >= 33) {
                    permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openAppSettings(ctx)
                }
            }
            KDivider()
            PermRow("相机", cameraGranted) {
                permLauncher.launch(android.Manifest.permission.CAMERA)
            }
            KDivider()
            PermRow("生物识别", bioOk) {
                openBiometricSettings(ctx)
            }
            KDivider()
            PermRow("自动填充服务", afOn) {
                Autofill.openAutofillSettings(ctx)
            }
            KDivider()
            PermRow("无障碍", qfOn) {
                Autofill.openAccessibilitySettings(ctx)
            }
        },
        buttons = { KTextButton("关闭", onClose, Modifier.weight(1f)) }
    )
}

fun openBiometricSettings(c: android.content.Context) {
    try {
        val i = if (Build.VERSION.SDK_INT >= 30) Intent(Settings.ACTION_BIOMETRIC_ENROLL)
        else Intent(Settings.ACTION_SECURITY_SETTINGS)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        c.startActivity(i)
    } catch (t: Throwable) {
        openAppSettings(c)
    }
}

@Composable
fun PermRow(name: String, granted: Boolean, onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { if (!granted) onFix() }.padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KText(name, Modifier.weight(1f), txt = Txt.Body1)
        KText(
            if (granted) "已授予" else "未授予 · 去开启",
            txt = Txt.Footnote2,
            color = if (granted) accentColor() else errorColor()
        )
    }
}

fun openAppSettings(c: android.content.Context) {
    try {
        val i = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", c.packageName, null)
        )
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        c.startActivity(i)
    } catch (t: Throwable) {
    }
}

@Composable
fun ExportDialog(
    encrypt: Boolean,
    onEncrypt: (Boolean) -> Unit,
    pw: String,
    onPw: (String) -> Unit,
    onGo: (Int) -> Unit,
    onClose: () -> Unit
) {
    var kind by remember { mutableIntStateOf(0) }
    KDialog(
        title = "导出",
        onDismiss = onClose,
        content = {
            listOf(
                "KeyBox（自有格式）",
                "JSON（兼容 Bitwarden 等）",
                "CSV（兼容 Chrome / LastPass / KeePass 等）"
            ).forEachIndexed { i, s ->
                Row(
                    Modifier.fillMaxWidth().clickable { kind = i }.padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KCheckbox(kind == i, { if (it) kind = i })
                    Spacer(Modifier.width(8.dp))
                    KText(s, txt = Txt.Body2)
                }
            }
            KSwitchRow("加密导出", checked = encrypt, onChange = onEncrypt)
            if (encrypt) {
                KField(pw, onPw, "导出密码", Modifier.fillMaxWidth(), password = true)
            }
        },
        buttons = {
            KTextButton("取消", onClose, Modifier.weight(1f))
            KButton("选择位置", { onGo(kind) }, Modifier.weight(1f))
        }
    )
}
