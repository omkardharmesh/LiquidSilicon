package io.github.omkardharmesh.liquidsilicon.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.omkardharmesh.liquidsilicon.GlassShape
import io.github.omkardharmesh.liquidsilicon.GlassStyle
import io.github.omkardharmesh.liquidsilicon.GlassSurface
import io.github.omkardharmesh.liquidsilicon.GlassText
import io.github.omkardharmesh.liquidsilicon.glass
import io.github.omkardharmesh.liquidsilicon.glassContentColor
import io.github.omkardharmesh.liquidsilicon.rememberGlassState
import kotlin.math.roundToInt

private val PanelStyle = GlassStyle.Regular.copy(adaptive = false, surface = Color(0xFF1C1C1E).copy(alpha = 0.55f))
private val Presets = listOf(GlassStyle.Regular, GlassStyle.Clear, GlassStyle.Bar)
private val Tints = listOf(Color.Unspecified, Color(0xFF0A84FF), Color(0xFFFF375F), Color(0xFF30D158), Color(0xFFFF9F0A))

@Stable
internal class LabState {
    var preset by mutableIntStateOf(0)
    var shape by mutableIntStateOf(0)
    var pattern by mutableIntStateOf(0)
    var blur by mutableFloatStateOf(GlassStyle.Regular.blurRadius.value)
    var bend by mutableFloatStateOf(GlassStyle.Regular.refractionAmount.value)
    var edge by mutableFloatStateOf(GlassStyle.Regular.refractionHeight.value)
    var prism by mutableFloatStateOf(GlassStyle.Regular.dispersion)
    var vibrance by mutableFloatStateOf(GlassStyle.Regular.saturation)
    var tint by mutableIntStateOf(0)
    var lensOffset by mutableStateOf(Offset.Unspecified)

    fun applyPreset(index: Int) {
        preset = index
        val style = Presets[index]
        blur = style.blurRadius.value
        bend = style.refractionAmount.value
        edge = style.refractionHeight.value
        prism = style.dispersion
        vibrance = style.saturation
    }

    fun style(): GlassStyle = Presets[preset].copy(
        blurRadius = blur.dp,
        refractionAmount = bend.dp,
        refractionHeight = edge.dp,
        dispersion = prism,
        saturation = vibrance,
        tint = Tints[tint]
    )
}

@Composable
internal fun LabBackground(lab: LabState) {
    when (lab.pattern) {
        0 -> Box(Modifier.fillMaxSize()) {
            LiveScene(
                Modifier.fillMaxSize(),
                listOf(Color(0xFF14141C), Color(0xFFFF375F), Color(0xFF0A84FF), Color(0xFFFFD60A), Color(0xFF30D158))
            )
            BasicText(
                "LIQUID\nGLASS",
                Modifier.align(Alignment.TopCenter).padding(top = 150.dp),
                style = TextStyle(Color.White, 72.sp, FontWeight.Black, lineHeight = 68.sp, textAlign = TextAlign.Center)
            )
        }

        1 -> TextPattern()
        else -> GridPattern()
    }
}

@Composable
private fun TextPattern() {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 22.dp, vertical = 60.dp)
    ) {
        repeat(4) { index ->
            BasicText(
                listOf("Refraction", "Dispersion", "Specular", "Adaptive tint")[index],
                style = TextStyle(listOf(Accent, Color(0xFFFF375F), Color(0xFF30D158), Color(0xFFFF9F0A))[index], 28.sp, FontWeight.Black)
            )
            BasicText(
                "Light bends as it passes through the curved rim of the glass, so the content behind it " +
                    "stretches towards the edges while the centre stays sharp and readable.",
                style = TextStyle(Color(0xFF1C1C1E), 17.sp, lineHeight = 24.sp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GridPattern() {
    Canvas(Modifier.fillMaxSize().background(Color.White)) {
        val step = 18.dp.toPx()
        var x = 0f
        var i = 0
        while (x < size.width) {
            drawRect(if (i % 2 == 0) Accent else Color(0xFFFF375F), Offset(x, 0f), Size(step / 2, size.height))
            x += step
            i++
        }
        var y = 0f
        while (y < size.height) {
            drawLine(Color.Black.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            y += step * 2
        }
    }
}

@Composable
internal fun BoxScope.LabOverlay(lab: LabState) {
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val lensSize = when (lab.shape) {
            0 -> DpSize(240.dp, 96.dp)
            1 -> DpSize(150.dp, 150.dp)
            else -> DpSize(240.dp, 150.dp)
        }
        val lensShape = when (lab.shape) {
            0 -> GlassShape.Capsule
            1 -> GlassShape.Circle
            else -> GlassShape.RoundedRect(36.dp)
        }
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val lensW = with(density) { lensSize.width.toPx() }
        val lensH = with(density) { lensSize.height.toPx() }
        val fallback = Offset((maxW - lensW) / 2f, with(density) { 140.dp.toPx() })
        val offset = if (lab.lensOffset.isSpecified) lab.lensOffset else fallback

        Box(
            Modifier
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .size(lensSize)
                .glass(lensShape, lab.style(), interactive = true)
                .pointerInput(maxW, maxH, lensW, lensH) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val current = if (lab.lensOffset.isSpecified) lab.lensOffset else fallback
                            val next = current + change.positionChange()
                            lab.lensOffset = Offset(next.x.coerceIn(0f, maxW - lensW), next.y.coerceIn(0f, maxH - lensH))
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            GlassText("Drag me", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
        }

        LabPanel(
            lab,
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp)
                .padding(bottom = TabBarClearance)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun LabPanel(lab: LabState, modifier: Modifier) {
    GlassSurface(
        modifier,
        shape = GlassShape.RoundedRect(28.dp),
        style = PanelStyle,
        state = rememberGlassState(initialLuminance = 0f),
        contentAlignment = Alignment.TopStart
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassText("Glass Lab", style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold))
                Spacer(Modifier.weight(1f))
                FpsBadge()
            }
            Segmented(listOf("Regular", "Clear", "Bar"), lab.preset, lab::applyPreset)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Segmented(listOf("Pill", "Circle", "Card"), lab.shape, { lab.shape = it; lab.lensOffset = Offset.Unspecified }, Modifier.weight(1f))
                Segmented(listOf("Scene", "Text", "Grid"), lab.pattern, { lab.pattern = it }, Modifier.weight(1f))
            }
            LabSlider("Blur", lab.blur, 0f..20f, { "${it.roundToInt()}dp" }) { lab.blur = it }
            LabSlider("Bend", lab.bend, 0f..40f, { "${it.roundToInt()}dp" }) { lab.bend = it }
            LabSlider("Edge", lab.edge, 4f..48f, { "${it.roundToInt()}dp" }) { lab.edge = it }
            LabSlider("Prism", lab.prism, 0f..0.6f, { "${(it * 100).roundToInt()}%" }) { lab.prism = it }
            LabSlider("Vibrance", lab.vibrance, 0f..2.5f, { "${(it * 10).roundToInt() / 10f}" }) { lab.vibrance = it }
            TintRow(lab.tint) { lab.tint = it }
        }
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val color = glassContentColor()
    Row(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(color.copy(alpha = 0.08f), CircleShape)
            .padding(3.dp)
    ) {
        options.forEachIndexed { index, option ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .then(if (index == selected) Modifier.background(color.copy(alpha = 0.18f), CircleShape) else Modifier)
                    .clickable(interactionSource = null, indication = null) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                GlassText(
                    option,
                    style = TextStyle(fontSize = 12.sp, fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Medium),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LabSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit
) {
    val color = glassContentColor()
    val latest by rememberUpdatedState(onChange)
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassText(label, Modifier.width(72.dp), style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium))
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(range) {
                    fun update(x: Float) {
                        val f = (x / size.width).coerceIn(0f, 1f)
                        latest(range.start + f * (range.endInclusive - range.start))
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        update(down.position.x)
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            update(change.position.x)
                            change.consume()
                        }
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val f = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
                val r = 10.dp.toPx()
                val y = size.height / 2
                val x = r + f * (size.width - 2 * r)
                val track = 4.dp.toPx()
                drawLine(color.copy(alpha = 0.2f), Offset(r, y), Offset(size.width - r, y), track, StrokeCap.Round)
                drawLine(Accent, Offset(r, y), Offset(x, y), track, StrokeCap.Round)
                drawCircle(Color.Black.copy(alpha = 0.18f), r + 1.dp.toPx(), Offset(x, y + 1.5.dp.toPx()))
                drawCircle(Color.White, r, Offset(x, y))
            }
        }
        GlassText(
            format(value),
            Modifier.width(48.dp),
            style = TextStyle(fontSize = 12.sp, textAlign = TextAlign.End)
        )
    }
}

@Composable
private fun TintRow(selected: Int, onSelect: (Int) -> Unit) {
    val color = glassContentColor()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassText("Tint", Modifier.width(62.dp), style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium))
        Tints.forEachIndexed { index, tint ->
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (tint.isSpecified) tint else color.copy(alpha = 0.12f), CircleShape)
                    .then(if (index == selected) Modifier.border(2.5.dp, color, CircleShape) else Modifier)
                    .clickable(interactionSource = null, indication = null) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                if (!tint.isSpecified) {
                    Canvas(Modifier.size(14.dp)) {
                        drawLine(color, Offset(0f, size.height), Offset(size.width, 0f), 2.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
        }
    }
}
