package com.tunelle.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunelle.data.devicesound.DeviceSound
import com.tunelle.data.settings.AppSettings
import com.tunelle.data.settings.EqPreset
import com.tunelle.ui.theme.Icon
import com.tunelle.ui.theme.Text

/** The presets worth one tap from the player; the rest are on the equalizer page. */
private val QUICK_PRESETS = listOf(EqPreset.FLAT, EqPreset.STUDIO, EqPreset.HIFI, EqPreset.WARM, EqPreset.BASS, EqPreset.DEEP_BASS, EqPreset.VOCAL_CLARITY, EqPreset.LATE_NIGHT)

private fun formatDb(v: Float): String = when {
    kotlin.math.abs(v) < 0.05f -> "0 dB"
    else -> (if (v > 0) "+" else "−") + "%.1f".format(kotlin.math.abs(v)).removeSuffix(".0") + " dB"
}

/**
 * The sound of what's playing, tuned without leaving the player: bass, treble
 * and a few presets for the device the music is going to, whether that device
 * keeps its own sound, and the way to the full equalizer.
 */
@Composable
fun QuickSound(onOpenEqualizer: (() -> Unit)?, modifier: Modifier = Modifier) {
    val eq by AppSettings.equalizer.collectAsState()
    val device by DeviceSound.current.collectAsState()
    val perDevice by DeviceSound.enabled.collectAsState()
    val preset = EqPreset.entries.firstOrNull { it.bands == eq.bands }

    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 12.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(device.kind.icon(), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (perDevice) "Sound for" else "Sound", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (perDevice) device.name else "Every device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Switch(eq.enabled, { v -> AppSettings.updateEqualizer { it.copy(enabled = v) } })
        }

        ToneSlider("Bass", eq.bassDb) { v -> AppSettings.updateEqualizer { it.copy(enabled = true, bassDb = v) } }
        ToneSlider("Treble", eq.trebleDb) { v -> AppSettings.updateEqualizer { it.copy(enabled = true, trebleDb = v) } }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QUICK_PRESETS.forEach { p ->
                FilterChip(
                    selected = eq.enabled && preset == p,
                    onClick = { AppSettings.updateEqualizer { it.copy(enabled = true, bands = p.bands) } },
                    label = { Text(p.label) },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Switch) { DeviceSound.setEnabled(!perDevice) }.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Remember for this device", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (perDevice) "Each speaker and pair of headphones keeps its own sound" else "One sound for everything",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(perDevice, DeviceSound::setEnabled)
        }

        if (onOpenEqualizer != null) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpenEqualizer).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Equalizer, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Full equalizer", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(preset?.label ?: "Custom", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ToneSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(formatDb(value), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = { v -> onChange((v * 2).toInt() / 2f) },
            valueRange = -12f..12f,
        )
    }
}
