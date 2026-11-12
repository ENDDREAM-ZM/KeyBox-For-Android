package com.enddream.keybox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import top.yukonga.miuix.kmp.basic.Button as MxButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MxButtonDefaults
import top.yukonga.miuix.kmp.basic.Card as MxCard
import top.yukonga.miuix.kmp.basic.Checkbox as MxCheckbox
import top.yukonga.miuix.kmp.basic.FloatingActionButton as MxFab
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MxDivider
import top.yukonga.miuix.kmp.basic.IconButton as MxIconButton
import top.yukonga.miuix.kmp.basic.NavigationBar as MxNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MxNavigationBarItem
import top.yukonga.miuix.kmp.basic.Slider as MxSlider
import top.yukonga.miuix.kmp.basic.SmallTitle as MxSmallTitle
import top.yukonga.miuix.kmp.basic.Switch as MxSwitch
import top.yukonga.miuix.kmp.basic.Text as MxText
import top.yukonga.miuix.kmp.basic.TextButton as MxTextButton
import top.yukonga.miuix.kmp.basic.TextField as MxTextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog as MxOverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost

val LocalStyle = staticCompositionLocalOf { UiStyle.Material }

@Composable
fun isMiuix(): Boolean = LocalStyle.current == UiStyle.Miuix

enum class Txt { Title1, Title2, Title3, Body1, Body2, Footnote1, Footnote2, Button, Main }

@Composable
private fun mxStyle(t: Txt): TextStyle = when (t) {
    Txt.Title1 -> MiuixTheme.textStyles.title1
    Txt.Title2 -> MiuixTheme.textStyles.title2
    Txt.Title3 -> MiuixTheme.textStyles.title3
    Txt.Body1 -> MiuixTheme.textStyles.body1
    Txt.Body2 -> MiuixTheme.textStyles.body2
    Txt.Footnote1 -> MiuixTheme.textStyles.footnote1
    Txt.Footnote2 -> MiuixTheme.textStyles.footnote2
    Txt.Button -> MiuixTheme.textStyles.button
    Txt.Main -> MiuixTheme.textStyles.main
}

@Composable
private fun mdStyle(t: Txt): TextStyle = when (t) {
    Txt.Title1 -> TextStyle(fontFamily = FontFamily.Default, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    Txt.Title2 -> TextStyle(fontFamily = FontFamily.Default, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    Txt.Title3 -> TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    Txt.Body1 -> TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp)
    Txt.Body2 -> TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp)
    Txt.Footnote1 -> TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp)
    Txt.Footnote2 -> TextStyle(fontFamily = FontFamily.Default, fontSize = 11.sp, lineHeight = 15.sp)
    Txt.Button -> TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    Txt.Main -> TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp)
}

@Composable
fun KText(
    text: String,
    modifier: Modifier = Modifier,
    txt: Txt = Txt.Body1,
    color: Color? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    mono: Boolean = false,
    weight: FontWeight? = null,
    align: TextAlign? = null
) {
    if (isMiuix()) {
        MxText(
            text = text,
            modifier = modifier,
            color = color ?: MiuixTheme.colorScheme.onSurface,
            fontWeight = weight,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            textAlign = align,
            maxLines = maxLines,
            overflow = overflow,
            style = mxStyle(txt)
        )
    } else {
        Text(
            text = text,
            modifier = modifier,
            color = color ?: MaterialTheme.colorScheme.onSurface,
            style = mdStyle(txt),
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            fontWeight = weight,
            textAlign = align,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}

@Composable
fun KCard(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    if (isMiuix()) {
        MxCard(
            modifier = modifier,
            cornerRadius = 18.dp,
            insideMargin = PaddingValues(padding)
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(Modifier.padding(padding)) { content() }
        }
    }
}

@Composable
fun KSection(text: String, modifier: Modifier = Modifier) {
    if (isMiuix()) {
        MxSmallTitle(
            text = text,
            modifier = modifier.padding(start = 4.dp, top = 14.dp, bottom = 5.dp)
        )
    } else {
        Text(
            text,
            modifier.padding(start = 4.dp, top = 14.dp, bottom = 5.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Default
        )
    }
}

@Composable
fun KSwitch(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    if (isMiuix()) {
        MxSwitch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    } else {
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
fun KCheckboxRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMiuix()) {
            KCheckbox(checked, onChange)
            Spacer(Modifier.width(8.dp))
        } else {
            Box(Modifier.offset(x = (-12).dp)) {
                KCheckbox(checked, onChange)
            }
            Spacer(Modifier.width(2.dp))
        }
        KText(text, txt = Txt.Body2)
    }
}

@Composable
fun KCheckbox(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    if (isMiuix()) {
        MxCheckbox(
            state = if (checked) ToggleableState.On else ToggleableState.Off,
            onClick = { onChange(!checked) },
            enabled = enabled
        )
    } else {
        Checkbox(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
fun KField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    password: Boolean = false,
    readOnly: Boolean = false,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    asPlaceholder: Boolean = false,
    mono: Boolean = false,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    cornerRadius: Dp? = null
) {
    if (isMiuix()) {
        MxTextField(
            value = value,
            onValueChange = onChange,
            modifier = if (cornerRadius != null)
                modifier.clip(RoundedCornerShape(cornerRadius)) else modifier,
            insideMargin = DpSize(12.dp, 10.dp),
            label = label,
            useLabelAsPlaceholder = asPlaceholder,
            enabled = enabled,
            readOnly = readOnly,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
            trailingIcon = trailing,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = if (mono) MiuixTheme.textStyles.main.copy(fontFamily = FontFamily.Monospace)
            else MiuixTheme.textStyles.main,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = modifier,
            label = if (asPlaceholder) null else ({ Text(label, fontFamily = FontFamily.Default) }),
            placeholder = if (asPlaceholder) ({ Text(label, fontFamily = FontFamily.Default) }) else null,
            singleLine = singleLine,
            readOnly = readOnly,
            enabled = enabled,
            minLines = minLines,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
            textStyle = if (mono) MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            else MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Default),
            trailingIcon = trailing,
            shape = if (cornerRadius != null) RoundedCornerShape(cornerRadius)
            else OutlinedTextFieldDefaults.shape
        )
    }
}

@Composable
fun KButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true
) {
    if (isMiuix()) {
        MxButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            minHeight = 38.dp,
            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            colors = if (primary) MxButtonDefaults.buttonColorsPrimary() else MxButtonDefaults.buttonColors()
        ) { MxText(text, style = MiuixTheme.textStyles.button) }
    } else {
        if (primary) {
            Button(onClick = onClick, modifier = modifier, enabled = enabled) {
                Text(text, fontFamily = FontFamily.Default)
            }
        } else {
            OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) {
                Text(text, fontFamily = FontFamily.Default)
            }
        }
    }
}

@Composable
fun KTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (isMiuix()) {
        MxTextButton(
            text = text,
            onClick = onClick,
            modifier = modifier.heightIn(min = 38.dp),
            enabled = enabled,
            minHeight = 38.dp,
            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        )
    } else {
        TextButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 38.dp),
            enabled = enabled
        ) {
            Text(text, fontFamily = FontFamily.Default)
        }
    }
}

@Composable
fun KIconButton(icon: ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (isMiuix()) {
        MxIconButton(onClick = onClick, modifier = modifier) {
            Icon(icon, desc, tint = MiuixTheme.colorScheme.onSurface)
        }
    } else {
        IconButton(onClick = onClick, modifier = modifier) { Icon(icon, desc) }
    }
}

@Composable
fun KRoundIconButton(icon: ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (isMiuix()) MiuixTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.secondaryContainer
    Box(
        modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            desc,
            Modifier.size(18.dp),
            tint = if (isMiuix()) MiuixTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun KSlider(
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    if (isMiuix()) {
        MxSlider(value = value, onValueChange = onChange, modifier = modifier, valueRange = range)
    } else {
        Slider(value = value, onValueChange = onChange, modifier = modifier, valueRange = range)
    }
}

@Composable
fun KDivider(modifier: Modifier = Modifier, start: Dp = 0.dp) {
    if (isMiuix()) {
        MxDivider(modifier.padding(start = start), color = MiuixTheme.colorScheme.dividerLine.copy(alpha = 0.6f))
    } else {
        HorizontalDivider(modifier.padding(start = start), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

val LocalHaze = androidx.compose.runtime.compositionLocalOf<HazeState?> { null }
val LocalChromeTop = androidx.compose.runtime.compositionLocalOf<Dp> { 0.dp }

@Composable
fun KScaffold(
    topBar: @Composable () -> Unit,
    topContent: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit,
    fab: @Composable () -> Unit,
    topFallback: Dp = 76.dp,
    bottomFallback: Dp = 84.dp,
    content: @Composable (PaddingValues) -> Unit
) {
    val haze = remember { HazeState() }
    var topPx by remember { mutableIntStateOf(0) }
    var botPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val bg = if (isMiuix()) MiuixTheme.colorScheme.background else MaterialTheme.colorScheme.background
    val barTint = if (isMiuix()) MiuixTheme.colorScheme.surface else MaterialTheme.colorScheme.surface

    val topDp = if (topPx == 0) topFallback else with(density) { topPx.toDp() }
    val botDp = if (botPx == 0) bottomFallback else with(density) { botPx.toDp() }

    Box(Modifier.fillMaxSize().background(bg)) {
        CompositionLocalProvider(
            LocalHaze provides haze,
            LocalTopInset provides topDp,
            LocalChromeTop provides topDp,
            LocalBottomInset provides botDp
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().hazeSource(haze)) {
                    content(PaddingValues(0.dp))
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { topPx = it.height }
                .hazeEffect(haze) {
                    backgroundColor = barTint.copy(alpha = 0.55f)
                    blurRadius = 24.dp
                    noiseFactor = 0.02f
                    fallbackTint = HazeTint(barTint.copy(alpha = 0.94f), BlendMode.SrcOver)
                }
        ) {
            Column {
                topBar()
                if (BannerState.visible) MasterBannerStrip()
                topContent()
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { botPx = it.height }
                .hazeEffect(haze) {
                    backgroundColor = barTint.copy(alpha = 0.55f)
                    blurRadius = 24.dp
                    noiseFactor = 0.02f
                    fallbackTint = HazeTint(barTint.copy(alpha = 0.94f), BlendMode.SrcOver)
                }
        ) { bottomBar() }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = botDp + 16.dp)
        ) { fab() }
        if (isMiuix()) MiuixPopupHost()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    if (isMiuix()) {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    MxIconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "返回", tint = MiuixTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                MxText(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.title2,
                    color = MiuixTheme.colorScheme.onSurface
                )
                actions()
            }
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(7.dp))
                MxText(
                    subtitle,
                    modifier = Modifier.padding(start = if (onBack != null) 4.dp else 0.dp),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    } else {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    Box(
                        Modifier.size(34.dp).clickable { onBack() },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Icon(Icons.Rounded.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = mdStyle(Txt.Title2),
                    fontFamily = FontFamily.Default
                )
                actions()
            }
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    modifier = Modifier.padding(start = if (onBack != null) 4.dp else 0.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Default,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun KNavBar(items: List<Triple<String, ImageVector, Int>>, selected: Int, onSelect: (Int) -> Unit) {
    if (isMiuix()) {
        MxNavigationBar(color = Color.Transparent) {
            items.forEach { (label, icon, idx) ->
                MxNavigationBarItem(
                    selected = selected == idx,
                    onClick = { onSelect(idx) },
                    icon = icon,
                    label = label
                )
            }
        }
    } else {
        NavigationBar(containerColor = Color.Transparent) {
            items.forEach { (label, icon, idx) ->
                NavigationBarItem(
                    selected = selected == idx,
                    onClick = { onSelect(idx) },
                    icon = { Icon(icon, null) },
                    label = { Text(label, fontFamily = FontFamily.Default) }
                )
            }
        }
    }
}

@Composable
fun KFab(
    onClick: () -> Unit,
    icon: ImageVector,
    desc: String,
    modifier: Modifier = Modifier
) {
    if (isMiuix()) {
        MxFab(
            onClick = onClick,
            modifier = modifier,
            containerColor = MiuixTheme.colorScheme.primaryContainer,
            minWidth = 56.dp,
            minHeight = 56.dp
        ) {
            Icon(icon, desc, tint = MiuixTheme.colorScheme.onPrimaryContainer)
        }
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(icon, desc, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun KDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    buttons: @Composable RowScope.() -> Unit
) {
    if (isMiuix()) {
        MxOverlayDialog(show = true, title = title, onDismissRequest = onDismiss) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) { content() }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) { buttons() }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            title = { Text(title, fontFamily = FontFamily.Default) },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState())
                ) { content() }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { buttons() }
            }
        )
    }
}

@Composable
fun KChip(text: String) {
    if (isMiuix()) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            MxText(
                text,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    } else {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontFamily = FontFamily.Default
            )
        }
    }
}

@Composable
fun KRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            val tileBg = if (isMiuix()) MiuixTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.secondaryContainer
            val tint = if (isMiuix()) MiuixTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer
            Row(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tileBg),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) { Icon(icon, null, Modifier.size(18.dp), tint = tint) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            KText(title, txt = Txt.Body1)
            if (subtitle != null) {
                KText(
                    subtitle,
                    txt = Txt.Footnote2,
                    color = if (isMiuix()) MiuixTheme.colorScheme.onSurfaceVariantSummary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (value != null) {
            KText(
                value,
                txt = Txt.Footnote1,
                color = if (isMiuix()) MiuixTheme.colorScheme.onSurfaceVariantSummary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) trailing()
        if (onClick != null && trailing == null) {
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                Modifier.size(18.dp),
                tint = if (isMiuix()) MiuixTheme.colorScheme.onSurfaceVariantSummary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun KSwitchRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            val tileBg = if (isMiuix()) MiuixTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.secondaryContainer
            val tint = if (isMiuix()) MiuixTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer
            Row(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tileBg),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) { Icon(icon, null, Modifier.size(18.dp), tint = tint) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            KText(title, txt = Txt.Body1)
            if (subtitle != null) {
                KText(
                    subtitle,
                    txt = Txt.Footnote2,
                    color = if (isMiuix()) MiuixTheme.colorScheme.onSurfaceVariantSummary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        KSwitch(checked = checked, onChange = onChange, enabled = enabled)
    }
}

@Composable
fun secondaryColor(): Color =
    if (isMiuix()) MiuixTheme.colorScheme.onSurfaceVariantSummary else MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun accentColor(): Color =
    if (isMiuix()) MiuixTheme.colorScheme.primary else MaterialTheme.colorScheme.primary

@Composable
fun errorColor(): Color =
    if (isMiuix()) MiuixTheme.colorScheme.error else MaterialTheme.colorScheme.error

@Composable
fun KChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = when {
        selected && isMiuix() -> MiuixTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primary
        isMiuix() -> MiuixTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val fg = when {
        selected && isMiuix() -> MiuixTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.onPrimary
        isMiuix() -> MiuixTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Box(
        modifier
            .height(34.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        KText(text, txt = Txt.Footnote1, color = fg)
    }
}

@Composable
fun KSwitchCell(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isMiuix()) MiuixTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KText(label, Modifier.weight(1f), txt = Txt.Footnote1)
        KSwitch(checked = checked, onChange = onChange)
    }
}

@Composable
fun KSpinner(size: Dp = 30.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(size),
            strokeWidth = 3.dp,
            color = accentColor()
        )
    }
}

@Composable
fun OnResume(onResume: () -> Unit) {
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val cb = androidx.compose.runtime.rememberUpdatedState(onResume)
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) cb.value()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}
