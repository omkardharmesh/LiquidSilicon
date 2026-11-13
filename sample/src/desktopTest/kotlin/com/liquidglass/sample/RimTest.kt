package com.liquidglass.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import com.liquidglass.GlassShape
import com.liquidglass.GlassStyle
import com.liquidglass.glass
import com.liquidglass.layerBackdrop
import com.liquidglass.rememberLayerBackdrop
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

@OptIn(ExperimentalComposeUiApi::class)
class RimTest {

    @Test
    fun rim() {
        ImageComposeScene(1000, 500, Density(2.5f)) { RimScene() }.use { scene ->
            var t = 0L
            repeat(10) { scene.render(t); t += 16_000_000L }
            File(System.getProperty("user.dir"), "build/screenshots/rim.png").apply { parentFile.mkdirs() }
                .writeBytes(scene.render(t).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }
}

@Composable
private fun RimScene() {
    val backdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color(0xFFF2F2F7))) {
            Box(
                Modifier.offset(30.dp, 20.dp).size(60.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF30D158), Color(0xFF0A84FF))), RoundedCornerShape(14.dp))
            )
            Box(
                Modifier.offset(30.dp, 140.dp).size(60.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF5E5CE6), Color(0xFFFF375F))), RoundedCornerShape(14.dp))
            )
        }
        Box(
            Modifier.padding(start = 16.dp, end = 16.dp).offset(y = 72.dp).fillMaxWidth().height(80.dp)
                .glass(GlassShape.Capsule, GlassStyle.Regular.copy(adaptive = false), backdrop = backdrop)
        )
    }
}
