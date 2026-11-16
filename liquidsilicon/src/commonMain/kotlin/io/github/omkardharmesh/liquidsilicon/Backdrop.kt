package io.github.omkardharmesh.liquidsilicon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntSize
import io.github.omkardharmesh.liquidsilicon.internal.LuminanceMap
import io.github.omkardharmesh.liquidsilicon.internal.cacheBackdropAsTexture
import io.github.omkardharmesh.liquidsilicon.internal.readArgbPixels
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

@Stable
public interface Backdrop {

    public fun DrawScope.drawBackdrop(coordinates: LayoutCoordinates)
}

@Composable
public fun rememberLayerBackdrop(graphicsLayer: GraphicsLayer = rememberGraphicsLayer()): LayerBackdrop {
    return remember(graphicsLayer) { LayerBackdrop(graphicsLayer) }
}

@Composable
public fun rememberCanvasBackdrop(onDraw: DrawScope.() -> Unit): Backdrop {
    return remember(onDraw) { CanvasBackdrop(onDraw) }
}

@Composable
public fun rememberCombinedBackdrop(vararg backdrops: Backdrop): Backdrop {
    return remember(*backdrops) { CombinedBackdrop(backdrops.toList()) }
}

/**
 * Captures the content of the node it is attached to with [Modifier.layerBackdrop].
 * Glass surfaces using it must not be placed inside that node, otherwise the capture would recurse.
 */
@Stable
public class LayerBackdrop internal constructor(
    public val graphicsLayer: GraphicsLayer
) : Backdrop {

    internal var sourceCoordinates: LayoutCoordinates? = null
    internal var sourcePosition: Offset by mutableStateOf(Offset.Unspecified)

    internal var luminanceMap: LuminanceMap? by mutableStateOf(null)
    internal var luminanceSubscribers: Int by mutableIntStateOf(0)

    internal var graphicsContext: GraphicsContext? = null
    internal var sourceSize: IntSize = IntSize.Zero
    private val blurLayers = HashMap<Int, SharedBlur>()

    internal fun DrawScope.drawBlurredBackdrop(coordinates: LayoutCoordinates, radius: Float, scale: Float): Boolean {
        if (sourcePosition == Offset.Unspecified) return false
        val source = sourceCoordinates ?: return false
        val context = graphicsContext ?: return false
        if (!source.isAttached || !coordinates.isAttached) return false
        val size = sourceSize
        if (size.width <= 0 || size.height <= 0) return false
        val key = (radius * 4f).roundToInt() * 64 + (scale * 32f).roundToInt()
        val shared = blurLayers.getOrPut(key) { SharedBlur(context.createGraphicsLayer(), radius, scale) }
        shared.update(this, graphicsLayer, size)
        val offset = offsetOf(source, coordinates)
        translate(-offset.x, -offset.y) {
            scale(1f / scale, 1f / scale, Offset.Zero) {
                drawLayer(shared.layer)
            }
        }
        return true
    }

    internal fun refreshBlurLayers(scope: DrawScope) {
        for (shared in blurLayers.values) shared.invalidate()
    }

    internal fun releaseBlurLayers() {
        val context = graphicsContext
        for (shared in blurLayers.values) context?.releaseGraphicsLayer(shared.layer)
        blurLayers.clear()
    }

    override fun DrawScope.drawBackdrop(coordinates: LayoutCoordinates) {
        if (sourcePosition == Offset.Unspecified) return
        val source = sourceCoordinates ?: return
        if (!source.isAttached || !coordinates.isAttached) return
        val offset = offsetOf(source, coordinates)
        translate(-offset.x, -offset.y) {
            drawLayer(graphicsLayer)
        }
    }

    internal fun regionOf(coordinates: LayoutCoordinates): Rect? {
        val source = sourceCoordinates ?: return null
        if (!source.isAttached || !coordinates.isAttached) return null
        val offset = offsetOf(source, coordinates)
        val size = coordinates.size
        return Rect(offset.x, offset.y, offset.x + size.width, offset.y + size.height)
    }

    private fun offsetOf(source: LayoutCoordinates, coordinates: LayoutCoordinates): Offset =
        try {
            source.localPositionOf(coordinates, Offset.Zero)
        } catch (_: IllegalArgumentException) {
            coordinates.positionInWindow() - source.positionInWindow()
        } catch (_: IllegalStateException) {
            coordinates.positionInWindow() - source.positionInWindow()
        }
}

private class SharedBlur(val layer: GraphicsLayer, val radius: Float, val scale: Float) {

    private var recordedSize = IntSize.Zero
    private var dirty = true

    fun invalidate() {
        if (!cacheBackdropAsTexture()) dirty = true
    }

    fun update(scope: DrawScope, source: GraphicsLayer, sourceSize: IntSize) {
        if (!dirty && recordedSize == sourceSize) return
        val w = ceil(sourceSize.width * scale).toInt().coerceAtLeast(1)
        val h = ceil(sourceSize.height * scale).toInt().coerceAtLeast(1)
        with(scope) {
            layer.record(IntSize(w, h)) {
                scale(scale, scale, Offset.Zero) {
                    drawLayer(source)
                }
            }
        }
        layer.clip = io.github.omkardharmesh.liquidsilicon.internal.clipGlassLayers()
        layer.renderEffect = BlurEffect(radius * scale, radius * scale, TileMode.Clamp)
        recordedSize = sourceSize
        dirty = false
    }
}

private class CanvasBackdrop(val onDraw: DrawScope.() -> Unit) : Backdrop {

    override fun DrawScope.drawBackdrop(coordinates: LayoutCoordinates) {
        onDraw()
    }
}

private class CombinedBackdrop(val backdrops: List<Backdrop>) : Backdrop {

    override fun DrawScope.drawBackdrop(coordinates: LayoutCoordinates) {
        for (backdrop in backdrops) {
            with(backdrop) { drawBackdrop(coordinates) }
        }
    }
}

internal fun Backdrop.luminanceSource(): LayerBackdrop? = when (this) {
    is LayerBackdrop -> this
    is CombinedBackdrop -> backdrops.firstNotNullOfOrNull { it.luminanceSource() }
    else -> null
}

public fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier =
    this then LayerBackdropElement(backdrop)

private class LayerBackdropElement(val backdrop: LayerBackdrop) : ModifierNodeElement<LayerBackdropNode>() {

    override fun create(): LayerBackdropNode = LayerBackdropNode(backdrop)

    override fun update(node: LayerBackdropNode) {
        node.update(backdrop)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layerBackdrop"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is LayerBackdropElement && other.backdrop == backdrop)

    override fun hashCode(): Int = backdrop.hashCode()
}

private const val ThumbnailWidth = 16
private const val SampleIntervalMs = 500L

private class LayerBackdropNode(var backdrop: LayerBackdrop) :
    DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

    private var thumbnailLayer: GraphicsLayer? = null
    private var sourceSize = IntSize.Zero

    fun update(backdrop: LayerBackdrop) {
        if (this.backdrop != backdrop) {
            detachBackdrop()
            this.backdrop = backdrop
            if (isAttached) {
                backdrop.graphicsContext = requireGraphicsContext()
                startSampling()
            }
        }
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val layer = backdrop.graphicsLayer
        layer.record(size.toIntSize()) {
            this@draw.drawContent()
        }
        drawLayer(layer)
        backdrop.refreshBlurLayers(this)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (!coordinates.isAttached) return
        backdrop.sourceCoordinates = coordinates
        sourceSize = coordinates.size
        backdrop.sourceSize = coordinates.size
        val position = coordinates.positionInWindow()
        if (backdrop.sourcePosition != position) {
            backdrop.sourcePosition = position
        }
    }

    override fun onAttach() {
        backdrop.graphicsContext = requireGraphicsContext()
        thumbnailLayer = requireGraphicsContext().createGraphicsLayer()
        startSampling()
    }

    override fun onDetach() {
        detachBackdrop()
        thumbnailLayer?.let(requireGraphicsContext()::releaseGraphicsLayer)
        thumbnailLayer = null
    }

    private fun detachBackdrop() {
        backdrop.releaseBlurLayers()
        backdrop.graphicsContext = null
        backdrop.sourceCoordinates = null
        backdrop.sourcePosition = Offset.Unspecified
        backdrop.luminanceMap = null
    }

    private fun startSampling() {
        val target = backdrop
        coroutineScope.launch {
            snapshotFlow { target.luminanceSubscribers > 0 }.collectLatest { active ->
                while (active && isActive) {
                    sample(target)
                    delay(SampleIntervalMs)
                }
            }
        }
    }

    private suspend fun sample(target: LayerBackdrop) {
        val layer = thumbnailLayer ?: return
        val size = sourceSize
        if (size.width <= 0 || size.height <= 0) return
        val tw = ThumbnailWidth
        val th = (tw * size.height.toFloat() / size.width).roundToInt().coerceIn(1, 64)
        layer.record(requireDensity(), requireLayoutDirection(), IntSize(tw, th)) {
            scale(tw.toFloat() / size.width, th.toFloat() / size.height, Offset.Zero) {
                drawLayer(target.graphicsLayer)
            }
        }
        val pixels = try {
            layer.toImageBitmap().readArgbPixels()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        } ?: return
        if (pixels.size != tw * th) return
        target.luminanceMap = LuminanceMap(tw, th, pixels, size.width.toFloat(), size.height.toFloat())
    }
}
