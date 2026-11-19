package com.enddream.keybox

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

val LocalGlassWall = compositionLocalOf<LayerBackdrop?> { null }
val LocalGlassContent = compositionLocalOf<LayerBackdrop?> { null }
object GlassHost {
    var frame by mutableStateOf<(@Composable () -> Unit)?>(null)
    var dismiss: (() -> Unit)? = null
    var active = 0
    var hasHost by mutableStateOf(false)
}

@Composable
fun glassDark(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

@Composable
fun glassSurface(): Color {
    val d = glassDark()
    return if (d) Color(0xFF1C1C1E).copy(alpha = 0.38f) else Color.White.copy(alpha = 0.30f)
}

@Composable
fun glassSurfaceStrong(): Color {
    val d = glassDark()
    return if (d) Color(0xFF1C1C1E).copy(alpha = 0.62f) else Color.White.copy(alpha = 0.52f)
}

@Composable
fun glassThin(): Color {
    val d = glassDark()
    return if (d) Color(0xFF1C1C1E).copy(alpha = 0.30f) else Color.White.copy(alpha = 0.24f)
}

@Composable
fun glassStroke(): Color {
    val d = glassDark()
    return if (d) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.75f)
}

@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    tint: Color,
    backdrop: LayerBackdrop? = LocalGlassWall.current,
    blurSize: Dp = 14.dp,
    refraction: Dp = 8.dp
): Modifier {
    if (backdrop == null || Build.VERSION.SDK_INT < 31) return this.background(tint, shape)
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurSize.toPx())
            lens(refraction.toPx(), (refraction * 2).toPx())
        },
        highlight = { Highlight.Plain },
        onDrawSurface = { drawRect(tint) }
    )
}

@Composable
fun Modifier.liquidGlassBar(shape: Shape, tint: Color, backdrop: LayerBackdrop?): Modifier {
    if (backdrop == null || Build.VERSION.SDK_INT < 31) return this.background(tint, shape)
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(22.dp.toPx())
        },
        highlight = { Highlight.Plain },
        onDrawSurface = { drawRect(tint) }
    )
}

@Composable
fun GlassWallpaper(modifier: Modifier = Modifier) {
    val dark = glassDark()
    val base = if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF0F0F0F)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFC9C9C9)))
    }
    val blobs = if (dark) listOf(
        Triple(0.08f, 0.04f, Color.White.copy(alpha = 0.12f)),
        Triple(0.95f, 0.20f, Color.White.copy(alpha = 0.08f)),
        Triple(0.30f, 0.58f, Color.White.copy(alpha = 0.05f)),
        Triple(0.85f, 0.96f, Color.Black.copy(alpha = 0.45f))
    ) else listOf(
        Triple(0.08f, 0.04f, Color.White),
        Triple(0.95f, 0.20f, Color(0xFFEDEDED)),
        Triple(0.30f, 0.58f, Color(0xFFF8F8F8)),
        Triple(0.85f, 0.96f, Color(0xFFB2B2B2))
    )
    Box(
        modifier
            .fillMaxSize()
            .background(base)
            .drawBehind {
                blobs.forEach { (fx, fy, c) ->
                    val center = Offset(size.width * fx, size.height * fy)
                    val radius = size.maxDimension * 0.50f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(c, Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
            }
    )
}

@Composable
fun LiquidScaffold(
    topBar: @Composable () -> Unit,
    topContent: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    fab: @Composable () -> Unit,
    topFallback: Dp,
    bottomFallback: Dp,
    content: @Composable (PaddingValues) -> Unit
) {
    val wall = rememberLayerBackdrop()
    val contentLayer = rememberLayerBackdrop()
    var topPx by remember { mutableIntStateOf(0) }
    var botPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val topDp = if (topPx == 0) topFallback else with(density) { topPx.toDp() }
    val botDp = if (botPx == 0) bottomFallback else with(density) { botPx.toDp() }
    val dark = glassDark()
    DisposableEffect(Unit) {
        GlassHost.active++
        GlassHost.hasHost = true
        onDispose {
            GlassHost.active--
            if (GlassHost.active == 0) {
                GlassHost.hasHost = false
                GlassHost.frame = null
                GlassHost.dismiss = null
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().layerBackdrop(wall)) {
            GlassWallpaper()
        }
        CompositionLocalProvider(
            LocalTopInset provides topDp,
            LocalChromeTop provides topDp,
            LocalBottomInset provides botDp,
            LocalGlassWall provides wall,
            LocalGlassContent provides contentLayer
        ) {
            Box(Modifier.fillMaxSize().layerBackdrop(contentLayer)) {
                content(PaddingValues(0.dp))
            }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { topPx = it.height }
                    .liquidGlassBar(RoundedCornerShape(0.dp), glassSurfaceStrong(), contentLayer)
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
                    .then(
                        if (ChromeState.floating) Modifier
                            .padding(start = 14.dp, end = 14.dp, bottom = 8.dp)
                            .navigationBarsPadding()
                        else Modifier
                    )
                    .then(
                        if (ChromeState.floating) Modifier.border(
                            1.dp,
                            Color.White.copy(alpha = if (glassDark()) 0.16f else 0.60f),
                            RoundedCornerShape(28.dp)
                        ) else Modifier
                    )
                    .liquidGlassBar(
                        if (ChromeState.floating) RoundedCornerShape(28.dp) else RectangleShape,
                        glassSurfaceStrong(),
                        contentLayer
                    )
            ) {
                Box(Modifier.consumeWindowInsets(WindowInsets.navigationBars)) { bottomBar() }
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = botDp + 16.dp)
            ) { fab() }

            GlassHost.frame?.let { frame ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = if (dark) 0.40f else 0.24f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { GlassHost.dismiss?.invoke() }
                ) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 26.dp)
                            .liquidGlass(
                                RoundedCornerShape(28.dp),
                                glassSurfaceStrong(),
                                contentLayer,
                                18.dp,
                                10.dp
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}
                            .padding(20.dp)
                            .heightIn(max = 560.dp)
                            .verticalScroll(rememberScrollState())
                    ) { frame() }
                }
            }
        }
    }
}

@Composable
fun LiquidDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    buttons: @Composable RowScope.() -> Unit
) {
    if (!GlassHost.hasHost) {
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
                Row {
                    buttons()
                }
            }
        )
        return
    }
    BackHandler { onDismiss() }
    SideEffect {
        GlassHost.dismiss = onDismiss
        GlassHost.frame = {
            Column(Modifier.fillMaxWidth().widthIn(max = 420.dp)) {
                KText(title, txt = Txt.Title2)
                Column(Modifier.padding(top = 10.dp)) { content() }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) { buttons() }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            GlassHost.frame = null
            GlassHost.dismiss = null
        }
    }
}
