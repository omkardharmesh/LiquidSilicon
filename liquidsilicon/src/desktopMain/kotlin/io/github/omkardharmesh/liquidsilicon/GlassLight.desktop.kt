package io.github.omkardharmesh.liquidsilicon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
public actual fun rememberDeviceTiltLight(baseAngleDegrees: Float): GlassLight =
    remember(baseAngleDegrees) { GlassLight(baseAngleDegrees) }
