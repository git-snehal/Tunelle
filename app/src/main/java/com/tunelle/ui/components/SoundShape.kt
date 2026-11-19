package com.tunelle.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.tunelle.data.settings.AppSettings

/**
 * Your sound, drawn as the Echo mark: the inner arch thickens with bass, the
 * outer ones brighten and firm up with treble, and with the equalizer off the
 * mark rests thin and plain. Each output keeps its own sound, so headphones and
 * the speaker each have their own shape. [pulse] (0..1) swells it outward, for
 * the moment a new device takes over.
 */
@Composable
fun SoundShape(modifier: Modifier = Modifier, ink: Color, pulse: () -> Float = { 0f }) {
    val eq by AppSettings.equalizer.collectAsState()
    val boost by AppSettings.sound.collectAsState()
    val on = eq.enabled
    // Bass counts the tone control, the lowest bands and bass boost; treble the tone control and the top bands.
    val bassDb = if (on) eq.bassDb + eq.bands.take(4).average().toFloat() + boost.bassBoost / 160f else 0f
    val trebleDb = if (on) eq.trebleDb + eq.bands.takeLast(4).average().toFloat() else 0f
    val bass by animateFloatAsState((bassDb / 12f).coerceIn(-1f, 1f), spring(dampingRatio = 0.6f, stiffness = 220f), label = "shapeBass")
    val treble by animateFloatAsState((trebleDb / 12f).coerceIn(-1f, 1f), spring(dampingRatio = 0.6f, stiffness = 220f), label = "shapeTreble")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = pulse().coerceIn(0f, 1f)
        val unit = w / 24f
        // Three arches standing on the foot, the outer ones widening a little with the pulse.
        listOf(
            Triple(0.46f, (1.1f + treble * 0.7f).coerceAtLeast(0.5f), (0.45f + treble * 0.35f).coerceIn(0.15f, 1f)),
            Triple(0.33f, (1.5f + treble * 0.4f + bass * 0.3f).coerceAtLeast(0.6f), (0.75f + treble * 0.2f).coerceIn(0.3f, 1f)),
            Triple(0.20f, (2.0f + bass * 1.4f).coerceAtLeast(0.8f), 1f),
        ).forEachIndexed { i, (radius, width, alpha) ->
            val r = w * radius * (1f + p * 0.18f * (i + 1) / 3f)
            val cx = w / 2f
            val cy = h * 0.52f
            val path = Path().apply {
                moveTo(cx - r, h * 0.94f)
                lineTo(cx - r, cy)
                arcTo(Rect(cx - r, cy - r, cx + r, cy + r), 180f, 180f, false)
                lineTo(cx + r, h * 0.94f)
            }
            drawPath(path, ink.copy(alpha = alpha * (1f - p * 0.3f)), style = Stroke(width * unit, cap = StrokeCap.Round))
        }
        drawCircle(EMBER, radius = unit * (1.5f + p * 0.8f), center = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.82f))
    }
}
