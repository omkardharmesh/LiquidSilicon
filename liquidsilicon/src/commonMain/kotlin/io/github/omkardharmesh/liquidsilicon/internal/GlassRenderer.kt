package io.github.omkardharmesh.liquidsilicon.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.omkardharmesh.liquidsilicon.Backdrop
import io.github.omkardharmesh.liquidsilicon.GlassStyle
import io.github.omkardharmesh.liquidsilicon.addGlassShape
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

internal class GlassRenderer {

    private var blurLayer: GraphicsLayer? = null
    private var glassLayer: GraphicsLayer? = null

    private var glassEffect: RenderEffect? = null

    private var blurEffect: RenderEffect? = null
    private var blurEffectRadius = -1f

    private var saturationFilter: ColorFilter? = null
    private var saturationFilterValue = Float.NaN

    private val rects = FloatArray(MaxGlassShapes * 4)
    private val glows = FloatArray(MaxGlassShapes * 4)
    private val scalars = FloatArray(ScalarCount)
    private val lastRects = FloatArray(MaxGlassShapes * 4)
    private val lastGlows = FloatArray(MaxGlassShapes * 4)
    private val lastRadii = FloatArray(MaxGlassShapes * 4)
    private val lastExtras = FloatArray(MaxGlassShapes * 4)
    private val lastTints = FloatArray(MaxGlassShapes * 4)
    private val lastScalars = FloatArray(ScalarCount)
    private var lastCount = -1

    private val path = Path()

    fun attach(context: GraphicsContext) {
        if (blurLayer == null) blurLayer = context.createGraphicsLayer()
        if (glassLayer == null) glassLayer = context.createGraphicsLayer()
    }

    fun detach(context: GraphicsContext) {
        blurLayer?.let(context::releaseGraphicsLayer)
        glassLayer?.let(context::releaseGraphicsLayer)
        blurLayer = null
        glassLayer = null
        glassEffect = null
        blurEffect = null
        blurEffectRadius = -1f
        lastCount = -1
    }

    fun DrawScope.drawGlass(
        backdrop: Backdrop?,
        coordinates: LayoutCoordinates,
        shapes: GlassShapes,
        style: GlassStyle,
        light: Offset,
        luminance: Float,
        smoothing: Float
    ) {
        if (shapes.count == 0) return
        val brightness = if (style.adaptive && luminance >= 0f) smoothstep(0.35f, 0.65f, luminance) else 1f
        when {
            backdrop == null -> drawFlatFallback(shapes, style, light, brightness)

            isRuntimeShaderSupported() && !GlassPrograms.failed ->
                drawShaderGlass(backdrop, coordinates, shapes, style, light, brightness, smoothing)

            isRenderEffectSupported() ->
                drawBlurFallback(backdrop, coordinates, shapes, style, light, brightness)

            else -> drawFlatFallback(shapes, style, light, brightness)
        }
    }

    private fun DrawScope.drawShaderGlass(
        backdrop: Backdrop,
        coordinates: LayoutCoordinates,
        shapes: GlassShapes,
        style: GlassStyle,
        light: Offset,
        brightness: Float,
        smoothing: Float
    ) {
        val glass = glassLayer ?: return
        val blurPx = style.blurRadius.toPx()
        val margin = ceil(blurPx + 1f + smoothing * 0.25f * (shapes.count - 1).coerceIn(0, 3))

        val left = floor(shapes.left - margin).toInt()
        val top = floor(shapes.top - margin).toInt()
        val width = ceil(shapes.right + margin).toInt() - left
        val height = ceil(shapes.bottom + margin).toInt() - top
        if (width <= 0 || height <= 0) return

        val effect = obtainGlassEffect(shapes, style, light, brightness, smoothing, left.toFloat(), top.toFloat())
        if (effect == null) {
            drawBlurFallback(backdrop, coordinates, shapes, style, light, brightness)
            return
        }
        drawShadow(shapes, style, smoothing)
        recordBackdrop(glass, backdrop, coordinates, left, top, width, height, blurPx, style, null)
        glass.clip = clipGlassLayers()
        glass.renderEffect = effect
        glass.topLeft = IntOffset(left, top)
        drawLayer(glass)
    }

    private fun DrawScope.drawShadow(shapes: GlassShapes, style: GlassStyle, smoothing: Float) {
        val shadow = style.shadow ?: return
        if (shadow.alpha <= 0f) return
        val program = GlassPrograms.shadow() ?: return
        val radius = shadow.radius.toPx().coerceAtLeast(1f)
        val dy = shadow.offsetY.toPx()
        program.setInt("count", shapes.count)
        program.setFloatArray("rects", shapes.rects)
        program.setFloatArray("radii", shapes.radii)
        program.setFloatArray("extras", shapes.extras)
        program.setFloat("smoothing", smoothing)
        program.setFloat4("shadow", 0f, dy, radius, shadow.alpha)
        val extent = radius + abs(dy)
        val brush = ShaderBrush(program.asShader())
        val topLeft = Offset(shapes.left - extent, shapes.top - extent)
        val size = Size(shapes.right - shapes.left + 2f * extent, shapes.bottom - shapes.top + 2f * extent)
        var hole = -1
        var holeArea = 0f
        for (i in 0 until shapes.count) {
            val j = i * 4
            val inset = shapes.corners[i].cornerExtent() + 1f
            val w = 2f * (shapes.rects[j + 2] - inset)
            val h = 2f * (shapes.rects[j + 3] - inset)
            if (w > 0f && h > 0f && w * h > holeArea) {
                hole = i
                holeArea = w * h
            }
        }
        if (hole < 0 || holeArea < size.width * size.height * 0.1f) {
            drawRect(brush, topLeft, size)
            return
        }
        val inset = shapes.corners[hole].cornerExtent() + 1f
        clipRect(
            shapes.shapeLeft(hole) + inset,
            shapes.shapeTop(hole) + inset,
            shapes.shapeRight(hole) - inset,
            shapes.shapeBottom(hole) - inset,
            ClipOp.Difference
        ) {
            drawRect(brush, topLeft, size)
        }
    }

    private fun DrawScope.recordBackdrop(
        target: GraphicsLayer,
        backdrop: Backdrop,
        coordinates: LayoutCoordinates,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        blurPx: Float,
        style: GlassStyle,
        colorFilter: ColorFilter?
    ) {
        val blur = blurLayer
        val wantsBlur = blurPx >= 1.5f * density && isRenderEffectSupported()
        val shared = if (wantsBlur) backdrop as? io.github.omkardharmesh.liquidsilicon.LayerBackdrop else null
        if (shared != null) {
            val scale = resolutionScale(style, blurPx / density)
            var drawn = false
            target.record(IntSize(width, height)) {
                translate(-left.toFloat(), -top.toFloat()) {
                    drawn = with(shared) { drawBlurredBackdrop(coordinates, blurPx, scale) }
                }
            }
            target.colorFilter = colorFilter
            if (drawn) return
        }
        if (wantsBlur && blur != null) {
            val scale = resolutionScale(style, blurPx / density)
            val sw = ceil(width * scale).toInt().coerceAtLeast(1)
            val sh = ceil(height * scale).toInt().coerceAtLeast(1)
            blur.record(IntSize(sw, sh)) {
                scale(scale, scale, Offset.Zero) {
                    translate(-left.toFloat(), -top.toFloat()) {
                        with(backdrop) { drawBackdrop(coordinates) }
                    }
                }
            }
            blur.clip = clipGlassLayers()
            blur.renderEffect = obtainBlurEffect(blurPx * scale)
            blur.pivotOffset = Offset.Zero
            blur.scaleX = 1f / scale
            blur.scaleY = 1f / scale
            blur.topLeft = IntOffset.Zero
            blur.colorFilter = colorFilter
            target.record(IntSize(width, height)) { drawLayer(blur) }
        } else {
            target.record(IntSize(width, height)) {
                translate(-left.toFloat(), -top.toFloat()) {
                    with(backdrop) { drawBackdrop(coordinates) }
                }
            }
            target.colorFilter = colorFilter
        }
    }

    private fun obtainBlurEffect(radius: Float): RenderEffect {
        val cached = blurEffect
        if (cached != null && blurEffectRadius == radius) return cached
        return BlurEffect(radius, radius, TileMode.Clamp).also {
            blurEffect = it
            blurEffectRadius = radius
        }
    }

    private fun DrawScope.obtainGlassEffect(
        shapes: GlassShapes,
        style: GlassStyle,
        light: Offset,
        brightness: Float,
        smoothing: Float,
        offsetX: Float,
        offsetY: Float
    ): RenderEffect? {
        val single = shapes.count == 1
        val program = (if (single) GlassPrograms.single() else GlassPrograms.glass()) ?: return null

        for (i in 0 until MaxGlassShapes) {
            val j = i * 4
            rects[j] = shapes.rects[j] - offsetX
            rects[j + 1] = shapes.rects[j + 1] - offsetY
            rects[j + 2] = shapes.rects[j + 2]
            rects[j + 3] = shapes.rects[j + 3]
            glows[j] = shapes.glows[j] - offsetX
            glows[j + 1] = shapes.glows[j + 1] - offsetY
            glows[j + 2] = shapes.glows[j + 2]
            glows[j + 3] = shapes.glows[j + 3]
        }

        val surface = lerp(style.darkSurface, style.surface, brightness)
        var maxHeight = 0f
        for (i in 0 until shapes.count) maxHeight = max(maxHeight, shapes.extras[i * 4 + 1])
        val specularWidth = style.specularWidth.toPx()
        val innerRadius = style.innerShadowRadius.toPx().coerceAtLeast(1f)
        val band = max(maxHeight, max(specularWidth, if (style.innerShadow > 0f) innerRadius else 0f)) + 1f

        scalars[0] = smoothing
        scalars[1] = style.refractionAmount.toPx()
        scalars[2] = style.dispersion
        scalars[3] = style.saturation
        scalars[4] = style.brightness
        scalars[5] = style.contrast
        scalars[6] = surface.red * surface.alpha
        scalars[7] = surface.green * surface.alpha
        scalars[8] = surface.blue * surface.alpha
        scalars[9] = surface.alpha
        scalars[10] = light.x
        scalars[11] = light.y
        scalars[12] = style.specular
        scalars[13] = specularWidth
        scalars[14] = band
        scalars[15] = if (single) 1f else 0f
        scalars[16] = 0f
        scalars[17] = 0f
        scalars[18] = style.innerShadow
        scalars[19] = innerRadius
        scalars[20] = style.magnification.coerceAtLeast(0.1f)

        val cached = glassEffect
        if (cached != null &&
            lastCount == shapes.count &&
            rects.contentEquals(lastRects) &&
            glows.contentEquals(lastGlows) &&
            shapes.radii.contentEquals(lastRadii) &&
            shapes.extras.contentEquals(lastExtras) &&
            shapes.tints.contentEquals(lastTints) &&
            scalars.contentEquals(lastScalars)
        ) {
            return cached
        }

        program.setFloat("refractionAmount", scalars[1])
        program.setFloat("dispersion", scalars[2])
        program.setFloatArray("colorParams", floatArrayOf(scalars[3], scalars[4], scalars[5]))
        program.setFloat4("surface", scalars[6], scalars[7], scalars[8], scalars[9])
        program.setFloat2("lightDir", scalars[10], scalars[11])
        program.setFloat("specular", scalars[12])
        program.setFloat("specularWidth", scalars[13])
        program.setFloat2("innerShadow", scalars[18], scalars[19])
        program.setFloat("magnification", scalars[20])
        if (single) {
            program.setFloat4("rect", rects[0], rects[1], rects[2], rects[3])
            program.setFloat4("radii", shapes.radii[0], shapes.radii[1], shapes.radii[2], shapes.radii[3])
            program.setFloatArray("shapeParams", floatArrayOf(shapes.extras[0], shapes.extras[1], band))
            program.setFloat4("tint", shapes.tints[0], shapes.tints[1], shapes.tints[2], shapes.tints[3])
            program.setFloat4("glow", glows[0], glows[1], glows[2], glows[3])
        } else {
            program.setInt("count", shapes.count)
            program.setFloatArray("rects", rects)
            program.setFloatArray("radii", shapes.radii)
            program.setFloatArray("extras", shapes.extras)
            program.setFloatArray("tints", shapes.tints)
            program.setFloatArray("glows", glows)
            program.setFloat("smoothing", scalars[0])
            program.setFloat("band", band)
        }

        val effect = program.createEffect("content")
        glassEffect = effect
        lastCount = shapes.count
        rects.copyInto(lastRects)
        glows.copyInto(lastGlows)
        shapes.radii.copyInto(lastRadii)
        shapes.extras.copyInto(lastExtras)
        shapes.tints.copyInto(lastTints)
        scalars.copyInto(lastScalars)
        return effect
    }

    private fun DrawScope.drawBlurFallback(
        backdrop: Backdrop,
        coordinates: LayoutCoordinates,
        shapes: GlassShapes,
        style: GlassStyle,
        light: Offset,
        brightness: Float
    ) {
        val glass = glassLayer ?: return
        val blurPx = style.blurRadius.toPx().coerceAtLeast(style.refractionHeight.toPx() * 0.25f)
        val margin = ceil(blurPx * 1.5f + 2f)
        val left = floor(shapes.left - margin).toInt()
        val top = floor(shapes.top - margin).toInt()
        val width = ceil(shapes.right + margin).toInt() - left
        val height = ceil(shapes.bottom + margin).toInt() - top
        if (width <= 0 || height <= 0) return

        recordBackdrop(glass, backdrop, coordinates, left, top, width, height, blurPx, style, saturation(style.saturation))
        glass.renderEffect = null
        glass.topLeft = IntOffset(left, top)

        val surface = lerp(style.darkSurface, style.surface, brightness)
        for (i in 0 until shapes.count) {
            buildPath(shapes, i)
            clipPath(path) {
                drawLayer(glass)
                drawRect(surface)
                shapes.tintAt(i)?.let { drawRect(it.copy(alpha = it.alpha * 0.75f)) }
                drawGlow(shapes, i)
            }
            drawRim(shapes, i, style, light)
        }
    }

    private fun DrawScope.drawFlatFallback(
        shapes: GlassShapes,
        style: GlassStyle,
        light: Offset,
        brightness: Float
    ) {
        val fill = lerp(style.fallbackDarkSurface, style.fallbackSurface, brightness)
        for (i in 0 until shapes.count) {
            buildPath(shapes, i)
            drawPath(path, fill)
            shapes.tintAt(i)?.let { drawPath(path, it.copy(alpha = it.alpha * 0.9f)) }
            clipPath(path) { drawGlow(shapes, i) }
            drawRim(shapes, i, style, light)
        }
    }

    private fun buildPath(shapes: GlassShapes, index: Int) {
        path.reset()
        path.addGlassShape(
            shapes.shapeLeft(index),
            shapes.shapeTop(index),
            shapes.shapeRight(index),
            shapes.shapeBottom(index),
            shapes.corners[index]
        )
    }

    private fun DrawScope.drawGlow(shapes: GlassShapes, index: Int) {
        val j = index * 4
        val intensity = shapes.glows[j + 3]
        val radius = shapes.glows[j + 2]
        if (intensity <= 0f || radius <= 0f) return
        val center = Offset(shapes.glows[j], shapes.glows[j + 1])
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = intensity.coerceIn(0f, 1f)), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }

    private fun DrawScope.drawRim(shapes: GlassShapes, index: Int, style: GlassStyle, light: Offset) {
        if (style.specular <= 0f) return
        val l = shapes.shapeLeft(index)
        val t = shapes.shapeTop(index)
        val r = shapes.shapeRight(index)
        val b = shapes.shapeBottom(index)
        val center = Offset((l + r) / 2f, (t + b) / 2f)
        val extent = Size(r - l, b - t).maxDimension / 2f
        val strong = Color.White.copy(alpha = (0.7f * style.specular).coerceIn(0f, 1f))
        val weak = Color.White.copy(alpha = (0.08f * style.specular).coerceIn(0f, 1f))
        val mid = Color.White.copy(alpha = (0.35f * style.specular).coerceIn(0f, 1f))
        drawPath(
            path,
            Brush.linearGradient(
                0f to strong,
                0.5f to weak,
                1f to mid,
                start = center + light * extent,
                end = center - light * extent
            ),
            style = Stroke(width = style.specularWidth.toPx().coerceAtLeast(1f))
        )
    }

    private fun saturation(value: Float): ColorFilter? {
        if (value == 1f) return null
        if (value == saturationFilterValue) return saturationFilter
        return ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(value) }).also {
            saturationFilter = it
            saturationFilterValue = value
        }
    }

    private companion object {
        const val ScalarCount = 21
    }
}

internal fun resolutionScale(style: GlassStyle, blurDp: Float): Float {
    if (style.resolutionScale > 0f) return style.resolutionScale.coerceIn(0.1f, 1f)
    return when {
        blurDp < 2f -> 0.5f
        blurDp < 3f -> 0.33f
        else -> 0.25f
    }
}

internal fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal object GlassPrograms {

    var failed: Boolean = false
        private set

    private var glassProgram: PlatformShader? = null
    private var singleProgram: PlatformShader? = null
    private var shadowProgram: PlatformShader? = null

    fun single(): PlatformShader? = singleProgram ?: compile(SingleGlassShaderSource)?.also { singleProgram = it }

    fun glass(): PlatformShader? = glassProgram ?: compile(GlassShaderSource)?.also { glassProgram = it }

    fun shadow(): PlatformShader? = shadowProgram ?: compile(GlassShadowShaderSource)?.also { shadowProgram = it }

    private fun compile(source: String): PlatformShader? {
        if (failed) return null
        return try {
            createPlatformShader(source)
        } catch (e: Throwable) {
            failed = true
            println("LiquidSilicon: glass shader unavailable, using fallback: ${e.message}")
            null
        }
    }
}
