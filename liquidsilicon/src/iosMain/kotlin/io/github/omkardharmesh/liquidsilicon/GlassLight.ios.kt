package io.github.omkardharmesh.liquidsilicon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import kotlin.math.sqrt

@OptIn(ExperimentalForeignApi::class)
@Composable
public actual fun rememberDeviceTiltLight(baseAngleDegrees: Float): GlassLight {
    val light = remember(baseAngleDegrees) { TiltGlassLight(baseAngleDegrees) }
    DisposableEffect(light) {
        val manager = CMMotionManager()
        if (manager.deviceMotionAvailable) {
            manager.deviceMotionUpdateInterval = 1.0 / 50.0
            manager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue) { motion, _ ->
                motion?.gravity?.useContents {
                    val gx = x.toFloat()
                    val gy = y.toFloat()
                    val gz = z.toFloat()
                    light.onUpVector(-gx, gy, sqrt(gx * gx + gy * gy + gz * gz))
                }
            }
        }
        onDispose { manager.stopDeviceMotionUpdates() }
    }
    return light
}
