# LiquidSilicon

iOS 26–style Liquid Glass for Compose Multiplatform (Android, iOS, desktop).

## Install

Not published yet. Once it is:

```kotlin
implementation("io.github.omkardharmesh:liquidsilicon:0.1.0")
```

Everything lives in the `io.github.omkardharmesh.liquidsilicon` package.

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

## Demo app

Four tabs, all sharing one floating `GlassTabBar` (press and drag it to lift the selection into a lens):

- Home: streaming home page. Glass top bar, and glass Play / My List buttons that refract the hero as it scrolls.
- Shorts: vertical pager with a glass For You / Following switch, a glass caption card, and an action column
  in a `GlassContainer`. Tapping Share makes new buttons melt out of it.
- Lab: a draggable lens over text, a grid or a live scene, with live sliders for blur, bend, edge, prism,
  vibrance and tint, plus style and shape presets.
- About: device, renderer tier and FPS, a tilt-light switch, and feature cards.

## Performance

moto g85 (Snapdragon 695), R8 release build, scrolling the earlier single-screen sample at 60 Hz:

| | median frame | p90 | janky |
|---|---|---|---|
| sample without glass | 9 ms | 11 ms | 0.6% |
| sample with all glass | 12–13 ms | 14 ms | 1.3% |

`scripts/bench.sh` reproduces the scroll benchmark on a connected device.

The four-tab demo on the same phone, scrolling or swiping at 120 Hz (R8 release build): Home 115 fps, Shorts 100 fps,
Lab 119 fps, About 86 fps. About is the heaviest screen, with five large glass cards over an animated background.
Every tab holds 58–60 fps at 60 Hz.

## Build

```bash
./gradlew :liquidsilicon:publishToMavenLocal
./gradlew :androidApp:installRelease        # demo
./gradlew :sample:run                       # desktop demo
./gradlew :sample:desktopTest               # renders PNGs to sample/build/screenshots
```

## Signing

`:androidApp` release builds are signed from `keystore.properties` in the repo root (gitignored), falling back
to `liquidsilicon.signing.*` Gradle properties, then to the debug key:

```
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## Publishing

Set in `~/.gradle/gradle.properties`:

```
liquidsilicon.repoUrl=https://maven.pkg.github.com/<user>/<repo>
liquidsilicon.repoUser=<github user>
liquidsilicon.repoToken=<token with write:packages>
```

Then `./gradlew :liquidsilicon:publishAllPublicationsToRemoteRepository`. Change `GROUP` / `VERSION_NAME` in
`gradle.properties` as needed.
