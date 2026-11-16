package io.github.omkardharmesh.liquidsilicon.sample

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

internal object AppLogo {

    const val Canvas = "M0,0h108v108h-108z"
    const val Blob = "M69.59,54.88 A21,21 0 1,1 54.09,38.63 A6,6 0 0,0 61.15,34.94 A10,10 0 1,1 73.61,48 A6,6 0 0,0 69.59,54.88 Z"
    const val Shadow = "M23,64 A27,27 0 1,1 77,64 A27,27 0 1,1 23,64 Z"
    const val Specular = "M32.57,55.51 A16.8,16.8 0 0,1 43.81,43.02"
    const val BudSpecular = "M63.73,36.04 A7.2,7.2 0 0,1 68.04,31.73"
    const val PrismCyan = "M67.45,64.99 A19.4,19.4 0 0,1 48.32,78.39"

    val Indigo = Color(0xFF5E5CE6)
    val Purple = Color(0xFFBF5AF2)
    val Pink = Color(0xFFFF375F)
    val Orange = Color(0xFFFF9F0A)
    val Cyan = Color(0xFF64D2FF)

    val Full: ImageVector by lazy {
        ImageVector.Builder("LiquidSiliconLogo", 108.dp, 108.dp, 108f, 108f).apply {
            background()
            foreground()
        }.build()
    }

    private fun ImageVector.Builder.background() {
        val canvas = addPathNodes(Canvas)
        addPath(canvas, fill = Brush.linearGradient(listOf(Indigo, Purple, Pink), Offset(0f, 0f), Offset(108f, 108f)))
        addPath(canvas, fill = Brush.radialGradient(listOf(Orange.copy(alpha = 0.9f), Orange.copy(alpha = 0f)), Offset(96f, 104f), 62f))
        addPath(canvas, fill = Brush.radialGradient(listOf(Cyan.copy(alpha = 0.75f), Cyan.copy(alpha = 0f)), Offset(8f, 6f), 64f))
        addPath(canvas, fill = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), Offset(40f, 30f), 44f))
    }

    private fun ImageVector.Builder.foreground() {
        val blob = addPathNodes(Blob)
        addPath(
            addPathNodes(Shadow),
            fill = Brush.radialGradient(
                0f to Color.Black.copy(alpha = 0.32f),
                0.72f to Color.Black.copy(alpha = 0.12f),
                1f to Color.Transparent,
                center = Offset(50f, 64f),
                radius = 27f
            )
        )
        addPath(
            blob,
            fill = Brush.linearGradient(
                listOf(Color(0xFFFF6FA0), Color(0xFFC86CF5), Color(0xFF7F86FF)),
                Offset(34f, 40f),
                Offset(66f, 78f)
            )
        )
        addPath(
            blob,
            fill = Brush.radialGradient(
                listOf(Color(0xFFFFC27A).copy(alpha = 0.55f), Color(0xFFFFC27A).copy(alpha = 0f)),
                Offset(64f, 44f),
                16f
            )
        )
        addPath(
            blob,
            fill = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.28f)),
                Offset(32f, 36f),
                Offset(68f, 80f)
            )
        )
        addPath(
            blob,
            stroke = Brush.linearGradient(
                listOf(Color.White, Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.85f)),
                Offset(30f, 34f),
                Offset(72f, 80f)
            ),
            strokeLineWidth = 1.5f
        )
        stroke(PrismCyan, Cyan.copy(alpha = 0.7f), 0.9f)
        stroke(Specular, Color.White.copy(alpha = 0.95f), 2.8f)
        stroke(BudSpecular, Color.White.copy(alpha = 0.9f), 2f)
    }

    private fun ImageVector.Builder.stroke(path: String, color: Color, width: Float) {
        addPath(addPathNodes(path), stroke = SolidColor(color), strokeLineWidth = width, strokeLineCap = StrokeCap.Round)
    }
}
