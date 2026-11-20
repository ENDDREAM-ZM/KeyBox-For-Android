package com.enddream.keybox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.enddream.keybox.core.Entry
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class FieldDraft(
    val name: String = "",
    val type: Int = 0,
    val value: String = "",
    val copy: Boolean = true,
    val reveal: Boolean = false
)

@Composable
fun EditScreen(e: Entry, fresh: Boolean, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(e.title) }
    var user by remember { mutableStateOf(e.user) }
    var pass by remember { mutableStateOf(e.pass) }
    var note by remember { mutableStateOf(e.note) }
    var pkg by remember { mutableStateOf(e.pkg) }
    var cat by remember { mutableStateOf(e.cat) }
    var fav by remember { mutableStateOf(e.fav == 1) }
    var showPw by remember { mutableStateOf(false) }
    var catMenu by remember { mutableStateOf(false) }
    var appPicker by remember { mutableStateOf(false) }
    var gen by remember { mutableStateOf(false) }
    var newCat by remember { mutableStateOf("") }
    var askDelete by remember { mutableStateOf(false) }

    val urls = remember { mutableStateListOf<String>().apply { addAll(e.urlList()) } }
    val fieldDrafts = remember {
        mutableStateListOf<FieldDraft>().apply {
            e.fields.forEach { add(FieldDraft(it.name, it.type, it.value, it.copy == 1)) }
        }
    }

    fun save() {
        e.title = title
        e.user = user
        e.pass = pass
        e.note = note
        e.pkg = pkg
        e.cat = cat
        e.fav = if (fav) 1 else 0
        e.urls = urls.filter { it.isNotBlank() }.map { it.trim() }.toMutableList()
        e.url = e.urls.firstOrNull() ?: ""
        e.fields = fieldDrafts
            .filter { it.name.isNotBlank() }
            .map { Entry.Field(it.name.trim(), it.type, it.value, if (it.copy) 1 else 0) }
            .toMutableList()
        e.utime = System.currentTimeMillis()
        val live = Store.find(e.id)
        if (live == null) {
            Store.vault.entries.add(e)
        } else {
            live.title = e.title; live.user = e.user; live.pass = e.pass
            live.note = e.note; live.pkg = e.pkg; live.cat = e.cat
            live.fav = e.fav; live.url = e.url; live.urls = e.urls; live.utime = e.utime
            live.fields = e.fields
        }
        Store.save(ctx)
        onClose()
    }

    KScaffold(
        topBar = {
            KTopBar(
                title = if (fresh) "新建密码" else "编辑密码",
                onBack = onClose,
                actions = { KTextButton("保存", { save() }) }
            )
        },
        bottomBar = {},
        fab = {}
    ) { _ ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp)
                .padding(top = LocalTopInset.current + 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KCard(padding = 12.dp) {
                Row(
                    Modifier.fillMaxWidth().clickable { fav = !fav }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KText(if (fav) "已收藏" else "收藏", modifier = Modifier.weight(1f), txt = Txt.Body1)
                    Icon(
                        if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        "收藏",
                        tint = accentColor()
                    )
                }
                Spacer(Modifier.height(8.dp))
                KField(title, { title = it }, "标题", Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                KField(user, { user = it }, "用户名", Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                KField(
                    pass, { pass = it }, "密码", Modifier.fillMaxWidth(),
                    password = !showPw,
                    trailing = {
                        Row {
                            KTextButton(if (showPw) "隐藏" else "显示", { showPw = !showPw })
                            KTextButton("生成", { gen = true })
                        }
                    }
                )
                if (pass.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    KText("强度：" + strengthLabel(pass), txt = Txt.Footnote2, color = secondaryColor())
                }
            }

            KCard(padding = 12.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KText("网址", modifier = Modifier.weight(1f), txt = Txt.Body2)
                    KTextButton("添加网址", { urls.add("") })
                }
                urls.forEachIndexed { i, u ->
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KField(
                            u,
                            { v -> urls[i] = v },
                            "https://example.com/",
                            Modifier.weight(1f),
                            keyboardType = KeyboardType.Uri,
                            asPlaceholder = true
                        )
                        KIconButton(Icons.Rounded.Close, "删除网址", { urls.removeAt(i) })
                    }
                }
            }

            KCard(padding = 12.dp) {
                val preview = remember(pkg, title, urls.toList()) {
                    Entry().also {
                        it.pkg = pkg
                        it.title = title
                        it.urls = urls.filter { u -> u.isNotBlank() }.toMutableList()
                        it.url = it.urls.firstOrNull() ?: ""
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EntryIcon(preview, 38)
                    Spacer(Modifier.width(8.dp))
                    KField(pkg, { pkg = it }, "应用包名", Modifier.weight(1f))
                    KIconButton(Icons.Rounded.Add, "选择应用", { appPicker = true })
                }
                Spacer(Modifier.height(10.dp))
                Box {
                    KField(
                        Store.vault.catName(cat), { }, "分类", Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailing = { Icon(Icons.Rounded.ArrowDropDown, "展开") }
                    )
                    Box(Modifier.matchParentSize().clickable { catMenu = true })
                }
            }

            KCard(padding = 12.dp) {
                KField(note, { note = it }, "备注", Modifier.fillMaxWidth(), singleLine = false, minLines = 3)
            }

            KCard(padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KText("自定义字段", modifier = Modifier.weight(1f), txt = Txt.Body2)
                    KTextButton("添加字段", {
                        fieldDrafts.add(FieldDraft(type = 0, copy = true))
                    })
                }
                if (fieldDrafts.isEmpty()) {
                    KText(
                        "如「安全提问」「恢复码」「是否已开启两步验证」",
                        txt = Txt.Footnote2,
                        color = secondaryColor()
                    )
                }
                fieldDrafts.forEachIndexed { i, f ->
                    val cur = f
                    if (i > 0) {
                        Spacer(Modifier.height(8.dp))
                        KDivider(Modifier.padding(bottom = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KField(
                            cur.name,
                            { v -> fieldDrafts[i] = cur.copy(name = v) },
                            "字段名称",
                            Modifier.weight(1f)
                        )
                        KIconButton(Icons.Rounded.Close, "删除字段", { fieldDrafts.removeAt(i) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        KChoiceChip("文本", cur.type == 0, {
                            fieldDrafts[i] = cur.copy(type = 0)
                        }, Modifier.width(66.dp))
                        KChoiceChip("隐藏", cur.type == 1, {
                            fieldDrafts[i] = cur.copy(type = 1)
                        }, Modifier.width(66.dp))
                        KChoiceChip("开关", cur.type == 2, {
                            fieldDrafts[i] = cur.copy(
                                type = 2,
                                value = if (cur.value == "1") "1" else "0"
                            )
                        }, Modifier.width(66.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    if (cur.type == 2) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            KText(
                                if (cur.value == "1") "已开启" else "未开启",
                                modifier = Modifier.weight(1f),
                                txt = Txt.Body1
                            )
                            KSwitch(cur.value == "1", { on ->
                                fieldDrafts[i] = cur.copy(value = if (on) "1" else "0")
                            })
                        }
                    } else {
                        KField(
                            cur.value,
                            { v -> fieldDrafts[i] = cur.copy(value = v) },
                            if (cur.type == 1) "隐藏内容，如恢复码" else "字段内容",
                            Modifier.fillMaxWidth(),
                            password = cur.type == 1 && !cur.reveal,
                            trailing = if (cur.type == 1) {
                                {
                                    KTextButton(if (cur.reveal) "隐藏" else "显示", {
                                        fieldDrafts[i] = cur.copy(reveal = !cur.reveal)
                                    })
                                }
                            } else null
                        )
                    }
                    if (cur.type != 2) {
                        Spacer(Modifier.height(4.dp))
                        KCheckboxRow("显示一键复制按钮", cur.copy) { on ->
                            fieldDrafts[i] = cur.copy(copy = on)
                        }
                    }
                }
            }

            if (!fresh) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KButton(
                        "彻底删除",
                        { askDelete = true },
                        Modifier.weight(1f)
                    )
                    KButton(
                        "移入回收站",
                        {
                            Store.vault.trash(e.id)
                            Store.save(ctx)
                            onClose()
                        },
                        Modifier.weight(1f),
                        primary = false
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    if (askDelete) {
        KDialog(
            title = "彻底删除",
            onDismiss = { askDelete = false },
            content = { KText("删除后无法恢复，确定要彻底删除这条密码吗？", txt = Txt.Body2) },
            buttons = {
                KTextButton("取消", { askDelete = false }, Modifier.weight(1f))
                KButton("删除", {
                    askDelete = false
                    Totp.remove(ctx, e.id)
                    Store.vault.remove(e.id)
                    Store.save(ctx)
                    onClose()
                }, Modifier.weight(1f))
            }
        )
    }

    if (catMenu) {
        KDialog(
            title = "选择分类",
            onDismiss = { catMenu = false },
            content = {
                FilterRow("未分类", cat.isEmpty()) { cat = ""; catMenu = false }
                Store.vault.cats.forEach { c ->
                    FilterRow(c.name, cat == c.id) { cat = c.id; catMenu = false }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KField(newCat, { newCat = it }, "新建分类", Modifier.weight(1f))
                    KIconButton(Icons.Rounded.Add, "添加分类", {
                        val n = newCat.trim()
                        if (n.isNotEmpty()) {
                            val c = Store.vault.catAdd(n)
                            Store.save(ctx)
                            cat = c.id
                            newCat = ""
                            catMenu = false
                        }
                    })
                }
            },
            buttons = { KTextButton("关闭", { catMenu = false }, Modifier.weight(1f)) }
        )
    }
    if (appPicker) {
        InstalledAppsDialog({ appPicker = false }) { p, _ ->
            pkg = p
            appPicker = false
        }
    }
    if (gen) {
        GeneratePasswordDialog(onClose = { gen = false }, onPick = { pass = it; gen = false })
    }
}
