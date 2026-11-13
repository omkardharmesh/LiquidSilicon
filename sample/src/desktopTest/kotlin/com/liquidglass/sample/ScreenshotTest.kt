package com.liquidglass.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.use
import com.liquidglass.GlassShape
import com.liquidglass.GlassStyle
import com.liquidglass.glass
import com.liquidglass.layerBackdrop
import com.liquidglass.rememberLayerBackdrop
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class ScreenshotTest {

    private val outDir = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
    private val density = Density(2.5f)
    private val width = (400 * 2.5f).toInt()
    private val height = (860 * 2.5f).toInt()

    private fun capture(
        name: String,
        w: Int = width,
        h: Int = height,
        scale: Float = 2.5f,
        actions: ImageComposeScene.(Long) -> Long = { it },
        content: @Composable () -> Unit
    ) {
        ImageComposeScene(w, h, Density(scale), content = content).use { scene ->
            var time = 0L
            repeat(30) {
                scene.render(time)
                time += 16_000_000L
            }
            time = scene.actions(time)
            val image = scene.render(time)
            val data = image.encodeToData(EncodedImageFormat.PNG)!!
            val file = File(outDir, "$name.png")
            file.writeBytes(data.bytes)
            assertTrue(file.length() > 0)
        }
    }

    @Test
    fun home() = capture("home") { SampleApp() }

    @Test
    fun darkSection() = capture("dark") { SampleApp(initialScroll = (1612 * 2.5f).toInt()) }

    @Test
    fun pressed() = capture(
        "pressed",
        actions = { start ->
            sendPointerEvent(PointerEventType.Press, Offset(80f * 2.5f, 196f * 2.5f))
            var t = start
            repeat(12) {
                t += 16_000_000L
                render(t)
            }
            t
        }
    ) { SampleApp() }

    @Test
    fun morph() = capture(
        "morph",
        actions = { start ->
            val p = Offset(360f * 2.5f, 728f * 2.5f)
            sendPointerEvent(PointerEventType.Press, p)
            sendPointerEvent(PointerEventType.Release, p)
            var t = start
            repeat(14) {
                t += 16_000_000L
                render(t)
            }
            t
        }
    ) { SampleApp() }

    @Test
    fun tabDrag() = capture(
        "tabdrag",
        actions = { start ->
            var t = start
            sendPointerEvent(PointerEventType.Press, Offset(58f * 2.5f, 816f * 2.5f))
            repeat(8) {
                t += 16_000_000L
                render(t)
            }
            for (i in 1..10) {
                sendPointerEvent(PointerEventType.Move, Offset((58f + i * 8f) * 2.5f, 816f * 2.5f))
                t += 16_000_000L
                render(t)
            }
            t
        }
    ) { SampleApp() }

    @Test
    fun closeup() = capture("closeup", w = 900, h = 520, scale = 3f) { Closeup() }
}

@Composable
private fun Closeup() {
    Box(Modifier.fillMaxSize()) {
        val backdrop = rememberLayerBackdrop()
        Canvas(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            drawRect(Color.White)
            val step = 12.dp.toPx()
            var x = 0f
            var i = 0
            while (x < size.width) {
                drawRect(
                    if (i % 2 == 0) Color(0xFF0A84FF) else Color(0xFFFF375F),
                    topLeft = Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(step / 2, size.height)
                )
                x += step
                i++
            }
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .width(220.dp)
                .height(110.dp)
                .glass(GlassShape.RoundedRect(32.dp), GlassStyle.Regular.copy(adaptive = false), backdrop = backdrop),
            contentAlignment = Alignment.Center
        ) {
            BasicText("Glass", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold))
        }
    }
}
