package com.tunelle.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import com.tunelle.data.MusicLanguage
import com.tunelle.playback.AudioLevels
import com.tunelle.ui.components.EMBER
import com.tunelle.ui.theme.Text
import kotlinx.coroutines.delay

/** The time of day, as the greetings split it. */
internal enum class DayPart { MORNING, AFTERNOON, EVENING }

internal fun dayPart(hour: Int): DayPart = when (hour) {
    in 5..11 -> DayPart.MORNING
    in 12..16 -> DayPart.AFTERNOON
    else -> DayPart.EVENING
}

/**
 * How each language says good morning, afternoon and evening. Where a time-of-day
 * greeting isn't how people greet (Malayalam), its everyday greeting stands for all three.
 */
private val GREETINGS: Map<MusicLanguage, List<String>> = mapOf(
    MusicLanguage.HINDI to listOf("सुप्रभात", "शुभ दोपहर", "शुभ संध्या"),
    MusicLanguage.BHOJPURI to listOf("सुप्रभात", "शुभ दोपहर", "शुभ संध्या"),
    MusicLanguage.HARYANVI to listOf("सुप्रभात", "शुभ दोपहर", "शुभ संध्या"),
    MusicLanguage.PUNJABI to listOf("ਸ਼ੁਭ ਸਵੇਰ", "ਸ਼ੁਭ ਦੁਪਹਿਰ", "ਸ਼ੁਭ ਸ਼ਾਮ"),
    MusicLanguage.TAMIL to listOf("காலை வணக்கம்", "மதிய வணக்கம்", "மாலை வணக்கம்"),
    MusicLanguage.TELUGU to listOf("శుభోదయం", "శుభ మధ్యాహ్నం", "శుభ సాయంత్రం"),
    MusicLanguage.BENGALI to listOf("সুপ্রভাত", "শুভ দুপুর", "শুভ সন্ধ্যা"),
    MusicLanguage.MARATHI to listOf("सुप्रभात", "शुभ दुपार", "शुभ संध्याकाळ"),
    MusicLanguage.KANNADA to listOf("ಶುಭೋದಯ", "ಶುಭ ಮಧ್ಯಾಹ್ನ", "ಶುಭ ಸಂಜೆ"),
    MusicLanguage.MALAYALAM to listOf("നമസ്കാരം", "നമസ്കാരം", "നമസ്കാരം"),
    MusicLanguage.GUJARATI to listOf("સુપ્રભાત", "શુભ બપોર", "શુભ સાંજ"),
    MusicLanguage.URDU to listOf("صبح بخیر", "دوپہر بخیر", "شام بخیر"),
    MusicLanguage.SPANISH to listOf("Buenos días", "Buenas tardes", "Buenas noches"),
    MusicLanguage.KOREAN to listOf("좋은 아침이에요", "좋은 오후예요", "좋은 저녁이에요"),
    MusicLanguage.JAPANESE to listOf("おはよう", "こんにちは", "こんばんは"),
    MusicLanguage.ARABIC to listOf("صباح الخير", "مساء الخير", "مساء الخير"),
    MusicLanguage.FRENCH to listOf("Bonjour", "Bon après-midi", "Bonsoir"),
    MusicLanguage.PORTUGUESE to listOf("Bom dia", "Boa tarde", "Boa noite"),
    MusicLanguage.INDONESIAN to listOf("Selamat pagi", "Selamat siang", "Selamat malam"),
)

internal fun greetingIn(language: MusicLanguage, part: DayPart): String? = GREETINGS[language]?.get(part.ordinal)

/** The blocks the greeting resolves out of, like a dot-matrix display finding its letters. */
private const val STATIC = "▖▗▘▙▚▛▜▝▞▟▒░"

/**
 * The greeting in the app's language, then in turn in each language the
 * listener picked on the welcome. Each change decodes from a shimmer of blocks,
 * left to right. The ember full stop after it breathes with what's playing.
 */
@Composable
internal fun Greeting(
    base: String,
    languages: List<MusicLanguage>,
    part: DayPart,
    playing: Boolean,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val lines = remember(base, languages, part) {
        (listOf(base) + languages.mapNotNull { greetingIn(it, part) }).distinct()
    }
    var shown by remember(lines) { mutableStateOf(lines.first()) }
    LaunchedEffect(lines, animate) {
        if (!animate || lines.size < 2) { shown = lines.first(); return@LaunchedEffect }
        var i = 0
        while (true) {
            delay(GREETING_HOLD_MS)
            i = (i + 1) % lines.size
            val target = lines[i]
            val steps = 14
            for (s in 1..steps) {
                val kept = target.length * s / steps
                val noise = (0 until minOf(6, target.length - kept)).map { STATIC.random() }.joinToString("")
                shown = target.take(kept) + noise
                delay(40)
            }
            shown = target
        }
    }
    val beat = rememberBeat(playing && animate)
    val text = buildAnnotatedString {
        append(shown)
        appendInlineContent(DOT_ID, ".")
    }
    Text(
        text,
        style = MaterialTheme.typography.displaySmall,
        maxLines = 2,
        modifier = modifier,
        inlineContent = mapOf(
            // The full stop, in ember: still when nothing plays, swelling softly with the bass when it does.
            DOT_ID to InlineTextContent(Placeholder(0.32.em, 1.em, PlaceholderVerticalAlign.TextBottom)) { DotGlyph(beat) },
        ),
    )
}

/** The ember point itself, sitting on the baseline. */
@Composable
private fun DotGlyph(beat: MutableFloatState) {
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val s = 1f + 0.55f * beat.floatValue
                scaleX = s
                scaleY = s
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.74f)
            },
    ) {
        val r = size.width * 0.3f
        drawCircle(EMBER, radius = r, center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.74f))
    }
}

/**
 * How strongly the music hits right now, 0 to 1, mostly from the bass and heavily
 * smoothed, so things breathe with it rather than flash. Read from the app's own
 * audio, so no microphone. Rests at 0 when [on] is false.
 */
@Composable
internal fun rememberBeat(on: Boolean): MutableFloatState {
    val beat = remember { mutableFloatStateOf(0f) }
    DisposableEffect(on) {
        if (on) AudioLevels.hold()
        onDispose { if (on) AudioLevels.release() }
    }
    LaunchedEffect(on) {
        if (!on) { beat.floatValue = 0f; return@LaunchedEffect }
        var last = 0L
        var peak = 0.05f
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0f, 0.1f)
                last = now
                val level = AudioLevels.at(System.nanoTime())?.let { it.bass * 0.75f + it.level * 0.25f } ?: 0f
                peak = maxOf(level, peak * (1f - dt * 0.3f)).coerceAtLeast(0.02f)
                val target = (level / peak).coerceIn(0f, 1f)
                beat.floatValue += (target - beat.floatValue) * (dt * if (target > beat.floatValue) 9f else 2.5f).coerceAtMost(1f)
            }
        }
    }
    return beat
}

private const val DOT_ID = "dot"
private const val GREETING_HOLD_MS = 6_000L
