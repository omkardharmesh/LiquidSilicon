package io.github.omkardharmesh.liquidsilicon.sample

internal actual fun platformSummary(): PlatformSummary = PlatformSummary(
    "${System.getProperty("os.name")} ${System.getProperty("os.version")}",
    "JVM ${System.getProperty("java.version")}",
    "Full glass · Skia"
)
