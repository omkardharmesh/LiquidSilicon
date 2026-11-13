package com.liquidglass.internal

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader

internal expect fun isRenderEffectSupported(): Boolean

internal expect fun isRuntimeShaderSupported(): Boolean

internal interface PlatformShader {

    fun setFloat(name: String, value: Float)

    fun setFloat2(name: String, x: Float, y: Float)

    fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float)

    fun setFloatArray(name: String, values: FloatArray)

    fun setInt(name: String, value: Int)

    fun createEffect(inputName: String): RenderEffect

    fun asShader(): Shader
}

internal expect fun createPlatformShader(source: String): PlatformShader

internal expect fun ImageBitmap.readArgbPixels(): IntArray?

internal expect fun cacheBackdropAsTexture(): Boolean

internal expect fun clipGlassLayers(): Boolean
