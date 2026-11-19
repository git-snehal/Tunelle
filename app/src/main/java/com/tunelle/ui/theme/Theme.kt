package com.tunelle.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.tunelle.R
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.tunelle.data.settings.PaletteStyleOption
import com.tunelle.data.settings.ThemeMode
import com.tunelle.data.settings.ThemeSettings
import com.tunelle.data.settings.CornerStyle
import com.tunelle.data.settings.SurfaceTone
import com.tunelle.data.settings.TitleFont
import com.tunelle.data.settings.effective

@Composable
fun ThemeSettings.isDark(): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

fun PaletteStyleOption.toPaletteStyle(): PaletteStyle = PaletteStyle.valueOf(name)

/**
 * The app theme. The scheme comes from, in order: the playing track's artwork
 * (when that option is on and a color has been read from it), Material You
 * wallpaper colors (Android 12+), or the chosen accent. Color changes animate,
 * so a new track's colors ease in rather than snap.
 */
/** The colour read from the playing song's cover, or null with nothing playing; set whatever the theme follows. */
val LocalArtworkSeed = compositionLocalOf<Color?> { null }

/** True when the style sets big titles and numbers in dot matrix, for numbers drawn outside the type scale. */
val LocalDotTitles = androidx.compose.runtime.staticCompositionLocalOf { false }

/**
 * The playing song's own accent pair (primary, tertiary) from a scheme seeded
 * by its cover, for small touches that follow the music even when the app's
 * theme doesn't; the theme's own when nothing is playing. Eases between songs.
 */
@Composable
fun songAccents(): Pair<Color, Color> {
    val seed = LocalArtworkSeed.current
    val theme = MaterialTheme.colorScheme
    if (seed == null) return theme.primary to theme.tertiary
    val scheme = rememberDynamicColorScheme(
        seedColor = seed,
        isDark = theme.background.red + theme.background.green + theme.background.blue < 1.5f,
        isAmoled = false,
        style = PaletteStyle.Vibrant,
    )
    val primary by animateColorAsState(scheme.primary, tween(600), label = "songPrimary")
    val tertiary by animateColorAsState(scheme.tertiary, tween(600), label = "songTertiary")
    return primary to tertiary
}

@Composable
fun TunelleTheme(
    settings: ThemeSettings,
    artworkSeed: Color? = null,
    content: @Composable () -> Unit,
) {
    val look = settings.effective()
    val dark = settings.isDark()
    val seed = artworkSeed?.takeIf { look.colorFromArtwork }
    val useWallpaper = seed == null && look.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val generated = rememberDynamicColorScheme(
        seedColor = seed ?: Color(look.seedColor),
        isDark = dark,
        isAmoled = look.pureBlack,
        style = look.paletteStyle.toPaletteStyle(),
    )
    val base = if (useWallpaper) {
        val context = LocalContext.current
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        generated
    }
    val toned = when {
        dark && look.pureBlack -> base.toPureBlack()
        look.surfaces == SurfaceTone.MONO -> base.toNeutral(dark)
        else -> base
    }
    // Neutral looks (Signal) show the accent exactly as picked, ember stays ember in light and
    // dark alike, instead of the tonal palette's darker or paler stand-in for it.
    val scheme = if (look.surfaces == SurfaceTone.MONO && seed == null && !useWallpaper) toned.withExactAccent(Color(look.seedColor)) else toned

    MaterialTheme(
        colorScheme = scheme.animated(),
        typography = rememberAppTypography(settings),
        shapes = shapesFor(look.corners),
    ) {
        CompositionLocalProvider(
            LocalArtworkSeed provides artworkSeed,
            LocalDotTitles provides (look.titleFont == TitleFont.DOT_MATRIX),
            com.tunelle.ui.components.LocalFocusRingColor provides MaterialTheme.colorScheme.primary,
            content = content,
        )
    }
}

/**
 * A dark scheme seeded from artwork, for the full player: it always reads as
 * a dark, immersive surface whatever the app theme is.
 */
@Composable
fun PlayerTheme(seed: Color?, settings: ThemeSettings, neutral: Boolean = false, active: Boolean = true, content: @Composable () -> Unit) {
    val scheme = rememberDynamicColorScheme(
        seedColor = seed ?: Color(settings.seedColor),
        isDark = true,
        isAmoled = settings.pureBlack,
        style = PaletteStyle.Vibrant,
    )
    val amoled = settings.playerBackground == com.tunelle.data.settings.PlayerBackground.AMOLED
    // Keep artwork-derived background tones even when the layout uses white controls.
    val playerScheme = if (neutral) scheme.copy(
        primary = Color.White, onPrimary = Color.Black, secondary = Color.White, tertiary = Color.White,
        background = Color(0xFF202023), surface = Color(0xFF202023), onSurface = Color.White,
        onBackground = Color.White, onSurfaceVariant = Color(0xFFCACACE),
        surfaceContainer = Color(0xFF2B2B2E), surfaceContainerHigh = Color(0xFF36363A),
    ) else scheme
    MaterialTheme(
        colorScheme = if (!active) MaterialTheme.colorScheme else if (amoled) playerScheme.toPureBlack() else playerScheme.animated(),
        typography = rememberAppTypography(settings),
        shapes = shapesFor(settings.effective().corners),
    ) {
        CompositionLocalProvider(LocalDotTitles provides (settings.effective().titleFont == TitleFont.DOT_MATRIX), content = content)
    }
}

/**
 * Black backgrounds with neutral gray cards, so color comes only from the
 * accent and the artwork, never from tinted surfaces.
 */
private fun ColorScheme.toPureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF242426),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0E0E0F),
    surfaceContainer = Color(0xFF1B1B1D),
    surfaceContainerHigh = Color(0xFF232325),
    surfaceContainerHighest = Color(0xFF2D2D30),
    onSurface = Color(0xFFF2F2F3),
    onBackground = Color(0xFFF2F2F3),
    onSurfaceVariant = Color(0xFF9A9AA0),
    outline = Color(0xFF4A4A4F),
    outlineVariant = Color(0xFF2E2E32),
)

/**
 * The accent as picked for primary, with readable ink on it, and a soft wash of it
 * for containers. Text on the accent goes black or white by the accent's lightness.
 */
private fun ColorScheme.withExactAccent(accent: Color): ColorScheme {
    val ink = if (accent.luminance() > 0.45f) Color(0xFF111112) else Color.White
    return copy(
        primary = accent,
        onPrimary = ink,
        primaryContainer = accent.copy(alpha = 0.18f).compositeOver(surface),
        onPrimaryContainer = onSurface,
        inversePrimary = accent,
        surfaceTint = accent,
    )
}

/**
 * Neutral backgrounds and cards for the Signal look: white and light greys,
 * or near-black and dark greys, so the accent is the only colour on screen.
 */
private fun ColorScheme.toNeutral(dark: Boolean): ColorScheme = if (dark) copy(
    background = Color(0xFF0B0B0C),
    surface = Color(0xFF0B0B0C),
    surfaceVariant = Color(0xFF26262A),
    surfaceContainerLowest = Color(0xFF070708),
    surfaceContainerLow = Color(0xFF121214),
    surfaceContainer = Color(0xFF19191B),
    surfaceContainerHigh = Color(0xFF222225),
    surfaceContainerHighest = Color(0xFF2C2C30),
    onSurface = Color(0xFFF4F4F5),
    onBackground = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFFA1A1A8),
    outline = Color(0xFF505056),
    outlineVariant = Color(0xFF303035),
    secondaryContainer = Color(0xFF2C2C30),
    onSecondaryContainer = Color(0xFFF4F4F5),
) else copy(
    background = Color(0xFFF7F7F6),
    surface = Color(0xFFF7F7F6),
    surfaceVariant = Color(0xFFE6E6E4),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F1EF),
    surfaceContainer = Color(0xFFECECEA),
    surfaceContainerHigh = Color(0xFFE5E5E3),
    surfaceContainerHighest = Color(0xFFDDDDDA),
    onSurface = Color(0xFF111112),
    onBackground = Color(0xFF111112),
    onSurfaceVariant = Color(0xFF5E5E63),
    outline = Color(0xFF8A8A8F),
    outlineVariant = Color(0xFFCFCFCC),
    secondaryContainer = Color(0xFFE2E2E0),
    onSecondaryContainer = Color(0xFF111112),
)

/** The scheme with every colour easing to its new value, so a change of seed fades rather than snaps. */
@Composable
internal fun ColorScheme.animated(): ColorScheme {
    val spec = tween<Color>(durationMillis = 600)
    @Composable
    fun Color.a() = animateColorAsState(this, spec, label = "scheme").value
    return copy(
        primary = primary.a(),
        onPrimary = onPrimary.a(),
        primaryContainer = primaryContainer.a(),
        onPrimaryContainer = onPrimaryContainer.a(),
        secondary = secondary.a(),
        onSecondary = onSecondary.a(),
        secondaryContainer = secondaryContainer.a(),
        onSecondaryContainer = onSecondaryContainer.a(),
        tertiary = tertiary.a(),
        tertiaryContainer = tertiaryContainer.a(),
        onTertiaryContainer = onTertiaryContainer.a(),
        background = background.a(),
        onBackground = onBackground.a(),
        surface = surface.a(),
        onSurface = onSurface.a(),
        surfaceVariant = surfaceVariant.a(),
        onSurfaceVariant = onSurfaceVariant.a(),
        surfaceContainerLowest = surfaceContainerLowest.a(),
        surfaceContainerLow = surfaceContainerLow.a(),
        surfaceContainer = surfaceContainer.a(),
        surfaceContainerHigh = surfaceContainerHigh.a(),
        surfaceContainerHighest = surfaceContainerHighest.a(),
        outline = outline.a(),
        outlineVariant = outlineVariant.a(),
    )
}

private val baseType = Typography()

/**
 * Outfit, Tunelle's typeface: a geometric sans with round, open letters
 * (SIL Open Font License; see third_party/outfit). One variable font file,
 * instanced at the weights the app uses.
 */
@OptIn(ExperimentalTextApi::class)
val Outfit = FontFamily(
    listOf(300, 400, 500, 600, 700, 800).map { w ->
        Font(R.font.outfit_variable, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

private fun TextStyle.outfit(weight: FontWeight? = null, spacing: Float? = null) =
    copy(fontFamily = Outfit, fontWeight = weight ?: fontWeight, letterSpacing = spacing?.sp ?: letterSpacing)

val AppTypography = Typography(
    displayLarge = baseType.displayLarge.outfit(FontWeight.Bold, -1.2f),
    displayMedium = baseType.displayMedium.outfit(FontWeight.Bold, -1f),
    displaySmall = baseType.displaySmall.outfit(FontWeight.Bold, -0.6f),
    headlineLarge = baseType.headlineLarge.outfit(FontWeight.Bold, -0.4f),
    headlineMedium = baseType.headlineMedium.outfit(FontWeight.SemiBold, -0.3f),
    headlineSmall = baseType.headlineSmall.outfit(FontWeight.SemiBold, -0.2f),
    titleLarge = baseType.titleLarge.outfit(FontWeight.SemiBold, -0.1f),
    titleMedium = baseType.titleMedium.outfit(FontWeight.Medium),
    titleSmall = baseType.titleSmall.outfit(FontWeight.Medium),
    bodyLarge = baseType.bodyLarge.outfit(),
    bodyMedium = baseType.bodyMedium.outfit(),
    bodySmall = baseType.bodySmall.outfit(),
    labelLarge = baseType.labelLarge.outfit(FontWeight.Medium),
    labelMedium = baseType.labelMedium.outfit(FontWeight.Medium),
    labelSmall = baseType.labelSmall.outfit(FontWeight.Medium),
)

/** Large, heavy lyric lines, read at a glance from arm's length. */
val LyricsTextStyle = TextStyle(
    fontFamily = Outfit,
    fontSize = 34.sp,
    lineHeight = 41.sp,
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = (-0.6).sp,
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val CrispShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

private val RoundShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(42.dp),
)

fun shapesFor(corners: CornerStyle): Shapes = when (corners) {
    CornerStyle.CRISP -> CrispShapes
    CornerStyle.BALANCED -> AppShapes
    CornerStyle.ROUND -> RoundShapes
}

/**
 * Space Grotesk, Tunelle's text face: a technical grotesque with a little character
 * (SIL Open Font License; see third_party/space-grotesk). One variable file.
 */
@OptIn(ExperimentalTextApi::class)
val SpaceGrotesk = FontFamily(
    listOf(300, 400, 500, 600, 700).map { w ->
        Font(R.font.space_grotesk_variable, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

/**
 * Geist, the Signal look's text face: a clean, modern grotesque made for screens
 * (SIL Open Font License; see third_party/geist). One variable file.
 */
@OptIn(ExperimentalTextApi::class)
val Geist = FontFamily(
    listOf(300, 400, 500, 600, 700).map { w ->
        Font(R.font.geist_variable, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

/** Geist Mono, for the small uppercase labels over sections in the Signal look. */
@OptIn(ExperimentalTextApi::class)
val GeistMono = FontFamily(
    listOf(400, 500).map { w ->
        Font(R.font.geist_mono_variable, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

/**
 * Doto, a dot-matrix face for small numbers in the Signal look (the date, song times)
 * (SIL Open Font License; see third_party/doto). Set heavy so the dots read.
 */
@OptIn(ExperimentalTextApi::class)
val DotMatrix = FontFamily(
    listOf(500, 700, 800, 900).map { w ->
        Font(R.font.doto_variable, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

@Composable
fun rememberSeedSwatches(colors: List<Int>): List<Color> = remember(colors) { colors.map { Color(it) } }

/** System sans avoids thin OEM/variable-font rendering; imported files fall back safely if removed. */
@Composable
internal fun rememberAppFont(settings: ThemeSettings): FontFamily {
    val context = LocalContext.current
    val font = settings.effective().font
    return remember(font, settings.customFontId) {
        when (font) {
            com.tunelle.data.settings.AppFont.GEIST -> Geist
            com.tunelle.data.settings.AppFont.SPACE_GROTESK -> SpaceGrotesk
            com.tunelle.data.settings.AppFont.SYSTEM -> FontFamily.SansSerif
            com.tunelle.data.settings.AppFont.OUTFIT -> Outfit
            com.tunelle.data.settings.AppFont.SERIF -> FontFamily.Serif
            com.tunelle.data.settings.AppFont.MONO -> FontFamily.Monospace
            com.tunelle.data.settings.AppFont.CUSTOM -> runCatching {
                val file = requireNotNull(ImportedFonts.file(context, settings.customFontId))
                FontFamily(requireNotNull(android.graphics.Typeface.Builder(file).build()))
            }.getOrDefault(FontFamily.SansSerif)
        }
    }
}

@Composable
private fun rememberAppTypography(settings: ThemeSettings): Typography {
    val family = rememberAppFont(settings)
    val dots = settings.effective().titleFont == TitleFont.DOT_MATRIX
    return remember(family, settings.textWeight, dots) {
        fun TextStyle.readable() = copy(fontFamily = family,
            fontWeight = FontWeight(maxOf(fontWeight?.weight ?: 400, settings.textWeight.minimum)))
        // Signal: big titles in the text face, medium weight and set tight, like an
        // editorial headline. The dot face is kept for small numbers, drawn by hand.
        fun TextStyle.title(spacing: Float) = if (!dots) readable() else copy(fontFamily = family, fontWeight = FontWeight.Medium, letterSpacing = spacing.sp)
        AppTypography.run { copy(
            displayLarge = displayLarge.title(-1.8f), displayMedium = displayMedium.title(-1.5f), displaySmall = displaySmall.title(-1.2f),
            headlineLarge = headlineLarge.title(-0.9f), headlineMedium = headlineMedium.title(-0.6f), headlineSmall = headlineSmall.title(-0.3f),
            titleLarge = titleLarge.readable(), titleMedium = titleMedium.readable(), titleSmall = titleSmall.readable(),
            bodyLarge = bodyLarge.readable(), bodyMedium = bodyMedium.readable(), bodySmall = bodySmall.readable(),
            labelLarge = labelLarge.readable(), labelMedium = labelMedium.readable(), labelSmall = labelSmall.readable(),
        ) }
    }
}
