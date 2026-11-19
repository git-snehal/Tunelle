package com.tunelle.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunelle.data.settings.AppFont
import com.tunelle.data.settings.AppSettings
import com.tunelle.data.settings.AppStyle
import com.tunelle.data.settings.CornerStyle
import com.tunelle.data.settings.ThemeSettings
import com.tunelle.data.settings.TitleFont
import com.tunelle.data.settings.effective
import com.tunelle.ui.theme.DotMatrix
import com.tunelle.ui.theme.Outfit
import com.tunelle.ui.theme.SpaceGrotesk
import com.tunelle.ui.theme.Text

/**
 * Tunelle's looks side by side, each drawn as a small card in its own colours,
 * type and corners, so the choice is made by seeing rather than reading.
 */
@Composable
internal fun StylePicker(theme: ThemeSettings, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppStyle.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { style ->
                    StyleCard(
                        look = (if (style == AppStyle.CUSTOM && theme.style != AppStyle.CUSTOM) theme.effective() else theme.copy(style = style).effective()),
                        style = style,
                        selected = theme.style == style,
                        modifier = Modifier.weight(1f),
                    ) { AppSettings.updateTheme { t -> if (style == AppStyle.CUSTOM) t.effective().copy(style = AppStyle.CUSTOM) else t.copy(style = style) } }
                }
            }
        }
        Text(
            theme.style.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun cornerOf(c: CornerStyle): Dp = when (c) {
    CornerStyle.CRISP -> 8.dp
    CornerStyle.BALANCED -> 16.dp
    CornerStyle.ROUND -> 26.dp
}

private fun familyOf(f: AppFont): FontFamily = when (f) {
    AppFont.SPACE_GROTESK -> SpaceGrotesk
    AppFont.OUTFIT -> Outfit
    AppFont.SERIF -> FontFamily.Serif
    AppFont.MONO -> FontFamily.Monospace
    AppFont.SYSTEM, AppFont.CUSTOM -> FontFamily.SansSerif
}

@Composable
private fun StyleCard(look: ThemeSettings, style: AppStyle, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val accent = Color(look.seedColor)
    val corner = cornerOf(look.corners)
    val shape = RoundedCornerShape(corner)
    // Each card keeps its own small palette, so all four read the same in light and dark.
    val (paper, ink, card) = when (style) {
        AppStyle.SIGNAL -> Triple(Color(0xFF0B0B0C), Color(0xFFF4F4F5), Color(0xFF222225))
        AppStyle.SOFT -> Triple(Color(0xFFFFF1EA), Color(0xFF3A2620), Color(0xFFFFDCCB))
        AppStyle.VIVID -> Triple(Color(0xFF2A0F3D), Color.White, Color(0xFF7A2E8F))
        AppStyle.CUSTOM -> Triple(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surfaceContainerHighest)
    }
    val titleFamily = if (look.titleFont == TitleFont.DOT_MATRIX) DotMatrix else familyOf(look.font)
    Column(
        modifier
            .clip(shape)
            .background(
                if (style == AppStyle.VIVID) androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFFF5A36), Color(0xFF7A2E8F), Color(0xFF2A0F3D)))
                else androidx.compose.ui.graphics.SolidColor(paper),
            )
            .border(if (selected) 2.5.dp else 1.dp, if (selected) accent else ink.copy(alpha = 0.15f), shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = "${style.label} style" }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                style.label,
                color = ink,
                fontFamily = titleFamily,
                fontWeight = if (look.titleFont == TitleFont.DOT_MATRIX) FontWeight.Black else FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.size(14.dp).clip(CircleShape).background(accent))
        }
        Spacer(Modifier.height(10.dp))
        // A tiny "now playing" row: cover, two text lines and the accent play button.
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(corner * 0.6f)).background(card).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(26.dp).clip(RoundedCornerShape(corner * 0.35f)).background(ink.copy(alpha = 0.25f)))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Aa Song", color = ink, fontFamily = familyOf(look.font), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                Box(Modifier.padding(top = 4.dp).fillMaxWidth(0.6f).height(4.dp).clip(CircleShape).background(ink.copy(alpha = 0.3f)))
            }
            Box(Modifier.size(22.dp).clip(CircleShape).background(accent))
        }
    }
}
