package io.github.omkardharmesh.liquidsilicon.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import io.github.omkardharmesh.liquidsilicon.ResolvedCorners
import kotlin.math.max
import kotlin.math.min

internal class GlassShapes {

    var count: Int = 0
        private set

    val rects = FloatArray(MaxGlassShapes * 4)
    val radii = FloatArray(MaxGlassShapes * 4)
    val extras = FloatArray(MaxGlassShapes * 4)
    val tints = FloatArray(MaxGlassShapes * 4)
    val glows = FloatArray(MaxGlassShapes * 4)
    val corners = Array(MaxGlassShapes) { ResolvedCorners() }

    var left = 0f
        private set
    var top = 0f
        private set
    var right = 0f
        private set
    var bottom = 0f
        private set

    fun clear() {
        count = 0
        rects.fill(0f)
        radii.fill(0f)
        extras.fill(0f)
        tints.fill(0f)
        glows.fill(0f)
    }

    fun add(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        corners: ResolvedCorners,
        refractionHeight: Float,
        tint: Color,
        glowX: Float,
        glowY: Float,
        glowRadius: Float,
        glowIntensity: Float
    ) {
        if (count >= MaxGlassShapes) return
        if (right <= left || bottom <= top) return
        val i = count * 4
        val hw = (right - left) / 2f
        val hh = (bottom - top) / 2f
        rects[i] = left + hw
        rects[i + 1] = top + hh
        rects[i + 2] = hw
        rects[i + 3] = hh
        radii[i] = corners.topLeft
        radii[i + 1] = corners.topRight
        radii[i + 2] = corners.bottomRight
        radii[i + 3] = corners.bottomLeft
        extras[i] = corners.exponent
        extras[i + 1] = min(refractionHeight, min(hw, hh))
        if (tint.isSpecified) {
            tints[i] = tint.red
            tints[i + 1] = tint.green
            tints[i + 2] = tint.blue
            tints[i + 3] = tint.alpha
        }
        glows[i] = glowX
        glows[i + 1] = glowY
        glows[i + 2] = glowRadius
        glows[i + 3] = glowIntensity
        this.corners[count].set(
            corners.topLeft,
            corners.topRight,
            corners.bottomRight,
            corners.bottomLeft,
            corners.exponent
        )
        if (count == 0) {
            this.left = left
            this.top = top
            this.right = right
            this.bottom = bottom
        } else {
            this.left = min(this.left, left)
            this.top = min(this.top, top)
            this.right = max(this.right, right)
            this.bottom = max(this.bottom, bottom)
        }
        count++
    }

    fun shapeLeft(index: Int): Float = rects[index * 4] - rects[index * 4 + 2]

    fun tintAt(index: Int): Color? {
        val j = index * 4
        val alpha = tints[j + 3]
        return if (alpha > 0f) Color(tints[j], tints[j + 1], tints[j + 2], alpha) else null
    }
    fun shapeTop(index: Int): Float = rects[index * 4 + 1] - rects[index * 4 + 3]
    fun shapeRight(index: Int): Float = rects[index * 4] + rects[index * 4 + 2]
    fun shapeBottom(index: Int): Float = rects[index * 4 + 1] + rects[index * 4 + 3]
}
