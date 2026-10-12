package es.unocerobits.tlotraigo.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import es.unocerobits.tlotraigo.data.Palette
import es.unocerobits.tlotraigo.data.UiSettings

private fun Color.mix(o: Color, t: Float) = lerp(this, o, t)
private val W = Color.White
private val B = Color.Black

/** Genera un esquema Material 3 completo a partir de los tres colores de la paleta. */
fun schemeFor(p: Palette, dark: Boolean): ColorScheme {
    val pr = Color(p.primary); val se = Color(p.secondary); val te = Color(p.tertiary)
    return if (!dark) lightColorScheme(
        primary = pr, onPrimary = W, primaryContainer = pr.mix(W, .80f), onPrimaryContainer = pr.mix(B, .55f),
        secondary = se, onSecondary = W, secondaryContainer = se.mix(W, .76f), onSecondaryContainer = se.mix(B, .6f),
        tertiary = te, onTertiary = W, tertiaryContainer = te.mix(W, .80f), onTertiaryContainer = te.mix(B, .6f),
        background = pr.mix(W, .95f), onBackground = Color(0xFF1B1D1F).mix(pr, .12f),
        surface = pr.mix(W, .95f), onSurface = Color(0xFF1B1D1F).mix(pr, .12f),
        surfaceVariant = pr.mix(W, .87f), onSurfaceVariant = Color(0xFF45484B).mix(pr, .18f),
        surfaceContainerLowest = W, surfaceContainerLow = pr.mix(W, .97f), surfaceContainer = W,
        surfaceContainerHigh = pr.mix(W, .91f), surfaceContainerHighest = pr.mix(W, .87f),
        outline = Color(0xFF74777A).mix(pr, .2f), outlineVariant = Color(0xFFC6C8CA).mix(pr, .2f),
        error = Color(0xFFB3261E), onError = W, errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
        inverseSurface = Color(0xFF2F3133), inverseOnSurface = Color(0xFFF1F0F4), inversePrimary = pr.mix(W, .55f),
    ) else darkColorScheme(
        primary = pr.mix(W, .55f), onPrimary = pr.mix(B, .62f), primaryContainer = pr.mix(B, .35f), onPrimaryContainer = pr.mix(W, .82f),
        secondary = se.mix(W, .55f), onSecondary = se.mix(B, .62f), secondaryContainer = se.mix(B, .38f), onSecondaryContainer = se.mix(W, .82f),
        tertiary = te.mix(W, .55f), onTertiary = te.mix(B, .62f), tertiaryContainer = te.mix(B, .38f), onTertiaryContainer = te.mix(W, .82f),
        background = Color(0xFF111315).mix(pr, .07f), onBackground = Color(0xFFE3E3E6),
        surface = Color(0xFF111315).mix(pr, .07f), onSurface = Color(0xFFE3E3E6),
        surfaceVariant = Color(0xFF3F4346).mix(pr, .1f), onSurfaceVariant = Color(0xFFC5C8CB),
        surfaceContainerLowest = Color(0xFF0C0E10), surfaceContainerLow = Color(0xFF181B1D).mix(pr, .07f),
        surfaceContainer = Color(0xFF1D2022).mix(pr, .08f), surfaceContainerHigh = Color(0xFF272A2D).mix(pr, .08f),
        surfaceContainerHighest = Color(0xFF323538).mix(pr, .08f),
        outline = Color(0xFF8F9295), outlineVariant = Color(0xFF45484B),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
    )
}

// Alto contraste (WCAG AAA) claro: blanco/negro/azul marino. Oscuro: negro/amarillo.
private val ContrastLight = lightColorScheme(
    primary = Color(0xFF002A6B), onPrimary = W, primaryContainer = Color(0xFF002A6B), onPrimaryContainer = W,
    secondary = Color(0xFF5A1D00), onSecondary = W, secondaryContainer = Color(0xFFFFE08A), onSecondaryContainer = B,
    tertiary = Color(0xFF6B0019), onTertiary = W, tertiaryContainer = Color(0xFFFFD9DC), onTertiaryContainer = B,
    background = W, onBackground = B, surface = W, onSurface = B, surfaceVariant = Color(0xFFEDEDED), onSurfaceVariant = B,
    surfaceContainerLowest = W, surfaceContainerLow = W, surfaceContainer = W, surfaceContainerHigh = Color(0xFFF2F2F2),
    surfaceContainerHighest = Color(0xFFE6E6E6), outline = B, outlineVariant = Color(0xFF555555),
    error = Color(0xFF8C0009), onError = W, errorContainer = Color(0xFFFFDAD6), onErrorContainer = B,
)
private val ContrastDark = darkColorScheme(
    primary = Color(0xFFFFE500), onPrimary = B, primaryContainer = Color(0xFFFFE500), onPrimaryContainer = B,
    secondary = Color(0xFF7DF9FF), onSecondary = B, secondaryContainer = Color(0xFF7DF9FF), onSecondaryContainer = B,
    tertiary = Color(0xFFFF9E9E), onTertiary = B, tertiaryContainer = Color(0xFFFF9E9E), onTertiaryContainer = B,
    background = B, onBackground = W, surface = B, onSurface = W, surfaceVariant = Color(0xFF1A1A1A), onSurfaceVariant = W,
    surfaceContainerLowest = B, surfaceContainerLow = Color(0xFF0D0D0D), surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1C1C1C), surfaceContainerHighest = Color(0xFF262626),
    outline = W, outlineVariant = Color(0xFFBDBDBD),
    error = Color(0xFFFF6E6E), onError = B, errorContainer = Color(0xFFFF6E6E), onErrorContainer = B,
)

val LocalSenior = compositionLocalOf { true }
val LocalHighContrast = compositionLocalOf { false }

@Composable
private fun ColorScheme.animated(): ColorScheme {
    @Composable fun a(c: Color) = animateColorAsState(c, tween(450), label = "c").value
    return copy(
        primary = a(primary), onPrimary = a(onPrimary), primaryContainer = a(primaryContainer), onPrimaryContainer = a(onPrimaryContainer),
        secondary = a(secondary), onSecondary = a(onSecondary), secondaryContainer = a(secondaryContainer), onSecondaryContainer = a(onSecondaryContainer),
        tertiary = a(tertiary), tertiaryContainer = a(tertiaryContainer), onTertiaryContainer = a(onTertiaryContainer),
        background = a(background), onBackground = a(onBackground), surface = a(surface), onSurface = a(onSurface),
        surfaceVariant = a(surfaceVariant), onSurfaceVariant = a(onSurfaceVariant),
        surfaceContainer = a(surfaceContainer), surfaceContainerHigh = a(surfaceContainerHigh),
        surfaceContainerHighest = a(surfaceContainerHighest), surfaceContainerLow = a(surfaceContainerLow),
        outline = a(outline), outlineVariant = a(outlineVariant),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(26.dp), extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun AppTheme(settings: UiSettings, content: @Composable () -> Unit) {
    val dark = settings.mode.isDark(isSystemInDarkTheme())
    val scheme = when {
        settings.highContrast -> if (dark) ContrastDark else ContrastLight
        else -> schemeFor(settings.palette, dark)
    }.animated()
    val d = LocalDensity.current
    val scale = if (settings.senior) maxOf(settings.textScale, 1.15f) else settings.textScale
    CompositionLocalProvider(
        LocalDensity provides Density(d.density, (d.fontScale * scale).coerceAtMost(1.9f)),
        LocalMinimumInteractiveComponentSize provides if (settings.senior) 56.dp else 48.dp,
        LocalSenior provides settings.senior,
        LocalHighContrast provides settings.highContrast,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typographyFor(settings.font), shapes = AppShapes, content = content)
    }
}
