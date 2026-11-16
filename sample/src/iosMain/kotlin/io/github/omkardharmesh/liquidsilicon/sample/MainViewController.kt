package io.github.omkardharmesh.liquidsilicon.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { SampleApp() }
