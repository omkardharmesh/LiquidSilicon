package com.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Stable
public interface GlassLight {

    /** Unit vector in screen space (y down) pointing from the glass towards the light source. */
    public val direction: Offset
}

public fun GlassLight(angleDegrees: Float): GlassLight = StaticGlassLight(angleToDirection(angleDegrees))

public val LocalGlassLight: androidx.compose.runtime.ProvidableCompositionLocal<GlassLight> =
    staticCompositionLocalOf { DefaultGlassLight }

public const val DefaultLightAngle: Float = -135f

internal val DefaultGlassLight: GlassLight = StaticGlassLight(angleToDirection(DefaultLightAngle))

@Composable
public expect fun rememberDeviceTiltLight(baseAngleDegrees: Float = DefaultLightAngle): GlassLight

private class StaticGlassLight(override val direction: Offset) : GlassLight

internal fun angleToDirection(angleDegrees: Float): Offset {
    val rad = angleDegrees * (PI / 180.0).toFloat()
    return Offset(cos(rad), sin(rad))
}

@Stable
internal class TiltGlassLight(baseAngleDegrees: Float) : GlassLight {

    private val base = angleToDirection(baseAngleDegrees)
    private var smoothed = base

    override var direction: Offset by mutableStateOf(base)
        private set

    fun onUpVector(upX: Float, upY: Float, magnitude: Float) {
        val planar = sqrt(upX * upX + upY * upY)
        val weight = if (magnitude > 0f) (planar / magnitude).coerceIn(0f, 1f) * 0.85f else 0f
        val target =
            if (planar > 1e-3f) {
                Offset(
                    base.x * (1f - weight) + upX / planar * weight,
                    base.y * (1f - weight) + upY / planar * weight
                )
            } else {
                base
            }
        smoothed = Offset(
            smoothed.x + (target.x - smoothed.x) * 0.12f,
            smoothed.y + (target.y - smoothed.y) * 0.12f
        )
        val len = smoothed.getDistance()
        if (len < 1e-3f) return
        val next = Offset(smoothed.x / len, smoothed.y / len)
        var delta = abs(atan2(next.y, next.x) - atan2(direction.y, direction.x))
        if (delta > PI.toFloat()) delta = 2f * PI.toFloat() - delta
        if (delta > 0.035f) {
            direction = next
        }
    }
}
