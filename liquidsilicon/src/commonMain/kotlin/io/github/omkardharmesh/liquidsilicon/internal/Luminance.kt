package io.github.omkardharmesh.liquidsilicon.internal

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import io.github.omkardharmesh.liquidsilicon.GlassState
import io.github.omkardharmesh.liquidsilicon.LayerBackdrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

internal class LuminanceMap(
    private val width: Int,
    private val height: Int,
    argb: IntArray,
    private val sourceWidth: Float,
    private val sourceHeight: Float
) {
    private val luma = FloatArray(argb.size) { i ->
        val c = argb[i]
        val r = (c shr 16 and 0xFF) / 255f
        val g = (c shr 8 and 0xFF) / 255f
        val b = (c and 0xFF) / 255f
        0.2126f * r + 0.7152f * g + 0.0722f * b
    }
    private val alpha = FloatArray(argb.size) { i -> (argb[i] ushr 24 and 0xFF) / 255f }

    fun average(region: Rect): Float {
        val x0 = floor(region.left / sourceWidth * width).toInt().coerceIn(0, width - 1)
        val x1 = ceil(region.right / sourceWidth * width).toInt().coerceIn(x0 + 1, width)
        val y0 = floor(region.top / sourceHeight * height).toInt().coerceIn(0, height - 1)
        val y1 = ceil(region.bottom / sourceHeight * height).toInt().coerceIn(y0 + 1, height)
        var sum = 0f
        var weight = 0f
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val i = y * width + x
                sum += luma[i] * alpha[i]
                weight += alpha[i]
            }
        }
        return if (weight > 0f) (sum / weight).coerceIn(0f, 1f) else -1f
    }
}

internal class LuminanceSubscription {

    private var job: Job? = null

    fun start(
        scope: CoroutineScope,
        source: LayerBackdrop,
        coordinates: () -> LayoutCoordinates?,
        state: () -> GlassState
    ) {
        stop()
        job = scope.launch {
            source.luminanceSubscribers++
            try {
                androidx.compose.runtime.snapshotFlow { source.luminanceMap }.collect { map ->
                    if (map == null) return@collect
                    val coords = coordinates() ?: return@collect
                    val region = source.regionOf(coords) ?: return@collect
                    val value = map.average(region)
                    if (value >= 0f) state().onLuminance(value, scope)
                }
            } finally {
                source.luminanceSubscribers--
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
