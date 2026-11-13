package com.liquidglass.sample

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Liquid Glass",
        state = rememberWindowState(size = DpSize(420.dp, 880.dp))
    ) {
        SampleApp()
    }
}
