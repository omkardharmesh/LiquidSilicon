package com.liquidglass

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public val LocalGlassBackdrop: ProvidableCompositionLocal<Backdrop?> = staticCompositionLocalOf { null }

@Composable
public fun GlassScreen(
    modifier: Modifier = Modifier,
    tiltLight: Boolean = true,
    background: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    val light = if (tiltLight) rememberDeviceTiltLight() else LocalGlassLight.current
    CompositionLocalProvider(
        LocalGlassBackdrop provides backdrop,
        LocalGlassLight provides light
    ) {
        Box(modifier) {
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop), content = background)
            content()
        }
    }
}

@Composable
public fun glassContentColor(): Color = LocalGlassContentColor.current.takeOrElse { Color.Black }

@Composable
public fun GlassText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color.takeOrElse { style.color.takeOrElse { glassContentColor() } }),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
public fun GlassIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.Unspecified
) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint.takeOrElse { glassContentColor() })
    )
}

@Composable
public fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    style: GlassStyle = GlassStyle.Regular,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true
) {
    GlassButton(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = GlassShape.Circle,
        style = style,
        enabled = enabled
    ) {
        GlassIcon(icon, contentDescription, size = size * 0.42f, tint = tint)
    }
}
