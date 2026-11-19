package com.tunelle.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunelle.data.ExploreFeed
import com.tunelle.data.MusicLanguage
import com.tunelle.data.devicesound.DeviceSound
import com.tunelle.data.model.BrowseType
import com.tunelle.data.model.ShelfItem
import com.tunelle.data.settings.AppSettings
import com.tunelle.data.settings.EqPreset
import com.tunelle.ui.components.ECHO_DOT
import com.tunelle.ui.components.ECHO_INNER
import com.tunelle.ui.components.ECHO_MIDDLE
import com.tunelle.ui.components.ECHO_OUTER
import com.tunelle.ui.components.EMBER
import com.tunelle.ui.components.ItemCard

import com.tunelle.ui.components.Shelf
import com.tunelle.ui.components.echoPath
import com.tunelle.ui.settings.icon
import com.tunelle.ui.theme.DotMatrix
import com.tunelle.ui.theme.Icon
import com.tunelle.ui.theme.LocalDotTitles
import com.tunelle.ui.theme.Text
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The top of Home's bar: the Echo mark, small, and today's date beside it,
 * read like a display ("WED · 19 NOV") in the dot-matrix looks.
 */
@Composable
internal fun HomeBrand(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val arches = remember { listOf(echoPath(ECHO_OUTER) to 0.45f, echoPath(ECHO_MIDDLE) to 0.75f, echoPath(ECHO_INNER) to 1f) }
    val dot = remember { echoPath(ECHO_DOT) }
    val today = remember {
        val d = LocalDate.now()
        d.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase() + " · " +
            d.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())).uppercase()
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(30.dp)) {
            // The mark's own drawing spans about 24..84 across its 108 units; that span fills the box.
            val s = size.width / MARK_SPAN
            scale(s, pivot = Offset(0f, 0f)) {
                translate(-23f, -21.5f) {
                    arches.forEach { (p, a) -> drawPath(p, ink, alpha = a) }
                    drawPath(dot, EMBER)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            today,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = if (LocalDotTitles.current) DotMatrix else null,
            fontWeight = if (LocalDotTitles.current) FontWeight.Black else FontWeight.Medium,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * Where the music is going and how it's tuned there ("AirBass Headphone ·
 * Studio"), as a pill under the greeting. A tap opens the equalizer.
 */
@Composable
internal fun SoundChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val device by DeviceSound.current.collectAsState()
    val eq by AppSettings.equalizer.collectAsState()
    val sound = when {
        !eq.enabled -> com.tunelle.i18n.Tr.t("Equalizer off")
        else -> EqPreset.entries.firstOrNull { it.bands == eq.bands && eq.bassDb == 0f && eq.trebleDb == 0f }?.label?.let(com.tunelle.i18n.Tr::t)
            ?: buildList {
                if (eq.bassDb != 0f) add(com.tunelle.i18n.Tr.f("Bass {0}", (if (eq.bassDb > 0) "+" else "") + "%.1f".format(eq.bassDb).removeSuffix(".0")))
                if (eq.trebleDb != 0f) add(com.tunelle.i18n.Tr.f("Treble {0}", (if (eq.trebleDb > 0) "+" else "") + "%.1f".format(eq.trebleDb).removeSuffix(".0")))
            }.joinToString(" ").ifEmpty { com.tunelle.i18n.Tr.t("Custom sound") }
    }
    Row(
        modifier
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(role = Role.Button, onClickLabel = com.tunelle.i18n.Tr.t("Tune the sound"), onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) { Icon(device.kind.icon(), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary) }
        Spacer(Modifier.width(10.dp))
        Text(device.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(
            "  ·  $sound",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** How much of the mark's 108-unit space its drawing takes, edge to edge. */
private const val MARK_SPAN = 62f

/** Artists for the welcome's languages, fetched once a session. */
private object LanguageArtists {
    var key: String? = null
    var artists: List<ExploreFeed.Artist> = emptyList()
}

/**
 * Artists who sing in the languages picked on the welcome, a row of round
 * faces. Nothing shows when no language was picked or none could be found.
 */
@Composable
internal fun LanguageArtistsShelf(onItemClick: (ShelfItem) -> Unit) {
    val library by AppSettings.library.collectAsState()
    val languages = library.musicLanguages.mapNotNull(MusicLanguage::of)
    if (languages.isEmpty()) return
    val key = languages.joinToString(",") { it.name }
    val artists by produceState(if (LanguageArtists.key == key) LanguageArtists.artists else emptyList(), key) {
        if (LanguageArtists.key == key && LanguageArtists.artists.isNotEmpty()) return@produceState
        val found = runCatching { MusicLanguage.artists(languages, perLanguage = 10) }.getOrDefault(emptyList())
        LanguageArtists.key = key
        LanguageArtists.artists = found
        value = found
    }
    if (artists.isEmpty()) return
    val names = languages.map { com.tunelle.i18n.Tr.t(it.label) }
    Shelf(
        title = "In your languages",
        subtitle = names.take(3).joinToString(" · "),
        items = artists,
    ) { a ->
        ItemCard(
            a.name, com.tunelle.i18n.Tr.t("Artist"), a.thumbnailUrl, BrowseType.ARTIST,
            onClick = { onItemClick(ShelfItem(a.name, "", a.thumbnailUrl, null, a.browseId)) },
            width = 128.dp,
        )
    }
}
