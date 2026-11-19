package com.tunelle.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Tunelle's own icons for the main places, drawn on a 24 grid in one line
 * style: 1.8 strokes with round ends, and a solid (or heavier) form when the
 * place is open. Home is a small tunnel arch, after the Echo mark.
 */
object TunelleIcons {
    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private fun ImageVector.Builder.line(d: String, width: Float = 1.8f) = addPath(
        PathParser().parsePathString(d).toNodes(),
        stroke = SolidColor(Color.Black), strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
    )

    private fun ImageVector.Builder.solid(d: String, evenOdd: Boolean = false) = addPath(
        PathParser().parsePathString(d).toNodes(),
        fill = SolidColor(Color.Black),
        pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
    )



    /** A tunnel arch, two deep, after the Echo mark. */
    val Home = icon("Home") {
        line("M5,20 V11 A7,7 0 0 1 19,11 V20")
        line("M9,20 V12.5 A3,3 0 0 1 15,12.5 V20", width = 1.4f)
    }
    val HomeOpen = icon("HomeOpen") {
        solid("M4,20.5 V11 A8,8 0 0 1 20,11 V20.5 Z M9.4,20.5 V13 A2.6,2.6 0 0 1 14.6,13 V20.5 Z", evenOdd = true)
    }

    /** A dial with a needle: somewhere new. */
    val Explore = icon("Explore") {
        line("M3,12 A9,9 0 1 1 21,12 A9,9 0 1 1 3,12 Z")
        line("M12,6.8 L14.1,12 L12,17.2 L9.9,12 Z", width = 1.5f)
    }
    val ExploreOpen = icon("ExploreOpen") {
        solid("M2.5,12 A9.5,9.5 0 1 1 21.5,12 A9.5,9.5 0 1 1 2.5,12 Z M12,6.6 L9.7,12 L12,17.4 L14.3,12 Z", evenOdd = true)
    }

    /** A voice sending out rings. */
    val Podcasts = icon("Podcasts") {
        solid("M10.2,12.5 A1.8,1.8 0 1 1 13.8,12.5 A1.8,1.8 0 1 1 10.2,12.5 Z")
        line("M8.6,16.4 A5,5 0 1 1 15.4,16.4")
        line("M5.8,19.2 A9,9 0 1 1 18.2,19.2")
    }
    val PodcastsOpen = icon("PodcastsOpen") {
        solid("M9.6,12.5 A2.4,2.4 0 1 1 14.4,12.5 A2.4,2.4 0 1 1 9.6,12.5 Z")
        line("M8.6,16.4 A5,5 0 1 1 15.4,16.4", width = 2.6f)
        line("M5.8,19.2 A9,9 0 1 1 18.2,19.2", width = 2.6f)
    }

    /** Records on a shelf, one leaning. */
    val Library = icon("Library") {
        line("M5.5,4.5 V19.5")
        line("M10,4.5 V19.5")
        line("M14.2,5.2 L18.8,18.8")
    }
    val LibraryOpen = icon("LibraryOpen") {
        line("M5.5,4.5 V19.5", width = 3f)
        line("M10.5,4.5 V19.5", width = 3f)
        line("M14.6,5.2 L19.2,18.8", width = 3f)
    }

    /** A lens. */
    val Search = icon("Search") {
        line("M4,10.5 A6.5,6.5 0 1 1 17,10.5 A6.5,6.5 0 1 1 4,10.5 Z")
        line("M15.4,15.4 L20,20")
    }
    val SearchOpen = icon("SearchOpen") {
        line("M4,10.5 A6.5,6.5 0 1 1 17,10.5 A6.5,6.5 0 1 1 4,10.5 Z", width = 2.6f)
        line("M15.4,15.4 L20,20", width = 2.8f)
    }
}
