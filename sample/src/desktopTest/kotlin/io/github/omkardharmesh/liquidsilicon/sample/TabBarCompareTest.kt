package io.github.omkardharmesh.liquidsilicon.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.use
import io.github.omkardharmesh.liquidsilicon.GlassTab
import io.github.omkardharmesh.liquidsilicon.GlassTabBar
import io.github.omkardharmesh.liquidsilicon.LocalGlassContentColor
import io.github.omkardharmesh.liquidsilicon.layerBackdrop
import io.github.omkardharmesh.liquidsilicon.rememberLayerBackdrop
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

@OptIn(ExperimentalComposeUiApi::class)
class TabBarCompareTest {
    private fun shot(name: String, press: Boolean) {
        ImageComposeScene(1000, 600, Density(2.5f)) { CompareScene() }.use { scene ->
            var t = 0L
            repeat(20) { scene.render(t); t += 16_000_000L }
            if (press) {
                scene.sendPointerEvent(PointerEventType.Press, Offset(80f * 2.5f, 196f * 2.5f))
                repeat(10) { scene.render(t); t += 16_000_000L }
                scene.sendPointerEvent(PointerEventType.Move, Offset(130f * 2.5f, 196f * 2.5f))
                repeat(6) { scene.render(t); t += 16_000_000L }
            }
            val img = scene.render(t)
            File(System.getProperty("user.dir"), "build/screenshots/compare_$name.png").apply { parentFile.mkdirs() }
                .writeBytes(img.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    @Test fun rest() = shot("rest", false)
    @Test fun pressed() = shot("pressed", true)
}

@Composable
private fun CompareScene() {
    val backdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color(0xFFF2F2F7))) {
            Box(Modifier.fillMaxWidth().height(110.dp).background(Brush.linearGradient(listOf(Color(0xFF5E5CE6), Color(0xFFFF375F), Color(0xFFFF9F0A)))))
            repeat(3) { i ->
                Row(Modifier.fillMaxWidth().padding(16.dp, 8.dp).height(80.dp).background(Color.White, RoundedCornerShape(20.dp)).padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(Brush.linearGradient(listOf(Color(0xFF30D158), Color(0xFF0A84FF))), RoundedCornerShape(14.dp)))
                    Spacer(Modifier.width(12.dp))
                    BasicText("Episode ${i + 1}", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
        var selected by remember { mutableIntStateOf(0) }
        GlassTabBar(selectedIndex = selected, onTabSelected = { selected = it }, tabCount = 4, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 164.dp), backdrop = backdrop) {
            repeat(4) { index ->
                GlassTab(index == selected, { selected = index }) {
                    val c = LocalGlassContentColor.current
                    Canvas(Modifier.size(28.dp)) { drawCircle(c, size.minDimension / 2.6f) }
                    BasicText("Tab ${index + 1}", style = TextStyle(c, 12.sp))
                }
            }
        }
    }
}
