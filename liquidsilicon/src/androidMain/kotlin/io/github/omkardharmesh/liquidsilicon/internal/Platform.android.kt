package io.github.omkardharmesh.liquidsilicon.internal

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect

internal actual fun isRenderEffectSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

internal actual fun isRuntimeShaderSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

internal actual fun createPlatformShader(source: String): PlatformShader {
    check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { "RuntimeShader requires API 33" }
    return AndroidPlatformShader(source)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AndroidPlatformShader(source: String) : PlatformShader {

    private val shader = android.graphics.RuntimeShader(source)

    override fun setFloat(name: String, value: Float) = shader.setFloatUniform(name, value)

    override fun setFloat2(name: String, x: Float, y: Float) = shader.setFloatUniform(name, x, y)

    override fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float) =
        shader.setFloatUniform(name, x, y, z, w)

    override fun setFloatArray(name: String, values: FloatArray) = shader.setFloatUniform(name, values)

    override fun setInt(name: String, value: Int) = shader.setIntUniform(name, value)

    override fun createEffect(inputName: String): RenderEffect =
        android.graphics.RenderEffect.createRuntimeShaderEffect(shader, inputName).asComposeRenderEffect()

    override fun asShader(): Shader = shader
}

internal actual fun ImageBitmap.readArgbPixels(): IntArray? {
    var bitmap = asAndroidBitmap()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
    }
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return null
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    return pixels
}

internal actual fun cacheBackdropAsTexture(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

internal actual fun clipGlassLayers(): Boolean = true
