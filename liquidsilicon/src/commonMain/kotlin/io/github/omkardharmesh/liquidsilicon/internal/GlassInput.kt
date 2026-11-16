package io.github.omkardharmesh.liquidsilicon.internal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import io.github.omkardharmesh.liquidsilicon.GlassState
import kotlinx.coroutines.CoroutineScope

internal suspend fun PointerInputScope.detectGlassPress(
    enabled: () -> Boolean,
    state: () -> GlassState,
    scope: CoroutineScope
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
        if (!enabled()) return@awaitEachGesture
        val glassState = state()
        glassState.onPress(down.position, scope)
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            if (change.isConsumed && change.positionChanged()) break
            glassState.onDrag(change.position, down.position, scope)
        }
        glassState.onRelease(scope)
    }
}
