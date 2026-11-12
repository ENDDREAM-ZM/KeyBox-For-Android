package com.enddream.keybox

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.enddream.keybox.core.Entry
import com.enddream.keybox.core.Vault
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DetailDialog(e: Entry, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val ctx = LocalContext.current
    var showPw by remember { mutableStateOf(false) }
    var fav by remember { mutableStateOf(e.fav == 1) }
    val urls = e.urlList()

    KDialog(
        title = e.displayTitle(),
        onDismiss = onDismiss,
        content = {
            Row(
                Modifier.fillMaxWidth().clickable {
                    fav = !fav
                    Store.find(e.id)?.let { it.fav = if (fav) 1 else 0 }
                    Store.save(ctx)
                }.padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KText(if (fav) "已收藏" else "收藏", modifier = Modifier.weight(1f), txt = Txt.Body1)
                Icon(
                    if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    "收藏",
                    tint = accentColor()
                )
            }
            KDivider()
            InfoRow("用户名", e.user) { copyText(ctx, e.user) }
            KText("密码", txt = Txt.Footnote2, color = secondaryColor())
            Row(verticalAlignment = Alignment.CenterVertically) {
                KText(
                    if (showPw) e.pass else "•".repeat(e.pass.length.coerceAtLeast(1)),
                    modifier = Modifier.weight(1f),
                    txt = Txt.Body1,
                    mono = true
                )
                KTextButton(if (showPw) "隐藏" else "显示", { showPw = !showPw })
                KIconButton(Icons.Rounded.ContentCopy, "复制密码", { copyText(ctx, e.pass) })
            }
            KDivider()
            if (urls.isEmpty()) {
                InfoRow("网址", "") { }
            } else {
                KText("网址", txt = Txt.Footnote2, color = secondaryColor())
                urls.forEach { u ->
                    KText(
                        u,
                        modifier = Modifier.fillMaxWidth().clickable { openUrl(ctx, u) }.padding(vertical = 6.dp),
                        txt = Txt.Body1,
                        color = accentColor()
                    )
                }
            }
            if (e.pkg.isNotEmpty()) {
                InfoRow("应用", e.pkg) { launchApp(ctx, e.pkg) }
            }
            InfoRow("分类", Store.vault.catName(e.cat)) { }
            if (e.tags.isNotEmpty()) InfoRow("标签", e.tags) { }
            InfoRow("备注", e.note) { copyText(ctx, e.note) }
            InfoRow("密码强度", strengthLabel(e.pass)) { }
        },
        buttons = {
            KButton("关闭", onDismiss, Modifier.weight(1f), primary = false)
            KButton("编辑", onEdit, Modifier.weight(1f))
        }
    )
}

@Composable
fun InfoRow(label: String, value: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 6.dp)) {
        KText(label, txt = Txt.Footnote2, color = secondaryColor())
        KText(nz(value), txt = Txt.Body1)
    }
}

@Composable
fun InstalledAppsDialog(onClose: () -> Unit, onPick: (String, String) -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    var withSystem by remember { mutableStateOf(Prefs.showSystemApps(ctx)) }
    val apps = remember(withSystem) {
        val list = mutableListOf<Pair<String, String>>()
        try {
            val pm = ctx.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0).forEach { ri ->
                val p = ri.activityInfo.packageName
                val ai = ri.activityInfo.applicationInfo
                val sys = (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                if (p != ctx.packageName && (!sys || withSystem) && list.none { it.first == p }) {
                    list.add(p to pm.getApplicationLabel(ai).toString())
                }
            }
            list.sortBy { it.second.lowercase() }
        } catch (t: Throwable) {
        }
        list
    }
    val filtered = remember(apps, q) {
        if (q.isBlank()) apps
        else apps.filter { it.first.contains(q, true) || it.second.contains(q, true) }
    }

    KDialog(
        title = "选择应用",
        onDismiss = onClose,
        content = {
            KField(
                value = q,
                onChange = { q = it },
                label = "搜索应用名或包名",
                modifier = Modifier.fillMaxWidth(),
                asPlaceholder = true
            )
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth().clickable {
                    withSystem = !withSystem
                    Prefs.setShowSystemApps(ctx, withSystem)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                KCheckbox(withSystem, {
                    withSystem = it
                    Prefs.setShowSystemApps(ctx, it)
                })
                Spacer(Modifier.width(6.dp))
                KText("显示系统应用", txt = Txt.Footnote2)
            }
            Spacer(Modifier.height(6.dp))
            if (filtered.isEmpty()) {
                KText("没有匹配的应用", txt = Txt.Body2, color = secondaryColor())
            } else {
                LazyColumn(Modifier.height(300.dp)) {
                    items(filtered) { (p, n) ->
                        Column(Modifier.fillMaxWidth().clickable { onPick(p, n) }.padding(vertical = 7.dp)) {
                            KText(n, txt = Txt.Body1)
                            KText(p, txt = Txt.Footnote2, color = secondaryColor())
                        }
                    }
                }
            }
        },
        buttons = { KTextButton("取消", onClose, Modifier.weight(1f)) }
    )
}

@Composable
fun GeneratePasswordDialog(onClose: () -> Unit, onPick: (String) -> Unit) {
    var len by remember { mutableIntStateOf(20) }
    var lower by remember { mutableStateOf(true) }
    var upper by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var minDigits by remember { mutableIntStateOf(1) }
    var minSymbols by remember { mutableIntStateOf(1) }

    val result = remember(len, lower, upper, digits, symbols, minDigits, minSymbols) {
        Vault.generatePassword(len, lower, upper, digits, symbols, minDigits, minSymbols)
    }

    KDialog(
        title = "生成密码",
        onDismiss = onClose,
        content = {
            KText(result, txt = Txt.Body1, mono = true)
            Spacer(Modifier.height(8.dp))
            KText("长度：$len", txt = Txt.Body2)
            KSlider(len.toFloat(), { len = it.toInt() }, 4f..60f, Modifier.fillMaxWidth())
            KSwitchRow("小写字母", checked = lower, onChange = { lower = it })
            KSwitchRow("大写字母", checked = upper, onChange = { upper = it })
            KSwitchRow("数字", checked = digits, onChange = { digits = it })
            KSwitchRow("特殊符号", checked = symbols, onChange = { symbols = it })
            StepperRow("数字最少个数", minDigits) { minDigits = it }
            StepperRow("特殊符号最少个数", minSymbols) { minSymbols = it }
        },
        buttons = {
            KTextButton("取消", onClose, Modifier.weight(1f))
            KButton("使用", { onPick(result) }, Modifier.weight(1f))
        }
    )
}

@Composable
fun StepperRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        KText(label, Modifier.weight(1f), txt = Txt.Body1)
        KRoundIconButton(Icons.Rounded.Remove, "减少", { if (value > 0) onChange(value - 1) })
        KText("$value", Modifier.padding(horizontal = 10.dp), txt = Txt.Body1)
        KRoundIconButton(Icons.Rounded.Add, "增加", { if (value < 32) onChange(value + 1) })
    }
}
