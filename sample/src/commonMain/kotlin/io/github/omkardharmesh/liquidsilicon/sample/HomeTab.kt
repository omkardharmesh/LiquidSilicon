package io.github.omkardharmesh.liquidsilicon.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.omkardharmesh.liquidsilicon.GlassButton
import io.github.omkardharmesh.liquidsilicon.GlassIcon
import io.github.omkardharmesh.liquidsilicon.GlassIconButton
import io.github.omkardharmesh.liquidsilicon.GlassStyle
import io.github.omkardharmesh.liquidsilicon.GlassSurface
import io.github.omkardharmesh.liquidsilicon.GlassText

private val HeroHeight = 560.dp
private val HeroButtonsTop = 468.dp
private val HomeBackgroundColor = Color(0xFF0B0B10)
private val ButtonLabel = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

@Composable
internal fun HomeBackground(scroll: ScrollState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(HomeBackgroundColor)
            .verticalScroll(scroll)
    ) {
        Hero(Shows[0])
        Rail("Continue watching", Shows.subList(1, 7), showProgress = true)
        TopTen(Shows.subList(2, 9))
        Rail("Trending romance", listOf(Shows[3], Shows[5], Shows[0], Shows[8], Shows[6]))
        Rail("New this week", Shows.reversed().take(6))
        Spacer(Modifier.height(TabBarClearance + 40.dp))
    }
}

@Composable
internal fun BoxScope.HomeOverlay(scroll: ScrollState, onPlay: (Int) -> Unit) {
    var inList by rememberSaveable { mutableStateOf(false) }
    Row(
        Modifier
            .align(Alignment.TopStart)
            .offset { IntOffset(0, HeroButtonsTop.roundToPx() - scroll.value) }
            .graphicsLayer {
                val top = HeroButtonsTop.toPx() - scroll.value
                alpha = ((top - 96.dp.toPx()) / 80.dp.toPx()).coerceIn(0f, 1f)
            }
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassButton(
            onClick = { onPlay(0) },
            modifier = Modifier.weight(1f).height(52.dp),
            style = GlassStyle.Regular.tint(Color.White)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIcon(SampleIcons.Play, null, size = 22.dp, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                GlassText("Play", style = ButtonLabel, color = Color.Black)
            }
        }
        GlassButton(onClick = { inList = !inList }, modifier = Modifier.weight(1f).height(52.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIcon(if (inList) SampleIcons.Check else SampleIcons.Add, null, size = 22.dp)
                Spacer(Modifier.width(6.dp))
                GlassText(if (inList) "Added" else "My List", style = ButtonLabel)
            }
        }
    }
    Box(
        Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .graphicsLayer { alpha = (scroll.value / 240.dp.toPx()).coerceIn(0f, 1f) }
            .background(Brush.verticalGradient(listOf(HomeBackgroundColor.copy(alpha = 0.85f), Color.Transparent)))
    ) {
        Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).height(84.dp))
    }
    Row(
        Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassSurface(Modifier.height(44.dp)) {
            GlassText(
                "LiquidSilicon",
                Modifier.padding(horizontal = 18.dp),
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black)
            )
        }
        Spacer(Modifier.weight(1f))
        GlassIconButton(onClick = {}, icon = SampleIcons.Search, contentDescription = "Search", size = 44.dp)
        GlassIconButton(onClick = {}, icon = SampleIcons.Bell, contentDescription = "Notifications", size = 44.dp)
    }
}

@Composable
private fun Hero(show: Show) {
    val phase = rememberScenePhase(9000)
    Box(Modifier.fillMaxWidth().height(HeroHeight)) {
        PosterArt(show, Modifier.fillMaxSize(), phase = { phase.value })
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.22f to Color.Transparent,
                        0.6f to Color.Transparent,
                        1f to HomeBackgroundColor
                    )
                )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 112.dp)) {
            BasicText(
                "#1 today",
                Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                style = TextStyle(Color(0xFFFFD60A), 12.sp, FontWeight.Bold)
            )
            Spacer(Modifier.height(10.dp))
            BasicText(
                show.title.uppercase(),
                style = TextStyle(Color.White, 42.sp, FontWeight.Black, lineHeight = 42.sp)
            )
            Spacer(Modifier.height(8.dp))
            BasicText(
                "${show.genre} · ${show.episodes} episodes · 2026",
                style = TextStyle(Color.White.copy(alpha = 0.8f), 14.sp)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    BasicText(
        title,
        Modifier.padding(horizontal = 20.dp),
        style = TextStyle(Color.White, 19.sp, FontWeight.Bold)
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Rail(title: String, items: List<Show>, showProgress: Boolean = false) {
    Column(Modifier.padding(top = 28.dp)) {
        SectionTitle(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { show ->
                Column(Modifier.width(118.dp)) {
                    Poster(show, Modifier.fillMaxWidth().height(168.dp))
                    if (showProgress) {
                        Spacer(Modifier.height(8.dp))
                        ProgressLine(((show.seed * 37) % 75 + 12) / 100f)
                    }
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        show.title,
                        style = TextStyle(Color.White, 13.sp, FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        if (showProgress) "EP ${(show.seed * 7) % show.episodes + 1}" else "${show.episodes} episodes",
                        style = TextStyle(Color.White.copy(alpha = 0.55f), 11.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopTen(items: List<Show>) {
    Column(Modifier.padding(top = 28.dp)) {
        SectionTitle("Top 10 today")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(items) { index, show ->
                Box(Modifier.width(158.dp).height(176.dp)) {
                    BasicText(
                        "${index + 1}",
                        Modifier.align(Alignment.BottomStart),
                        style = TextStyle(Color(0xFF3A3A46), 120.sp, FontWeight.Black, lineHeight = 120.sp)
                    )
                    Poster(show, Modifier.align(Alignment.CenterEnd).width(108.dp).height(160.dp), titleSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ProgressLine(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color.White.copy(alpha = 0.2f), CircleShape)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(3.dp)
                .background(Color(0xFFFF375F), CircleShape)
        )
    }
}
