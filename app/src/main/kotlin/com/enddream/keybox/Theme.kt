package com.enddream.keybox

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.Colors as MiuixColors
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixLocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.TextStyles
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDark
import top.yukonga.miuix.kmp.theme.defaultTextStyles
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLight

enum class UiStyle { Material, Miuix }

val PresetSeeds = intArrayOf(
    0xFF006A60.toInt(), 0xFF3482FF.toInt(), 0xFF7C4DFF.toInt(), 0xFFD81B60.toInt(),
    0xFFE94634.toInt(), 0xFFF57C00.toInt(), 0xFF2E7D32.toInt(), 0xFF00838F.toInt()
)

fun hsl(h: Float, s: Float, l: Float): Color {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val hp = ((h % 360f) + 360f) % 360f / 60f
    val x = c * (1f - kotlin.math.abs(hp % 2f - 1f))
    val (r, g, b) = when {
        hp < 1f -> Triple(c, x, 0f)
        hp < 2f -> Triple(x, c, 0f)
        hp < 3f -> Triple(0f, c, x)
        hp < 4f -> Triple(0f, x, c)
        hp < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    return Color(
        (r + m).coerceIn(0f, 1f),
        (g + m).coerceIn(0f, 1f),
        (b + m).coerceIn(0f, 1f)
    )
}

fun seedScheme(seed: Color, dark: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    val h = hsv[0]
    val s = hsv[1].coerceIn(0.25f, 0.9f)
    val accentHue = if (s < 0.3f) 210f else h

    val primary = if (dark) hsl(accentHue, s * 0.85f, 0.72f) else hsl(accentHue, s, 0.40f)
    val onPrimary = if (dark) hsl(accentHue, s, 0.18f) else Color.White
    val primaryContainer = if (dark) hsl(accentHue, s * 0.9f, 0.28f) else hsl(accentHue, s * 0.85f, 0.88f)
    val onPrimaryContainer = if (dark) hsl(accentHue, s * 0.8f, 0.92f) else hsl(accentHue, s, 0.18f)
    val secondary = if (dark) hsl(accentHue, s * 0.35f, 0.78f) else hsl(accentHue, s * 0.4f, 0.42f)
    val onSecondary = if (dark) hsl(accentHue, s * 0.3f, 0.20f) else Color.White
    val secondaryContainer = if (dark) hsl(accentHue, s * 0.35f, 0.26f) else hsl(accentHue, s * 0.4f, 0.90f)
    val onSecondaryContainer = if (dark) hsl(accentHue, s * 0.3f, 0.92f) else hsl(accentHue, s * 0.35f, 0.18f)
    val th = (accentHue + 55f) % 360f
    val tertiary = if (dark) hsl(th, s * 0.5f, 0.76f) else hsl(th, s * 0.5f, 0.42f)
    val tertiaryContainer = if (dark) hsl(th, s * 0.45f, 0.26f) else hsl(th, s * 0.45f, 0.90f)
    val background = if (dark) hsl(accentHue, 0.10f, 0.07f) else hsl(accentHue, 0.14f, 0.985f)
    val onBackground = if (dark) hsl(accentHue, 0.06f, 0.93f) else hsl(accentHue, 0.10f, 0.12f)
    val surface = if (dark) hsl(accentHue, 0.09f, 0.09f) else hsl(accentHue, 0.12f, 0.99f)
    val surfaceVariant = if (dark) hsl(accentHue, 0.09f, 0.20f) else hsl(accentHue, 0.10f, 0.92f)
    val onSurfaceVariant = if (dark) hsl(accentHue, 0.08f, 0.78f) else hsl(accentHue, 0.07f, 0.38f)
    val surfaceContainerLow = if (dark) hsl(accentHue, 0.09f, 0.12f) else hsl(accentHue, 0.12f, 0.96f)
    val surfaceContainer = if (dark) hsl(accentHue, 0.09f, 0.15f) else hsl(accentHue, 0.12f, 0.94f)
    val surfaceContainerHigh = if (dark) hsl(accentHue, 0.09f, 0.18f) else hsl(accentHue, 0.12f, 0.92f)
    val outline = if (dark) hsl(accentHue, 0.08f, 0.60f) else hsl(accentHue, 0.07f, 0.55f)
    val outlineVariant = if (dark) hsl(accentHue, 0.08f, 0.30f) else hsl(accentHue, 0.08f, 0.82f)

    return if (dark) darkColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = onSecondary,
        secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary, tertiaryContainer = tertiaryContainer,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onBackground,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLow = surfaceContainerLow, surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        outline = outline, outlineVariant = outlineVariant
    ) else lightColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = onSecondary,
        secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary, tertiaryContainer = tertiaryContainer,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onBackground,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLow = surfaceContainerLow, surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        outline = outline, outlineVariant = outlineVariant
    )
}

fun ColorScheme.toMiuixColors(dark: Boolean): MiuixColors {
    val card = surfaceContainerLow
    val onCard = onSurface
    val disabledBg = onSurface.copy(alpha = 0.10f)
    val disabledFg = onSurface.copy(alpha = 0.34f)
    return if (dark) miuixDark(
        primary = primary, onPrimary = onPrimary, primaryVariant = primary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = onSecondary,
        secondaryVariant = card, onSecondaryVariant = onCard,
        secondaryContainer = card, onSecondaryContainer = onCard,
        secondaryContainerVariant = surfaceContainer, onSecondaryContainerVariant = onSurfaceVariant,
        tertiaryContainer = primaryContainer, onTertiaryContainer = onPrimaryContainer,
        tertiaryContainerVariant = surfaceContainerHigh,
        error = error, onError = onError,
        errorContainer = error.copy(alpha = 0.14f), onErrorContainer = error,
        disabledPrimaryButton = disabledBg, disabledOnPrimaryButton = disabledFg,
        disabledSecondaryVariant = disabledBg, disabledOnSecondaryVariant = disabledFg,
        disabledSecondary = disabledBg, disabledOnSecondary = disabledFg,
        background = background, onBackground = onBackground,
        surface = card, onSurface = onCard,
        surfaceContainer = surfaceContainer, onSurfaceContainer = onSurface,
        surfaceContainerHigh = surfaceContainerHigh, onSurfaceContainerHigh = onSurface,
        outline = outline, dividerLine = outlineVariant,
        onSurfaceVariantSummary = onSurfaceVariant,
        onSurfaceVariantActions = onSurfaceVariant,
        disabledOnSurface = disabledFg
    ) else miuixLight(
        primary = primary, onPrimary = onPrimary, primaryVariant = primary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = onSecondary,
        secondaryVariant = card, onSecondaryVariant = onCard,
        secondaryContainer = card, onSecondaryContainer = onCard,
        secondaryContainerVariant = surfaceContainer, onSecondaryContainerVariant = onSurfaceVariant,
        tertiaryContainer = primaryContainer, onTertiaryContainer = onPrimaryContainer,
        tertiaryContainerVariant = surfaceContainerHigh,
        error = error, onError = onError,
        errorContainer = error.copy(alpha = 0.12f), onErrorContainer = error,
        disabledPrimaryButton = disabledBg, disabledOnPrimaryButton = disabledFg,
        disabledSecondaryVariant = disabledBg, disabledOnSecondaryVariant = disabledFg,
        disabledSecondary = disabledBg, disabledOnSecondary = disabledFg,
        background = background, onBackground = onBackground,
        surface = card, onSurface = onCard,
        surfaceContainer = surfaceContainer, onSurfaceContainer = onSurface,
        surfaceContainerHigh = surfaceContainerHigh, onSurfaceContainerHigh = onSurface,
        outline = outline, dividerLine = outlineVariant,
        onSurfaceVariantSummary = onSurfaceVariant,
        onSurfaceVariantActions = onSurfaceVariant,
        disabledOnSurface = disabledFg
    )
}

private fun tx(size: Int, weight: FontWeight = FontWeight.Normal, line: Int = 0): TextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = if (line > 0) line.sp else TextUnit.Unspecified
)

fun compactMiuixTextStyles(): TextStyles = defaultTextStyles(
    main = tx(14),
    paragraph = tx(14, FontWeight.Normal, 21),
    body1 = tx(14),
    body2 = tx(13),
    button = tx(14, FontWeight.Medium),
    footnote1 = tx(12),
    footnote2 = tx(11, FontWeight.Normal, 15),
    headline1 = tx(15, FontWeight.Medium),
    headline2 = tx(14, FontWeight.Medium),
    subtitle = tx(12, FontWeight.Bold),
    title1 = tx(24, FontWeight.SemiBold),
    title2 = tx(19, FontWeight.SemiBold),
    title3 = tx(16, FontWeight.Medium),
    title4 = tx(15, FontWeight.Medium)
)

@Composable
fun KeyBoxTheme(
    style: UiStyle, seed: Int, dynamic: Boolean, dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        else -> seedScheme(Color(seed), dark)
    }
    val mxColors = remember(scheme) { scheme.toMiuixColors(dark) }
    val mxText = remember { compactMiuixTextStyles() }
    MaterialTheme(colorScheme = scheme) {
        if (style == UiStyle.Miuix) {
            MiuixTheme(colors = mxColors, textStyles = mxText) {
                CompositionLocalProvider(
                    MiuixLocalContentColor provides scheme.onSurface
                ) { content() }
            }
        } else {
            content()
        }
    }
}
