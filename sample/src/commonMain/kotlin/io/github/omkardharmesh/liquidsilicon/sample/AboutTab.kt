package io.github.omkardharmesh.liquidsilicon.sample

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.omkardharmesh.liquidsilicon.GlassShape
import io.github.omkardharmesh.liquidsilicon.GlassSurface
import io.github.omkardharmesh.liquidsilicon.GlassText
import io.github.omkardharmesh.liquidsilicon.glassContentColor

internal data class PlatformSummary(val platform: String, val device: String, val renderer: String)

internal expect fun platformSummary(): PlatformSummary

@Composable
internal fun AboutBackground() {
    LiveScene(
        Modifier.fillMaxSize(),
        listOf(Color(0xFFF2F2F7), Color(0xFF64D2FF), Color(0xFFFF9F0A), Color(0xFFBF5AF2), Color(0xFF30D158))
    )
}

@Composable
internal fun BoxScope.AboutOverlay(tiltLight: Boolean, onTiltLightChange: (Boolean) -> Unit) {
    val info = remember { platformSummary() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = TabBarClearance + 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GlassSurface(Modifier.fillMaxWidth(), shape = GlassShape.Card, contentAlignment = Alignment.TopStart) {
            Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    rememberVectorPainter(AppLogo.Full),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    GlassText("LiquidSilicon", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Black))
                    Spacer(Modifier.height(2.dp))
                    GlassText("iOS 26–style glass for Compose Multiplatform", style = TextStyle(fontSize = 13.sp))
                    Spacer(Modifier.height(10.dp))
                    FpsBadge()
                }
            }
        }
        InfoCard("This device") {
            InfoRow("Platform", info.platform)
            InfoRow("Device", info.device)
            InfoRow("Renderer", info.renderer)
        }
        InfoCard("Settings") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    GlassText("Tilt-driven highlights", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                    GlassText(
                        "Rim light follows how you hold the phone",
                        style = TextStyle(fontSize = 12.sp),
                        color = glassContentColor().copy(alpha = 0.65f)
                    )
                }
                GlassSwitch(tiltLight, onTiltLightChange)
            }
        }
        InfoCard("Try this") {
            Bullet("Press and drag the tab bar — the selection lifts into a lens.")
            Bullet("Shorts → Share: new buttons melt out of the share button.")
            Bullet("Home: scroll the hero under the Play buttons to see refraction.")
            Bullet("Lab: change blur, bend, prism and tint live, then drag the lens.")
        }
        InfoCard("What's inside") {
            Bullet("Edge refraction with chromatic dispersion")
            Bullet("Shapes that merge like SwiftUI's GlassEffectContainer")
            Bullet("Automatic light/dark content colour")
            Bullet("Fallbacks for Android 7–12")
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(Modifier.fillMaxWidth(), shape = GlassShape.RoundedRect(24.dp), contentAlignment = Alignment.TopStart) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassText(
                title.uppercase(),
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = glassContentColor().copy(alpha = 0.6f)
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        GlassText(label, Modifier.width(84.dp), style = TextStyle(fontSize = 14.sp), color = glassContentColor().copy(alpha = 0.65f))
        GlassText(value, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun Bullet(text: String) {
    Row {
        Box(
            Modifier
                .padding(top = 7.dp, end = 10.dp)
                .size(6.dp)
                .background(Accent, CircleShape)
        )
        GlassText(text, style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp))
    }
}

@Composable
private fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val knob by animateDpAsState(if (checked) 22.dp else 0.dp, spring(dampingRatio = 0.6f, stiffness = 500f), label = "knob")
    Box(
        Modifier
            .size(width = 54.dp, height = 32.dp)
            .background(if (checked) Color(0xFF30D158) else glassContentColor().copy(alpha = 0.2f), CircleShape)
            .clickable(interactionSource = null, indication = null) { onCheckedChange(!checked) }
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .offset(x = knob)
                .size(26.dp)
                .background(Color.White, CircleShape)
        )
    }
}
