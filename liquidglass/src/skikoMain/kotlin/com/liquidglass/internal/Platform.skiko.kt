package com.liquidglass.internal

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.asComposeShader
import androidx.compose.ui.graphics.asComposeRenderEffect
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal actual fun isRenderEffectSupported(): Boolean = true

internal actual fun isRuntimeShaderSupported(): Boolean = true

internal actual fun createPlatformShader(source: String): PlatformShader = SkikoPlatformShader(source)

private class SkikoPlatformShader(source: String) : PlatformShader {

    private val builder = RuntimeShaderBuilder(RuntimeEffect.makeForShader(source))

    override fun setFloat(name: String, value: Float) = builder.uniform(name, value)

    override fun setFloat2(name: String, x: Float, y: Float) = builder.uniform(name, x, y)

    override fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float) =
        builder.uniform(name, x, y, z, w)

    override fun setFloatArray(name: String, values: FloatArray) = builder.uniform(name, values)

    override fun setInt(name: String, value: Int) = builder.uniform(name, value)

    override fun createEffect(inputName: String): RenderEffect =
        ImageFilter.makeRuntimeShader(builder, inputName, null).asComposeRenderEffect()

    override fun asShader(): Shader = builder.makeShader().asComposeShader()
}

internal actual fun ImageBitmap.readArgbPixels(): IntArray? {
    if (width <= 0 || height <= 0) return null
    val pixels = IntArray(width * height)
    readPixels(pixels)
    return pixels
}

internal actual fun cacheBackdropAsTexture(): Boolean = false

internal actual fun clipGlassLayers(): Boolean = false
