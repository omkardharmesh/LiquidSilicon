package io.github.omkardharmesh.liquidsilicon.sample

import android.os.Build

internal actual fun platformSummary(): PlatformSummary {
    val sdk = Build.VERSION.SDK_INT
    val renderer = when {
        sdk >= Build.VERSION_CODES.TIRAMISU -> "Full glass · AGSL shaders"
        sdk >= Build.VERSION_CODES.S -> "Blur fallback · RenderEffect"
        else -> "Frosted fallback"
    }
    val maker = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    return PlatformSummary("Android ${Build.VERSION.RELEASE} (API $sdk)", "$maker ${Build.MODEL}", renderer)
}
