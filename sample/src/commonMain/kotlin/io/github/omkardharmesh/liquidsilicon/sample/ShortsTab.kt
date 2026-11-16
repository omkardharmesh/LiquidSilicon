package io.github.omkardharmesh.liquidsilicon.sample

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.omkardharmesh.liquidsilicon.GlassContainer
import io.github.omkardharmesh.liquidsilicon.GlassContainerScope
import io.github.omkardharmesh.liquidsilicon.GlassIcon
import io.github.omkardharmesh.liquidsilicon.GlassShape
import io.github.omkardharmesh.liquidsilicon.GlassSurface
import io.github.omkardharmesh.liquidsilicon.GlassTab
import io.github.omkardharmesh.liquidsilicon.GlassTabBar
import io.github.omkardharmesh.liquidsilicon.GlassText
import io.github.omkardharmesh.liquidsilicon.glassContentColor
import kotlin.math.roundToInt

private val ActionStep = 78.dp
private val ActionSize = 52.dp
private val LabelHeight = 22.dp

@Composable
internal fun ShortsBackground(pager: PagerState) {
    VerticalPager(
        state = pager,
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) { page ->
        ShortsScene(Shows[page])
    }
}

@Composable
private fun ShortsScene(show: Show) {
    val phase = rememberScenePhase(6000)
    Box(Modifier.fillMaxSize()) {
        PosterArt(show, Modifier.fillMaxSize(), phase = { phase.value })
        BasicText(
            "“${show.line}”",
            Modifier.align(Alignment.Center).offset(y = 90.dp).padding(start = 32.dp, end = 88.dp),
            style = TextStyle(
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 10f)
            )
        )
    }
}

@Composable
internal fun BoxScope.ShortsOverlay(pager: PagerState) {
    val page = pager.currentPage
    val show = Shows[page]
    var feed by rememberSaveable { mutableIntStateOf(0) }
    GlassTabBar(
        selectedIndex = feed,
        onTabSelected = { feed = it },
        tabCount = 2,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 8.dp)
            .width(220.dp),
        height = 44.dp
    ) {
        listOf("For You", "Following").forEachIndexed { index, label ->
            GlassTab(selected = index == feed, onClick = { feed = index }) {
                GlassText(label, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
            }
        }
    }
    ShortsActions(
        show = show,
        page = page,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(end = 10.dp, bottom = TabBarClearance + 116.dp)
    )
    CaptionCard(
        show = show,
        page = page,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp)
            .padding(bottom = TabBarClearance + 4.dp)
            .fillMaxWidth()
    )
}

@Composable
private fun ShortsActions(show: Show, page: Int, modifier: Modifier) {
    val liked = remember { mutableStateMapOf<Int, Boolean>() }
    val saved = remember { mutableStateMapOf<Int, Boolean>() }
    var shareOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val progress by animateFloatAsState(
        targetValue = if (shareOpen) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 210f),
        label = "share"
    )
    val isLiked = liked[page] == true
    val isSaved = saved[page] == true
    val baseLikes = 18_000 + show.seed * 13_700

    GlassContainer(
        modifier = modifier.size(width = 272.dp, height = ActionStep * 3 + ActionSize + LabelHeight),
        spacing = 18.dp,
        contentAlignment = Alignment.BottomEnd
    ) {
        val buds = listOf(SampleIcons.Link to "Copy link", SampleIcons.Download to "Save video", SampleIcons.Star to "Rate")
        buds.forEachIndexed { index, (icon, label) ->
            val distance = 66.dp * (index + 1)
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            -(2.dp.roundToPx() + (distance.toPx() * progress).roundToInt()),
                            -(LabelHeight + 2.dp).roundToPx()
                        )
                    }
                    .size(48.dp)
                    .graphicsLayer {
                        val s = 0.3f + 0.7f * progress.coerceIn(0f, 1f)
                        scaleX = s
                        scaleY = s
                    }
                    .glass(GlassShape.Circle, interactive = true)
                    .clickable(interactionSource = null, indication = null, enabled = shareOpen) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        shareOpen = false
                    },
                contentAlignment = Alignment.Center
            ) {
                GlassIcon(icon, label, Modifier.alpha(progress.coerceIn(0f, 1f)), size = 22.dp)
            }
        }
        Action(
            icon = SampleIcons.Favorite,
            label = compactCount(baseLikes + if (isLiked) 1 else 0),
            slot = 3,
            iconTint = if (isLiked) Color(0xFFFF375F) else Color.Unspecified,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                liked[page] = !isLiked
            }
        )
        Action(SampleIcons.Chat, compactCount(baseLikes / 23), slot = 2, onClick = {})
        Action(
            icon = SampleIcons.Bookmark,
            label = if (isSaved) "Saved" else "Save",
            slot = 1,
            iconTint = if (isSaved) Color(0xFFFFD60A) else Color.Unspecified,
            onClick = { saved[page] = !isSaved }
        )
        Action(
            icon = SampleIcons.Share,
            label = "Share",
            slot = 0,
            tint = if (shareOpen) Accent else Color.Unspecified,
            iconTint = if (shareOpen) Color.White else Color.Unspecified,
            onClick = { shareOpen = !shareOpen }
        )
    }
}

@Composable
private fun GlassContainerScope.Action(
    icon: ImageVector,
    label: String,
    slot: Int,
    onClick: () -> Unit,
    tint: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified
) {
    Box(
        Modifier
            .offset { IntOffset(-2.dp.roundToPx(), -(slot * ActionStep.roundToPx()) - LabelHeight.roundToPx()) }
            .size(ActionSize)
            .glass(GlassShape.Circle, interactive = true, tint = tint)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        GlassIcon(icon, label, size = 24.dp, tint = iconTint)
    }
    Box(
        Modifier
            .offset { IntOffset(0, -(slot * ActionStep.roundToPx())) }
            .width(56.dp)
            .height(LabelHeight),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 1f), 4f)
            )
        )
    }
}

@Composable
private fun CaptionCard(show: Show, page: Int, modifier: Modifier) {
    GlassSurface(modifier, shape = GlassShape.RoundedRect(26.dp), contentAlignment = Alignment.TopStart) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
            GlassText(show.title, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            GlassText(
                "Episode ${page + 1} of ${show.episodes} · ${show.genre}",
                style = TextStyle(fontSize = 13.sp),
                color = glassContentColor().copy(alpha = 0.75f),
                maxLines = 1
            )
            Spacer(Modifier.height(12.dp))
            EpisodeProgress()
        }
    }
}

@Composable
private fun EpisodeProgress() {
    val color = glassContentColor()
    val transition = rememberInfiniteTransition(label = "episode")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )
    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
        val y = size.height / 2
        val stroke = size.height
        drawLine(color.copy(alpha = 0.25f), Offset(stroke / 2, y), Offset(size.width - stroke / 2, y), stroke, StrokeCap.Round)
        val end = stroke / 2 + (size.width - stroke) * progress.value
        drawLine(color, Offset(stroke / 2, y), Offset(end, y), stroke, StrokeCap.Round)
    }
}
