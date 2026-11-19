package com.tunelle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.ln

/** Plays once per launch of the app, not each time the screen is rebuilt. */
object Opening {
    var played = false
}

/** The splash's near-black, which the system splash screen and the icon use too. */
val OPENING_FIELD = Color(0xFF0B0B0C)

private val ARCH = Color(0xFFF4F4F5)

/**
 * The opening, telling the story in the name: sound through a tunnel becomes music.
 *
 * Echo takes over from the system splash at the same size. Its arches ripple
 * out one after another from the middle, the ember point inside grows into a
 * music note, and then the screen flies through the tunnel's opening, which
 * becomes a window onto the app as it rushes toward you. [onDive] reports the
 * dive, 0 to 1, so the app behind can come forward; [onDone] removes this.
 */
@Composable
fun OpeningReveal(onDive: (Float) -> Unit, onDone: () -> Unit) {
    val outer = remember { echoPath(ECHO_OUTER) }
    val middle = remember { echoPath(ECHO_MIDDLE) }
    val inner = remember { echoPath(ECHO_INNER) }
    val dot = remember { echoPath(ECHO_DOT) }
    val note = remember { echoPath(ECHO_NOTE) }
    val hole = remember { echoPath(ECHO_HOLE) }
    val ripple = remember { Animatable(0f) }
    val sing = remember { Animatable(0f) }
    val dive = remember { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    val report by rememberUpdatedState(onDive)
    LaunchedEffect(Unit) {
        ripple.animateTo(1f, tween(RIPPLE_MS, easing = LinearEasing))
        sing.animateTo(1f, tween(SING_MS, easing = CubicBezierEasing(0.3f, 1.4f, 0.5f, 1f)))
        kotlinx.coroutines.delay(HOLD_MS)
        // Slow to start and then rushing, like being pulled into the tunnel.
        dive.animateTo(1f, tween(DIVE_MS, easing = CubicBezierEasing(0.6f, 0f, 0.85f, 0.35f))) { report(value) }
        report(1f)
        done()
    }
    Canvas(
        Modifier
            .fillMaxSize()
            // Its own layer, so the opening cut through it shows the app below.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val r = ripple.value
        val n = sing.value
        val d = dive.value
        // The system splash draws the 108-unit icon 240dp across; start exactly there.
        val base = 240.dp.toPx() / MARK_VIEWPORT
        val s = base * exp(ln(ZOOM) * d * d)
        val fade = 1f - smooth(0.75f, 1f, d)
        // Zoom toward the middle of the tunnel's opening, so it's what fills the screen.
        val pivot = Offset(54f, 66f)

        drawRect(OPENING_FIELD, alpha = fade)
        translate(center.x - pivot.x * s, center.y - pivot.y * s) {
            scale(s, s, pivot = Offset.Zero) {
                // Each arch rises in a moment after the one inside it, like a ring of sound.
                arch(inner, smooth(0.00f, 0.45f, r), 1f, fade)
                arch(middle, smooth(0.20f, 0.70f, r), 0.75f, fade)
                arch(outer, smooth(0.40f, 0.95f, r), 0.45f, fade)
                // The point of light (there from the start, as on the icon), then the note it becomes.
                val point = 1f - n.coerceIn(0f, 1f)
                if (point > 0f) scale(0.6f + 0.4f * point, pivot = Offset(54f, 73.5f)) {
                    drawPath(dot, EMBER, alpha = point * fade)
                }
                if (n > 0f) scale(0.4f + 0.6f * n, pivot = Offset(54f, 70f)) {
                    drawPath(note, EMBER, alpha = n.coerceIn(0f, 1f) * fade)
                }
                // Then the opening becomes a window onto the app.
                val window = smooth(0.02f, 0.2f, d)
                if (window > 0f) drawPath(hole, Color.Black, alpha = window, blendMode = BlendMode.DstOut)
            }
        }
    }
}

/**
 * One arch. It starts exactly as the system splash drew it, so nothing jumps;
 * as the ripple passes through it swells outward and brightens, then settles.
 */
private fun DrawScope.arch(path: androidx.compose.ui.graphics.Path, t: Float, alpha: Float, fade: Float) {
    val pulse = kotlin.math.sin(Math.PI.toFloat() * t)
    scale(1f + 0.07f * pulse, pivot = Offset(54f, 80f)) {
        drawPath(path, ARCH, alpha = (alpha + (1f - alpha) * pulse) * fade)
    }
}

private fun smooth(from: Float, to: Float, x: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private const val RIPPLE_MS = 700
private const val SING_MS = 380
private const val HOLD_MS = 160L
private const val DIVE_MS = 850
/** How many times its size the mark grows to, enough for the tunnel's opening to fill the screen. */
private const val ZOOM = 60f
