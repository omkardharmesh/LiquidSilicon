package com.liquidglass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class GlassShadow(
    val radius: Dp = 24.dp,
    val offsetY: Dp = 4.dp,
    val alpha: Float = 0.12f
)

@Immutable
public data class GlassStyle(
    val blurRadius: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val dispersion: Float,
    val saturation: Float,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val tint: Color = Color.Unspecified,
    val surface: Color,
    val darkSurface: Color,
    val specular: Float,
    val specularWidth: Dp = 1.dp,
    val shadow: GlassShadow? = GlassShadow(),
    val innerShadow: Float = 0f,
    val innerShadowRadius: Dp = 10.dp,
    val magnification: Float = 1f,
    val adaptive: Boolean = true,
    val fallbackSurface: Color,
    val fallbackDarkSurface: Color,
    val resolutionScale: Float = AutoResolution
) {

    public fun tint(color: Color): GlassStyle = copy(tint = color)

    public companion object {

        public const val AutoResolution: Float = 0f

        public val Regular: GlassStyle = GlassStyle(
            blurRadius = 5.dp,
            refractionHeight = 22.dp,
            refractionAmount = 20.dp,
            dispersion = 0.18f,
            saturation = 1.7f,
            brightness = 0.02f,
            surface = Color.White.copy(alpha = 0.16f),
            darkSurface = Color(0xFF1C1C1E).copy(alpha = 0.22f),
            specular = 0.85f,
            fallbackSurface = Color(0xFFF5F5F7).copy(alpha = 0.88f),
            fallbackDarkSurface = Color(0xFF2C2C2E).copy(alpha = 0.88f)
        )

        public val Clear: GlassStyle = GlassStyle(
            blurRadius = 1.dp,
            refractionHeight = 26.dp,
            refractionAmount = 26.dp,
            dispersion = 0.22f,
            saturation = 1.25f,
            surface = Color.White.copy(alpha = 0.04f),
            darkSurface = Color.Black.copy(alpha = 0.18f),
            specular = 1f,
            shadow = GlassShadow(alpha = 0.08f),
            fallbackSurface = Color(0xFFF5F5F7).copy(alpha = 0.6f),
            fallbackDarkSurface = Color(0xFF2C2C2E).copy(alpha = 0.6f)
        )

        public val Bar: GlassStyle = GlassStyle(
            blurRadius = 8.dp,
            refractionHeight = 24.dp,
            refractionAmount = 24.dp,
            dispersion = 0f,
            saturation = 1.5f,
            surface = Color(0xFFFAFAFA).copy(alpha = 0.4f),
            darkSurface = Color(0xFF121212).copy(alpha = 0.4f),
            specular = 0.7f,
            specularWidth = 0.75.dp,
            fallbackSurface = Color(0xFFF5F5F7).copy(alpha = 0.92f),
            fallbackDarkSurface = Color(0xFF1C1C1E).copy(alpha = 0.92f)
        )
    }
}
