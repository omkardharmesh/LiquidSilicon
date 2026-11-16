package io.github.omkardharmesh.liquidsilicon.sample

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.omkardharmesh.liquidsilicon.GlassText
import io.github.omkardharmesh.liquidsilicon.glassContentColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class Show(
    val seed: Int,
    val title: String,
    val genre: String,
    val episodes: Int,
    val line: String,
    val colors: List<Color>
)

internal val Shows = listOf(
    Show(0, "The Glass Heir", "Romance", 80, "You were never supposed to find out.", listOf(Color(0xFF5E5CE6), Color(0xFFFF375F))),
    Show(1, "Midnight Vows", "Thriller", 64, "Sign it, or lose everything.", listOf(Color(0xFF1C1C3A), Color(0xFF0A84FF))),
    Show(2, "Crimson Promise", "Revenge", 72, "I came back for what's mine.", listOf(Color(0xFFFF453A), Color(0xFF3A0A1A))),
    Show(3, "Second Chance CEO", "Romance", 90, "Five years. Not one call.", listOf(Color(0xFFFF9F0A), Color(0xFFFF375F))),
    Show(4, "Royal Deception", "Fantasy", 60, "The crown was never yours.", listOf(Color(0xFFBF5AF2), Color(0xFF1C1C3A))),
    Show(5, "Love in Monsoon", "Romance", 48, "Stay until the rain stops.", listOf(Color(0xFF30D158), Color(0xFF0A84FF))),
    Show(6, "The Last Letter", "Family", 40, "Read it after I'm gone.", listOf(Color(0xFFFFD60A), Color(0xFFFF9F0A))),
    Show(7, "Silent Revenge", "Thriller", 70, "They buried the wrong sister.", listOf(Color(0xFF64D2FF), Color(0xFF1C1C3A))),
    Show(8, "Stolen Hearts", "Comedy", 55, "Technically, I stole it back.", listOf(Color(0xFFFF375F), Color(0xFFFFD60A))),
    Show(9, "Mirror Lake", "Mystery", 36, "Something looked back.", listOf(Color(0xFF0A84FF), Color(0xFF30D158)))
)

internal val Accent = Color(0xFF0A84FF)
internal val TabBarClearance = 96.dp

@Composable
internal fun PosterArt(show: Show, modifier: Modifier, phase: () -> Float = { 0f }) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = phase()
        drawRect(Brush.linearGradient(show.colors, start = Offset.Zero, end = Offset(w, h)))
        val light = Offset(w * (0.72f - 0.3f * p), h * (0.26f + 0.08f * p))
        val lightRadius = size.maxDimension * 0.55f
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), light, lightRadius),
            lightRadius,
            light
        )
        var x = 0f
        var i = 0
        while (x < w) {
            val bw = w * (0.06f + ((show.seed + i * 7) % 5) * 0.012f)
            val bh = h * (0.1f + ((show.seed * 3 + i * 11) % 7) * 0.025f)
            drawRect(Color.Black.copy(alpha = 0.16f), Offset(x, h * 0.74f - bh), Size(bw, bh + h))
            x += bw + w * 0.012f
            i++
        }
        val shade = Color.Black.copy(alpha = 0.34f)
        fun figure(cx: Float, scale: Float) {
            val r = w * 0.075f * scale
            val cy = h * 0.52f
            drawCircle(shade, r, Offset(cx, cy))
            drawRoundRect(shade, Offset(cx - 1.9f * r, cy + 1.25f * r), Size(3.8f * r, h), CornerRadius(1.9f * r))
        }
        figure(w * (0.36f + 0.02f * p), 1f)
        figure(w * (0.63f - 0.02f * p), 0.88f)
        drawRect(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)))
    }
}

@Composable
internal fun Poster(show: Show, modifier: Modifier, titleSize: TextUnit = 15.sp) {
    Box(modifier.clip(RoundedCornerShape(14.dp))) {
        PosterArt(show, Modifier.matchParentSize())
        BasicText(
            show.title,
            Modifier.align(Alignment.BottomStart).padding(10.dp),
            style = TextStyle(Color.White, titleSize, FontWeight.Black, lineHeight = titleSize * 1.05f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun rememberScenePhase(durationMillis: Int = 5000): State<Float> {
    val transition = rememberInfiniteTransition(label = "scene")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing), RepeatMode.Reverse),
        label = "phase"
    )
}

@Composable
internal fun LiveScene(modifier: Modifier, colors: List<Color>) {
    val transition = rememberInfiniteTransition(label = "live")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "phase"
    )
    BoxWithConstraints(modifier) {
        Canvas(
            Modifier
                .requiredSize(maxWidth / LiveSceneScale, maxHeight / LiveSceneScale)
                .graphicsLayer {
                    scaleX = LiveSceneScale
                    scaleY = LiveSceneScale
                    transformOrigin = TransformOrigin(0f, 0f)
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            val p = phase.value
            drawRect(colors[0])
            colors.drop(1).forEachIndexed { i, c ->
                val a = p + i * 2.1f
                val center = Offset(size.width * (0.5f + 0.38f * cos(a)), size.height * (0.42f + 0.3f * sin(a * 1.3f)))
                val r = size.maxDimension * 0.42f
                drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center, r), r, center)
            }
        }
    }
}

private const val LiveSceneScale = 4f

@Composable
internal fun rememberFps(): State<Int> {
    val fps = remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        var frames = 0
        var start = 0L
        while (true) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                frames++
                val elapsed = now - start
                if (elapsed >= 500_000_000L) {
                    fps.intValue = (frames * 1_000_000_000.0 / elapsed).roundToInt()
                    frames = 0
                    start = now
                }
            }
        }
    }
    return fps
}

@Composable
internal fun FpsBadge(modifier: Modifier = Modifier) {
    val fps by rememberFps()
    val color = glassContentColor()
    Row(
        modifier
            .background(color.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .padding(end = 6.dp)
                .background(if (fps >= 55) Color(0xFF30D158) else Color(0xFFFF9F0A), CircleShape)
                .padding(3.dp)
        )
        GlassText("$fps fps", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
    }
}

internal fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> "${value / 100_000 / 10f}M"
    value >= 1_000 -> "${value / 1_000}K"
    else -> value.toString()
}
