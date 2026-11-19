package com.tunelle.ui.theme

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Tunelle's arch: the tunnel from the Echo mark as a frame. A full half-circle
 * across the top, straight sides, and gently rounded feet. Covers and portraits
 * take it in the Signal look, so the app has one silhouette of its own.
 */
class ArchShape(private val foot: Dp = 10.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = size.width / 2f
        // A frame shorter than half its width can't hold the half-circle: fall back to a pill top.
        val top = minOf(r, size.height / 2f)
        val f = with(density) { foot.toPx() }.coerceAtMost(size.width / 4f)
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    topLeft = CornerRadius(r, top),
                    topRight = CornerRadius(r, top),
                    bottomRight = CornerRadius(f, f),
                    bottomLeft = CornerRadius(f, f),
                ),
            )
        }
        return Outline.Generic(path)
    }

    /** The arch's centre line at [inset] in from the edge, for drawing along it (the player's progress). */
    companion object {
        /**
         * A point [t] (0..1) of the way along the arch's centre line inset by [inset]:
         * up the left side from the foot, over the top, and down the right.
         * Returns the point and the path's total length.
         */
        fun along(t: Float, w: Float, h: Float, inset: Float): Triple<Float, Float, Float> {
            val r = w / 2f - inset
            val cx = w / 2f
            val cy = w / 2f
            val side = (h - inset) - cy
            val arc = Math.PI.toFloat() * r
            val total = side * 2 + arc
            val d = t.coerceIn(0f, 1f) * total
            return when {
                d <= side -> Triple(inset, h - inset - d, total)
                d <= side + arc -> {
                    val a = Math.PI + (d - side) / r
                    Triple(cx + r * kotlin.math.cos(a).toFloat(), cy + r * kotlin.math.sin(a).toFloat(), total)
                }
                else -> Triple(w - inset, cy + (d - side - arc), total)
            }
        }

        /** The arch's centre line inset by [inset], from the left foot round to the right foot. */
        fun line(w: Float, h: Float, inset: Float): Path = Path().apply {
            val r = w / 2f - inset
            moveTo(inset, h - inset)
            lineTo(inset, w / 2f)
            arcTo(Rect(inset, w / 2f - r, w - inset, w / 2f + r), 180f, 180f, false)
            lineTo(w - inset, h - inset)
        }
    }
}

/** The shape covers and portraits take: the arch in the Signal look, [otherwise] in the rest. */
@androidx.compose.runtime.Composable
fun coverShape(otherwise: Shape): Shape = if (LocalDotTitles.current) ArchShape() else otherwise

/**
 * The track the player's progress runs along: an arch standing [gap] outside the
 * cover's [cover] rectangle, up its left side, over the top and down the right,
 * its feet level with the cover's foot.
 */
class ArchTrack(cover: androidx.compose.ui.geometry.Rect, gap: Float) {
    private val left = cover.left - gap
    private val right = cover.right + gap
    private val bottom = cover.bottom
    private val cx = (left + right) / 2f
    private val r = (right - left) / 2f
    private val cy = cover.top - gap + r
    private val side = (bottom - cy).coerceAtLeast(0f)
    private val arc = Math.PI.toFloat() * r
    val length = side * 2 + arc

    /** The point [t] (0..1) of the way round. */
    fun at(t: Float): androidx.compose.ui.geometry.Offset {
        val d = t.coerceIn(0f, 1f) * length
        return when {
            d <= side -> androidx.compose.ui.geometry.Offset(left, bottom - d)
            d <= side + arc -> {
                val a = Math.PI + (d - side) / r
                androidx.compose.ui.geometry.Offset(cx + r * kotlin.math.cos(a).toFloat(), cy + r * kotlin.math.sin(a).toFloat())
            }
            else -> androidx.compose.ui.geometry.Offset(right, cy + (d - side - arc))
        }
    }

    /** How far round (0..1) the point on the track nearest [p] is, and how far [p] is from it. */
    fun nearest(p: androidx.compose.ui.geometry.Offset): Pair<Float, Float> {
        val yLeft = p.y.coerceIn(cy, bottom)
        val onLeft = (bottom - yLeft) to kotlin.math.hypot(p.x - left, p.y - yLeft)
        val onRight = (side + arc + (yLeft - cy)) to kotlin.math.hypot(p.x - right, p.y - yLeft)
        val angle = kotlin.math.atan2((p.y - cy).coerceAtMost(0f).toDouble(), (p.x - cx).toDouble())
        val a = (if (angle <= 0) angle + 2 * Math.PI else angle).coerceIn(Math.PI, 2 * Math.PI)
        val q = at(((side + (a - Math.PI) * r) / length).toFloat())
        val onArc = (side + ((a - Math.PI) * r).toFloat()) to kotlin.math.hypot(p.x - q.x, p.y - q.y)
        val best = listOf(onLeft, onRight, onArc).minBy { it.second }
        return (best.first / length).coerceIn(0f, 1f) to best.second
    }

    /** The whole track as a path. */
    fun path(): Path = Path().apply {
        moveTo(left, bottom)
        lineTo(left, cy)
        arcTo(androidx.compose.ui.geometry.Rect(left, cy - r, right, cy + r), 180f, 180f, false)
        lineTo(right, bottom)
    }
}
