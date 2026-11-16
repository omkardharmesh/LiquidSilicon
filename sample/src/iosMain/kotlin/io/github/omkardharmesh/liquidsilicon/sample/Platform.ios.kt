package io.github.omkardharmesh.liquidsilicon.sample

import platform.UIKit.UIDevice

internal actual fun platformSummary(): PlatformSummary {
    val device = UIDevice.currentDevice
    return PlatformSummary("${device.systemName} ${device.systemVersion}", device.model, "Full glass · Skia/Metal")
}
