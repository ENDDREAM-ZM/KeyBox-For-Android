package com.enddream.keybox

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
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
import com.enddream.keybox.core.Vault

@Composable
private fun ToggleCell(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        KText(label, modifier = Modifier.weight(1f), txt = Txt.Body2)
        KSwitch(checked, onChange)
    }
}

private fun genName(len: Int, cap: Boolean, withDigits: Boolean): String {
    val word = Vault.generateUsername(0, cap, false)
    if (word.length >= len) return word.substring(0, len)
    val sb = StringBuilder(word)
    val pool = if (withDigits) "abcdefghijkmnopqrstuvwxyz23456789"
    else "abcdefghijkmnopqrstuvwxyz"
    while (sb.length < len) sb.append(pool[(0 until pool.length).random()])
    return sb.toString()
}

@Composable
private fun LenInput(value: String, onChange: (String) -> Unit) {
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val line = MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier
            .width(56.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, line, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Default
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ResultBox(text: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val line = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, line, RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun GeneratorScreen(onUsePass: (String) -> Unit, onUseName: (String) -> Unit) {
    val ctx = LocalContext.current
    var len by remember { mutableIntStateOf(20) }
    var lenText by remember { mutableStateOf("20") }
    var lower by remember { mutableStateOf(true) }
    var upper by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var minDigits by remember { mutableIntStateOf(1) }
    var minSymbols by remember { mutableIntStateOf(1) }
    var pass by remember { mutableStateOf("") }

    var cap by remember { mutableStateOf(true) }
    var withDigits by remember { mutableStateOf(false) }
    var nlen by remember { mutableIntStateOf(12) }
    var nlenText by remember { mutableStateOf("12") }
    var name by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(top = LocalTopInset.current + 4.dp, bottom = LocalBottomInset.current + 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KCard(padding = 14.dp) {
            KText("随机密码", txt = Txt.Title3)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ResultBox(pass, Modifier.weight(1f), mono = true)
                KIconButton(Icons.Rounded.ContentCopy, "复制", { copyText(ctx, pass) })
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                KText("密码长度：", Modifier.weight(1f), txt = Txt.Body2)
                LenInput(lenText) {
                    val t = it.filter { c -> c.isDigit() }.take(3)
                    val v = t.toIntOrNull()
                    when {
                        v == null -> lenText = t
                        v > 256 -> { lenText = "256"; len = 256 }
                        else -> { lenText = t; if (v in 4..256) len = v }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            KText("范围 4 - 256", txt = Txt.Footnote2, color = secondaryColor())

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ToggleCell("小写字母", lower, { lower = it }, Modifier.weight(1f))
                ToggleCell("大写字母", upper, { upper = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ToggleCell("数字", digits, { digits = it }, Modifier.weight(1f))
                ToggleCell("特殊符号", symbols, { symbols = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            StepperRow("数字最少个数", minDigits) { minDigits = it }
            StepperRow("特殊符号最少个数", minSymbols) { minSymbols = it }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KButton(
                    "生成密码",
                    { pass = Vault.generatePassword(len, lower, upper, digits, symbols, minDigits, minSymbols) },
                    Modifier.weight(1f)
                )
                KButton(
                    "用此密码新建",
                    { if (pass.isNotEmpty()) onUsePass(pass) },
                    Modifier.weight(1f),
                    enabled = pass.isNotEmpty(),
                    primary = false
                )
            }
        }

        KCard(padding = 14.dp) {
            KText("随机用户名", txt = Txt.Title3)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ResultBox(name, Modifier.weight(1f), mono = false)
                KIconButton(Icons.Rounded.ContentCopy, "复制", { copyText(ctx, name) })
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                KText("用户名长度：", Modifier.weight(1f), txt = Txt.Body2)
                LenInput(nlenText) {
                    val t = it.filter { c -> c.isDigit() }.take(2)
                    val v = t.toIntOrNull()
                    when {
                        v == null -> nlenText = t
                        v > 20 -> { nlenText = "20"; nlen = 20 }
                        else -> { nlenText = t; if (v in 3..20) nlen = v }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            KText("范围 3 - 20", txt = Txt.Footnote2, color = secondaryColor())
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ToggleCell("首字母大写", cap, { cap = it }, Modifier.weight(1f))
                ToggleCell("包含数字", withDigits, { withDigits = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KButton(
                    "生成用户名",
                    { name = genName(nlen, cap, withDigits) },
                    Modifier.weight(1f)
                )
                KButton(
                    "用此用户名新建",
                    { if (name.isNotEmpty()) onUseName(name) },
                    Modifier.weight(1f),
                    enabled = name.isNotEmpty(),
                    primary = false
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
