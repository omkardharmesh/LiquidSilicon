# LiquidGlass

iOS 26–style Liquid Glass for Compose Multiplatform (Android, iOS, desktop).

## Quick start

Wrap the screen in `GlassScreen`. Whatever you put in `background` is what the glass sees through.
Anything in the content block can be glass — no other setup.

```kotlin
GlassScreen(
    background = { EpisodeList() }          // scrolling content, video (TextureView), images…
) {
    GlassButton(onClick = { play() }, modifier = Modifier.align(Alignment.TopEnd).height(48.dp)) {
        GlassText("Play")
    }

    GlassTabBar(
        items = listOf(
            GlassTabItem("Home", Icons.Default.Home),
            GlassTabItem("Shorts", Icons.Default.PlayArrow),
            GlassTabItem("Profile", Icons.Default.Person)
        ),
        selectedIndex = tab,
        onTabSelected = { tab = it },
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
    )
}
```

## Building blocks

| Want | Use |
|---|---|
| Glass on any composable | `Modifier.glass(shape, style, interactive)` |
| Glass box with content | `GlassSurface { … }` |
| Button | `GlassButton(onClick) { … }`, `GlassIconButton(onClick, icon, "desc")` |
| iOS tab bar with lens | `GlassTabBar(items, selectedIndex, onTabSelected)` |
| Shapes that melt together | `GlassContainer(spacing = 16.dp) { Box(Modifier.glass(GlassShape.Circle)) }` |
| Text / icons that stay readable | `GlassText`, `GlassIcon` (auto black/white from what's behind) |

Shapes: `GlassShape.Capsule` (default), `Circle`, `Card`, `RoundedRect(radius)`.
Styles: `GlassStyle.Regular` (default), `Clear`, `Bar`, `Regular.tint(color)`, or `copy(blurRadius = …)`.

Inside `GlassContainer`, `Modifier.glass(...)` registers the shape with the container instead of drawing on its
own, so neighbours closer than `spacing` merge.

## Rules

1. Glass must be inside `GlassScreen`'s content (or pass `backdrop =` explicitly). Outside, it draws a plain
   frosted fill instead of crashing.
2. Content drawn by a `SurfaceView` (ExoPlayer's default `PlayerView`) is invisible to glass. Use
   `surface_type="texture_view"` if glass sits over video.
3. Judge performance on release builds; debug Compose is several times slower.

## Advanced

`rememberLayerBackdrop()` + `Modifier.layerBackdrop(backdrop)` + `backdrop = backdrop` on any glass API lets you
capture something other than the screen background. `rememberGlassState()` exposes `isPressed`,
`pressProgress` and `luminance`. `LocalGlassLight` controls the highlight direction (`GlassScreen` enables
device tilt by default; pass `tiltLight = false` to disable).

## Rendering per platform

| Platform | Result |
|---|---|
| Android 13+ (API 33), iOS, desktop | Full: refraction, dispersion, blur, specular, shadow, merging |
| Android 12 (API 31–32) | Blurred + saturated backdrop clipped to shape, gradient rim, no refraction |
| Android 7–11 (API 24–30) | Translucent adaptive fill + gradient rim |

## Performance

moto g85 (Snapdragon 695), R8 release build, scrolling the sample:

| | median frame | p90 | janky |
|---|---|---|---|
| sample without glass | 9 ms | 11 ms | 0.6% |
| sample with all glass | 12–13 ms | 14 ms | 1.3% |

`scripts/bench.sh` reproduces the scroll benchmark on a connected device.

## Build

```bash
./gradlew :liquidglass:publishToMavenLocal
./gradlew :androidApp:installRelease        # demo
./gradlew :sample:run                       # desktop demo
./gradlew :sample:desktopTest               # renders PNGs to sample/build/screenshots
```

## Publishing

Set in `~/.gradle/gradle.properties`:

```
liquidglass.repoUrl=https://maven.pkg.github.com/<user>/<repo>
liquidglass.repoUser=<github user>
liquidglass.repoToken=<token with write:packages>
```

Then `./gradlew :liquidglass:publishAllPublicationsToRemoteRepository`. Change `GROUP` / `VERSION_NAME` in
`gradle.properties` as needed.
