package io.github.omkardharmesh.liquidsilicon

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

@Immutable
public sealed class GlassShape : Shape {

    internal abstract fun resolve(
        size: Size,
        density: Density,
        layoutDirection: LayoutDirection,
        out: ResolvedCorners
    )

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val corners = ResolvedCorners()
        resolve(size, density, layoutDirection, corners)
        return if (corners.exponent == 2f) {
            Outline.Rounded(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    topLeftCornerRadius = CornerRadius(corners.topLeft),
                    topRightCornerRadius = CornerRadius(corners.topRight),
                    bottomRightCornerRadius = CornerRadius(corners.bottomRight),
                    bottomLeftCornerRadius = CornerRadius(corners.bottomLeft)
                )
            )
        } else {
            Outline.Generic(Path().apply { addGlassShape(0f, 0f, size.width, size.height, corners) })
        }
    }

    @Immutable
    public data object Capsule : GlassShape() {

        override fun resolve(size: Size, density: Density, layoutDirection: LayoutDirection, out: ResolvedCorners) {
            val r = size.minDimension / 2f
            out.set(r, r, r, r, 2f)
        }
    }

    @Immutable
    public data class RoundedRect(
        val topStart: Dp,
        val topEnd: Dp,
        val bottomEnd: Dp,
        val bottomStart: Dp,
        val continuous: Boolean = true
    ) : GlassShape() {

        public constructor(radius: Dp, continuous: Boolean = true) :
            this(radius, radius, radius, radius, continuous)

        override fun resolve(size: Size, density: Density, layoutDirection: LayoutDirection, out: ResolvedCorners) {
            val maxR = size.minDimension / 2f
            val ltr = layoutDirection == LayoutDirection.Ltr
            val ts = with(density) { topStart.toPx() }
            val te = with(density) { topEnd.toPx() }
            val be = with(density) { bottomEnd.toPx() }
            val bs = with(density) { bottomStart.toPx() }
            val tl = if (ltr) ts else te
            val tr = if (ltr) te else ts
            val br = if (ltr) be else bs
            val bl = if (ltr) bs else be
            if (!continuous) {
                out.set(min(tl, maxR), min(tr, maxR), min(br, maxR), min(bl, maxR), 2f)
                return
            }
            val largest = maxOf(tl, tr, br, bl)
            out.set(
                continuousExtent(tl, maxR),
                continuousExtent(tr, maxR),
                continuousExtent(br, maxR),
                continuousExtent(bl, maxR),
                continuousExponent(largest, maxR)
            )
        }
    }

    public companion object {

        public val Circle: GlassShape get() = Capsule

        public val Card: GlassShape = RoundedRect(24.dp)
    }
}

internal class ResolvedCorners {
    var topLeft = 0f
    var topRight = 0f
    var bottomRight = 0f
    var bottomLeft = 0f
    var exponent = 2f

    fun set(tl: Float, tr: Float, br: Float, bl: Float, e: Float) {
        topLeft = tl.coerceAtLeast(0f)
        topRight = tr.coerceAtLeast(0f)
        bottomRight = br.coerceAtLeast(0f)
        bottomLeft = bl.coerceAtLeast(0f)
        exponent = e
    }

    fun scale(factor: Float, maxR: Float) {
        topLeft = min(topLeft * factor, maxR)
        topRight = min(topRight * factor, maxR)
        bottomRight = min(bottomRight * factor, maxR)
        bottomLeft = min(bottomLeft * factor, maxR)
    }

    fun cornerExtent(): Float {
        val r = maxOf(topLeft, topRight, bottomRight, bottomLeft)
        return if (exponent == 2f) r else r * ContinuousExtent
    }
}

private const val ContinuousExtent = 1.528f
private const val ContinuousExponent = 3.3f

private fun continuousExtent(r: Float, maxR: Float): Float = min(r * ContinuousExtent, maxR)

private fun continuousExponent(r: Float, maxR: Float): Float {
    if (r <= 0f) return 2f
    val extent = r * ContinuousExtent
    if (extent <= maxR) return ContinuousExponent
    val t = ((extent - maxR) / (r * (ContinuousExtent - 1f))).coerceIn(0f, 1f)
    return ContinuousExponent + (2f - ContinuousExponent) * t
}

internal fun Path.addGlassShape(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    corners: ResolvedCorners
) {
    val e = corners.exponent
    if (e == 2f) {
        addRoundRect(
            RoundRect(
                left, top, right, bottom,
                CornerRadius(corners.topLeft),
                CornerRadius(corners.topRight),
                CornerRadius(corners.bottomRight),
                CornerRadius(corners.bottomLeft)
            )
        )
        return
    }
    val tl = corners.topLeft
    val tr = corners.topRight
    val br = corners.bottomRight
    val bl = corners.bottomLeft
    moveTo(left + tl, top)
    lineTo(right - tr, top)
    corner(right - tr, top + tr, 1f, -1f, tr, e, fromSide = false)
    lineTo(right, bottom - br)
    corner(right - br, bottom - br, 1f, 1f, br, e, fromSide = true)
    lineTo(left + bl, bottom)
    corner(left + bl, bottom - bl, -1f, 1f, bl, e, fromSide = false)
    lineTo(left, top + tl)
    corner(left + tl, top + tl, -1f, -1f, tl, e, fromSide = true)
    close()
}

private const val CornerSegments = 16

private fun Path.corner(
    cx: Float,
    cy: Float,
    sx: Float,
    sy: Float,
    radius: Float,
    exponent: Float,
    fromSide: Boolean
) {
    if (radius <= 0f) return
    val power = 2f / exponent
    for (i in 1..CornerSegments) {
        val f = i.toFloat() / CornerSegments
        val theta = (if (fromSide) f else 1f - f) * (PI / 2).toFloat()
        val x = cx + sx * radius * cos(theta).coerceAtLeast(0f).pow(power)
        val y = cy + sy * radius * sin(theta).coerceAtLeast(0f).pow(power)
        lineTo(x, y)
    }
}
