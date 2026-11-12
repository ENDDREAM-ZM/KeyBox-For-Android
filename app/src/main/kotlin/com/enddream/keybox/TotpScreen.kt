package com.enddream.keybox

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TotpScreen(size: Int) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var editing by remember { mutableStateOf<Totp.Token?>(null) }
    var chooser by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var prefill by remember { mutableStateOf<Totp.Parsed?>(null) }
    var menuFor by remember { mutableStateOf<Totp.Token?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val text = if (res.resultCode == android.app.Activity.RESULT_OK)
            res.data?.getStringExtra(ScanActivity.EXTRA_RESULT) else null
        if (!text.isNullOrBlank()) {
            val p = Totp.parseOtpUri(text)
            if (p != null) {
                prefill = p
                creating = true
            } else {
                msg = "这个二维码不是 otpauth 格式"
            }
        }
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scanLauncher.launch(android.content.Intent(ctx, ScanActivity::class.java))
        else msg = "没有相机权限，无法扫码"
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    val list = remember(tick) { Totp.all(ctx) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(LocalTopInset.current))
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        KText("未添加动态令牌", txt = Txt.Title3)
                        Spacer(Modifier.height(8.dp))
                        KText(
                            "点击右下角 + 以添加，添加后即可使用动态令牌。",
                            txt = Txt.Body2,
                            color = secondaryColor(),
                            align = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                KCard(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp).weight(1f).fillMaxWidth(),
                    padding = 0.dp
                ) {
                    LazyColumn {
                        items(list, key = { it.id }) { t ->
                            val code = Totp.code(t.secret, t.digits, t.period, now)
                            val remain = Totp.remain(t.period, now)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (code.isNotEmpty()) {
                                            copyText(ctx, code)
                                            msg = "已复制 " + code
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                        .background(categoryColorFor(t.issuer.ifEmpty { t.account })),
                                    contentAlignment = Alignment.Center
                                ) {
                                    KText(
                                        (t.issuer.ifEmpty { t.account }.ifEmpty { "?" }).take(1).uppercase(),
                                        txt = Txt.Body1,
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    KText(
                                        t.issuer.ifEmpty { t.account.ifEmpty { "未命名" } },
                                        txt = Txt.Body1,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (t.account.isNotEmpty() && t.issuer.isNotEmpty()) {
                                        KText(
                                            t.account,
                                            txt = Txt.Footnote2,
                                            color = secondaryColor(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                KText(
                                    if (code.isEmpty()) "------"
                                    else code.chunked((code.length + 1) / 2).joinToString(" "),
                                    txt = Txt.Title3,
                                    mono = true,
                                    color = accentColor()
                                )
                                Spacer(Modifier.width(8.dp))
                                CountRing(remain = remain, period = t.period)
                                KIconButton(Icons.Rounded.MoreVert, "更多", { menuFor = t })
                            }
                            KDivider(Modifier.padding(start = 60.dp))
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = LocalBottomInset.current + 16.dp)
        ) {
            KFab(onClick = { chooser = true }, icon = Icons.Rounded.Add, desc = "添加动态令牌")
        }
    }

    if (chooser) {
        KDialog(
            title = "添加动态令牌",
            onDismiss = { chooser = false },
            content = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .clickable {
                            chooser = false
                            permLauncher.launch(Manifest.permission.CAMERA)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.QrCodeScanner, null,
                        tint = accentColor(),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    KText("扫描二维码", txt = Txt.Title3)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .clickable {
                            chooser = false
                            prefill = null
                            creating = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.Edit, null,
                        tint = accentColor(),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    KText("手动填写", txt = Txt.Title3)
                }
            },
            buttons = { KTextButton("取消", { chooser = false }, Modifier.weight(1f)) }
        )
    }

    if (creating) {
        TokenDialog(null, prefill, onClose = { creating = false; prefill = null }, onSaved = { tick++ })
    }
    editing?.let { t ->
        TokenDialog(t, null, onClose = { editing = null }, onSaved = { tick++ })
    }
    menuFor?.let { t ->
        KDialog(
            title = t.label(),
            onDismiss = { menuFor = null },
            content = {
                KText("每 ${t.period} 秒刷新一次，${t.digits} 位", txt = Txt.Body2, color = secondaryColor())
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clickable {
                    copyText(ctx, Totp.code(t.secret, t.digits, t.period))
                    menuFor = null
                }.padding(vertical = 8.dp)) {
                    KText("复制当前验证码", txt = Txt.Body1)
                }
                Row(Modifier.fillMaxWidth().clickable {
                    menuFor = null
                    editing = t
                }.padding(vertical = 8.dp)) {
                    KText("编辑", txt = Txt.Body1)
                }
            },
            buttons = {
                KTextButton("删除", {
                    Totp.remove(ctx, t.id)
                    menuFor = null
                    tick++
                }, Modifier.weight(1f))
                KButton("关闭", { menuFor = null }, Modifier.weight(1f))
            }
        )
    }

    msg?.let {
        LaunchedEffect(it) {
            delay(1800)
            msg = null
        }
    }
    if (msg != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            KCard(Modifier.padding(bottom = LocalBottomInset.current + 20.dp), padding = 10.dp) {
                KText(msg!!, txt = Txt.Footnote2)
            }
        }
    }
}

@Composable
fun TokenDialog(token: Totp.Token?, prefill: Totp.Parsed?, onClose: () -> Unit, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    var issuer by remember(token, prefill) {
        mutableStateOf(token?.issuer ?: prefill?.issuer ?: "")
    }
    var account by remember(token, prefill) {
        mutableStateOf(token?.account ?: prefill?.account ?: "")
    }
    var secret by remember(token, prefill) {
        mutableStateOf(token?.secret ?: prefill?.secret ?: "")
    }
    val digits = token?.digits ?: prefill?.digits ?: 6
    var period by remember(token, prefill) {
        mutableIntStateOf(token?.period ?: prefill?.period ?: 30)
    }
    var msg by remember { mutableStateOf<String?>(null) }

    KDialog(
        title = if (token == null) "添加动态令牌" else "编辑动态令牌",
        onDismiss = onClose,
        content = {
            KField(issuer, { issuer = it }, "名称", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            KField(account, { account = it }, "账号", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            KField(secret, {
                secret = it
                val p = Totp.parseOtpUri(it)
                if (p != null) {
                    if (p.issuer.isNotEmpty()) issuer = p.issuer
                    if (p.account.isNotEmpty()) account = p.account
                    secret = p.secret
                    period = p.period
                }
            }, "密钥 / otpauth 链接", Modifier.fillMaxWidth(), mono = true)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                KText("代码有效时间", Modifier.weight(1f), txt = Txt.Body1)
                KChoiceChip("30 秒", period == 30, { period = 30 }, Modifier.width(76.dp))
                Spacer(Modifier.width(8.dp))
                KChoiceChip("60 秒", period == 60, { period = 60 }, Modifier.width(72.dp))
            }
            msg?.let {
                Spacer(Modifier.height(8.dp))
                KText(it, txt = Txt.Footnote2, color = errorColor())
            }
        },
        buttons = {
            KTextButton("取消", onClose, Modifier.weight(1f))
            KButton("保存", {
                val clean = secret.trim().replace(" ", "").replace("-", "").uppercase()
                when {
                    clean.isEmpty() -> msg = "请填写密钥"
                    Totp.code(clean, digits, period).isEmpty() -> msg = "密钥格式不对，检查一下"
                    token == null -> {
                        Totp.add(ctx, issuer, account, clean, digits, period)
                        onSaved()
                        onClose()
                    }
                    else -> {
                        Totp.update(
                            ctx,
                            token.copy(
                                issuer = issuer, account = account, secret = clean,
                                digits = digits, period = period
                            )
                        )
                        onSaved()
                        onClose()
                    }
                }
            }, Modifier.weight(1f))
        }
    )
}

@Composable
fun CountRing(remain: Int, period: Int) {
    val track = if (isMiuix()) MiuixTheme.colorScheme.surfaceContainerHigh
    else androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
    val fg = accentColor()
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke)
            )
            drawArc(
                color = fg,
                startAngle = -90f,
                sweepAngle = 360f * remain.toFloat() / period.coerceAtLeast(1),
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke)
            )
        }
        KText("$remain", txt = Txt.Footnote2, color = secondaryColor())
    }
}
