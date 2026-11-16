package io.github.omkardharmesh.liquidsilicon

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.omkardharmesh.liquidsilicon.internal.GlassRenderer
import io.github.omkardharmesh.liquidsilicon.internal.GlassShapes
import io.github.omkardharmesh.liquidsilicon.internal.LuminanceSubscription
import io.github.omkardharmesh.liquidsilicon.internal.detectGlassPress

@Stable
public interface GlassContainerScope {

    public fun Modifier.glass(
        shape: GlassShape = GlassShape.Capsule,
        interactive: Boolean = false,
        tint: Color = Color.Unspecified,
        state: GlassState? = null
    ): Modifier
}

@Composable
public fun GlassContainer(
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle.Regular,
    spacing: Dp = 16.dp,
    state: GlassState = rememberGlassState(),
    contentAlignment: Alignment = Alignment.TopStart,
    backdrop: Backdrop? = null,
    content: @Composable GlassContainerScope.() -> Unit
) {
    val registry = remember { GlassRegistry() }
    Box(
        modifier = modifier.then(GlassContainerElement(backdrop, style, spacing, state, registry)),
        contentAlignment = contentAlignment
    ) {
        val scope = remember(registry) { GlassContainerScopeImpl(registry) }
        CompositionLocalProvider(LocalGlassContentColor provides state.contentColor) {
            scope.content()
        }
    }
}

internal class GlassRegistry {
    val entries = mutableStateListOf<GlassEntry>()
}

@Stable
internal class GlassEntry(state: GlassState) {
    var shape: GlassShape by mutableStateOf(GlassShape.Capsule)
    var interactive: Boolean by mutableStateOf(false)
    var tint: Color by mutableStateOf(Color.Unspecified)
    var state: GlassState by mutableStateOf(state)
    var coordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())
}

private class GlassContainerScopeImpl(
    private val registry: GlassRegistry
) : GlassContainerScope {

    override fun Modifier.glass(
        shape: GlassShape,
        interactive: Boolean,
        tint: Color,
        state: GlassState?
    ): Modifier = this then GlassItemElement(registry, shape, interactive, tint, state)
}

private data class GlassItemElement(
    val registry: GlassRegistry,
    val shape: GlassShape,
    val interactive: Boolean,
    val tint: Color,
    val state: GlassState?
) : ModifierNodeElement<GlassItemNode>() {

    override fun create(): GlassItemNode = GlassItemNode(registry, shape, interactive, tint, state)

    override fun update(node: GlassItemNode) {
        node.update(registry, shape, interactive, tint, state)
    }
}

private class GlassItemNode(
    private var registry: GlassRegistry,
    shape: GlassShape,
    interactive: Boolean,
    tint: Color,
    private var externalState: GlassState?
) : DelegatingNode(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private val internalState by lazy { GlassState() }
    private val entry = GlassEntry(externalState ?: internalState).also {
        it.shape = shape
        it.interactive = interactive
        it.tint = tint
    }
    private val transform = GlassTransform()

    init {
        delegate(
            SuspendingPointerInputModifierNode {
                detectGlassPress({ entry.interactive }, { entry.state }, this@GlassItemNode.coroutineScope)
            }
        )
    }

    fun update(
        registry: GlassRegistry,
        shape: GlassShape,
        interactive: Boolean,
        tint: Color,
        state: GlassState?
    ) {
        if (registry !== this.registry) {
            this.registry.entries.remove(entry)
            this.registry = registry
            if (isAttached) registry.entries.add(entry)
        }
        externalState = state
        entry.shape = shape
        entry.interactive = interactive
        entry.tint = tint
        entry.state = state ?: internalState
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        entry.state.computeTransform(size, this, entry.interactive, transform)
        if (transform.isIdentity) {
            drawContent()
        } else {
            translate(transform.translation.x, transform.translation.y) {
                scale(transform.contentScale, transform.contentScale, center) {
                    this@draw.drawContent()
                }
            }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) entry.coordinates = coordinates
    }

    override fun onAttach() {
        registry.entries.add(entry)
    }

    override fun onDetach() {
        registry.entries.remove(entry)
        entry.coordinates = null
    }
}

private data class GlassContainerElement(
    val backdrop: Backdrop?,
    val style: GlassStyle,
    val spacing: Dp,
    val state: GlassState,
    val registry: GlassRegistry
) : ModifierNodeElement<GlassContainerNode>() {

    override fun create(): GlassContainerNode = GlassContainerNode(backdrop, style, spacing, state, registry)

    override fun update(node: GlassContainerNode) {
        node.update(backdrop, style, spacing, state, registry)
    }
}

private class GlassContainerNode(
    private var backdrop: Backdrop?,
    private var style: GlassStyle,
    private var spacing: Dp,
    private var state: GlassState,
    private var registry: GlassRegistry
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode, CompositionLocalConsumerModifierNode {

    private val renderer = GlassRenderer()
    private val shapes = GlassShapes()
    private val corners = ResolvedCorners()
    private val transform = GlassTransform()
    private val luminance = LuminanceSubscription()
    private var lastPosition = Offset.Unspecified

    fun update(
        backdrop: Backdrop?,
        style: GlassStyle,
        spacing: Dp,
        state: GlassState,
        registry: GlassRegistry
    ) {
        val restartSampling = this.style.adaptive != style.adaptive || this.backdrop != backdrop
        this.backdrop = backdrop
        this.style = style
        this.spacing = spacing
        this.state = state
        this.registry = registry
        if (restartSampling && isAttached) {
            luminance.stop()
            startSampling()
        }
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val container = requireLayoutCoordinates()
        shapes.clear()
        for (entry in registry.entries) {
            val coordinates = entry.coordinates ?: continue
            if (!coordinates.isAttached) continue
            val bounds = container.localBoundingBoxOf(coordinates, clipBounds = false)
            entry.state.computeTransform(bounds.size, this, entry.interactive, transform)
            entry.shape.resolve(
                Size(transform.right - transform.left, transform.bottom - transform.top),
                this,
                layoutDirection,
                corners
            )
            shapes.add(
                bounds.left + transform.left,
                bounds.top + transform.top,
                bounds.left + transform.right,
                bounds.top + transform.bottom,
                corners,
                style.refractionHeight.toPx(),
                entry.tint,
                bounds.left + transform.glowCenter.x,
                bounds.top + transform.glowCenter.y,
                transform.glowRadius,
                transform.glowIntensity
            )
        }
        val light = currentValueOf(LocalGlassLight).direction
        with(renderer) {
            drawGlass(resolvedBackdrop(), container, shapes, style, light, state.luminance, spacing.toPx())
        }
        drawContent()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (!coordinates.isAttached) return
        val position = coordinates.positionInWindow()
        if (position != lastPosition) {
            lastPosition = position
            invalidateDraw()
        }
    }

    override fun onAttach() {
        val context = requireGraphicsContext()
        renderer.attach(context)
        startSampling()
    }

    override fun onDetach() {
        luminance.stop()
        val context = requireGraphicsContext()
        renderer.detach(context)
        lastPosition = Offset.Unspecified
    }

    private fun resolvedBackdrop(): Backdrop? = backdrop ?: currentValueOf(LocalGlassBackdrop)

    private fun startSampling() {
        if (!style.adaptive) return
        val source = resolvedBackdrop()?.luminanceSource() ?: return
        luminance.start(
            scope = coroutineScope,
            source = source,
            coordinates = { if (isAttached) requireLayoutCoordinates() else null },
            state = { state }
        )
    }

}
