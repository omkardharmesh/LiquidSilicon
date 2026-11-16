package io.github.omkardharmesh.liquidsilicon.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import io.github.omkardharmesh.liquidsilicon.GlassScreen
import io.github.omkardharmesh.liquidsilicon.GlassTabBar
import io.github.omkardharmesh.liquidsilicon.GlassTabItem

private val Tabs = listOf(
    GlassTabItem("Home", SampleIcons.Home),
    GlassTabItem("Shorts", SampleIcons.Play),
    GlassTabItem("Lab", SampleIcons.Tune),
    GlassTabItem("About", SampleIcons.Info)
)

@Composable
fun SampleApp(
    initialTab: Int = 0,
    initialScroll: Int = 0,
    onLightBackgroundChange: (Boolean) -> Unit = {}
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var tiltLight by rememberSaveable { mutableStateOf(true) }
    val homeScroll = rememberScrollState(initialScroll)
    val pager = rememberPagerState { Shows.size }
    val lab = remember { LabState() }
    val haptics = LocalHapticFeedback.current
    val lightBackground = tab == 3 || (tab == 2 && lab.pattern != 0)
    val onLightChange by rememberUpdatedState(onLightBackgroundChange)
    LaunchedEffect(lightBackground) { onLightChange(lightBackground) }

    GlassScreen(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        tiltLight = tiltLight,
        background = {
            when (tab) {
                0 -> HomeBackground(homeScroll)
                1 -> ShortsBackground(pager)
                2 -> LabBackground(lab)
                else -> AboutBackground()
            }
        }
    ) {
        when (tab) {
            0 -> HomeOverlay(homeScroll, onPlay = { index ->
                pager.requestScrollToPage(index)
                tab = 1
            })
            1 -> ShortsOverlay(pager)
            2 -> LabOverlay(lab)
            else -> AboutOverlay(tiltLight, onTiltLightChange = { tiltLight = it })
        }
        GlassTabBar(
            items = Tabs,
            selectedIndex = tab,
            onTabSelected = {
                if (it != tab) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    tab = it
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .fillMaxWidth()
        )
    }
}
