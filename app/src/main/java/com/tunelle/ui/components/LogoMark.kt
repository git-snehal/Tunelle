package com.tunelle.ui.components

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

/**
 * Echo, the Tunelle mark, as path data in the launcher icon's 108 by 108 space:
 * the same shapes as res/drawable/ic_launcher_foreground.xml, for drawing at any
 * size. Three arches of sound spreading out through a tunnel, fading as they go,
 * and a point of ember light inside, which the opening turns into a music note.
 */
internal const val MARK_VIEWPORT = 108f

internal const val ECHO_OUTER = "M24.12,80.70 V54.00 A29.88,29.88 0 0 1 83.88,54.00 V80.70 A1.08,1.08 0 0 1 81.72,80.70 V54.00 A27.72,27.72 0 0 0 26.28,54.00 V80.70 A1.08,1.08 0 0 1 24.12,80.70 Z"
internal const val ECHO_MIDDLE = "M31.38,80.70 V54.00 A22.62,22.62 0 0 1 76.62,54.00 V80.70 A1.62,1.62 0 0 1 73.38,80.70 V54.00 A19.38,19.38 0 0 0 34.62,54.00 V80.70 A1.62,1.62 0 0 1 31.38,80.70 Z"
internal const val ECHO_INNER = "M38.40,80.70 V54.00 A15.60,15.60 0 0 1 69.60,54.00 V80.70 A2.40,2.40 0 0 1 64.80,80.70 V54.00 A10.80,10.80 0 0 0 43.20,54.00 V80.70 A2.40,2.40 0 0 1 38.40,80.70 Z"
internal const val ECHO_DOT = "M49.56,73.50 A4.44,4.44 0 1 1 58.44,73.50 A4.44,4.44 0 1 1 49.56,73.50 Z"

/** The music note the ember point becomes, standing inside the inner arch. */
internal const val ECHO_NOTE = "M57.21,71.63 A4.75,3.70 -20 1 1 48.29,74.87 A4.75,3.70 -20 1 1 57.21,71.63 Z M56.00,55.75 H58.10 V72.75 H56.00 Z M56.00,54.75 L58.00,54.75 Q64.25,57.25 64.25,64.75 L62.25,64.75 Q61.75,58.75 56.00,56.75 Z"

/** The opening of the tunnel: everything inside the inner arch, which the opening flies through. */
internal const val ECHO_HOLE = "M43.20,80.70 V54.00 A10.80,10.80 0 0 1 64.80,54.00 V80.70 Z"

/** The Tunelle mark's accent. */
internal val EMBER = androidx.compose.ui.graphics.Color(0xFFFF5A36)

internal fun echoPath(data: String): Path = PathParser().parsePathString(data).toPath()

/** The whole mark as one [Path] in [MARK_VIEWPORT] units. */
internal fun markPath(): Path = echoPath("$ECHO_OUTER $ECHO_MIDDLE $ECHO_INNER $ECHO_DOT")
