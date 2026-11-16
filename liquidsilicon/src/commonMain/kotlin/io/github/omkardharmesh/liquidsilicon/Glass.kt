package io.github.omkardharmesh.liquidsilicon

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.semantics.Role
import io.github.omkardharmesh.liquidsilicon.internal.GlassRenderer
import io.github.omkardharmesh.liquidsilicon.internal.GlassShapes
import io.github.omkardharmesh.liquidsilicon.internal.LuminanceSubscription
import io.github.omkardharmesh.liquidsilicon.internal.detectGlassPress

public val LocalGlassContentColor: ProvidableCompositionLocal<Color> = compositionLocalOf { Color.Unspecified }

public fun Modifier.glass(
    shape: GlassShape = GlassShape.Capsule,
    style: GlassStyle = GlassStyle.Regular,
    interactive: Boolean = false,
    state: GlassState? = null,
    backdrop: Backdrop? = null
): Modifier = this then GlassElement(backdrop, shape, style, interactive, state, gestures = interactive)

internal fun Modifier.glassWithoutGestures(
    backdrop: Backdrop?,
    shape: GlassShape,
    style: GlassStyle,
    state: GlassState
): Modifier = this then GlassElement(backdrop, shape, style, interactive = true, state = state, gestures = false)

@Composable
public fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: GlassShape = GlassShape.Capsule,
    style: GlassStyle = GlassStyle.Regular,
    interactive: Boolean = false,
    state: GlassState = rememberGlassState(),
    contentAlignment: Alignment = Alignment.Center,
    backdrop: Backdrop? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.glass(shape, style, interactive, state, backdrop),
        contentAlignment = contentAlignment
    ) {
        CompositionLocalProvider(LocalGlassContentColor provides state.contentColor) {
            content()
        }
    }
}

@Composable
public fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: GlassShape = GlassShape.Capsule,
    style: GlassStyle = GlassStyle.Regular,
    enabled: Boolean = true,
    state: GlassState = rememberGlassState(),
    backdrop: Backdrop? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        backdrop = backdrop,
        modifier = modifier.clickable(
            interactionSource = null,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        ),
        shape = shape,
        style = style,
        interactive = enabled,
        state = state,
        content = content
    )
}

private data class GlassElement(
    val backdrop: Backdrop?,
    val shape: GlassShape,
    val style: GlassStyle,
    val interactive: Boolean,
    val state: GlassState?,
    val gestures: Boolean
) : ModifierNodeElement<GlassNode>() {

    override fun create(): GlassNode = GlassNode(backdrop, shape, style, interactive, state, gestures)

    override fun update(node: GlassNode) {
        node.update(backdrop, shape, style, interactive, state, gestures)
    }
}

internal class GlassNode(
    private var backdrop: Backdrop?,
    private var shape: GlassShape,
    private var style: GlassStyle,
    private var interactive: Boolean,
    private var externalState: GlassState?,
    private var gestures: Boolean
) : DelegatingNode(), DrawModifierNode, GlobalPositionAwareModifierNode, CompositionLocalConsumerModifierNode {

    private val internalState by lazy { GlassState() }
    private val state: GlassState get() = externalState ?: internalState

    private val renderer = GlassRenderer()
    private val shapes = GlassShapes()
    private val corners = ResolvedCorners()
    private val transform = GlassTransform()
    private val luminance = LuminanceSubscription()
    private var lastPosition = Offset.Unspecified

    init {
        delegate(
            SuspendingPointerInputModifierNode {
                detectGlassPress({ interactive && gestures }, { state }, this@GlassNode.coroutineScope)
            }
        )
    }

    fun update(
        backdrop: Backdrop?,
        shape: GlassShape,
        style: GlassStyle,
        interactive: Boolean,
        state: GlassState?,
        gestures: Boolean
    ) {
        this.gestures = gestures
        val adaptiveChanged = this.style.adaptive != style.adaptive || this.backdrop != backdrop
        this.backdrop = backdrop
        this.shape = shape
        this.style = style
        this.interactive = interactive
        this.externalState = state
        if (adaptiveChanged && isAttached) {
            luminance.stop()
            startSampling()
        }
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val coordinates = requireLayoutCoordinates()
        val state = state
        state.computeTransform(size, this, interactive, transform)

        shapes.clear()
        shape.resolve(Size(transform.right - transform.left, transform.bottom - transform.top), this, layoutDirection, corners)
        shapes.add(
            transform.left,
            transform.top,
            transform.right,
            transform.bottom,
            corners,
            style.refractionHeight.toPx(),
            style.tint,
            transform.glowCenter.x,
            transform.glowCenter.y,
            transform.glowRadius,
            transform.glowIntensity
        )
        val light = currentValueOf(LocalGlassLight).direction
        with(renderer) {
            drawGlass(resolvedBackdrop(), coordinates, shapes, style, light, state.luminance, 0f)
        }

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
