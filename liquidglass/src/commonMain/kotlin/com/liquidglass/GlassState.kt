package com.liquidglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.liquidglass.internal.smoothstep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.tanh

@Stable
public class GlassState(initialLuminance: Float = 1f) {

    internal val pressAnimation = Animatable(0f, 0.001f)
    internal val dragAnimation = Animatable(Offset.Zero, Offset.VectorConverter, Offset(0.5f, 0.5f))
    internal val luminanceAnimation = Animatable(initialLuminance.coerceIn(0f, 1f), 0.001f)

    internal var touchPosition: Offset by mutableStateOf(Offset.Zero)

    public var isPressed: Boolean by mutableStateOf(false)
        internal set

    public val pressProgress: Float get() = pressAnimation.value

    public val luminance: Float get() = luminanceAnimation.value

    public val contentColor: Color
        get() = lerp(Color.White, Color.Black, smoothstep(0.42f, 0.58f, luminanceAnimation.value))

    internal fun onPress(position: Offset, scope: CoroutineScope) {
        isPressed = true
        touchPosition = position
        scope.launch { dragAnimation.snapTo(Offset.Zero) }
        scope.launch { pressAnimation.animateTo(1f, PressSpring) }
    }

    internal fun onDrag(position: Offset, start: Offset, scope: CoroutineScope) {
        touchPosition = position
        scope.launch { dragAnimation.snapTo(position - start) }
    }

    internal fun onRelease(scope: CoroutineScope) {
        isPressed = false
        scope.launch { pressAnimation.animateTo(0f, PressSpring) }
        scope.launch { dragAnimation.animateTo(Offset.Zero, DragSpring) }
    }

    internal fun onLuminance(value: Float, scope: CoroutineScope) {
        if (abs(value - luminanceAnimation.targetValue) < 0.06f) return
        scope.launch { luminanceAnimation.animateTo(value, tween(450)) }
    }

    internal var transformOverride: (GlassTransform.(Size, Density) -> Unit)? = null

    internal fun computeTransform(size: Size, density: Density, interactive: Boolean, out: GlassTransform) {
        out.reset(size)
        val override = transformOverride
        if (override != null) {
            if (!size.isEmpty()) override(out, size, density)
            return
        }
        if (!interactive || size.isEmpty()) return
        val progress = pressAnimation.value
        val drag = dragAnimation.value
        if (progress == 0f && drag == Offset.Zero) return

        val minDim = size.minDimension
        val maxDim = size.maxDimension
        val grow = min(with(density) { 5.dp.toPx() }, minDim * 0.12f) * progress
        val stretchMax = with(density) { 4.dp.toPx() }
        val stretchX = stretchMax * abs(tanh(drag.x / maxDim))
        val stretchY = stretchMax * abs(tanh(drag.y / maxDim))
        val maxOffset = minDim * 0.5f
        val dx = maxOffset * tanh(0.12f * drag.x / maxOffset)
        val dy = maxOffset * tanh(0.12f * drag.y / maxOffset)

        out.left = -grow - stretchX + dx
        out.right = size.width + grow + stretchX + dx
        out.top = -grow - stretchY + dy
        out.bottom = size.height + grow + stretchY + dy
        out.translation = Offset(dx, dy)
        out.contentScale = min((size.width + 2f * grow) / size.width, (size.height + 2f * grow) / size.height)
        out.glowCenter = touchPosition + Offset(dx, dy)
        out.glowRadius = minDim * 1.2f
        out.glowIntensity = 0.14f * progress.coerceIn(0f, 1.2f)
    }

    private companion object {
        val PressSpring = spring(dampingRatio = 0.5f, stiffness = 320f, visibilityThreshold = 0.001f)
        val DragSpring = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = Offset(0.5f, 0.5f))
    }
}

@Composable
public fun rememberGlassState(initialLuminance: Float = 1f): GlassState =
    remember { GlassState(initialLuminance) }

internal class GlassTransform {
    var left = 0f
    var top = 0f
    var right = 0f
    var bottom = 0f
    var translation = Offset.Zero
    var contentScale = 1f
    var glowCenter = Offset.Zero
    var glowRadius = 0f
    var glowIntensity = 0f

    fun reset(size: Size) {
        left = 0f
        top = 0f
        right = size.width
        bottom = size.height
        translation = Offset.Zero
        contentScale = 1f
        glowCenter = Offset.Zero
        glowRadius = 0f
        glowIntensity = 0f
    }

    val isIdentity: Boolean get() = translation == Offset.Zero && contentScale == 1f
}
