package com.tunelle.ui.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.SpeakerPhone
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunelle.data.devicesound.DeviceProfile
import com.tunelle.data.devicesound.DeviceSound
import com.tunelle.data.devicesound.OutputKind
import com.tunelle.data.settings.AppSettings
import com.tunelle.data.settings.EqPreset
import com.tunelle.ui.components.GroupCard
import com.tunelle.ui.components.ToggleRow
import com.tunelle.ui.theme.Icon
import com.tunelle.ui.theme.Text

internal fun OutputKind.icon(): ImageVector = when (this) {
    OutputKind.SPEAKER -> Icons.Rounded.SpeakerPhone
    OutputKind.WIRED -> Icons.Rounded.Headphones
    OutputKind.BLUETOOTH -> Icons.Rounded.Bluetooth
    OutputKind.USB -> Icons.Rounded.Usb
}

/** A short line about a saved sound: its preset or "Custom", and the tone controls when moved. */
private fun describe(p: DeviceProfile): String {
    val eq = p.equalizer
    val parts = mutableListOf<String>()
    parts += if (!eq.enabled) com.tunelle.i18n.Tr.t("Equalizer off")
    else EqPreset.entries.firstOrNull { it.bands == eq.bands }?.label?.let(com.tunelle.i18n.Tr::t) ?: com.tunelle.i18n.Tr.t("Custom curve")
    fun db(v: Float) = (if (v > 0) "+" else "") + (if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)) + " dB"
    if (eq.bassDb != 0f) parts += com.tunelle.i18n.Tr.f("Bass {0}", db(eq.bassDb))
    if (eq.trebleDb != 0f) parts += com.tunelle.i18n.Tr.f("Treble {0}", db(eq.trebleDb))
    if (p.bassBoost > 0) parts += com.tunelle.i18n.Tr.t("Bass boost")
    eq.headphone?.let { parts += it.name }
    return parts.joinToString(" · ")
}

/**
 * Sound per device, at the top of the equalizer: which output is being tuned
 * now, the switch for remembering a sound per device, and the others saved.
 */
@Composable
internal fun DeviceSoundCard() {
    val enabled by DeviceSound.enabled.collectAsState()
    val current by DeviceSound.current.collectAsState()
    val saved by DeviceSound.profiles.collectAsState()
    // Read so the list redraws as the live sound changes.
    AppSettings.equalizer.collectAsState().value
    val others = saved.values.filter { it.device.key != current.key }.sortedByDescending { it.savedAt }

    GroupCard {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(current.kind.icon(), null, tint = MaterialTheme.colorScheme.onPrimary) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (enabled) "Tuning for" else "Playing through",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(current.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        ToggleRow(
            "Sound per device",
            enabled,
            DeviceSound::setEnabled,
            summary = if (enabled) "Your speaker, headphones and each Bluetooth device keep their own sound. It switches by itself when you connect"
            else "One sound for everything. Turn on to tune each speaker and pair of headphones on its own",
            icon = current.kind.icon(),
        )
        if (enabled && others.isNotEmpty()) {
            Column(Modifier.padding(start = 20.dp, end = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Saved for", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                others.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(p.device.kind.icon(), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.device.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                            Text(describe(p), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                        IconButton(onClick = { DeviceSound.forget(p.device.key) }) {
                            Icon(Icons.Rounded.Close, com.tunelle.i18n.Tr.f("Forget {0}", p.device.name), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
