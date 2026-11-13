package com.liquidglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.internal.isRenderEffectSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

private class TabBarInfo(
    val accent: Color,
    val flat: Boolean,
    val scale: () -> Float
)

private val LocalTabBarInfo = staticCompositionLocalOf { TabBarInfo(Color.Unspecified, false) { 1f } }

private val BarPadding = 4.dp
private const val PressedPillScale = 78f / 56f

@Immutable
public data class GlassTabItem(
    val label: String,
    val icon: ImageVector? = null,
    val contentDescription: String? = label
)

@Composable
public fun GlassTabBar(
    items: List<GlassTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.Bar,
    accentColor: Color = Color(0xFF0088FF),
    height: Dp = 64.dp,
    backdrop: Backdrop? = null
) {
    GlassTabBar(
        selectedIndex = selectedIndex,
        onTabSelected = onTabSelected,
        tabCount = items.size,
        modifier = modifier,
        style = style,
        accentColor = accentColor,
        height = height,
        backdrop = backdrop
    ) {
        items.forEachIndexed { index, item ->
            GlassTab(selected = index == selectedIndex, onClick = { onTabSelected(index) }) {
                item.icon?.let { GlassIcon(it, item.contentDescription) }
                GlassText(item.label, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            }
        }
    }
}

@Composable
public fun GlassTabBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabCount: Int,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.Bar,
    accentColor: Color = Color(0xFF0088FF),
    height: Dp = 64.dp,
    backdrop: Backdrop? = null,
    content: @Composable RowScope.() -> Unit
) {
    require(tabCount > 0) { "tabCount must be positive" }
    val resolved = backdrop ?: LocalGlassBackdrop.current
    val scope = rememberCoroutineScope()
    val motion = remember(scope, tabCount) { TabBarMotion(scope, selectedIndex, tabCount) }
    val barState = rememberGlassState()
    val pillState = remember { GlassState() }
    val tabsBackdrop = rememberLayerBackdrop()
    val pillBackdrop = if (resolved != null) rememberCombinedBackdrop(resolved, tabsBackdrop) else tabsBackdrop
    val onSelected by rememberUpdatedState(onTabSelected)
    val flat = !isRenderEffectSupported()

    LaunchedEffect(motion, selectedIndex) { motion.sync(selectedIndex) }

    BoxWithConstraints(modifier.height(height)) {
        val density = LocalDensity.current
        val padding = with(density) { BarPadding.toPx() }
        val tabWidth = (constraints.maxWidth - 2f * padding) / tabCount
        val tabWidthDp = with(density) { tabWidth.toDp() }
        motion.tabWidth = tabWidth
        motion.padding = padding
        motion.maxPanelOffset = with(density) { 4.dp.toPx() }
        motion.barGrow = with(density) { 16.dp.toPx() }

        barState.transformOverride = { size, _ -> motion.barTransform(this, size) }
        pillState.transformOverride = { size, _ -> motion.pillTransform(this, size) }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(motion) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        motion.onDown(down.position.x, down.uptimeMillis)
                        var last = down.position.x
                        var cancelled = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) {
                                cancelled = true
                                break
                            }
                            if (!change.pressed) break
                            if (change.isConsumed) {
                                cancelled = true
                                break
                            }
                            val dx = change.position.x - last
                            if (dx != 0f) {
                                motion.onDrag(change.position.x, dx, change.uptimeMillis)
                                change.consume()
                                last = change.position.x
                            }
                        }
                        val index = motion.onUp(cancelled)
                        if (index != null) onSelected(index)
                    }
                }
        ) {
            CompositionLocalProvider(
                LocalTabBarInfo provides TabBarInfo(accentColor, flat) { 1f },
                LocalGlassContentColor provides barState.contentColor
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .glassWithoutGestures(resolved, GlassShape.Capsule, style, barState)
                        .padding(horizontal = BarPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }

            if (!flat) {
                CompositionLocalProvider(
                    LocalTabBarInfo provides TabBarInfo(accentColor, false) { 1f + 0.2f * motion.press },
                    LocalGlassContentColor provides accentColor
                ) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .clearAndSetSemantics {}
                            .alpha(0f)
                            .layerBackdrop(tabsBackdrop)
                            .glassWithoutGestures(
                                resolved,
                                GlassShape.Capsule,
                                style.copy(adaptive = false, shadow = null),
                                barState
                            )
                            .padding(horizontal = BarPadding)
                            .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                        verticalAlignment = Alignment.CenterVertically,
                        content = content
                    )
                }
            }

            SelectionPill(motion, pillBackdrop, pillState, barState, tabWidthDp)
        }
    }
}

@Composable
public fun RowScope.GlassTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val info = LocalTabBarInfo.current
    val color = if (selected) info.accent else LocalGlassContentColor.current
    Column(
        modifier
            .weight(1f)
            .fillMaxHeight()
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
                onClick {
                    onClick()
                    true
                }
            }
            .graphicsLayer {
                val s = info.scale()
                scaleX = s
                scaleY = s
            },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(LocalGlassContentColor provides color) {
            content()
        }
    }
}

@Composable
private fun BoxScope.SelectionPill(
    motion: TabBarMotion,
    backdrop: Backdrop,
    state: GlassState,
    barState: GlassState,
    tabWidth: Dp
) {
    val progress = motion.press.coerceIn(0f, 1f)
    val restFill = if (barState.luminance > 0.5f) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f)
    val style = remember(progress, restFill) { pillStyle(progress, restFill) }
    Box(
        Modifier
            .align(Alignment.CenterStart)
            .offset { IntOffset((motion.padding + motion.value * motion.tabWidth + motion.panelOffset).roundToInt(), 0) }
            .padding(vertical = BarPadding)
            .width(tabWidth)
            .fillMaxHeight()
            .then(
                if (progress > 0.001f) {
                    Modifier.glassWithoutGestures(backdrop, GlassShape.Capsule, style, state)
                } else {
                    Modifier.background(restFill, CircleShape)
                }
            )
    )
}

private fun pillStyle(progress: Float, restFill: Color): GlassStyle {
    val surface = lerp(restFill, Color.Black.copy(alpha = 0.03f), progress)
    return GlassStyle(
        blurRadius = 0.dp,
        refractionHeight = 18.dp * progress,
        refractionAmount = 10.dp * progress,
        dispersion = 0.35f * progress,
        saturation = 1f,
        surface = surface,
        darkSurface = surface,
        specular = progress,
        specularWidth = 0.75.dp,
        shadow = if (progress > 0.01f) GlassShadow(radius = 16.dp, offsetY = 4.dp, alpha = 0.16f * progress) else null,
        innerShadow = 0.2f * progress,
        innerShadowRadius = 8.dp,
        adaptive = false,
        fallbackSurface = restFill,
        fallbackDarkSurface = restFill,
        resolutionScale = 1f
    )
}

@Stable
private class TabBarMotion(
    private val scope: CoroutineScope,
    initialIndex: Int,
    private val count: Int
) {
    private val valueAnim = Animatable(initialIndex.toFloat(), 0.001f)
    private val pressAnim = Animatable(0f, 0.001f)
    private val scaleXAnim = Animatable(1f, 0.001f)
    private val scaleYAnim = Animatable(1f, 0.001f)
    private val velocityAnim = Animatable(0f, 0.01f)
    private val panelAnim = Animatable(0f, 0.5f)

    var tabWidth = 1f
    var padding = 0f
    var maxPanelOffset = 0f
    var barGrow = 0f

    private var target = initialIndex.toFloat()
    private var dragging = false
    private var lastDragTime = 0L
    private var releaseJob: Job? = null

    val value: Float get() = valueAnim.value
    val press: Float get() = pressAnim.value

    val panelOffset: Float
        get() {
            val fraction = (panelAnim.value / (tabWidth * count)).coerceIn(-1f, 1f)
            return maxPanelOffset * fraction.sign * EaseOut.transform(abs(fraction))
        }

    private val maxIndex get() = (count - 1).toFloat()

    private fun indexAt(x: Float): Float = ((x - padding) / tabWidth - 0.5f).coerceIn(0f, maxIndex)

    fun onDown(x: Float, time: Long) {
        dragging = true
        lastDragTime = time
        lift()
        moveTo(indexAt(x))
    }

    fun onDrag(x: Float, dx: Float, time: Long) {
        val previous = target
        val next = indexAt(x)
        moveTo(next)
        val dt = (time - lastDragTime).coerceIn(1L, 100L) / 1000f
        lastDragTime = time
        val velocity = ((next - previous) / dt / maxIndex.coerceAtLeast(1f)).coerceIn(-20f, 20f)
        scope.launch { velocityAnim.animateTo(velocity, VelocitySpring) }
        scope.launch { panelAnim.snapTo(panelAnim.value + dx) }
    }

    fun onUp(cancelled: Boolean): Int? {
        dragging = false
        val index = if (cancelled) null else target.roundToInt().coerceIn(0, count - 1)
        if (index != null) moveTo(index.toFloat())
        scope.launch { velocityAnim.animateTo(0f, VelocitySpring) }
        scope.launch { panelAnim.animateTo(0f, spring(1f, 300f, 0.5f)) }
        settle()
        return index
    }

    fun sync(index: Int) {
        if (dragging) return
        if (target == index.toFloat()) return
        lift()
        moveTo(index.toFloat())
        settle()
    }

    private fun moveTo(value: Float) {
        target = value
        scope.launch { valueAnim.animateTo(value, ValueSpring) }
    }

    private fun lift() {
        releaseJob?.cancel()
        scope.launch { pressAnim.animateTo(1f, PressSpring) }
        scope.launch { scaleXAnim.animateTo(PressedPillScale, ScaleXSpring) }
        scope.launch { scaleYAnim.animateTo(PressedPillScale, ScaleYSpring) }
    }

    private fun settle() {
        releaseJob?.cancel()
        releaseJob = scope.launch {
            val threshold = maxIndex.coerceAtLeast(1f) * 0.025f
            snapshotFlow { abs(valueAnim.value - target) < threshold }.first { it }
            if (dragging) return@launch
            launch { pressAnim.animateTo(0f, PressSpring) }
            launch { scaleXAnim.animateTo(1f, ScaleXSpring) }
            launch { scaleYAnim.animateTo(1f, ScaleYSpring) }
        }
    }

    fun barTransform(out: GlassTransform, size: androidx.compose.ui.geometry.Size) {
        val p = pressAnim.value
        val s = 1f + barGrow * p / size.width
        val panel = panelOffset
        val hw = size.width / 2f * s
        val hh = size.height / 2f * s
        out.left = size.width / 2f - hw + panel
        out.right = size.width / 2f + hw + panel
        out.top = size.height / 2f - hh
        out.bottom = size.height / 2f + hh
        out.translation = Offset(panel, 0f)
        out.contentScale = s
        out.glowCenter = Offset(padding + (valueAnim.value + 0.5f) * tabWidth + panel, size.height / 2f)
        out.glowRadius = size.height * 1.6f
        out.glowIntensity = 0.1f * p.coerceIn(0f, 1f)
    }

    fun pillTransform(out: GlassTransform, size: androidx.compose.ui.geometry.Size) {
        val v = abs(velocityAnim.value) / 10f
        val sx = scaleXAnim.value / (1f - (v * 0.75f).coerceAtMost(0.2f))
        val sy = scaleYAnim.value * (1f - (v * 0.25f).coerceAtMost(0.2f))
        val hw = size.width / 2f * sx
        val hh = size.height / 2f * sy
        out.left = size.width / 2f - hw
        out.right = size.width / 2f + hw
        out.top = size.height / 2f - hh
        out.bottom = size.height / 2f + hh
        out.glowCenter = Offset(size.width / 2f, size.height / 2f)
        out.glowRadius = size.minDimension
        out.glowIntensity = 0.05f * pressAnim.value.coerceIn(0f, 1f)
    }

    private companion object {
        val ValueSpring = spring(1f, 1000f, 0.001f)
        val VelocitySpring = spring(0.5f, 300f, 0.01f)
        val PressSpring = spring(1f, 1000f, 0.001f)
        val ScaleXSpring = spring(0.6f, 250f, 0.001f)
        val ScaleYSpring = spring(0.7f, 250f, 0.001f)
    }
}
