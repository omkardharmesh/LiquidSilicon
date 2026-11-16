package io.github.omkardharmesh.liquidsilicon.sample

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
import io.github.omkardharmesh.liquidsilicon.GlassShape
import io.github.omkardharmesh.liquidsilicon.GlassStyle
import io.github.omkardharmesh.liquidsilicon.glass
import io.github.omkardharmesh.liquidsilicon.layerBackdrop
import io.github.omkardharmesh.liquidsilicon.rememberLayerBackdrop
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class ScreenshotTest {

    private val outDir = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
    private val scale = 2.5f
    private val width = (400 * scale).toInt()
    private val height = (860 * scale).toInt()

    private fun ImageComposeScene.frames(start: Long, count: Int): Long {
        var t = start
        repeat(count) {
            t += 16_000_000L
            render(t)
        }
        return t
    }

    private fun ImageComposeScene.tap(x: Float, y: Float, start: Long): Long {
        val p = Offset(x * scale, y * scale)
        sendPointerEvent(PointerEventType.Press, p)
        sendPointerEvent(PointerEventType.Release, p)
        return frames(start, 40)
    }

    private fun capture(
        name: String,
        w: Int = width,
        h: Int = height,
        density: Float = scale,
        actions: ImageComposeScene.(Long) -> Long = { it },
        content: @Composable () -> Unit
    ) {
        ImageComposeScene(w, h, Density(density), content = content).use { scene ->
            var time = scene.frames(0L, 30)
            time = scene.actions(time)
            val image = scene.render(time)
            val file = File(outDir, "$name.png")
            file.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            assertTrue(file.length() > 0)
        }
    }

    @Test
    fun home() = capture("home") { SampleApp() }

    @Test
    fun homeScrolled() = capture("home_scrolled") { SampleApp(initialScroll = (420 * scale).toInt()) }

    @Test
    fun shorts() = capture("shorts") { SampleApp(initialTab = 1) }

    @Test
    fun shortsShare() = capture("shorts_share", actions = { tap(360f, 590f, it) }) { SampleApp(initialTab = 1) }

    @Test
    fun lab() = capture("lab") { SampleApp(initialTab = 2) }

    @Test
    fun about() = capture("about") { SampleApp(initialTab = 3) }

    @Test
    fun tabDrag() = capture(
        "tabdrag",
        actions = { start ->
            sendPointerEvent(PointerEventType.Press, Offset(71f * scale, 816f * scale))
            var t = frames(start, 8)
            for (i in 1..10) {
                sendPointerEvent(PointerEventType.Move, Offset((71f + i * 8f) * scale, 816f * scale))
                t = frames(t, 1)
            }
            t
        }
    ) { SampleApp() }

    @Test
    fun closeup() = capture("closeup", w = 900, h = 520, density = 3f) { Closeup() }

    @Test
    fun logo() = capture("logo", w = 1024, h = 1024, density = 1f) {
        androidx.compose.foundation.Image(
            androidx.compose.ui.graphics.vector.rememberVectorPainter(AppLogo.Full),
            null,
            Modifier.fillMaxSize()
        )
    }
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
