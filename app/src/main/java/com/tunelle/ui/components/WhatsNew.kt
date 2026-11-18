package com.tunelle.ui.components

import android.content.Context
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Button
import com.tunelle.ui.theme.Icon
import androidx.compose.material3.MaterialTheme
import com.tunelle.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * What changed, shown once after an update: the notes for each version
 * since the one last opened, newest first. Not shown on a fresh install.
 */
object WhatsNew {
    data class Note(val icon: ImageVector, val title: String, val line: String)

    /**
     * Newest first. Add the coming version's notes at the top when releasing.
     * Empty for now: OpenTune's notes were for OpenTune's own versions.
     */
    val NOTES: List<Pair<String, List<Note>>> = emptyList()

    private const val PREFS = "whats_new"

    /**
     * The notes to show now, if any, and remembers the version as seen. A
     * fresh install has nothing new to tell, so it only remembers.
     */
    fun due(context: Context, version: String): List<Pair<String, List<Note>>> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = p.getString("seen", null)
        if (seen == version) return emptyList()
        p.edit().putString("seen", version).apply()
        val fresh = seen == null && runCatching {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0)
            }
            info.firstInstallTime == info.lastUpdateTime
        }.getOrDefault(true)
        if (fresh) return emptyList()
        return since(seen, version, com.tunelle.BuildConfig.CHANGELOG)
    }

    /**
     * The notes for versions after [seen] up to [version], at most three
     * versions. When [version] has none of its own (it was released before
     * they were written), its store [changelog] stands in, so an update
     * from 0.4.5 to 0.4.7 never opens on 0.4.6's notes.
     */
    internal fun since(seen: String?, version: String, changelog: String = "", all: List<Pair<String, List<Note>>> = NOTES): List<Pair<String, List<Note>>> {
        val notes = all.filter { (v, _) -> compare(v, version) <= 0 && (seen == null || compare(v, seen) > 0) }
        val own = notes.firstOrNull()?.first?.let { compare(it, version) == 0 } == true
        val stand = if (own) null else fromChangelog(changelog).takeIf { it.isNotEmpty() }?.let { version to it }
        return (listOfNotNull(stand) + notes).take(3)
    }

    /** A store changelog's "• line" items as notes. */
    internal fun fromChangelog(text: String): List<Note> =
        text.lineSequence().map { it.trim().removePrefix("•").removePrefix("-").trim() }.filter { it.isNotEmpty() }
            .map { Note(Icons.Rounded.NewReleases, it, "") }.toList()

    internal fun compare(a: String, b: String): Int {
        val x = a.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val c = (x.getOrElse(i) { 0 }).compareTo(y.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }
}

/** A release cover, readable feature cards, and older changes tucked into version groups. */
@Composable
fun WhatsNewSheet(notes: List<Pair<String, List<WhatsNew.Note>>>, version: String, onDismiss: () -> Unit) {
    val ownNotes = notes.firstOrNull()?.first == version
    FloatingCard(
        onDismiss = onDismiss,
        title = com.tunelle.i18n.Tr.f("What's new in {0}", version),
        actions = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) { Text("Start listening") }
        },
    ) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ReleaseHero(version, "Installed · ready to listen")
            if (notes.isEmpty()) Text("You're up to date. Enjoy your music.", Modifier.padding(16.dp))
            notes.forEachIndexed { group, (releaseVersion, list) ->
                if (group == 0) {
                    Text(if (ownNotes) "The highlights" else com.tunelle.i18n.Tr.f("In {0}", releaseVersion),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp).semantics { heading() })
                    list.forEachIndexed { index, note -> NoteRow(note, index) }
                } else {
                    var expanded by rememberSaveable(releaseVersion) { mutableStateOf(false) }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)) {
                        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(com.tunelle.i18n.Tr.f("Earlier · {0}", releaseVersion), style = MaterialTheme.typography.titleMedium)
                                Text(com.tunelle.i18n.Tr.f("{0} changes", list.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                if (expanded) "Collapse changes" else "Expand changes")
                        }
                        if (expanded) list.forEachIndexed { index, note -> NoteRow(note, index) }
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun NoteRow(note: WhatsNew.Note, index: Int) {
    val ui by com.tunelle.data.settings.AppSettings.ui.collectAsState()
    val shown = remember { Animatable(if (ui.reduceAnimation) 1f else 0f) }
    LaunchedEffect(ui.reduceAnimation) {
        if (ui.reduceAnimation) shown.snapTo(1f) else {
            delay(60L + index.coerceAtMost(5) * 45L)
            shown.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = 300f))
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = shown.value.coerceIn(0f, 1f)
                translationY = (1f - shown.value) * 24.dp.toPx()
            }
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(18.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(note.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (note.line.isNotEmpty()) Text(note.line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
